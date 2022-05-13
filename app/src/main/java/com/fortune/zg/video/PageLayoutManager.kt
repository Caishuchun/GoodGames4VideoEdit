package com.fortune.zg.video

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class PageLayoutManager : LinearLayoutManager, RecyclerView.OnChildAttachStateChangeListener {

    private var mPagerSnapHelper: PagerSnapHelper? = null
    private var mDrift = 0 //偏移量
    private var mOnViewPagerListener: OnViewPagerListener? = null
    private var currentPosition = -1 //当前position

    constructor(context: Context) : super(context)
    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    ) {
        mPagerSnapHelper = PagerSnapHelper()
    }

    /**
     * layoutManager完全加入到RecycleView中的时候调用
     */
    override fun onAttachedToWindow(view: RecyclerView?) {
        view?.addOnChildAttachStateChangeListener(this)
        mPagerSnapHelper?.attachToRecyclerView(view)
        super.onAttachedToWindow(view)
    }

    /**
     * 垂直方向滑动偏移量
     */
    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        mDrift = dy
        return super.scrollVerticallyBy(dy, recycler, state)
    }

    /**
     * 垂直滑动
     */
    override fun canScrollVertically(): Boolean {
        return true
    }

    /**
     * 滑动监听
     */
    override fun onScrollStateChanged(state: Int) {
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            //滑动结束,暂停了
            val snapView = mPagerSnapHelper?.findSnapView(this)
            if (null != snapView) {
                val position = getPosition(snapView)
                if (currentPosition != position) {
                    currentPosition = position
                    mOnViewPagerListener?.onPageSelected(snapView, position)
                }
            }
        }
        super.onScrollStateChanged(state)
    }

    /**
     * Item添加进来
     */
    override fun onChildViewAttachedToWindow(view: View) {
        val position = getPosition(view)
//        if (mDrift >= 50) {
//            //向上滑
//            LogUtils.d("----------------------1111up,position=$position")
//            mOnViewPagerListener?.onPageSelected(view, position)
//        } else if (mDrift < -50) {
//            //向下滑
//            LogUtils.d("----------------------1111down,position=$position")
//            mOnViewPagerListener?.onPageSelected(view, position)
//        }
    }

    /**
     * Item移除
     */
    override fun onChildViewDetachedFromWindow(view: View) {
        val position = getPosition(view)
        if (mDrift >0) {
            //向上滑
            mOnViewPagerListener?.onPageRelease(view, position)
        } else if (mDrift < 0) {
            //向下滑
            mOnViewPagerListener?.onPageRelease(view, position)
        }
    }

    fun setOnViewPagerListener(listener: OnViewPagerListener) {
        this.mOnViewPagerListener = listener
    }
}