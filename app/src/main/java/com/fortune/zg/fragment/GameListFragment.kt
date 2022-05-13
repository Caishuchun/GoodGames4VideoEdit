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
import com.fortune.zg.activity.GameDetailActivity
import com.fortune.zg.activity.LiveActivity
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.adapter.BaseAdapter
import com.fortune.zg.bean.GameListWithMvBean
import com.fortune.zg.bean.VideoIdListBean
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.video.VideoActivity
import com.fortune.zg.video.VideoIdListUtil
import com.fortune.zg.widget.SafeStaggeredGridLayoutManager
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
import kotlinx.android.synthetic.main.fragment_game_list.view.*
import kotlinx.android.synthetic.main.layout_game_item.view.*
import kotlinx.android.synthetic.main.layout_item_tag.view.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

private const val SYSTEM = "system"

class GameListFragment : Fragment() {

    private var mView: View? = null
    private var gameListObservable: Disposable? = null
    private var videoIdListObservable: Disposable? = null
    private var timer: Disposable? = null
    private var gameLists = mutableListOf<GameListWithMvBean.DataBean.ListBean>()

    lateinit var adapter: BaseAdapter<GameListWithMvBean.DataBean.ListBean>

    private var currentPage = 1
    private var countPage = -1

    private var system: Int = 0

