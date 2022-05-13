package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.http.RetrofitProgressUploadListener
import com.fortune.zg.http.RetrofitUploadProgressUtil
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.issue.MakeVideoActivity
import com.fortune.zg.issue.PreviewActivity
import com.fortune.zg.issue.SelectMaterialUtil
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.luck.picture.lib.entity.LocalMedia
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_issue_mv.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Suppress("INACCESSIBLE_TYPE")
class IssueMvActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: IssueMvActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val MV_FILE = "mv_file"
        const val MV_COVER_TIME = "mv_cover_time"

        const val FROM_HIS = "from_his"
        const val MV_ID = "mv_id"
        const val MV_TITLE = "mv_title"
        const val MV_PATH = "mv_path"
        const val MV_COVER_PATH = "mv_cover_path"
        const val IS_ADVERTISING_MV = "is_advertising_mv"
        const val LONG_URL = "long_url"
        const val SHORT_URL = "short_url"
        const val AUDIT_STATUS = "audit_status"
        const val AUDIT_MSG = "audit_msg"

        const val VIDEO_PATH = "video_path"
        const val VIDEO_COVER_PAHT = "video_cover_path"
        const val PC_GIFT = "pc_gift"
    }

    private var mvFile: LocalMedia? = null
    private var mSelectData = mutableListOf<LocalMedia>()
    private var mMvCoverTime = 1L
    private var currentCoverFile: File? = null

    private var videoPath = ""
    private var videoCoverPath = ""

    private var isFromHis = false
    private var mMvId: Int? = null
    private var mMvTitle = ""
    private var mMvPath: String? = null
    private var mMvCoverPath: String? = null
    private var pcGift: String = ""

    private var mIsAdvertising = false
    private var mLongUrl = ""
    private var mShortUrl = ""
    private var mAuditStatus = -1
    private var mAuditMsg = ""
    private var isUploadVideoOver = false //视频是否上传完成

    private var isAdvertising = false //是不是广告视频
    private var getConfigObservable: Disposable? = null
    private var uploadVideoObservable: Disposable? = null
    private var uploadPictureObservable: Disposable? = null
    private var issueVideoObservable: Disposable? = null

    private var isFromMakeVideoActivity = false //是否来自视频编辑界面,false说明是直接发布
    override fun getLayoutId() = R.layout.activity_issue_mv

    override fun doSomething() {
        instance = this
        initView()
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        StatusBarUtils.setTextDark(this, true)
        isFromHis = intent.getBooleanExtra(FROM_HIS, false)
        if (isFromHis) {
            //从视频发布历史跳转过来
            mMvId = intent.getIntExtra(MV_ID, -1)
            mMvTitle = intent.getStringExtra(MV_TITLE)!!
            mMvPath = intent.getStringExtra(MV_PATH)!!
            mMvCoverPath = intent.getStringExtra(MV_COVER_PATH)!!
            mIsAdvertising = intent.getBooleanExtra(IS_ADVERTISING_MV, false)
            mLongUrl = intent.getStringExtra(LONG_URL)!!
            mShortUrl = intent.getStringExtra(SHORT_URL)!!
            mAuditStatus = intent.getIntExtra(AUDIT_STATUS, -1)
            mAuditMsg = intent.getStringExtra(AUDIT_MSG)!!
            pcGift = intent.getStringExtra(PC_GIFT)!!
            isUploadVideoOver = true
            et_issueMv_title.setText(mMvTitle)
            Glide.with(this)
                .load(mMvCoverPath)
                .into(iv_issueMv_mvCover)
            cb_issueMv.isChecked = mIsAdvertising
            isAdvertising = mIsAdvertising
            if (!mIsAdvertising) {
                tv_issueMv_tip.text = getString(R.string.string_025)
                et_issueMv_url.visibility = View.GONE
                view_issueMv_url.visibility = View.GONE
                ll_issueMv_url_short.visibility = View.GONE
            } else {
                toGetQQ()
                et_issueMv_url.setText(mLongUrl)
            }
            if (mAuditStatus == 2) {
                tv_issueMv_fail.visibility = View.VISIBLE
                tv_issueMv_fail.text = "${getString(R.string.string_030)}$mAuditMsg"
                tv_issueMv_issue.text = getString(R.string.string_029)
                iv_issueMv_title_delete.visibility = View.VISIBLE
                iv_issueMv_url_delete.visibility = View.VISIBLE
            } else {
                iv_issueMv_title_delete.visibility = View.GONE
                iv_issueMv_url_delete.visibility = View.GONE
                tv_issueMv_issue.visibility = View.GONE
                et_issueMv_title.isEnabled = false
                et_issueMv_url.isEnabled = false
                iv_issueMv_mvCover.isEnabled = false
                cb_issueMv.isEnabled = false
            }
        } else {
            //要发送视频
            tv_issueMv_status_root.visibility = View.VISIBLE
            videoPath = intent.getStringExtra(VIDEO_PATH)!!
            videoCoverPath = intent.getStringExtra(VIDEO_COVER_PAHT)!!
            if (videoCoverPath == "") {
                Thread {
                    isFromMakeVideoActivity = false
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(videoPath)
                    val frameAtIndex = mediaMetadataRetriever.getFrameAtTime(
                        1L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )!!
                    val bitmap = Bitmap.createScaledBitmap(
                        frameAtIndex,
                        frameAtIndex.width / 2,
                        frameAtIndex.height / 2,
                        false
                    )
                    frameAtIndex.recycle()
                    videoCoverPath =
                        getExternalFilesDir("pic2video").toString() + "/cover_${System.currentTimeMillis()}.jpg"
                    val fileOutputStream = FileOutputStream(File(videoCoverPath))
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                    fileOutputStream.flush()
                    fileOutputStream.close()
                    runOnUiThread {
                        currentCoverFile = File(videoCoverPath)
                        Glide.with(this)
                            .load(videoCoverPath)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(iv_issueMv_mvCover)
                    }
                }.start()
            } else {
                isFromMakeVideoActivity = true
                currentCoverFile = File(videoCoverPath)
                Glide.with(this)
                    .load(videoCoverPath)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(iv_issueMv_mvCover)
            }
            cb_issueMv.isChecked = false
            pb_issueMv_video.max = 100
            toUploadVideo(videoPath)
        }

        RxView.clicks(iv_issueMv_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        RxView.clicks(iv_issueMv_mvCover)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val intent = Intent(this, PreviewActivity::class.java)
                intent.putExtra(PreviewActivity.IS_VIDEO, true)
                if (isFromHis) {
                    intent.putExtra(PreviewActivity.FILE_PATH, mMvPath)
                    intent.putExtra(PreviewActivity.VIDEO_COVER, mMvCoverPath)
                } else {
                    intent.putExtra(PreviewActivity.FILE_PATH, videoPath)
                    intent.putExtra(PreviewActivity.VIDEO_COVER, videoCoverPath)
                }
                startActivity(intent)
            }

        cb_issueMv.setOnCheckedChangeListener { buttonView, isChecked ->
            isAdvertising = isChecked
            if (isChecked) {
                toGetQQ()
            } else {
                tv_issueMv_tip.text = getString(R.string.string_025)
                et_issueMv_url.visibility = View.GONE
                view_issueMv_url.visibility = View.GONE
                ll_issueMv_url_short.visibility = View.GONE
                ll_issueMv_giftDes.visibility = View.GONE
            }
        }
        RxTextView.textChanges(et_issueMv_title)
            .skipInitialValue()
            .subscribe {
                if (it.isEmpty()) {
                    iv_issueMv_title_delete.visibility = View.GONE
                } else {
                    iv_issueMv_title_delete.visibility = View.VISIBLE
                }
            }

        RxView.clicks(iv_issueMv_title_delete)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                et_issueMv_title.setText("")
            }

        RxTextView.textChanges(et_issueMv_url)
            .skipInitialValue()
            .subscribe {
                if (it.isEmpty()) {
                    iv_issueMv_url_delete.visibility = View.GONE
                } else {
                    iv_issueMv_url_delete.visibility = View.VISIBLE
                }
            }
        RxView.clicks(iv_issueMv_url_delete)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                et_issueMv_url.setText("")
            }

        RxView.clicks(tv_issueMv_issue)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toCheckInfo()
            }
    }

    /**
     * 上传视频
     */
    private fun toUploadVideo(videoPath: String) {
        isUploadVideoOver = false
        tv_issueMv_status.text = getString(R.string.string_043)
        ll_issueMv_status_des.visibility = View.VISIBLE
        val videoFile = File(videoPath)
        val body = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile)
        val progressRequestBody = RetrofitUploadProgressUtil.getProgressRequestBody(body,
            object : RetrofitProgressUploadListener {
                override fun progress(progress: Int) {
                    LogUtils.d("${javaClass.simpleName}=>上传进度:视频progress=$progress")
                    runOnUiThread {
                        pb_issueMv_video.progress = progress
                        if (progress == 100) {
                            tv_issueMv_status.text = getString(R.string.string_047)
                            ll_issueMv_status_des.visibility = View.GONE
                        }
                    }
                }

                override fun speedAndTimeLeft(speed: String, timeLeft: String) {
                    runOnUiThread {
                        tv_issueMv_speed.text = speed
                        tv_issueMv_timeLeft_num.text = timeLeft
                    }
                }
            })
        val createFormData = MultipartBody.Part.createFormData(
            "file",
            URLEncoder.encode(videoFile.name, "UTF-8"),
            progressRequestBody
        )
        val only_url = RequestBody.create(MediaType.parse("text/plain"), "1")
        val uploadVideo = RetrofitUtils.builder().uploadVideo(createFormData, only_url)
        uploadVideoObservable = uploadVideo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                isUploadVideoOver = true
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            mMvPath = it.getData()!!.url
                            tv_issueMv_status.text = getString(R.string.string_044)
                            ll_issueMv_status_des.visibility = View.GONE
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            tv_issueMv_status.text = getString(R.string.string_056)
                            ll_issueMv_status_des.visibility = View.GONE
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    tv_issueMv_status.text = getString(R.string.string_056)
                    ll_issueMv_status_des.visibility = View.GONE
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                tv_issueMv_status.text = getString(R.string.string_056)
                ll_issueMv_status_des.visibility = View.GONE
                isUploadVideoOver = true
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 检查数据
     */
    private fun toCheckInfo() {
        if (et_issueMv_title.text.isNotEmpty()) {
            if (OtherUtils.isGotOutOfLine(this, et_issueMv_title.text.toString().trim()) == 1) {
                ToastUtils.show("视频标题${getString(R.string.string_057)}")
            } else if (OtherUtils.isGotOutOfLine(
                    this,
                    et_issueMv_title.text.toString().trim()
                ) == 2
            ) {
                ToastUtils.show("视频标题${getString(R.string.string_053)}")
            } else {
                if (isAdvertising) {
                    if (et_issueMv_url.text.isNotEmpty()) {
                        if (OtherUtils.isUrl(et_issueMv_url.text.toString().trim())) {
                            infoSure()
                        } else {
                            ToastUtils.show(getString(R.string.string_055))
                        }
                    } else {
                        ToastUtils.show(getString(R.string.string_036))
                    }
                } else {
                    infoSure()
                }
            }
        } else {
            ToastUtils.show(getString(R.string.string_037))
        }
    }

    /**
     * 数据没问题了
     */
    private fun infoSure() {
        if (!isUploadVideoOver) {
            ToastUtils.show(getString(R.string.string_047))
        } else {
            DialogUtils.showDialogWithProgress(this, getString(R.string.string_038))
            if (null == mMvId) {
                //新上传
                toUploadVideoCover()
            } else {
                toIssueVideo()
            }
        }
    }

    /**
     * 上传视频封面
     */
    private fun toUploadVideoCover() {
        val body = RequestBody.create(MediaType.parse("multipart/form-data"), currentCoverFile!!)
        val createFormData = MultipartBody.Part.createFormData(
            "file",
            URLEncoder.encode(currentCoverFile!!.name, "UTF-8"),
            body
        )
        val uploadPicture = RetrofitUtils.builder().uploadPicture(createFormData)
        uploadPictureObservable = uploadPicture.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                isUploadVideoOver = true
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            mMvCoverPath = it.getData()!!.url
                            toIssueVideo()
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
                isUploadVideoOver = true
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 发布视频
     */
    private fun toIssueVideo() {
        val title = et_issueMv_title.text.toString().trim()
        val is_ad: String
        val game_url: String
        if (isAdvertising) {
            is_ad = "1"
            game_url = et_issueMv_url.text.toString().trim()
        } else {
            is_ad = "0"
            game_url = ""
        }

        val issueVideo = RetrofitUtils.builder().issueVideo(
            title,
            is_ad,
            game_url,
            mMvId,
            mMvPath!!,
            mMvCoverPath!!,
            getGiftInfo()
        )

        issueVideoObservable = issueVideo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            ToastUtils.show(getString(R.string.string_041))
                            if (intent.getBooleanExtra(FROM_HIS, false)) {
                                setResult(RESULT_OK)
                            }
                            if (isFromMakeVideoActivity) {
                                MakeVideoActivity.getInstance()?.finish()
                            }
                            SelectMaterialUtil.dismiss()
                            finish()
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
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
     * 获取礼包信息
     */
    private fun getGiftInfo(): String {
        var result = ""
        if (et_issueMv_giftDes1.text.isNotEmpty()) {
            result += "||${et_issueMv_giftDes1.text.toString().trim()}"
        }
        if (et_issueMv_giftDes2.text.isNotEmpty()) {
            result += "||${et_issueMv_giftDes2.text.toString().trim()}"
        }
        if (et_issueMv_giftDes3.text.isNotEmpty()) {
            result += "||${et_issueMv_giftDes3.text.toString().trim()}"
        }
        return if (result.startsWith("||")) result.substring(2, result.length) else result
    }

    /**
     * 如果选择了广告视频,则需要获取收费QQ
     */
    @SuppressLint("CheckResult")
    private fun toGetQQ() {
        DialogUtils.showBeautifulDialog(this)
        val getConfig = RetrofitUtils.builder().getConfig()
        getConfigObservable = getConfig.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            tv_issueMv_tip.text =
                                getString(R.string.string_026).replace("XX", it.data.qq)
                            et_issueMv_url.visibility = View.VISIBLE
                            if (isFromHis) {
                                if (pcGift != "") {
                                    ll_issueMv_giftDes.visibility = View.VISIBLE
                                    et_issueMv_giftDes1.isEnabled = false
                                    et_issueMv_giftDes2.isEnabled = false
                                    et_issueMv_giftDes3.isEnabled = false
                                    val giftList = pcGift.split("||")
                                    if (giftList.isNotEmpty()) {
                                        when (giftList.size) {
                                            1 -> {
                                                et_issueMv_giftDes1.setText(giftList[0])
                                            }
                                            2 -> {
                                                et_issueMv_giftDes1.setText(giftList[0])
                                                et_issueMv_giftDes2.setText(giftList[1])
                                            }
                                            3 -> {
                                                et_issueMv_giftDes1.setText(giftList[0])
                                                et_issueMv_giftDes2.setText(giftList[1])
                                                et_issueMv_giftDes3.setText(giftList[2])
                                            }
                                        }
                                    }
                                } else {
                                    ll_issueMv_giftDes.visibility = View.GONE
                                }
                            } else {
                                ll_issueMv_giftDes.visibility = View.VISIBLE
                            }
                            if (mShortUrl.isNotEmpty()) {
                                view_issueMv_url.visibility = View.VISIBLE
                                ll_issueMv_url_short.visibility = View.VISIBLE
                                tv_issueMv_url_short.text = mShortUrl
                            }
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

    override fun destroy() {
        getConfigObservable?.dispose()
        getConfigObservable = null

        uploadVideoObservable?.dispose()
        uploadVideoObservable = null

        uploadPictureObservable?.dispose()
        uploadPictureObservable = null

        issueVideoObservable?.dispose()
        issueVideoObservable = null
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

}