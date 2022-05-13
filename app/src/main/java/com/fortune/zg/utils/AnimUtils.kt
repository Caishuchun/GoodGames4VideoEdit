package com.fortune.zg.utils

import android.view.View
import android.view.animation.AlphaAnimation

/**
 *滚动通知的淡入淡出动画
 */
object AnimUtils {

    /**
     * 淡入淡出
     * @param fade true 淡入  false 淡出
     */
    fun alpha(view: View, fade: Boolean, time: Long = 1000) {
        val alphaAnimation = if (fade) {
            AlphaAnimation(0f, 1f)
        } else {
            AlphaAnimation(1f, 0f)
        }
        alphaAnimation.duration = time
        alphaAnimation.fillAfter = true
        view.startAnimation(alphaAnimation)
    }

    fun clear(view: View) {
        view.clearAnimation()
    }
}