package com.fortune.zg.widget

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller

/**
 * 实现recycleView位移到位置上,并吸顶
 */
class MyLinearSmoothScroller(context: Context) : LinearSmoothScroller(context) {

    override fun calculateDtToFit(
        viewStart: Int,
        viewEnd: Int,
        boxStart: Int,
        boxEnd: Int,
        snapPreference: Int
    ): Int {
        return boxStart - viewStart
    }
}