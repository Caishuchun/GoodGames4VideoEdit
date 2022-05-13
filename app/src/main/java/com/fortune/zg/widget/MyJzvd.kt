package com.fortune.zg.widget

import android.content.Context
import android.util.AttributeSet
import cn.jzvd.JzvdStd
import com.fortune.zg.R

/**
 * 游戏详情顶部的视频播放器
 */
class MyJzvd : JzvdStd {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    override fun getLayoutId() = R.layout.layout_jzvd_my

    override fun dissmissControlView() {
        if (state != STATE_NORMAL && state != STATE_ERROR && state != STATE_AUTO_COMPLETE) {
            post {
                bottomContainer.visibility = INVISIBLE
                topContainer.visibility = INVISIBLE
                startButton.visibility = INVISIBLE
                if (screen != SCREEN_TINY) {
                    bottomProgressBar.visibility = VISIBLE
                }
            }
        }
    }
}