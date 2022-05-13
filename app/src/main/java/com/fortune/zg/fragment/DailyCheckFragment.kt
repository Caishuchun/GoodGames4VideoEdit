package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.fortune.zg.R
import com.fortune.zg.activity.GiftActivity
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.bean.DailyCheckListBean
import com.fortune.zg.bean.RedPointBean
import com.fortune.zg.event.GiftNeedNewInfo
import com.fortune.zg.event.GiftShowPoint
import com.fortune.zg.event.GiftShowState
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_daily_check.view.*
import kotlinx.android.synthetic.main.item_daily_check.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.TimeUnit

class DailyCheckFragment : Fragment() {

    private var mView: View? = null
    private var adapter: BaseAdapterWithPosition<DailyCheckListBean.DataBean.ListBean>? = null
    private var dailyCheckListObservable: Disposable? = null
    private var dailyCheckObservable: Disposable? = null
    private var mData = mutableListOf<DailyCheckListBean.DataBean.ListBean>()
    private var canClickPosition = 0
    private var isTodayGet = false //今天是否领取了

    companion object {
        fun newInstance() = DailyCheckFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_daily_check, container, false)
        initView()
        getData()
        return mView
    }

    private fun getData() {
        DialogUtils.showBeautifulDialog(activity as GiftActivity)
        val dailyCheckList = RetrofitUtils.builder().dailyCheckList()
        dailyCheckListObservable = dailyCheckList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null && it.getData()!!.list != null) {
                                if (it.getData()?.is_clock_in == 0) {
                                    isTodayGet = false
                                    //没签到,小红点
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.SHOW,
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS
                                        )
                                    )
                                } else {
                                    isTodayGet = true
                                    //签到,去掉小红点
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.UN_SHOW,
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS
                                        )
                                    )
                                }
                                if (it.getData()!!.list != null) {
                                    mData.clear()
                                    for (data in it.getData()!!.list!!) {
                                        data.let { dataInfo -> mData.add(dataInfo) }
                                    }
                                }
                                if (mData.size > 0) {
                                    for (index in mData.indices) {
                                        if (mData[index].status == 0) {
                                            canClickPosition = index
                                            break
                                        }
                                    }
                                }
                            }
                            val scrollToPosition = if (canClickPosition < 4) {
                                0
                            } else {
                                val i = canClickPosition / 4
                                i * 4
                            }
                            mView?.rv_gift_dailyCheck?.scrollToPosition(scrollToPosition)
                            LogUtils.d("canClickPosition:$canClickPosition")
                            adapter?.notifyDataSetChanged()
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(activity as GiftActivity)
                        }
                        else -> {
                            (activity as GiftActivity).finish()
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    (activity as GiftActivity).finish()
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                (activity as GiftActivity).finish()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as GiftActivity, it))
            })
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        adapter = BaseAdapterWithPosition.Builder<DailyCheckListBean.DataBean.ListBean>()
            .setData(mData)
            .setLayoutId(R.layout.item_daily_check)
            .addBindView { itemView, itemData, position ->
                val scaleAnimation = ScaleAnimation(
                    1f,
                    1.2f,
                    1f,
                    1.2f,
                    PhoneInfoUtils.getWidth(activity as GiftActivity) / 360f * 30f / 2,
                    PhoneInfoUtils.getWidth(activity as GiftActivity) / 360f * 30f / 2
                )
                scaleAnimation.duration = 500
                scaleAnimation.repeatMode = ScaleAnimation.REVERSE
                scaleAnimation.repeatCount = Int.MAX_VALUE
                if (position == canClickPosition && !isTodayGet
                ) {
                    //今天的没有领取的话,就开始布灵布灵的
                    itemView.iv_item_dailyCheck_type.startAnimation(scaleAnimation)
                }

                itemView.rl_item_dailyCheck_bg.setBackgroundResource(
                    if (itemData.status == 0) R.drawable.bg_daily_checkable
                    else R.drawable.bg_daily_checked
                )
                itemView.tv_item_dailyCheck_title.text =
                    if (itemData.status != 0) getString(R.string.signed) else getString(R.string.day).replace(
                        "X",
                        (position + 1).toString()
                    )
                itemView.iv_item_dailyCheck_type.setImageResource(
                    if (itemData.type == 1) if (itemData.status == 0) R.mipmap.experience else R.mipmap.experience_ed
                    else if (itemData.status == 0) R.mipmap.money else R.mipmap.money_ed
                )
                itemView.tv_item_dailyCheck_num.text =
                    if (itemData.type == 1) "XP +${itemData.num}" else "${getString(R.string.integral)} +${itemData.num}"

                itemView.tv_item_dailyCheck_num.setTextColor(
                    if (itemData.status == 0) resources.getColor(R.color.orange_FF9C00)
                    else resources.getColor(R.color.gray_C4C4C4)
                )

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.status == 0 && position == canClickPosition && !isTodayGet)
                            toAddExperienceAndIntegral(itemData.type!!, itemData.num!!, itemView)
                    }
            }
            .create()

        mView?.rv_gift_dailyCheck?.adapter = adapter
        mView?.rv_gift_dailyCheck?.setItemViewCacheSize(32)
        mView?.rv_gift_dailyCheck?.layoutManager =
            SafeStaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)
    }

    /**
     * 签到获取经验或者积分
     */
    private fun toAddExperienceAndIntegral(type: Int, num: Int, itemView: View) {
        DialogUtils.showBeautifulDialog(activity as GiftActivity)
        val dailyCheck = RetrofitUtils.builder().dailyCheck()
        dailyCheckObservable = dailyCheck.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            itemView.iv_item_dailyCheck_type.clearAnimation()
                            itemView.setBackgroundResource(R.drawable.bg_daily_checked)
                            itemView.tv_item_dailyCheck_title.text = getString(R.string.signed)
                            itemView.tv_item_dailyCheck_num.setTextColor(resources.getColor(R.color.gray_C4C4C4))
                            itemView.iv_item_dailyCheck_type.setImageResource(
                                if (type == 1) R.mipmap.experience_ed
                                else R.mipmap.money_ed
                            )
                            //当签到成功时候,需要把小红点去掉
                            if (RedPointBean.getData() != null) {
                                val data = RedPointBean.getData()!!
                                data.daily_clock_in = 0
                                RedPointBean.setData(data)
                                EventBus.getDefault().postSticky(data)
                            }
                            EventBus.getDefault().postSticky(
                                GiftShowPoint(
                                    GiftShowState.UN_SHOW,
                                    GiftShowState.USELESS,
                                    GiftShowState.USELESS
                                )
                            )
                            if (type == 1) {
                                GetSuccessDialog.showExperienceDialog(
                                    activity as GiftActivity,
                                    num,
                                    object : GetSuccessDialog.OnCancelListener {
                                        override fun setOnCancel() {
                                            EventBus.getDefault().postSticky(it.getData()!!)
                                        }
                                    }
                                )
                            } else {
                                GetSuccessDialog.showIntegralDialog(activity as GiftActivity,
                                    num,
                                    object : GetSuccessDialog.OnCancelListener {
                                        override fun setOnCancel() {
                                            EventBus.getDefault().postSticky(it.getData()!!)
                                        }
                                    })
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
                (activity as GiftActivity).finish()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as GiftActivity, it))
            })
    }

    @Subscribe
    fun time12NeedNewInfo(giftNeedNewInfo: GiftNeedNewInfo) {
        if (MyApp.getInstance().isHaveToken() && giftNeedNewInfo.isShowDailyCheckNeed) {
            getData()
        }
    }

    override fun onDestroy() {
        dailyCheckListObservable?.dispose()
        dailyCheckListObservable = null

        dailyCheckObservable?.dispose()
        dailyCheckObservable = null
        super.onDestroy()
    }
}