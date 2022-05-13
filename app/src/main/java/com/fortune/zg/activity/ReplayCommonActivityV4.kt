package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.view.KeyEvent
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
import com.fortune.zg.bean.CommonContentBean
import com.fortune.zg.bean.ReplayCommonListBean
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
import kotlinx.android.synthetic.main.activity_repaly_common.*
import kotlinx.android.synthetic.main.item_common_re.view.*
import kotlinx.android.synthetic.main.item_pic_video_big.view.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("INACCESSIBLE_TYPE")
class ReplayCommonActivityV4 : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: ReplayCommonActivityV4
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val SYSTEM = "system"
        const val VIDEO_ID = "video_id"
        const val VIDEO_LIST = "video_list"
        const val IMAGE_LIST = "image_list"
        const val VIDEO_FILE = "video_file"
        const val VIDEO_NAME = "video_name"
        const val VIDEO_DESC = "video_desc"
        const val VIDEO_COVER = "video_cover"
        const val COMMON_ID = "common_id"
        const val COMMON_HEAD = "common_head"
        const val COMMON_NAME = "common_name"
        const val COMMON_TIME = "common_time"
        const val COMMON_CONTENT = "common_content"
        const val COMMON_REPLAY = "common_replay"
        const val COMMON_LIKE = "common_like"
        const val COMMON_IS_LIKE = "common_is_like"
    }

    private var system = 1
    private var videoId = 0
    private var videoList = ""
    private var imageList = ""
    private var videoFile = ""
    private var videoName = ""
    private var videoDesc = ""
    private var videoCover = ""
    private var commonId = 0
    private var commonHead = ""
    private var commonName = ""
    private var commonTime = ""
    private var commonContent = ""
    private var commonReplay = 0
    private var commonLike = 0
    private var commonIsLike = false

    private var picAndVideoCount = 0
    private var result: CommonContentBean? = null
    private var uploadPictureObservable: Disposable? = null
    private var uploadVideoObservable: Disposable? = null
    private var replayCommonObservable: Disposable? = null

    private var replayCommonListObservable: Disposable? = null
    private var adapter: BaseAdapter<ReplayCommonListBean.DataBean.ListBean>? = null
    private var replayCommonLists = mutableListOf<ReplayCommonListBean.DataBean.ListBean>()
    private var currentPage = 1
    private var countPage = 1
    private var isReplay = false

    private var commonLikeObservable: Disposable? = null

    override fun getLayoutId() = R.layout.activity_repaly_common
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this

        system = intent.getIntExtra(SYSTEM, 1)
        videoId = intent.getIntExtra(VIDEO_ID, -1)
        videoList = intent.getStringExtra(VIDEO_LIST)!!
        imageList = intent.getStringExtra(IMAGE_LIST)!!
        videoFile = intent.getStringExtra(VIDEO_FILE)!!
        videoName = intent.getStringExtra(VIDEO_NAME)!!
        videoDesc = intent.getStringExtra(VIDEO_DESC)!!
        videoCover = intent.getStringExtra(VIDEO_COVER)!!
        commonId = intent.getIntExtra(COMMON_ID, 0)
        commonHead = intent.getStringExtra(COMMON_HEAD)!!
        commonName = intent.getStringExtra(COMMON_NAME)!!
        commonTime = intent.getStringExtra(COMMON_TIME)!!
        commonContent = intent.getStringExtra(COMMON_CONTENT)!!
        commonReplay = intent.getIntExtra(COMMON_REPLAY, 0)
        commonLike = intent.getIntExtra(COMMON_LIKE, 0)
        commonIsLike = intent.getBooleanExtra(COMMON_IS_LIKE, false)

        initView()
        getInfo()
    }

    /**
     * 获取评论
     */
    @SuppressLint("SetTextI18n")
    private fun getInfo(needDialog: Boolean = true) {
        if (needDialog)
            DialogUtils.showBeautifulDialog(this)
        val replayCommonList = RetrofitUtils.builder().replayCommonList(commonId, currentPage)
        replayCommonListObservable = replayCommonList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null && it.getData()?.paging?.count != null && it.getData()?.paging?.count!! > 0) {
                                replayCommonLists.addAll(it.getData()!!.list!!)
                                adapter?.notifyDataSetChanged()
                                tv_replay_common_title.text = "${it.getData()?.paging?.count!!}条回复"
                                tv_replay_common_replay.text =
                                    it.getData()?.paging?.count!!.toString()
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

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        ce_replay_common_addCommon.setHint("${getString(R.string.string_016)}$commonName")
        var mHeight = 0
        SoftKeyBoardChangeListener(this).init()
            .setHeightListener(object : SoftKeyBoardChangeListener.HeightListener {
                override fun onHeightChanged(height: Int) {
                    LogUtils.d("${javaClass.simpleName}=SoftKeyBoardChangeListener:$height")
                    if (height != mHeight) {
                        mHeight = height
                        if (height == 0) {
                            ce_replay_common_addCommon.changeSoftKeyBoardState(false)
                        } else {
                            ce_replay_common_addCommon.changeSoftKeyBoardState(true)
                        }
                    }
                }
            })
        ce_replay_common_addCommon.setOnCommonSubmit(object : CommonEdit.OnCommonSubmit {
            override fun submit(common: String) {
                toDisposeCommon(common)
            }
        })

        RxView.clicks(iv_replay_common_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (isReplay) {
                    setResult(RESULT_OK)
                }
                finish()
            }

        tv_replay_common_title.text = "${commonReplay}条回复"

        RxView.clicks(tv_replay_common_refresh)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //刷新
                currentPage = 1
                replayCommonLists.clear()
                getInfo()
            }

        Glide.with(this)
            .load(commonHead)
            .placeholder(R.mipmap.bg_gray_6)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(iv_replay_common_head)

        tv_replay_common_name.text = commonName
        tv_replay_common_time.text = formatTime(commonTime)

        val commentContent = Gson().fromJson<CommonContentBean>(
            commonContent,
            CommonContentBean::class.java
        )
        val isHasText = commentContent.getText() != null
                && commentContent.getText()?.isNotEmpty() == true
        tv_replay_common_common.visibility =
            if (isHasText) View.VISIBLE else View.GONE
        if (isHasText) {
            tv_replay_common_common.text = commentContent.getText()
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
        cv_replay_common_picAndVideo.adapter = picAndVideoAdapter
        cv_replay_common_picAndVideo.layoutManager = SafeStaggeredGridLayoutManager(
            3, StaggeredGridLayoutManager.VERTICAL
        )

        tv_replay_common_replay.text = commonReplay.toString()
        if (commonLike == 0) {
            tv_replay_common_like.visibility = View.GONE
        } else {
            tv_replay_common_like.visibility = View.VISIBLE
            tv_replay_common_like.text = commonLike.toString()
        }
        iv_replay_common_like.setImageResource(if (commonIsLike) R.mipmap.mv_icon_good_focus else R.mipmap.mv_icon_good)

        adapter = BaseAdapter.Builder<ReplayCommonListBean.DataBean.ListBean>()
            .setLayoutId(R.layout.item_common_re)
            .setData(replayCommonLists)
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
                    itemView.tv_item_common_replay_common.text = commentReplayContent.getText()
                }

                var replayPicAndVideoAdapter: BaseAdapter<CommonContentBean.ListBean>? = null
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
                replayPicAndVideoAdapter = BaseAdapter.Builder<CommonContentBean.ListBean>()
                    .setData(replayPicAndVideo)
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
                itemView.cv_item_common_replay_picAndVideo.adapter = replayPicAndVideoAdapter
                itemView.cv_item_common_replay_picAndVideo.layoutManager =
                    SafeStaggeredGridLayoutManager(
                        3, StaggeredGridLayoutManager.VERTICAL
                    )
            }
            .create()
        rv_replay_common.adapter = adapter
        rv_replay_common.layoutManager = SafeLinearLayoutManager(this)
        rv_replay_common.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

        refresh_replay_common.setEnableRefresh(false)
        refresh_replay_common.setEnableLoadMore(true)
        refresh_replay_common.setEnableLoadMoreWhenContentNotFull(false)
