package com.fortune.zg.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.fortune.zg.R

/**
 * 视频下载控件+动画效果
 */
class DownloadMvView(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {

    private var mWidth = 0
    private var mHeight = 0
    private var mProgress = 0

    private val textPaint = Paint()
    private val circlePaint = Paint()
    private val progressPaint = Paint()
    private var isPause = false

    private var baseline = 0f
    private var rectF4Text = RectF(0f, 0f, 0f, 0f)
    private var rectF4Progress = RectF(0f, 0f, 0f, 0f)

    init {
        textPaint.isAntiAlias = true
        textPaint.color = resources.getColor(R.color.white_FFFFFF)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.style = Paint.Style.FILL
        textPaint.strokeWidth = 0f

        circlePaint.isAntiAlias = true
        circlePaint.color = resources.getColor(R.color.white_FFFFFF)
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeCap = Paint.Cap.ROUND

        progressPaint.isAntiAlias = true
        progressPaint.color = resources.getColor(R.color.green_2EC8AC)
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeCap = Paint.Cap.ROUND
    }

    /**
     * 设置进度
     * @param
     * 0    下载图标
     * 100  完成图标
     * else 下载进度更新
     */
    fun setProgress(progress: Int) {
        isPause = false
        mProgress = progress
        postInvalidate()
    }

    /**
     * 暂停
     */
    fun pause() {
        if (mProgress < 100) {
            isPause = true
            postInvalidate()
        }
    }

    fun getProgress() = mProgress

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h

        circlePaint.strokeWidth = mWidth / 24f
        progressPaint.strokeWidth = mWidth / 24f

        textPaint.textSize = mWidth / 3f
        val fontMetrics = textPaint.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        rectF4Text = RectF(0f, 0f, mWidth.toFloat(), mWidth.toFloat())
        rectF4Progress = RectF(
            0f + mWidth / 24,
            0f + mWidth / 24,
            mWidth.toFloat() - mWidth / 24,
            mWidth.toFloat() - mWidth / 24
        )
        baseline = rectF4Text.centerY() + distance
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (isPause) {
            //画圆环
            canvas?.drawCircle(mWidth / 2f, mWidth / 2f, mWidth / 2f - mWidth / 24, circlePaint)
            //画进度条
            canvas?.drawArc(
                rectF4Progress,
                -90f,
                (mProgress * 3.6).toFloat(),
                false,
                progressPaint
            )
            //再画两条竖线
            canvas?.drawLine(
                mWidth * 2f / 5,
                mWidth / 3f,
                mWidth * 2f / 5,
                mWidth / 3f * 2,
                circlePaint
            )
            canvas?.drawLine(
                mWidth * 3f / 5,
                mWidth / 3f,
                mWidth * 3f / 5,
                mWidth / 3f * 2,
                circlePaint
            )
        } else {
            when (mProgress) {
                0 -> {
                    //画圆环
                    canvas?.drawCircle(
                        mWidth / 2f,
                        mWidth / 2f,
                        mWidth / 2f - mWidth / 24,
                        circlePaint
                    )
                    //画箭头
                    canvas?.drawLine(
                        mWidth / 2f,
                        mWidth / 4f - mWidth / 24f,
                        mWidth / 2f,
                        mWidth / 4f * 3 - mWidth / 24f,
                        circlePaint
                    )
                    canvas?.drawLine(
                        mWidth / 2f - mWidth / 5f,
                        mWidth / 4f * 3 - mWidth / 5f - mWidth / 24f,
                        mWidth / 2f,
                        mWidth / 4f * 3 - mWidth / 24f,
                        circlePaint
                    )
                    canvas?.drawLine(
                        mWidth / 2f + mWidth / 5f,
                        mWidth / 4f * 3 - mWidth / 5f - mWidth / 24f,
                        mWidth / 2f,
                        mWidth / 4f * 3 - mWidth / 24f,
                        circlePaint
                    )
                    //画小横线
                    canvas?.drawLine(
                        mWidth / 2f - mWidth / 5f,
                        mWidth / 4f * 3 + mWidth / 24f,
                        mWidth / 2f + mWidth / 5f,
                        mWidth / 4f * 3 + mWidth / 24f,
                        circlePaint
                    )
                }
                100 -> {
                    //画圆环
                    canvas?.drawCircle(
                        mWidth / 2f,
                        mWidth / 2f,
                        mWidth / 2f - mWidth / 24,
                        progressPaint
                    )
                    //画对号
                    canvas?.drawLine(
                        mWidth / 4f,
                        mWidth / 2f,
                        mWidth / 5f * 2,
                        mWidth / 2f + mWidth / 6f,
                        progressPaint
                    )
                    canvas?.drawLine(
                        mWidth / 5f * 2,
                        mWidth / 2f + mWidth / 6f,
                        mWidth / 4f * 3,
                        mWidth / 3f,
                        progressPaint
                    )
                }
                else -> {
                    //画圆环
                    canvas?.drawCircle(
                        mWidth / 2f,
                        mWidth / 2f,
                        mWidth / 2f - mWidth / 24,
                        circlePaint
                    )
                    //画进度条
                    canvas?.drawArc(
                        rectF4Progress,
                        -90f,
                        (mProgress * 3.6).toFloat(),
                        false,
                        progressPaint
                    )
                    //画进度
                    canvas?.drawText("$mProgress%", rectF4Text.centerX(), baseline, textPaint)
                }
            }
        }
    }
}