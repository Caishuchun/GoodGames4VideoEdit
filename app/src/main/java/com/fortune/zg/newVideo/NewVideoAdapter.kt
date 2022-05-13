package com.fortune.zg.newVideo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.Jzvd
import cn.jzvd.JzvdStd
import com.arialyy.aria.core.Aria
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.activity.CommentDetailActivityV4
import com.fortune.zg.activity.WebActivity
import com.fortune.zg.bean.GameDownloadNotify
import com.fortune.zg.bean.MvGetMsgBean
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.event.DanmuState
import com.fortune.zg.event.GameDownload
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.issue.TimeFormat
import com.fortune.zg.listener.SoftKeyBoardChangeListener
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.widget.MessageOCView
import com.fortune.zg.widget.MyJzvd4Mv
import com.fortune.zg.widget.RoundImageView
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_mv_detail.view.*
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.ui.widget.DanmakuView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class NewVideoAdapter(
    context: Activity,
    data: MutableList<NewVideoListsBean.Data.VideoBean>,
    videoType: Int
) :
    RecyclerView.Adapter<NewVideoAdapter.ViewHolder>() {

    private var mContent = context
    private var mData = data
    private var mVideoType = videoType
    private var collectVideoObservable: Disposable? = null
    private var mvShareObservable: Disposable? = null
    private var giftTimer: Disposable? = null
    private var videoGiftObservable: Disposable? = null
    private var mvSendMsgObservable: Disposable? = null
    private var mvLikeObservable: Disposable? = null
    private var mvGetMsgObservable: Disposable? = null
    private val numberFormat = DecimalFormat("#0.00")

    private var gameDownloadStartObservable: Disposable? = null
    private var gameDownloadCompleteObservable: Disposable? = null
    private var gameInstallStartObservable: Disposable? = null
    private var downId: Int? = null

    private var currentVideoId = 0
    private var currentHolder: NewVideoAdapter.ViewHolder? = null

    private var mDanmakuContext = DanmakuContext.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewVideoAdapter.ViewHolder {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_mv_detail, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("CheckResult")
    override fun onBindViewHolder(holder: NewVideoAdapter.ViewHolder, position: Int) {
        holder.llTopBack.visibility = View.GONE

        val video = mData[position]
        val videoId = video.video_id
        val videoCover = video.video_cover
        val videoCoverWidth = video.video_cover_width!!
        val videoCoverHeight = video.video_cover_height!!
        val screenWidth = PhoneInfoUtils.getWidth(mContent)
        val screenHeight = PhoneInfoUtils.getHeight(mContent)

        holder.videoView.setButtonSize(screenWidth / 360f * 50)

        holder.tvVideoId.text = videoId.toString()
        RxView.clicks(holder.ivBack)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                mContent.finish()
            }

        if (videoCoverWidth != 1) {
            holder.videoView.setPosterParams(
                videoCoverWidth, videoCoverHeight, screenWidth, screenHeight, false
            )
            holder.videoView.setMvLayoutParams(
                videoCoverWidth, videoCoverHeight, screenWidth, screenHeight, false
            )
        }
        Glide.with(mContent)
            .load(videoCover)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.videoView.posterImageView)

        holder.ivGameGift.visibility = View.GONE
        holder.tvGameGift.visibility = View.GONE
        holder.llGameShortUrl.visibility = View.GONE
        holder.rlGameDownload.visibility = View.GONE
        holder.ivUserAvatar.setImageResource(R.mipmap.icon)
        holder.tvVideoTitle.text = "好服多多优质视频"
        holder.tvUpdateTime.text = ""
        holder.tvUserName.text = "好服多多"
        holder.ivVideoFav.setImageResource(R.mipmap.collect_un)
        holder.tvVideoShare.text = "0"
        holder.tvVideoComment.text = "0"
        holder.tvVideoLike.text = "0"
        holder.tvVideoLook.text = "0"
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //上面可以设置好的
        var llTopBack = itemView.ll_mv_detail_top
        val tvVideoId: TextView = itemView.tv_mv_detail_num
        val ivBack: ImageView = itemView.iv_mv_detail_back
        val videoView: MyJzvd4Mv = itemView.jzvd_mv_detail
        val danmuRoot: MessageOCView = itemView.messageOCView_me_detail
        val danmuView: DanmakuView = itemView.danmaku_mv_detail
        val rlEnumView: LinearLayout = itemView.rl_me_detail_enum
        val llEnterView: LinearLayout = itemView.ll_me_detail_message
        val etDanmuContent: EditText = itemView.et_me_detail_message

        //下面随时变的
        val tvSendDanmu: TextView = itemView.tv_me_detail_sendMessage
        val ivUserAvatar: RoundImageView = itemView.riv_mv_detail_headIcon
        val tvUserName: TextView = itemView.tv_mv_detail_userName
        val tvUpdateTime: TextView = itemView.tv_mv_detail_time
        val tvVideoTitle: TextView = itemView.tv_mv_detail_title
        val ivGameGift: ImageView = itemView.iv_mv_detail_gift
        val tvGameGift: TextView = itemView.tv_mv_detail_gift
        val rlGameDownload: RelativeLayout = itemView.rl_mv_detail_download
        val pbGameDownload: ProgressBar = itemView.pb_mv_detail_download_downloadGame
        val ivGameDownloadIcon: ImageView = itemView.iv_mv_detail_download_icon
        val tvGameDownloadMsg: TextView = itemView.tv_mv_detail_download_msg
        val ivGameDownloadCancel: ImageView = itemView.iv_mv_detail_download_cancel
        val llGameShortUrl: LinearLayout = itemView.ll_me_detail_shortUrl
        val tvGameShortUrl: TextView = itemView.tv_me_detail_shortUrl
        val ivVideoFav: ImageView = itemView.iv_mv_detail_fav
        val tvVideoFav: TextView = itemView.tv_mv_detail_collect
        val llVideoShare: LinearLayout = itemView.ll_mv_detail_share
        val tvVideoShare: TextView = itemView.tv_mv_share_detail_num
        val llVideoComment: LinearLayout = itemView.ll_mv_detail_msg
        val tvVideoComment: TextView = itemView.tv_mv_msg_detail_num
        val llVideoLike: LinearLayout = itemView.ll_mv_detail_good
        val ivVideoLike: ImageView = itemView.iv_mv_detail_good
        val tvVideoLike: TextView = itemView.tv_mv_good_detail_num
        val llVideoLook: LinearLayout = itemView.ll_mv_detail_look
        val tvVideoLook: TextView = itemView.tv_mv_look_detail_num
    }

    /**
     * Activity生命周期
     */
    fun onResume() {
        Jzvd.goOnPlayOnResume()
    }

    /**
     * Activity生命周期
     */
    fun onPause() {
        Jzvd.goOnPlayOnPause()
    }

    @SuppressLint("SetTextI18n")
    @Subscribe
    fun onDownload(gameDownload: GameDownload) {
        val holder = currentHolder!!
        val filePath = gameDownload.task?.filePath!!
        val gameName =
            downloadFileUrl.substring(downloadFileUrl.lastIndexOf("/") + 1, downloadFileUrl.length)
        when (gameDownload.state) {
            GameDownload.STATE.START -> {

            }
            GameDownload.STATE.RUNNING -> {
                if (filePath.contains(gameName)) {
                    //说明是当前界面的手游
                    holder.ivGameDownloadCancel.visibility = View.VISIBLE
                    holder.pbGameDownload.progress = gameDownload.task.percent
                    holder.ivGameDownloadIcon.visibility = View.GONE
                    holder.tvGameDownloadMsg.text =
                        "${mContent.getString(R.string.downloading)} ${gameDownload.task.percent}%"
                }
            }
            GameDownload.STATE.PAUSE -> {
                if (filePath.contains(gameName)) {
                    //说明是当前界面的手游
                    holder.ivGameDownloadCancel.visibility = View.VISIBLE
                    holder.pbGameDownload.progress = gameDownload.task.percent
                    holder.ivGameDownloadIcon.visibility = View.GONE
                    holder.tvGameDownloadMsg.text = mContent.getString(R.string.re_download)
                }
            }
            GameDownload.STATE.RESUME -> {

            }
            GameDownload.STATE.COMPLETE -> {
                if (filePath.contains(gameName)) {
                    //说明是当前界面的手游
                    SPUtils.putValue("TASK_ID_$currentVideoId", -1L)
                    holder.ivGameDownloadCancel.visibility = View.GONE
                    holder.pbGameDownload.progress = 100
                    holder.ivGameDownloadIcon.visibility = View.GONE
                    holder.tvGameDownloadMsg.text = mContent.getString(R.string.open_game)
                }
                if (downId != null) {
                    val gameDownComplete = RetrofitUtils.builder().gameDownComplete4Video(downId!!)
                    gameDownloadCompleteObservable =
                        gameDownComplete.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                //正常的也不错
                            }, {
                                //异常的不管
                            })
                    val gameInstallComplete =
                        RetrofitUtils.builder().gameInstallComplete4Video(downId!!)
                    gameInstallStartObservable = gameInstallComplete.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            //正常的也不错
                        }, {
                            //异常的不管
                        })
                }
            }
            GameDownload.STATE.CANCEL -> {
                if (filePath.contains(gameName)) {
                    //说明是当前界面的手游
                    SPUtils.putValue("TASK_ID_$currentVideoId", -1L)
                    holder.ivGameDownloadCancel.visibility = View.GONE
                    holder.pbGameDownload.progress = 0
                    holder.ivGameDownloadIcon.visibility = View.VISIBLE
                    holder.tvGameDownloadMsg.text = "下载游戏"
//                        "下载游戏(${
//                            numberFormat.format(
//                                downloadFileSize.toFloat() / 1024.0 / 1024.0
//                            )
//                        }M)"
                }
            }
            GameDownload.STATE.FAIL -> {

            }
        }
    }

    /**
     * 页面退出的时候
     */
    fun exit() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        collectVideoObservable?.dispose()
        collectVideoObservable = null

        mvShareObservable?.dispose()
        mvShareObservable = null

        giftTimer?.dispose()
        giftTimer = null

        videoGiftObservable?.dispose()
        videoGiftObservable = null

        mvSendMsgObservable?.dispose()
        mvSendMsgObservable = null

        mvLikeObservable?.dispose()
        mvLikeObservable = null

        mvGetMsgObservable?.dispose()
        mvGetMsgObservable = null

        gameDownloadStartObservable?.dispose()
        gameDownloadStartObservable = null

        gameDownloadCompleteObservable?.dispose()
        gameDownloadCompleteObservable = null

        gameInstallStartObservable?.dispose()
        gameInstallStartObservable = null
    }

    /**
     * 获取视频播放进度数据
     */
    private fun getVideoProgressInfo() = VideoProgressInfo(
        videoId = currentVideoId,
        p5 = p5,
        p20 = p20,
        p50 = p50,
        p80 = p80,
        p100 = p100,
        pOver = pRePlay
    )

    data class VideoProgressInfo(
        var videoId: Int,
        var p5: Int,
        var p20: Int,
        var p50: Int,
        var p80: Int,
        var p100: Int,
        var pOver: Int,
        var deviceId: String = GetDeviceId.getDeviceId(MyApp.getInstance())
    )

    /**
     * 销毁
     */
    @SuppressLint("CheckResult")
    fun releaseView(itemView: View) {
        val videoProgressInfo = getVideoProgressInfo()
        LogUtils.d("videoProgressInfo:${Gson().toJson(videoProgressInfo)}")
        val uploadVideoPro = RetrofitUtils.builder().uploadVideoPro(
            videoProgressInfo.videoId,
            videoProgressInfo.p5,
            videoProgressInfo.p20,
            videoProgressInfo.p50,
            videoProgressInfo.p80,
            videoProgressInfo.p100,
            videoProgressInfo.pOver,
            videoProgressInfo.deviceId
        )
        uploadVideoPro.subscribeOn(Schedulers.io())
            .throttleFirst(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
            }, {
            })

