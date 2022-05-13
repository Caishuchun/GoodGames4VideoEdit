package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.UserInfoBean
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.StatusBarUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_account_safe.*
import java.util.concurrent.TimeUnit

class AccountSafeActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: AccountSafeActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_account_safe

    @SuppressLint("CheckResult")
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        if (UserInfoBean.getData() != null) {
            val data = UserInfoBean.getData()
            if (!data?.user_avatar.isNullOrEmpty() && !data?.user_avatar!!.endsWith("avatar/default.jpg")) {
                Glide.with(this)
                    .load(data.user_avatar)
                    .placeholder(R.mipmap.bg_gray_6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(civ_safe_head)
            }
            tv_safe_name.text = data?.user_name
            tv_safe_phone.text = data?.user_phone
        } else {
            finish()
        }

        RxView.clicks(iv_safe_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }
        RxView.clicks(ll_safe_changePhone)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                startActivity(Intent(this, ChangePhone1Activity::class.java))
            }
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