package com.fortune.zg.activity

import android.annotation.SuppressLint
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.event.LoginChangePage
import com.fortune.zg.fragment.LoginFirstFragment
import com.fortune.zg.fragment.LoginSecondFragment
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.StatusBarUtils
import com.umeng.analytics.MobclickAgent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LoginActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: LoginActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        @SuppressLint("StaticFieldLeak")
        lateinit var loginFirstFragment: LoginFirstFragment

    }

    private var curent = 1

    override fun getLayoutId() = R.layout.activity_login

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, false)
        instance = this
        loginFirstFragment = LoginFirstFragment.newInstance()
        EventBus.getDefault().register(this)

        initView()
    }

    private fun initView() {
        curent = 1
        val supportFragmentManager = supportFragmentManager
        val beginTransaction = supportFragmentManager.beginTransaction()
        beginTransaction.add(R.id.fl_login, loginFirstFragment).commit()
    }

    @Subscribe
    fun changePage(loginChangePage: LoginChangePage) {
        when (loginChangePage.currentPage) {
            1 -> {
                curent = 2
                val loginSecondFragment = LoginSecondFragment.newInstance(
                    loginChangePage.areaCode!!,
                    loginChangePage.phone!!
                )
                val beginTransaction = supportFragmentManager.beginTransaction()
                beginTransaction.replace(R.id.fl_login, loginSecondFragment)
                beginTransaction.commitAllowingStateLoss()
            }
            2 -> {
                curent = 1
                val beginTransaction = supportFragmentManager.beginTransaction()
                beginTransaction.replace(R.id.fl_login, loginFirstFragment)
                beginTransaction.commitAllowingStateLoss()
            }
        }
    }

    override fun onBackPressed() {
        if (curent == 2) {
            curent = 1
            val beginTransaction = supportFragmentManager.beginTransaction()
            beginTransaction.replace(R.id.fl_login, loginFirstFragment)
            beginTransaction.commitAllowingStateLoss()
        } else {
            super.onBackPressed()
        }
    }

    override fun destroy() {
        EventBus.getDefault().unregister(this)
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