//        val holder = ViewHolder(itemView)
//        holder.danmuView.clear()
//        Jzvd.releaseAllVideos()

        collectVideoObservable?.dispose()
        collectVideoObservable = null

        mvShareObservable?.dispose()
        mvShareObservable = null

        giftTimer?.dispose()
        giftTimer = null

        videoGiftObservable?.dispose()
        videoGiftObservable = null

        mvSendMsgObservable?.dispose()
        mvSendMsgObservable = null

        mvLikeObservable?.dispose()
        mvLikeObservable = null

        mvGetMsgObservable?.dispose()
        mvGetMsgObservable = null

        gameDownloadStartObservable?.dispose()
        gameDownloadStartObservable = null

        gameDownloadCompleteObservable?.dispose()
        gameDownloadCompleteObservable = null

        gameInstallStartObservable?.dispose()
        gameInstallStartObservable = null

        p5 = 0
        isAddP5 = false
        p20 = 0
        isAddP20 = false
        p50 = 0
        isAddP50 = false
        p80 = 0
        isAddP80 = false
        p100 = 0
        isAddP100 = false
        pRePlay = 0
        mHandler.removeMessages(0)
    }

    private var totalShare = 0 //总分享
    private var isFav = false //是否收藏
    private var isLike = false //是否点赞
    private var canSend = false //弹幕是否可以发送
    private var countDown = 0 //礼包码倒计时
    private var content = "" //评论
    private var danmuLists = mutableListOf<MvGetMsgBean.DataBean.ListBean>() //弹幕列表
    private var taskId = -1L
    private var currentVideoName = ""
    private var downloadFileUrl = ""
    private var downloadFileSize = 0
    private var downloadFilePackageName = ""

    private var p5 = 0
    private var isAddP5 = false
    private var p20 = 0
    private var isAddP20 = false
    private var p50 = 0
    private var isAddP50 = false
    private var p80 = 0
    private var isAddP80 = false
    private var p100 = 0
    private var isAddP100 = false
    private var pRePlay = 0

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val videoView = currentHolder?.videoView
            if (videoView != null) {
                val currentPositionWhenPlaying = videoView.currentPositionWhenPlaying
                val duration = videoView.duration
//                LogUtils.d("currentPositionWhenPlaying:$currentPositionWhenPlaying,duration:$duration")
//                LogUtils.d("p5:$p5,p20:$p20,p50:$p50,p80:$p80,p100:$p100,pReplay:$pRePlay")
                if (currentPositionWhenPlaying > 0 && duration > 0) {
                    if (currentPositionWhenPlaying + 500 >= duration) {
                        //说明播完了
                        p100++
                        pRePlay++
                        isAddP5 = false
                        isAddP20 = false
                        isAddP50 = false
                        isAddP80 = false
                        isAddP100 = false
                    } else {
                        val progress = currentPositionWhenPlaying.toFloat() / duration * 100
                        when {
                            progress < 5 -> {
                                if (!isAddP5) {
                                    p5++
                                    isAddP5 = true
                                } else {
                                    isAddP20 = false
                                    isAddP50 = false
                                    isAddP80 = false
                                    isAddP100 = false
                                }
                            }
                            progress < 20 -> {
                                if (!isAddP20) {
                                    p20++
                                    isAddP20 = true
                                } else {
                                    isAddP5 = false
                                    isAddP50 = false
                                    isAddP80 = false
                                    isAddP100 = false
                                }
                            }
                            progress < 50 -> {
                                if (!isAddP50) {
                                    p50++
                                    isAddP50 = true
                                } else {
                                    isAddP5 = false
                                    isAddP20 = false
                                    isAddP80 = false
                                    isAddP100 = false
                                }
                            }
                            progress < 80 -> {
                                if (!isAddP80) {
                                    p80++
                                    isAddP80 = true
                                } else {
                                    isAddP5 = false
                                    isAddP20 = false
                                    isAddP50 = false
                                    isAddP100 = false
                                }
                            }
                        }
                    }
                }
            }
            sendEmptyMessageDelayed(0, 500)
        }
    }

    /**
     * 设置视频数据
     */
    @SuppressLint("SimpleDateFormat", "CheckResult", "SetTextI18n")
    fun setVideoInfo(videoInfo: NewVideoListsBean.Data.VideoBean, itemView: View, position: Int) {
        RetrofitUtils.builder().getVideoLists()
        Jzvd.SAVE_PROGRESS = false
        currentVideoId = videoInfo.video_id!!
        currentHolder = ViewHolder(itemView)
        currentVideoName = videoInfo.video_name.toString()
        val holder = currentHolder!!

        val mvInfo = RetrofitUtils.builder().mvInfo(currentVideoId.toString())
        val mvInfoObservable = mvInfo.subscribeOn(Schedulers.io())
            .throttleFirst(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
            }, {
            })

