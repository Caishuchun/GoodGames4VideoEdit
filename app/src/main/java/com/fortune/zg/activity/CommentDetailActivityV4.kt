package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapter
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.*
import com.fortune.zg.http.RetrofitProgressUploadListener
import com.fortune.zg.http.RetrofitUploadProgressUtil
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.listener.SoftKeyBoardChangeListener
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.video.VideoActivity
import com.fortune.zg.widget.CommonEdit
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.fortune.zg.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_comment_detail.*
import kotlinx.android.synthetic.main.activity_live.*
import kotlinx.android.synthetic.main.activity_repaly_common.*
import kotlinx.android.synthetic.main.fragment_mv.view.*
import kotlinx.android.synthetic.main.item_common.*
import kotlinx.android.synthetic.main.item_common.view.*
import kotlinx.android.synthetic.main.item_common_re.view.*
import kotlinx.android.synthetic.main.item_mv.view.*
import kotlinx.android.synthetic.main.item_pic_video_big.view.*
import kotlinx.android.synthetic.main.item_pic_video_small.view.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("INACCESSIBLE_TYPE")
class CommentDetailActivityV4 : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: CommentDetailActivityV4
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val SYSTEM = "system"
        const val IS_MAIN_VIDEO = "isMainVideo"
        const val VIDEO_ID = "video_id"
        const val VIDEO_NAME = "video_name"
        const val VIDEO_DESC = "video_desc"
        const val VIDEO_LIST = "video_list"
        const val IMAGE_LIST = "image_list"
    }

    private var system = 1
    private var isMainVideo = false
    private var videoId = ""
    private var videoFile = ""
    private var videoCover = ""
    private var videoCoverWidth = 0
    private var videoCoverHeiht = 0
    private var videoName = ""
    private var videoDesc = ""
    private var videoList = ""
    private var imageList = ""
    private var myCommonObservable: Disposable? = null
    private var otherCommonObservable: Disposable? = null
    private var currentPage = 1
    private var countPage = 1

    private var adapter4MyCommon: BaseAdapter<MyCommonBean.DataBean>? = null
    private var data4MyCommon = mutableListOf<MyCommonBean.DataBean>()

    private var adapter4CommonLists: BaseAdapter<CommonListBean.DataBean.ListBean>? = null
    private var data4CommonLists = mutableListOf<CommonListBean.DataBean.ListBean>()

    private var isShare = false
    private var mvShareObservable: Disposable? = null
    private var mvLikeObservable: Disposable? = null
    private var commonLikeObservable: Disposable? = null
    private var mvInfoObservable: Disposable? = null

    private var picAndVideoCount = 0
    private var result: CommonContentBean? = null
    private var uploadPictureObservable: Disposable? = null
    private var uploadVideoObservable: Disposable? = null
    private var submitCommonObservable: Disposable? = null

    @SuppressLint("SimpleDateFormat")
    private var dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    override fun getLayoutId() = R.layout.activity_comment_detail
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        system = intent.getIntExtra(SYSTEM, 1)
        isMainVideo = intent.getBooleanExtra(IS_MAIN_VIDEO, false)
        videoId = intent.getStringExtra(VIDEO_ID)!!
        videoName = intent.getStringExtra(VIDEO_NAME)!!
        videoDesc = intent.getStringExtra(VIDEO_DESC)!!
        videoList = intent.getStringExtra(VIDEO_LIST)!!
        imageList = intent.getStringExtra(IMAGE_LIST)!!
        initView()
        getData()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        var mHeight = 0
        SoftKeyBoardChangeListener(this).init()
            .setHeightListener(object : SoftKeyBoardChangeListener.HeightListener {
                override fun onHeightChanged(height: Int) {
                    LogUtils.d("${javaClass.simpleName}=SoftKeyBoardChangeListener:$height")
                    if (height != mHeight) {
                        mHeight = height
                        if (height == 0) {
                            ce_common_detail_addCommon.changeSoftKeyBoardState(false)
                        } else {
                            ce_common_detail_addCommon.changeSoftKeyBoardState(true)
                        }
                    }
                }
            })
        ce_common_detail_addCommon.setOnCommonSubmit(object : CommonEdit.OnCommonSubmit {
            override fun submit(common: String) {
                toDisposeCommon(common)
            }
        })

        /**
         * 自个的评论
         */
        adapter4MyCommon = BaseAdapter.Builder<MyCommonBean.DataBean>()
            .setLayoutId(R.layout.item_common)
            .setData(data4MyCommon)
            .addBindView { itemView, itemData ->
                Glide.with(this)
                    .load(itemData.user_avatar)
                    .placeholder(R.mipmap.bg_gray_6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.iv_item_common_head)
                itemView.tv_item_common_name.text = itemData.user_name
                itemView.tv_item_common_time.text = formatTime(itemData.comment_time.toString())

                val commentContent = Gson().fromJson<CommonContentBean>(
                    itemData.comment_content,
                    CommonContentBean::class.java
                )
                val isHasText = commentContent.getText() != null
                        && commentContent.getText()?.isNotEmpty() == true
                itemView.tv_item_common_common.visibility =
                    if (isHasText) View.VISIBLE else View.GONE
                if (isHasText) {
                    itemView.tv_item_common_common.text = commentContent.getText()
                }

                var picAndVideoAdapter: BaseAdapter<CommonContentBean.ListBean>? = null
                var picAndVideo = mutableListOf<CommonContentBean.ListBean>()
                if (commentContent.getList() != null
                    && commentContent.getList()?.isNotEmpty() == true
                    && commentContent.getList()?.size!! > 0
                ) {
                    //肯定有图片或者视频
                    picAndVideo = commentContent.getList() as ArrayList<CommonContentBean.ListBean>
                    picAndVideoAdapter?.notifyDataSetChanged()
                } else {
                    if (commentContent.getCover() != null
                        && commentContent.getCover()?.isNotEmpty() == true
                    ) {
                        //有个视频
                        val listBean = CommonContentBean.ListBean()
                        listBean.cover = commentContent.getCover()
                        listBean.url = commentContent.getUrl()
                        listBean.video_id = commentContent.getVideo_id()
                        picAndVideo.add(listBean)
                        picAndVideoAdapter?.notifyDataSetChanged()
                    } else if (commentContent.getUrl() != null
                        && commentContent.getUrl()?.isNotEmpty() == true
                    ) {
                        //没有视频,有这个的就是图片
                        val listBean = CommonContentBean.ListBean()
                        listBean.url = commentContent.getUrl()
                        picAndVideo.add(listBean)
                        picAndVideoAdapter?.notifyDataSetChanged()
                    }
                }
                picAndVideoAdapter = BaseAdapter.Builder<CommonContentBean.ListBean>()
                    .setData(picAndVideo)
                    .setLayoutId(R.layout.item_pic_video_big)
                    .addBindView { itemView, itemData ->
                        if (itemData.cover != null && itemData.cover?.isNotEmpty() == true) {
                            //视频
                            itemView.iv_item_picAndVideo_big_play.visibility = View.VISIBLE
                            LogUtils.d("==============itemData.cover:${itemData.cover}")
                            Glide.with(this)
                                .load(itemData.cover)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(itemView.iv_item_picAndVideo_big)
                        } else {
                            //图片
                            itemView.iv_item_picAndVideo_big_play.visibility = View.GONE
                            Glide.with(this)
                                .load(itemData.url)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(itemView.iv_item_picAndVideo_big)
                        }
                        RxView.clicks(itemView.iv_item_picAndVideo_big)
                            .throttleFirst(200, TimeUnit.MILLISECONDS)
                            .subscribe {
                                if (itemData.cover != null && itemData.cover?.isNotEmpty() == true) {
                                    //是视频
                                    val intent = Intent(this, VideoActivity::class.java)
                                    intent.putExtra(VideoActivity.VIDEO_TYPE, system)
                                    intent.putExtra(
                                        VideoActivity.VIDEO_ID,
                                        itemData.video_id!!.toInt()
                                    )
                                    intent.putExtra(VideoActivity.VIDEO_COVER, itemData.cover)
                                    intent.putExtra(VideoActivity.IS_MAIN_VIDEO, false)
                                    startActivity(intent)
                                } else {
                                    //是图片
                                    val intent = Intent(this, ShowPicActivity::class.java)
                                    intent.putExtra(ShowPicActivity.POSITION, 1)
                                    intent.putExtra(
                                        ShowPicActivity.LIST,
                                        mutableListOf(itemData.url) as ArrayList<String>
                                    )
                                    startActivity(intent)
                                }
                            }
                    }
                    .create()
                itemView.cv_item_common_picAndVideo.adapter = picAndVideoAdapter
                itemView.cv_item_common_picAndVideo.layoutManager = SafeStaggeredGridLayoutManager(
                    3, StaggeredGridLayoutManager.VERTICAL
                )

                if (itemData.comment_reply != null && itemData.comment_reply!! > 0) {
                    itemView.tv_item_common_replay.visibility = View.VISIBLE
                    itemView.tv_item_common_replay.text = itemData.comment_reply.toString()
                    itemView.tv_item_common_showAll.visibility = View.VISIBLE
                } else {
                    itemView.tv_item_common_replay.visibility = View.GONE
                    itemView.tv_item_common_showAll.visibility = View.GONE
                }
                if (itemData.comment_up != null && itemData.comment_up!! > 0) {
                    itemView.tv_item_common_like.visibility = View.VISIBLE
                    itemView.tv_item_common_like.text = itemData.comment_up.toString()
                } else {
                    itemView.tv_item_common_like.visibility = View.GONE
                }
                if (itemData.comment_operate != null && itemData.comment_up!! == 1) {
                    itemView.iv_item_common_like.setImageResource(R.mipmap.mv_icon_good_focus)
                } else {
                    itemView.iv_item_common_like.setImageResource(R.mipmap.mv_icon_good)
                }

                RxView.clicks(itemView.ll_item_common_share)
                    .throttleFirst(
                        200, TimeUnit.MILLISECONDS
                    ).subscribe {
                        BottomDialog.shareMV(
                            this,
                            videoId,
                            videoName,
                            videoDesc,
                            videoCover,
                            object : BottomDialog.IsClickItem {
                                override fun isClickItem() {
                                }
                            }
                        )
                    }
                var type =
                    if (null != itemData.comment_operate && itemData.comment_operate == 1) 1 else 0
                RxView.clicks(itemView.ll_item_common_msg)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        //去查看所有的评论
                        val intent = Intent(this, ReplayCommonActivityV4::class.java)
                        intent.putExtra(ReplayCommonActivityV4.SYSTEM, system)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_ID, videoId.toInt())
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_LIST, videoList)
                        intent.putExtra(ReplayCommonActivityV4.IMAGE_LIST, imageList)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_FILE, videoFile)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_NAME, videoName)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_DESC, videoDesc)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_COVER, videoCover)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_ID, itemData.comment_id)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_HEAD, itemData.user_avatar)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_NAME, itemData.user_name)
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_TIME,
                            itemData.comment_time.toString()
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_CONTENT,
                            itemData.comment_content
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_REPLAY,
                            itemData.comment_reply!!
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_LIKE,
                            itemView.tv_item_common_like.text.toString().toInt()
                        )
                        intent.putExtra(ReplayCommonActivityV4.COMMON_IS_LIKE, type == 1)
                        startActivityForResult(intent, 10086)
                    }
                RxView.clicks(itemView.ll_item_common_good)
                    .throttleFirst(
                        if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                        TimeUnit.MILLISECONDS
                    ).subscribe {
                        if (MyApp.getInstance().isHaveToken()) {
                            type = if (type == 1) 0 else 1
                            toCommonAddLikeNum(itemData.comment_id!!)
                            itemView.iv_item_common_like.setImageResource(
                                if (type == 1) R.mipmap.mv_icon_good_focus
                                else R.mipmap.mv_icon_good
                            )
                            val num = itemView.tv_item_common_like.text.toString()
                                .toInt() + if (type == 1) 1 else -1
                            if (num > 0) {
                                itemView.tv_item_common_like.visibility = View.VISIBLE
                                itemView.tv_item_common_like.text = "$num"
                            } else {
                                itemView.tv_item_common_like.text = "$num"
                                itemView.tv_item_common_like.visibility = View.GONE
                            }
                        } else {
                            LoginUtils.toQuickLogin(this)
                        }
                    }
                RxView.clicks(itemView.tv_item_common_showAll)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        //去查看所有的评论
                        val intent = Intent(this, ReplayCommonActivityV4::class.java)
                        intent.putExtra(ReplayCommonActivityV4.SYSTEM, system)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_ID, videoId.toInt())
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_LIST, videoList)
                        intent.putExtra(ReplayCommonActivityV4.IMAGE_LIST, imageList)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_FILE, videoFile)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_NAME, videoName)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_DESC, videoDesc)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_COVER, videoCover)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_ID, itemData.comment_id)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_HEAD, itemData.user_avatar)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_NAME, itemData.user_name)
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_TIME,
                            itemData.comment_time.toString()
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_CONTENT,
                            itemData.comment_content
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_REPLAY,
                            itemData.comment_reply!!
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_LIKE,
                            itemView.tv_item_common_like.text.toString().toInt()
                        )
                        intent.putExtra(ReplayCommonActivityV4.COMMON_IS_LIKE, type == 1)
                        startActivityForResult(intent, 10083)
                    }
            }
            .create()
        rv_comment_my.adapter = adapter4MyCommon
        rv_comment_my.layoutManager = object : SafeLinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        rv_comment_my.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

        /**
         * 玩家的评论
         */
        adapter4CommonLists = BaseAdapter.Builder<CommonListBean.DataBean.ListBean>()
            .setData(data4CommonLists)
            .setLayoutId(R.layout.item_common)
            .addBindView { itemView, itemData ->
                Glide.with(this)
                    .load(itemData.user_avatar)
                    .placeholder(R.mipmap.bg_gray_6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.iv_item_common_head)
                itemView.tv_item_common_name.text = itemData.user_name
                itemView.tv_item_common_time.text = formatTime(itemData.comment_time.toString())

                val commentContent = Gson().fromJson<CommonContentBean>(
                    itemData.comment_content,
                    CommonContentBean::class.java
                )
                val isHasText = commentContent.getText() != null
                        && commentContent.getText()?.isNotEmpty() == true
                itemView.tv_item_common_common.visibility =
                    if (isHasText) View.VISIBLE else View.GONE
                if (isHasText) {
                    itemView.tv_item_common_common.text = commentContent.getText()
                }

                var picAndVideoAdapter: BaseAdapter<CommonContentBean.ListBean>? = null
                var picAndVideo = mutableListOf<CommonContentBean.ListBean>()
                if (commentContent.getList() != null
                    && commentContent.getList()?.isNotEmpty() == true
                    && commentContent.getList()?.size!! > 0
                ) {
                    //肯定有图片或者视频
                    picAndVideo = commentContent.getList() as ArrayList<CommonContentBean.ListBean>
                    picAndVideoAdapter?.notifyDataSetChanged()
                } else {
                    if (commentContent.getCover() != null
                        && commentContent.getCover()?.isNotEmpty() == true
                    ) {
                        //有个视频
                        val listBean = CommonContentBean.ListBean()
                        listBean.cover = commentContent.getCover()
                        listBean.url = commentContent.getUrl()
                        listBean.video_id = commentContent.getVideo_id()
                        picAndVideo.add(listBean)
                        picAndVideoAdapter?.notifyDataSetChanged()
                    } else if (commentContent.getUrl() != null
                        && commentContent.getUrl()?.isNotEmpty() == true
                    ) {
                        //没有视频,有这个的就是图片
                        val listBean = CommonContentBean.ListBean()
                        listBean.url = commentContent.getUrl()
                        picAndVideo.add(listBean)
                        picAndVideoAdapter?.notifyDataSetChanged()
                    }
                }
                picAndVideoAdapter = BaseAdapter.Builder<CommonContentBean.ListBean>()
                    .setData(picAndVideo)
                    .setLayoutId(R.layout.item_pic_video_big)
                    .addBindView { itemView, itemData ->
                        if (itemData.cover != null && itemData.cover?.isNotEmpty() == true) {
                            //视频
                            itemView.iv_item_picAndVideo_big_play.visibility = View.VISIBLE
                            Glide.with(this)
                                .load(itemData.cover)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(itemView.iv_item_picAndVideo_big)
                        } else {
                            //图片
                            itemView.iv_item_picAndVideo_big_play.visibility = View.GONE
                            Glide.with(this)
                                .load(itemData.url)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(itemView.iv_item_picAndVideo_big)
                        }
                        RxView.clicks(itemView.iv_item_picAndVideo_big)
                            .throttleFirst(200, TimeUnit.MILLISECONDS)
                            .subscribe {
                                if (itemData.cover != null && itemData.cover?.isNotEmpty() == true) {
                                    //是视频
                                    val intent = Intent(this, VideoActivity::class.java)
                                    intent.putExtra(VideoActivity.VIDEO_TYPE, system)
                                    intent.putExtra(
                                        VideoActivity.VIDEO_ID,
                                        itemData.video_id!!.toInt()
                                    )
                                    intent.putExtra(VideoActivity.VIDEO_COVER, itemData.cover)
                                    intent.putExtra(VideoActivity.IS_MAIN_VIDEO, false)
                                    startActivity(intent)
                                } else {
                                    //是图片
                                    val intent = Intent(this, ShowPicActivity::class.java)
                                    intent.putExtra(ShowPicActivity.POSITION, 1)
                                    intent.putExtra(
                                        ShowPicActivity.LIST,
                                        mutableListOf(itemData.url) as ArrayList<String>
                                    )
                                    startActivity(intent)
                                }
                            }
                    }
                    .create()
                itemView.cv_item_common_picAndVideo.adapter = picAndVideoAdapter
                itemView.cv_item_common_picAndVideo.layoutManager = SafeStaggeredGridLayoutManager(
                    3, StaggeredGridLayoutManager.VERTICAL
                )

                if (itemData.comment_reply != null && itemData.comment_reply!! > 0) {
                    itemView.tv_item_common_replay.visibility = View.VISIBLE
                    itemView.tv_item_common_replay.text = itemData.comment_reply.toString()
                } else {
                    itemView.tv_item_common_replay.visibility = View.GONE
                }
                if (itemData.comment_up != null && itemData.comment_up!! > 0) {
                    itemView.tv_item_common_like.visibility = View.VISIBLE
                    itemView.tv_item_common_like.text = itemData.comment_up.toString()
                } else {
                    itemView.tv_item_common_like.visibility = View.GONE
                }
                if (itemData.comment_operate != null && itemData.comment_up!! == 1) {
                    itemView.iv_item_common_like.setImageResource(R.mipmap.mv_icon_good_focus)
                } else {
                    itemView.iv_item_common_like.setImageResource(R.mipmap.mv_icon_good)
                }
                if (itemData.comment_reply_part != null && itemData.comment_reply_part!!.isNotEmpty()) {
                    itemView.rv_item_common_re.visibility = View.VISIBLE
                    /**
                     * 单条目下的部分回复
                     */
                    val adapter4PartReplay =
                        BaseAdapter.Builder<CommonListBean.DataBean.ListBean.CommentReplyPartBean>()
                            .setData(itemData.comment_reply_part!!)
                            .setLayoutId(R.layout.item_common_re)
                            .addBindView { itemView, itemData ->
                                Glide.with(this)
                                    .load(itemData.user_avatar)
                                    .placeholder(R.mipmap.bg_gray_6)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(itemView.iv_item_common_replay_head)
                                itemView.tv_item_common_replay_name.text = itemData.user_name
                                itemView.tv_item_common_replay_time.text =
                                    formatTime(itemData.reply_time.toString())

                                val commentReplayContent = Gson().fromJson<CommonContentBean>(
                                    itemData.reply_content,
                                    CommonContentBean::class.java
                                )
                                val replayIsHasText = commentReplayContent.getText() != null
                                        && commentReplayContent.getText()?.isNotEmpty() == true
                                itemView.tv_item_common_replay_common.visibility =
                                    if (replayIsHasText) View.VISIBLE else View.GONE
                                if (replayIsHasText) {
                                    itemView.tv_item_common_replay_common.text =
                                        commentReplayContent.getText()
                                }

                                var replayPicAndVideoAdapter: BaseAdapter<CommonContentBean.ListBean>? =
                                    null
                                var replayPicAndVideo = mutableListOf<CommonContentBean.ListBean>()
                                if (commentReplayContent.getList() != null
                                    && commentReplayContent.getList()?.isNotEmpty() == true
                                    && commentReplayContent.getList()?.size!! > 0
                                ) {
                                    //肯定有图片或者视频
                                    replayPicAndVideo =
                                        commentReplayContent.getList() as ArrayList<CommonContentBean.ListBean>
                                    replayPicAndVideoAdapter?.notifyDataSetChanged()
                                } else {
                                    if (commentReplayContent.getCover() != null
                                        && commentReplayContent.getCover()?.isNotEmpty() == true
                                    ) {
                                        //有个视频
                                        val listBean = CommonContentBean.ListBean()
                                        listBean.cover = commentReplayContent.getCover()
                                        listBean.url = commentReplayContent.getUrl()
                                        listBean.video_id = commentReplayContent.getVideo_id()
                                        replayPicAndVideo.add(listBean)
                                        replayPicAndVideoAdapter?.notifyDataSetChanged()
                                    } else if (commentReplayContent.getUrl() != null
                                        && commentReplayContent.getUrl()?.isNotEmpty() == true
                                    ) {
                                        //没有视频,有这个的就是图片
                                        val listBean = CommonContentBean.ListBean()
                                        listBean.url = commentReplayContent.getUrl()
                                        replayPicAndVideo.add(listBean)
                                        replayPicAndVideoAdapter?.notifyDataSetChanged()
                                    }
                                }
                                replayPicAndVideoAdapter =
                                    BaseAdapter.Builder<CommonContentBean.ListBean>()
                                        .setData(replayPicAndVideo)
                                        .setLayoutId(R.layout.item_pic_video_small)
                                        .addBindView { itemView, itemData ->
                                            if (itemData.cover != null && itemData.cover?.isNotEmpty() == true) {
                                                //视频
                                                itemView.iv_item_picAndVideo_small_play.visibility =
                                                    View.VISIBLE
                                                Glide.with(this)
                                                    .load(itemData.cover)
                                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                    .into(itemView.iv_item_picAndVideo_small)
                                            } else {
                                                //图片
                                                itemView.iv_item_picAndVideo_small_play.visibility =
                                                    View.GONE
                                                Glide.with(this)
                                                    .load(itemData.url)
                                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                    .into(itemView.iv_item_picAndVideo_small)
                                            }
                                            RxView.clicks(itemView.iv_item_picAndVideo_small)
                                                .throttleFirst(200, TimeUnit.MILLISECONDS)
                                                .subscribe {
                                                    if (itemData.cover != null && itemData.cover?.isNotEmpty() == true) {
                                                        //是视频
                                                        val intent =
                                                            Intent(this, VideoActivity::class.java)
                                                        intent.putExtra(
                                                            VideoActivity.VIDEO_TYPE,
                                                            system
                                                        )
                                                        intent.putExtra(
                                                            VideoActivity.VIDEO_ID,
                                                            itemData.video_id!!.toInt()
                                                        )
                                                        intent.putExtra(
                                                            VideoActivity.VIDEO_COVER,
                                                            itemData.cover
                                                        )
                                                        intent.putExtra(
                                                            VideoActivity.IS_MAIN_VIDEO,
                                                            false
                                                        )
                                                        startActivity(intent)
                                                    } else {
                                                        //是图片
                                                        val intent = Intent(
                                                            this,
                                                            ShowPicActivity::class.java
                                                        )
                                                        intent.putExtra(
                                                            ShowPicActivity.POSITION,
                                                            1
                                                        )
                                                        intent.putExtra(
                                                            ShowPicActivity.LIST,
                                                            mutableListOf(itemData.url) as ArrayList<String>
                                                        )
                                                        startActivity(intent)
                                                    }
                                                }
                                        }
                                        .create()
                                itemView.cv_item_common_replay_picAndVideo.adapter =
                                    replayPicAndVideoAdapter
                                itemView.cv_item_common_replay_picAndVideo.layoutManager =
                                    SafeStaggeredGridLayoutManager(
                                        3, StaggeredGridLayoutManager.VERTICAL
                                    )
                            }
                            .create()
                    itemView.rv_item_common_re.adapter = adapter4PartReplay
                    itemView.rv_item_common_re.layoutManager =
                        object : SafeLinearLayoutManager(this) {
                            override fun canScrollVertically(): Boolean {
                                return false
                            }
                        }
                    itemView.rv_item_common_re.addItemDecoration(object :
                        RecyclerView.ItemDecoration() {
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

                    if (itemData.comment_reply!!.toInt() > itemData.comment_reply_part!!.size) {
                        itemView.tv_item_common_showAll.visibility = View.VISIBLE
                    } else {
                        itemView.tv_item_common_showAll.visibility = View.GONE
                    }
                } else {
                    itemView.rv_item_common_re.visibility = View.GONE
                }

                RxView.clicks(itemView.ll_item_common_share)
                    .throttleFirst(
                        200, TimeUnit.MILLISECONDS
                    ).subscribe {
                        BottomDialog.shareMV(
                            this,
                            videoId,
                            videoName,
                            videoDesc,
                            videoCover,
                            object : BottomDialog.IsClickItem {
                                override fun isClickItem() {
                                }
                            }
                        )
                    }
                var type =
                    if (null != itemData.comment_operate && itemData.comment_operate == 1) 1 else 0
                RxView.clicks(itemView.ll_item_common_msg)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        //去查看所有的评论
                        val intent = Intent(this, ReplayCommonActivityV4::class.java)
                        intent.putExtra(ReplayCommonActivityV4.SYSTEM, system)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_ID, videoId.toInt())
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_LIST, videoList)
                        intent.putExtra(ReplayCommonActivityV4.IMAGE_LIST, imageList)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_FILE, videoFile)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_NAME, videoName)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_DESC, videoDesc)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_COVER, videoCover)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_ID, itemData.comment_id)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_HEAD, itemData.user_avatar)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_NAME, itemData.user_name)
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_TIME,
                            itemData.comment_time.toString()
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_CONTENT,
                            itemData.comment_content
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_REPLAY,
                            itemData.comment_reply!!
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_LIKE,
                            itemView.tv_item_common_like.text.toString().toInt()
                        )
                        intent.putExtra(ReplayCommonActivityV4.COMMON_IS_LIKE, type == 1)
                        startActivityForResult(intent, 10086)
                    }
                RxView.clicks(itemView.ll_item_common_good)
                    .throttleFirst(
                        if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                        TimeUnit.MILLISECONDS
                    ).subscribe {
                        if (MyApp.getInstance().isHaveToken()) {
                            type = if (type == 1) 0 else 1
                            toCommonAddLikeNum(itemData.comment_id!!)
                            itemView.iv_item_common_like.setImageResource(
                                if (type == 1) R.mipmap.mv_icon_good_focus
                                else R.mipmap.mv_icon_good
                            )
                            val num = itemView.tv_item_common_like.text.toString()
                                .toInt() + if (type == 1) 1 else -1
                            if (num > 0) {
                                itemView.tv_item_common_like.visibility = View.VISIBLE
                                itemView.tv_item_common_like.text = "$num"
                            } else {
                                itemView.tv_item_common_like.text = "$num"
                                itemView.tv_item_common_like.visibility = View.GONE
                            }
                        } else {
                            LoginUtils.toQuickLogin(this)
                        }
                    }
                RxView.clicks(itemView.tv_item_common_showAll)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        //去查看所有的评论
                        val intent = Intent(this, ReplayCommonActivityV4::class.java)
                        intent.putExtra(ReplayCommonActivityV4.SYSTEM, system)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_ID, videoId.toInt())
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_LIST, videoList)
                        intent.putExtra(ReplayCommonActivityV4.IMAGE_LIST, imageList)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_FILE, videoFile)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_NAME, videoName)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_DESC, videoDesc)
                        intent.putExtra(ReplayCommonActivityV4.VIDEO_COVER, videoCover)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_ID, itemData.comment_id)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_HEAD, itemData.user_avatar)
                        intent.putExtra(ReplayCommonActivityV4.COMMON_NAME, itemData.user_name)
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_TIME,
                            itemData.comment_time.toString()
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_CONTENT,
                            itemData.comment_content
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_REPLAY,
                            itemData.comment_reply!!
                        )
                        intent.putExtra(
                            ReplayCommonActivityV4.COMMON_LIKE,
                            itemView.tv_item_common_like.text.toString().toInt()
                        )
                        intent.putExtra(ReplayCommonActivityV4.COMMON_IS_LIKE, type == 1)
                        startActivityForResult(intent, 10086)
                    }
            }
            .create()
        rv_comment_detail.adapter = adapter4CommonLists
        rv_comment_detail.layoutManager = object : SafeLinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        rv_comment_detail.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
    }

    private fun getData() {
        toMvInfo()
        getMyCommon()
        getOtherCommon()
    }

    /**
     * 获取视频数据
     */
    private fun toMvInfo() {
        DialogUtils.showBeautifulDialog(this)
        val mvInfo =
            RetrofitUtils.builder().mvInfo(video_id = videoId)
        mvInfoObservable = mvInfo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            toSetInfo(it.getData()!!)
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.printStackTrace()}")
                DialogUtils.dismissLoading()
                ToastUtils.show(
                    HttpExceptionUtils.getExceptionMsg(this, it)
                )
            })
    }

    /**
     * 获取我的评论
     */
    @SuppressLint("SetTextI18n")
    private fun getMyCommon() {
        if (!MyApp.getInstance().isHaveToken()) {
            return
        }
        DialogUtils.showBeautifulDialog(this)
        val myCommon = RetrofitUtils.builder().myCommon(video_id = videoId.toInt())
        myCommonObservable = myCommon.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            data4MyCommon.clear()
                            if (!it.getData().isNullOrEmpty() && it.getData()?.size!! > 0) {
                                ll_comment_detail_mine_title.visibility = View.VISIBLE
                                rv_comment_my.visibility = View.VISIBLE
                                var countReplay = 0
                                for (data in it.getData()!!) {
                                    countReplay += data?.comment_reply!!
                                }
                                tv_comment_detail_mineNum.text =
                                    getString(R.string.string_001).replace(
                                        "X",
                                        countReplay.toString()
                                    )
                                data4MyCommon.addAll(it.getData() as MutableList<MyCommonBean.DataBean>)
                                adapter4MyCommon?.notifyDataSetChanged()
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.printStackTrace()}")
                DialogUtils.dismissLoading()
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 获取评论列表
     */
    @SuppressLint("SetTextI18n")
    private fun getOtherCommon(needDialog: Boolean = true) {
        if (needDialog)
            DialogUtils.showBeautifulDialog(this)
        val commonList = RetrofitUtils.builder().commonList(videoId.toInt(), currentPage)
        otherCommonObservable = commonList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            data4CommonLists.clear()
                            if (it.getData() != null && it.getData()?.paging?.count != null && it.getData()?.paging?.count!! > 0) {
                                data4CommonLists.addAll(it.getData()!!.list!!)
                                adapter4CommonLists?.notifyDataSetChanged()
                                tv_comment_detail_otherNum.text =
                                    getString(R.string.string_002).replace(
                                        "X",
                                        it.getData()!!.paging!!.count!!.toString()
                                    )
                                countPage =
                                    it.getData()?.paging?.count!! / it.getData()?.paging?.limit!!
                                if (it.getData()?.paging?.count!! % it.getData()?.paging?.limit!! != 0) {
                                    countPage++
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.printStackTrace()}")
                DialogUtils.dismissLoading()
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    @SuppressLint("CheckResult")
    private fun toSetInfo(data: MvDetailBean.DataBean) {
        if (isMainVideo) {
            videoList = Gson().toJson(data.videoList)
            imageList = Gson().toJson(data.imageList)
        }

        tv_comment_detail_mv_userName.text = data.user_name
        Glide.with(this)
            .load(data.user_avatar)
            .placeholder(R.mipmap.bg_gray_6)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(riv_comment_detail_mv_headIcon)
        tv_comment_detail_mv_title.text = data.video_name
        Glide.with(this)
            .load(data.video_cover)
            .placeholder(R.mipmap.bg_gray_6)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(iv_comment_detail_mv_poster)
        val width = data.video_cover_width!!
        val height = data.video_cover_height!!
        val newWidth: Int
        val newHeight: Int
        val layoutParams = rl_comment_detail_mv.layoutParams
        val screenWidth =
            PhoneInfoUtils.getWidth(this@CommentDetailActivityV4).toDouble()
        if (width > height) {
            newWidth = (screenWidth / 360f * 328).toInt()
            newHeight = (newWidth.toFloat() / width * height).toInt()
        } else {
            newWidth = (screenWidth / 360f * 150).toInt()
            newHeight = (newWidth.toFloat() / width * height).toInt()
        }
        layoutParams.width = newWidth
        layoutParams.height = newHeight
        rl_comment_detail_mv.layoutParams = layoutParams

        if (data.video_update_time?.contains(":") == true) {
            tv_comment_detail_mv_updateTime.text = data.video_update_time
        } else {
            val format = dateFormat.format(data.video_update_time!!.toLong() * 1000)
            tv_comment_detail_mv_updateTime.text = format
        }
        tv_comment_detail_mv_share_num.text = data.total_share.toString()
        tv_comment_detail_mv_msg_num.text = data.total_comment.toString()
        tv_comment_detail_mv_good_num.text = data.total_like.toString()
        tv_comment_detail_mv_look_num.text = data.total_view.toString()
        iv_comment_detail_mv_good.setImageResource(
            if (data.is_like == 1) R.mipmap.mv_icon_good_focus
            else R.mipmap.mv_icon_good
        )

        videoFile = data.video_file!!
        videoCover = data.video_cover!!
        videoCoverWidth = data.video_cover_width!!
        videoCoverHeiht = data.video_cover_height!!

        RxView.clicks(iv_comment_detail_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        RxView.clicks(iv_comment_detail_mv_poster)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toMvDetailActivity()
            }

        RxView.clicks(iv_comment_detail_mv_play)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toMvDetailActivity()
            }

        RxView.clicks(iv_comment_detail_mv_share)
            .throttleFirst(
                200, TimeUnit.MILLISECONDS
            ).subscribe {
                BottomDialog.shareMV(
                    this,
                    data.video_id.toString(),
                    videoName,
                    videoDesc,
                    data.video_cover!!,
                    object : BottomDialog.IsClickItem {
                        override fun isClickItem() {
                            isShare = true
                        }
                    }
                )
            }

        RxView.clicks(iv_comment_detail_mv_msg)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //滚动
                sv_comment_detail.smoothScrollTo(0, ll_comment_detail_mvRoot.measuredHeight)
            }

        var videoIsLike = data.is_like == 1
        RxView.clicks(iv_comment_detail_mv_good)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            ).subscribe {
                //点赞
                if (MyApp.getInstance().isHaveToken()) {
                    videoIsLike = !videoIsLike
                    iv_comment_detail_mv_good.setImageResource(if (videoIsLike) R.mipmap.mv_icon_good_focus else R.mipmap.mv_icon_good)
                    tv_comment_detail_mv_good_num.text =
                        (tv_comment_detail_mv_good_num.text.toString()
                            .toInt() + if (videoIsLike) 1 else -1).toString()
                    toMvAddLikeNum(if (videoIsLike) 0 else 1)
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }

        refresh_comment_detail.setEnableRefresh(false)
        refresh_comment_detail.setEnableLoadMore(true)
        refresh_comment_detail.setEnableLoadMoreWhenContentNotFull(false)
        refresh_comment_detail.setRefreshFooter(ClassicsFooter(this))
        refresh_comment_detail.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    getOtherCommon(false)
                } else {
                    refresh_replay_common.finishLoadMoreWithNoMoreData()
                }
            }
        })
    }

    /**
     * 评论增加/减少点赞数量
     */
    private fun toCommonAddLikeNum(commonId: Int) {
        val commonLike = RetrofitUtils.builder().commonLike(commonId)
        commonLikeObservable = commonLike.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${Gson().toJson(it)}")
            })
    }

    /**
     * 视频增加/减少点赞数量
     */
    private fun toMvAddLikeNum(is_cancel: Int) {
        val mvLike = RetrofitUtils.builder().mvLike(videoId, is_cancel)
        mvLikeObservable = mvLike.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${Gson().toJson(it)}")
            })
    }

    /**
     * 跳转到视频详情界面
     */
    private fun toMvDetailActivity() {
        val intent = Intent(this, VideoActivity::class.java)
        intent.putExtra(VideoActivity.VIDEO_TYPE, system)
        intent.putExtra(VideoActivity.VIDEO_ID, videoId.toInt())
        intent.putExtra(VideoActivity.VIDEO_COVER, videoCover)
        intent.putExtra(VideoActivity.VIDEO_COVER_WIDTH, videoCoverWidth)
        intent.putExtra(VideoActivity.VIDEO_COVER_HEIGHT, videoCoverHeiht)
        intent.putExtra(VideoActivity.IS_MAIN_VIDEO, false)
        startActivity(intent)
    }

    override fun destroy() {
        myCommonObservable?.dispose()
        myCommonObservable = null

        otherCommonObservable?.dispose()
        otherCommonObservable = null

        mvShareObservable?.dispose()
        mvShareObservable = null

        mvLikeObservable?.dispose()
        mvLikeObservable = null

        commonLikeObservable?.dispose()
        commonLikeObservable = null

        mvInfoObservable?.dispose()
        mvInfoObservable = null

        uploadPictureObservable?.dispose()
        uploadPictureObservable = null

        uploadVideoObservable?.dispose()
        uploadVideoObservable = null

        submitCommonObservable?.dispose()
        submitCommonObservable = null
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
        if (isShare) {
            isShare = false
            toAddShareNum()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10086 && resultCode == RESULT_OK) {
            getData()
        }
    }

    /**
     * 增加分享次数
     */
    private fun toAddShareNum() {
        val mvShare = RetrofitUtils.builder().mvShare(videoId)
        mvShareObservable = mvShare.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                tv_comment_detail_mv_share_num.text =
                    (tv_comment_detail_mv_share_num.text.toString().trim().toInt()
                        .plus(1)).toString()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${Gson().toJson(it)}")
            })
    }

    /**
     * 开始处理评论数据
     */
    private fun toDisposeCommon(common: String) {
        result = Gson().fromJson<CommonContentBean>(
            common,
            CommonContentBean::class.java
        )
        val text = result?.getText()
        if (text != null) {
            if (OtherUtils.isGotOutOfLine(this, text) == 1) {
                ToastUtils.show("评论${getString(R.string.string_057)}")
                return
            } else if (OtherUtils.isGotOutOfLine(this, text) == 2) {
                ToastUtils.show("评论${getString(R.string.string_053)}")
                return
            }
        }
        if (result?.getList() != null && result?.getList()?.isNotEmpty() == true) {
            //有图片
            picAndVideoCount = result?.getList()!!.size
            DialogUtils.showDialogWithProgress(this, "")
            toDisposePicAndVideo(0)
        } else {
            //没图片,就一定有文字,直接上传评论即可
            toSubmitReplay(common)
        }
    }

    /**
     * 开始处理视频、图片
     */
    private fun toDisposePicAndVideo(currentPosition: Int) {
        val listBean = result?.getList()?.get(currentPosition)!!
        if (listBean.cover != null && listBean.cover == "video") {
            //视频
            toUploadVideo(currentPosition)
        } else {
            //图片
            toUploadPic(currentPosition)
        }
    }

    /**
     * 上传图片
     */
    private fun toUploadPic(currentPosition: Int) {
        DialogUtils.setDialogMsg("${getString(R.string.string_004)}${currentPosition + 1}/$picAndVideoCount")
        val currentData = result?.getList()!![currentPosition]!!
        val file = File(currentData.url!!)
        val body = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val progressRequestBody = RetrofitUploadProgressUtil.getProgressRequestBody(body,
            object : RetrofitProgressUploadListener {
                override fun progress(progress: Int) {
                    LogUtils.d("${javaClass.simpleName}=>上传进度:图片progress=$progress")
                }

                override fun speedAndTimeLeft(speed: String, timeLeft: String) {

                }
            })
        val createFormData = MultipartBody.Part.createFormData(
            "file",
            URLEncoder.encode(file.name, "UTF-8"),
            progressRequestBody
        )
        val uploadPicture = RetrofitUtils.builder().uploadPicture(createFormData)
        uploadPictureObservable = uploadPicture.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            currentData.url = it.getData()!!.url
                            if (picAndVideoCount > currentPosition + 1) {
                                //说明还有
                                toDisposePicAndVideo(currentPosition + 1)
                            } else {
                                //说明没了
                                toSubmitReplay(Gson().toJson(result))
                            }
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
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

    /**
     * 上传视频
     */
    private fun toUploadVideo(currentPosition: Int) {
        val videoInfo = result?.getList()?.get(currentPosition)!!
        val url = videoInfo.url!!
        val videoFile = File(url)
        DialogUtils.setDialogMsg("${getString(R.string.string_005)}${currentPosition + 1}/$picAndVideoCount")
        val body = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile)
        val progressRequestBody = RetrofitUploadProgressUtil.getProgressRequestBody(body,
            object : RetrofitProgressUploadListener {
                override fun progress(progress: Int) {
                    LogUtils.d("${javaClass.simpleName}=>上传进度:视频progress=$progress")
                }

                override fun speedAndTimeLeft(speed: String, timeLeft: String) {
                }
            })
        val createFormData = MultipartBody.Part.createFormData(
            "file",
            URLEncoder.encode(videoFile.name, "UTF-8"),
            progressRequestBody
        )
        val only_url = RequestBody.create(MediaType.parse("text/plain"), "0")
        val uploadVideo = RetrofitUtils.builder().uploadVideo(createFormData, only_url)
        uploadVideoObservable = uploadVideo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            val currentData = result?.getList()!![currentPosition]!!
                            currentData.url = it.getData()!!.url
                            currentData.cover = it.getData()!!.cover
                            currentData.video_id = it.getData()!!.id
                            if (picAndVideoCount > currentPosition + 1) {
                                //说明还有
                                toDisposePicAndVideo(currentPosition + 1)
                            } else {
                                //说明没了
                                toSubmitReplay(Gson().toJson(result))
                            }
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
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

    /**
     * 提交评论
     */
    private fun toSubmitReplay(common: String) {
        DialogUtils.setDialogMsg(getString(R.string.string_006))
        val submitCommon = RetrofitUtils.builder().submitCommon(videoId.toInt(), common)
        submitCommonObservable = submitCommon.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            ToastUtils.show(getString(R.string.string_007))
                            data4MyCommon.clear()
                            data4CommonLists.clear()
                            getData()
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatTime(time: String): String {
        val timeMillis = time.toLong() * 1000
        val simpleDateFormat = SimpleDateFormat("MM-dd  HH:mm")
        return simpleDateFormat.format(timeMillis)
    }

    /**
     * 用来隐藏软键盘
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (isShouldHideKeyBoard(view, ev)) {
                OtherUtils.hindKeyboard(this, iv_comment_detail_back)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 判断点击去取是否需要隐藏软键盘
     */
    private fun isShouldHideKeyBoard(view: View?, event: MotionEvent): Boolean {
        if (view is EditText) {
            val arrayOf = intArrayOf(0, 0)
            val parent = view.parent as View
            parent.getLocationInWindow(arrayOf)
            val left = 0
            val top = arrayOf[1]
            val screenWidth = PhoneInfoUtils.getWidth(this)
            val needAdd =
                (Math.min(PhoneInfoUtils.getWidth(this), PhoneInfoUtils.getHeight(this))
                    .toFloat() / 360 * 40).toInt()
            val bottom = top + view.height + needAdd
            val right = screenWidth
            //不在EditText
            return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
        }
        //EditText没有焦点
        return false
    }
}