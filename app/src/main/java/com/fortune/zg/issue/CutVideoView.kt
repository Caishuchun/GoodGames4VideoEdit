package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import com.fortune.zg.R
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.TimeUtils

class CutVideoView(activity: Context, attributeSet: AttributeSet) :
    View(activity, attributeSet) {

    private var mWidth = 0f
    private var mHeight = 0f
    private var horizontalLinePaint = Paint() //横线
    private var verticalLinePaint = Paint() //竖线
    private var shortLinePaint = Paint() //小短线
    private var rectanglePaint = Paint() //矩形
    private var cornerPaint = Paint() //四个内部小角角

    private val HORIZONTAL_LINE_WIDTH = 5f //横线宽度
    private val VERTICAL_LINE_WIDTH = 2f //竖线宽度
    private val RECTANGLE_WIDTH = 20f //矩形的宽度
    private val ROUND = 6f //圆角
    private val SPACE = 10f //上线边距
    private var SHORT_LINE_SIZE = 10f / 2 //小短线长度
    private var SHORT_LINE_WIDTH = 3f //小短线粗细
    private var CORNER = 8f // 4个角角的大小,矩形边长=圆直径

    private var currentStartLeft = 0f //当前截取左端左上角
    private var currentEndRight = 0f //当前截取右端右上角
    private var currentIndex = 0f //当前指针位置

    private var downPosition = -1 //按下点位置

    private var mDuration = 0L //总时长
    private var mVideoStartTime = 0L //视频起始时间
    private var mVideoEndTime = 0L //视频结束时间
    private var mVideoIndex = 0L //视频当前指针
    private var mListener: OnCutoutListener? = null

    private var touchType = TouchType.UNKNOWN

    enum class TouchType {
        DOWN, MOVE, UNKNOWN
    }

    init {
        horizontalLinePaint.isAntiAlias = true
        horizontalLinePaint.color = resources.getColor(R.color.white_FFFFFF)
        horizontalLinePaint.style = Paint.Style.STROKE

        verticalLinePaint.isAntiAlias = true
        verticalLinePaint.color = resources.getColor(R.color.white_FFFFFF)
        verticalLinePaint.style = Paint.Style.STROKE
        verticalLinePaint.strokeCap = Paint.Cap.ROUND

        shortLinePaint.isAntiAlias = true
        shortLinePaint.color = resources.getColor(R.color.green_2EC8AC)
        shortLinePaint.style = Paint.Style.STROKE
        shortLinePaint.strokeCap = Paint.Cap.ROUND

        rectanglePaint.isAntiAlias = true
        rectanglePaint.color = resources.getColor(R.color.white_FFFFFF)
        rectanglePaint.style = Paint.Style.FILL

        cornerPaint.isAntiAlias = true
        cornerPaint.color = resources.getColor(R.color.white_FFFFFF)
        cornerPaint.style = Paint.Style.FILL
    }

    /**
     * 当前选择类型
     */
    private var currentType = CutVideoType.CUT_VIDEO

    enum class CutVideoType {
        CUT_VIDEO, //裁剪视频模式
        SELECT_COVER //选择封面模式
    }

    /**
     * 0.设置控件模式
     */
    fun setType(type: CutVideoType) {
        this.currentType = type
    }

    /**
     * 1.设置视频总时长
     */
    fun setDuration(duration: Long) {
        mDuration = duration
        mVideoEndTime = mDuration
    }

    /**
     * 2.设置监听
     */
    fun setOnCutoutListener(listener: OnCutoutListener) {
        mListener = listener
    }

    /**
     * 3.设置当前播放指针
     */
    fun setIndex(index: Long) {
        currentIndex = if (index == -1L) {
            currentEndRight - formatSize(RECTANGLE_WIDTH) - formatSize(VERTICAL_LINE_WIDTH)
        } else {
            val fl = index.toFloat() / mDuration
            (mWidth - 2 * formatSize(RECTANGLE_WIDTH)) * fl + formatSize(RECTANGLE_WIDTH)
        }
        postInvalidate()
    }

    /**
     * 设置起始头
     */
    fun setStart(start: Long) {
        currentStartLeft = getIndex(start, true)
        LogUtils.d("=======111=start:$start,currentStartLeft:$currentStartLeft")
        postInvalidate()
    }

    /**
     * 设置结束尾
     */
    fun setEnd(end: Long) {
        currentEndRight = getIndex(end, false)
        LogUtils.d("=======111=end:$end,currentEndRight:$currentEndRight")
        postInvalidate()
    }

    /**
     * 重置
     */
    fun reset() {
        currentStartLeft = 0f
        currentEndRight = mWidth
        postInvalidate()
    }

    /**
     * 监听
     */
    interface OnCutoutListener {

        fun videoInterval(
            startTimeLong: Long,
            startTime: String,
            endTimeLong: Long,
            endTime: String
        )

        fun videoCurrentIndex(indexLong: Long, index: String)
    }

    /**
     * 格式化数值,保持一致性
     * @param size 输入360比例的尺寸大小
     * @return 返回正常像素比例下的大小
     */
    private fun formatSize(size: Float) = mWidth / 360 * size

    /**
     * 布局定下之后,确定一些参数
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w.toFloat()
        mHeight = h.toFloat()

        currentStartLeft = 0f
        currentEndRight = mWidth
        currentIndex = formatSize(RECTANGLE_WIDTH)

        horizontalLinePaint.strokeWidth = formatSize(HORIZONTAL_LINE_WIDTH)
        verticalLinePaint.strokeWidth = formatSize(VERTICAL_LINE_WIDTH)
        shortLinePaint.strokeWidth = formatSize(SHORT_LINE_WIDTH)
        cornerPaint.strokeWidth = formatSize(SHORT_LINE_WIDTH)
    }

    /**
     * 监听触摸事件
     * -1范围外
     * 0 左端
     * 1 中间
     * 2 右端
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var x = event?.x!!
        val y = event.y
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchType = TouchType.DOWN
                when (getPosition(x, y)) {
                    0 -> {
                        downPosition = 0
                        true
                    }
                    1 -> {
                        downPosition = 1
                        currentIndex = x
                        val currentTime = getCurrentTime(x)
                        if (currentTime != mVideoIndex) {
                            mListener?.videoCurrentIndex(
                                currentTime,
                                TimeUtils.formatHms(currentTime.toInt() / 1000)
                            )
                        }
                        mVideoIndex = currentTime
                        postInvalidate()
                        true
                    }
                    2 -> {
                        downPosition = 2
                        true
                    }
                    else -> {
                        downPosition = -1
                        false
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
//                LogUtils.d("位置_move:x = $x,y = $y")
                touchType = TouchType.MOVE
                when (downPosition) {
                    0 -> {
                        if (x <= 0f) {
                            x = 0f
                        } else if (x >= currentEndRight - 2 * formatSize(RECTANGLE_WIDTH) - formatSize(
                                VERTICAL_LINE_WIDTH
                            )
                        ) {
                            x = currentEndRight - 2 * formatSize(RECTANGLE_WIDTH) - formatSize(
                                VERTICAL_LINE_WIDTH
                            )
                        }
                        val currentTime = getCurrentTime(x + formatSize(RECTANGLE_WIDTH))
                        if (currentTime != mVideoStartTime) {
                            mListener?.videoCurrentIndex(
                                currentTime,
                                TimeUtils.formatHms(currentTime.toInt() / 1000)
                            )
                        }
                        mVideoStartTime = currentTime
                        if (currentIndex < currentStartLeft + formatSize(RECTANGLE_WIDTH)) {
                            currentIndex = x + formatSize(RECTANGLE_WIDTH)
                        }
                        currentStartLeft = x
                        postInvalidate()
                    }
                    1 -> {
                        if (x <= currentStartLeft + formatSize(RECTANGLE_WIDTH)) {
                            x = currentStartLeft + formatSize(RECTANGLE_WIDTH)
                        } else if (x >= currentEndRight - formatSize(RECTANGLE_WIDTH) - formatSize(
                                VERTICAL_LINE_WIDTH
                            )
                        ) {
                            x = currentEndRight - formatSize(RECTANGLE_WIDTH) - formatSize(
                                VERTICAL_LINE_WIDTH
                            )
                        }
                        val currentTime = when (x) {
                            formatSize(RECTANGLE_WIDTH) -> {
                                0L
                            }
                            mWidth - formatSize(RECTANGLE_WIDTH) - formatSize(
                                VERTICAL_LINE_WIDTH
                            ) -> {
                                mDuration
                            }
                            else -> {
                                getCurrentTime(x)
                            }
                        }
                        if (currentTime != mVideoIndex) {
                            mListener?.videoCurrentIndex(
                                currentTime,
                                TimeUtils.formatHms(currentTime.toInt() / 1000)
                            )
                        }
                        mVideoIndex = currentTime
                        currentIndex = x
                        postInvalidate()
                    }
                    2 -> {
                        if (x >= mWidth) {
                            x = mWidth
                        } else if (x <= currentStartLeft + 2 * formatSize(RECTANGLE_WIDTH) + formatSize(
                                VERTICAL_LINE_WIDTH
                            )
                        ) {
                            x = currentStartLeft + 2 * formatSize(RECTANGLE_WIDTH) + formatSize(
                                VERTICAL_LINE_WIDTH
                            )
                        }
                        val currentTime = getCurrentTime(x - formatSize(RECTANGLE_WIDTH))
                        if (currentTime != mVideoEndTime) {
                            mListener?.videoCurrentIndex(
                                currentTime,
                                TimeUtils.formatHms(currentTime.toInt() / 1000)
                            )
                        }
                        mVideoEndTime = currentTime
                        if (currentIndex > currentEndRight - formatSize(RECTANGLE_WIDTH) - formatSize(
                                VERTICAL_LINE_WIDTH
                            )
                        ) {
                            currentIndex =
                                x - formatSize(RECTANGLE_WIDTH) - formatSize(VERTICAL_LINE_WIDTH)
                        }
                        currentEndRight = x
                        postInvalidate()
                    }
                    else -> {
                    }
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                mListener?.videoInterval(
                    mVideoStartTime,
                    TimeUtils.formatHms(mVideoStartTime.toInt() / 1000),
                    mVideoEndTime,
                    TimeUtils.formatHms(mVideoEndTime.toInt() / 1000)
                )
                true
            }
            else -> {
                touchType = TouchType.UNKNOWN
                false
            }
        }
    }

    /**
     * 根据触摸点确定当前时间
     */
    private fun getCurrentTime(x: Float) = when {
        x < formatSize(RECTANGLE_WIDTH) -> {
            0L
        }
        x > mWidth - formatSize(RECTANGLE_WIDTH) -> {
            mDuration
        }
        else -> {
            ((x - formatSize(RECTANGLE_WIDTH)) / (mWidth - 2 * formatSize(RECTANGLE_WIDTH)) * mDuration).toLong()
        }
    }

    /**
     * 根据时间获取当前位置
     */
    private fun getIndex(time: Long, isStart: Boolean) = when (time) {
        0L -> {
            0f
        }
        mDuration -> {
            mWidth
        }
        else -> {
            if (isStart) {
                time * (mWidth - 2 * formatSize(RECTANGLE_WIDTH)) / mDuration
            } else {
                time * (mWidth - 2 * formatSize(RECTANGLE_WIDTH)) / mDuration + 2 * formatSize(
                    RECTANGLE_WIDTH
                )
            }
        }
    }

    /**
     * 获取当前按下点的位置
     * @return
     * -1 范围外
     * 0 左端
     * 1 中间
     * 2 右端
     */
    private fun getPosition(x: Float, y: Float) =
        if (x >= currentStartLeft && x <= currentStartLeft + formatSize(RECTANGLE_WIDTH)
            && y >= 0 && y <= mHeight
        ) {
            //按下点在左端滑动位置
            if (currentType == CutVideoType.CUT_VIDEO) {
                0
            } else {
                -1
            }
        } else if (x >= currentStartLeft + formatSize(RECTANGLE_WIDTH)
            && x <= currentEndRight - formatSize(RECTANGLE_WIDTH)
            && y >= 0 && y <= mHeight
        ) {
            //按下点在中间
            if (currentType == CutVideoType.SELECT_COVER) {
                1
            } else {
                -1
            }
        } else if (x >= currentEndRight - formatSize(RECTANGLE_WIDTH) && x <= currentEndRight
            && y >= 0 && y <= mHeight
        ) {
            //按下点在右端滑动位置
            if (currentType == CutVideoType.CUT_VIDEO) {
                2
            } else {
                -1
            }
        } else {
            -1
        }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //上边框
        canvas?.drawLine(
            currentStartLeft + formatSize(ROUND),
            0f + formatSize(SPACE + HORIZONTAL_LINE_WIDTH / 2),
            currentEndRight - formatSize(ROUND),
            0f + formatSize(SPACE + HORIZONTAL_LINE_WIDTH / 2),
            horizontalLinePaint
        )

        //下边框
        canvas?.drawLine(
            currentStartLeft + formatSize(ROUND),
            mHeight - formatSize(SPACE + HORIZONTAL_LINE_WIDTH / 2),
            currentEndRight - formatSize(ROUND),
            mHeight - formatSize(SPACE + HORIZONTAL_LINE_WIDTH / 2),
            horizontalLinePaint
        )

        //竖线
        if (currentType == CutVideoType.SELECT_COVER) {
            canvas?.drawLine(
                currentIndex + VERTICAL_LINE_WIDTH * 2f,
                0f + formatSize(1f),
                currentIndex + VERTICAL_LINE_WIDTH * 2f,
                mHeight - formatSize(1f),
                verticalLinePaint
            )
        }
        //左边框
        canvas?.drawRoundRect(
            currentStartLeft,
            0f + formatSize(SPACE),
            currentStartLeft + formatSize(RECTANGLE_WIDTH),
            mHeight - formatSize(SPACE),
            formatSize(ROUND),
            formatSize(ROUND),
            rectanglePaint
        )

        //左边框小短线
        canvas?.drawLine(
            currentStartLeft + formatSize(RECTANGLE_WIDTH / 2),
            mHeight / 2 - formatSize(SHORT_LINE_SIZE),
            currentStartLeft + formatSize(RECTANGLE_WIDTH / 2),
            mHeight / 2 + formatSize(SHORT_LINE_SIZE),
            shortLinePaint
        )

        //左上角区域
        val pathLeftTop = Path()
        val rectLeftTop = RectF(
            currentStartLeft + formatSize(RECTANGLE_WIDTH),
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH),
            currentStartLeft + formatSize(RECTANGLE_WIDTH) + formatSize(CORNER) * 2,
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH) + formatSize(CORNER) * 2
        )
        pathLeftTop.moveTo(
            currentStartLeft + formatSize(RECTANGLE_WIDTH),
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathLeftTop.lineTo(
            currentStartLeft + formatSize(RECTANGLE_WIDTH),
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH) + formatSize(CORNER)
        )
        pathLeftTop.addArc(
            rectLeftTop,
            -180f,
            90f
        )
        pathLeftTop.lineTo(
            currentStartLeft + formatSize(RECTANGLE_WIDTH),
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathLeftTop.close()
        canvas?.drawPath(pathLeftTop, cornerPaint)

        //左下角区域
        val pathLeftBottom = Path()
        val rectLeftBottom = RectF(
            currentStartLeft + formatSize(RECTANGLE_WIDTH),
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH) - formatSize(CORNER) * 2,
            currentStartLeft + formatSize(RECTANGLE_WIDTH) + formatSize(CORNER) * 2,
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathLeftBottom.moveTo(
            currentStartLeft + formatSize(RECTANGLE_WIDTH),
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathLeftBottom.lineTo(
            currentStartLeft + formatSize(RECTANGLE_WIDTH) + formatSize(CORNER),
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathLeftBottom.addArc(
            rectLeftBottom,
            90f,
            90f
        )
        pathLeftBottom.lineTo(
            currentStartLeft + formatSize(RECTANGLE_WIDTH),
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathLeftBottom.close()
        canvas?.drawPath(pathLeftBottom, cornerPaint)

        //右边框
        canvas?.drawRoundRect(
            currentEndRight - formatSize(RECTANGLE_WIDTH),
            0f + formatSize(SPACE),
            currentEndRight,
            mHeight - formatSize(SPACE),
            formatSize(ROUND),
            formatSize(ROUND),
            rectanglePaint
        )

        //右上角区域
        val pathRightTop = Path()
        val rectRightTop = RectF(
            currentEndRight - formatSize(RECTANGLE_WIDTH) - formatSize(CORNER) * 2,
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH),
            currentEndRight - formatSize(RECTANGLE_WIDTH),
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH) + formatSize(CORNER) * 2
        )
        pathRightTop.moveTo(
            currentEndRight - formatSize(RECTANGLE_WIDTH),
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathRightTop.lineTo(
            currentEndRight - formatSize(RECTANGLE_WIDTH),
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH) + formatSize(CORNER)
        )
        pathRightTop.addArc(
            rectRightTop,
            0f,
            -90f
        )
        pathRightTop.lineTo(
            currentEndRight - formatSize(RECTANGLE_WIDTH),
            0f + formatSize(SPACE) + formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathRightTop.close()
        canvas?.drawPath(pathRightTop, cornerPaint)

        //右下角区域
        val pathRightBottom = Path()
        val rectRightBottom = RectF(
            currentEndRight - formatSize(RECTANGLE_WIDTH) - formatSize(CORNER) * 2,
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH) - formatSize(CORNER) * 2,
            currentEndRight - formatSize(RECTANGLE_WIDTH),
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathRightBottom.moveTo(
            currentEndRight - formatSize(RECTANGLE_WIDTH),
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathRightBottom.lineTo(
            currentEndRight - formatSize(RECTANGLE_WIDTH),
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH) - formatSize(CORNER)
        )
        pathRightBottom.addArc(
            rectRightBottom,
            0f,
            90f
        )
        pathRightBottom.lineTo(
            currentEndRight - formatSize(RECTANGLE_WIDTH),
            mHeight - formatSize(SPACE) - formatSize(HORIZONTAL_LINE_WIDTH)
        )
        pathRightBottom.close()
        canvas?.drawPath(pathRightBottom, cornerPaint)

        //右边框小短线
        canvas?.drawLine(
            currentEndRight - formatSize(RECTANGLE_WIDTH / 2),
            mHeight / 2 - formatSize(SHORT_LINE_SIZE),
            currentEndRight - formatSize(RECTANGLE_WIDTH / 2),
            mHeight / 2 + formatSize(SHORT_LINE_SIZE),
            shortLinePaint
        )
    }

}