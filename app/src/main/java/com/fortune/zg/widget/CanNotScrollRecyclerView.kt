package com.fortune.zg.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class CanNotScrollRecyclerView : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    )

    /**
     * 禁止触摸事件,不禁止smoothScrollToPosition
     */
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        return true
    }

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        return false
    }
}