package com.fortune.zg.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import cn.jzvd.JzvdStd
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadTask
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.AllCodeRecordsBean
import com.fortune.zg.bean.GameDownloadNotify
import com.fortune.zg.bean.RedPointBean
import com.fortune.zg.bean.VersionBean
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.event.*
import com.fortune.zg.fragment.*
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.issue.*
import com.fortune.zg.listener.OnBottomBarItemSelectListener
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.room.DownloadGame
import com.fortune.zg.room.DownloadGameDataBase
import com.fortune.zg.utils.*
import com.fortune.zg.utils.ActivityManager
import com.fortune.zg.widget.BarrageView
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.luck.picture.lib.tools.PictureFileUtils
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_v5.*
import kotlinx.android.synthetic.main.activity_search_game.*
import kotlinx.android.synthetic.main.fragment_game_list.view.*
import me.weyye.hipermission.HiPermission
import me.weyye.hipermission.PermissionCallback
import me.weyye.hipermission.PermissionItem
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import android.os.Bundle

import android.app.Activity

import com.fortune.zg.plugin.PluginManager
import com.fortune.zg.plugin.ProxyActivity

import dalvik.system.DexClassLoader
import java.lang.reflect.Method


class MainActivityV5 : BaseActivity() {

    private var homeFragment: HomeFragmentV5? = null
    private var pcFragment: PcFragmentV2? = null
    private var phoneFragment: PhoneFragmentV2? = null
    private var navigationFragment: NavigationFragment? = null
    private var canQuit = false
    private var currentFragment: Fragment? = null
    private var downloadPath = "" //下载的安装路径
    private var isDownloadApp = false //是否在下载app
    private var allCodeRecordsObservable: Disposable? = null

    private var intentFilter: IntentFilter? = null
    private var timeChangeReceiver: TimeChangeReceiver? = null

