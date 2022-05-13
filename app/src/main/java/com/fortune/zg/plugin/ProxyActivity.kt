package com.fortune.zg.plugin

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import com.fortune.proxylib.ProxyInterface
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.utils.LogUtils

class ProxyActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: ProxyActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val CLASS_NAME = "className"

    }

    private var className: String? = null
    private var realActivity: ProxyInterface? = null

    override fun getLayoutId() = R.layout.activity_porxy

    override fun doSomething() {
        className = intent.getStringExtra(CLASS_NAME)
        LogUtils.d("Plugin++++++>className:${className}")
        try {
            val loadClass = classLoader.loadClass("com.fortune.testunity.MainActivity")
            val constructor = loadClass.getConstructor(*arrayOf<Class<*>>())
            realActivity = constructor.newInstance(*arrayOf<Any>()) as ProxyInterface
            realActivity?.onAttach(this)

            val bundle = Bundle()
            realActivity?.onCreate(bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        realActivity?.onStart()
    }

    override fun onResume() {
        super.onResume()
        realActivity?.onResume()
    }

    override fun onStop() {
        super.onStop()
        realActivity?.onStop()
    }

    override fun onPause() {
        super.onPause()
        realActivity?.onPause()
    }

    override fun destroy() {
        realActivity?.onDestroy()
    }

    override fun getClassLoader(): ClassLoader {
        LogUtils.d("Plugin++++++>getClassLoader:${PluginManager.dexClassLoader}")
        return PluginManager.dexClassLoader!!
    }

    override fun getResources(): Resources {
        LogUtils.d("Plugin++++++>getResources:${PluginManager.resources}")
        return PluginManager.resources!!
    }
}