//        val isOpenDanmu = SPUtils.getBoolean(SPArgument.IS_OPEN_DANMU, true)
//        if (isOpenDanmu) {
//            holder.danmuRoot.open()
//            holder.danmuView.visibility = View.VISIBLE
//        } else {
//            holder.danmuRoot.close()
//            holder.danmuView.visibility = View.GONE
//        }
        val videoCoverWidth = videoInfo.video_cover_width!!
        val videoCoverHeight = videoInfo.video_cover_height!!

        //及时纠错视频尺寸问题
        val screenWidth = PhoneInfoUtils.getWidth(mContent)
        val screenHeight = PhoneInfoUtils.getHeight(mContent)
        holder.videoView.setPosterParams(
            videoCoverWidth,
            videoCoverHeight,
            screenWidth,
            screenHeight,
            false
        )
        holder.videoView.setMvLayoutParams(
            videoCoverWidth,
            videoCoverHeight,
            screenWidth,
            screenHeight,
            false
        )

//        holder.danmuView.enableDanmakuDrawingCache(true)
//        holder.danmuView.setCallback(object : DrawHandler.Callback {
//            override fun prepared() {
//                holder.danmuView.start()
//            }
//
//            override fun updateTimer(timer: DanmakuTimer?) {
//            }
//
//            override fun danmakuShown(danmaku: BaseDanmaku?) {
//            }
//
//            override fun drawingFinished() {
//            }
//        })
//        val parser = object : BaseDanmakuParser() {
//            override fun parse(): IDanmakus {
//                return Danmakus()
//            }
//        }

