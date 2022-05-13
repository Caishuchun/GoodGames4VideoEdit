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
import kotlinx.android.synthetic.main.layout_top_tab_detail.view.*
import java.util.concurrent.TimeUnit

/**
 * 游戏详情顶部Tab
 */
@SuppressLint("CheckResult")
class TopTab4Detail(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    /**
     * 实现回调接口的方法
     *
     * @param onItemListener 回调接口的实例
     */
    fun setOnItemListener(onItemListener: OnBottomBarItemSelectListener) {
        mOnItemListener = onItemListener
    }

    fun setCurrentItem(index: Int) {
        changeItemStyle(index, false)
    }

    fun setCurrentItem(index: Int, needCallBack: Boolean) {
        changeItemStyle(index, needCallBack)
    }

    private var mOnItemListener: OnBottomBarItemSelectListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mView: View
    }

    init {
        //获取布局文件
        mView = LayoutInflater.from(context).inflate(R.layout.layout_top_tab_detail, this, true)

        RxView.clicks(mView.rl_top_game)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(0, true)
            }
        RxView.clicks(mView.rl_top_update)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(1, true)
            }
        RxView.clicks(mView.rl_top_detail)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(2, true)
            }
        RxView.clicks(mView.rl_top_common)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(3, true)
            }
    }

    private fun changeItemStyle(index: Int, needCallBack: Boolean) {
        if (needCallBack) {
            mOnItemListener?.setOnItemSelectListener(index)
        }
        mView.tv_top_game.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_top_update.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_top_detail.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_top_common.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)

        mView.view_top_game.visibility = View.INVISIBLE
        mView.view_top_update.visibility = View.INVISIBLE
        mView.view_top_detail.visibility = View.INVISIBLE
        mView.view_top_common.visibility = View.INVISIBLE

        when (index) {
            0 -> {
                mView.tv_top_game.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.view_top_game.visibility = View.VISIBLE
            }
            1 -> {
                mView.tv_top_update.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.view_top_update.visibility = View.VISIBLE
            }
            2 -> {
                mView.tv_top_detail.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.view_top_detail.visibility = View.VISIBLE
            }
            3 -> {
                mView.tv_top_common.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.view_top_common.visibility = View.VISIBLE
            }
        }
    }

}