//        refresh_replay_common.setRefreshHeader(MaterialHeader(this))
        refresh_replay_common.setRefreshFooter(ClassicsFooter(this))

        refresh_replay_common.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    getInfo(false)
                } else {
                    refresh_replay_common.finishLoadMoreWithNoMoreData()
                }
            }
        })

        RxView.clicks(ll_replay_common_share)
            .throttleFirst(
                200, TimeUnit.MILLISECONDS
            ).subscribe {
                BottomDialog.shareMV(
                    this,
                    videoId.toString(),
                    videoName,
                    videoDesc,
                    videoCover,
                    object : BottomDialog.IsClickItem {
                        override fun isClickItem() {
                        }
                    })
            }

        RxView.clicks(ll_replay_common_msg)
            .throttleFirst(
                200, TimeUnit.MILLISECONDS
            ).subscribe {
                ce_replay_common_addCommon.getFocus()
            }

        var currentIsLike = commonIsLike
        RxView.clicks(ll_replay_common_good)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            ).subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    toCommonAddLikeNum(commonId)
                    currentIsLike = !currentIsLike
                    var count = tv_replay_common_like.text.toString().toInt()
                    count += if (currentIsLike) 1 else -1
                    tv_replay_common_like.text = "$count"
                    if (count > 0) {
                        tv_replay_common_like.visibility = View.VISIBLE
                    } else {
                        tv_replay_common_like.visibility = View.GONE
                    }
                    iv_replay_common_like.setImageResource(if (currentIsLike) R.mipmap.mv_icon_good_focus else R.mipmap.mv_icon_good)
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }
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
                    LogUtils.d("${javaClass.simpleName}=>上传进度:图片progress=$progress")
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
        val replayCommon = RetrofitUtils.builder().replayCommon(commonId, common)
        replayCommonObservable = replayCommon.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            isReplay = true
                            ToastUtils.show(getString(R.string.string_007))
                            currentPage = 1
                            replayCommonLists.clear()
                            getInfo()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isReplay) {
                LogUtils.d("replay..........1")
                setResult(RESULT_OK)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun destroy() {
        replayCommonListObservable?.dispose()
        replayCommonListObservable = null

        commonLikeObservable?.dispose()
        commonLikeObservable = null

        uploadPictureObservable?.dispose()
        uploadPictureObservable = null

        uploadVideoObservable?.dispose()
        uploadVideoObservable = null

        replayCommonObservable?.dispose()
        replayCommonObservable = null
    }


    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
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
                OtherUtils.hindKeyboard(this, iv_replay_common_back)
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