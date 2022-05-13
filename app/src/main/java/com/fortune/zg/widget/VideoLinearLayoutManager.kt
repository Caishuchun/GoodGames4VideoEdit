package com.fortune.zg.widget

import android.content.Context
import androidx.recyclerview.widget.RecyclerView

class VideoLinearLayoutManager(
    context: Context?,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false
) : SafeLinearLayoutManager(context, orientation, reverseLayout) {

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView?,
        state: RecyclerView.State?,
        position: Int
    ) {
        val myLinearSmoothScroller = MyLinearSmoothScroller(recyclerView!!.context)
        myLinearSmoothScroller.targetPosition = position
        startSmoothScroll(myLinearSmoothScroller)
    }
}