    @SuppressLint("SimpleDateFormat")
    class TimeChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK -> {
                    if (MyApp.getInstance().isHaveToken()) {
                        val currentTimeMillis = System.currentTimeMillis()
                        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        val currentTime = df.format(currentTimeMillis)
                        val hour = currentTime.split(" ")[1].split(":")[0]
                        val minute = currentTime.split(" ")[1].split(":")[1]
                        if ((hour == "05" || hour == "10" || hour == "15" || hour == "20") && minute == "00") {
                            LogUtils.d("=======$hour:$minute")
                            EventBus.getDefault().postSticky(RedPointChange())
                            //好巧不巧,正好处于白嫖界面等着的话,需要通知白嫖获取新数据
                            if (MyApp.getInstance().getCurrentActivity() == "GiftActivity") {
                                EventBus.getDefault().post(
                                    GiftNeedNewInfo(
                                        isShowDailyCheckNeed = false,
                                        isShowWhitePiaoNeed = true,
                                        isShowInviteGiftNeed = false
                                    )
                                )
                            }
                        }
                        //如果更巧的话,在晚上12点卡点,礼包三个界面都需要重新获取数据
                        if (hour == "00" && minute == "00") {
                            EventBus.getDefault().postSticky(RedPointChange())
                            if (MyApp.getInstance().getCurrentActivity() == "GiftActivity") {
                                EventBus.getDefault().post(
                                    GiftNeedNewInfo(
                                        isShowDailyCheckNeed = true,
                                        isShowWhitePiaoNeed = true,
                                        isShowInviteGiftNeed = true
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: MainActivityV5
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        var mainPage: MainPage = MainPage.MAIN
    }

    enum class MainPage {
        MAIN, PHONE, ISSUE, PC, MINE
    }

    override fun getLayoutId() = R.layout.activity_main_v5

    //为了不保存Fragment,直接清掉
    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
    }

    /**
     * 双击退出
     */
    @SuppressLint("CheckResult")
    override fun onBackPressed() {
        if (canQuit) {
            super.onBackPressed()
        } else {
            if (currentFragment == homeFragment) {
                homeFragment?.currentFragment?.toJump()
            }
            ToastUtils.show(getString(R.string.double_click_quit))
            canQuit = true
            Observable.timer(2, TimeUnit.SECONDS)
                .subscribe {
                    canQuit = false
                }
        }
    }

    @SuppressLint("CheckResult")
    override fun doSomething() {
        PluginManager.setContext(this)
        PictureFileUtils.deleteAllCacheDirFile(this)

        instance = this
        EventBus.getDefault().register(this)
        StatusBarUtils.setTextDark(this, false)
        Aria.download(this).register()

        homeFragment = HomeFragmentV5.newInstance()
        currentFragment = homeFragment
        navigationFragment = NavigationFragment.newInstance()

        initView()
        toDeleteGameApk()

        intentFilter = IntentFilter()
        intentFilter?.addAction(Intent.ACTION_TIME_TICK)
        if (timeChangeReceiver == null) {
            timeChangeReceiver = TimeChangeReceiver()
        }
        registerReceiver(timeChangeReceiver, intentFilter)
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
        AnimUtils.clear(fl_mainV5_barrage)
        barrage_mainV5_text.stopAnim()
        var data = AllCodeRecordsBean.getData()
        if (data == null || data.isEmpty()) {
            Thread {
                Thread.sleep(2000)
                runOnUiThread {
                    toGetAllCodeRecords()
                    fl_mainV5_barrage.visibility = View.GONE
                }
            }.start()
            return
        }
        fl_mainV5_barrage.visibility = View.VISIBLE
        AnimUtils.alpha(fl_mainV5_barrage, true)
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                barrage_mainV5_text.setText(
                    this@MainActivityV5,
                    data?.get(0)?.user_phone!!,
                    data?.get(0)?.game_name,
                    data?.get(0)?.game_id,
                    data?.get(0)?.game_cover!!,
                    data?.get(0)?.game_badge!!,
                    data?.get(0)?.video_id,
                    data?.get(0)?.video_pos,
                    data?.get(0)?.video_name
                )
                barrage_mainV5_text.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
        barrage_mainV5_text.viewTreeObserver.addOnGlobalLayoutListener(listener)
        barrage_mainV5_text.setOnAnimationEndListener(object :
            BarrageView.OnAnimationEndListener {
            override fun setOnAnimationRealEnd() {
                AllCodeRecordsBean.removeData()
            }

            override fun setOnAnimationEnd(time: Long) {
                runOnUiThread {
                    AnimUtils.alpha(fl_mainV5_barrage, false, time)
                }
                val timer = Timer()
                val timerTask = object : TimerTask() {
                    override fun run() {
                        if (AllCodeRecordsBean.getData() != null
                            && AllCodeRecordsBean.getData()?.isNotEmpty() == true
                        ) {
                            runOnUiThread {
                                data = AllCodeRecordsBean.getData()
                                fl_mainV5_barrage.visibility = View.VISIBLE
                                AnimUtils.alpha(fl_mainV5_barrage, true)
                                barrage_mainV5_text.setText(
                                    this@MainActivityV5,
                                    data?.get(0)?.user_phone!!,
                                    data?.get(0)?.game_name,
                                    data?.get(0)?.game_id,
                                    data?.get(0)?.game_cover!!,
                                    data?.get(0)?.game_badge!!,
                                    data?.get(0)?.video_id,
                                    data?.get(0)?.video_pos,
                                    data?.get(0)?.video_name
                                )
                            }
                        } else {
                            runOnUiThread {
                                toGetAllCodeRecords()
                                fl_mainV5_barrage.visibility = View.GONE
                            }
                        }
                    }
                }
                timer.schedule(timerTask, time + (0L..4000L).random())
            }
        })
    }

    /**
     * 删除游戏安装包
     */
    private fun toDeleteGameApk() {
        //没有安装目录,就是么有安装包,啥也不做
        val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
        if (downloadDir.isFile) {
            //检查到安装目录变成文件,也就是么有安装包,啥也不做
            return
        }
        if (downloadDir.isDirectory) {
            //安装目录是正经的文件夹了
            val files = downloadDir.listFiles()
            if (files.isEmpty()) {
                //可是它为空,也就是么有安装包,啥也不做
                return
            }
            for (file in files) {
                if (file.isFile && file.name.endsWith(".apk") && !file.name.contains(packageName)) {
                    //文件夹下有文件,并且就是文件,最重要的是apk文件
                    val split = file.name.split("_")
                    val version = split[split.size - 1]
                    val packageName = file.name.replace(version, "").replace(".apk", "")
                    if (InstallApkUtils.isInstallApk(this, packageName)) {
                        Thread {
                            DeleteApkUtils.deleteApk(File("${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()}/${file.name}"))
                        }.start()
                    }
                }
            }
        }
    }

    /**
     * apk下载
     */
    private fun toDownloadApk(updateUrl: String) {
        isDownloadApp = true
        Aria.download(this)
            .load(updateUrl) //读取下载地址
            .setFilePath(downloadPath, true) //设置文件保存的完整路径
            .ignoreFilePathOccupy()
            .ignoreCheckPermissions()
            .create()
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        if (homeFragment == null && fragment is HomeFragmentV5) {
            homeFragment = fragment
        } else if (phoneFragment == null && fragment is PhoneFragmentV2) {
            phoneFragment = fragment
        } else if (pcFragment == null && fragment is PcFragmentV2) {
            pcFragment = fragment
        } else if (navigationFragment == null && fragment is NavigationFragment) {
            navigationFragment = fragment
        }
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fl_mainV5, navigationFragment!!)
            .add(R.id.fl_mainV5, homeFragment!!)
            .commit()

        val data = VersionBean.getData()
        if (data != null) {
            val newVersion = data.version_name!!.replace(".", "").toInt()
            val currentVersion = MyApp.getInstance().getVersion().replace(".", "").toInt()
            LogUtils.d("toDownLoadApk==>newVersion = $newVersion, currentVersion = $currentVersion")
//            isDownloadApp = true
            val versionName = data.update_url!!.substring(data.update_url!!.lastIndexOf("/") + 1)
            downloadPath =
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + versionName
            SPUtils.putValue(SPArgument.APP_DOWNLOAD_PATH, downloadPath)
            if (newVersion > currentVersion) {
                //需要更新的话,直接更新
                if (isApkDownload(File(downloadPath))) {
                    installAPK(File(downloadPath))
                } else {
                    toDownloadApk(data.update_url!!)
                }
            } else {
                if (isApkDownload(File(downloadPath))) {
                    toDeleteApk()
                }
                //已经更新过
                val isNeedUpdateDialog = SPUtils.getBoolean(SPArgument.IS_NEED_UPDATE_DIALOG, true)
                if (isNeedUpdateDialog) {
                    SPUtils.putValue(SPArgument.IS_NEED_UPDATE_DIALOG, false)
                    VersionDialog.show(
                        this@MainActivityV5,
                        data.update_msg.toString(),
                        object : VersionDialog.OnUpdateAPP {
                            override fun onUpdate() {
                                ll_mainV5_shade_root.visibility = View.VISIBLE
                            }
                        }
                    )
                }
            }
        }

        tab_mainV5.setCurrentItem(0)
        toChangeFragment(0)

        tab_mainV5.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                toChangeFragment(index)
            }
        })

