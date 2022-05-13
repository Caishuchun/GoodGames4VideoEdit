package com.fortune.zg.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.fortune.zg.R

class VideoCoverSeekBar : LinearLayout {

    private var mView: View? = null

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        initView(context)
    }

    private fun initView(context: Context) {
        mView =
            LayoutInflater.from(context).inflate(R.layout.layout_video_cover_seekbar, this, true)
    }

}