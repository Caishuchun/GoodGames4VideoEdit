package com.fortune.zg.activity

import android.annotation.SuppressLint
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.fragment.Fav4GameFragment
import com.fortune.zg.fragment.Fav4VideoFragment
import com.fortune.zg.listener.OnBottomBarItemSelectListener
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_fav.*
import java.util.concurrent.TimeUnit

class FavActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: FavActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_fav

    override fun doSomething() {
        instance = this
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_fav_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        switchFragment(0)
        tt_fav.setCurrentItem(0)
        tt_fav.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                switchFragment(index)
            }
        })
    }

    private fun switchFragment(index: Int) {
        val beginTransaction = supportFragmentManager.beginTransaction()
        beginTransaction.replace(
            R.id.fl_fav,
            if (index == 0) Fav4GameFragment.newInstance() else Fav4VideoFragment.newInstance()
        )
            .commit()
    }

    override fun destroy() {
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