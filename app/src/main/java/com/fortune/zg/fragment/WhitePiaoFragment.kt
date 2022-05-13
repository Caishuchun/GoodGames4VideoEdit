package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.zg.R
import com.fortune.zg.activity.GiftActivity
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.bean.RedPointBean
import com.fortune.zg.bean.WhitePiaoListBean
import com.fortune.zg.event.GiftNeedNewInfo
import com.fortune.zg.event.GiftShowPoint
import com.fortune.zg.event.GiftShowState
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_white_piao.view.*
import kotlinx.android.synthetic.main.item_white_piao.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class WhitePiaoFragment : Fragment() {

    private var mView: View? = null
    private var adapter: BaseAdapterWithPosition<WhitePiaoListBean.DataBean>? = null
    private var whitePiaoListObservable: Disposable? = null
    private var whitePiaoObservable: Disposable? = null
    private var mDate = mutableListOf<WhitePiaoListBean.DataBean>()
    private var canClick = false

    companion object {
        fun newInstance() = WhitePiaoFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_white_piao, container, false)
        getInfo()
        return mView
    }

    /**
     * 获取数据
     */
    private fun getInfo() {
        DialogUtils.showBeautifulDialog(activity as GiftActivity)
        val whitePiaoList = RetrofitUtils.builder().whitePiaoList()
        whitePiaoListObservable = whitePiaoList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            mDate = it.getData() as MutableList<WhitePiaoListBean.DataBean>
                            for (data in mDate) {
                                if (data.status == 1) {
                                    canClick = true
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.USELESS,
                                            GiftShowState.SHOW,
                                            GiftShowState.USELESS
                                        )
                                    )
                                }
                            }
                            initView()
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

    @SuppressLint("SimpleDateFormat", "SetTextI18n", "CheckResult")
    private fun initView() {
        val currentTimeMillis = System.currentTimeMillis()
        val format = SimpleDateFormat("yyyyMMDD HH:mm:ss").format(currentTimeMillis)
        val currentHour = format.split(" ")[1].split(":")[0].toInt()
        LogUtils.d("currentHour = $currentHour")

        adapter = BaseAdapterWithPosition.Builder<WhitePiaoListBean.DataBean>()
            .setLayoutId(R.layout.item_white_piao)
            .setData(mDate)
            .addBindView { itemView, itemData, position ->
                /**
                 * 已领取
                 */
                fun got(time: String) {
                    itemView.rl_white_piao_bg.setBackgroundResource(R.drawable.bg_white_piao_got)
                    itemView.iv_white_piao_integral.setImageResource(R.mipmap.money_ed)
                    itemView.iv_white_piao_experience.setImageResource(R.mipmap.experience_ed)
                    itemView.iv_white_piao_add.setImageResource(R.mipmap.add_ed)
                    itemView.tv_white_piao_btn.text =
                        "$time  ${getString(R.string.got)}"
                }

                /**
                 * 点击领取
                 */
                fun get(time: String) {
                    itemView.rl_white_piao_bg.setBackgroundResource(R.drawable.bg_white_piao_get)
                    itemView.iv_white_piao_integral.setImageResource(R.mipmap.money)
                    itemView.iv_white_piao_experience.setImageResource(R.mipmap.experience)
                    itemView.iv_white_piao_add.setImageResource(R.mipmap.add)
                    itemView.tv_white_piao_btn.text =
                        "$time  ${getString(R.string.click_get)}"
                }

                /**
                 * 可以领取
                 */
                fun canGet(time: String) {
                    itemView.rl_white_piao_bg.setBackgroundResource(R.drawable.bg_white_piao_can_get)
                    itemView.iv_white_piao_integral.setImageResource(R.mipmap.money_ed)
                    itemView.iv_white_piao_experience.setImageResource(R.mipmap.experience_ed)
                    itemView.iv_white_piao_add.setImageResource(R.mipmap.add_ed)
                    itemView.tv_white_piao_btn.text =
                        "$time  ${getString(R.string.can_get)}"
                }

                /**
                 * 已过期
                 */
                fun overdue(time: String) {
                    itemView.rl_white_piao_bg.setBackgroundResource(R.drawable.bg_white_piao_overdue)
                    itemView.iv_white_piao_integral.setImageResource(R.mipmap.money_ed)
                    itemView.iv_white_piao_experience.setImageResource(R.mipmap.experience_ed)
                    itemView.iv_white_piao_add.setImageResource(R.mipmap.add_ed)
                    itemView.tv_white_piao_btn.text =
                        "$time  ${getString(R.string.overdue)}"
                }

                itemView.tv_white_piao_integral.text =
                    "${getString(R.string.integral)} +${itemData.integral}"
                itemView.tv_white_piao_experience.text =
                    "${getString(R.string.experience)} +${itemData.exp}"

                when (itemData.id) {
                    1 -> {
                        when (itemData.status) {
                            0 -> {
                                canGet(getString(R.string.time_5_9))
                            }
                            1 -> {
                                get(getString(R.string.time_5_9))
                            }
                            2 -> {
                                overdue(getString(R.string.time_5_9))
                            }
                            3 -> {
                                got(getString(R.string.time_5_9))
                            }
                        }
                    }
                    2 -> {
                        when (itemData.status) {
                            0 -> {
                                canGet(getString(R.string.time_10_14))
                            }
                            1 -> {
                                get(getString(R.string.time_10_14))
                            }
                            2 -> {
                                overdue(getString(R.string.time_10_14))
                            }
                            3 -> {
                                got(getString(R.string.time_10_14))
                            }
                        }
                    }
                    3 -> {
                        when (itemData.status) {
                            0 -> {
                                canGet(getString(R.string.time_15_19))
                            }
                            1 -> {
                                get(getString(R.string.time_15_19))
                            }
                            2 -> {
                                overdue(getString(R.string.time_15_19))
                            }
                            3 -> {
                                got(getString(R.string.time_15_19))
                            }
                        }
                    }
                    4 -> {
                        when (itemData.status) {
                            0 -> {
                                canGet(getString(R.string.time_20_24))
                            }
                            1 -> {
                                get(getString(R.string.time_20_24))
                            }
                            2 -> {
                                overdue(getString(R.string.time_20_24))
                            }
                            3 -> {
                                got(getString(R.string.time_20_24))
                            }
                        }
                    }
                }

                /**
                 * 进行白嫖
                 */
                fun toWhitePiao(id: Int) {
                    DialogUtils.showBeautifulDialog(activity as GiftActivity)
                    val whitePiao = RetrofitUtils.builder().whitePiao(id)
                    whitePiaoObservable = whitePiao.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            DialogUtils.dismissLoading()
                            LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                            if (it != null) {
                                when (it.getCode()) {
                                    1 -> {
                                        canClick = false
                                        //白嫖成功后,去掉小红点
                                        if (RedPointBean.getData() != null) {
                                            val data = RedPointBean.getData()!!
                                            data.limit_time = 0
                                            RedPointBean.setData(data)
                                            EventBus.getDefault().postSticky(data)
                                        }
                                        EventBus.getDefault().postSticky(
                                            GiftShowPoint(
                                                GiftShowState.USELESS,
                                                GiftShowState.UN_SHOW,
                                                GiftShowState.USELESS
                                            )
                                        )

                                        //显示弹框后再通知更新数据
                                        GetSuccessDialog.showDialog(
                                            activity as GiftActivity,
                                            itemData.integral!!,
                                            itemData.exp!!,
                                            object : GetSuccessDialog.OnCancelListener {
                                                override fun setOnCancel() {
                                                    EventBus.getDefault().postSticky(it.getData()!!)
                                                    when (position) {
                                                        0 -> {
                                                            got(getString(R.string.time_5_9))
                                                        }
                                                        1 -> {
                                                            got(getString(R.string.time_10_14))
                                                        }
                                                        2 -> {
                                                            got(getString(R.string.time_15_19))
                                                        }
                                                        3 -> {
                                                            got(getString(R.string.time_20_24))
                                                        }
                                                    }
                                                }
                                            })
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
                            ToastUtils.show(
                                HttpExceptionUtils.getExceptionMsg(
                                    activity as GiftActivity,
                                    it
                                )
                            )
                        })
                }

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.status == 1 && canClick)
                            toWhitePiao(itemData.id!!)
                    }
            }
            .create()
        mView?.rv_whitePiao?.adapter = adapter
        mView?.rv_whitePiao?.layoutManager =
            SafeLinearLayoutManager(activity as GiftActivity)
    }

    @Subscribe
    fun time12NeedNewInfo(giftNeedNewInfo: GiftNeedNewInfo) {
        if (MyApp.getInstance().isHaveToken() && giftNeedNewInfo.isShowWhitePiaoNeed) {
            getInfo()
        }
    }

    override fun onDestroy() {
        whitePiaoListObservable?.dispose()
        whitePiaoListObservable = null

        whitePiaoObservable?.dispose()
        whitePiaoObservable = null
        super.onDestroy()
    }
}