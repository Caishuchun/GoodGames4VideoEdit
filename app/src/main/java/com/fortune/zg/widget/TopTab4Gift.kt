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
import kotlinx.android.synthetic.main.layout_top_tab_gift.view.*
import java.util.concurrent.TimeUnit

/**
 * 礼物积分界面顶部tab
 */
@SuppressLint("CheckResult")
class TopTab4Gift(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {


    /**
     * 实现回调接口的方法
     *
     * @param onItemListener 回调接口的实例
     */
    fun setOnItemListener(onItemListener: OnBottomBarItemSelectListener) {
        mOnItemListener = onItemListener
    }

    private var mOnItemListener: OnBottomBarItemSelectListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mView: View
    }

    private var hasDailyCheck = false
    private var hasWhitePiao = false
    private var hasInviteGift = false

    fun setDailyCheckPoint(isShowDailyCheck: Boolean) {
        hasDailyCheck = isShowDailyCheck
        mView.view_top_dailyCheck.visibility = if (hasDailyCheck) VISIBLE else GONE
    }

    fun setWhitePiaoPoint(isWhitePiao: Boolean) {
        hasWhitePiao = isWhitePiao
        mView.view_top_whitePiao.visibility = if (hasWhitePiao) VISIBLE else GONE
    }

    fun setInviteGiftPoint(isInviteGift: Boolean) {
        hasInviteGift = isInviteGift
        mView.view_top_inviteGift.visibility = if (hasInviteGift) VISIBLE else GONE
    }

    fun setCurrentItem(index: Int) {
        changeItemStyle(index)
    }

    init {
        //获取布局文件
        mView = LayoutInflater.from(context).inflate(R.layout.layout_top_tab_gift, this, true)

        RxView.clicks(mView.rl_top_dailyCheck)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(0)
            }
        RxView.clicks(mView.rl_top_whitePiao)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(1)
            }
        RxView.clicks(mView.rl_top_inviteGift)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItemStyle(2)
            }
    }

    private fun changeItemStyle(index: Int) {
        mOnItemListener?.setOnItemSelectListener(index)

        mView.tv_top_dailyCheck.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_top_whitePiao.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_top_inviteGift.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)

        mView.tv_top_dailyCheck.setTextColor(resources.getColor(R.color.black_1A241F))
        mView.tv_top_whitePiao.setTextColor(resources.getColor(R.color.black_1A241F))
        mView.tv_top_inviteGift.setTextColor(resources.getColor(R.color.black_1A241F))

        mView.rl_top_dailyCheck.setBackgroundResource(R.drawable.bg_gift_item_unselect)
        mView.rl_top_whitePiao.setBackgroundResource(R.drawable.bg_gift_item_unselect)
        mView.rl_top_inviteGift.setBackgroundResource(R.drawable.bg_gift_item_unselect)

        mView.view_top_dailyCheck.visibility = if (hasDailyCheck) View.VISIBLE else View.GONE
        mView.view_top_whitePiao.visibility = if (hasWhitePiao) View.VISIBLE else View.GONE
        mView.view_top_inviteGift.visibility = if (hasInviteGift) View.VISIBLE else View.GONE

        when (index) {
            0 -> {
//                hasDailyCheck = false
//                mView.view_top_dailyCheck.visibility = View.GONE
                mView.rl_top_dailyCheck.setBackgroundResource(R.drawable.bg_gift_item_selected)
                mView.tv_top_dailyCheck.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.tv_top_dailyCheck.setTextColor(resources.getColor(R.color.green_63C5AD))
            }
            1 -> {
//                hasWhitePiao = false
//                mView.view_top_whitePiao.visibility = View.GONE
                mView.rl_top_whitePiao.setBackgroundResource(R.drawable.bg_gift_item_selected)
                mView.tv_top_whitePiao.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.tv_top_whitePiao.setTextColor(resources.getColor(R.color.green_63C5AD))
            }
            2 -> {
//                hasInviteGift = false
//                mView.view_top_inviteGift.visibility = View.GONE
                mView.rl_top_inviteGift.setBackgroundResource(R.drawable.bg_gift_item_selected)
                mView.tv_top_inviteGift.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.tv_top_inviteGift.setTextColor(resources.getColor(R.color.green_63C5AD))
            }
        }
    }

}