package com.zrh.audio.player.ui

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.setMargins
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.zrh.audio.lib.AudioUtils
import com.zrh.audio.player.R
import com.zrh.audio.player.databinding.ActivityMainBinding
import com.zrh.audio.player.databinding.PopDeleteBinding
import com.zrh.audio.player.databinding.PopOptionsBinding
import com.zrh.audio.player.db.entity.AudioEntity
import com.zrh.audio.player.service.AudioPlayService
import com.zrh.audio.player.service.IAudioPlayService
import com.zrh.audio.player.utils.FileUtils
import com.zrh.audio.player.utils.dp2px
import com.zrh.audio.player.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * ✅️ 支持音频列表展示
 * ✅ 支持录音
 * ✅️ 支持选择本地文件播放
 * ✅️️ 支持前后台播放音频
 */
class MainActivity : AppCompatActivity(), AudioListAdapter.OnActionListener {

    private val mViewModel: AudioViewModel by viewModels()
    private lateinit var mBinding: ActivityMainBinding

    private val mAudioAdapter: AudioListAdapter by lazy { AudioListAdapter() }
    private val audioPermissionLauncher: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.getOrDefault(Manifest.permission.RECORD_AUDIO, false)) {
            startRecord()
        } else {
            toast("Please grant record audio permission!")
        }
    }
    private val sdcardPermissionLauncher: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.getOrDefault(Manifest.permission.WRITE_EXTERNAL_STORAGE, false)) {
            selectFile()
        } else {
            toast("Please grant access media file permission!")
        }
    }
    private val selectFileLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                if (data.data != null) {
                    parseAudioInfo(data.data!!)
                }
            }
        }

    // 选中的AudioItem
    private var mSelectedAudioItem: AudioListAdapter.AudioItem? = null

    // 与Service通信的Connection
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mAudioPlayService = service as IAudioPlayService
            registerReceiver(receiver, IntentFilter(AudioPlayService.EVENT_ACTION))
            loadAudioList()
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    // Service代理
    private var mAudioPlayService: IAudioPlayService? = null

    // 监听Service广播
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != AudioPlayService.EVENT_ACTION) return

            val id = intent.getLongExtra(AudioPlayService.AUDIO_ID, 0)
            if (mSelectedAudioItem?.data?.id != id) {
                val item = mAudioAdapter.findItem(id) ?: return
                mAudioAdapter.selectItem(item)
                updatePlayInfo(item)
                updatePlayState(false)
                updatePlayProgress(0f)
            }
            val audioItem = mSelectedAudioItem ?: return

            //根据播放事件更新UI
            val type = intent.getIntExtra(AudioPlayService.EVENT_TYPE, -1)
            if (type == -1) return
            when (type) {
                AudioPlayService.EVENT_COMPLETE, AudioPlayService.EVENT_ERROR -> {
                    mAudioAdapter.pauseItem(audioItem)
                    updatePlayState(false)
                    updatePlayProgress(0f)
                    if (type == AudioPlayService.EVENT_ERROR) toast("Play error!")
                }
                AudioPlayService.EVENT_PROGRESS -> {
                    val progress = intent.getIntExtra(AudioPlayService.AUDIO_PROGRESS, 0)
                    val duration = intent.getIntExtra(AudioPlayService.AUDIO_DURATION, 1)
                    updatePlayProgress(100f * progress / duration)
                }
                AudioPlayService.EVENT_PLAY -> {
                    mAudioAdapter.playItem(audioItem)
                    updatePlayState(true)
                }
                AudioPlayService.EVENT_PAUSE -> {
                    mAudioAdapter.pauseItem(audioItem)
                    updatePlayState(false)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        initAudioListView()
        initListener()

        connectAudioService()
    }

    private fun connectAudioService() {
        bindService(Intent(this, AudioPlayService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    private fun initAudioListView() {
        mBinding.rvAudioList.apply {
            val space = dp2px(10)
            addItemDecoration(object :ItemDecoration(){
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    if (parent.getChildAdapterPosition(view) == 0){
                        outRect.top = space
                    }
                    outRect.bottom = space
                }
            })
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mAudioAdapter
        }
    }

    private fun initListener() {
        mBinding.btnAdd.setOnClickListener {
            showPopWindow()
        }
        mAudioAdapter.setOnActionListener(this)
        mBinding.ivPlayIcon.setOnClickListener {
            mSelectedAudioItem?.let { item ->
                if (item.isPlaying) {
                    onItemPause(item)
                } else {
                    onItemPlay(item)
                }
            }
        }
    }

    private fun showPopWindow() {
        val pop = PopupWindow(this)
        val binding = PopOptionsBinding.inflate(layoutInflater)
        binding.btnRecord.setOnClickListener {
            pop.dismiss()
            requestPermissionAndRecord()
        }
        binding.btnSelect.setOnClickListener {
            pop.dismiss()
            requestPermissionAndSelectFile()
        }

        pop.contentView = binding.root
        pop.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.transparent)))
        pop.isOutsideTouchable = true
        pop.isFocusable = true
        pop.showAsDropDown(mBinding.btnAdd, 0, 0, Gravity.CENTER)
        val lp = pop.contentView.layoutParams as ViewGroup.MarginLayoutParams
        lp.width = dp2px(200)
        lp.setMargins(dp2px(10))
        pop.contentView.layoutParams = lp
        ViewCompat.setElevation(pop.contentView, 8f)
    }

    private fun requestPermissionAndRecord() {
        val granted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startRecord()
        } else {
            audioPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun startRecord() {
        AudioRecordDialog(this).apply {
            setOnComplete { duration, file ->
                this@MainActivity.insertAudio(duration, file)
            }
            show()
        }
    }

    private fun insertAudio(duration: Int, file: File) {
        val entity = AudioEntity(
            name = file.name,
            filePath = file.absolutePath,
            duration = duration,
            createAt = System.currentTimeMillis()
        )
        mViewModel.insertAudio(entity)
            .onEach {
                mAudioAdapter.addItem(0, AudioListAdapter.AudioItem(it))
            }
            .launchIn(lifecycleScope)
    }

    private fun requestPermissionAndSelectFile() {
        val granted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            selectFile()
        } else {
            sdcardPermissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun selectFile() {
        val i = Intent()
        i.type = "audio/*"
        i.putExtra(Intent.EXTRA_MIME_TYPES, arrayListOf("audio/*"))
        i.action = Intent.ACTION_GET_CONTENT
        i.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            selectFileLauncher.launch(i)
        } catch (e: Exception) {
            e.printStackTrace()
            toast("error")
        }
    }

    private fun parseAudioInfo(uri: Uri) {
        flow {
            var cursor: Cursor? = null
            try {
                val projection = arrayOf(
                    OpenableColumns.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.MIME_TYPE
                )
                cursor = contentResolver.query(uri, projection, null, null, null) ?: return@flow
                cursor.moveToFirst()

                val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)) ?: ""
                if (!mimeType.startsWith("audio")) return@flow

                val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))
                if (size == 0L) return@flow

                val name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)) ?: ""
                val file =
                    FileUtils.getFileFromUri(applicationContext, uri, File(filesDir, "audio"), name) ?: return@flow

                var duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                if (duration == 0) {
                    duration = AudioUtils.getDuration(file)
                }
                val entity = AudioEntity(name, file.absolutePath, duration, System.currentTimeMillis())
                emit(entity)
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
            .flowOn(Dispatchers.IO)
            .flatMapConcat { mViewModel.insertAudio(it) }
            .onEach {
                mAudioAdapter.addItem(0, AudioListAdapter.AudioItem(it))
            }
            .launchIn(lifecycleScope)
    }

    /**
     * 加载音频列表
     */
    private fun loadAudioList() {
        mViewModel.getAudioList()
            .onEach { list ->
                val playing = mAudioPlayService?.isPlaying() ?: false
                val audioId = mAudioPlayService?.getAudioId() ?: 0
                val progress = mAudioPlayService?.getProgress() ?: 0
                val duration = mAudioPlayService?.getDuration() ?: 1
                mAudioAdapter.setList(list.map {
                    AudioListAdapter.AudioItem(it).apply {
                        // 更新正在播放的音频信息
                        if (audioId == it.id) {
                            isSelected = true
                            isPlaying = playing
                            updatePlayInfo(this)
                            updatePlayProgress(100f * progress / duration)
                            updatePlayState(isPlaying)
                        }
                    }
                })
            }
            .launchIn(lifecycleScope)
    }

    override fun onItemPlay(item: AudioListAdapter.AudioItem) {
        mAudioPlayService?.apply {
            if (item.data.id != getAudioId()) {
                setAudio(item.data)
            }
            play()
        }
    }

    override fun onItemPause(item: AudioListAdapter.AudioItem) {
        mAudioPlayService?.pause()
    }

    override fun onLongClick(view:View, item: AudioListAdapter.AudioItem) {
        val pop = PopupWindow(this)
        val binding = PopDeleteBinding.inflate(layoutInflater)
        binding.btnDelete.setOnClickListener {
            pop.dismiss()
            onDelete(item)
        }

        pop.contentView = binding.root
        pop.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.transparent)))
        pop.isOutsideTouchable = true
        pop.isFocusable = true
        pop.showAsDropDown(view, 0, 0, Gravity.CENTER)
        val lp = pop.contentView.layoutParams as ViewGroup.MarginLayoutParams
        lp.width = dp2px(200)
        lp.setMargins(dp2px(10))
        pop.contentView.layoutParams = lp
        ViewCompat.setElevation(pop.contentView, 8f)
    }

    private fun onDelete(item: AudioListAdapter.AudioItem) {
        mViewModel.deleteAudio(item.data)
            .onEach {
                mAudioAdapter.deleteItem(item)
                if (item == mSelectedAudioItem){
                    updatePlayInfo(null)
                    updatePlayState(false)
                    updatePlayProgress(0f)
                    mAudioPlayService?.stop()
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun updatePlayInfo(item: AudioListAdapter.AudioItem?) {
        mSelectedAudioItem = item
        mBinding.tvAudioName.text = item?.data?.name ?: ""
    }

    private fun updatePlayProgress(progress: Float) {
        mBinding.ivPlayIcon.setProgress(progress)
    }

    private fun updatePlayState(isPlaying: Boolean) {
        val icon = if (isPlaying) R.mipmap.pause else R.mipmap.play
        mBinding.ivPlayIcon.setImageResource(icon)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        unregisterReceiver(receiver)
    }
}