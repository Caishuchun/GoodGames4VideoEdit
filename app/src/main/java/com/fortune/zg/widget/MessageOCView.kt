package com.fortune.zg.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.fortune.zg.R

/**
 * 在视频界面发送弹幕的控件
 */
@SuppressLint("UseCompatLoadingForDrawables")
class MessageOCView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mListener: OnMessageClickListener? = null
    private val OPEN = 1
    private val CLOSE = 2

    private var bgPaint = Paint()
    private var textPaint = Paint()
    private var bitmapPaint = Paint()

    private var messageBitmap: Bitmap? = null
    private var messageCloseBitmap: Bitmap? = null
    private var messageOpenBitmap: Bitmap? = null

    private var mWidth = 0f
    private var mHeight = 0f
    private var mDuration = 500
    private var speed = 0f

    private var leftCircle = RectF(0f, 0f, 0f, 0f)
    private var rightCircle = RectF(0f, 0f, 0f, 0f)
    private var centerRect = RectF(0f, 0f, 0f, 0f)

    private var mOuterSrcRect4Message = Rect(0, 0, 0, 0)
    private var mOuterDstRect4Message = Rect(0, 0, 0, 0)
    private var mOuterSrcRect4MessageClose = Rect(0, 0, 0, 0)
    private var mOuterDstRect4MessageClose = Rect(0, 0, 0, 0)
    private var mOuterSrcRect4MessageOpen = Rect(0, 0, 0, 0)
    private var mOuterDstRect4MessageOpen = Rect(0, 0, 0, 0)

    private var baseline = 0f
    private var rectF4Text = RectF(0f, 0f, 0f, 0f)

    private var isRunning = false
    private var currentState = OPEN

    init {
        bgPaint.isAntiAlias = true
        bgPaint.color = resources.getColor(R.color.black_494A49)
        bgPaint.style = Paint.Style.FILL

        textPaint.isAntiAlias = true
        textPaint.color = resources.getColor(R.color.white_FFFFFF)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.style = Paint.Style.FILL
        textPaint.strokeWidth = 2f

        messageBitmap = (resources.getDrawable(R.mipmap.message) as BitmapDrawable).bitmap
        messageCloseBitmap =
            (resources.getDrawable(R.mipmap.message_close) as BitmapDrawable).bitmap
        messageOpenBitmap = (resources.getDrawable(R.mipmap.message_open) as BitmapDrawable).bitmap
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mHeight = if (h % 2 == 0) h.toFloat() else (h - 1).toFloat()
        mWidth = mHeight * 3.5f

        speed = mHeight * 2.5f / (mDuration / 50)
        changeView()
    }

    private fun changeView() {
        leftCircle = RectF(0f, 0f, mHeight, mHeight)
        centerRect =
            RectF(mHeight / 2f, 0f, (mWidth - mHeight / 2), mHeight)
        rightCircle =
            RectF(mWidth - mHeight, 0f, mWidth, mHeight)

        //第一个是绘制的图片区域,正常就是整个;第二个是绘制位置,看自己
        mOuterSrcRect4Message = Rect(0, 0, messageBitmap!!.width, messageBitmap!!.height)
        mOuterDstRect4Message = Rect(
            (mHeight * 1 / 4).toInt(),
            (mHeight * 1 / 4).toInt(),
            (mHeight * 3 / 4).toInt(),
            (mHeight * 3 / 4).toInt()
        )

        //文本
        textPaint.textSize = mHeight / 3f
        val fontMetrics = textPaint.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        rectF4Text = RectF(0f, 0f, mHeight * 3f, mHeight.toFloat())
        baseline = rectF4Text.centerY() + distance

        mOuterSrcRect4MessageClose = Rect(0, 0, messageBitmap!!.width, messageBitmap!!.height)
        mOuterDstRect4MessageClose = Rect(
            (mHeight * 1 / 4).toInt(),
            (mHeight * 1 / 4).toInt(),
            (mHeight * 3 / 4).toInt(),
            (mHeight * 3 / 4).toInt()
        )

        mOuterSrcRect4MessageOpen = Rect(0, 0, messageBitmap!!.width, messageBitmap!!.height)
        mOuterDstRect4MessageOpen = Rect(
            (mWidth - mHeight * 3 / 4).toInt(),
            (mHeight * 1 / 4).toInt(),
            (mWidth - mHeight * 1 / 4).toInt(),
            (mHeight * 3 / 4).toInt()
        )
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawArc(leftCircle, -90f, -180f, true, bgPaint)
        canvas?.drawRect(centerRect, bgPaint)
        canvas?.drawArc(rightCircle, -90f, 180f, true, bgPaint)

        when (mWidth) {
            mHeight * 3.5f -> {
                //全部展示的时候
                canvas?.drawBitmap(
                    messageBitmap!!,
                    mOuterSrcRect4Message,
                    mOuterDstRect4Message,
                    bitmapPaint
                )

                canvas?.drawLine(
                    mHeight,
                    mHeight * 13f / 36,
                    mHeight,
                    mHeight * 23f / 36,
                    textPaint
                )
                //TODO 这里留一个没有写到string.xml下的文字
                canvas?.drawText("发弹幕...", mHeight * 6f / 5, baseline, textPaint)

                canvas?.drawBitmap(
                    messageOpenBitmap!!,
                    mOuterSrcRect4MessageOpen,
                    mOuterDstRect4MessageOpen,
                    bitmapPaint
                )
            }
            mHeight -> {
                //缩进去的时候
                canvas?.drawBitmap(
                    messageCloseBitmap!!,
                    mOuterSrcRect4Message,
                    mOuterDstRect4Message,
                    bitmapPaint
                )
            }
            else -> {
                //缩放的时候
                when (currentState) {
                    OPEN -> {
                        canvas?.drawBitmap(
                            messageOpenBitmap!!,
                            mOuterSrcRect4MessageOpen,
                            mOuterDstRect4MessageOpen,
                            bitmapPaint
                        )
                    }
                    CLOSE -> {
                        canvas?.drawBitmap(
                            messageCloseBitmap!!,
                            mOuterSrcRect4MessageOpen,
                            mOuterDstRect4MessageOpen,
                            bitmapPaint
                        )
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val downX = event.x
                val downY = event.y
                if (downX in 0f..mHeight && downY in 0f..mHeight
                ) {
                    Log.d("=====", "开始打开了...")
                    if (!isRunning && currentState == CLOSE) {
                        currentState = OPEN
                        isRunning = true
                        mHandler.sendEmptyMessage(OPEN)
                        mListener?.open()
                    }
                } else if (downX in mWidth - mHeight..mWidth && downY in 0f..mHeight) {
                    Log.d("=====", "开始关闭了...")
                    if (!isRunning && currentState == OPEN) {
                        currentState = CLOSE
                        isRunning = true
                        mHandler.sendEmptyMessage(CLOSE)
                        mListener?.close()
                    }
                } else if (downX in mHeight - mHeight..mWidth && downY in 0f..mHeight) {
                    Log.d("=====", "开始发弹幕了...")
                    mListener?.show()
                }
            }
        }
        return true
    }

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                OPEN -> {
                    mWidth += speed
                    changeView()
                    postInvalidate()
                    if (mWidth >= mHeight * 3.5f) {
                        mWidth = mHeight * 3.5f
                        currentState = OPEN
                        isRunning = false
                        removeMessages(OPEN)
                    } else {
                        sendEmptyMessageDelayed(OPEN, 1)
                    }
                }
                CLOSE -> {
                    mWidth -= speed
                    changeView()
                    postInvalidate()
                    if (mWidth <= mHeight) {
                        mWidth = mHeight
                        currentState = CLOSE
                        isRunning = false
                        removeMessages(CLOSE)
                    } else {
                        sendEmptyMessageDelayed(CLOSE, 1)
                    }
                }
            }
        }
    }

    /**
     * 弹幕开关弹开
     */
    fun open(){
        if (!isRunning && currentState == CLOSE) {
            currentState = OPEN
            isRunning = true
            mHandler.sendEmptyMessage(OPEN)
            mListener?.open()
        }
    }

    /**
     * 弹幕开关缩回
     */
    fun close(){
        if (!isRunning && currentState == OPEN) {
            currentState = CLOSE
            isRunning = true
            mHandler.sendEmptyMessage(CLOSE)
            mListener?.close()
        }
    }

    fun setOnMessageClickListener(listener: OnMessageClickListener) {
        mListener = listener
    }

    interface OnMessageClickListener {
        fun close()
        fun open()
        fun show()
    }
}