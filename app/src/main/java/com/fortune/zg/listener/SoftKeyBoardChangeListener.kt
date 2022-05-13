package com.fortune.zg.listener

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.PopupWindow

class SoftKeyBoardChangeListener(private val mActivity: Activity) : PopupWindow(),
    OnGlobalLayoutListener {
    private var mView: View? = null
    private var mHeightMax = 0
    private var mListener: HeightListener? = null
    fun init(): SoftKeyBoardChangeListener {
        val view: View = mActivity.window.decorView
        view.post(Runnable {
            showAtLocation(view, Gravity.NO_GRAVITY, 0, 0)
        })
        return this
    }

    fun release() {
        dismiss()
        mListener = null
    }

    fun setHeightListener(listener: HeightListener?): SoftKeyBoardChangeListener {
        mListener = listener
        return this
    }

    interface HeightListener {
        fun onHeightChanged(height: Int)
    }

    override fun onGlobalLayout() {
        val rect = Rect()
        mView?.getWindowVisibleDisplayFrame(rect)
        if (rect.bottom > mHeightMax) {
            mHeightMax = rect.bottom
        }

        // 两者的差值就是键盘的高度
        val keyboardHeight = mHeightMax - rect.bottom
        if (mListener != null) {
            mListener!!.onHeightChanged(keyboardHeight)
        }
    }

    init {
        mView = View(mActivity)
        contentView = mView

        //监听全局layout
        mView?.viewTreeObserver?.addOnGlobalLayoutListener(this)
        setBackgroundDrawable(ColorDrawable(0))
        isOutsideTouchable = true

        //设置宽高
        width = 0
        height = WindowManager.LayoutParams.MATCH_PARENT

        //设置软键盘弹出方式
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        inputMethodMode = INPUT_METHOD_NEEDED
    }
}