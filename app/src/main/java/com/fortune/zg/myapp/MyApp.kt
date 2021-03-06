package com.fortune.zg.myapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Build
import android.os.Bundle
import com.alibaba.sdk.android.push.CloudPushService
import com.alibaba.sdk.android.push.CommonCallback
import com.alibaba.sdk.android.push.huawei.HuaWeiRegister
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory
import com.alibaba.sdk.android.push.register.MiPushRegister
import com.alibaba.sdk.android.push.register.OppoRegister
import com.alibaba.sdk.android.push.register.VivoRegister
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.base.BaseAppUpdateSetting
import com.fortune.zg.base.BaseUiListener
import com.fortune.zg.constants.FilesArgument
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.event.RedPointChange
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.service.MyMessageIntentService
import com.fortune.zg.utils.*
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import com.qiniu.droid.imsdk.QNIMClient
import com.tencent.connect.share.QQShare
import com.tencent.connect.share.QQShare.SHARE_TO_QQ_TYPE_AUDIO
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXVideoObject
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.tencent.tauth.Tencent
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.umcrash.UMCrash
import im.floo.floolib.*
import io.microshow.rxffmpeg.RxFFmpegInvoke
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Author: ?????????
 * Time: 2020/4/14 9:56
 * Description:
 */

class MyApp : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: MyApp? = null
        var isBackground = false
        fun getInstance() = instance!!

        lateinit var mWxApi: IWXAPI
        lateinit var mTencent: Tencent

        init {
            System.loadLibrary("floo")
        }
    }

    private var currentActivity: String = ""

    override fun onCreate() {
        super.onCreate()
        instance = this
        val formatStrategy: FormatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(false)
            .tag("goodGames")
            .build()

        Logger.addLogAdapter(AndroidLogAdapter(formatStrategy))
        CrashHandler.instance!!.init(this)
        UMConfigure.init(
            this, "60150d9f6a2a470e8f981910", null,
            UMConfigure.DEVICE_TYPE_PHONE, ""
        )
        UMConfigure.setLogEnabled(BaseAppUpdateSetting.isDebug)
        UMCrash.registerUMCrashCallback {
            return@registerUMCrashCallback "UMCrash_hfdd"
        }
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL)
        UMConfigure.setProcessEvent(true)
        initWeChat()

        initPush(this)

        RxFFmpegInvoke.getInstance().setDebug(true)

        initQNIM()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity.javaClass.simpleName
                if (isBackground) {
                    isBackground = false
                    EventBus.getDefault().postSticky(RedPointChange())
                    LogUtils.d("app ????????????")
                }
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        })
    }

    /**
     * ????????????????????????
     */
    private fun initQNIM() {
        val appPath = applicationContext.filesDir.path
        val dataPath = File("$appPath/data_dir")
        val cachePath = File("$appPath/cache_dir")
        dataPath.mkdirs()
        cachePath.mkdirs()

        val pushId: String? = null

        val config = BMXSDKConfig(
            BMXClientType.Android,
            "1",
            dataPath.absolutePath,
            cachePath.absolutePath,
            pushId ?: "MaxIM"
        )
        config.consoleOutput = true
        config.logLevel = BMXLogLevel.Debug
        config.appID = "ekzkolgcdsbg"
        config.setEnvironmentType(BMXPushEnvironmentType.Development)
        QNIMClient.init(config)
    }


    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            isBackground = true
            LogUtils.d("app ???????????????")
        }
    }

    /**
     * ????????????Activity
     */
    fun getCurrentActivity() = currentActivity

    /**
     * ???????????????
     */
    private fun initPush(applicationContext: Application) {
        createNotificationChannel()
        PushServiceFactory.init(applicationContext)
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.setLogLevel(CloudPushService.LOG_DEBUG)
        pushService.setPushIntentService(MyMessageIntentService::class.java)
        pushService.register(applicationContext, object : CommonCallback {
            override fun onSuccess(response: String) {
                LogUtils.d("push_register_success=>$response")
            }

            override fun onFailed(errorCode: String, errorMessage: String) {
                LogUtils.d("push_register_fail=>$errorCode,$errorMessage")
            }

        })
        // ??????????????????????????????????????????????????????????????????????????????????????????
        MiPushRegister.register(applicationContext, "2882303761519866952", "5491986646952")
        // ??????????????????????????????????????????????????????????????????????????????????????????
        HuaWeiRegister.register(applicationContext)
        // vivo????????????
        VivoRegister.register(applicationContext)
        // OPPO???????????? appKey/appSecret???OPPO?????????????????????
        OppoRegister.register(
            applicationContext,
            "06c2f8fdbb7a4eaba79ea519d2a996f3",
            "701c15ae5a87421da84e4c24af24ca5e"
        )
    }

    /**
     * ???????????????
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            // ???????????????id
            val id = "1" //???????????????????????????????????????????????????????????????????????????Android 8.0?????????????????????????????????
            // ??????????????????????????????????????????.
            val name: CharSequence = getString(R.string.app_name)
            // ??????????????????????????????????????????
            val description = getString(R.string.quickly)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(id, name, importance)
            // ???????????????????????????
            mChannel.description = description
            // ??????????????????????????????????????? android ?????????????????????
            mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            // ??????????????????????????????????????? android ?????????????????????
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            //?????????notificationmanager????????????????????????
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }

    /**
     * ?????????????????????
     */
    private fun initWeChat() {
        mWxApi = WXAPIFactory.createWXAPI(this, FilesArgument.WECHAT_ID, true)
        // ??????
        mWxApi.registerApp(FilesArgument.WECHAT_ID)
        //?????????????????????????????????????????????????????????
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // ??????app???????????????
                mWxApi.registerApp(FilesArgument.WECHAT_ID)
            }
        }, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
        // Tencent??????SDK???????????????????????????????????????Tencent????????????????????????OpenAPI???
        // ??????APP_ID??????????????????????????????appid????????????String???
        mTencent = Tencent.createInstance(FilesArgument.QQ_ID, applicationContext)
    }

    /**
     * ?????????QQ??????QQ??????
     */
    fun shareToQQ(url: String, title: String, tags: String, activity: Activity, isQZone: Boolean) {
        if (!InstallApkUtils.isInstallQQ(this)) {
            ToastUtils.show(getString(R.string.need_install_qq))
            return
        }
        val params = Bundle()
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT)
        params.putString(QQShare.SHARE_TO_QQ_TITLE, title)
        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, tags)
        params.putString(QQShare.SHARE_TO_QQ_APP_NAME, getString(R.string.app_name))
        params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, url)
        if (isQZone) {
            params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN)
        }
        mTencent.shareToQQ(activity, params, BaseUiListener())
    }

    /***
     * ???????????????/?????????
     */
    fun shareToWechat(url: String, title: String, tags: String, friendsCircle: Boolean) {
        if (!InstallApkUtils.isInstallWeChat(this)) {
            ToastUtils.show(getString(R.string.need_install_wechat))
            return
        }
        val webpage = WXWebpageObject()
        webpage.webpageUrl = if (url.isEmpty()) "http://5745.com" else url
        val msg = WXMediaMessage(webpage)
        msg.title = title
        msg.description = tags
        val thumbBmp: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.share_b)!!
        msg.thumbData = bmpToByteArray(thumbBmp, true)
        val req = SendMessageToWX.Req()
        req.transaction = buildTransaction("webpage")
        req.message = msg
        req.scene =
            if (friendsCircle) SendMessageToWX.Req.WXSceneTimeline else SendMessageToWX.Req.WXSceneSession
        req.userOpenId = FilesArgument.WECHAT_ID

        //??????api??????????????????????????????
        mWxApi.sendReq(req)
    }

    /**
     * ???????????????qq
     */
    fun shareMVtoQQ(activity: Activity, url: String, title: String, des: String, cover: String) {
        if (!InstallApkUtils.isInstallQQ(this)) {
            ToastUtils.show(getString(R.string.need_install_qq))
            return
        }
        val params = Bundle()
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, SHARE_TO_QQ_TYPE_AUDIO)
        /**
         * ?????????????????????
        ????????????????????????????????????
        ???????????????
         */
        params.putString(QQShare.SHARE_TO_QQ_TITLE, "????????????????????????????????????,???????????????!")
        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, title)
        params.putString(
            QQShare.SHARE_TO_QQ_TARGET_URL,
            "${RetrofitUtils.baseUrl}index/video/?id=$url"
        )
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, cover)
        params.putString(QQShare.SHARE_TO_QQ_AUDIO_URL, url)
        params.putString(QQShare.SHARE_TO_QQ_APP_NAME, getString(R.string.app_name))
        mTencent.shareToQQ(activity, params, BaseUiListener())
    }

    /**
     * ?????????????????????
     */
    fun shareMVtoWechat(url: String, title: String, des: String, cover: String) {
        if (!InstallApkUtils.isInstallWeChat(this)) {
            ToastUtils.show(getString(R.string.need_install_wechat))
            return
        }
        //???????????????WXVideoObject?????????url
        val video = WXVideoObject()
        video.videoUrl = "${RetrofitUtils.baseUrl}index/video/?id=$url"
        //??? WXVideoObject ????????????????????? WXMediaMessage ??????
        val msg = WXMediaMessage(video)
        msg.title = "????????????????????????????????????,???????????????!"
        msg.description = title
        //????????????Req
        val req = SendMessageToWX.Req()
        req.transaction = buildTransaction("video")
        req.message = msg
        req.scene = SendMessageToWX.Req.WXSceneSession
        req.userOpenId = FilesArgument.WECHAT_ID
        var thumbBmp: Bitmap? = null
        Thread {
            try {
                val bitmap = Glide.with(this)
                    .asBitmap()
                    .load(cover)
                    .submit(200, 200)
                    .get()
                thumbBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                msg.thumbData = bmpToByteArray(thumbBmp!!, true)
                //??????api??????????????????????????????
                mWxApi.sendReq(req)
            }
        }.start()
    }

    /**
     * ???????????????qq
     */
    fun shareLiveToQQ(
        activity: Activity,
        url: String,
        title: String,
        liver: String,
        cover: String
    ) {
        if (!InstallApkUtils.isInstallQQ(this)) {
            ToastUtils.show(getString(R.string.need_install_qq))
            return
        }
        val params = Bundle()
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, SHARE_TO_QQ_TYPE_AUDIO)
        /**
         * ?????????????????????
        ????????????????????????????????????
        ???????????????
         */
        params.putString(QQShare.SHARE_TO_QQ_TITLE, "???????????????\"$liver\"?????????")
        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, title)
        params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, "http://5745.com")
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, cover)
        params.putString(QQShare.SHARE_TO_QQ_AUDIO_URL, url)
        params.putString(QQShare.SHARE_TO_QQ_APP_NAME, getString(R.string.app_name))
        mTencent.shareToQQ(activity, params, BaseUiListener())
    }

    /**
     * ?????????????????????
     */
    fun shareLiveToWechat(url: String, title: String, liver: String, cover: String) {
        if (!InstallApkUtils.isInstallWeChat(this)) {
            ToastUtils.show(getString(R.string.need_install_wechat))
            return
        }
        //???????????????WXVideoObject?????????url
        val video = WXVideoObject()
        video.videoUrl = "http://5745.com"
        //??? WXVideoObject ????????????????????? WXMediaMessage ??????
        val msg = WXMediaMessage(video)
        msg.title = "???????????????\"$liver\"?????????"
        msg.description = title
        //????????????Req
        val req = SendMessageToWX.Req()
        req.transaction = buildTransaction("video")
        req.message = msg
        req.scene = SendMessageToWX.Req.WXSceneSession
        req.userOpenId = FilesArgument.WECHAT_ID
        var thumbBmp: Bitmap? = null
        Thread {
            try {
                val bitmap = Glide.with(this)
                    .asBitmap()
                    .load(cover)
                    .submit(200, 200)
                    .get()
                thumbBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                msg.thumbData = bmpToByteArray(thumbBmp!!, true)
                //??????api??????????????????????????????
                mWxApi.sendReq(req)
            }
        }.start()
    }

    /**
     * ????????????,eg:1.2.0
     */
    fun getVersion() = packageManager.getPackageInfo(packageName, 0).versionName!!

    /**
     * ???????????????.eg:??????????????????
     */
    fun getVersionCode() = packageManager.getPackageInfo(packageName, 0).versionCode

    /**
     * ??????????????????
     */
    fun isHaveToken(): Boolean {
        val loginToken = SPUtils.getString(SPArgument.LOGIN_TOKEN, null)
        return loginToken?.isNotEmpty() == true
    }

    /**
     * ??????
     */
    private fun bmpToByteArray(bmp: Bitmap, needRecycle: Boolean): ByteArray? {
        var i: Int
        var j: Int
        if (bmp.height > bmp.width) {
            i = bmp.width
            j = bmp.width
        } else {
            i = bmp.height
            j = bmp.height
        }
        val localBitmap = Bitmap.createBitmap(i, j, Bitmap.Config.RGB_565)
        val localCanvas = Canvas(localBitmap)
        while (true) {
            localCanvas.drawBitmap(bmp, Rect(0, 0, i, j), Rect(0, 0, i, j), null)
            if (needRecycle) bmp.recycle()
            val localByteArrayOutputStream = ByteArrayOutputStream()
            localBitmap.compress(
                Bitmap.CompressFormat.JPEG, 100,
                localByteArrayOutputStream
            )
            localBitmap.recycle()
            val arrayOfByte = localByteArrayOutputStream.toByteArray()
            try {
                localByteArrayOutputStream.close()
                return arrayOfByte
            } catch (e: java.lang.Exception) {
                //F.out(e);
            }
            i = bmp.height
            j = bmp.height
        }
    }


    private fun buildTransaction(type: String?): String {
        return if (type == null) System.currentTimeMillis()
            .toString() else type + System.currentTimeMillis()
    }
}