    @SuppressLint("SimpleDateFormat")
    private val df = SimpleDateFormat("MM.dd")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            system = it.getInt(SYSTEM)
        }
    }

    companion object {
        fun newInstance(system: Int) = GameListFragment().apply {
            arguments = Bundle().apply {
                putInt(SYSTEM, system)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_game_list, container, false)
        initView()
        getInfo(needLoading = true, isRefresh = false)
        timer = Observable.interval(5, 5, TimeUnit.MINUTES)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (!MyApp.isBackground) {
                    currentPage = 1
                    mView?.refresh_game_list?.finishRefresh()
                    mView?.refresh_game_list?.finishLoadMore()
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
            DialogUtils.showBeautifulDialog(activity as MainActivityV5)
        }
        val gameList = RetrofitUtils.builder().gameList(system, currentPage)
        gameListObservable = gameList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (needLoading) {
                    DialogUtils.dismissLoading()
                }
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (isRefresh) {
                    mView?.refresh_game_list?.finishRefresh()
                } else {
                    mView?.refresh_game_list?.finishLoadMore()
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
                                it.getData()?.list?.let { list ->
                                    gameLists.addAll(list)
                                }
                                adapter.notifyDataSetChanged()
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
                if (isRefresh) {
                    mView?.refresh_game_list?.finishRefresh()
                } else {
                    mView?.refresh_game_list?.finishLoadMore()
                }
                if (needLoading) {
                    DialogUtils.dismissLoading()
                }
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as MainActivityV5, it))
            })
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initView() {
        mView?.refresh_game_list?.setEnableRefresh(true)
        mView?.refresh_game_list?.setEnableLoadMore(true)
        mView?.refresh_game_list?.setEnableLoadMoreWhenContentNotFull(false)
        mView?.refresh_game_list?.setRefreshHeader(MaterialHeader(activity))
        mView?.refresh_game_list?.setRefreshFooter(ClassicsFooter(activity))

        mView?.refresh_game_list?.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                currentPage = 1
                MobclickAgent.onEvent(activity, "get_game_info", "${getCurrentPage(system)}_info_1")
                getInfo(needLoading = false, isRefresh = true)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    MobclickAgent.onEvent(
                        activity,
                        "get_game_info",
                        "${getCurrentPage(system)}_info_$currentPage"
                    )
                    getInfo(needLoading = false, isRefresh = false)
                } else {
                    MobclickAgent.onEvent(
                        activity,
                        "get_game_info",
                        "${getCurrentPage(system)}_info_over"
                    )
                    mView?.refresh_game_list?.finishLoadMoreWithNoMoreData()
                }
            }
        })
        adapter = BaseAdapter.Builder<GameListWithMvBean.DataBean.ListBean>()
            .setData(gameLists)
            .setLayoutId(R.layout.layout_game_item)
            .addBindView { itemView, itemData ->
                if (itemData.item_type == "video") {
                    itemView.ll_item_game_normal?.visibility = View.GONE
                    itemView.ll_item_game_live?.visibility = View.GONE
                    itemView.rl_item_game_mv?.visibility = View.VISIBLE
                    itemView.tv_item_game_mv_des.text = itemData.video_name

                    val layoutParams = itemView.civ_item_game_mv_icon.layoutParams
                    val videoCoverHeight = itemData.video_cover_height!!
                    val videoCoverWidth = itemData.video_cover_width!!
                    val screenWidth = PhoneInfoUtils.getWidth(activity as MainActivityV5)
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
//                        .placeholder(R.mipmap.bg_gray_6)
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

                    itemView.tv_item_game_mv_share.text = "${itemData.total_share}"
                    itemView.tv_item_game_mv_msg.text = "${itemData.total_comment}"
                    itemView.tv_item_game_mv_good.text = "${itemData.total_like}"
                    itemView.tv_item_game_mv_look.text = "${itemData.total_view}"

                    RxView.clicks(itemView.rootView)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toGetVideoList(itemData.video_id!!)
                        }
                } else if (itemData.item_type == "live") {
                    itemView.ll_item_game_normal?.visibility = View.GONE
                    itemView.ll_item_game_live?.visibility = View.VISIBLE
                    itemView.rl_item_game_mv?.visibility = View.GONE
                    Glide.with(this)
                        .load(itemData.cover)
                        .placeholder(R.mipmap.bg_gray_6)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(itemView.civ_item_live_cover)
                    itemView.iv_item_live_status.visibility =
                        if (itemData.status == 1) View.VISIBLE else View.GONE
                    itemView.tv_item_live_liver.text = itemData.anchor_name
                    val random = (11..14).random()
                    itemView.tv_item_live_online.text = "${(itemData.online_user!! + 1) * random}人"
                    itemView.tv_item_live_title.text = itemData.intro
                    itemView.tv_item_live_tag.text = itemData.tag

                    RxView.clicks(itemView.rootView)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            val intent = Intent(activity, LiveActivity::class.java)
                            intent.putExtra(LiveActivity.LIVE_ID, itemData.live_id)
                            intent.putExtra(LiveActivity.IM_GROUP_ID, itemData.im_group_id)
                            startActivity(intent)
                        }
                } else {
                    itemView.ll_item_game_normal?.visibility = View.VISIBLE
                    itemView.ll_item_game_live?.visibility = View.GONE
                    itemView.rl_item_game_mv?.visibility = View.GONE
                    Glide.with(this)
                        .load(itemData.game_cover)
                        .placeholder(R.mipmap.bg_gray_6)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(itemView.civ_item_game_icon)
                    itemView.tv_item_game_hot.text = itemData.game_hits.toString()
                    itemView.tv_item_game_name.text = itemData.game_name
                    itemView.tv_item_game_type.text = getSystem(itemData.game_system!!)
                    itemView.tv_item_game_type.setBackgroundResource(
                        if (itemData.game_system == 1) R.drawable.bg_game_type_purple
                        else R.drawable.bg_game_type_blue
                    )
                    itemView.tv_item_game_updateTime.text =
                        "${df.format(itemData.game_update_time!! * 1000L)}${getString(R.string.update)}"

                    if (null == itemData.game_gift_last) {
                        itemView.iv_item_game_gift.visibility = View.GONE
                        itemView.tv_item_game_gift_num.visibility = View.GONE
                    } else {
                        itemView.iv_item_game_gift.visibility = View.VISIBLE
                        itemView.tv_item_game_gift_num.visibility = View.VISIBLE
                        itemView.tv_item_game_gift_num.text = "x${itemData.game_gift_last}"
                    }

                    itemView.fl_item_game.removeAllViews()
                    for (index in itemData.game_tag!!.indices) {
                        val view =
                            LayoutInflater.from(activity)
                                .inflate(R.layout.layout_item_tag, null)
                        view.tv_tag.text = itemData.game_tag!![index]
                        itemView.fl_item_game.addView(view)
                    }

                    itemView.iv_item_game_badge.setImageResource(
                        if (itemData.game_badge == "new") R.mipmap.game_new
                        else R.mipmap.good
                    )
                    itemView.tv_item_game_des.text = itemData.game_desc

                    RxView.clicks(itemView.rootView)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            MobclickAgent.onEvent(
                                activity,
                                "game_detail",
                                "game_detail_from_${getCurrentPage(system)}"
                            )
                            val intent =
                                Intent(
                                    activity as MainActivityV5,
                                    GameDetailActivity::class.java
                                )
                            intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id!!)
                            intent.putExtra(GameDetailActivity.GAME_COVER, itemData.game_cover)
                            intent.putExtra(GameDetailActivity.GAME_BADGE, itemData.game_badge)
                            startActivityForResult(intent, 2021)
                        }
                }
            }
            .create()
        mView?.cv_game_list?.adapter = adapter
        mView?.cv_game_list?.layoutManager =
            SafeStaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)

        mView?.cv_game_list?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val mFirstVisibleItems: IntArray? = null
                val position =
                    (mView?.cv_game_list?.layoutManager as StaggeredGridLayoutManager)
                        .findFirstVisibleItemPositions(mFirstVisibleItems)
                LogUtils.d("onScrolled=>position:${position[0]}")
                if (currentPage == 1 || position[0] >= 12 * (currentPage - 1)) {
                    if (currentPage < countPage) {
                        currentPage++
                        MobclickAgent.onEvent(
                            activity,
                            "get_game_info",
                            "${getCurrentPage(system)}_info_$currentPage"
                        )
                        getInfo(needLoading = false, isRefresh = false)
                    } else {
                        MobclickAgent.onEvent(
                            activity,
                            "get_game_info",
                            "${getCurrentPage(system)}_info_over"
                        )
                        mView?.refresh_game_list?.finishLoadMoreWithNoMoreData()
                    }
                }
            }
        })
    }

    /**
     * 获取视频Id列表
     */
    private fun toGetVideoList(videoId: Int) {
        val videoIdList = RetrofitUtils.builder().videoIdList(videoId, system)
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
                                        activity as MainActivityV5,
                                        "video_view",
                                        "video_home"
                                    )
                                    val intent = Intent(activity, VideoActivity::class.java)
                                    intent.putExtra(VideoActivity.VIDEO_TYPE, system)
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
                            ActivityManager.toSplashActivity(activity as MainActivityV5)
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
                    HttpExceptionUtils.getExceptionMsg(activity as MainActivityV5, it)
                )
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2021 && resultCode == 2022 && data != null) {
            val gameId = data.getIntExtra(GameDetailActivity.GAME_ID, -1)
            for (gameInfo in gameLists) {
                if (gameInfo.game_id == gameId) {
                    gameLists.remove(gameInfo)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    /**
     * 格式化系统类型
     */
    private fun getSystem(type: Int) = when (type) {
        1 -> getString(R.string.pc)
        else -> getString(R.string.phone)
    }

    /**
     * 获取当前页面
     */
    private fun getCurrentPage(system: Int) =
        when (system) {
            1 -> "all"
            2 -> "pc"
            else -> "phone"
        }

    override fun onDestroy() {
        gameListObservable?.dispose()
        timer?.dispose()

        gameListObservable = null
        timer = null

        videoIdListObservable?.dispose()
        videoIdListObservable = null
        super.onDestroy()
    }
}