package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.activity.FavActivity
import com.fortune.zg.activity.GameDetailActivity
import com.fortune.zg.adapter.BaseAdapter
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
import kotlinx.android.synthetic.main.fragment_fav4_game.view.*
import kotlinx.android.synthetic.main.layout_item_search_result.view.*
import kotlinx.android.synthetic.main.layout_item_tag.view.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class Fav4GameFragment : Fragment() {

    companion object {
        fun newInstance() = Fav4GameFragment()
    }

    private var mView: View? = null

    private var currentPage = 1
    private var countPage = 1
    private var collectListObservable: Disposable? = null
    private var adapter: BaseAdapter<GameListBean.DataBean.ListBean>? = null
    private var gameLists = mutableListOf<GameListBean.DataBean.ListBean>()

    @SuppressLint("SimpleDateFormat")
    private val df = SimpleDateFormat("MM.dd")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_fav4_game, container, false)
        initView()
        return mView
    }

    override fun onResume() {
        super.onResume()
        toGetInfo(isNeedDialog = false, isRefresh = false)
    }

    /**
     * 获取收藏列表
     */
    private fun toGetInfo(isNeedDialog: Boolean, isRefresh: Boolean) {
        if (isNeedDialog) {
            DialogUtils.showBeautifulDialog(activity as FavActivity)
        }
        val collectList = RetrofitUtils.builder().collectList(currentPage)
        collectListObservable = collectList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mView?.refresh_fav_game?.finishRefresh()
                mView?.refresh_fav_game?.finishLoadMore()
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
                            ActivityManager.toSplashActivity(activity as FavActivity)
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
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as FavActivity, it))
                if (isNeedDialog) {
                    DialogUtils.dismissLoading()
                }
            })
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initView() {
        mView?.refresh_fav_game?.setEnableRefresh(true)
        mView?.refresh_fav_game?.setEnableLoadMore(true)
        mView?.refresh_fav_game?.setEnableLoadMoreWhenContentNotFull(false)
        mView?.refresh_fav_game?.setRefreshHeader(MaterialHeader(activity as FavActivity))
        mView?.refresh_fav_game?.setRefreshFooter(ClassicsFooter(activity as FavActivity))

        mView?.refresh_fav_game?.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                currentPage = 1
                toGetInfo(isNeedDialog = false, isRefresh = true)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    toGetInfo(isNeedDialog = false, isRefresh = false)
                } else {
                    mView?.refresh_fav_game?.finishLoadMoreWithNoMoreData()
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
                            LayoutInflater.from(activity as FavActivity)
                                .inflate(R.layout.layout_item_tag, null)
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
                            activity as FavActivity,
                            "game_detail",
                            "game_detail_from_collect"
                        )
                        val intent = Intent(activity as FavActivity, GameDetailActivity::class.java)
                        intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id)
                        intent.putExtra(GameDetailActivity.GAME_COVER, itemData.game_cover)
                        intent.putExtra(GameDetailActivity.GAME_BADGE, itemData.game_badge)
                        startActivity(intent)
                    }
            }
            .create()

        mView?.rv_fav_game?.adapter = adapter
        mView?.rv_fav_game?.layoutManager = SafeLinearLayoutManager(activity as FavActivity)
    }

    /**
     * 格式化系统类型
     */
    private fun getSystem(type: Int) = when (type) {
        1 -> getString(R.string.pc)
        else -> getString(R.string.phone)
    }

    override fun onDestroy() {
        collectListObservable?.dispose()
        collectListObservable = null
        super.onDestroy()
    }
}