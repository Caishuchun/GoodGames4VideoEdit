package com.fortune.zg.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import cn.jzvd.JZUtils
import cn.jzvd.Jzvd
import cn.jzvd.JzvdStd
import com.fortune.zg.R
import kotlinx.android.synthetic.main.layout_jzvd_mv.view.*
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

/**
 * 自适应宽高的视频播放器
 */
class MyJzvd4Mv : JzvdStd {

    private var mListener: PlayListener? = null
    private var landscapeWidth = 0
    private var landscapeHeight = 0
    private var portraitWidth = 0
    private var portraitHeight = 0
    private var isLandscape = false
    private var buttonSize= 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    override fun getLayoutId() = R.layout.layout_jzvd_mv

    override fun changeUiToComplete() {
        super.changeUiToComplete()
        mListener?.rePlay()
    }

    /**
     * 加载条和进度条显示问题
     */
    override fun setAllControlsVisiblity(
        topCon: Int,
        bottomCon: Int,
        startBtn: Int,
        loadingPro: Int,
        posterImg: Int,
        bottomPro: Int,
        retryLayout: Int
    ) {
        super.setAllControlsVisiblity(
            topCon,
            bottomCon,
            startBtn,
            loadingPro,
            posterImg,
            View.GONE,
            retryLayout
        )
        if (loadingPro == View.VISIBLE) {
            video_loading.start()
            pb_video.visibility = View.GONE
        } else {
            video_loading.cancel()
            pb_video.visibility = View.VISIBLE
        }
        loadingProgressBar.visibility = View.GONE
    }

    /**
     * 进度条更新
     */
    override fun onProgress(progress: Int, position: Long, duration: Long) {
        super.onProgress(progress, position, duration)
        pb_video.progress = progress
    }

    override fun dissmissControlView() {
        if (state != STATE_NORMAL && state != STATE_ERROR && state != STATE_AUTO_COMPLETE) {
            post {
                bottomContainer.visibility = INVISIBLE
                topContainer.visibility = INVISIBLE
                startButton.visibility = INVISIBLE
                if (screen != SCREEN_TINY) {
                    bottomProgressBar.visibility = VISIBLE
                }
            }
        }
    }

    /**
     * 设置按钮大小
     */
    fun setButtonSize(size: Float) {
        buttonSize = size.toInt()
    }

    override fun changeStartButtonSize(size: Int) {
        var lp = startButton.layoutParams
        lp.height = buttonSize
        lp.width = buttonSize
        lp = loadingProgressBar.layoutParams
        lp.height = size
        lp.width = size
    }

    /**
     * 修改开始/暂停按键
     */
    override fun updateStartImage() {
        when (state) {
            STATE_PLAYING -> {
                startButton.visibility = VISIBLE
                startButton.setImageResource(R.drawable.video_pause)
                replayTextView.visibility = GONE
            }
            STATE_ERROR -> {
                startButton.visibility = INVISIBLE
                replayTextView.visibility = GONE
            }
            STATE_AUTO_COMPLETE -> {
                startButton.visibility = VISIBLE
                startButton.setImageResource(cn.jzvd.R.drawable.jz_click_replay_selector)
                replayTextView.visibility = VISIBLE
            }
            STATE_NORMAL->{
                startButton.visibility = INVISIBLE
                startButton.setImageResource(R.drawable.video_play)
                replayTextView.visibility = GONE
            }
            else -> {
                startButton.setImageResource(R.drawable.video_play)
                replayTextView.visibility = GONE
            }
        }
    }

    override fun gotoFullscreen() {
        gotoFullscreenTime = System.currentTimeMillis()
        var vg = parent as ViewGroup
        jzvdContext = vg.context
        blockLayoutParams = layoutParams
        blockIndex = vg.indexOfChild(this)

        vg.removeView(this)
        cloneAJzvd(vg)
        CONTAINER_LIST.add(vg)
        vg = JZUtils.scanForActivity(jzvdContext).window.decorView as ViewGroup

        val fullLayout: ViewGroup.LayoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )

        vg.addView(this, fullLayout)
        val layoutParams = surface_container.layoutParams
        layoutParams.width = landscapeWidth
        layoutParams.height = landscapeHeight
        surface_container.layoutParams = layoutParams

        setScreenFullscreen()
        JZUtils.hideStatusBar(jzvdContext)
        JZUtils.setRequestedOrientation(jzvdContext, FULLSCREEN_ORIENTATION)
        JZUtils.hideSystemUI(jzvdContext) //华为手机和有虚拟键的手机全屏时可隐藏虚拟键 issue:1326
    }

    override fun cloneAJzvd(vg: ViewGroup) {
        try {
            val constructor = javaClass.getConstructor(
                Context::class.java
            ) as Constructor<Jzvd>
            val jzvd = constructor.newInstance(context)
            jzvd.id = id
            jzvd.minimumWidth = blockWidth
            jzvd.minimumHeight = blockHeight
            vg.addView(jzvd, blockIndex, blockLayoutParams)
            jzvd.setUp(jzDataSource.cloneMe(), SCREEN_NORMAL, mediaInterfaceClass)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        }
    }

    /**
     * 设置视频的宽高
     */
    fun setMvLayoutParams(
        width: Int,
        height: Int,
        screenWidth: Int,
        screenHeight: Int,
        isLandscape: Boolean
    ) {
        val layoutParams = surface_container.layoutParams
        var realWidth = 0
        var realHeight = 0
        this.isLandscape = isLandscape
        if (isLandscape) {
            //横屏
            realHeight = screenWidth
            val stepWidth = (realHeight.toFloat() / height * width).toInt()
            realWidth = Math.min(stepWidth, screenHeight)
        } else {
            //竖屏
            realWidth = screenWidth
            val stepHeight = (screenWidth.toFloat() / width * height).toInt()
            realHeight = Math.min(stepHeight, screenHeight)

            portraitWidth = realWidth
            portraitHeight = realHeight

            landscapeHeight = screenWidth
            val stepWidth = (landscapeHeight.toFloat() / height * width).toInt()
            landscapeWidth = Math.min(stepWidth, screenHeight)
        }
        layoutParams.width = realWidth
        layoutParams.height = realHeight
        surface_container.layoutParams = layoutParams
    }

    /**
     * 设置封面的宽高
     */
    fun setPosterParams(
        width: Int,
        height: Int,
        screenWidth: Int,
        screenHeight: Int,
        isLandscape: Boolean
    ) {
        val layoutParams = poster.layoutParams
        var realWidth = 0
        var realHeight = 0
        this.isLandscape = isLandscape
        if (isLandscape) {
            //横屏
            realHeight = screenWidth
            val stepWidth = (realHeight.toFloat() / height * width).toInt()
            realWidth = Math.min(stepWidth, screenHeight)
        } else {
            //竖屏
            realWidth = screenWidth
            val stepHeight = (screenWidth.toFloat() / width * height).toInt()
            realHeight = Math.min(stepHeight, screenHeight)

            portraitWidth = realWidth
            portraitHeight = realHeight

            landscapeHeight = screenWidth
            val stepWidth = (landscapeHeight.toFloat() / height * width).toInt()
            landscapeWidth = Math.min(stepWidth, screenHeight)
        }
        layoutParams.width = realWidth
        layoutParams.height = realHeight
        poster.layoutParams = layoutParams
    }


    fun setPlayListener(listener: PlayListener) {
        mListener = listener
    }

    interface PlayListener {
        fun rePlay()
    }

}
