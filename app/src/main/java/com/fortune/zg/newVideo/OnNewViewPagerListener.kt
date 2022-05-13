package com.fortune.zg.newVideo

import android.view.View

interface OnNewViewPagerListener {

    //初始化完成
    fun onInitComplete(itemView: View,position: Int)

    //释放的监听
    fun onPageRelease(itemView: View, isNext: Boolean, position: Int)

    //选中监听以及判断是否滑动到底部
    fun onPageSelected(itemView: View, position: Int, isBottom: Boolean)
}