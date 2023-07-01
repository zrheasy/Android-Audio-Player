package com.zrh.audio.player.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.zrh.audio.player.R
import com.zrh.audio.player.utils.dp2px

/**
 * Author zrh
 * Date 2022/11/18 16:43
 * Description
 */
class ProgressImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {
    private val circleRect: RectF = RectF()

    private var progress: Float = 0f
    private val progressPaint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        style = Paint.Style.STROKE
        strokeWidth = context.dp2px(2).toFloat()
        color = ContextCompat.getColor(context, R.color.green)
    }

    private var borderNormalColor: Int = ContextCompat.getColor(context, R.color.background)
    private val borderPaint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        style = Paint.Style.STROKE
        strokeWidth = context.dp2px(2).toFloat()
        color = borderNormalColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = w.coerceAtMost(h) - progressPaint.strokeWidth
        circleRect.top = (h - size) / 2f
        circleRect.bottom = circleRect.top + size
        circleRect.left = (w - size) / 2f
        circleRect.right = circleRect.left + size
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawOval(circleRect, borderPaint)

        val angle = (360 * progress / 100).toFloat()
        canvas.drawArc(circleRect, -90f, angle, false, progressPaint)
    }

    fun setProgress(progress: Float) {
        when {
            progress < 0 -> this.progress = 0f
            progress > 100 -> this.progress = 100f
            else -> this.progress = progress
        }
        invalidate()
    }
}