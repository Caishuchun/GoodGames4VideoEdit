package com.fortune.zg.issue

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.PhoneInfoUtils


/**
 * Created by Robert on 2017/6/20.
 */
class MoveLayout : RelativeLayout {
    private var dragDirection = CENTER
    private var lastX = 0
    private var lastY = 0
    private var screenWidth = 0
    private var screenHeight = 0
    private var oriLeft = 0
    private var oriRight = 0
    private var oriTop = 0
    private var oriBottom = 0

    /**
     * 标示此类的每个实例的id
     */
    var identity = 0

    /**
     * 触控区域设定
     */
    private val touchAreaLength = 100
    private var minHeight = 120
    private var minWidth = 180

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        val width = PhoneInfoUtils.getWidth(context as VideoMainActivity)
        val height = PhoneInfoUtils.getHeight(context)
        screenHeight = height //getResources().getDisplayMetrics().heightPixels - 40;
        screenWidth = width // getResources().getDisplayMetrics().widthPixels;
    }

    fun setCanMoveArea(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    fun setViewWidthHeight(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    fun setMinHeight(height: Int) {
        minHeight = height
        if (minHeight < touchAreaLength * 2) {
            minHeight = touchAreaLength * 2
        }
    }

    fun setMinWidth(width: Int) {
        minWidth = width
        if (minWidth < touchAreaLength * 3) {
            minWidth = touchAreaLength * 3
        }
    }

    private var mFixedSize = false
    fun setFixedSize(b: Boolean) {
        mFixedSize = b
    }

    private var mDeleteHeight = 0
    private var mDeleteWidth = 0
    private var isInDeleteArea = false
    fun setDeleteWidthHeight(width: Int, height: Int) {
        mDeleteWidth = screenWidth - width
        mDeleteHeight = height
    }

    private var canMove = true
    fun isCanMove(isCanMove:Boolean){
        canMove = isCanMove
    }

    private var firstClick = 0L

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(!canMove){
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val currentTimeMillis = System.currentTimeMillis()
                if (currentTimeMillis - firstClick < 500) {
                    mListener4Moving?.doubleClick(identity)
                }
                firstClick = currentTimeMillis
                //   Log.d(TAG, "onTouchEvent: down height="+ getHeight());
                oriLeft = left
                oriRight = right
                oriTop = top
                oriBottom = bottom
                lastY = event.rawY.toInt()
                lastX = event.rawX.toInt()
                dragDirection = getDirection(event.x.toInt(), event.y.toInt())
                LogUtils.d("onTouchEvent: dragDirection:$dragDirection")
            }
            MotionEvent.ACTION_UP -> {
                //      Log.d(TAG, "onTouchEvent: up");
                spotL = false
                spotT = false
                spotR = false
                spotB = false
                requestLayout()
                mDeleteView!!.visibility = INVISIBLE

                mListener4Moving?.moveEnd(false,identity)
            }
            MotionEvent.ACTION_MOVE -> {
                // Log.d(TAG, "onTouchEvent: move");
                val tempRawX = event.rawX.toInt()
                val tempRawY = event.rawY.toInt()
                val dx = tempRawX - lastX
                val dy = tempRawY - lastY
                lastX = tempRawX
                lastY = tempRawY
                when (dragDirection) {
                    LEFT -> left(dx)
                    RIGHT -> right(dx)
                    BOTTOM -> bottom(dy)
                    TOP -> top(dy)
                    CENTER -> center(dx, dy)
                    LEFT_TOP -> leftTop(dx)
                    RIGHT_TOP -> rightTop(dx)
                    LEFT_BOTTOM -> leftBottom(dx)
                    RIGHT_BOTTOM -> rightBottom(dx)
                }

                //new pos l t r b is set into oriLeft, oriTop, oriRight, oriBottom
                val lp = LayoutParams(oriRight - oriLeft, oriBottom - oriTop)
                lp.setMargins(oriLeft, oriTop, 0, 0)
                layoutParams = lp
            }
        }
        return super.onTouchEvent(event)
    }

    private fun rightBottom(dx: Int) {
        right(dx)
        bottom(dx / 5)
    }

    private fun leftBottom(dx: Int) {
        left(dx)
        bottom(-dx / 5)
    }

    private fun rightTop(dx: Int) {
        right(dx)
        top(-dx / 5)
    }

    private fun leftTop(dx: Int) {
        left(dx)
        top(dx / 5)
    }

    /**
     * 触摸点为中心->>移动
     */
    private fun center(dx: Int, dy: Int) {
        var left = left + dx
        var top = top + dy
        var right = right + dx
        var bottom = bottom + dy
        if (left < 0) {
            left = 0
            right = left + width
        }
        if (right > screenWidth) {
            right = screenWidth
            left = right - width
        }
        if (top < 0) {
            top = 0
            bottom = top + height
        }
        if (bottom > screenHeight) {
            bottom = screenHeight
            top = bottom - height
        }
        oriLeft = left
        oriTop = top
        oriRight = right
        oriBottom = bottom

        if (Math.abs(dx) > 10 || Math.abs(dy) > 10)
            mListener4Moving?.moving()

        //todo: show delete icon
        mDeleteView!!.visibility = VISIBLE
        //do delete
//        LogUtils.d(
//            "center: oriRight$oriRight  mDeleteWidth$mDeleteWidth  oriTop$oriTop  mDeleteHeightv$mDeleteHeight"
//        )
        if (!isInDeleteArea && oriRight > mDeleteWidth && oriTop < mDeleteHeight) { //delete

            if (mListener != null) {
                mListener!!.onDeleteMoveLayout(identity)
                mDeleteView!!.visibility = INVISIBLE
                isInDeleteArea = true //this object is deleted ,only one-time-using

                mListener4Moving?.moveEnd(true,identity)
            }
        }
    }

    /**
     * 触摸点为上边缘
     */
    private fun top(dy: Int) {
        oriTop += dy
        if (oriTop < 0) {
            oriTop = 0
        }
        if (oriBottom - oriTop < minHeight) {
            oriTop = oriBottom - minHeight
        }
    }

    /**
     * 触摸点为下边缘
     */
    private fun bottom(dy: Int) {
        oriBottom += dy
        if (oriBottom > screenHeight) {
            oriBottom = screenHeight
        }
        if (oriBottom - oriTop < minHeight) {
            oriBottom = minHeight + oriTop
        }
    }

    /**
     * 触摸点为右边缘
     */
    private fun right(dx: Int) {
        oriRight += dx
        if (oriRight > screenWidth) {
            oriRight = screenWidth
        }
        if (oriRight - oriLeft < minWidth) {
            oriRight = oriLeft + minWidth
        }
    }

    /**
     * 触摸点为左边缘
     */
    private fun left(dx: Int) {
        oriLeft += dx
        if (oriLeft < 0) {
            oriLeft = 0
        }
        if (oriRight - oriLeft < minWidth) {
            oriLeft = oriRight - minWidth
        }
    }

    private fun getDirection(x: Int, y: Int): String {
        val left = left
        val right = right
        val bottom = bottom
        val top = top

        if (x < touchAreaLength && y < touchAreaLength) {
            spotL = true
            spotT = true
            requestLayout()
            return LEFT_TOP
        }
        if (y < touchAreaLength && right - left - x < touchAreaLength) {
            spotR = true
            spotT = true
            requestLayout()
            return RIGHT_TOP
        }
        if (x < touchAreaLength && bottom - top - y < touchAreaLength) {
            spotL = true
            spotB = true
            requestLayout()
            return LEFT_BOTTOM
        }
        if (right - left - x < touchAreaLength && bottom - top - y < touchAreaLength) {
            spotR = true
            spotB = true
            requestLayout()
            return RIGHT_BOTTOM
        }
//        if (x < touchAreaLength) {
//            spotL = true
//            requestLayout()
//            return LEFT
//        }
//        if (y < touchAreaLength) {
//            spotT = true
//            requestLayout()
//            return TOP
//        }
//        if (right - left - x < touchAreaLength) {
//            spotR = true
//            requestLayout()
//            return RIGHT
//        }
//        if (bottom - top - y < touchAreaLength) {
//            spotB = true
//            requestLayout()
//            return BOTTOM
//        }
        return CENTER
    }

    private var spotL = false
    private var spotT = false
    private var spotR = false
    private var spotB = false
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        val rlt = getChildAt(0) as RelativeLayout
        val count = rlt.childCount
        for (a in 0 until count) {
            if (a == 1) {        //l
                if (spotL) rlt.getChildAt(a).visibility =
                    VISIBLE else rlt.getChildAt(a).visibility =
                    INVISIBLE
            } else if (a == 2) { //t
                if (spotT) rlt.getChildAt(a).visibility =
                    VISIBLE else rlt.getChildAt(a).visibility =
                    INVISIBLE
            } else if (a == 3) { //r
                if (spotR) rlt.getChildAt(a).visibility =
                    VISIBLE else rlt.getChildAt(a).visibility =
                    INVISIBLE
            } else if (a == 4) { //b
                if (spotB) rlt.getChildAt(a).visibility =
                    VISIBLE else rlt.getChildAt(a).visibility =
                    INVISIBLE
            }
            // Log.d(TAG, "onLayout: "+rlt.getChildAt(a).getClass().toString());
        }
    }

    //set the main delete area object (to change visibility)
    private var mDeleteView: View? = null
    fun setDeleteView(v: View?) {
        mDeleteView = v
    }

    //delete listener
    private var mListener: DeleteMoveLayout? = null

    interface DeleteMoveLayout {
        fun onDeleteMoveLayout(identity: Int)
    }

    fun setOnDeleteMoveLayout(l: DeleteMoveLayout?) {
        mListener = l
    }

    interface MovingListener {
        fun moving()
        fun moveEnd(isDelete: Boolean,identity:Int)
        fun doubleClick(identity:Int)
    }

    private var mListener4Moving: MovingListener? = null

    fun setOnMovingListener(l: MovingListener?) {
        mListener4Moving = l
    }

    companion object {
        private const val TOP = "top"
        private const val LEFT = "left"
        private const val BOTTOM = "bottom"
        private const val RIGHT = "right"
        private const val LEFT_TOP = "left_top"
        private const val RIGHT_TOP = "right_top"
        private const val LEFT_BOTTOM = "left_bottom"
        private const val RIGHT_BOTTOM = "right_bottom"
        private const val CENTER = "center"
        private const val TAG = "MoveLinearLayout"
    }
}