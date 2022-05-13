package com.fortune.zg.activity

import android.annotation.SuppressLint
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.FansAndFollowLitBean
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_fans_and_follow_list.*
import kotlinx.android.synthetic.main.item_fans_follow_list.view.*
import java.util.concurrent.TimeUnit

class FansAndFollowListActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: FansAndFollowListActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val TYPE = "type"
        const val USER_ID = "user_id"
    }

    //0 关注列表  1 粉丝列表
    private var type = 0
    private var userId = -1
    private var mAdapter: BaseAdapterWithPosition<FansAndFollowLitBean.Data>? = null
    private var listObservable: Disposable? = null
    private var mData = mutableListOf<FansAndFollowLitBean.Data>()

    override fun getLayoutId() = R.layout.activity_fans_and_follow_list

    @SuppressLint("CheckResult")
    override fun doSomething() {
        instance = this

        RxView.clicks(iv_fansAndFollowList_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        type = intent.getIntExtra(TYPE, 0)
        userId = intent.getIntExtra(USER_ID, -1)
        when (type) {
            0 -> {
                tv_fansAndFollowList_title.text = "关注列表"
            }
            1 -> {
                tv_fansAndFollowList_title.text = "粉丝列表"
            }
        }

        mAdapter = BaseAdapterWithPosition.Builder<FansAndFollowLitBean.Data>()
            .setLayoutId(R.layout.item_fans_follow_list)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                itemView.tv_fansAndFollowList_userName.text =
                    if (itemData.user_name.isEmpty()) "好服多多" else itemData.user_name
            }
            .create()
        rv_fansAndFollowList.adapter = mAdapter
        rv_fansAndFollowList.layoutManager = SafeLinearLayoutManager(this)

        getInfo()
    }

    /**
     * 获取关注.粉丝列表
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun getInfo() {
        val list = if (type == 0) {
            RetrofitUtils.builder().followList(userId)
        } else {
            RetrofitUtils.builder().fansList(userId)
        }
        listObservable = list.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            mData.clear()
                            if (it.data.isNotEmpty()) {
                                mData.addAll(it.data)
                                mAdapter?.notifyDataSetChanged()
                            }
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    DialogUtils.dismissLoading()
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    override fun destroy() {
    }
}