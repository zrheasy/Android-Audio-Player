package com.zrh.audio.player.utils

import android.content.Context
import android.util.TypedValue
import android.widget.Toast

/**
 *
 * @author zrh
 * @date 2023/6/30
 *
 */
object AppUtils {
    fun formatTime(time: Int): String {
        val minutes = time / 60
        val seconds = time % 60
        return "${formatNumber(minutes)}:${formatNumber(seconds)}"
    }

    private fun formatNumber(num: Int): String {
        return if (num > 9) num.toString() else "0$num"
    }
}

fun Context.getScreenHeight(): Int {
    return resources.displayMetrics.heightPixels
}

fun Context.getScreenWidth(): Int {
    return resources.displayMetrics.widthPixels
}

fun Context.dp2px(dpValue: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dpValue.toFloat(),
        resources.displayMetrics
    ).toInt()
}

fun Context.sp2px(spVal: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        spVal.toFloat(),
        resources.displayMetrics
    ).toInt()
}

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}