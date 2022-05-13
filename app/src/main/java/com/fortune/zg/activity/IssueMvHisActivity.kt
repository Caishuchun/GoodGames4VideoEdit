package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.VideoHisBean
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.issue.SelectMaterialUtil
import com.fortune.zg.utils.*
import com.fortune.zg.video.VideoActivity
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.language.LanguageConfig
import com.luck.picture.lib.listener.OnResultCallbackListener
import com.luck.picture.lib.style.PictureCropParameterStyle
import com.luck.picture.lib.style.PictureParameterStyle
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_issue_mv_his.*
import kotlinx.android.synthetic.main.item_issue_mv_his.view.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class IssueMvHisActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: IssueMvHisActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    private var mAdapter: BaseAdapterWithPosition<VideoHisBean.Data.ListBean>? = null
    private var mData = mutableListOf<VideoHisBean.Data.ListBean>()
    private var currentPage = 1
    private var countPage = 1
    private var videoHisObservable: Disposable? = null
    private var deleteVideoObservable: Disposable? = null
    private var mvShareObservable: Disposable? = null
    private var isShare = false
    private var currentVideoId = -1
    private var currentShareNumText: TextView? = null

    @SuppressLint("SimpleDateFormat")
    private var dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    override fun getLayoutId() = R.layout.activity_issue_mv_his

    override fun doSomething() {
        instance = this
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        StatusBarUtils.setTextDark(this, true)
        RxView.clicks(iv_issueMvHis_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        RxView.clicks(iv_issueMvHis_issueMv)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
//                toSelectMv()
                SelectMaterialUtil.show(this, null)
            }

        refresh_issueMvHis.setEnableRefresh(true)
        refresh_issueMvHis.setEnableLoadMore(true)
        refresh_issueMvHis.setEnableLoadMoreWhenContentNotFull(false)
        refresh_issueMvHis.setRefreshHeader(MaterialHeader(this))
        refresh_issueMvHis.setRefreshFooter(ClassicsFooter(this))

        refresh_issueMvHis.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                currentPage = 1
                getInfo(needLoading = false, isRefresh = true)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    getInfo(needLoading = false, isRefresh = false)
                } else {
                    refresh_issueMvHis.finishLoadMoreWithNoMoreData()
                }
            }
        })

        rv_issueMvHis.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
        mAdapter = BaseAdapterWithPosition.Builder<VideoHisBean.Data.ListBean>()
            .setData(mData)
            .setLayoutId(R.layout.item_issue_mv_his)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData.user_avatar)
                    .placeholder(R.mipmap.bg_gray_6)
                    .into(itemView.riv_issueMvItem_headIcon)
                itemView.tv_issueMvItem_userName.text = itemData.user_name

                itemView.tv_issueMvItem_title.text = itemData.video_name
                itemView.tv_issueMvItem_status.text =
                    when (itemData.check_status) {
                        0 -> getString(R.string.string_050)
                        1 -> getString(R.string.string_051)
                        else -> getString(R.string.string_052)
                    }
                itemView.tv_issueMvItem_status.setTextColor(
                    when (itemData.check_status) {
                        0 -> Color.parseColor("#FF9C00")
                        1 -> Color.parseColor("#38C69A")
                        else -> Color.parseColor("#FB283F")
                    }
                )

                Glide.with(this)
                    .load(itemData.video_cover)
                    .placeholder(R.mipmap.bg_gray_6)
                    .into(itemView.iv_issueMvItem_cover)

                val width = itemData.video_cover_width
                val height = itemData.video_cover_height
                val newWidth: Int
                val newHeight: Int
                val layoutParams = itemView.rl_issueMvItem_root.layoutParams
                val screenWidth =
                    PhoneInfoUtils.getWidth(this).toDouble()
                if (width >= height) {
                    newWidth = (screenWidth / 360f * 328).toInt()
                    newHeight = (newWidth.toFloat() / width * height).toInt()
                } else {
                    newWidth = (screenWidth / 360f * 150).toInt()
                    newHeight = (newWidth.toFloat() / width * height).toInt()
                }
                layoutParams.width = newWidth
                layoutParams.height = newHeight
                itemView.rl_issueMvItem_root.layoutParams = layoutParams

                RxView.clicks(itemView.iv_issueMvItem_delete)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        DialogUtils.showDefaultDialog(this,
                            getString(R.string.string_033),
                            getString(R.string.string_034),
                            getString(R.string.cancel),
                            getString(R.string.sure),
                            object : DialogUtils.OnDialogListener {
                                override fun next() {
                                    deleteVideo(itemData.video_id)
                                }
                            })
                    }

                RxView.clicks(itemView.iv_issueMvItem_play)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toMvDetailActivity(
                            itemData.video_id,
                            itemData.video_cover,
                            itemData.video_cover_width,
                            itemData.video_cover_height
                        )
                    }

                RxView.clicks(itemView.iv_issueMvItem_cover)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toMvDetailActivity(
                            itemData.video_id,
                            itemData.video_cover,
                            itemData.video_cover_width,
                            itemData.video_cover_height
                        )
                    }

                itemView.tv_issueMvItem_updateTime.text = dateFormat.format(
                    itemData.video_update_time.toLong() * 1000
                )

                if (itemData.check_status == 1) {
                    itemView.ll_issueMvItem_bottom.visibility = View.VISIBLE
                    itemView.tv_issueMvItem_share_num.text = "${itemData.total_share}"
                    itemView.tv_issueMvItem_msg_num.text = "${itemData.total_comment}"
                    itemView.tv_issueMvItem_good_num.text = "${itemData.total_like}"
                    itemView.tv_issueMvItem_look_num.text = "${itemData.total_view}"

                    RxView.clicks(itemView.iv_issueMvItem_share)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            currentVideoId = itemData.video_id
                            currentShareNumText = itemView.tv_issueMvItem_share_num!!
                            BottomDialog.shareMV(
                                this,
                                itemData.video_id.toString(),
                                itemData.video_name,
                                itemData.video_desc,
                                itemData.video_cover,
                                object : BottomDialog.IsClickItem {
                                    override fun isClickItem() {
                                        isShare = true
                                    }
                                }
                            )
                        }

                    RxView.clicks(itemView.iv_issueMvItem_msg)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toCommentDetailActivity(
                                true,
                                itemData.video_id.toString(),
                                itemData.video_name,
                                itemData.video_desc,
                                Gson().toJson(itemData.videoList),
                                Gson().toJson(itemData.imageList)
                            )
                        }
                    RxView.clicks(itemView.iv_issueMvItem_good)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {}
                    RxView.clicks(itemView.iv_issueMvItem_look)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {}
                } else {
                    itemView.ll_issueMvItem_bottom.visibility = View.GONE
                }

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toIssueMvActivity(itemData)
                    }
            }
            .create()
        rv_issueMvHis.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
        rv_issueMvHis.adapter = mAdapter
        rv_issueMvHis.layoutManager = SafeLinearLayoutManager(this)
    }

    /**
     * 选择视频发布
     */
    private fun toSelectMv() {
        val pictureParameterStyle = PictureParameterStyle()
        pictureParameterStyle.isChangeStatusBarFontColor = true
        pictureParameterStyle.pictureStatusBarColor =
            ContextCompat.getColor(this, R.color.white_FFFFFF)
        pictureParameterStyle.isOpenCheckNumStyle = true
        pictureParameterStyle.pictureCheckedStyle = R.drawable.bg_pic_check
        pictureParameterStyle.isOpenCompletedNumStyle = true
        pictureParameterStyle.pictureTitleTextSize = 20
        pictureParameterStyle.pictureTitleTextColor =
            ContextCompat.getColor(this, R.color.black_1A241F)
        pictureParameterStyle.pictureCancelTextColor =
            ContextCompat.getColor(this, R.color.orange_FFC273)
        pictureParameterStyle.pictureLeftBackIcon = R.mipmap.back_black
        pictureParameterStyle.pictureTitleUpResId = R.mipmap.up
        pictureParameterStyle.pictureTitleDownResId = R.mipmap.down
        pictureParameterStyle.pictureBottomBgColor =
            ContextCompat.getColor(this, R.color.black_2A2C36)
        pictureParameterStyle.picturePreviewTextColor =
            ContextCompat.getColor(this, R.color.green_2EA992)
        pictureParameterStyle.pictureUnPreviewTextColor =
            ContextCompat.getColor(this, R.color.gray_F7F7F7)
        pictureParameterStyle.pictureCompleteTextColor =
            ContextCompat.getColor(this, R.color.green_2EA992)
        pictureParameterStyle.pictureUnCompleteTextColor =
            ContextCompat.getColor(this, R.color.gray_F7F7F7)

        val pictureCropParameterStyle = PictureCropParameterStyle(
            ContextCompat.getColor(this, R.color.white_FFFFFF),
            ContextCompat.getColor(this, R.color.white_FFFFFF),
            ContextCompat.getColor(this, R.color.black_2A2C36),
            pictureParameterStyle.isChangeStatusBarFontColor
        )

        PictureSelector.create(this)
            .openGallery(PictureMimeType.ofVideo())
            .isCamera(true)
            .selectionMode(PictureConfig.SINGLE)
            .maxSelectNum(1)
            .maxVideoSelectNum(1)
            .recordVideoSecond(3 * 60)
            .loadImageEngine(GlideEngine.createGlideEngine())
            .isCompress(true)
            .setPictureStyle(pictureParameterStyle)
            .setPictureCropStyle(pictureCropParameterStyle)
            .isWithVideoImage(false)
            .setLanguage(LanguageConfig.CHINESE)
            .forResult(object : OnResultCallbackListener<LocalMedia> {
                @SuppressLint("SetTextI18n")
                override fun onResult(result: MutableList<LocalMedia>?) {
                    LogUtils.d("${javaClass.simpleName}=PictureSelector==>${Gson().toJson(result)}")
                    val intent = Intent(this@IssueMvHisActivity, IssueMvActivity::class.java)
                    intent.putExtra(IssueMvActivity.MV_FILE, result!![0])
                    startActivity(intent)
                }

                override fun onCancel() {
                    LogUtils.d("${javaClass.simpleName}=PictureSelector==>onCancel")
                }
            })
    }

    /**
     * 跳转到视频详情界面
     */
    private fun toMvDetailActivity(
        videoId: Int,
        videoCover: String,
        videoCoverWidth: Int,
        videoCoverHeight: Int
    ) {
        val intent = Intent(this, VideoActivity::class.java)
        intent.putExtra(VideoActivity.VIDEO_TYPE, 1)
        intent.putExtra(
            VideoActivity.VIDEO_ID,
            videoId
        )
        intent.putExtra(VideoActivity.VIDEO_COVER, videoCover)
        intent.putExtra(VideoActivity.VIDEO_COVER_WIDTH, videoCoverWidth)
        intent.putExtra(VideoActivity.VIDEO_COVER_HEIGHT, videoCoverHeight)
        intent.putExtra(VideoActivity.IS_MAIN_VIDEO, false)
        startActivity(intent)
    }

    /**
     * 跳转到评论界面
     */
    private fun toCommentDetailActivity(
        isMainVideo: Boolean,
        videoId: String,
        videoName: String,
        videoDesc: String,
        videoList: String?,
        ImageList: String?
    ) {
        val intent = Intent(this, CommentDetailActivityV4::class.java)
        intent.putExtra(CommentDetailActivityV4.SYSTEM, 1)
        intent.putExtra(CommentDetailActivityV4.IS_MAIN_VIDEO, isMainVideo)
        intent.putExtra(CommentDetailActivityV4.VIDEO_ID, videoId)
        intent.putExtra(CommentDetailActivityV4.VIDEO_NAME, videoName)
        intent.putExtra(CommentDetailActivityV4.VIDEO_DESC, videoDesc)
        intent.putExtra(CommentDetailActivityV4.VIDEO_LIST, "")
        intent.putExtra(CommentDetailActivityV4.IMAGE_LIST, "")
        startActivity(intent)
    }

    /**
     * 跳转到发布视频界面
     */
    private fun toIssueMvActivity(info: VideoHisBean.Data.ListBean) {
        val intent = Intent(this, IssueMvActivity::class.java)
        intent.putExtra(IssueMvActivity.FROM_HIS, true)
        intent.putExtra(IssueMvActivity.MV_ID, info.video_id)
        intent.putExtra(IssueMvActivity.MV_TITLE, info.video_name)
        intent.putExtra(IssueMvActivity.MV_PATH, info.video_file)
        intent.putExtra(IssueMvActivity.MV_COVER_PATH, info.video_cover)
        intent.putExtra(IssueMvActivity.IS_ADVERTISING_MV, info.is_ad == 1)
        intent.putExtra(IssueMvActivity.LONG_URL, info.game_url)
        intent.putExtra(IssueMvActivity.SHORT_URL, info.game_url_short)
        intent.putExtra(IssueMvActivity.AUDIT_STATUS, info.check_status)
        intent.putExtra(IssueMvActivity.AUDIT_MSG, info.refuse_reason)
        intent.putExtra(IssueMvActivity.PC_GIFT, info.pc_gift)
        startActivityForResult(intent, 1020)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1020 && resultCode == RESULT_OK) {
            getInfo(needLoading = true, isRefresh = false)
        }
    }

    /**
     * 删除视频
     */
    private fun deleteVideo(video_id: Int) {
        DialogUtils.showBeautifulDialog(this)
        val deleteVideo = RetrofitUtils.builder().deleteVideo(video_id)
        deleteVideoObservable = deleteVideo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            for (data in mData) {
                                if (data.video_id == video_id) {
                                    mData.remove(data)
                                    break
                                }
                            }
                            mAdapter?.notifyDataSetChanged()
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
     * 获取视频发布历史
     */
    @SuppressLint("CheckResult")
    private fun getInfo(needLoading: Boolean, isRefresh: Boolean) {
        if (needLoading) {
            DialogUtils.showBeautifulDialog(this)
        }
        val videoHis = RetrofitUtils.builder().videoHis(currentPage)
        videoHisObservable = videoHis.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (needLoading) {
                    DialogUtils.dismissLoading()
                }
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (isRefresh) {
                    refresh_issueMvHis.finishRefresh()
                } else {
                    refresh_issueMvHis.finishLoadMore()
                }
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (currentPage == 1) {
                                mData.clear()
                                mAdapter?.notifyDataSetChanged()
                            }
                            val count = it.data.paging.count
                            val limit = it.data.paging.limit
                            countPage = count / limit
                            if (count % limit != 0) {
                                countPage++
                            }
                            if (countPage == 0) {
                                countPage = 1
                            }
                            if (it.data.list.isNotEmpty()) {
                                mData.addAll(it.data.list)
                            }
                            mAdapter?.notifyDataSetChanged()
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
                if (isRefresh) {
                    refresh_issueMvHis.finishRefresh()
                } else {
                    refresh_issueMvHis.finishLoadMore()
                }
                if (needLoading) {
                    DialogUtils.dismissLoading()
                }
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    override fun destroy() {
        videoHisObservable?.dispose()
        videoHisObservable = null

        deleteVideoObservable?.dispose()
        deleteVideoObservable = null

        mvShareObservable?.dispose()
        mvShareObservable = null
    }


    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
        if (isShare) {
            isShare = false
            toAddShareNum()
        }
        getInfo(needLoading = true, isRefresh = false)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
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
}