//        holder.danmuView.release()
//        holder.danmuView.prepare(parser, mDanmakuContext)
//        holder.danmuRoot.setOnMessageClickListener(object :
//            MessageOCView.OnMessageClickListener {
//            override fun close() {
//                SPUtils.putValue(SPArgument.IS_OPEN_DANMU, false)
//                EventBus.getDefault().postSticky(DanmuState(false))
//                holder.danmuView.visibility = View.GONE
//            }
//
//            override fun open() {
//                SPUtils.putValue(SPArgument.IS_OPEN_DANMU, true)
//                EventBus.getDefault().postSticky(DanmuState(true))
//                holder.danmuView.visibility = View.VISIBLE
//            }
//
//            override fun show() {
//                holder.danmuView.pause()
//                Jzvd.goOnPlayOnPause()
//                holder.rlEnumView.visibility = View.GONE
//                holder.llEnterView.visibility = View.VISIBLE
//                OtherUtils.showSoftKeyboard(
//                    mContent,
//                    holder.etDanmuContent
//                )
//            }
//        })
        Glide.with(mContent)
            .load(videoInfo.video_cover)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.videoView.posterImageView)
        holder.videoView.posterImageView?.scaleType = ImageView.ScaleType.FIT_XY
        Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT)
        holder.videoView.setPlayListener(object : MyJzvd4Mv.PlayListener {
            override fun rePlay() {
                holder.videoView.startVideo()
//                holder.danmuView.start(0)
            }
        })

//        Thread {
//            toGetDanmu(videoInfo.video_id!!, holder)
//        }.start()
        Glide.with(mContent)
            .load(videoInfo.user_avatar)
            .into(holder.ivUserAvatar)
