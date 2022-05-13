package com.fortune.zg.video

import android.view.View

interface OnViewPagerListener {

    //停止播放的监听
    fun onPageRelease(itemView: View, position: Int)

    //播放的监听
    fun onPageSelected(itemView: View, position: Int)
}