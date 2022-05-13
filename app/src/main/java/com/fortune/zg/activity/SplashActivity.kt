package com.fortune.zg.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.VersionBean
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_splash.*
import me.weyye.hipermission.HiPermission
import me.weyye.hipermission.PermissionCallback
import me.weyye.hipermission.PermissionItem
import java.util.concurrent.TimeUnit


class SplashActivity : BaseActivity() {

    private var checkVersionObservable: Disposable? = null
    private var countDownTimeObservable: Disposable? = null
    private var countDownTime = 3
    private var isFirst = true
    private var permissionLists = mutableListOf<String>()

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: SplashActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_splash

    @SuppressLint("CheckResult")
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, false)
        instance = this

        getPermission(0)
//        checkPermissions()
    }

    /**
     * 获取权限
     */
    private fun getPermission(count: Int) {
        val permissions = arrayListOf(
            PermissionItem(
                Manifest.permission.READ_PHONE_STATE,
                "网络状态",
                R.drawable.permission_ic_phone
            ),
            PermissionItem(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "文件管理",
                R.drawable.permission_ic_storage
            )
        )
        HiPermission.create(this)
            .permissions(permissions)
            .msg("为了您正常使用好服多多,需要以下权限:\n1.网络状态:用以更好获取游戏数据\n2.文件管理:用以发布视频缓存数据")
            .filterColor(ResourcesCompat.getColor(resources,R.color.green_2EC8AC,theme))
            .style(R.style.PermissionStyle)
            .checkMutiPermission(object : PermissionCallback {
                override fun onClose() {
                    LogUtils.d("HiPermission=>onClose()")
                    if (count == 0) {
                        getPermission(1)
                    } else {
                        finish()
                    }
                }

                override fun onFinish() {
                    LogUtils.d("HiPermission=>onFinish()")
                    toCountDown()
                }

                override fun onDeny(permission: String?, position: Int) {
                    LogUtils.d("HiPermission=>onDeny(permission:$permission,position:$position)")
                }

                override fun onGuarantee(permission: String?, position: Int) {
                    LogUtils.d("HiPermission=>onGuarantee(permission:$permission,position:$position)")
                }
            })
    }

    /**
     * 检查权限
     */
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLists.add(android.Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLists.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionLists.size == 0) {
            toCountDown()
        } else {
            toGetPermission(permissionLists)
        }
    }

    /**
     * 倒计时5s进入下一个页面
     */
    @SuppressLint("CheckResult")
    fun toCountDown() {
        countDownTimeObservable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (isFirst) {
                    isFirst = false
                    toMain4CheckVersion()
                }
                if (countDownTime > 0) {
                    tv_splash_countDown.text = countDownTime.toString()
                    countDownTime--
                } else {
                    //不管不顾,直接进主界面
                    toMain()
                    checkVersionObservable?.dispose()
                    countDownTimeObservable?.dispose()

                    checkVersionObservable = null
                    countDownTimeObservable = null
                }
            }
    }

    /**
     * 跳转到主界面前,先检查版本更新状态
     */
    @SuppressLint("CheckResult")
    private fun toMain4CheckVersion() {
        val checkVersion = RetrofitUtils.builder().checkVersion()
        checkVersionObservable = checkVersion
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    if (it.getCode() == 1) {
                        VersionBean.setData(it.getData()!!)
                    } else if (it.getCode() == -1) {
                        ToastUtils.show(it.getMsg()!!)
                        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
                        ActivityManager.toSplashActivity(this)
                    }
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
//                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 现在可以跳转到主界面
     */
    @SuppressLint("CheckResult")
    private fun toMain() {
        SPUtils.putValue(SPArgument.IS_LOGIN, true)
        startActivity(Intent(this, MainActivityV5::class.java))
        finish()
    }

    /**
     * 申请权限
     */
    private fun toGetPermission(permissions: List<String>) {
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && permissions.isNotEmpty()) {
                val noGrantedPermissionLists = mutableListOf<String>()
                var isOk = true
                for (index in grantResults.indices) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        noGrantedPermissionLists.add(permissions[index])
                        isOk = false
                    }
                }
                if (isOk) {
                    toCountDown()
                } else {
                    ToastUtils.show("需要权限运行APP!")
                    permissionLists.clear()
                    permissionLists.addAll(noGrantedPermissionLists)
                    toGetPermission(permissionLists)
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun destroy() {
        checkVersionObservable?.dispose()
        countDownTimeObservable?.dispose()

        checkVersionObservable = null
        countDownTimeObservable = null
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