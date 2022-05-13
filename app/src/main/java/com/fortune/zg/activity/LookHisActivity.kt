package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapter
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.GameListBean
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_look_his.*
import kotlinx.android.synthetic.main.layout_item_search_result.view.*
import kotlinx.android.synthetic.main.layout_item_tag.view.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class LookHisActivity : BaseActivity() {

    private var gameLists = mutableListOf<GameListBean.DataBean.ListBean>()
    private var adapter: BaseAdapter<GameListBean.DataBean.ListBean>? = null
    private var collectListObservable: Disposable? = null
    private var currentPage = 1
    private var countPage = 1

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: LookHisActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    @SuppressLint("SimpleDateFormat")
    private val df = SimpleDateFormat("MM.dd")

    override fun getLayoutId() = R.layout.activity_look_his

    @SuppressLint("CheckResult")
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, false)
        instance = this

        RxView.clicks(iv_lookHis_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        initView()
    }

    /**
     * 获取收藏列表
     */
    private fun toGetInfo(isNeedDialog: Boolean, isRefresh: Boolean) {
        if (isNeedDialog) {
            DialogUtils.showBeautifulDialog(this)
        }
        val collectList = RetrofitUtils.builder().collectList(currentPage)
        collectListObservable = collectList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                refresh_collect_list.finishRefresh()
                refresh_collect_list.finishLoadMore()
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (isNeedDialog) {
                    DialogUtils.dismissLoading()
                }
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                if (currentPage == 1) {
                                    gameLists.clear()
                                    adapter?.notifyDataSetChanged()
                                }
                                val count = it.getData()?.paging?.count!!
                                val limit = it.getData()?.paging?.limit!!
                                countPage = count / limit
                                if (count % limit != 0) {
                                    countPage++
                                }
                                if (countPage == 0) {
                                    countPage = 1
                                }
                                if (it.getData()?.list != null) {
                                    it.getData()?.list?.let { list ->
                                        gameLists.addAll(list)
                                    }
                                    adapter?.notifyDataSetChanged()
                                }
                            }
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
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                if (isNeedDialog) {
                    DialogUtils.dismissLoading()
                }
            })
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initView() {
        refresh_collect_list.setEnableRefresh(true)
        refresh_collect_list.setEnableLoadMore(true)
        refresh_collect_list.setEnableLoadMoreWhenContentNotFull(false)
        refresh_collect_list.setRefreshHeader(MaterialHeader(this))
        refresh_collect_list.setRefreshFooter(ClassicsFooter(this))

        refresh_collect_list.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                currentPage = 1
                toGetInfo(isNeedDialog = false, isRefresh = true)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    toGetInfo(isNeedDialog = false, isRefresh = false)
                } else {
                    refresh_collect_list.finishLoadMoreWithNoMoreData()
                }
            }
        })

        adapter = BaseAdapter.Builder<GameListBean.DataBean.ListBean>()
            .setData(gameLists)
            .setLayoutId(R.layout.layout_item_search_result)
            .addBindView { itemView, itemData ->
                Glide.with(this)
                    .load(itemData.game_cover)
                    .placeholder(R.mipmap.bg_gray_6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.iv_item_result_icon)
                itemView.tv_item_result_hot.text = itemData.game_hits.toString()
                itemView.tv_item_result_name.text = itemData.game_name
                itemView.tv_item_result_type.text = getSystem(itemData.game_system)
                itemView.tv_item_result_type.setBackgroundResource(
                    if (itemData.game_system == 1) R.drawable.bg_game_type_purple
                    else R.drawable.bg_game_type_blue
                )
                itemView.tv_item_result_time.text =
                    "${df.format(itemData.game_update_time * 1000L)}${getString(R.string.update)}"
                itemView.tv_item_result_code.text =
                    "${getString(R.string.game_id)}${itemData.game_id}"

                itemView.fl_item_result_tag.removeAllViews()
                if (!itemData.game_tag.isNullOrEmpty()) {
                    for (tag in itemData.game_tag!!) {
                        val view =
                            LayoutInflater.from(this).inflate(R.layout.layout_item_tag, null)
                        view.tv_tag.text = tag
                        itemView.fl_item_result_tag.addView(view)
                    }
                }

                if (itemData.game_system == 1) {
                    itemView.iv_item_result_gift.visibility = View.VISIBLE
                    itemView.tv_item_result_gift_num.visibility = View.VISIBLE
                    itemView.tv_item_result_gift_num.text = "x${itemData.game_gift_last}"
                } else {
                    itemView.iv_item_result_gift.visibility = View.GONE
                    itemView.tv_item_result_gift_num.visibility = View.GONE
                }

                itemView.iv_item_result_news.setImageResource(
                    if (itemData.game_badge == "new") R.mipmap.game_new
                    else R.mipmap.good
                )
                itemView.tv_item_result_des.text = itemData.game_desc

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        MobclickAgent.onEvent(
                            this,
                            "game_detail",
                            "game_detail_from_collect"
                        )
                        val intent = Intent(this, GameDetailActivity::class.java)
                        intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id)
                        intent.putExtra(GameDetailActivity.GAME_COVER, itemData.game_cover)
                        intent.putExtra(GameDetailActivity.GAME_BADGE, itemData.game_badge)
                        startActivity(intent)
                    }
            }
            .create()

        rv_collect_list.adapter = adapter
        rv_collect_list.layoutManager = SafeLinearLayoutManager(this)
    }

    /**
     * 格式化系统类型
     */
    private fun getSystem(type: Int) = when (type) {
        1 -> getString(R.string.pc)
        else -> getString(R.string.phone)
    }

    override fun destroy() {
        collectListObservable?.dispose()
        collectListObservable = null
    }

    override fun onResume() {
        super.onResume()
        toGetInfo(isNeedDialog = true, isRefresh = false)
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}