package com.fortune.zg.issue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.fortune.zg.R
import com.fortune.zg.issue.MoveLayout.DeleteMoveLayout
import com.fortune.zg.issue.MoveLayout.MovingListener
import java.util.*


/**
 * Created by Robert on 2017/6/21.
 */
class DragView : RelativeLayout, DeleteMoveLayout, MovingListener {
    private var mSelfViewWidth = 0
    private var mSelfViewHeight = 0
    private var mContext: Context? = null

    /**
     * the identity of the moveLayout
     */
    private var mLocalIdentity = 0
    private var mMoveLayoutList: MutableList<MoveLayout>? = null

    /*
    * 拖拽框最小尺寸
    */
    private var mMinHeight = 150
    private var mMinWidth = 300
    private val mIsAddDeleteView = false
    private var deleteArea: TextView? = null
    private val DELETE_AREA_WIDTH = 400
    private val DELETE_AREA_HEIGHT = 90

    constructor(context: Context) : super(context) {
        init(context, this)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, this)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, this)
    }

    private fun init(c: Context, thisp: DragView) {
        mContext = c
        mMoveLayoutList = ArrayList()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
//          Log.e(TAG, "onDraw: height=" + getHeight());
        mSelfViewWidth = width
        mSelfViewHeight = height
        if (mMoveLayoutList != null) {
            val count = mMoveLayoutList!!.size
            for (a in 0 until count) {
                mMoveLayoutList!![a].setViewWidthHeight(mSelfViewWidth, mSelfViewHeight)
                mMoveLayoutList!![a].setDeleteWidthHeight(DELETE_AREA_WIDTH, DELETE_AREA_HEIGHT)
            }
        }
    }

    /**
     * call set Min height before addDragView
     * @param height
     */
    private fun setMinHeight(height: Int) {
        mMinHeight = height
    }

    /**
     * call set Min width before addDragView
     * @param width
     */
    private fun setMinWidth(width: Int) {
        mMinWidth = width
    }

    /**
     * 每个moveLayout都可以拥有自己的最小尺寸
     */
    fun addDragView(
        resId: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        isFixedSize: Boolean,
        whitebg: Boolean,
        minwidth: Int,
        minheight: Int
    ) {
        val inflater2 = LayoutInflater.from(mContext)
        val selfView = inflater2.inflate(resId, null)
        addDragView(selfView, left, top, right, bottom, isFixedSize, whitebg, minwidth, minheight)
    }

    /**
     * 每个moveLayout都可以拥有自己的最小尺寸
     */
    @JvmOverloads
    fun addDragView(
        selfView: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        isFixedSize: Boolean,
        whitebg: Boolean,
        minwidth: Int = mMinWidth,
        minheight: Int = mMinHeight
    ) {
        //    invalidate();
        //  Log.e(TAG, "addDragView: height="+getHeight() +"   width+"+ getWidth() );
        val moveLayout = MoveLayout(mContext)
        moveLayout.isClickable = true
        moveLayout.setCanMoveArea(width, height)
        moveLayout.setDeleteWidthHeight(DELETE_AREA_WIDTH, DELETE_AREA_HEIGHT)
        moveLayout.setMinHeight(minheight)
        moveLayout.setMinWidth(minwidth)
        var tempWidth = right - left
        var tempHeight = bottom - top
        if (tempWidth < minwidth) tempWidth = minwidth
        if (tempHeight < minheight) tempHeight = minheight

        //set postion
        val lp = LayoutParams(tempWidth, tempHeight)
        lp.setMargins(left, top, 0, 0)
        moveLayout.layoutParams = lp

        //add sub view (has click indicator)
        val inflater = LayoutInflater.from(mContext)
        val dragSubView: View = inflater.inflate(R.layout.drag_sub_view, null)
        val addYourViewHere =
            dragSubView.findViewById<View>(R.id.add_your_view_here) as LinearLayout
        val lv = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        addYourViewHere.addView(selfView, lv)
        (selfView as ScaleEditText).setSelfMoveLayout(moveLayout)
        if (whitebg) {
            val changeBg = dragSubView.findViewById<View>(R.id.change_bg) as LinearLayout
            changeBg.setBackgroundResource(R.drawable.bg_make_video_text)
            (selfView as ScaleEditText).setBg(changeBg)
        }
        moveLayout.addView(dragSubView)

        //set fixed size
        moveLayout.setFixedSize(isFixedSize)
        moveLayout.setOnDeleteMoveLayout(this)
        //移动监听
        moveLayout.setOnMovingListener(this)
        moveLayout.identity = mLocalIdentity++
        if (!mIsAddDeleteView) {
            //add delete area
            deleteArea = TextView(mContext)
            deleteArea!!.text = "拖动到此处删除"
            deleteArea!!.setTextColor(Color.WHITE)
            deleteArea!!.setBackgroundColor(Color.argb(80, 255, 0, 0))
            val dellayout = LayoutParams(DELETE_AREA_WIDTH, DELETE_AREA_HEIGHT)
            dellayout.addRule(ALIGN_PARENT_RIGHT)
            dellayout.addRule(ALIGN_PARENT_TOP)
            deleteArea!!.layoutParams = dellayout
            deleteArea!!.gravity = Gravity.CENTER
            // moveLayout.setDeleteWidthHeight(180, 90);
            deleteArea!!.visibility = INVISIBLE
            addView(deleteArea)
        }

        //set view to get control
        moveLayout.setDeleteView(deleteArea)
        addView(moveLayout)
        mMoveLayoutList!!.add(moveLayout)
    }

    fun addDragView(
        resId: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        isFixedSize: Boolean,
        whitebg: Boolean
    ) {
        val inflater2 = LayoutInflater.from(mContext)
        val selfView = inflater2.inflate(resId, null)
        addDragView(selfView, left, top, right, bottom, isFixedSize, whitebg)
    }

    override fun onDeleteMoveLayout(identity: Int) {
        val count = mMoveLayoutList!!.size
        for (a in 0 until count) {
            if (mMoveLayoutList!![a].identity == identity) {
                //delete
                removeView(mMoveLayoutList!![a])
            }
        }
    }


    companion object {
        private const val TAG = "DragView"
    }

    override fun moving() {
        mListener?.moving()
    }

    override fun moveEnd(isDelete: Boolean, identity: Int) {
        var index = 0
        val count = mMoveLayoutList!!.size
        for (a in 0 until count) {
            if (mMoveLayoutList!![a].identity == identity) {
                index = a
            }
        }
        mListener?.moveEnd(isDelete, index)
    }

    override fun doubleClick(identity: Int) {
        var index = 0
        val count = mMoveLayoutList!!.size
        for (a in 0 until count) {
            if (mMoveLayoutList!![a].identity == identity) {
                index = a
            }
        }
        mListener?.doubleClick(index)
    }

    fun removeAll() {
        removeAllViews()
        mMoveLayoutList?.clear()
    }

    fun canMove(isCanMove: Boolean) {
        if (mMoveLayoutList != null && mMoveLayoutList?.isNotEmpty() == true) {
            for (moveLayout in mMoveLayoutList!!) {
                moveLayout.isCanMove(isCanMove)
            }
        }
    }

    interface OnMovingListener {
        fun moving()
        fun moveEnd(isDelete: Boolean, index: Int)
        fun doubleClick(index: Int)
    }

    private var mListener: OnMovingListener? = null

    fun setOnMovingListener(l: OnMovingListener?) {
        mListener = l
    }
}