        RxView.clicks(tv_mainV5_know)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                ll_mainV5_shade_root.visibility = View.GONE
            }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun login(loginStatusChange: LoginStatusChange) {
        LogUtils.d("loginStatusChange.isLogin:${loginStatusChange.isLogin},mainPage:$mainPage")
        if (loginStatusChange.isLogin && mainPage == MainPage.MINE) {
            LogUtils.d("loginStatusChange")
            tab_mainV5.setCurrentItem(4)
            toChangeFragment(4)
        }
    }

    /**
     * 更新fragment
     */
    @SuppressLint("HandlerLeak")
    private fun toChangeFragment(index: Int) {
        when (index) {
            0 -> {
                if (currentFragment != homeFragment) {
                    hideAll()
                    currentFragment = homeFragment
                    supportFragmentManager.beginTransaction()
                        .show(currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    homeFragment?.refresh()
                }
            }
            1 -> {
                hideAll()
                if (null == phoneFragment) {
                    phoneFragment = PhoneFragmentV2.newInstance()
                    currentFragment = phoneFragment
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_mainV5, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    currentFragment = phoneFragment
                    supportFragmentManager.beginTransaction()
                        .show(currentFragment!!)
                        .commitAllowingStateLoss()
                }
            }
            2 -> {
                val permissions = arrayListOf(
                    PermissionItem(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        "文件管理",
                        R.drawable.permission_ic_storage
                    )
                )
                HiPermission.create(this)
                    .permissions(permissions)
                    .msg("为了您正常使用好服多多,需要以下权限:\n文件管理:用以发布视频缓存数据")
                    .filterColor(ResourcesCompat.getColor(resources, R.color.green_2EC8AC, theme))
                    .style(R.style.PermissionStyle)
                    .checkMutiPermission(object : PermissionCallback {
                        override fun onClose() {
                            LogUtils.d("HiPermission=>onClose()")
                        }

                        override fun onFinish() {
                            LogUtils.d("HiPermission=>onFinish()")
                            if (currentFragment == homeFragment) {
                                JzvdStd.goOnPlayOnPause()
                            }
                            SelectMaterialUtil.show(this@MainActivityV5, object : SelectMaterialUtil.CancelCallBack {
                                override fun cancel() {
                                    if (currentFragment == homeFragment) {
                                        val currentJzvd = homeFragment!!.currentFragment!!.getCurrentJzvd()
                                        currentJzvd?.startVideoAfterPreloading()
                                    }
                                }
                            })
//                            toStartGame()
                        }

                        override fun onDeny(permission: String?, position: Int) {
                            LogUtils.d("HiPermission=>onDeny(permission:$permission,position:$position)")
                        }

                        override fun onGuarantee(permission: String?, position: Int) {
                            LogUtils.d("HiPermission=>onGuarantee(permission:$permission,position:$position)")
                        }
                    })
            }
            3 -> {
                hideAll()
                if (null == pcFragment) {
                    pcFragment = PcFragmentV2.newInstance()
                    currentFragment = pcFragment
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_mainV5, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    currentFragment = pcFragment
                    supportFragmentManager.beginTransaction()
                        .show(currentFragment!!)
                        .commitAllowingStateLoss()
                }
            }
            4 -> {
                hideAll()
                if (null == navigationFragment) {
                    navigationFragment = NavigationFragment.newInstance()
                    currentFragment = navigationFragment
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_mainV5, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    currentFragment = navigationFragment
                    supportFragmentManager.beginTransaction()
                        .show(currentFragment!!)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    private var count = 0

    @SuppressLint("SdCardPath")
    private fun toStartGame() {
//        val bundle = Bundle()
//        bundle.putBoolean("KEY_START_FROM_OTHER_ACTIVITY", true)
        val dexpath =
            "/sdcard/Android/data/com.fortune.zg/files/Download/9004543154eab94b0ccfecdb7cd5ac0c.apk"
//        val dexoutputpath = "/sdcard/Android/data/com.fortune.zg/files/Download/"
//        loadAPK(bundle, dexpath, dexoutputpath)

        if (count == 0) {
            PluginManager.loadPath(dexpath)
            count++
        } else {
            count--
            val intent = Intent(this, ProxyActivity::class.java)
            intent.putExtra(ProxyActivity.CLASS_NAME, PluginManager.enterActivityName)
            startActivity(intent)
        }
    }

    fun loadAPK(paramBundle: Bundle, dexpath: String?, dexoutputpath: String?) {
        val localClassLoader = ClassLoader.getSystemClassLoader()
        val localDexClassLoader = DexClassLoader(dexpath, dexoutputpath, null, localClassLoader)
        try {
            val plocalObject =
                packageManager.getPackageArchiveInfo(dexpath!!, PackageManager.GET_ACTIVITIES)
            if (plocalObject!!.activities != null && plocalObject.activities.isNotEmpty()) {
                val activityname = plocalObject.activities[0].name
                LogUtils.d("activityname = $activityname")
                val localClass = localDexClassLoader.loadClass(activityname)
                val localConstructor = localClass.getConstructor(*arrayOf())
                val instance = localConstructor.newInstance(*arrayOf())
                LogUtils.d("instance = $instance")
                val methodonCreate = localClass.getDeclaredMethod(
                    "init", *arrayOf<Class<*>>(
                        Bundle::class.java
                    )
                )
                methodonCreate.isAccessible = true
                methodonCreate.invoke(instance, paramBundle)
            }
            return
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }


    @SuppressLint("WrongConstant")
    fun LoadAPK(paramBundle: Bundle, dexpath: String?, dexoutputpath: String?) {
        val localClassLoader = ClassLoader.getSystemClassLoader()
        val localDexClassLoader = DexClassLoader(
            dexpath,
            dexoutputpath, null, localClassLoader
        )
        try {
            val plocalObject = packageManager.getPackageArchiveInfo(dexpath!!, 1)
            if (plocalObject!!.activities != null
                && plocalObject.activities.isNotEmpty()
            ) {
                val activityname = plocalObject.activities[0].name
                LogUtils.d("activityname = $activityname")
//                val intent = Intent(Intent.ACTION_MAIN)
//                intent.addCategory(Intent.CATEGORY_LAUNCHER)
//                val cn = ComponentName("com.xinyu.wangzhesm201213_57450112", "com.xinyu.pingtai.MainActivity")
//                intent.component = cn
//                startActivity(intent)
                val localClass = localDexClassLoader.loadClass(activityname)
//                val intent = Intent(this, localClass)
//                startActivity(intent)

                val localConstructor = localClass.getConstructor(*arrayOf())
                val instance: Any = localConstructor.newInstance(arrayOf<Any>())
                LogUtils.d("instance = $instance")
                val localMethodSetActivity: Method = localClass.getDeclaredMethod(
                    "setActivity", *arrayOf<Class<*>>(Activity::class.java)
                )
                localMethodSetActivity.setAccessible(true)
                localMethodSetActivity.invoke(instance, arrayOf<Any>(this))
                val methodonCreate: Method = localClass.getDeclaredMethod(
                    "onCreate", *arrayOf<Class<*>>(Bundle::class.java)
                )
                methodonCreate.setAccessible(true)
                methodonCreate.invoke(instance, arrayOf<Any>(paramBundle))
            }
            return
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * 隐藏掉所有东西
     */
    private fun hideAll() {
        supportFragmentManager.beginTransaction()
            .hide(currentFragment!!)
//            .hide(homeFragment!!)
//            .hide(pcFragment ?: homeFragment!!)
//            .hide(phoneFragment ?: homeFragment!!)
//            .hide(navigationFragment ?: homeFragment!!)
            .commitAllowingStateLoss()
    }

    override fun destroy() {
        allCodeRecordsObservable?.dispose()
        allCodeRecordsObservable = null

        EventBus.getDefault().unregister(this)
        downloadPath = ""
        SPUtils.putValue(SPArgument.APP_DOWNLOAD_PATH, null)
        isDownloadApp = false
        Aria.download(this).stopAllTask()
        Aria.download(this).resumeAllTask()

        VersionDialog.dismiss()
        unregisterReceiver(timeChangeReceiver)
    }

    override fun onTaskResume(task: DownloadTask?) {
        if (isDownloadApp) {
            if (!ApkDownloadDialog.isShowing())
                ApkDownloadDialog.showDialog(this)
            ApkDownloadDialog.setProgress(task?.percent ?: 0)
        } else {
            val extendField = task?.extendField
            LogUtils.d("++++++++++++resume$extendField")
            EventBus.getDefault().postSticky(GameDownload(task, GameDownload.STATE.RESUME))
        }
    }

    override fun onTaskStart(task: DownloadTask?) {
        if (isDownloadApp) {
            if (!ApkDownloadDialog.isShowing())
                ApkDownloadDialog.showDialog(this)
            ApkDownloadDialog.setProgress(task?.percent ?: 0)
        } else {
            val extendField = task?.extendField
            LogUtils.d("++++++++++++start$extendField")
            EventBus.getDefault().postSticky(GameDownload(task, GameDownload.STATE.START))
        }
    }

    override fun onTaskStop(task: DownloadTask?) {
        if (!isDownloadApp) {
            val extendField = task?.extendField
            LogUtils.d("++++++++++++stop$extendField")
            val data = Gson().fromJson(extendField, GameDownloadNotify::class.java)
            cancelNotify(data.gameVideoId)
            EventBus.getDefault().postSticky(GameDownload(task, GameDownload.STATE.PAUSE))
        }
    }

    override fun onTaskCancel(task: DownloadTask?) {
        if (isDownloadApp) {
            ApkDownloadDialog.dismissLoading()
        } else {
            val extendField = task?.extendField
            LogUtils.d("++++++++++++cancel$extendField")
            val data = Gson().fromJson(extendField, GameDownloadNotify::class.java)
            cancelNotify(data.gameVideoId)
            EventBus.getDefault().postSticky(GameDownload(task, GameDownload.STATE.CANCEL))
        }
        isDownloadApp = false
    }

    override fun onTaskFail(task: DownloadTask?, e: Exception?) {
        if (isDownloadApp) {
            ApkDownloadDialog.dismissLoading()
        } else {
            val extendField = task?.extendField
            LogUtils.d("++++++++++++fail$extendField")
            val data = Gson().fromJson(extendField, GameDownloadNotify::class.java)
            cancelNotify(data.gameVideoId)
            EventBus.getDefault().postSticky(GameDownload(task, GameDownload.STATE.FAIL))
        }
        isDownloadApp = false
    }

    @SuppressLint("CheckResult")
    override fun onTaskComplete(task: DownloadTask?) {
        if (isDownloadApp) {
            isDownloadApp = false
            ApkDownloadDialog.setProgress(100)
            ApkDownloadDialog.dismissLoading()
            Aria.download(this).stopAllTask()
            Aria.download(this).removeAllTask(false)
            if (isApkDownload(File(downloadPath)) && !MyApp.isBackground) {
                installAPK(File(downloadPath))
            }
        } else {
            val extendField = task?.extendField
            LogUtils.d("++++++++++++complete$extendField")
            val data = Gson().fromJson(extendField, GameDownloadNotify::class.java)
            SPUtils.putValue("TASK_ID_${data.gameVideoId}", -1L)
            cancelNotify(data.gameVideoId)
            Thread.sleep(200)
            toInstallGame(task?.filePath)
            val dataBase = DownloadGameDataBase.getDataBase(this)
            val downloadGameDao = dataBase.downloadGameDao()
            val all = downloadGameDao.all
            if (all.isEmpty()) {
                downloadGameDao.add(
                    DownloadGame(
                        data.gameVideoId,
                        data.gameName,
                        data.gameIcon,
                        data.gameSize,
                        data.gameDownloadUrl,
                        data.gamePackageName
                    )
                )
            } else {
                for (game in all) {
                    if (game.video_id == data.gameVideoId) {
                        downloadGameDao.delete(game)
                        break
                    }
                }
                downloadGameDao.add(
                    DownloadGame(
                        data.gameVideoId,
                        data.gameName,
                        data.gameIcon,
                        data.gameSize,
                        data.gameDownloadUrl,
                        data.gamePackageName
                    )
                )
            }
            EventBus.getDefault().postSticky(GameDownload(task, GameDownload.STATE.COMPLETE))
        }
    }

    /**
     * 安装游戏
     */
    private fun toInstallGame(filePath: String?) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val file = File(filePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val apkUri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
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
        startActivity(intent)
    }

    var isPause = false
    override fun onTaskRunning(task: DownloadTask?) {
        if (isDownloadApp) {
            if (currentFragment == homeFragment) {
                val currentJzvd = homeFragment?.currentFragment?.getCurrentJzvd()
                if (currentJzvd != null && !isPause) {
                    isPause = true
                    JzvdStd.goOnPlayOnPause()
                }
            }
            if (!ApkDownloadDialog.isShowing())
                ApkDownloadDialog.showDialog(this)
            ApkDownloadDialog.setProgress(task?.percent!!)
        } else {
            val extendField = task?.extendField
            LogUtils.d("++++++++++++running$extendField")
            val data = Gson().fromJson(extendField, GameDownloadNotify::class.java)
            showNotify(data.gameName ?: "", data.gameVideoId, data.gameIcon, task!!.percent)
            EventBus.getDefault().postSticky(GameDownload(task, GameDownload.STATE.RUNNING))
        }
    }

    /**
     * 跳转到home界面
     */
    fun toHomeFragment() {
        tab_mainV5.setCurrentItem(0)
        toChangeFragment(0)
    }

    /**
     * 创建通知栏
     */
    private fun showNotify(gameName: String, videoId: Int, gameIcon: String, progress: Int) {
        LogUtils.d("++++++++++++++++++showNotify")
        val view = RemoteViews(packageName, R.layout.notify_download)
        view.setProgressBar(R.id.pb_notify_progress, 100, progress, false)
        view.setTextViewText(R.id.tv_notify_title, gameName)
        view.setTextViewText(R.id.tv_notify_progress, "正在下载 $progress%")
        val intent = Intent(this, DownloadActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val build = NotificationCompat.Builder(this, "2")
            .setSmallIcon(R.mipmap.icon_small)
            .setContent(view)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel("2", "游戏下载", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "好服多多游戏下载"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(videoId, build.build())
    }

    /**
     * 干掉通知栏
     */
    private fun cancelNotify(videoId: Int) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(videoId)
    }

    /**
     * 判断文件是否完全下载下来
     */
    private fun isApkDownload(file: File) = file.exists() && file.isFile

    /**
     * 去删除安装包
     */
    private fun toDeleteApk() {
        Thread {
            DeleteApkUtils.deleteApk(File(downloadPath))
        }.start()
    }

    /**
     *下载到本地后执行安装
     */
    private fun installAPK(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canRequestPackageInstalls = packageManager.canRequestPackageInstalls()
            if (canRequestPackageInstalls) {
                toInstallApp(file)
            } else {
                val uri = Uri.parse("package:$packageName")
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
                startActivityForResult(intent, 100)
                return
            }
        } else {
            toInstallApp(file)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && packageManager.canRequestPackageInstalls()) {
            downloadPath = SPUtils.getString(SPArgument.APP_DOWNLOAD_PATH, "")!!
            if (isApkDownload(File(downloadPath))) {
                toInstallApp(File(downloadPath))
            }
        } else if (requestCode == 100 && !packageManager.canRequestPackageInstalls()) {
            ToastUtils.show(getString(R.string.author_fail))
        }
    }

    /**
     * 开始安装
     */
    private fun toInstallApp(file: File) {
        SPUtils.putValue(SPArgument.IS_NEED_UPDATE_DIALOG, true)
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            FileProvider.getUriForFile(this, "$packageName.provider", file)
        } else {
            Uri.fromFile(file)
        }
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun changeRedPoint(data: RedPointBean.DataBean) {
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun needNewAllCodeRecords(newAllCodeRecords: NeedNewAllCodeRecords) {
        toGetAllCodeRecords()
    }

    override fun onResume() {
        super.onResume()
        val data = AllCodeRecordsBean.getData()
        if (data.isNullOrEmpty()) {
            fl_mainV5_barrage.visibility = View.GONE
            toGetAllCodeRecords()
        } else {
            toShowAllCodeRecords()
        }
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun haveNewVideo(newVideo: NewVideo) {
//        tab_mainV5.haveNewVideo()
    }
}