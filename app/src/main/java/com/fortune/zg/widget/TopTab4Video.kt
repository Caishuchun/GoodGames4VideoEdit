package com.fortune.zg.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.fortune.zg.R
import com.fortune.zg.listener.OnBottomBarItemSelectListener
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.layout_top_tab_video.view.*
import java.util.concurrent.TimeUnit

/**
 * 我的游戏,顶部Tab
 */
@SuppressLint("CheckResult")
class TopTab4Video(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    /**
     * 实现回调接口的方法
     *
     * @param onItemListener 回调接口的实例
     */
    fun setOnItemListener(onItemListener: OnBottomBarItemSelectListener) {
        mOnItemListener = onItemListener
    }

    fun setCurrentItem(index: Int) {
        changeItemStyle(index)
    }

    /**
     * 有新视频发布了
     * @param type 0首页,1手游,2端游
     */
    fun haveNewVideo(type: Int) {
        when (type) {
            0 -> {
                mView.iv_video_all_point.visibility = View.VISIBLE
            }
            1 -> {
                mView.iv_video_all_point.visibility = View.VISIBLE
                mView.iv_video_phone_point.visibility = View.VISIBLE
            }
            2 -> {
                mView.iv_video_all_point.visibility = View.VISIBLE
                mView.iv_video_pc_point.visibility = View.VISIBLE
            }
        }
    }

    /**
     * 取消掉小红点
     */
    fun hideNewVideo() {
        mView.iv_video_all_point.visibility = View.GONE
        mView.iv_video_phone_point.visibility = View.GONE
        mView.iv_video_pc_point.visibility = View.GONE
    }

    private var mOnItemListener: OnBottomBarItemSelectListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mView: View
    }

    init {
        //获取布局文件
        mView = LayoutInflater.from(context).inflate(R.layout.layout_top_tab_video, this, true)
        RxView.clicks(mView.rl_video_all)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(0)
            }
        RxView.clicks(mView.rl_video_phone)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(1)
            }
        RxView.clicks(mView.rl_video_pc)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(2)
            }
    }

    private fun changeItemStyle(index: Int) {
        mOnItemListener?.setOnItemSelectListener(index)
        mView.tv_video_all.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_video_phone.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_video_pc.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)

        mView.view_video_all.visibility = View.INVISIBLE
        mView.view_video_phone.visibility = View.INVISIBLE
        mView.view_video_pc.visibility = View.INVISIBLE

        when (index) {
            0 -> {
                mView.tv_video_all.typeface =
                    Typeface.defaultFromStyle(Typeface.BOLD)
                mView.view_video_all.visibility = View.VISIBLE
            }
            1 -> {
                mView.tv_video_phone.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.view_video_phone.visibility = View.VISIBLE
            }
            2 -> {
                mView.tv_video_pc.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.view_video_pc.visibility = View.VISIBLE
            }
        }
    }
}