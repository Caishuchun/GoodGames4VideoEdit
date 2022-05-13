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
import kotlinx.android.synthetic.main.layout_top_tab_my_games.view.*
import java.util.concurrent.TimeUnit

/**
 * 我的游戏,顶部Tab
 */
@SuppressLint("CheckResult")
class TopTab4Fav(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

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

    private var mOnItemListener: OnBottomBarItemSelectListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mView: View
    }

    init {
        //获取布局文件
        mView = LayoutInflater.from(context).inflate(R.layout.layout_top_tab_my_games, this, true)
        mView.tv_my_games_pc.text = "游戏"
        mView.tv_my_games_phone.text = "视频"
        RxView.clicks(mView.rl_my_games_pc)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(0)
            }
        RxView.clicks(mView.rl_my_games_phone)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(1)
            }
    }

    private fun changeItemStyle(index: Int) {
        mOnItemListener?.setOnItemSelectListener(index)
        mView.tv_my_games_pc.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_my_games_phone.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)

        mView.view_my_games_pc.visibility = View.INVISIBLE
        mView.view_my_games_phone.visibility = View.INVISIBLE

        when (index) {
            0 -> {
                mView.tv_my_games_pc.typeface =
                    Typeface.defaultFromStyle(Typeface.BOLD)
                mView.view_my_games_pc.visibility = View.VISIBLE
            }
            1 -> {
                mView.tv_my_games_phone.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.view_my_games_phone.visibility = View.VISIBLE
            }
        }
    }
}