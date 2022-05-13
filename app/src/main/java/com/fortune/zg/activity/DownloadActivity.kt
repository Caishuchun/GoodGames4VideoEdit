package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.view.View
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.fragment.DownloadedFragment
import com.fortune.zg.fragment.DownloadingFragment
import com.fortune.zg.utils.ActivityManager
import com.fortune.zg.utils.StatusBarUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_download.*
import java.util.concurrent.TimeUnit

class DownloadActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: DownloadActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    private var currentFragment = 0
    override fun getLayoutId() = R.layout.activity_download

    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)
        val beginTransaction = supportFragmentManager.beginTransaction()
        beginTransaction
            .add(R.id.fl_download, DownloadingFragment.newInstance())
            .commit()

        initView()
    }

    /**
     * 初始化
     */
    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_download_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        RxView.clicks(ll_download_downloading)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentFragment == 1) {
                    tv_download_downloading.setTextColor(resources.getColor(R.color.black_1A241F))
                    vv_download_downloading.visibility = View.VISIBLE
                    tv_download_downloaded.setTextColor(resources.getColor(R.color.gray_9F9F9F))
                    vv_download_downloaded.visibility = View.GONE
                    currentFragment = 0
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fl_download, DownloadingFragment.newInstance())
                        .commit()
                }
            }

        RxView.clicks(ll_download_downloaded)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentFragment == 0) {
                    tv_download_downloading.setTextColor(resources.getColor(R.color.gray_9F9F9F))
                    vv_download_downloading.visibility = View.GONE
                    tv_download_downloaded.setTextColor(resources.getColor(R.color.black_1A241F))
                    vv_download_downloaded.visibility = View.VISIBLE
                    currentFragment = 1
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fl_download, DownloadedFragment.newInstance())
                        .commit()
                }
            }
    }

    /**
     * 直接跳转到首页
     */
    fun toMainHome() {
        MainActivityV5.getInstance()?.toHomeFragment()
        ActivityManager.toMainActivity()
        finish()
    }

    /**
     * 跳转到下载界面
     */
    fun toDownloadingFragment() {
        tv_download_downloading.setTextColor(resources.getColor(R.color.black_1A241F))
        vv_download_downloading.visibility = View.VISIBLE
        tv_download_downloaded.setTextColor(resources.getColor(R.color.gray_9F9F9F))
        vv_download_downloaded.visibility = View.GONE
        currentFragment = 0
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_download, DownloadingFragment.newInstance())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    override fun destroy() {
    }

}