package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.activity.GiftActivity
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.bean.GetShareListBean
import com.fortune.zg.bean.RedPointBean
import com.fortune.zg.bean.UserInfoBean
import com.fortune.zg.event.GiftNeedNewInfo
import com.fortune.zg.event.GiftShowPoint
import com.fortune.zg.event.GiftShowState
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_invite_gift.view.*
import kotlinx.android.synthetic.main.item_invite_gift.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.TimeUnit

class InviteGiftFragment : Fragment() {

    private var mView: View? = null
    private var adapter: BaseAdapterWithPosition<GetShareListBean.DataBean.ListBean>? = null

    private var isShare = false
    private var getShareListObservable: Disposable? = null
    private var getInviteGiftObservable: Disposable? = null
    private var getShareUrlObservable: Disposable? = null
    private var shareFinishObservable: Disposable? = null

    private var inviteReward = 0 //邀请奖励
    private var maxLevel = 0 //领取奖励的最低等级
    private var shareReward = 0 //分享奖励等级

    private var currentDayShare = false //当天分享领取了没
    private var canGet = 0 //有多少个可以领取的
    private var isCreateGet = true

    private var mData = mutableListOf<GetShareListBean.DataBean.ListBean>()

    companion object {
        fun newInstance() = InviteGiftFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_invite_gift, container, false)
        initView()
        if (isCreateGet) {
            getShareList()
            isCreateGet = false
        }
        return mView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            getShareList()
        }
    }

    /**
     * 获取分享列表
     */
    @SuppressLint("SetTextI18n")
    private fun getShareList() {
        DialogUtils.showBeautifulDialog(activity as GiftActivity)
        val getShareList = RetrofitUtils.builder().getShareList()
        getShareListObservable = getShareList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                shareReward = it.getData()?.share_reward!!
                                inviteReward = it.getData()?.invite_reward!!
                                maxLevel = it.getData()?.max_level!!
                                if (it.getData()?.share_reward == 0) {
                                    //已经领取过当天的分享
                                    mView?.ll_inviteGift_money?.let { money ->
                                        money.visibility = View.GONE
                                    }
                                    mView?.tv_inviteGift_tips?.let { tips ->
                                        tips.visibility = View.INVISIBLE
                                    }
                                    currentDayShare = false
                                } else {
                                    currentDayShare = true
                                    mView?.tv_inviteGift_money?.text = "+$shareReward"
                                    mView?.tv_inviteGift_tips?.text =
                                        getString(R.string.share_tips).replace(
                                            "X",
                                            shareReward.toString()
                                        )
                                }
                                mView?.tv_inviteGift_inviteMoney?.text = "+$inviteReward"
                                if (it.getData()?.list != null) {
                                    mData.clear()
                                    for (data in it.getData()!!.list!!) {
                                        mData.add(data)
                                        if (data.receive == 0 && data.user?.user_level!! >= maxLevel) {
                                            //有没有领取的升级领取
                                            canGet++
                                        }
                                    }
                                    formatInfo()
                                    adapter?.notifyDataSetChanged()
                                }
                                if (!currentDayShare && canGet == 0) {
                                    if (RedPointBean.getData() != null) {
                                        val data = RedPointBean.getData()!!
                                        data.invite = 0
                                        RedPointBean.setData(data)
                                        EventBus.getDefault().postSticky(data)
                                    }
                                    //都没有可领取的话,没有小红点
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS,
                                            GiftShowState.UN_SHOW
                                        )
                                    )
                                } else {
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS,
                                            GiftShowState.SHOW
                                        )
                                    )
                                }
                            }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(activity as GiftActivity)
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as GiftActivity, it))
            })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        mView?.civ_inviteGift_head?.let {
            if (UserInfoBean.getData()?.user_avatar == null || UserInfoBean.getData()?.user_avatar?.contains(
                    "avatar/default.jpg"
                ) == true
            ) {
                it.setImageResource(R.mipmap.head_photo)
            } else {
                Glide.with(this)
                    .load(UserInfoBean.getData()?.user_avatar)
                    .placeholder(R.mipmap.head_photo)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(it)
            }
        }

        mView?.ll_inviteGift_share?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    toGetShareUrl()
                }
        }

        adapter = BaseAdapterWithPosition.Builder<GetShareListBean.DataBean.ListBean>()
            .setLayoutId(R.layout.item_invite_gift)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData.user?.user_avatar)
                    .placeholder(R.mipmap.bg_gray_6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.riv_item_gift_head)
                itemView.iv_item_gift_from.setImageResource(
                    when (itemData.channel) {
                        "wx" -> R.mipmap.gift_wechat
                        "qq" -> R.mipmap.gift_qq
                        else -> R.drawable.transparent
                    }
                )
                OmitTextViewUtils.omitTextView(
                    itemView.tv_item_gift_name,
                    itemData.user?.user_name!!,
                    4
                )
                itemView.tv_item_gift_integral.text = inviteReward.toString()
                itemView.tv_item_gift_tips.text =
                    getString(R.string.update_app_level).replace("X", maxLevel.toString())
                when (itemData.receive) {
                    0 -> {
                        //没有领取
                        itemView.tv_item_gift_level.text = "${itemData.user?.user_level}/$maxLevel"
                        if (itemData.user?.user_level!! >= maxLevel) {
                            //可以领取了
                            itemView.tv_item_gift_level.visibility = View.VISIBLE
                            itemView.tv_item_gift_get.text = getString(R.string.get)
                            itemView.tv_item_gift_get.setTextColor(resources.getColor(R.color.white_FFFFFF))
                            itemView.tv_item_gift_get.setBackgroundResource(R.drawable.bg_invite_gift_can_get)
                        } else {
                            //不能领取
                            itemView.tv_item_gift_level.visibility = View.VISIBLE
                            itemView.tv_item_gift_get.text = getString(R.string.get)
                            itemView.tv_item_gift_get.setTextColor(resources.getColor(R.color.white_FFFFFF))
                            itemView.tv_item_gift_get.setBackgroundResource(R.drawable.bg_invite_gift_can_not_get)
                        }
                    }
                    1 -> {
                        //领取了
                        itemView.tv_item_gift_level.visibility = View.GONE
                        itemView.tv_item_gift_get.text = getString(R.string.got)
                        itemView.tv_item_gift_get.setTextColor(resources.getColor(R.color.green_63C5AD))
                        itemView.tv_item_gift_get.setBackgroundResource(R.drawable.transparent)
                    }
                }

                RxView.clicks(itemView.tv_item_gift_get)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.receive == 0 && itemData.user?.user_level!! >= maxLevel) {
                            toGetInviteGift(itemData.id, itemView)
                            canGet--
                            if (!currentDayShare && canGet == 0) {
                                if (RedPointBean.getData() != null) {
                                    val data = RedPointBean.getData()!!
                                    data.invite = 0
                                    RedPointBean.setData(data)
                                    EventBus.getDefault().postSticky(data)
                                }
                                //都没有可领取的话,没有小红点
                                EventBus.getDefault().postSticky(
                                    GiftShowPoint(
                                        GiftShowState.USELESS,
                                        GiftShowState.USELESS,
                                        GiftShowState.UN_SHOW
                                    )
                                )
                            } else {
                                EventBus.getDefault().postSticky(
                                    GiftShowPoint(
                                        GiftShowState.USELESS,
                                        GiftShowState.USELESS,
                                        GiftShowState.SHOW
                                    )
                                )
                            }
                        }
                    }
            }.create()

        mView?.rv_inviteGift?.adapter = adapter
        mView?.rv_inviteGift?.layoutManager = SafeLinearLayoutManager(activity as GiftActivity)
    }

    /**
     * 格式化一下数据进行排序,将已领取放在底下
     */
    private fun formatInfo() {
        val canGetList = mutableListOf<GetShareListBean.DataBean.ListBean>()
        val waitGetList = mutableListOf<GetShareListBean.DataBean.ListBean>()
        val overGetList = mutableListOf<GetShareListBean.DataBean.ListBean>()
        if (mData.size > 0) {
            for (data in mData) {
                if (data.receive == 0) {
                    //没领取
                    if (data.user?.user_level!! >= maxLevel) {
                        //满足领取要求
                        canGetList.add(data)
                    } else {
                        waitGetList.add(data)
                    }
                } else if (data.receive == 1) {
                    //已领取
                    overGetList.add(data)
                }
            }
        }
        mData.clear()
        mData.addAll(canGetList)
        mData.addAll(waitGetList)
        mData.addAll(overGetList)
    }

    /**
     * 领取邀请奖励
     */
    private fun toGetInviteGift(id: Int?, itemView: View) {
        DialogUtils.showBeautifulDialog(activity as GiftActivity)
        val getInviteGift = RetrofitUtils.builder().getInviteGift(id!!)
        getInviteGiftObservable = getInviteGift.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                //领取成功之后的变形
                                itemView.tv_item_gift_level.visibility = View.GONE
                                itemView.tv_item_gift_get.text = getString(R.string.got)
                                itemView.tv_item_gift_get.setTextColor(resources.getColor(R.color.green_63C5AD))
                                itemView.tv_item_gift_get.setBackgroundResource(R.drawable.transparent)

                                val oldUserIntegral = UserInfoBean.getData()?.user_integral!!
                                val currentUserIntegral = it.getData()?.user_integral!!
                                if (currentUserIntegral > oldUserIntegral) {
                                    //说明是领到好东西了
                                    GetSuccessDialog.showIntegralDialog(
                                        activity as GiftActivity,
                                        inviteReward,
                                        object : GetSuccessDialog.OnCancelListener {
                                            override fun setOnCancel() {
                                                getShareList()
                                                val data = it.getData()!!
                                                data.is_upgrade = 0
                                                data.give_integral = 0
                                                EventBus.getDefault().postSticky(data)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(activity as GiftActivity)
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as GiftActivity, it))
            })
    }

    /**
     * 先获取分享链接
     */
    private fun toGetShareUrl() {
        DialogUtils.showBeautifulDialog(activity as GiftActivity)
        val getShareUrl = RetrofitUtils.builder().getShareUrl()
        getShareUrlObservable = getShareUrl.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null && it.getData()?.url != null) {
                                isShare = true
                                BottomDialog.showShare(
                                    activity as GiftActivity,
                                    it.getData()?.url!!,
                                    getString(R.string.level_update_share_title),
                                    getString(R.string.level_update_share_msg),
                                    false,
                                    needSpaceAndMoment = false
                                )
                            }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(activity as GiftActivity)
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as GiftActivity, it))
            })
    }

    override fun onStart() {
        super.onStart()
        if (isShare) {
            isShare = false
            toFinishShare()
        }
    }

    /**
     * 完成分享
     */
    private fun toFinishShare() {
        DialogUtils.showBeautifulDialog(activity as GiftActivity)
        val shareFinish = RetrofitUtils.builder().shareFinish()
        shareFinishObservable = shareFinish.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                //修改底部按钮
                                mView?.ll_inviteGift_money?.let { money ->
                                    money.visibility = View.GONE
                                }
                                mView?.tv_inviteGift_tips?.let { tips ->
                                    tips.visibility = View.INVISIBLE
                                }

                                val oldUserIntegral = UserInfoBean.getData()?.user_integral!!
                                val currentUserIntegral = it.getData()?.user_integral!!
                                if (currentUserIntegral > oldUserIntegral) {
                                    //领到好东西了,直接取消小红点
                                    if (RedPointBean.getData() != null) {
                                        val data = RedPointBean.getData()!!
                                        data.invite_share = 0
                                        RedPointBean.setData(data)
                                        EventBus.getDefault().postSticky(data)
                                    }
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS,
                                            GiftShowState.UN_SHOW
                                        )
                                    )

                                    //说明是领到好东西了
                                    it.getData()?.is_upgrade = 0
                                    it.getData()?.give_integral = 0
                                    GetSuccessDialog.showIntegralDialog(
                                        activity as GiftActivity,
                                        shareReward,
                                        object : GetSuccessDialog.OnCancelListener {
                                            override fun setOnCancel() {
                                                EventBus.getDefault().postSticky(it.getData())
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(activity as GiftActivity)
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as GiftActivity, it))
            })
    }

    @Subscribe
    fun time12NeedNewInfo(giftNeedNewInfo: GiftNeedNewInfo) {
        if (MyApp.getInstance().isHaveToken() && giftNeedNewInfo.isShowInviteGiftNeed) {
            getShareList()
        }
    }

    override fun onDestroy() {
        getShareListObservable?.dispose()
        getShareListObservable = null

        getInviteGiftObservable?.dispose()
        getInviteGiftObservable = null

        getShareUrlObservable?.dispose()
        getShareUrlObservable = null

        shareFinishObservable?.dispose()
        shareFinishObservable = null
        super.onDestroy()
    }
}