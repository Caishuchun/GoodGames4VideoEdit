package com.fortune.zg.activity

import android.content.Intent
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity

class JumpActivity : BaseActivity() {
    companion object {
        private lateinit var instance: JumpActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_jump

    override fun doSomething() {
        instance = this
        startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }

    override fun destroy() {
    }
}