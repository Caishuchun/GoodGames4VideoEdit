package com.fortune.zg.widget

import androidx.recyclerview.widget.RecyclerView

class VideoStaggeredGridLayoutManager(
    spanCount: Int, orientation: Int
) : SafeStaggeredGridLayoutManager(spanCount, orientation) {

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