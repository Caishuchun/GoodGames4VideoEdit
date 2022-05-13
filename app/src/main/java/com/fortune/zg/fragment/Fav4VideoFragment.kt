package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.activity.FavActivity
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.bean.GameListWithMvBean
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.fortune.zg.video.VideoActivity
import com.fortune.zg.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_fav4_video.view.*
import kotlinx.android.synthetic.main.layout_game_item.view.*
import java.util.concurrent.TimeUnit

class Fav4VideoFragment : Fragment() {

    companion object {
        fun newInstance() = Fav4VideoFragment()
    }

    private var mView: View? = null
    private var gameLists = mutableListOf<GameListWithMvBean.DataBean.ListBean>()
    private var adapter: BaseAdapterWithPosition<GameListWithMvBean.DataBean.ListBean>? = null
    private var collectVideoListObservable: Disposable? = null
    private var currentPage = 1
    private var countPage = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_fav4_video, container, false)
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
        val collectVideoList = RetrofitUtils.builder().collectVideoList(currentPage)
        collectVideoListObservable = collectVideoList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mView?.refresh_fav_video?.finishRefresh()
                mView?.refresh_fav_video?.finishLoadMore()
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
        mView?.refresh_fav_video?.setEnableRefresh(true)
        mView?.refresh_fav_video?.setEnableLoadMore(true)
        mView?.refresh_fav_video?.setEnableLoadMoreWhenContentNotFull(false)
        mView?.refresh_fav_video?.setRefreshHeader(MaterialHeader(activity as FavActivity))
        mView?.refresh_fav_video?.setRefreshFooter(ClassicsFooter(activity as FavActivity))

        mView?.refresh_fav_video?.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                currentPage = 1
                toGetInfo(isNeedDialog = false, isRefresh = true)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    toGetInfo(isNeedDialog = false, isRefresh = false)
                } else {
                    mView?.refresh_fav_video?.finishLoadMoreWithNoMoreData()
                }
            }
        })

        adapter = BaseAdapterWithPosition.Builder<GameListWithMvBean.DataBean.ListBean>()
            .setData(gameLists)
            .setLayoutId(R.layout.layout_game_item)
            .addBindView { itemView, itemData, position ->
                itemView.ll_item_game_normal?.visibility = View.GONE
                itemView.ll_item_game_live?.visibility = View.GONE
                itemView.rl_item_game_mv?.visibility = View.VISIBLE
                itemView.tv_item_game_mv_des.text = itemData.video_name

                val layoutParams = itemView.civ_item_game_mv_icon.layoutParams
                val videoCoverHeight = itemData.video_cover_height!!
                val videoCoverWidth = itemData.video_cover_width!!
                val screenWidth = PhoneInfoUtils.getWidth(activity as FavActivity)
                layoutParams.width = (120f / 360 * screenWidth).toInt()
                val realHeight =
                    (110f / 360 * screenWidth / videoCoverWidth * videoCoverHeight).toInt()
                val maxHeight = (190f / 360 * screenWidth).toInt()
                layoutParams.height = Math.min(realHeight, maxHeight)
                itemView.civ_item_game_mv_icon.layoutParams = layoutParams

                val rootLayoutParams = itemView.rl_item_game_mv.layoutParams
                rootLayoutParams.width = (120f / 360 * screenWidth).toInt()
                rootLayoutParams.height =
                    if (realHeight < maxHeight * 3 / 4) maxHeight * 9 / 10
                    else maxHeight
                itemView.rl_item_game_mv.layoutParams = rootLayoutParams

                Glide.with(this)
                    .load(itemData.video_cover)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.civ_item_game_mv_icon)

                itemView.civ_item_game_mv_icon.isNeedRadius(realHeight >= maxHeight)

                itemView.tv_item_game_mv_share.text = "${itemData.total_share}"
                itemView.tv_item_game_mv_msg.text = "${itemData.total_comment}"
                itemView.tv_item_game_mv_good.text = "${itemData.total_like}"
                itemView.tv_item_game_mv_look.text = "${itemData.total_view}"

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val intent = Intent(activity as FavActivity, VideoActivity::class.java)
                        intent.putExtra(VideoActivity.VIDEO_TYPE, 1)
                        intent.putExtra(
                            VideoActivity.VIDEO_ID,
                            itemData.video_id
                        )
                        intent.putExtra(VideoActivity.VIDEO_COVER, itemData.video_cover)
                        intent.putExtra(VideoActivity.VIDEO_COVER_WIDTH, itemData.video_cover_width)
                        intent.putExtra(
                            VideoActivity.VIDEO_COVER_HEIGHT,
                            itemData.video_cover_height
                        )
                        intent.putExtra(VideoActivity.IS_MAIN_VIDEO, false)
                        startActivity(intent)
                    }
            }.create()
        mView?.rv_fav_video?.adapter = adapter
        mView?.rv_fav_video?.layoutManager =
            SafeStaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
    }

    override fun onDestroy() {
        collectVideoListObservable?.dispose()
        collectVideoListObservable = null
        super.onDestroy()
    }
}