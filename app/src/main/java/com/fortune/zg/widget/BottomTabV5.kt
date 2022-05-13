package com.fortune.zg.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.fortune.zg.R
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.listener.OnBottomBarItemSelectListener
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.LoginUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.layout_bottom_tab_v5.view.*
import java.util.concurrent.TimeUnit

/**
 * 首页的底部tab_V5
 */
@SuppressLint("CheckResult")
class BottomTabV5(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private var currentPos = 0

    /**
     * 实现回调接口的方法
     *
     * @param onItemListener 回调接口的实例
     */
    fun setOnItemListener(onItemListener: OnBottomBarItemSelectListener) {
        mOnItemListener = onItemListener
    }

    fun setCurrentItem(index: Int) {
        currentPos = index
        changeItemStyle(index)
    }

    /**
     * 有新视频发布了
     */
    fun haveNewVideo() {
        mView.iv_bottomTabV5_home_normal.visibility = View.VISIBLE
        mView.iv_bottomTabV5_home_focus.visibility = View.VISIBLE
    }

    /**
     * 取消掉小红点
     */
    fun hideNewVideo() {
        mView.iv_bottomTabV5_home_normal.visibility = View.GONE
        mView.iv_bottomTabV5_home_focus.visibility = View.GONE
    }

    private var mOnItemListener: OnBottomBarItemSelectListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mView: View
    }

    init {
        //获取布局文件
        mView = LayoutInflater.from(context).inflate(R.layout.layout_bottom_tab_v5, this, true)
        RxView.clicks(mView.rl_bottomTabV5_home)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                MainActivityV5.mainPage = MainActivityV5.MainPage.MAIN
                if (currentPos != 0) {
                    currentPos = 0
                    changeItemStyle(0)
                }
            }
        RxView.clicks(mView.rl_bottomTabV5_phone)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                MainActivityV5.mainPage = MainActivityV5.MainPage.PHONE
                if (currentPos != 1) {
                    currentPos = 1
                    changeItemStyle(1)
                }
            }
        RxView.clicks(mView.rl_bottomTabV5_issue)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            )
            .subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    currentPos = 2
                    changeItemStyle(2)
                } else {
                    MainActivityV5.getInstance()?.let { it1 -> LoginUtils.toQuickLogin(it1) }
                }
            }
        RxView.clicks(mView.rl_bottomTabV5_pc)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                MainActivityV5.mainPage = MainActivityV5.MainPage.PC
                if (currentPos != 3) {
                    currentPos = 3
                    changeItemStyle(3)
                }
            }
        RxView.clicks(mView.rl_bottomTabV5_mine)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                MainActivityV5.mainPage = MainActivityV5.MainPage.MINE
                MobclickAgent.onEvent(
                    context,
                    "video_view",
                    "video_button"
                )
                currentPos = 4
                changeItemStyle(4)
            }
    }

    private fun changeItemStyle(index: Int) {
        mOnItemListener?.setOnItemSelectListener(index)
        if (index == 2) {
            return
        }
        mView.rl_bottomTabV5_home_normal.visibility = View.VISIBLE
        mView.rl_bottomTabV5_home_focus.visibility = View.GONE
        mView.tv_bottomTabV5_home.setTextColor(resources.getColor(R.color.black_878787))

        mView.iv_bottomTabV5_phone_normal.visibility = View.VISIBLE
        mView.iv_bottomTabV5_phone_focus.visibility = View.GONE
        mView.tv_bottomTabV5_phone.setTextColor(resources.getColor(R.color.black_878787))

        mView.iv_bottomTabV5_pc_normal.visibility = View.VISIBLE
        mView.iv_bottomTabV5_pc_focus.visibility = View.GONE
        mView.tv_bottomTabV5_pc.setTextColor(resources.getColor(R.color.black_878787))

        mView.iv_bottomTabV5_mine_normal.visibility = View.VISIBLE
        mView.iv_bottomTabV5_mine_focus.visibility = View.GONE
        mView.tv_bottomTabV5_mine.setTextColor(resources.getColor(R.color.black_878787))
        when (index) {
            0 -> {
                mView.rl_bottomTabV5_home_normal.visibility = View.GONE
                mView.rl_bottomTabV5_home_focus.visibility = View.VISIBLE
                mView.tv_bottomTabV5_home.setTextColor(resources.getColor(R.color.green_63C5AD))
                mView.view_bottomTabV5_bg.setBackgroundColor(resources.getColor(R.color.black_000000))
            }
            1 -> {
                mView.iv_bottomTabV5_phone_normal.visibility = View.GONE
                mView.iv_bottomTabV5_phone_focus.visibility = View.VISIBLE
                mView.tv_bottomTabV5_phone.setTextColor(resources.getColor(R.color.green_63C5AD))
                mView.view_bottomTabV5_bg.setBackgroundColor(resources.getColor(R.color.white_FFFFFF))
            }
            2 -> {
            }
            3 -> {
                mView.iv_bottomTabV5_pc_normal.visibility = View.GONE
                mView.iv_bottomTabV5_pc_focus.visibility = View.VISIBLE
                mView.tv_bottomTabV5_pc.setTextColor(resources.getColor(R.color.green_63C5AD))
                mView.view_bottomTabV5_bg.setBackgroundColor(resources.getColor(R.color.white_FFFFFF))
            }
            4 -> {
                mView.iv_bottomTabV5_mine_normal.visibility = View.GONE
                mView.iv_bottomTabV5_mine_focus.visibility = View.VISIBLE
                mView.tv_bottomTabV5_mine.setTextColor(resources.getColor(R.color.green_63C5AD))
                mView.view_bottomTabV5_bg.setBackgroundColor(resources.getColor(R.color.white_FFFFFF))
            }
        }
    }
}