package com.fortune.zg.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import com.fortune.zg.R
import com.fortune.zg.activity.GameDetailActivity
import com.fortune.zg.utils.ImageSpanUtils
import com.fortune.zg.utils.PhoneInfoUtils
import com.jakewharton.rxbinding2.view.RxView
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 弹幕广告的显示
 * 在首页一直播放"xxx领取了yyy"
 */
class BarrageView : androidx.appcompat.widget.AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private var mWidth = 0f
    private var realWidth = 0f
    private var translateAnimation: TranslateAnimation? = null
    private var mListener: OnAnimationEndListener? = null
    private var mTextClickListener: OnTextClickListener? = null

    private var timer1 = Timer()
    private var timer2 = Timer()

    init {
        isSingleLine = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = measuredWidth.toFloat()
        realWidth = paint.measureText("恭喜用户 110****1111 在《ABCDEFGABCDEFGABCDEFG》中成功抢到超级大礼包")
        setMeasuredDimension(realWidth.toInt(), measuredHeight)
    }

    override fun isFocused() = true

    @SuppressLint("CheckResult")
    fun setText(
        activity: Activity,
        userPhone: String,
        gameName: String?,
        gameId: Int?,
        gameCover: String?,
        gameBadge: String?,
        videoID: Int?,
        videoPos: Int?,
        videoName: String?,
        needSavePage: Boolean = true
    ) {

        val isVideo = null != videoID
        var msg = activity.getString(R.string.tips_msg)
        val replaceName = if (isVideo) {
            videoName
        } else {
            gameName
        } ?: "好服多多"
        msg = msg.replace("X", userPhone)
            .replace("Y", replaceName)
        msg = "  $msg"

        val builder = SpannableStringBuilder(msg)

        //字体颜色
        val userPhoneStart = msg.indexOf(userPhone)
        val userPhoneEnd = userPhoneStart + userPhone.length
        builder.setSpan(
            ForegroundColorSpan(Color.parseColor("#2EA992")),
            userPhoneStart,
            userPhoneEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val gameNameStart = msg.indexOf(replaceName) - 1
        val gameNameEnd = gameNameStart + replaceName.length + 2
        builder.setSpan(
            ForegroundColorSpan(Color.parseColor("#F03D3D")),
            gameNameStart,
            gameNameEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        //图片
        val bitmap = BitmapFactory.decodeResource(activity.resources, R.mipmap.gift_close)
        val width = PhoneInfoUtils.getWidth(activity)
        val scale = width.toFloat() / 360.toFloat()
        val imaSpan =
            CenteredImageSpan(activity, ImageSpanUtils.imageScale(bitmap, 12 * scale, 12 * scale))
        builder.setSpan(
            imaSpan,
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        text = builder
        translateAnimation = TranslateAnimation(mWidth, -realWidth, 0f, 0f)
        val duration = 9000L
        translateAnimation?.duration = duration
        translateAnimation?.fillAfter = true
        translateAnimation?.interpolator = LinearInterpolator()
        translateAnimation?.setAnimationListener(object : Animation.AnimationListener {
            @SuppressLint("CheckResult")
            override fun onAnimationStart(animation: Animation?) {
                timer1.cancel()
                timer2.cancel()
                timer1 = Timer()
                val timerTask1 = object : TimerTask() {
                    override fun run() {
                        mListener?.setOnAnimationEnd(duration / 5 * 2)
                    }
                }
                timer1.schedule(timerTask1, duration / 5 * 3)
                timer2 = Timer()
                val timerTask2 = object : TimerTask() {
                    override fun run() {
                        mListener?.setOnAnimationRealEnd()
                    }
                }
                timer2.schedule(timerTask2, duration)
            }

            override fun onAnimationEnd(animation: Animation?) {
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        startAnimation(translateAnimation)

        RxView.clicks(this)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (!isVideo) {
                    val intent = Intent(activity, GameDetailActivity::class.java)
                    intent.putExtra(GameDetailActivity.GAME_ID, gameId)
                    intent.putExtra(GameDetailActivity.NEED_SAVE_PAGE, needSavePage)
                    intent.putExtra(GameDetailActivity.GAME_COVER, gameCover)
                    intent.putExtra(GameDetailActivity.GAME_BADGE, gameBadge)
                    activity.startActivity(intent)
                }
                mTextClickListener?.setOnClick()
            }
    }

    fun setOnclick(listener: OnTextClickListener) {
        mTextClickListener = listener
    }

    fun stopAnim() {
        clearAnimation()
        text = ""
    }

    fun setOnAnimationEndListener(listener: OnAnimationEndListener) {
        mListener = listener
    }

    interface OnAnimationEndListener {
        fun setOnAnimationEnd(time: Long)
        fun setOnAnimationRealEnd()
    }

    interface OnTextClickListener {
        fun setOnClick()
    }
}