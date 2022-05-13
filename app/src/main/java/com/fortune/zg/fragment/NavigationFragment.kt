package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.activity.*
import com.fortune.zg.base.BaseAppUpdateSetting
import com.fortune.zg.bean.RedPointBean
import com.fortune.zg.bean.UserInfoBean
import com.fortune.zg.event.LoginStatusChange
import com.fortune.zg.event.RedPointChange
import com.fortune.zg.event.UserInfoChange
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_navigation.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit


class NavigationFragment : Fragment() {

    private var mView: View? = null
    private var getUserInfoObservable: Disposable? = null
    private var redPointObservable: Disposable? = null
    private val df = DecimalFormat("#0.00")
    private var canRequest = true //防止小红点的重复

    companion object {
        fun newInstance() = NavigationFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_navigation, container, false)
        initView()
        toCheckRedPoint()
        initData()
        if (!MyApp.getInstance().isHaveToken()) {
            startAnimation()
        }
        return mView
    }

    /**
     * 填充数据
     */
    @SuppressLint("SetTextI18n")
    private fun initData() {
        getUserInfo()
    }

    /**
     * 获取用户信息
     */
    @SuppressLint("SetTextI18n")
    private fun getUserInfo() {
        if (!MyApp.getInstance().isHaveToken()) {
            return
        }
        DialogUtils.showBeautifulDialog(activity as MainActivityV5)
        val getUserInfo = RetrofitUtils.builder().getUserInfo()
        getUserInfoObservable = getUserInfo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            mView?.tv_navigation_name?.text = it.data?.user_name
                            mView?.tv_navigation_des?.text = it.data?.user_desc
                            mView?.tv_navigation_integral?.text =
                                "${getString(R.string.integral)} ${it.data?.user_integral}"
                            mView?.tv_navigation_level?.text = "Lv.${it.data?.user_level}"
                            if (it.data?.user_upgrade_exp != 0) {
                                val percent =
                                    df.format(it.data?.user_now_exp!! * 100f / it.data?.user_upgrade_exp!!)
                                mView?.tv_navigation_level_percent?.text = "$percent%"
                                mView?.pb_navigation_level?.max = it.data?.user_upgrade_exp!!
                                mView?.pb_navigation_level?.progress = it.data?.user_now_exp!!
                            } else {
                                mView?.tv_navigation_level_percent?.text = "0.00%"
                                mView?.pb_navigation_level?.progress = 0
                            }
                            if (!it.data?.user_avatar.isNullOrEmpty()
                                && !it.data?.user_avatar!!.endsWith("/avatar/default.jpg")
                            ) {
                                mView?.civ_navigation_head?.let { it1 ->
                                    Glide.with(this)
                                        .load(it.data?.user_avatar)
                                        .placeholder(R.mipmap.head_photo)
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(it1)
                                }
                            }
                            mView?.tv_navigation_likes?.text = "${it.data?.user_video_like} 获赞"
                            mView?.tv_navigation_focus?.text = "${it.data?.user_follow} 关注"
                            mView?.tv_navigation_fans?.text = "${it.data?.user_fans} 粉丝"

//                            RxView.clicks(mView?.tv_navigation_focus!!)
//                                .throttleFirst(
//                                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
//                                    TimeUnit.MILLISECONDS
//                                )
//                                .subscribe { itt ->
//                                    if (MyApp.getInstance().isHaveToken()) {
//                                        val intent =
//                                            Intent(activity, FansAndFollowListActivity::class.java)
//                                        intent.putExtra(FansAndFollowListActivity.TYPE, 0)
//                                        intent.putExtra(
//                                            FansAndFollowListActivity.USER_ID,
//                                            it.data!!.user_id!!.toInt()
//                                        )
//                                        startActivity(intent)
//                                    } else {
//                                        LoginUtils.toQuickLogin(activity as MainActivityV5)
//                                    }
//                                }
//
//                            RxView.clicks(mView?.tv_navigation_fans!!)
//                                .throttleFirst(
//                                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
//                                    TimeUnit.MILLISECONDS
//                                )
//                                .subscribe { itt ->
//                                    if (MyApp.getInstance().isHaveToken()) {
//                                        val intent = Intent(
//                                            activity,
//                                            FansAndFollowListActivity::class.java
//                                        )
//                                        intent.putExtra(FansAndFollowListActivity.TYPE, 1)
//                                        intent.putExtra(
//                                            FansAndFollowListActivity.USER_ID,
//                                            it.data!!.user_id!!.toInt()
//                                        )
//                                        startActivity(intent)
//                                    } else {
//                                        LoginUtils.toQuickLogin(activity as MainActivityV5)
//                                    }
//                                }

                            it.data?.let { it1 -> UserInfoBean.setData(it1) }
                            EventBus.getDefault()
                                .postSticky(
                                    UserInfoChange(
                                        it.data?.user_avatar,
                                        it.data?.user_name,
                                        it.data?.user_desc
                                    )
                                )
                        }
                        -1 -> {
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(activity as MainActivityV5)
                        }
                        else -> {
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as MainActivityV5, it))
                DialogUtils.dismissLoading()
            })
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initView() {
        mView?.civ_navigation_head?.let {
            RxView.clicks(it)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                ).subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        startActivity(Intent(activity, UserInfoActivity::class.java))
                    } else {
                        LoginUtils.toQuickLogin(activity as MainActivityV5)
                    }
                }
        }

        mView?.tv_navigation_version?.text =
            "V ${MyApp.getInstance().getVersion()}${BaseAppUpdateSetting.patch}"

        mView?.rl_navigation_gift?.let {
            RxView.clicks(it)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                ).subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        startActivity(Intent(activity, GiftActivity::class.java))
                    } else {
                        LoginUtils.toQuickLogin(activity as MainActivityV5)
                    }
                }
        }
        mView?.rl_navigation_level?.let {
            RxView.clicks(it)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                ).subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        startActivity(Intent(activity, GiftActivity::class.java))
                    } else {
                        LoginUtils.toQuickLogin(activity as MainActivityV5)
                    }
                }
        }
        mView?.rl_navigation_integral?.let {
            RxView.clicks(it)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                ).subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        startActivity(Intent(activity, GiftActivity::class.java))
                    } else {
                        LoginUtils.toQuickLogin(activity as MainActivityV5)
                    }
                }
        }

        mView?.tv_navigation_getSomething?.let {
            it.paint.flags = Paint.UNDERLINE_TEXT_FLAG
        }

        mView?.ll_navigation_personal?.let {
            RxView.clicks(it)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                ).subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        startActivity(Intent(activity, UserInfoActivity::class.java))
                    } else {
                        LoginUtils.toQuickLogin(activity as MainActivityV5)
                    }
                }
        }
        mView?.ll_navigation_his?.let {
            RxView.clicks(it)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                )
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        startActivity(Intent(activity, FavActivity::class.java))
                    } else {
                        LoginUtils.toQuickLogin(activity as MainActivityV5)
                    }
                }
        }
        mView?.ll_mine_download?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    startActivity(Intent(activity as MainActivityV5, DownloadActivity::class.java))
                }
        }
        mView?.ll_navigation_issueMvHistory?.let {
            RxView.clicks(it)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                )
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        startActivity(Intent(activity, IssueMvHisActivity::class.java))
                    } else {
                        LoginUtils.toQuickLogin(activity as MainActivityV5)
                    }
                }
        }
        mView?.ll_navigation_safe?.let {
            RxView.clicks(it)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                ).subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        startActivity(Intent(activity, AccountSafeActivity::class.java))
                    } else {
                        LoginUtils.toQuickLogin(activity as MainActivityV5)
                    }
                }
        }
        mView?.ll_navigation_exit?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        DialogUtils.showDefaultDialog(activity as MainActivityV5,
                            getString(R.string.quit),
                            getString(R.string.quit_tips),
                            getString(R.string.cancel),
                            getString(R.string.quit),
                            object : DialogUtils.OnDialogListener {
                                override fun next() {
                                    mView?.tv_navigation_name?.text = "点击头像登录"
                                    mView?.tv_navigation_des?.text = "这个人很懒,没有设置自己的简介"
                                    mView?.tv_navigation_integral?.text = "金币"
                                    mView?.tv_navigation_level?.text = "Lv.0"
                                    mView?.tv_navigation_level_percent?.text = "0.00%"
                                    mView?.pb_navigation_level?.progress = 0
                                    mView?.civ_navigation_head?.setImageResource(R.mipmap.head_photo)
                                    mView?.tv_navigation_likes?.text = "0 获赞"
                                    mView?.tv_navigation_focus?.text = "0 关注"
                                    mView?.tv_navigation_fans?.text = "0 粉丝"
                                    ActivityManager.exitLogin(activity as MainActivityV5)
                                }
                            })
                    }
                }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun changeUserInfo(userInfoChange: UserInfoChange) {
        if (!userInfoChange.headPath.isNullOrEmpty() && !userInfoChange.headPath.endsWith("avatar/default.jpg")) {
            mView?.civ_navigation_head?.let {
                Glide.with(this)
                    .load(userInfoChange.headPath)
                    .placeholder(R.mipmap.head_photo)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(it)
            }
        }
        mView?.tv_navigation_name?.text = userInfoChange.name
        mView?.tv_navigation_des?.text = userInfoChange.des
    }


    @SuppressLint("SetTextI18n")
    @Subscribe(sticky = true)
    fun changeExperienceAndIntegral(userInfo: UserInfoBean.Data) {
        mView?.tv_navigation_integral?.text =
            "${getString(R.string.integral)} ${userInfo.user_integral}"
        mView?.tv_navigation_level?.text = "Lv.${userInfo.user_level}"
        if (userInfo.user_upgrade_exp != 0) {
            val percent = df.format(userInfo.user_now_exp!! * 100f / userInfo.user_upgrade_exp!!)
            mView?.tv_navigation_level_percent?.text = "$percent%"
            mView?.pb_navigation_level?.max = userInfo.user_upgrade_exp!!
            mView?.pb_navigation_level?.progress = userInfo.user_now_exp!!
        } else {
            mView?.tv_navigation_level_percent?.text = "0.00%"
            mView?.pb_navigation_level?.progress = 0
        }
    }

    override fun onDestroy() {
        getUserInfoObservable?.dispose()
        getUserInfoObservable = null

        redPointObservable?.dispose()
        redPointObservable = null
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            stopAnimation()
        } else {
            if (MyApp.getInstance().isHaveToken() && RedPointBean.getData() != null) {
                val data = RedPointBean.getData()!!
                if (data.daily_clock_in == 0 && data.limit_time == 0 && data.invite == 0 && data.invite_share == 0) {
                    //都为0,说明没有红点
                    stopAnimation()
                } else {
                    //有红点
                    startAnimation()
                }
                EventBus.getDefault().postSticky(data)
            } else if (RedPointBean.getData() == null) {
                toCheckRedPoint()
            }

            if (MyApp.getInstance().isHaveToken()
                && (UserInfoBean.getData() == null && UserInfoBean.getData()?.user_phone == null)
            ) {
                getUserInfo()
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun checkRedPoint(redPointChange: RedPointChange) {
        if (MyApp.getInstance().isHaveToken()) {
            toCheckRedPoint()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun login(loginStatusChange: LoginStatusChange) {
        getUserInfo()
    }

    /**
     * 检查有没有小红点
     */
    private fun toCheckRedPoint() {
        if (MyApp.getInstance().isHaveToken() && canRequest) {
            canRequest = false
        } else {
            return
        }
        val redPoint = RetrofitUtils.builder().redPoint()
        redPointObservable = redPoint.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                canRequest = true
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                val data = it.getData()!!
                                RedPointBean.setData(data)
                                if (data.daily_clock_in == 0 && data.limit_time == 0 && data.invite == 0 && data.invite_share == 0) {
                                    //都为0,说明没有红点
                                    stopAnimation()
                                } else {
                                    //有红点
                                    startAnimation()
                                }
                                EventBus.getDefault().postSticky(data)
                            }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(activity as MainActivityV5)
                        }
                        else -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                canRequest = true
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as MainActivityV5, it))
            })
    }

    /**
     * 开始动画
     */
    private fun startAnimation() {
        stopAnimation()
        Thread.sleep(100)
        mView?.iv_navigation_gift_point?.visibility = View.VISIBLE
        FlipAnimUtils.startShakeByPropertyAnim(
            mView?.rl_navigation_gift,
            0.9f,
            1.1f,
            20f,
            2000L
        )
    }

    /**
     * 停止动画
     */
    private fun stopAnimation() {
        mView?.iv_navigation_gift_point?.visibility = View.GONE
        FlipAnimUtils.stopShakeByPropertyAnim(mView?.rl_navigation_gift!!)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun changeRedPoint(data: RedPointBean.DataBean) {
        LogUtils.d("+++${Gson().toJson(data)}")
        if (data.daily_clock_in == 0 && data.limit_time == 0 && data.invite == 0 && data.invite_share == 0) {
            stopAnimation()
        } else {
            startAnimation()
        }
    }
}