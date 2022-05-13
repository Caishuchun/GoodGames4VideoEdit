package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.activity.CommentDetailActivityV4
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.bean.MvListBean
import com.fortune.zg.bean.VideoIdListBean
import com.fortune.zg.event.PageScroll
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.video.VideoActivity
import com.fortune.zg.video.VideoIdListUtil
import com.fortune.zg.widget.VideoLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_mv.view.*
import kotlinx.android.synthetic.main.item_mv.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class MvFragment : Fragment() {

    private var mView: View? = null
    private var mAdapter: BaseAdapterWithPosition<MvListBean.DataBean.ListBean>? = null
    private var mData = mutableListOf<MvListBean.DataBean.ListBean>()
    private var currentPage = 1
    private var countPage = 0
    private var mvListObservable: Disposable? = null
    private var mvShareObservable: Disposable? = null
    private var mvLikeObservable: Disposable? = null
    private var isShare = false
    private var currentVideoId = -1
    private var currentShareNumText: TextView? = null
    private var videoIdListObservable: Disposable? = null

    companion object {
        @JvmStatic
        fun newInstance() = MvFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_mv, container, false)
        EventBus.getDefault().register(this)
        initView()
        getInfo()
        return mView
    }

    private fun getInfo(needLoading: Boolean = true, isRefresh: Boolean = false) {
        if (needLoading) {
            DialogUtils.showBeautifulDialog(activity as MainActivityV5)
        }
        val mvList = RetrofitUtils.builder().mvList("", currentPage)
        mvListObservable = mvList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (isRefresh) {
                    mView?.refresh_mv_list?.finishRefresh()
                } else {
                    mView?.refresh_mv_list?.finishLoadMore()
                }
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (currentPage == 1) {
                                mData.clear()
                                mAdapter?.notifyDataSetChanged()
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
                                    mData.addAll(list)
                                }
                                mAdapter?.notifyDataSetChanged()
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
                    mView?.refresh_mv_list?.finishRefresh()
                } else {
                    mView?.refresh_mv_list?.finishLoadMore()
                }
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as MainActivityV5, it))
            })
    }

    @SuppressLint("SimpleDateFormat", "CheckResult")
    private fun initView() {
        mAdapter = BaseAdapterWithPosition.Builder<MvListBean.DataBean.ListBean>()
            .setData(mData)
            .setLayoutId(R.layout.item_mv)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData.user_avatar)
                    .placeholder(R.mipmap.bg_gray_6)
                    .into(itemView.riv_mv_headIcon)
                itemView.tv_mv_userName.text =
                    if (itemData.user_name == "") getString(R.string.app_name) else itemData.user_name
                itemView.tv_mv_title.text = itemData.video_name

                Glide.with(this)
                    .load(itemData.video_cover)
                    .placeholder(R.mipmap.bg_gray_6)
                    .into(itemView.iv_mv_poster)
                val width = itemData.video_cover_width!!
                val height = itemData.video_cover_height!!
                val newWidth: Int
                val newHeight: Int
                val layoutParams = itemView.rl_mv_root.layoutParams
                val screenWidth =
                    PhoneInfoUtils.getWidth(activity as MainActivityV5).toDouble()
                if (width > height) {
                    newWidth = (screenWidth / 360f * 328).toInt()
                    newHeight = (newWidth.toFloat() / width * height).toInt()
                } else {
                    newWidth = (screenWidth / 360f * 150).toInt()
                    newHeight = (newWidth.toFloat() / width * height).toInt()
                }
                layoutParams.width = newWidth
                layoutParams.height = newHeight
                itemView.rl_mv_root.layoutParams = layoutParams
                itemView.tv_mv_updateTime.text = itemData.video_update_time
                if (itemData.video_update_time?.contains(":") == true) {
                    itemView.tv_mv_updateTime.text = itemData.video_update_time
                } else {
                    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    itemView.tv_mv_updateTime.text =
                        simpleDateFormat.format(itemData.video_update_time!!.toLong() * 1000)
                }
                itemView.tv_mv_share_num.text = "${itemData.total_share}"
                itemView.tv_mv_msg_num.text = "${itemData.total_comment}"
                itemView.tv_mv_good_num.text = "${itemData.total_like}"
                itemView.tv_mv_look_num.text = "${itemData.total_view}"

                var isLike = itemData.is_like != 0
                itemView.iv_mv_good.setImageResource(if (isLike) R.mipmap.mv_icon_good_focus else R.mipmap.mv_icon_good)

                RxView.clicks(itemView.iv_mv_play)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toGetVideoList(itemData.video_id!!)
                    }

                RxView.clicks(itemView.iv_mv_poster)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toGetVideoList(itemData.video_id!!)
                    }

                RxView.clicks(itemView.iv_mv_share)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        currentVideoId = itemData.video_id!!
                        currentShareNumText = itemView.tv_mv_share_num!!
                        BottomDialog.shareMV(
                            activity as MainActivityV5,
                            itemData.video_id.toString(),
                            itemData.video_name!!,
                            itemData.video_desc!!,
                            itemData.video_cover!!,
                            object : BottomDialog.IsClickItem {
                                override fun isClickItem() {
                                    isShare = true
                                }
                            }
                        )
                    }


                RxView.clicks(itemView.iv_mv_msg)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toCommonDetailActivity(
                            true,
                            itemData.video_id!!.toString(),
                            itemData.video_cover!!,
                            itemData.video_cover!!
                        )
                    }

                RxView.clicks(itemView.iv_mv_good)
                    .throttleFirst(
                        if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                        TimeUnit.MILLISECONDS
                    ).subscribe {
                        if (MyApp.getInstance().isHaveToken()) {
                            isLike = !isLike
                            currentVideoId = itemData.video_id!!
                            itemView.iv_mv_good.setImageResource(if (isLike) R.mipmap.mv_icon_good_focus else R.mipmap.mv_icon_good)
                            itemView.tv_mv_good_num.text =
                                if (isLike) (itemView.tv_mv_good_num.text.toString().trim()
                                    .toInt() + 1).toString()
                                else (itemView.tv_mv_good_num.text.toString().trim()
                                    .toInt() - 1).toString()
                            toAddLikeNum(if (isLike) 0 else 1)
                        } else {
                            LoginUtils.toQuickLogin(activity as MainActivityV5)
                        }
                    }
            }.create()

        mView?.cv_mv_list?.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.set(10, 10, 10, 10)
            }
        })
        mView?.cv_mv_list?.adapter = mAdapter
        mView?.cv_mv_list?.layoutManager = VideoLinearLayoutManager(activity)

        mView?.refresh_mv_list?.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                currentPage = 1
                getInfo(needLoading = false, isRefresh = true)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    getInfo(needLoading = false, isRefresh = false)
                } else {
                    mView?.refresh_mv_list?.finishLoadMoreWithNoMoreData()
                }
            }
        })
    }

    private fun toGetVideoList(videoId: Int) {
        val videoIdList = RetrofitUtils.builder().videoIdList(videoId, type = 1)
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
                                    intent.putExtra(VideoActivity.VIDEO_TYPE, 1)
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

    /**
     * 增加/减少点赞数量
     */
    private fun toAddLikeNum(is_cancel: Int) {
        val mvLike = RetrofitUtils.builder().mvLike(currentVideoId.toString(), is_cancel)
        mvLikeObservable = mvLike.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${Gson().toJson(it)}")
            })
    }

    override fun onResume() {
        super.onResume()
        if (isShare) {
            isShare = false
            toAddShareNum()
        }
    }

    /**
     * 增加分享次数
     */
    private fun toAddShareNum() {
        val mvShare = RetrofitUtils.builder().mvShare(currentVideoId.toString())
        mvShareObservable = mvShare.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                currentShareNumText?.text = (currentShareNumText?.text?.toString()?.trim()?.toInt()
                    ?.plus(1)).toString()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${Gson().toJson(it)}")
            })
    }

    /**
     * 跳转到评论详情
     */
    private fun toCommonDetailActivity(
        isMainVideo: Boolean,
        videoId: String,
        videoName: String,
        videoDesc: String
    ) {
        val intent = Intent(activity, CommentDetailActivityV4::class.java)
        intent.putExtra(CommentDetailActivityV4.SYSTEM, 1)
        intent.putExtra(CommentDetailActivityV4.IS_MAIN_VIDEO, isMainVideo)
        intent.putExtra(CommentDetailActivityV4.VIDEO_ID, videoId)
        intent.putExtra(CommentDetailActivityV4.VIDEO_NAME, videoName)
        intent.putExtra(CommentDetailActivityV4.VIDEO_DESC, videoDesc)
        intent.putExtra(CommentDetailActivityV4.VIDEO_LIST, "")
        intent.putExtra(CommentDetailActivityV4.IMAGE_LIST, "")
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        mvListObservable?.dispose()
        mvListObservable = null

        mvShareObservable?.dispose()
        mvShareObservable = null

        videoIdListObservable?.dispose()
        videoIdListObservable = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageScroll(pageScroll: PageScroll) {
        val position = pageScroll.position
        mView?.cv_mv_list?.smoothScrollToPosition(position)
        if (position >= mData.size - 4) {
            currentPage++
            getInfo(needLoading = false, isRefresh = false)
        }
    }
}