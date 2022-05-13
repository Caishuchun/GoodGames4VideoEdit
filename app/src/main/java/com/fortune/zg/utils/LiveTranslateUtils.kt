package com.fortune.zg.utils

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation


object LiveTranslateUtils {
    /**
     * 顶部进入动画
     */
    fun topEnter(view: View) {
        val mTopEnter = TranslateAnimation(
            1,
            0f,
            1,
            0f,
            1,
            -1f,
            1,
            0f
        )
        mTopEnter.duration = 200
        mTopEnter.interpolator = AccelerateInterpolator()
        mTopEnter.fillAfter = true
        view.startAnimation(mTopEnter)
    }

    /**
     * 顶部退出动画
     */
    fun topExit(view: View) {
        val mTopExit = TranslateAnimation(
            1,
            0f,
            1,
            0f,
            1,
            0f,
            1,
            -2f
        )
        mTopExit.duration = 200
        mTopExit.interpolator = AccelerateInterpolator()
        mTopExit.fillAfter = true
        mTopExit.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        view.startAnimation(mTopExit)
    }

    /**
     * 底部进入动画
     */
    fun bottomEnter(view: View) {
        val mBottomEnter = TranslateAnimation(
            1,
            0f,
            1,
            0f,
            1,
            1f,
            1,
            0f
        )
        mBottomEnter.duration = 200
        mBottomEnter.interpolator = AccelerateInterpolator()
        mBottomEnter.fillAfter = true
        view.startAnimation(mBottomEnter)
    }

    /**
     * 底部退出动画
     */
    fun bottomExit(view: View) {
        val mBottomExit = TranslateAnimation(
            1,
            0f,
            1,
            0f,
            1,
            0f,
            1,
            1f
        )
        mBottomExit.duration = 200
        mBottomExit.interpolator = AccelerateInterpolator()
        mBottomExit.fillAfter = true
        mBottomExit.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        view.startAnimation(mBottomExit)
    }
}