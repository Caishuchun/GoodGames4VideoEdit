package com.fortune.zg.issue

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

class CustomVideoView : VideoView {

    private var mListener: PlayPauseListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    )

    interface PlayPauseListener {
        fun onPlay()
        fun onPause()
    }

    fun setPlayPauseListener(listener: PlayPauseListener) {
        this.mListener = listener
    }

    override fun start() {
        super.start()
        mListener?.onPlay()
    }

    override fun pause() {
        super.pause()
        mListener?.onPause()
    }
}