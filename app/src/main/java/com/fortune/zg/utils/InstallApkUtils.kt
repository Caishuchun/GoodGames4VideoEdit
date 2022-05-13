package com.fortune.zg.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.util.*

@SuppressLint("QueryPermissionsNeeded")
object InstallApkUtils {
    /**
     * 判断是否安装QQ
     */
    fun isInstallQQ(context: Context): Boolean {
        val qqPackageName = "com.tencent.mobileqq"
        return try {
            val applicationInfo = context.packageManager.getApplicationInfo(
                qqPackageName,
                PackageManager.GET_UNINSTALLED_PACKAGES
            )
            true
        }catch (e:PackageManager.NameNotFoundException){
            false
        }
    }

    /**
     * 判断是否安装微信
     */
    fun isInstallWeChat(context: Context): Boolean {
        val packageManager = context.packageManager
        val installedPackages = packageManager.getInstalledPackages(0)
        for (packageInfo in installedPackages) {
            if ("com.tencent.mm" == packageInfo.packageName.toLowerCase(Locale.ROOT)) {
                return true
            }
        }
        return false
    }


    /**
     * 判断是否安装过该apk
     * @param packageName 包名
     */
    fun isInstallApk(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager
        val installedPackages = packageManager.getInstalledPackages(0)
        for (packageInfo in installedPackages) {
            if (packageName == packageInfo.packageName) {
                return true
            }
        }
        return false
    }

    /**
     * 获取手机已安装应用
     */
    fun installApks(context: Context): MutableList<PackageInfo> {
        val packageManager = context.packageManager
       return packageManager.getInstalledPackages(0)
    }
}