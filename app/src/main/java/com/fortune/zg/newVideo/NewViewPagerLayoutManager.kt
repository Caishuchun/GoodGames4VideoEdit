package com.fortune.zg.newVideo

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.fortune.zg.utils.LogUtils

class NewViewPagerLayoutManager : LinearLayoutManager {

    private var mPagerSnapHelper: PagerSnapHelper? = null
    private var mListener: OnNewViewPagerListener? = null
    private var mRecyclerView: RecyclerView? = null
    private var mDrift = 0 //位移,判断移动方向

    constructor(context: Context, orientation: Int) : super(context, orientation, false) {
        init()
    }

    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    ) {
        init()
    }

    private fun init() {
        mPagerSnapHelper = PagerSnapHelper()
    }

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        mPagerSnapHelper?.attachToRecyclerView(view)
        this.mRecyclerView = view
        mRecyclerView?.addOnChildAttachStateChangeListener(mChildAttachStateChangeListener)
    }

    private val mChildAttachStateChangeListener =
        object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                if (mListener != null && childCount == 1) {
                    mListener?.onInitComplete(view, getPosition(view))
                }
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                if (mDrift > 0) {
                    mListener?.onPageRelease(view, true, getPosition(view))
                } else if (mDrift < 0) {
                    mListener?.onPageRelease(view, false, getPosition(view))
                }
            }
        }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        //
    }

    override fun onScrollStateChanged(state: Int) {
        when (state) {
            RecyclerView.SCROLL_STATE_IDLE -> {
                //空闲停止
                val view = mPagerSnapHelper?.findSnapView(this)
                val position = view?.let { getPosition(it) }
                if (mListener != null && childCount == 1) {
                    if (position != null) {
                        mListener?.onPageSelected(view, position, position == itemCount - 1)
                    }
                }
            }
            RecyclerView.SCROLL_STATE_DRAGGING -> {
                //缓慢拖拽
                val view = mPagerSnapHelper?.findSnapView(this)
                if (view != null) {
                    val positionDrag = getPosition(view)
                }
            }
            RecyclerView.SCROLL_STATE_SETTLING -> {
                //快速滑动
                val view = mPagerSnapHelper?.findSnapView(this)
                if (view != null) {
                    val positionSetting = getPosition(view)
                }
            }
        }
    }

    /**
     * 竖直方向偏移量
     */
    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        this.mDrift = dy
        return super.scrollVerticallyBy(dy, recycler, state)
    }

    /**
     * 水平方向偏移量
     */
    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        this.mDrift = dx
        return super.scrollHorizontallyBy(dx, recycler, state)
    }

    /**
     * 设置监听
     */
    fun setOnViewPagerListener(listener: OnNewViewPagerListener) {
        this.mListener = listener
    }
}