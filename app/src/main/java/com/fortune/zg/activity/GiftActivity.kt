package com.fortune.zg.activity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.core.animation.addPauseListener
import androidx.fragment.app.Fragment
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.DailyCheckBean
import com.fortune.zg.bean.UserInfoBean
import com.fortune.zg.event.GiftShowPoint
import com.fortune.zg.event.GiftShowState
import com.fortune.zg.fragment.DailyCheckFragment
import com.fortune.zg.fragment.InviteGiftFragment
import com.fortune.zg.fragment.WhitePiaoFragment
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.listener.OnBottomBarItemSelectListener
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_gift.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class GiftActivity : BaseActivity() {

    private var currentFragment: Fragment? = null
    private var dailyCheckFragment: DailyCheckFragment? = null
    private var whitePiaoFragment: WhitePiaoFragment? = null
    private var inviteGiftFragment: InviteGiftFragment? = null

    private var doubleGetObservable: Disposable? = null

    private var isClickDoubleGet = false
    private var mData: DailyCheckBean.DataBean? = null
    private var df = DecimalFormat("#0.00")

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: GiftActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_gift

    override fun doSomething() {
        instance = this
        EventBus.getDefault().register(this)
        dailyCheckFragment = DailyCheckFragment.newInstance()
        whitePiaoFragment = WhitePiaoFragment.newInstance()
        inviteGiftFragment = InviteGiftFragment.newInstance()
        initView()
    }

    //为了不保存Fragment,直接清掉
    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        if (UserInfoBean.getData() != null) {
            tv_gift_level.text = "Lv.${UserInfoBean.getData()?.user_level}"
            tv_gift_integral.text = "${UserInfoBean.getData()?.user_integral}"
            pb_gift_level.max = UserInfoBean.getData()?.user_upgrade_exp!!
            pb_gift_level.progress = UserInfoBean.getData()?.user_now_exp!!
            if (UserInfoBean.getData()?.user_upgrade_exp!! == 0) {
                tv_gift_level_percent.text = "0.00%"
            } else {
                tv_gift_level_percent.text =
                    "${df.format(UserInfoBean.getData()?.user_now_exp!! * 100f / UserInfoBean.getData()?.user_upgrade_exp!!)}%"
            }
        }
        supportFragmentManager.beginTransaction()
            .add(R.id.fl_gift, dailyCheckFragment!!)
            .add(R.id.fl_gift, whitePiaoFragment!!)
            .add(R.id.fl_gift, inviteGiftFragment!!)
            .commit()

        RxView.clicks(iv_gift_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        tt_gift.setCurrentItem(0)
        toChangeFragment(0)
        tt_gift.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                toChangeFragment(index)
            }
        })
    }

    private fun toChangeFragment(index: Int) {
        hideAll()
        when (index) {
            0 -> {
                currentFragment = dailyCheckFragment
                supportFragmentManager.beginTransaction()
                    .show(currentFragment!!)
                    .commit()
            }
            1 -> {
                currentFragment = whitePiaoFragment
                supportFragmentManager.beginTransaction()
                    .show(currentFragment!!)
                    .commit()
            }
            2 -> {
                currentFragment = inviteGiftFragment
                supportFragmentManager.beginTransaction()
                    .show(currentFragment!!)
                    .commit()
            }
        }
    }

    private fun hideAll() {
        supportFragmentManager.beginTransaction()
            .hide(dailyCheckFragment!!)
            .hide(whitePiaoFragment!!)
            .hide(inviteGiftFragment!!)
            .commit()
    }

    @SuppressLint("SetTextI18n")
    @Subscribe
    fun toChangeExperienceAndIntegral(data: DailyCheckBean.DataBean) {
        if (data.is_upgrade == 0) {
            //如果不升级的话
            val newIntegral = data.user_integral
            if (newIntegral != tv_gift_integral.text.toString().trim().toInt()) {
                //领取积分
                val oldIntegral = tv_gift_integral.text.toString().trim().toInt()
                val animator =
                    ValueAnimator.ofInt(oldIntegral, newIntegral!!)
                animator.duration = 1000
                animator.interpolator = LinearInterpolator()
                animator.addUpdateListener {
                    tv_gift_integral.text = animator.animatedValue.toString()
                }
                animator.start()
                toChangeUI(data)
            }
            val oldProgress = pb_gift_level.progress
            if (oldProgress != data.user_now_exp!!) {
                //领取经验
                val animator =
                    ValueAnimator.ofInt(
                        oldProgress,
                        data.user_now_exp!!
                    )
                animator.duration = 1000
                animator.interpolator = LinearInterpolator()
                animator.addUpdateListener {
                    pb_gift_level.progress = animator.animatedValue.toString().toInt()
                    tv_gift_level_percent.text =
                        "${df.format(pb_gift_level.progress * 100f / data.user_upgrade_exp!!)}%"
                }
                animator.start()
                toChangeUI(data)
            }
        } else {
            //先将正常的流程走完,再研究是否double
            toUpdate(data, true)
        }
    }

    /**
     * 双倍领取金币
     */
    private fun toDoubleGet() {
        DialogUtils.showBeautifulDialog(this)
        val doubleGet = RetrofitUtils.builder().doubleGet()
        doubleGetObservable = doubleGet.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LevelUpdateDialog.dismissDialog()
                DialogUtils.dismissLoading()
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            it.getData()?.let { data -> toUpdate(data, false) }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 升一下等级
     * 可能是普通领取,也可能是分享后领取(double)
     */
    @SuppressLint("SetTextI18n")
    private fun toUpdate(data: DailyCheckBean.DataBean, isFirst: Boolean) {
        if (isFirst) {
            //第一阶段,首先要把之前的等级升满再说
            val oldProgress = pb_gift_level.progress
            val oldMax = pb_gift_level.max
            val animator = ValueAnimator.ofInt(
                oldProgress,
                oldMax
            )
            animator.duration = if (data.user_now_exp == 0) 1000 else 500 //是不是刚好升满
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                pb_gift_level.progress = animator.animatedValue.toString().toInt()
                tv_gift_level_percent.text =
                    "${df.format(pb_gift_level.progress * 100f / pb_gift_level.max)}%"
                if (pb_gift_level.progress == oldMax) {
                    if (data.user_now_exp == 0) {
                        //刚刚满级
                        tv_gift_level.text = "Lv.${data.user_level}"
                        pb_gift_level.max = data.user_upgrade_exp!!
                        pb_gift_level.progress = 0
                        tv_gift_level_percent.text = "0.00%"
                        toShowDoubleDialog(data)
                    } else {
                        //不仅仅升满级,而且还有盈余
                        tv_gift_level.text = "Lv.${data.user_level}"
                        pb_gift_level.max = data.user_upgrade_exp!!
                        val animator2 = ValueAnimator.ofInt(
                            0,
                            data.user_now_exp!!
                        )
                        animator2.duration = 500
                        animator2.interpolator = LinearInterpolator()
                        animator2.addUpdateListener {
                            pb_gift_level.progress = animator2.animatedValue.toString().toInt()
                            tv_gift_level_percent.text =
                                "${df.format(pb_gift_level.progress * 100f / pb_gift_level.max)}%"
                            if (pb_gift_level.progress == data.user_now_exp) {
                                //涨停了
                                toShowDoubleDialog(data)
                            }
                        }
                        animator2.start()
                    }
                }
            }
            animator.start()

            val oldIntegral = tv_gift_integral.text.toString().trim().toInt()
            val animator3 =
                ValueAnimator.ofInt(
                    oldIntegral,
                    data.user_integral!! - data.give_integral!!
                )
            animator3.duration = 1000
            animator3.interpolator = LinearInterpolator()
            animator3.addUpdateListener {
                tv_gift_integral.text = animator3.animatedValue.toString()
            }
            animator3.start()
            toChangeUI(data)
        } else {
            val oldIntegral = tv_gift_integral.text.toString().trim().toInt()
            val animator3 =
                ValueAnimator.ofInt(
                    oldIntegral,
                    data.user_integral!!
                )
            animator3.duration = 1000
            animator3.interpolator = LinearInterpolator()
            animator3.addUpdateListener {
                tv_gift_integral.text = animator3.animatedValue.toString()
            }
            animator3.start()
            toChangeUI(data)
        }
    }

    /**
     * 显示double领取的页面
     */
    private fun toShowDoubleDialog(data: DailyCheckBean.DataBean) {
        LevelUpdateDialog.show(
            this,
            data.user_level!!,
            data.give_integral!!,
            object : LevelUpdateDialog.OnLevelUpdateListener {
                override fun normalGet() {
                    LevelUpdateDialog.dismissDialog()
                    toUpdate(data, false)
                }

                override fun doubleGet() {
                    mData = data
                    isClickDoubleGet = true
                    BottomDialog.showShare(
                        this@GiftActivity,
                        "http://www.5745.com/share/game",
                        getString(R.string.level_update_share_title),
                        getString(R.string.level_update_share_msg),
                        false
                    )
                }
            })

    }

    /**
     * 通知修改"我的"界面的数据
     */
    private fun toChangeUI(data: DailyCheckBean.DataBean) {
        val userInfo = UserInfoBean.getData()
        userInfo?.user_integral = data.user_integral
        userInfo?.user_level = data.user_level
        userInfo?.user_now_exp = data.user_now_exp
        userInfo?.user_upgrade_exp = data.user_upgrade_exp
        EventBus.getDefault().postSticky(userInfo)
    }

    @Subscribe(sticky = true)
    fun showPoint(giftShowPoint: GiftShowPoint) {
        when (giftShowPoint.isShowDailyCheck) {
            GiftShowState.SHOW -> tt_gift.setDailyCheckPoint(true)
            GiftShowState.UN_SHOW -> tt_gift.setDailyCheckPoint(false)
            GiftShowState.USELESS -> {
                //啥也不干就可以
            }
        }
        when (giftShowPoint.isShowWhitePiao) {
            GiftShowState.SHOW -> tt_gift.setWhitePiaoPoint(true)
            GiftShowState.UN_SHOW -> tt_gift.setWhitePiaoPoint(false)
            GiftShowState.USELESS -> {
                //啥也不干就可以
            }
        }
        when (giftShowPoint.isShowInviteGift) {
            GiftShowState.SHOW -> tt_gift.setInviteGiftPoint(true)
            GiftShowState.UN_SHOW -> tt_gift.setInviteGiftPoint(false)
            GiftShowState.USELESS -> {
                //啥也不干就可以
            }
        }
    }

    override fun destroy() {
        EventBus.getDefault().unregister(this)

        doubleGetObservable?.dispose()
        doubleGetObservable = null
    }

    override fun onResume() {
        super.onResume()
        if (LevelUpdateDialog.isShowing() && isClickDoubleGet) {
            //正在分享这里,并且点击了double
            isClickDoubleGet = false
            toDoubleGet()
        }
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}