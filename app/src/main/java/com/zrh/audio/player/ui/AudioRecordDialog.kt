package com.zrh.audio.player.ui

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.WindowManager
import com.zrh.audio.lib.AudioRecorder
import com.zrh.audio.player.R
import com.zrh.audio.player.databinding.DialogRecordBinding
import com.zrh.audio.player.utils.AppUtils
import com.zrh.audio.player.utils.getScreenHeight
import com.zrh.audio.player.utils.toast
import java.io.File

/**
 *
 * @author zrh
 * @date 2023/6/30
 *
 */
typealias OnComplete = (duration: Int, file: File) -> Unit

class AudioRecordDialog(context: Context) : Dialog(context, R.style.AlertDialog), AudioRecorder.RecordListener {
    private val mBinding: DialogRecordBinding = DialogRecordBinding.inflate(layoutInflater)
    private var onComplete: OnComplete? = null
    private val recorder: AudioRecorder by lazy { AudioRecorder(context.filesDir.absolutePath + "/audio") }

    init {
        setContentView(mBinding.root)
        setCanceledOnTouchOutside(false)
        setCancelable(false)

        mBinding.btnCancel.setOnClickListener {
            recorder.cancel()
            dismiss()
        }
        mBinding.btnStop.setOnClickListener {
            recorder.stop()
        }

        recorder.setListener(this)

        setOnDismissListener { recorder.setListener(null) }
    }

    fun setOnComplete(onComplete: OnComplete) {
        this.onComplete = onComplete
    }

    override fun show() {
        super.show()

        val window = window
        val p = window!!.attributes
        p.width = WindowManager.LayoutParams.MATCH_PARENT
        p.height = context.getScreenHeight()
        p.gravity = Gravity.CENTER
        window.attributes = p

        recorder.start()
    }

    override fun onError(code: Int, msg: String) {
        context.toast("record error!")
        dismiss()
    }

    override fun onRecording(duration: Int, volume: Int) {
        mBinding.tvDuration.text = AppUtils.formatTime(duration/1000)
    }

    override fun onComplete(duration: Int, output: File) {
        onComplete?.invoke(duration, output)
        dismiss()
    }
}