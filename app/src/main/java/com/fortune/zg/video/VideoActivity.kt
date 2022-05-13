package com.fortune.zg.video

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.OrientationHelper
import cn.jzvd.Jzvd
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.VideoIdListBean
import com.fortune.zg.event.PageScroll
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.fragment_mv_detail.view.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class VideoActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: VideoActivity

        const val VIDEO_ID = "video_id"
        const val VIDEO_COVER = "video_cover"
        const val VIDEO_COVER_WIDTH = "video_cover_width"
        const val VIDEO_COVER_HEIGHT = "video_cover_height"
        const val VIDEO_TYPE = "video_type"
        const val IS_MAIN_VIDEO = "is_main_video"
        const val VIDEO_LIST = "video_list"
        const val CURRENT_POSITION = "current_position"
        const val DEFAULT_ID = "default_id"
    }

    private var mLayoutManager: PageLayoutManager? = null
    private var mAdapter: VideoAdapter? = null

    private var videoId = -1
    private var videoCover = ""
    private var videoCoverWidth = 0
    private var videoCoverHeight = 0
    private var videoType = 1
    private var isMainVideo = false
    private var videoList = mutableListOf<VideoIdListBean.Data.Video>()
    private var currentPosition = 0
    private var defaultVideoId = 0

    private var mvInfoObservable: Disposable? = null

    override fun getLayoutId() = R.layout.activity_video

    override fun doSomething() {
        instance = this

        videoId = intent.getIntExtra(VIDEO_ID, -1)
        videoCover = intent.getStringExtra(VIDEO_COVER).toString()
        videoCoverWidth = intent.getIntExtra(VIDEO_COVER_WIDTH, 1)
        videoCoverHeight = intent.getIntExtra(VIDEO_COVER_HEIGHT, 1)
        videoType = intent.getIntExtra(VIDEO_TYPE, 1)
        isMainVideo = intent.getBooleanExtra(IS_MAIN_VIDEO, false)
        if (isMainVideo) {
            videoList = VideoIdListUtil.getVideoIdList()
//                intent.getSerializableExtra(VIDEO_LIST) as MutableList<VideoIdListBean.Data.Video>
            currentPosition = intent.getIntExtra(CURRENT_POSITION, 0) - 1
            defaultVideoId = intent.getIntExtra(DEFAULT_ID, 0)
        } else {
            val videoInfo = VideoIdListBean.Data.Video(
                videoCover,
                videoCoverWidth,
                videoCoverHeight,
                videoId
            )
            videoList.add(videoInfo)
        }

        initView()
        initListener()
    }

    private fun initView() {
        mLayoutManager = PageLayoutManager(this, OrientationHelper.VERTICAL, false)
        mAdapter = VideoAdapter(this, defaultVideoId, videoList, videoType)
        rv_video.layoutManager = mLayoutManager
        rv_video.adapter = mAdapter

        rv_video.scrollToPosition(currentPosition)
        //Recycle绘制结束之后再获取当前控件
        rv_video.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val itemView = mLayoutManager?.findViewByPosition(currentPosition)
                toGetVideoInfo(videoId.toString(), itemView!!, currentPosition)
                //操作结束移除
                rv_video.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }


    private fun initListener() {
        mLayoutManager?.setOnViewPagerListener(object : OnViewPagerListener {
            override fun onPageSelected(itemView: View, position: Int) {
                EventBus.getDefault().postSticky(PageScroll(position))
                val videoId = itemView.tv_mv_detail_num.text.toString()
                toGetVideoInfo(videoId, itemView, position)
            }

            override fun onPageRelease(itemView: View, position: Int) {
                val holder = mAdapter?.ViewHolder(itemView)!!
                holder.ivUserAvatar.setImageResource(R.mipmap.icon)
                holder.tvUserName.text = ""
                holder.tvUpdateTime.text = ""
                holder.tvVideoTitle.text = ""
                holder.ivGameGift.visibility = View.GONE
                holder.rlGameDownload.visibility = View.GONE
                holder.llGameShortUrl.visibility = View.GONE
                OtherUtils.hindKeyboard(this@VideoActivity, holder.etDanmuContent)
                mAdapter?.releaseView(itemView)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
        mAdapter?.onResume()
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
        mAdapter?.onPause()
    }

    /**
     * 获取视频详情
     */
    private fun toGetVideoInfo(videoId: String, itemView: View, position: Int) {
        val mvInfo = RetrofitUtils.builder().mvInfo(videoId)
        mvInfoObservable = mvInfo.subscribeOn(Schedulers.io())
            .throttleFirst(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            mAdapter?.setVideoInfo(it.getData()!!, itemView, position)
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                        3 -> {
                            //视频不存在
                            toGetVideoInfo(defaultVideoId.toString(), itemView, position)
                        }
                        else -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.printStackTrace()}")
                ToastUtils.show(
                    HttpExceptionUtils.getExceptionMsg(this, it)
                )
            })
    }

    override fun destroy() {
        Jzvd.releaseAllVideos()

        mAdapter?.exit()

        mvInfoObservable?.dispose()
        mvInfoObservable = null
    }
}