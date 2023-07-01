package com.zrh.audio.player.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zrh.audio.player.R
import com.zrh.audio.player.databinding.ItemAudioBinding
import com.zrh.audio.player.db.entity.AudioEntity
import com.zrh.audio.player.utils.AppUtils

/**
 *
 * @author zrh
 * @date 2023/6/30
 *
 */
class AudioListAdapter : RecyclerView.Adapter<AudioListAdapter.AudioViewHolder>() {

    private val mList = ArrayList<AudioItem>()
    private var onActionListener: OnActionListener? = null

    fun setOnActionListener(listener: OnActionListener) {
        onActionListener = listener
    }

    fun setList(list: List<AudioItem>) {
        mList.clear()
        mList.addAll(list)
        notifyDataSetChanged()
    }

    fun addItem(i: Int, audioItem: AudioItem) {
        mList.add(i, audioItem)
        notifyDataSetChanged()
    }

    fun deleteItem(item: AudioItem) {
        val index = mList.indexOf(item)
        if (index != -1) {
            mList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun findItem(id: Long): AudioItem? {
        val list = mList.filter { it.data.id == id }
        if (list.isNotEmpty()) return list[0]
        return null
    }

    fun selectItem(item: AudioItem) {
        unselectAndPauseOtherItem(item)
        item.isSelected = true
        notifyItemChanged(item)
    }

    fun notifyItemChanged(item: AudioItem) {
        val index = mList.indexOf(item)
        if (index != -1) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return AudioViewHolder(ItemAudioBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val item = mList[position]
        val binding = holder.binding

        binding.tvAudioName.text = item.data.name
        binding.tvAudioDuration.text = item.duration

        val icon = if (item.isPlaying) R.mipmap.pause else R.mipmap.play
        binding.btnPlay.setImageResource(icon)

        binding.root.isSelected = item.isSelected

        binding.root.setOnClickListener {
            if (!item.isSelected) {
                onActionListener?.onItemPlay(item)
            }
        }
        binding.root.setOnLongClickListener {
            onActionListener?.onLongClick(it, item)
            return@setOnLongClickListener true
        }
        binding.btnPlay.setOnClickListener {
            if (item.isPlaying) {
                onActionListener?.onItemPause(item)
            } else {
                onActionListener?.onItemPlay(item)
            }
        }
    }

    private fun unselectAndPauseOtherItem(item: AudioItem) {
        mList.filter { it != item && it.isSelected }.forEach {
            it.isPlaying = false
            it.isSelected = false
            notifyItemChanged(it)
        }
    }

    fun playItem(item: AudioItem) {
        unselectAndPauseOtherItem(item)
        item.isPlaying = true
        item.isSelected = true
        notifyItemChanged(item)
    }

    fun pauseItem(item: AudioItem) {
        item.isPlaying = false
        notifyItemChanged(item)
    }

    override fun getItemCount(): Int = mList.size

    data class AudioItem(val data: AudioEntity, var isPlaying: Boolean = false, var isSelected: Boolean = false) {
        var duration: String = AppUtils.formatTime(data.duration / 1000)
    }

    class AudioViewHolder(val binding: ItemAudioBinding) : RecyclerView.ViewHolder(binding.root)

    interface OnActionListener {
        fun onItemPlay(item: AudioItem)
        fun onItemPause(item: AudioItem)
        fun onLongClick(view: View, item: AudioItem)
    }
}