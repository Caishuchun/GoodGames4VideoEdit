package com.fortune.zg.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.View
import com.fortune.zg.R

class VideoLoadingView(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {
    private var mWidth = 0
    private var mHeight = 0

    private val mPaint = Paint()
    private var space = 0f

    init {
        mPaint.isAntiAlias = true
        mPaint.color = resources.getColor(R.color.white_FFFFFF)
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = 0f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
    }

    /**
     * 开始动画
     */
    fun start() {
        this.visibility = View.VISIBLE
        mHandler.sendEmptyMessage(0)
    }

    @SuppressLint("HandlerLeak")
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (space <= mWidth / 2f) {
                space += mWidth / 50f
            } else {
                space = 0f
            }
            postInvalidate()
            sendEmptyMessageDelayed(0, 1)
        }
    }

    /**
     * 结束
     */
    fun cancel() {
        this.visibility = View.GONE
        mHandler.removeMessages(0)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(mWidth / 2f - space, 0f, mWidth / 2f + space, mHeight.toFloat(), mPaint)
    }
}