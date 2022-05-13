package com.fortune.zg.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import com.fortune.zg.utils.LogUtils
import dalvik.system.DexClassLoader

/**
 * 插件化管理
 */
@SuppressLint("StaticFieldLeak")
object PluginManager {
    var dexClassLoader: DexClassLoader? = null
    var resources: Resources? = null
    private var context: Context? = null
    var enterActivityName: String? = null //apk 入口

    fun setContext(context: Context) {
        this.context = context
    }

    fun getContext() = this.context

    /**
     * 加载
     */
    fun loadPath(path: String) {
        val dexOutFile = context?.getDir("dex", Context.MODE_PRIVATE)
        dexClassLoader = DexClassLoader(path, dexOutFile?.absolutePath, null, context?.classLoader)
        LogUtils.d("Plugin++++++>dexClassLoader:$dexClassLoader")

        val packageManager = context?.packageManager
        val packageInfo =
            packageManager?.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
        enterActivityName = packageInfo?.activities?.get(0)?.name
        LogUtils.d("Plugin++++++>enterActivityName:$enterActivityName")

        resources = getPluginResources(path)
        LogUtils.d("Plugin++++++>over")
    }

    /**
     * 获取资源文件
     */
    private fun getPluginResources(path: String): Resources? {
        //搞定新包资源加载器
        try {
            val assetManager = AssetManager::class.java.newInstance()
            val method = assetManager::class.java.getMethod("addAssetPath", String::class.java)
            LogUtils.d("Plugin++++++>method:$method")
            method.invoke(assetManager, path)
            val superRes = this.resources
            val mResources = Resources(
                assetManager,
                superRes?.displayMetrics,
                superRes?.configuration
            )
            LogUtils.d("Plugin++++++>resources:$mResources")
            return mResources
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}