//        RxView.clicks(holder.ivUserAvatar)
//            .throttleFirst(200, TimeUnit.MILLISECONDS)
//            .subscribe {
//                val intent = Intent(mContent, UserDetailActivity::class.java)
//                intent.putExtra(UserDetailActivity.VIDEO_ID, videoInfo.video_id)
//                mContent.startActivity(intent)
//            }

        holder.tvUserName.text =
            if (videoInfo.user_name.isNullOrEmpty()) "好服多多" else videoInfo.user_name
        if (videoInfo.video_type == 4) {
            //GM发布的视频
            holder.tvUpdateTime.text = "游戏视频推荐"
        } else {
            holder.tvUpdateTime.text = TimeFormat.formatFont(videoInfo.video_update_time!!.toLong())
        }
        holder.tvVideoTitle.text = videoInfo.video_name
        holder.videoView.setUp(videoInfo.video_file, null)
        holder.videoView.startVideoAfterPreloading()

        p5 = 0
        isAddP5 = false
        p20 = 0
        isAddP20 = false
        p50 = 0
        isAddP50 = false
        p80 = 0
        isAddP80 = false
        p100 = 0
        isAddP100 = false
        pRePlay = 0
        mHandler.removeMessages(0)
        mHandler.sendEmptyMessage(0)

        var type = -1
        if (null != videoInfo.video_type && videoInfo.video_type != 2) {
            if (null != videoInfo.is_fav && videoInfo.is_fav == 1) {
                isFav = true
                holder.ivVideoFav.setImageResource(R.mipmap.collect_in)
                holder.tvVideoFav.visibility = View.INVISIBLE
            } else {
                isFav = false
                holder.ivVideoFav.setImageResource(R.mipmap.collect_un)
                holder.tvVideoFav.visibility = View.VISIBLE
            }
            RxView.clicks(holder.ivVideoFav)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                )
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        toCollectVideo(type, videoInfo.video_id!!, isFav, holder)
                    } else {
                        LoginUtils.toQuickLogin(mContent)
                    }
                }
        } else {
            holder.ivVideoFav.visibility = View.GONE
            holder.tvVideoFav.visibility = View.GONE
        }

        totalShare = videoInfo.total_share!!
        holder.tvVideoShare.text = videoInfo.total_share.toString()
        RxView.clicks(holder.llVideoShare)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            )
            .subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    BottomDialog.shareMV(
                        mContent,
                        videoInfo.video_id.toString(),
                        videoInfo.video_name!!,
                        videoInfo.video_desc!!,
                        videoInfo.video_cover!!,
                        object : BottomDialog.IsClickItem {
                            override fun isClickItem() {
                                toAddShareNum(videoInfo.video_id!!, holder)
                            }
                        }
                    )
                } else {
                    LoginUtils.toQuickLogin(mContent)
                }
            }

        holder.tvVideoComment.text = videoInfo.total_comment.toString()
        RxView.clicks(holder.llVideoComment)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toCommonDetailActivity(
                    mData.size > 1,
                    videoInfo.video_id.toString(),
                    videoInfo.video_name ?: "",
                    videoInfo.video_desc ?: ""
                )
            }

        holder.tvVideoLike.text = videoInfo.total_like.toString()
        isLike = videoInfo.is_like != 0
        holder.ivVideoLike.setImageResource(if (isLike) R.mipmap.mv_icon_good_focus else R.mipmap.mv_icon_good)
        RxView.clicks(holder.llVideoLike)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            )
            .subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    isLike = !isLike
                    holder.ivVideoLike.setImageResource(if (isLike) R.mipmap.mv_icon_good_focus else R.mipmap.mv_icon_good)
                    holder.tvVideoLike.text =
                        if (isLike) (holder.tvVideoLike.text.toString().trim()
                            .toInt() + 1).toString()
                        else (holder.tvVideoLike.text.toString().trim()
                            .toInt() - 1).toString()
                    toAddLikeNum(videoInfo.video_id!!, if (isLike) 0 else 1)
                } else {
                    LoginUtils.toQuickLogin(mContent)
                }
            }

        holder.tvVideoLook.text = videoInfo.total_view.toString()

        //判读是不是用户发布视频
        if (videoInfo.platform.isNullOrEmpty()) {
            type = 0
            holder.rlGameDownload.visibility = View.GONE
            if (videoInfo.game_info?.pc_gift != null&& videoInfo.game_info?.pc_gift!="") {
                holder.ivGameGift.visibility = View.VISIBLE
                holder.tvGameGift.visibility = View.VISIBLE
                FlipAnimUtils.startShakeByPropertyAnim(
                    holder.ivGameGift,
                    0.95f, 1.05f, 5f, 2000L
                )
                RxView.clicks(holder.ivGameGift)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        GiftNumDialogUtils.show(
                            mContent,
                            0,
                            0,
                            null,
                            videoInfo.game_info.pc_gift
                        )
                    }
            }
            if (videoInfo.game_url_short.isNullOrEmpty()) {
                holder.llGameShortUrl.visibility = View.GONE
            } else {
                holder.llGameShortUrl.visibility = View.VISIBLE
                holder.tvGameShortUrl.text = videoInfo.game_url_short
                FlipAnimUtils.startShakeByPropertyAnim(
                    holder.llGameShortUrl,
                    0.95f, 1.05f, 5f, 2000L
                )
                RxView.longClicks(holder.llGameShortUrl)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        OtherUtils.copy(mContent, videoInfo.game_url_short!!)
                    }
                RxView.clicks(holder.llGameShortUrl)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val intent = Intent(mContent, WebActivity::class.java)
                        intent.putExtra(WebActivity.TYPE, WebActivity.GAME_WEB)
                        intent.putExtra(WebActivity.GAME_NAME, videoInfo.video_name)
                        intent.putExtra(WebActivity.GAME_URL, videoInfo.game_url_short)
                        mContent.startActivity(intent)
                    }
            }
        } else {
            when (videoInfo.platform) {
                "pc" -> {
                    type = 1
                    if (null != videoInfo.game_gift && null != videoInfo.game_gift?.count_down) {
                        holder.ivGameGift.visibility = View.VISIBLE
                        holder.tvGameGift.visibility = View.VISIBLE
                        countDown = videoInfo.game_gift?.count_down!!
                        toCountDown()
                        RxView.clicks(holder.ivGameGift)
                            .throttleFirst(
                                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                                TimeUnit.MILLISECONDS
                            )
                            .subscribe {
                                if (MyApp.getInstance().isHaveToken()) {
                                    if (countDown > 0) {
                                        GiftNumDialogUtils.show(
                                            mContent,
                                            videoInfo.video_pos!!,
                                            countDown,
                                            null
                                        )
                                    } else {
                                        toGetGiftCode(
                                            videoInfo.video_pos!!,
                                            videoInfo.game_gift?.id!!
                                        )
                                    }
                                } else {
                                    LoginUtils.toQuickLogin(mContent)
                                }
                            }
                        FlipAnimUtils.startShakeByPropertyAnim(
                            holder.ivGameGift,
                            0.95f, 1.05f, 5f, 2000L
                        )
                    }
                    holder.rlGameDownload.visibility = View.GONE
                    holder.llGameShortUrl.visibility = View.VISIBLE
                    holder.tvGameShortUrl.text = videoInfo.game_url_short
                    FlipAnimUtils.startShakeByPropertyAnim(
                        holder.llGameShortUrl,
                        0.95f, 1.05f, 5f, 2000L
                    )
                    RxView.longClicks(holder.llGameShortUrl)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            OtherUtils.copy(mContent, videoInfo.game_url_short!!)
                        }
                    RxView.clicks(holder.llGameShortUrl)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            val intent = Intent(mContent, WebActivity::class.java)
                            intent.putExtra(WebActivity.TYPE, WebActivity.GAME_WEB)
                            intent.putExtra(WebActivity.GAME_NAME, videoInfo.video_name)
                            intent.putExtra(WebActivity.GAME_URL, videoInfo.game_url_short)
                            mContent.startActivity(intent)
                        }
                }
                "mobile" -> {
                    type = 2
                    holder.llGameShortUrl.visibility = View.GONE
                    holder.pbGameDownload.visibility = View.GONE
                    holder.rlGameDownload.visibility = View.VISIBLE
                    val translateAnimation = TranslateAnimation(
                        Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF,
                        0.0f
                    )
                    translateAnimation.duration = 500
                    holder.rlGameDownload.animation = translateAnimation
                    translateAnimation.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {
                        }

                        override fun onAnimationEnd(animation: Animation?) {
                            holder.pbGameDownload.visibility = View.VISIBLE
                            val alphaAnimation = AlphaAnimation(0.1f, 1.0f)
                            alphaAnimation.duration = 1500
                            holder.pbGameDownload.animation = alphaAnimation
                        }

                        override fun onAnimationRepeat(animation: Animation?) {
                        }
                    })
                    val androidDownUrl = videoInfo.game_info?.android_down_url
                    val androidPackageName = videoInfo.game_info?.android_package_name
                    val androidPackageSize = videoInfo.game_info?.android_package_size
                    LogUtils.d("gameDownload=>androidDownUrl:$androidDownUrl, androidPackageName:$androidPackageName, androidPackageSize:$androidPackageSize")
                    taskId = SPUtils.getLong("TASK_ID_${videoInfo.video_id}", -1L)
                    if (null != androidDownUrl && null != androidPackageName && null != androidPackageSize) {
                        downloadFileUrl = androidDownUrl
                        downloadFileSize = androidPackageSize.toInt()
                        downloadFilePackageName = androidPackageName
                        holder.ivGameDownloadCancel.visibility = View.GONE
                        if (isInstalled(androidPackageName)) {
                            //游戏是否已经安装了
                            holder.pbGameDownload.progress = 100
                            holder.ivGameDownloadIcon.visibility = View.GONE
                            holder.tvGameDownloadMsg.text = mContent.getString(R.string.open_game)

                            val lastIndexOf = androidDownUrl.lastIndexOf("/")
                            val fileName =
                                androidDownUrl.substring(lastIndexOf + 1, androidDownUrl.length)
                            val dirPath =
                                mContent.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                    .toString()
                            val downloadPath = "$dirPath/$fileName"
                            DeleteApkUtils.deleteApk(File(downloadPath))
                        } else {
                            //游戏还没有安装
                            if (isDownloaded(androidDownUrl, androidPackageSize)) {
                                //文件已经下载了
                                holder.ivGameDownloadIcon.visibility = View.GONE
                                holder.pbGameDownload.progress = 100
                                holder.tvGameDownloadMsg.text =
                                    mContent.getString(R.string.open_game)
                            } else {
                                //文件没有下载完
                                LogUtils.d("-----${downloadPercent()}")
                                when (downloadPercent()) {
                                    0 -> {
                                        //一点都没有下载
                                        holder.pbGameDownload.progress = 0
                                        holder.ivGameDownloadIcon.visibility = View.VISIBLE
                                        holder.tvGameDownloadMsg.text = "下载游戏"
//                                            "下载游戏(${
//                                                numberFormat.format(
//                                                    androidPackageSize.toFloat() / 1024.0 / 1024.0
//                                                )
//                                            }M)"
                                    }
                                    else -> {
                                        //下载了一点
                                        holder.ivGameDownloadIcon.visibility = View.GONE
                                        holder.pbGameDownload.progress = downloadPercent()
                                        holder.tvGameDownloadMsg.text =
                                            mContent.getString(R.string.re_download)
                                    }
                                }
                            }
                        }

                        RxView.clicks(holder.ivGameDownloadCancel)
                            .throttleFirst(200, TimeUnit.MILLISECONDS)
                            .subscribe {
                                Aria.download(mContent)
                                    .load(taskId)
                                    .cancel(true)
                            }

                        RxView.clicks(holder.rlGameDownload)
                            .throttleFirst(200, TimeUnit.MILLISECONDS)
                            .subscribe {
                                if (isInstalled(androidPackageName)) {
                                    //安装了,点击就是打开
                                    val launchIntentForPackage =
                                        mContent.packageManager.getLaunchIntentForPackage(
                                            androidPackageName
                                        )
                                    mContent.startActivity(launchIntentForPackage)
                                } else {
                                    if (isDownloaded(androidDownUrl, androidPackageSize)) {
                                        //下载了,点击就是安装
                                        toInstallGame(androidDownUrl, videoInfo.video_name)
                                    } else {
                                        when (downloadPercent()) {
                                            0 -> {
                                                holder.ivGameDownloadCancel.visibility =
                                                    View.VISIBLE
                                                holder.pbGameDownload.progress = 0
                                                holder.ivGameDownloadIcon.visibility = View.GONE
                                                holder.tvGameDownloadMsg.text =
                                                    "${mContent.getString(R.string.downloading)} 0%"
                                                toDownloadGame(
                                                    androidDownUrl,
                                                    videoInfo.video_id,
                                                    videoInfo.video_name,
                                                    videoInfo.game_info!!.android_package_size!!.toLong(),
                                                    videoInfo.game_info!!.game_icon ?: "",
                                                    androidPackageName
                                                )
                                            }
                                            else -> {
                                                val running = Aria.download(mContent)
                                                    .load(taskId)
                                                    .isRunning
                                                if (running) {
                                                    Aria.download(mContent)
                                                        .load(taskId)
                                                        .stop()
                                                } else {
                                                    Aria.download(mContent)
                                                        .load(taskId)
                                                        .setExtendField(
                                                            makeJson(
                                                                videoInfo.video_id!!,
                                                                videoInfo.video_name!!,
                                                                videoInfo.game_info?.game_icon
                                                                    ?: "",
                                                                videoInfo.game_info?.android_package_size?.toLong()!!,
                                                                androidDownUrl,
                                                                androidPackageName
                                                            )
                                                        )
                                                        .resume()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }
                else -> {
                    type = 0
                    if (videoInfo.game_info?.pc_gift != null&& videoInfo.game_info?.pc_gift!="") {
                        holder.ivGameGift.visibility = View.VISIBLE
                        holder.tvGameGift.visibility = View.VISIBLE
                        FlipAnimUtils.startShakeByPropertyAnim(
                            holder.ivGameGift,
                            0.95f, 1.05f, 5f, 2000L
                        )
                        RxView.clicks(holder.ivGameGift)
                            .throttleFirst(200, TimeUnit.MILLISECONDS)
                            .subscribe {
                                GiftNumDialogUtils.show(
                                    mContent,
                                    0,
                                    0,
                                    null,
                                    videoInfo.game_info.pc_gift
                                )
                            }
                    }
                    holder.rlGameDownload.visibility = View.GONE
                    if (videoInfo.game_url_short.isNullOrEmpty()) {
                        holder.llGameShortUrl.visibility = View.GONE
                    } else {
                        holder.llGameShortUrl.visibility = View.VISIBLE
                        holder.tvGameShortUrl.text = videoInfo.game_url_short
                        FlipAnimUtils.startShakeByPropertyAnim(
                            holder.llGameShortUrl,
                            0.95f, 1.05f, 5f, 2000L
                        )
                        RxView.longClicks(holder.llGameShortUrl)
                            .throttleFirst(200, TimeUnit.MILLISECONDS)
                            .subscribe {
                                OtherUtils.copy(mContent, videoInfo.game_url_short!!)
                            }
                        RxView.clicks(holder.llGameShortUrl)
                            .throttleFirst(200, TimeUnit.MILLISECONDS)
                            .subscribe {
                                val intent = Intent(mContent, WebActivity::class.java)
                                intent.putExtra(WebActivity.TYPE, WebActivity.GAME_WEB)
                                intent.putExtra(WebActivity.GAME_NAME, videoInfo.video_name)
                                intent.putExtra(WebActivity.GAME_URL, videoInfo.game_url_short)
                                mContent.startActivity(intent)
                            }
                    }
                }
            }
        }

        //弹幕
//        var mHeight = 0
//        SoftKeyBoardChangeListener(mContent).init()
//            .setHeightListener(object : SoftKeyBoardChangeListener.HeightListener {
//                override fun onHeightChanged(height: Int) {
//                    LogUtils.d("SoftKeyBoardChangeListener:height=$height")
//                    if (height != mHeight) {
//                        mHeight = height
//                        if (height == 0) {
//                            holder.llEnterView.translationY = height.toFloat()
//                            holder.danmuView.resume()
//                            Jzvd.goOnPlayOnResume()
//                            holder.rlEnumView.visibility = View.VISIBLE
//                            holder.llEnterView.visibility = View.GONE
//                            if (content != "") {
//                                if (OtherUtils.isGotOutOfLine(
//                                        mContent,
//                                        content
//                                    ) == 1
//                                ) {
//                                    ToastUtils.show("弹幕${mContent.getString(R.string.string_057)}")
//                                    return
//                                } else if (OtherUtils.isGotOutOfLine(
//                                        mContent,
//                                        content
//                                    ) == 2
//                                ) {
//                                    ToastUtils.show("弹幕${mContent.getString(R.string.string_053)}")
//                                    return
//                                }
//                                val currentTime = holder.videoView.currentPositionWhenPlaying!!
//                                val msgBean = MvGetMsgBean.DataBean.ListBean()
//                                msgBean.content = content
//                                msgBean.video_diff_time = currentTime.toInt()
//                                danmuLists.add(msgBean)
//                                addDanmaku(
//                                    content = content,
//                                    withBorder = true,
//                                    time = currentTime,
//                                    holder = holder
//                                )
//                                toSendMsg(videoInfo.video_id.toString(), content, currentTime)
//                                content = ""
//                            }
//                        } else {
//                            val width = PhoneInfoUtils.getWidth(mContent)
//                            val tabBottom = 50 * (width.toFloat() /360 )
//                            holder.llEnterView.translationY = -(height.toFloat() - tabBottom)
//                        }
//                    }
//                }
//            })
//        holder.danmuRoot.setOnMessageClickListener(object : MessageOCView.OnMessageClickListener {
//            override fun close() {
//                SPUtils.putValue(SPArgument.IS_OPEN_DANMU, false)
//                holder.danmuView.visibility = View.GONE
//            }
//
//            override fun open() {
//                SPUtils.putValue(SPArgument.IS_OPEN_DANMU, true)
//                holder.danmuView.visibility = View.VISIBLE
//            }
//
//            override fun show() {
//                holder.danmuView.pause()
//                Jzvd.goOnPlayOnPause()
//                holder.rlEnumView.visibility = View.GONE
//                holder.llEnterView.visibility = View.VISIBLE
//                OtherUtils.showSoftKeyboard(mContent, holder.etDanmuContent)
//            }
//        })

        RxTextView.textChanges(holder.etDanmuContent)
            .skipInitialValue()
            .subscribe {
                if (it.isNullOrEmpty()) {
                    canSend = false
                    holder.tvSendDanmu.setBackgroundColor(mContent.resources.getColor(R.color.black_494A49))
                } else {
                    canSend = true
                    holder.tvSendDanmu.setBackgroundColor(mContent.resources.getColor(R.color.green_2EC8AC))
                }
            }

        RxView.clicks(holder.tvSendDanmu)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            )
            .subscribe {
                if (canSend) {
                    if (MyApp.getInstance().isHaveToken()) {
                        OtherUtils.hindKeyboard(
                            mContent,
                            holder.etDanmuContent
                        )
                        content = holder.etDanmuContent.text.toString().trim()
                        holder.etDanmuContent.setText("")
                    } else {
                        LoginUtils.toQuickLogin(mContent)
                    }
                }
            }
    }

    /**
     * 下载游戏
     */
    private fun toDownloadGame(
        androidDownUrl: String,
        videoId: Int?,
        videoName: String?,
        gamSize: Long,
        gameIcon: String,
        androidPackageName: String
    ) {
        MobclickAgent.onEvent(
            mContent,
            "game_download",
            "game_download_video"
        )
        val lastIndexOf = androidDownUrl.lastIndexOf("/")
        val fileName = androidDownUrl.substring(lastIndexOf + 1, androidDownUrl.length)
        val dirPath = mContent.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()
        val downloadPath = "$dirPath/$fileName"
        taskId = Aria.download(mContent)
            .load(androidDownUrl)
            .setExtendField(
                makeJson(
                    videoId!!,
                    videoName!!,
                    gameIcon,
                    gamSize,
                    androidDownUrl,
                    androidPackageName
                )
            )
            .setFilePath(downloadPath, true)
            .ignoreFilePathOccupy()
            .create()
        SPUtils.putValue("TASK_ID_$videoId", taskId)

        val gameDownStart = RetrofitUtils.builder().gameDownStart4Video(videoId)
        gameDownloadStartObservable = gameDownStart.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                downId = it?.getData()?.down_id
            }, {
                //异常的不管
            })
    }

    /**
     * 创建下载时使用的额为信息
     */
    private fun makeJson(
        videoId: Int,
        gameName: String,
        gameIcon: String,
        gameSize: Long,
        gameDownloadUrl: String,
        gamePackageName: String
    ): String {
        val gameDownloadNotify =
            GameDownloadNotify(
                gameIcon,
                videoId,
                gameName,
                gameSize,
                gameDownloadUrl,
                gamePackageName
            )
        return Gson().toJson(gameDownloadNotify)
    }

    /**
     * 查看下载进度
     */
    private fun downloadPercent() = Aria.download(mContent).load(taskId).percent

    /**
     * 去安装Apk
     */
    private fun toInstallGame(
        androidDownUrl: String,
        videoName: String?,
    ) {
        mContent.runOnUiThread {
            ToastUtils.show("《$videoName》${mContent.getString(R.string.download_success2install)}")
        }
        val lastIndexOf = androidDownUrl.lastIndexOf("/")
        val fileName = androidDownUrl.substring(lastIndexOf + 1, androidDownUrl.length)
        val dirPath = mContent.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()
        val downloadPath = "$dirPath/$fileName"

        LogUtils.d("+++downloadPath = $downloadPath")
        val file = File(downloadPath)
        if (file.exists() && file.isFile && file.length() > 1024 * 1024) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri =
                    FileProvider.getUriForFile(
                        mContent,
                        "${mContent.packageName}.provider",
                        file
                    )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(
                    Uri.fromFile(file),
                    "application/vnd.android.package-archive"
                )
            }
            mContent.startActivity(intent)
        }
    }

    /**
     * 判断是否已经下载了文件
     * @return
     */
    private fun isDownloaded(androidDownUrl: String, androidPackageSize: String): Boolean {
        val lastIndexOf = androidDownUrl.lastIndexOf("/")
        val fileName = androidDownUrl.substring(lastIndexOf + 1, androidDownUrl.length)
        val dirPath = mContent.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()
        val dir = File(dirPath)
        if (null == dir || !dir.exists() || !dir.isDirectory) {
            return false
        } else {
            val listFiles = dir.listFiles()
            if (listFiles.isEmpty()) {
                return false
            } else {
                for (file in listFiles) {
                    if (file.name == fileName) {
                        val length = file.length()
                        //文件大小差距小于100字节,应该是下完了
                        return Math.abs(length - androidPackageSize.toLong()) < 100
                    }
                }
                return false
            }
        }
    }

    /**
     * 判断游戏是否已经安装
     */
    private fun isInstalled(androidPackageName: String) =
        InstallApkUtils.isInstallApk(mContent, androidPackageName)

    /**
     * 获取弹幕信息
     */
    private fun toGetDanmu(videoId: Int, holder: ViewHolder) {
        val mvGetMsg =
            RetrofitUtils.builder()
                .mvGetMsg(video_id = videoId.toString(), last_time = null, video_diff_time = 0)
        mvGetMsgObservable = mvGetMsg.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null && it.getData()?.list?.isNotEmpty() == true) {
                                danmuLists.clear()
                                danmuLists.addAll(it.getData()?.list!!)
                                for (msg in it.getData()?.list!!) {
                                    addDanmaku(
                                        content = msg.content!!,
                                        time = msg.video_diff_time!!.toLong(),
                                        holder = holder
                                    )
                                }
                            }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(mContent)
                        }
                    }
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.printStackTrace()}")
            })
    }

    /**
     * 增删点赞数量
     */
    private fun toAddLikeNum(videoId: Int, is_cancel: Int) {
        val mvLike = RetrofitUtils.builder().mvLike(videoId.toString(), is_cancel)
        mvLikeObservable = mvLike.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${Gson().toJson(it)}")
            })
    }

    /**
     * 添加弹幕
     */
    private fun addDanmaku(
        content: String,
        withBorder: Boolean = false,
        textColor: Int = MyApp.getInstance().resources.getColor(R.color.white_FFFFFF),
        time: Long = 0L,
        holder: ViewHolder
    ) {
        LogUtils.d("${javaClass.simpleName}=content:$content,time:$time")
        val screenWidth = Math.min(
            PhoneInfoUtils.getWidth(mContent).toFloat(),
            PhoneInfoUtils.getHeight(mContent).toFloat()
        )
        val danmaku = mDanmakuContext.mDanmakuFactory?.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL)
        danmaku?.text = content
        danmaku?.padding = (3 * (screenWidth / 360)).toInt()
        danmaku?.textSize = 14 * (screenWidth / 360)
        danmaku?.textColor = textColor
        danmaku?.time = time
        if (withBorder) {
            danmaku?.borderColor = mContent.resources.getColor(R.color.green_2EC8AC)
        }
        holder.danmuView.addDanmaku(danmaku)
    }

    /**
     * 发送弹幕
     */
    private fun toSendMsg(video_id: String, content: String, time: Long) {
        val mvSendMsg =
            RetrofitUtils.builder()
                .mvSendMsg(video_id = video_id, content = content, video_diff_time = time.toInt())
        mvSendMsgObservable = mvSendMsg.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {

                        }
                        -1 -> {
                            it.msg.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(mContent)
                        }
                        else -> {
                            it.msg.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(mContent.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.printStackTrace()}")
                ToastUtils.show(
                    HttpExceptionUtils.getExceptionMsg(mContent, it)
                )
            })
    }

    /**
     * 获取礼包码
     */
    private fun toGetGiftCode(videoPos: Int, giftId: Int) {
        DialogUtils.showBeautifulDialog(mContent)
        val videoGift = RetrofitUtils.builder().videoGift(videoPos, giftId)
        videoGiftObservable = videoGift.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            MobclickAgent.onEvent(
                                mContent,
                                "game_gift_code",
                                "game_gift_code_video"
                            )
                            countDown = it.getData()!!.count_down!!
                            GiftNumDialogUtils.show(
                                mContent,
                                videoPos,
                                countDown,
                                it.getData()?.cdkey!!.toString()
                            )
                            toCountDown()
                        }
                        3 -> {
                            //抢光了
                            countDown = it.getData()!!.count_down!!
                            toCountDown()
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(mContent)
                        }
                        else -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(mContent.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(
                    HttpExceptionUtils.getExceptionMsg(mContent, it)
                )
                DialogUtils.dismissLoading()
            })
    }

    /**
     * 开始倒计时
     */
    private fun toCountDown() {
        if (countDown > 0) {
            giftTimer = Observable.interval(0, 1, TimeUnit.SECONDS)
                .subscribe {
                    countDown--
                    if (countDown == 0) {
                        giftTimer?.dispose()
                        giftTimer = null
                    }
                }
        }
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
        val intent = Intent(mContent, CommentDetailActivityV4::class.java)
        intent.putExtra(CommentDetailActivityV4.SYSTEM, mVideoType)
        intent.putExtra(CommentDetailActivityV4.IS_MAIN_VIDEO, isMainVideo)
        intent.putExtra(CommentDetailActivityV4.VIDEO_ID, videoId)
        intent.putExtra(CommentDetailActivityV4.VIDEO_NAME, videoName)
        intent.putExtra(CommentDetailActivityV4.VIDEO_DESC, videoDesc)
        intent.putExtra(CommentDetailActivityV4.VIDEO_LIST, "")
        intent.putExtra(CommentDetailActivityV4.IMAGE_LIST, "")
        mContent.startActivity(intent)
    }

    /**
     * 增加分享次数
     */
    private fun toAddShareNum(videoId: Int, holder: ViewHolder) {
        val mvShare = RetrofitUtils.builder().mvShare(videoId.toString())
        mvShareObservable = mvShare.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                totalShare += 1
                holder.tvVideoShare.text = totalShare.toString()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${Gson().toJson(it)}")
            })
    }

    /**
     * 取消/收藏视频
     */
    private fun toCollectVideo(type: Int, videoId: Int, isFav: Boolean, holder: ViewHolder) {
        val collectVideo = RetrofitUtils.builder().collectVideo(videoId, if (isFav) 1 else 0)
        collectVideoObservable = collectVideo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (isFav) {
                                ToastUtils.show(mContent.getString(R.string.string_048))
                                holder.ivVideoFav.setImageResource(R.mipmap.collect_un)
                                holder.tvVideoFav.visibility = View.VISIBLE
                            } else {
                                ToastUtils.show(mContent.getString(R.string.string_049))
                                holder.ivVideoFav.setImageResource(R.mipmap.collect_in)
                                holder.tvVideoFav.visibility = View.INVISIBLE
                                MobclickAgent.onEvent(
                                    mContent,
                                    "collect",
                                    when (type) {
                                        1 -> "collect_pc_video"
                                        2 -> "collect_phone_video"
                                        else -> "collect_normal_video"
                                    }

                                )
                            }
                            this.isFav = !isFav
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(mContent)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(mContent.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(
                    HttpExceptionUtils.getExceptionMsg(mContent, it)
                )
            })
    }
}