package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import cn.jzvd.*
import com.arialyy.aria.core.Aria
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fortune.zg.R
import com.fortune.zg.R.dimen.dp_90
import com.fortune.zg.adapter.BaseAdapter
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.*
import com.fortune.zg.event.GameDownload
import com.fortune.zg.event.GetGiftCode
import com.fortune.zg.event.LoginStatusChange
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.listener.OnBottomBarItemSelectListener
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.room.LocalGame
import com.fortune.zg.room.LocalGameDateBase
import com.fortune.zg.utils.*
import com.fortune.zg.widget.BarrageView
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_game_detail.*
import kotlinx.android.synthetic.main.fragment_game_list.view.*
import kotlinx.android.synthetic.main.item_gift_code_records.view.*
import kotlinx.android.synthetic.main.layout_item_detail_pic.view.*
import kotlinx.android.synthetic.main.layout_item_detail_video.view.*
import kotlinx.android.synthetic.main.layout_item_tag.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GameDetailActivity : BaseActivity() {

    private var isFromLive = false
    private var game_id: Int = -1
    private var need_save_page: Boolean = true
    private var game_cover: String? = null
    private var game_badge: String? = null
    private var game_name = ""
    private var game_desc = ""
    private var game_tag = ""
    private var game_hits = -1
    private var game_system = -1
    private var game_update_time = -1
    private var gameInfoObservable: Disposable? = null
    private lateinit var picAdapter: BaseAdapterWithPosition<String>
    private var picLists = mutableListOf<String>()
    private lateinit var videoAdapter: BaseAdapter<GameInfoBean.DataBean.GameVideoBean>
    private var videoLists = mutableListOf<GameInfoBean.DataBean.GameVideoBean>()
    private var videoItemLists = mutableListOf<JzvdStd>()

    private var getGiftObservable: Disposable? = null
    private var getRecordsObservable: Disposable? = null
    private var codeRecordsList = mutableListOf<GiftCodeRecordsBean.DataBean>()
    private var codeRecordsListAdapter: BaseAdapterWithPosition<GiftCodeRecordsBean.DataBean>? =
        null

    private var countDownObservable: Disposable? = null
    private var countDownTime = 0

    private var gameDownloadStartObservable: Disposable? = null
    private var gameDownloadCompleteObservable: Disposable? = null
    private var gameInstallStartObservable: Disposable? = null
    private var allCodeRecordsObservable: Disposable? = null
    private var downId: Int? = null

    private var collectGameObservable: Disposable? = null
    private var isCollect = false

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: GameDetailActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val GAME_ID = "game_id"
        const val IS_FROM_LIVE = "isFromLive"
        const val NEED_SAVE_PAGE = "need_save_page"
        const val GAME_COVER = "game_cover" //手游图标
        const val GAME_BADGE = "game_badge" //手游新标
    }

    private var gameIcon = "" //游戏icon
    private var downloadUrl = "" //下载地址
    private var downloadPath = "" //本地地址
    private var downloadSize = 0 //文件大小
    private var downloadPackageName = "" //包名
    private var downloadPackageVersion = "" //安装包版本
    private var isDownloading = false //是否正在下载
    private var taskId = -1L //下载Id
    private var isCancel = false //是否已经取消下载了
    private var isFirstComing = true //是否是第一次进入界面

    private var shareTitle = "" //分享标题
    private var shareTags = "" //分享标签
    private var isAndroidGame = false //是不是手游
    private var currentCodeRecords: AllCodeRecordsBean.DataBean? = null

    @SuppressLint("SimpleDateFormat")
    private val df = SimpleDateFormat("MM.dd")

    @SuppressLint("SimpleDateFormat")
    private val dateDf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

    private var numberFormat = DecimalFormat("#0.00")
    private var fl = 0.0

    override fun getLayoutId() = R.layout.activity_game_detail

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        EventBus.getDefault().register(this)
        instance = this
        isFromLive = intent.getBooleanExtra(IS_FROM_LIVE, false)
        game_id = intent.getIntExtra(GAME_ID, -1)
        need_save_page = intent.getBooleanExtra(NEED_SAVE_PAGE, true)
        game_cover = intent.getStringExtra(GAME_COVER)
        game_badge = intent.getStringExtra(GAME_BADGE)

        taskId = SPUtils.getLong("TASK_ID_$game_id", -1L)
        Aria.download(this).register()
        initView()
        getInfo()
    }

    /**
     * 获取全部领取记录
     */
    private fun toGetAllCodeRecords() {
        val allCodeRecords = RetrofitUtils.builder().allCodeRecords()
        allCodeRecordsObservable = allCodeRecords.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .throttleFirst(10, TimeUnit.SECONDS)
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null && it.getData()?.isNotEmpty() == true) {
                                AllCodeRecordsBean.setData(it.getData() as MutableList<AllCodeRecordsBean.DataBean?>?)
                                toShowAllCodeRecords()
                            }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                    }
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
            })
    }


    /**
     * 弹幕展示领取记录
     */
    @SuppressLint("CheckResult")
    private fun toShowAllCodeRecords() {
        AnimUtils.clear(fl_detail_barrage)
        barrage_detail_text.stopAnim()
        var data = AllCodeRecordsBean.getData()
        fl_detail_barrage.visibility = View.VISIBLE
        AnimUtils.alpha(fl_detail_barrage, true)
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                barrage_detail_text.setText(
                    this@GameDetailActivity,
                    data?.get(0)?.user_phone!!,
                    data?.get(0)?.game_name,
                    data?.get(0)?.game_id,
                    data?.get(0)?.game_cover!!,
                    data?.get(0)?.game_badge!!,
                    data?.get(0)?.video_id,
                    data?.get(0)?.video_pos,
                    data?.get(0)?.video_name,
                    false
                )
                barrage_detail_text.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
        barrage_detail_text.viewTreeObserver.addOnGlobalLayoutListener(listener)
        barrage_detail_text.setOnclick(object : BarrageView.OnTextClickListener {
            override fun setOnClick() {
                if (!need_save_page)
                    finish()
            }
        })
        barrage_detail_text.setOnAnimationEndListener(object :
            BarrageView.OnAnimationEndListener {
            override fun setOnAnimationRealEnd() {
                AllCodeRecordsBean.removeData()
            }

            override fun setOnAnimationEnd(time: Long) {
                AnimUtils.alpha(fl_detail_barrage, false, time)
                val timer = Timer()
                val timerTask = object : TimerTask() {
                    override fun run() {
                        if (AllCodeRecordsBean.getData() != null
                            && AllCodeRecordsBean.getData()?.isNotEmpty() == true
                        ) {
                            runOnUiThread {
                                fl_detail_barrage.visibility = View.VISIBLE
                                AnimUtils.alpha(fl_detail_barrage, true)
                                if (currentCodeRecords != null && currentCodeRecords?.user_phone != null) {
                                    barrage_detail_text.setText(
                                        this@GameDetailActivity, currentCodeRecords?.user_phone!!,
                                        currentCodeRecords?.game_name,
                                        currentCodeRecords?.game_id,
                                        currentCodeRecords?.game_cover!!,
                                        currentCodeRecords?.game_badge!!,
                                        null, null, null
                                    )
                                    currentCodeRecords = null
                                } else {
                                    data = AllCodeRecordsBean.getData()
                                    barrage_detail_text.setText(
                                        this@GameDetailActivity, data?.get(0)?.user_phone!!,
                                        data?.get(0)?.game_name,
                                        data?.get(0)?.game_id,
                                        data?.get(0)?.game_cover!!,
                                        data?.get(0)?.game_badge!!,
                                        null, null, null,
                                        false
                                    )
                                    barrage_detail_text.setOnclick(object :
                                        BarrageView.OnTextClickListener {
                                        override fun setOnClick() {
                                            if (!need_save_page)
                                                finish()
                                        }
                                    })
                                }
                            }
                        } else {
                            runOnUiThread {
                                toGetAllCodeRecords()
                                fl_detail_barrage.visibility = View.GONE
                            }
                        }
                    }
                }
                timer.schedule(timerTask, time + (0L..4000L).random())
            }
        })
    }


    /**
     * 获取游戏数据
     */
    private fun getInfo() {
        DialogUtils.showBeautifulDialog(this)
        val gameInfo = RetrofitUtils.builder().gameInfo(game_id)
        gameInfoObservable = gameInfo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            DialogUtils.dismissLoading()
                            val data = it.getData()
                            toInitView(data!!)
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                        3 -> {
                            DialogUtils.dismissLoading()
                            val intent = Intent()
                            intent.putExtra(GAME_ID, game_id)
                            setResult(2022, intent)
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                DialogUtils.dismissLoading()
            })
    }

    /**
     * 去展示数据
     */
    @SuppressLint("SetTextI18n", "CheckResult", "SimpleDateFormat")
    private fun toInitView(info: GameInfoBean.DataBean) {
        isAndroidGame = !info.game_down_info?.android_down_url.isNullOrEmpty()
                && info.game_down_info?.android_package_size != null
                && info.game_down_info?.android_package_size != 0
        //处理头儿
        if (info.game_video != null && info.game_video!!.isNotEmpty()) {
            //有视频显示视频
            video_detail_head.visibility = View.VISIBLE
            iv_detail_head.visibility = View.GONE
            Glide.with(this)
                .load(info.game_video!![0].cover)
                .placeholder(R.mipmap.bg_gray_6)
                .disallowHardwareConfig()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(video_detail_head.posterImageView)
            video_detail_head.posterImageView.scaleType = ImageView.ScaleType.FIT_XY
            Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT)
            video_detail_head.setUp(
                info.game_video!![0].url,
                null,
                JzvdStd.SCREEN_NORMAL
            )
            video_detail_head.startVideoAfterPreloading()
            if (isAndroidGame) {
                Glide.with(this)
                    .asBitmap()
                    .load(info.game_video!![0].cover)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            val width = resource.width
                            val height = resource.height
                            val screenWidth =
                                PhoneInfoUtils.getWidth(this@GameDetailActivity).toDouble()
                            val videoHeight = height * (screenWidth / width)
//                            LogUtils.d("height=$height,width=$width,screenWidth=$screenWidth,videoHeight=$videoHeight")
                            val layoutParams = video_detail_head.layoutParams
                            layoutParams.width = screenWidth.toInt()
                            layoutParams.height = videoHeight.toInt()
                            video_detail_head.layoutParams = layoutParams
                        }
                    })
            }
        } else {
            //没有视频,去加载截图
            video_detail_head.visibility = View.GONE
            iv_detail_head.visibility = View.VISIBLE
            if (info.game_pic != null && info.game_pic!!.isNotEmpty()) {
                //有图
                Glide.with(this)
                    .load(info.game_pic!![0])
                    .placeholder(R.mipmap.bg_gray_6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(iv_detail_head)
            } else {
                //没图就显示icon
                Glide.with(this)
                    .load(info.game_icon)
                    .placeholder(R.mipmap.bg_gray_6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(iv_detail_head)
            }
        }

        tv_detail_hot.text = info.game_hits.toString()

        //处理游戏信息
        Glide.with(this)
            .load(info.game_icon)
            .placeholder(R.mipmap.bg_gray_6)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(iv_detail_icon)
        game_name = info.game_name!!

        tv_detail_name.text = info.game_name
        shareTitle = info.game_name!!
        tv_detail_updateTime.text = df.format(info.game_update_time!! * 1000L)
        tv_detail_des.text = info.game_desc
        fl_detail.removeAllViews()
        for (index in info.game_tag!!.indices) {
            val view =
                LayoutInflater.from(this).inflate(R.layout.layout_item_tag, null)
            view.tv_tag.text = info.game_tag!![index]
            shareTags += if (info.game_tag != null && info.game_tag!!.isNotEmpty() && index != info.game_tag!!.size - 1) {
                "${info.game_tag!![index]}、"
            } else {
                info.game_tag!![index]
            }
            fl_detail.addView(view)
        }

        //收藏
        if (null != info.is_fav && info.is_fav == "1") {
            iv_detail_collect.setImageResource(R.mipmap.collect_in)
            tv_detail_collect.visibility = View.INVISIBLE
            isCollect = true
        } else {
            iv_detail_collect.setImageResource(R.mipmap.collect_un)
            tv_detail_collect.visibility = View.VISIBLE
            isCollect = false
        }

        //为了保存做的准备
        game_desc = info.game_desc!!
        game_tag = shareTags
        game_hits = info.game_hits!!
        game_system = info.game_system!!
        game_update_time = info.game_update_time!!

        if (isAndroidGame) {
            //Android数据存在,可以下载
            ll_detail_web.visibility = View.GONE
            ll_detail_phone.visibility = View.VISIBLE
            gameIcon = info.game_icon!!
            downloadUrl = info.game_down_info?.android_down_url!!
            downloadSize = info.game_down_info?.android_package_size!!
            tv_detail_download.text =
                "${getString(R.string.download)}(${numberFormat.format(downloadSize / 1024.0 / 1024.0)}M)"
        } else {
            ll_detail_web.visibility = View.VISIBLE
            ll_detail_phone.visibility = View.GONE
            tv_detail_web.text = info.game_down_info!!.windows_domain

            RxView.clicks(tv_detail_web)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    val intent = Intent(this, WebActivity::class.java)
                    intent.putExtra(WebActivity.TYPE, WebActivity.GAME_WEB)
                    intent.putExtra(WebActivity.GAME_NAME, info.game_name!!)
                    intent.putExtra(WebActivity.GAME_URL, info.game_down_info!!.windows_domain!!)
                    startActivity(intent)
                }
            RxView.longClicks(tv_detail_web)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    OtherUtils.copy(this, info.game_down_info!!.windows_domain!!)
                }
        }

        if (null == info.game_gift) {
            DialogUtils.dismissLoading()
            ll_detail_welfare.visibility = View.GONE
            ll_detail_records.visibility = View.GONE
        } else {
            if (MyApp.getInstance().isHaveToken()) {
                toGetRecords()
            } else {
                DialogUtils.dismissLoading()
            }

            ll_detail_welfare.visibility = View.VISIBLE

            tv_detail_gift_code_desc.text = info.game_gift?.desc
            tv_detail_gift_code_last.text = "x${info.game_gift?.last}"

            val width = PhoneInfoUtils.getWidth(this)
            if (info.game_gift?.count_down == null || info.game_gift?.count_down == 0) {
                tv_detail_gift_code_button_top.text = getString(R.string.get_gift_code)
                tv_detail_gift_code_button_bottom.text = getString(R.string.gift_code)
            } else {
                countDownTime = info.game_gift?.count_down!!
                countDownObservable = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (countDownTime > 0) {
                            ll_detail_get_gift_code.isEnabled = false
                            tv_detail_gift_code_button_top.text = TimeUtils.formatMs(countDownTime)
                            tv_detail_gift_code_button_bottom.text =
                                getString(R.string.to_get_gift_code)
                            countDownTime--
                        } else {
                            ll_detail_get_gift_code.isEnabled = true
                            tv_detail_gift_code_button_top.text = getString(R.string.get_gift_code)
                            tv_detail_gift_code_button_bottom.text = getString(R.string.gift_code)
                        }
                    }
            }
            tv_detail_gift_code_button_bottom.spacing = 30 * (360f / width)
            RxView.clicks(ll_detail_get_gift_code)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                )
                .subscribe {
                    if (info.game_gift?.count_down == 0 && info.game_gift?.last!! > 0) {
                        if (MyApp.getInstance().isHaveToken()) {
                            toGetGiftCode(info.game_system!!, info.game_gift?.id!!)
                        } else {
                            LoginUtils.toQuickLogin(this)
                        }
                    }
                }

            codeRecordsListAdapter = BaseAdapterWithPosition.Builder<GiftCodeRecordsBean.DataBean>()
                .setData(codeRecordsList)
                .setLayoutId(R.layout.item_gift_code_records)
                .addBindView { itemView, itemData, position ->
                    itemView.tv_item_gift_code_records_time.text =
                        dateDf.format(itemData.create_time!! * 1000L)
                    itemView.tv_item_gift_code_records_code.text = itemData.cdkey?.toString()
                }
                .create()
            rv_detail_records.adapter = codeRecordsListAdapter
            rv_detail_records.layoutManager = SafeLinearLayoutManager(this)
        }


        //处理更新信息
        tv_detail_version.text = "${getString(R.string.current_version)}: ${info.game_version}"
        tv_detail_days.text = getDays(info.game_update_time!!)
        tv_detail_logs.text = info.game_update_log

        RxView.clicks(tv_detail_allPic)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                MobclickAgent.onEvent(this, "review_img", "review_images")
                val intent = Intent(this, PicListActivity::class.java)
                intent.putExtra(PicListActivity.GAME_NAME, info.game_name)
                intent.putStringArrayListExtra(
                    PicListActivity.LIST,
                    picLists as ArrayList<String>
                )
                startActivity(intent)
            }

        //处理详情信息
        tv_detail_allPic.text =
            getString(R.string.all_pic).replace("0", info.game_pic?.size.toString())
        tv_detail_allVideo.text =
            getString(R.string.all_video).replace("0", info.game_video?.size.toString())
        for (pic in info.game_pic!!) {
            picLists.add(pic)
        }
        for (video in info.game_video!!) {
            videoLists.add(video)
        }
        picAdapter.notifyDataSetChanged()
        videoAdapter.notifyDataSetChanged()
        if (isAndroidGame) {
            downloadPackageName = info.game_down_info?.android_package_name!!
            downloadPackageVersion = info.game_down_info?.android_version_name!!
            val lastIndexOf = info.game_down_info?.android_down_url!!.lastIndexOf("/")
            val fileName =
                info.game_down_info?.android_down_url!!.substring(
                    lastIndexOf + 1,
                    info.game_down_info?.android_down_url!!.length
                )
            val dirPath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                .toString()
            downloadPath = "$dirPath/$fileName"
//                "${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()}/${downloadPackageName}_$downloadPackageVersion.apk"
            if (isInstall()) {
                //游戏已经安装了
                if (needUpdateGame()) {
                    //游戏需要更新,下载新包
                    tv_detail_download.text =
                        "${getString(R.string.update)}(${numberFormat.format(downloadSize / 1024.0 / 1024.0)}M)"
                } else {
                    //游戏不需要更新,直接打开游戏
                    tv_detail_download.text = getString(R.string.open_game)
                    //如果安装包还在的话,删除掉
                    toDeleteApk(true)
                }
            } else {
                if (isDownloadFile()) {
                    //已经下载完了
                    if (needUpdateApk()) {
                        //发现安装包需要更新,直接下载最新的安装包
                        tv_detail_download.text =
                            "${getString(R.string.download)}(${numberFormat.format(downloadSize / 1024.0 / 1024.0)}M)"
                        //此时需要删除老包
                        toDeleteApk(false)
                    } else {
                        //安装包不需要更新,直接安装使用
                        tv_detail_download.text = getString(R.string.install_now)
                    }
                } else {
                    //游戏没下载完成
                    when {
                        installPercent() == 0 -> {
                            //没有下载过
                            tv_detail_download.text =
                                "${getString(R.string.download)}(${numberFormat.format(downloadSize / 1024.0 / 1024.0)}M)"
                        }
                        else -> {
                            //下载过一点点
                            tv_detail_download.text = getString(R.string.re_download)
                        }
                    }
                }
            }

            RxView.clicks(ll_detail_phone)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (isAndroidGame) {
                        if (isCancel) {
                            isCancel = false
                            //已经被取消下载了,则直接开始从头开始下载
                            ll_detail_phone.setBackgroundResource(R.drawable.transparent)
                            tv_detail_download.text = "${getString(R.string.downloading)} 0%"
                            pb_detail_download.visibility = View.VISIBLE
                            pb_detail_download.progress = 0
                            iv_detail_cancel.visibility = View.VISIBLE
                            toDownload(info.game_down_info?.android_down_url!!)
                        } else {
                            //没有下载过
                            if (isInstall()) {
                                //已经安装
                                if (needUpdateGame()) {
                                    //需要更新,直接下载新包
                                    ll_detail_phone.setBackgroundResource(R.drawable.transparent)
                                    tv_detail_download.text =
                                        "${getString(R.string.downloading)} 0%"
                                    pb_detail_download.visibility = View.VISIBLE
                                    pb_detail_download.progress = 0
                                    iv_detail_cancel.visibility = View.VISIBLE
                                    toDownload(info.game_down_info?.android_down_url!!)
                                } else {
                                    //不需要更新
                                    toOpenGame()
                                }
                            } else {
                                if (isDownloadFile()) {
                                    //下载完成
                                    if (needUpdateApk()) {
                                        //需要更新安装包
                                        ll_detail_phone.setBackgroundResource(R.drawable.transparent)
                                        tv_detail_download.text =
                                            "${getString(R.string.downloading)} 0%"
                                        pb_detail_download.visibility = View.VISIBLE
                                        pb_detail_download.progress = 0
                                        iv_detail_cancel.visibility = View.VISIBLE
                                        toDownload(info.game_down_info?.android_down_url!!)
                                    } else {
                                        //不需要更新
                                        toInstallGame()
                                    }
                                } else {
                                    //没有下载过文件
                                    if (isFirstComing) {
                                        isFirstComing = false
                                        ll_detail_phone.setBackgroundResource(R.drawable.transparent)
                                        tv_detail_download.text =
                                            "${getString(R.string.downloading)} 0%"
                                        pb_detail_download.visibility = View.VISIBLE
                                        pb_detail_download.progress = 0
                                        iv_detail_cancel.visibility = View.VISIBLE
                                        toDownload(info.game_down_info?.android_down_url!!)
                                    } else {
                                        if (isDownloading) {
                                            isDownloading = false
                                            Aria.download(this)
                                                .load(taskId)
                                                .stop()
                                        } else {
                                            isDownloading = true
                                            Aria.download(this)
                                                .load(taskId)
                                                .setExtendField(
                                                    makeJson(
                                                        game_id,
                                                        game_name,
                                                        gameIcon,
                                                        downloadSize.toLong(),
                                                        downloadUrl,
                                                        downloadPackageName
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
        RxView.clicks(iv_detail_cancel)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                Aria.download(this)
                    .load(taskId)
                    .stop()
                //取消下载
                DialogUtils.showStopDownloadDialog(
                    this,
                    getString(R.string.cancel_download),
                    getString(R.string.cancel_download_msg),
                    getString(R.string.re_download),
                    getString(R.string.cancel_now),
                    object : DialogUtils.OnStopDownloadListener {
                        override fun cancel() {
                            Aria.download(this)
                                .load(taskId)
                                .setExtendField(
                                    makeJson(
                                        game_id,
                                        game_name,
                                        gameIcon,
                                        downloadSize.toLong(),
                                        downloadUrl,
                                        downloadPackageName
                                    )
                                )
                                .resume()
                        }

                        override fun next() {
                            isCancel = true
                            iv_detail_cancel.visibility = View.GONE
                            ll_detail_phone.setBackgroundResource(R.drawable.bg_green_6)
                            pb_detail_download.progress = 0
                            pb_detail_download.visibility = View.GONE
                            isDownloading = false
                            Aria.download(this@GameDetailActivity)
                                .load(taskId)
                                .cancel(true)
                        }
                    }
                )
            }
    }

    /**
     * 获取礼包码
     */
    @SuppressLint("SetTextI18n")
    private fun toGetGiftCode(system: Int, gift_id: Int) {
        DialogUtils.showBeautifulDialog(this)
        val getGiftCode = RetrofitUtils.builder().getGiftCode(game_id, gift_id)
        getGiftObservable = getGiftCode.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            MobclickAgent.onEvent(this, "game_gift_code", "game_gift_code_detail")
                            EventBus.getDefault().postSticky(GetGiftCode(1))
                            var last = tv_detail_gift_code_last.text.toString().trim()
                                .replace("x", "").toInt()
                            tv_detail_gift_code_last.text = "x${--last}"

                            toCountDown(it.getData()!!.count_down!!)
                            toGetRecords()

                            currentCodeRecords = AllCodeRecordsBean.DataBean()
                            currentCodeRecords?.game_name = game_name
                            currentCodeRecords?.user_phone = UserInfoBean.getData()!!.user_phone
                            currentCodeRecords?.game_id = game_id
                            currentCodeRecords?.game_cover = game_cover
                            currentCodeRecords?.game_badge = game_badge

                            GetGiftCodeDialog.show(
                                this@GameDetailActivity,
                                it.getData()!!.cdkey.toString()
                            )
                        }
                        3 -> {
                            //抢光了
                            tv_detail_gift_code_last.text = "x0"
                            toCountDown(it.getData()!!.count_down!!)
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                DialogUtils.dismissLoading()
            })
    }

    /**
     * 开始倒计时
     */
    private fun toCountDown(countDown: Int) {
        countDownTime = countDown
        countDownObservable = Observable.interval(1, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (countDownTime > 0) {
                    ll_detail_get_gift_code.isEnabled = false
                    tv_detail_gift_code_button_top.text = TimeUtils.formatMs(countDownTime)
                    tv_detail_gift_code_button_bottom.text =
                        getString(R.string.to_get_gift_code)
                    countDownTime--
                } else {
                    ll_detail_get_gift_code.isEnabled = true
                    tv_detail_gift_code_button_top.text = getString(R.string.get_gift_code)
                    tv_detail_gift_code_button_bottom.text = getString(R.string.gift_code)
                }
            }
    }


    @Subscribe(sticky = true)
    fun login(loginStatusChange: LoginStatusChange) {
        if (loginStatusChange.isLogin) {
            toGetRecords()
        }
    }

    /**
     * 获取礼包记录
     */
    private fun toGetRecords() {
        val giftCodeRecords = RetrofitUtils.builder().giftCodeRecords(game_id)
        getRecordsObservable = giftCodeRecords.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                codeRecordsList.clear()
                                for (data in it.getData()!!) {
                                    codeRecordsList.add(data!!)
                                }
                                codeRecordsListAdapter?.notifyDataSetChanged()
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
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                DialogUtils.dismissLoading()
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
     * 去删除安装包
     * @param isCurrentVersionApk 是不是当前版本的包
     */
    private fun toDeleteApk(isCurrentVersionApk: Boolean) {
        Thread {
            if (isCurrentVersionApk) {
                //删除当前包,直接使用当前包名+版本号
                val path =
                    "${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()}/${downloadPackageName}_$downloadPackageVersion.apk"
                DeleteApkUtils.deleteApk(File(path))
            } else {
                //不是当前包,则删除包名+非当前版本号
                val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                val files = downloadDir.listFiles()
                for (file in files) {
                    if (file.name.contains(downloadPackageName) && !file.name.contains(
                            downloadPackageVersion
                        )
                    ) {
                        DeleteApkUtils.deleteApk(File(file.path))
                    }
                }
            }
        }.start()
    }

    /**
     * 下载的安装包需不需要更新
     */
    private fun needUpdateApk(): Boolean {
        val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        when {
            downloadDir == null -> {
                return false
            }
            downloadDir.isFile -> {
                return false
            }
            downloadDir.isDirectory -> {
                val listFiles = downloadDir.listFiles()
                if (listFiles.isEmpty()) {
                    return false
                } else {
                    for (file in listFiles) {
                        return if (file.name.contains(downloadPackageName)) {
                            //文件名包含当前游戏包名,说明游戏文件存在
                            val lastIndexOf = file.name.lastIndexOf("_")
                            val apkVersion = file.name.substring(lastIndexOf + 1, file.name.length)
                                .replace(".apk", "")
                            downloadPackageVersion.replace(".", "")
                                .toInt() > apkVersion.replace(".", "").toInt()
                        } else {
                            false
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * 已安装游戏是否需要更新游戏
     */
    private fun needUpdateGame(): Boolean {
        val packageInfo = packageManager.getPackageInfo(downloadPackageName, 0)
        val versionName = packageInfo.versionName
        return downloadPackageVersion > versionName
    }

    /**
     * 判断文件是否完全下载下来
     */
    private fun isDownloadFile() = File(downloadPath).exists() && File(downloadPath).isFile


    /**
     * 下载进度,返回下载文件进度
     */
    private fun installPercent() = Aria.download(this)
        .load(taskId)
        .percent


    /**
     * 游戏是否安装过
     */
    private fun isInstall() =
        InstallApkUtils.isInstallApk(this, downloadPackageName)

    /**
     * 打开游戏
     */
    private fun toOpenGame() {
        val launchIntentForPackage = packageManager.getLaunchIntentForPackage(downloadPackageName)
        startActivity(launchIntentForPackage)
    }

    /**
     * 下载游戏
     */
    @SuppressLint("CheckResult")
    private fun toDownload(androidDownUrl: String) {
        if (isFromLive) {
            MobclickAgent.onEvent(
                this,
                "live",
                "live_game_download"
            )
        } else {
            MobclickAgent.onEvent(
                this,
                "game_download",
                "game_download_detail"
            )
        }
        taskId = Aria.download(this)
            .load(androidDownUrl) //读取下载地址
            .setFilePath(downloadPath, true) //设置文件保存的完整路径
            .setExtendField(
                makeJson(
                    game_id,
                    game_name,
                    gameIcon,
                    downloadSize.toLong(),
                    downloadUrl,
                    downloadPackageName
                )
            )
            .ignoreFilePathOccupy()
            .create()
        SPUtils.putValue("TASK_ID_$game_id", taskId)
        isDownloading = true
        if (taskId != -1L) {
            val gameDownStart = RetrofitUtils.builder().gameDownStart(game_id)
            gameDownloadStartObservable = gameDownStart.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    EventBus.getDefault().postSticky(GetGiftCode(2))
                    downId = it?.getData()?.down_id
                }, {
                    //异常的不管
                })
        }
    }

    /**
     * 计算更新时间
     */
    private fun getDays(updateTimeMillis: Int): String {
        val currentTimeMillis = System.currentTimeMillis() / 1000
        val l = (currentTimeMillis - updateTimeMillis) / 60 / 60 / 24
        return if (l <= 0) getString(R.string.update_today) else "$l${getString(R.string.days)}"
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_detail_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        RxView.clicks(tv_detail_records)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            )
            .subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    FlipAnimUtils.flip(this, ll_detail_welfare, ll_detail_records)
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }
        RxView.clicks(iv_detail_gift)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                FlipAnimUtils.flip(this, ll_detail_records, ll_detail_welfare)
            }

        picAdapter = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.layout_item_detail_pic)
            .setData(picLists)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData)
                    .placeholder(R.mipmap.bg_gray_6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.riv_detail_pic)
                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        MobclickAgent.onEvent(this, "review_img", "review_images")
                        val intent = Intent(this, ShowPicActivity::class.java)
                        intent.putExtra(ShowPicActivity.POSITION, position)
                        intent.putStringArrayListExtra(
                            ShowPicActivity.LIST,
                            picLists as ArrayList<String>
                        )
                        startActivity(intent)
                    }
            }
            .create()
        rv_detail_pic.adapter = picAdapter
        val linearLayoutManager = SafeLinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rv_detail_pic.layoutManager = linearLayoutManager

        videoAdapter = BaseAdapter.Builder<GameInfoBean.DataBean.GameVideoBean>()
            .setLayoutId(R.layout.layout_item_detail_video)
            .setData(videoLists)
            .addBindView { itemView, itemData ->
                Glide.with(this)
                    .load(itemData.cover)
                    .placeholder(R.mipmap.bg_gray_6)
                    .disallowHardwareConfig()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.video_detail_video.posterImageView)
                itemView.video_detail_video.posterImageView.scaleType = ImageView.ScaleType.FIT_XY
                itemView.video_detail_video.setUp(itemData.url, null, Jzvd.SCREEN_NORMAL)
                videoItemLists.add(itemView.video_detail_video)

                //视频的自动宽高
                if (isAndroidGame) {
                    Glide.with(this)
                        .asBitmap()
                        .load(itemData.cover)
                        .into(object : SimpleTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                val width = resource.width
                                val height = resource.height
                                val screenWidth =
                                    PhoneInfoUtils.getWidth(this@GameDetailActivity).toDouble()
                                val videoWidth = screenWidth / 360 * 205
                                val videoHeight = height * (videoWidth / width)
                                val layoutParams = itemView.video_detail_video.layoutParams
                                layoutParams.width = videoWidth.toInt()
                                layoutParams.height = videoHeight.toInt()
                                itemView.video_detail_video.layoutParams = layoutParams
                            }
                        })
                }
            }
            .create()
        rv_detail_video.adapter = videoAdapter
        val linearLayoutManager2 = SafeLinearLayoutManager(this)
        linearLayoutManager2.orientation = LinearLayoutManager.HORIZONTAL
        rv_detail_video.layoutManager = linearLayoutManager2

        tt_detail.setCurrentItem(1)
        tt_detail.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                if (fl > 0.2) {
                    when (index) {
                        1 -> {
                            nsv_detail.scrollTo(
                                0,
                                cv_detail_update.top - resources.getDimension(R.dimen.dp_115)
                                    .toInt()
                            )
                        }
                        2 -> {
                            nsv_detail.scrollTo(
                                0,
                                cv_detail_picAndVideo.top - resources.getDimension(R.dimen.dp_115)
                                    .toInt()
                            )
                        }
                        3 -> {
                            nsv_detail.scrollTo(
                                0,
                                cv_detail_common.top - resources.getDimension(R.dimen.dp_115)
                                    .toInt()
                            )
                        }
                        else -> {
                            nsv_detail.scrollTo(
                                0,
                                cv_detail_game.top - resources.getDimension(R.dimen.dp_115).toInt()
                            )
                        }
                    }
                }
            }
        })
        nsv_detail.setOnScrollChangeListener { v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
            fl = scrollY.toDouble() / resources.getDimension(dp_90).toDouble()
            ll_detail_top.alpha = fl.toFloat()
            if (scrollY == 0) {
                tt_detail.visibility = View.GONE
            } else {
                tt_detail.visibility = View.VISIBLE
            }

            when {
                scrollY >= cv_detail_common.top - resources.getDimension(R.dimen.dp_115) -> {
                    tt_detail.setCurrentItem(3)
                }
                scrollY >= cv_detail_picAndVideo.top - resources.getDimension(R.dimen.dp_115) -> {
                    tt_detail.setCurrentItem(2)
                }
                scrollY >= cv_detail_update.top - resources.getDimension(R.dimen.dp_115) -> {
                    tt_detail.setCurrentItem(1)
                }
                scrollY >= cv_detail_game.top - resources.getDimension(R.dimen.dp_115) -> {
                    //滑动到game
                    tt_detail.setCurrentItem(0)
                }
                else -> {
                    tt_detail.setCurrentItem(0)
                }
            }
        }

        RxView.clicks(iv_detail_share)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                BottomDialog.showShare(
                    this,
                    "http://www.5745.com/share/game/$game_id",
                    shareTitle,
                    shareTags
                )
            }

        RxView.clicks(ll_detail_collect)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            )
            .subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    toCollectOrUncollectGame()
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }
    }

    /**
     * 收藏/取消收藏
     */
    private fun toCollectOrUncollectGame() {
        val collectGame = RetrofitUtils.builder().collectGame(
            game_id,
            if (isCollect) 1 else 0
        )
        collectGameObservable = collectGame.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (isCollect) {
                                ToastUtils.show(getString(R.string.string_048))
                                iv_detail_collect.setImageResource(R.mipmap.collect_un)
                                tv_detail_collect.visibility = View.VISIBLE
                            } else {
                                ToastUtils.show(getString(R.string.string_049))
                                iv_detail_collect.setImageResource(R.mipmap.collect_in)
                                tv_detail_collect.visibility = View.INVISIBLE
                                MobclickAgent.onEvent(
                                    this,
                                    "collect",
                                    if (isAndroidGame) {
                                        "collect_phone"
                                    } else {
                                        "collect_pc"
                                    }
                                )
                            }
                            isCollect = !isCollect
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
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    override fun destroy() {
        EventBus.getDefault().unregister(this)

        downloadUrl = "" //下载地址
        downloadPath = "" //本地地址
        downloadSize = 0 //文件大小
        downloadPackageName = "" //包名
        downloadPackageVersion = "" //安装包版本
        isDownloading = false //是否正在下载
        taskId = -1L //下载Id
        isCancel = false //是否已经取消下载了
        isFirstComing = true //是否是第一次进入界面

        shareTitle = "" //分享标题
        shareTags = "" //分享标签
        isAndroidGame = false //是不是手游

        JZUtils.clearSavedProgress(this, null)
        JzvdStd.releaseAllVideos()

        gameInfoObservable?.dispose()
        gameInfoObservable = null

        gameDownloadStartObservable?.dispose()
        gameDownloadStartObservable = null
        gameDownloadCompleteObservable?.dispose()
        gameDownloadCompleteObservable = null
        gameInstallStartObservable?.dispose()
        gameInstallStartObservable = null
        allCodeRecordsObservable?.dispose()
        allCodeRecordsObservable = null

        getGiftObservable?.dispose()
        getGiftObservable = null
        getRecordsObservable?.dispose()
        getRecordsObservable = null
        countDownObservable?.dispose()
        countDownObservable = null

        collectGameObservable?.dispose()
        collectGameObservable = null
    }

    @SuppressLint("SetTextI18n")
    @Subscribe
    fun onDownload(gameDownload: GameDownload) {
        val task = gameDownload.task
        val extendField = task?.extendField
        val data = Gson().fromJson(extendField, GameDownloadNotify::class.java)
        if (data.gameVideoId == game_id) {
            when (gameDownload.state) {
                GameDownload.STATE.START -> {
                }
                GameDownload.STATE.RUNNING -> {
                    isDownloading = true
                    ll_detail_phone.setBackgroundResource(R.drawable.transparent)
                    tv_detail_download.text = "${getString(R.string.downloading)} ${task?.percent}%"
                    pb_detail_download.visibility = View.VISIBLE
                    pb_detail_download.progress = task?.percent!!
                    iv_detail_cancel.visibility = View.VISIBLE
                }
                GameDownload.STATE.PAUSE -> {
                    isDownloading = false
                    tv_detail_download.text = getString(R.string.re_download)
                }
                GameDownload.STATE.RESUME -> {
                    isDownloading = true
                    ll_detail_phone.setBackgroundResource(R.drawable.transparent)
                    pb_detail_download.visibility = View.VISIBLE
                    tv_detail_download.text = "${getString(R.string.downloading)} ${task?.percent}%"
                    pb_detail_download.progress = task?.percent!!
                    iv_detail_cancel.visibility = View.VISIBLE
                }
                GameDownload.STATE.COMPLETE -> {
                    toSaveLocalGame()
                    if (downId != null) {
                        val gameDownComplete = RetrofitUtils.builder().gameDownComplete(downId!!)
                        gameDownloadCompleteObservable =
                            gameDownComplete.subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    //正常的也不错
                                }, {
                                    //异常的不管
                                })
                    }
                    taskId = -1L
                    SPUtils.putValue("TASK_ID_$game_id", taskId)
                    isDownloading = false
                    pb_detail_download.visibility = View.GONE
                    iv_detail_cancel.visibility = View.GONE
                    ll_detail_phone.setBackgroundResource(R.drawable.bg_green_6)
                    pb_detail_download.progress = 100
                    tv_detail_download.text = getString(R.string.install_now)
                }
                GameDownload.STATE.CANCEL -> {
                    taskId = -1L
                    SPUtils.putValue("TASK_ID_$game_id", taskId)
                    isDownloading = false
                    pb_detail_download.visibility = View.GONE
                    iv_detail_cancel.visibility = View.GONE
                    ll_detail_phone.setBackgroundResource(R.drawable.bg_green_6)
                    tv_detail_download.text =
                        "${getString(R.string.download)}(${numberFormat.format(downloadSize / 1024.0 / 1024.0)}M)"
                }
                GameDownload.STATE.FAIL -> {
                }
            }
        }
    }

    /**
     * 安装游戏
     */
    private fun toInstallGame() {
        LogUtils.d("+++downloadPath = $downloadPath")
        if (downloadPath != "") {
            if (MyApp.isBackground) {
                runOnUiThread {
                    ToastUtils.show("《$shareTitle》${getString(R.string.download_success2install)}")
                }
            }
            val file = File(downloadPath)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri =
                    FileProvider.getUriForFile(this, "$packageName.provider", file)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(
                    Uri.fromFile(file),
                    "application/vnd.android.package-archive"
                )
            }
            startActivity(intent)
        }
    }

    /**
     * 安装啥的,重返当前界面时调用
     */
    @SuppressLint("SetTextI18n")
    override fun onRestart() {
        if (isInstall()) {
            if (downId != null) {
                val gameInstallComplete = RetrofitUtils.builder().gameInstallComplete(downId!!)
                gameInstallStartObservable = gameInstallComplete.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        //正常的也不错
                    }, {
                        //异常的不管
                    })
            }
            //如果安装包还在的话,删除掉
            toDeleteApk(true)
            //游戏已经安装了
            if (needUpdateGame()) {
                //游戏需要更新,下载新包
                tv_detail_download.text =
                    "${getString(R.string.update)}(${numberFormat.format(downloadSize / 1024.0 / 1024.0)}M)"
            } else {
                //游戏不需要更新,直接打开游戏
                tv_detail_download.text = getString(R.string.open_game)
            }
        } else {
            if (isDownloadFile()) {
                //已经下载完了
                if (needUpdateApk()) {
                    //发现安装包需要更新,直接下载最新的安装包
                    tv_detail_download.text =
                        "${getString(R.string.download)}(${numberFormat.format(downloadSize / 1024.0 / 1024.0)}M)"
                    //此时需要删除老包
                    toDeleteApk(false)
                } else {
                    //安装包不需要更新,直接安装使用
                    tv_detail_download.text = getString(R.string.install_now)
                }
            } else {
                //游戏没下载完成
                when {
                    installPercent() == 0 -> {
                        //没有下载过
                        isFirstComing = true
                        tv_detail_download.text =
                            "${getString(R.string.download)}(${numberFormat.format(downloadSize / 1024.0 / 1024.0)}M)"
                    }
                    else -> {
                        //下载过一点点
                        tv_detail_download.text = getString(R.string.re_download)
                    }
                }
            }
        }
        super.onRestart()
    }

    /**
     * 保存到本地游戏记录中
     */
    private fun toSaveLocalGame() {
        val localGameDateBase = LocalGameDateBase.getDataBase(this)
        val localGameDao = localGameDateBase.localGameDao()
        val all = localGameDao.all
        if (all.isNotEmpty()) {
            for (data in all) {
                if (data.game_id == game_id) {
                    localGameDao.delete(data)
                }
            }
        }
        val localGame = LocalGame(
            game_id = game_id,
            game_name = game_name,
            game_desc = game_desc,
            game_tag = game_tag,
            game_cover = game_cover!!,
            game_hits = game_hits,
            game_system = game_system,
            game_badge = game_badge!!,
            game_update_time = game_update_time
        )
        localGameDao.add(localGame)
        val all1 = localGameDao.all
        if (all1.size > 30) {
            localGameDao.delete(all1[0])
        }
        EventBus.getDefault().postSticky(GetGiftCode(2))
    }

    override fun onResume() {
        super.onResume()
        val data = AllCodeRecordsBean.getData()
        if (data.isNullOrEmpty()) {
            fl_detail_barrage.visibility = View.GONE
            toGetAllCodeRecords()
        } else {
            toShowAllCodeRecords()
        }
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        Jzvd.releaseAllVideos()
        MobclickAgent.onPause(this)
    }
}