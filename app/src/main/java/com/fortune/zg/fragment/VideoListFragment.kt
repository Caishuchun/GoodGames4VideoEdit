package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.bean.GameListWithMvBean
import com.fortune.zg.bean.VideoIdListBean
import com.fortune.zg.event.PageScroll
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.video.VideoActivity
import com.fortune.zg.video.VideoIdListUtil
import com.fortune.zg.widget.VideoStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_video_list.view.*
import kotlinx.android.synthetic.main.item_video_list.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

private const val TYPE = "type"

class VideoListFragment : Fragment() {
    private var mView: View? = null
    private var type: Int = 0

    private var videoListObservable: Disposable? = null
    private var timer: Disposable? = null
    private var gameLists = mutableListOf<GameListWithMvBean.DataBean.ListBean>()
    lateinit var adapter: BaseAdapterWithPosition<GameListWithMvBean.DataBean.ListBean>
    private var currentPage = 1
    private var countPage = -1
    private var videoIdListObservable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = it.getInt(TYPE)
        }
    }

    companion object {
        fun newInstance(type: Int) = VideoListFragment().apply {
            arguments = Bundle().apply {
                putInt(TYPE, type)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_video_list, container, false)
        EventBus.getDefault().register(this)
        initView()
        getInfo(needLoading = true, isRefresh = false)
        timer = Observable.interval(5, 5, TimeUnit.MINUTES)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (!MyApp.isBackground) {
                    currentPage = 1
                    mView?.refresh_video_list?.finishRefresh()
                    mView?.refresh_video_list?.finishLoadMore()
                    getInfo(needLoading = false, isRefresh = false)
                }
            }
        return mView
    }

    /**
     * 获取数据
     */
    @SuppressLint("CheckResult")
    private fun getInfo(needLoading: Boolean, isRefresh: Boolean) {
        if (needLoading) {
            MainActivityV5.getInstance()?.let { DialogUtils.showBeautifulDialog(it) }
        }
        val videoList = RetrofitUtils.builder().VideoList(type, currentPage)
        videoListObservable = videoList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                MobclickAgent.onEvent(
                    MainActivityV5.getInstance(),
                    "video_view",
                    "${getTypeString()}$currentPage"
                )
                if (needLoading) {
                    DialogUtils.dismissLoading()
                }
                if (currentPage == 1) {
                    (parentFragment as VideoFragment).hideNewVideo()
                }
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (isRefresh) {
                    mView?.refresh_video_list?.finishRefresh()
                } else {
                    mView?.refresh_video_list?.finishLoadMore()
                }
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (currentPage == 1) {
                                gameLists.clear()
                                adapter.notifyDataSetChanged()
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
                                val step = mutableListOf<GameListWithMvBean.DataBean.ListBean>()
                                for (data in it.getData()?.list!!) {
                                    if (data.item_type != "live") {
                                        step.add(data)
                                    }
                                }
                                gameLists.addAll(step)
                                adapter.notifyDataSetChanged()
                            }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            MainActivityV5.getInstance()?.let { it1 ->
                                ActivityManager.toSplashActivity(
                                    it1
                                )
                            }
                        }
                        else -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                if (isRefresh) {
                    mView?.refresh_video_list?.finishRefresh()
                } else {
                    mView?.refresh_video_list?.finishLoadMore()
                }
                if (needLoading) {
                    DialogUtils.dismissLoading()
                }
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(MainActivityV5.getInstance()?.let { it1 ->
                    HttpExceptionUtils.getExceptionMsg(
                        it1, it
                    )
                }!!)
            })
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initView() {
        mView?.refresh_video_list?.setEnableRefresh(true)
        mView?.refresh_video_list?.setEnableLoadMore(true)
        mView?.refresh_video_list?.setEnableLoadMoreWhenContentNotFull(false)
        mView?.refresh_video_list?.setRefreshHeader(MaterialHeader(activity))
        mView?.refresh_video_list?.setRefreshFooter(ClassicsFooter(activity))

        mView?.refresh_video_list?.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                currentPage = 1
                getInfo(needLoading = false, isRefresh = true)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    getInfo(needLoading = false, isRefresh = false)
                } else {
                    mView?.refresh_video_list?.finishLoadMoreWithNoMoreData()
                }
            }
        })
        adapter = BaseAdapterWithPosition.Builder<GameListWithMvBean.DataBean.ListBean>()
            .setData(gameLists)
            .setLayoutId(R.layout.item_video_list)
            .addBindView { itemView, itemData, position ->
                when (itemData.item_type) {
                    "video" -> {
                        itemView.tv_item_game_mv_des.text = itemData.video_name

                        val videoCoverHeight = itemData.video_cover_height!!
                        val videoCoverWidth = itemData.video_cover_width!!

                        val layoutParams = itemView.civ_item_game_mv_icon.layoutParams
                        val screenWidth = MainActivityV5.getInstance()?.let {
                            PhoneInfoUtils.getWidth(
                                it
                            )
                        }!!
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

                        if (itemData.total_share == 0) {
                            itemView.ll_item_game_mv_share.visibility = View.GONE
                        } else {
                            itemView.ll_item_game_mv_share.visibility = View.VISIBLE
                            itemView.tv_item_game_mv_share.text = "${itemData.total_share}"
                        }
                        if (itemData.total_comment == 0) {
                            itemView.ll_item_game_mv_msg.visibility = View.GONE
                        } else {
                            itemView.ll_item_game_mv_msg.visibility = View.VISIBLE
                            itemView.tv_item_game_mv_msg.text = "${itemData.total_comment}"
                        }
                        if (itemData.total_like == 0) {
                            itemView.ll_item_game_mv_good.visibility = View.GONE
                        } else {
                            itemView.ll_item_game_mv_good.visibility = View.VISIBLE
                            itemView.tv_item_game_mv_good.text = "${itemData.total_like}"
                        }
                        if (itemData.total_view == 0) {
                            itemView.ll_item_game_mv_look.visibility = View.GONE
                        } else {
                            itemView.ll_item_game_mv_look.visibility = View.VISIBLE
                            itemView.tv_item_game_mv_look.text = "${itemData.total_view}"
                        }

                        RxView.clicks(itemView.rootView)
                            .throttleFirst(200, TimeUnit.MILLISECONDS)
                            .subscribe {
                                toGetVideoList(itemData.video_id!!)
                            }
                    }
                }
            }
            .create()
        mView?.rv_video_list?.adapter = adapter
        mView?.rv_video_list?.layoutManager =
            VideoStaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)

        mView?.rv_video_list?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val mFirstVisibleItems: IntArray? = null
                val position =
                    (mView?.rv_video_list?.layoutManager as StaggeredGridLayoutManager)
                        .findFirstVisibleItemPositions(mFirstVisibleItems)
                LogUtils.d("onScrolled=>position:${position[0]}")
                if (currentPage == 1 || position[0] > 12 * (currentPage - 1)) {
                    if (currentPage < countPage) {
                        currentPage++
                        getInfo(needLoading = false, isRefresh = false)
                    } else {
                        MobclickAgent.onEvent(
                            MainActivityV5.getInstance(),
                            "video_view",
                            "${getTypeString()}over"
                        )
                        mView?.refresh_video_list?.finishLoadMoreWithNoMoreData()
                    }
                }
            }
        })
    }

    /**
     * 获取统计文字
     */
    private fun getTypeString() = when (type) {
        1 -> {
            "video_mv_all_"
        }
        2 -> {
            "video_mv_all_"
        }
        3 -> {
            "video_mv_all_"
        }
        else -> {
            "video_mv_all_"
        }
    }

    /**
     * 获取视频Id列表
     */
    private fun toGetVideoList(videoId: Int) {
        val videoIdList = RetrofitUtils.builder().videoIdList(videoId, type = type)
        videoIdListObservable = videoIdList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (it.data != null) {
                                val position = it.data.position
                                if (position == 0) {
                                    //没找到,说明该视频应该是删除了
                                    ToastUtils.show("该视频已删除")
                                } else {
                                    MobclickAgent.onEvent(
                                        MainActivityV5.getInstance(),
                                        "video_view",
                                        "video_mv"
                                    )
                                    val intent = Intent(activity, VideoActivity::class.java)
                                    intent.putExtra(VideoActivity.VIDEO_TYPE, type)
                                    intent.putExtra(VideoActivity.VIDEO_ID, videoId)
                                    intent.putExtra(
                                        VideoActivity.VIDEO_COVER,
                                        it.data.video_list[position - 1].video_cover
                                    )
                                    intent.putExtra(
                                        VideoActivity.VIDEO_COVER_WIDTH,
                                        it.data.video_list[position - 1].video_cover_width
                                    )
                                    intent.putExtra(
                                        VideoActivity.VIDEO_COVER_HEIGHT,
                                        it.data.video_list[position - 1].video_cover_height
                                    )
                                    intent.putExtra(VideoActivity.IS_MAIN_VIDEO, true)
//                                    intent.putExtra(
//                                        VideoActivity.VIDEO_LIST,
//                                        it.data.video_list as Serializable
//                                    )
                                    VideoIdListUtil.setVideoIdList(it.data.video_list as MutableList<VideoIdListBean.Data.Video>)
                                    intent.putExtra(
                                        VideoActivity.CURRENT_POSITION,
                                        position
                                    )
                                    intent.putExtra(
                                        VideoActivity.DEFAULT_ID,
                                        it.data.default_video_id
                                    )
                                    startActivity(intent)
                                }
                            } else {
                                ToastUtils.show("数据异常,请稍后重试")
                            }
                        }
                        -1 -> {
                            it.msg.let { it1 -> ToastUtils.show(it1) }
                            MainActivityV5.getInstance()?.let { it1 ->
                                ActivityManager.toSplashActivity(
                                    it1
                                )
                            }
                        }
                        else -> {
                            it.msg.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.printStackTrace()}")
                ToastUtils.show(
                    MainActivityV5.getInstance()?.let { it1 ->
                        HttpExceptionUtils.getExceptionMsg(
                            it1, it
                        )
                    }!!
                )
            })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun pageScroll(pageScroll: PageScroll) {
        val position = pageScroll.position
        mView?.rv_video_list?.smoothScrollToPosition(position)
        if (position >= gameLists.size - 3) {
            currentPage++
            getInfo(needLoading = false, isRefresh = false)
        }
    }

    override fun onDestroy() {
        videoListObservable?.dispose()
        videoListObservable = null

        videoIdListObservable?.dispose()
        videoIdListObservable = null
        super.onDestroy()
    }
}