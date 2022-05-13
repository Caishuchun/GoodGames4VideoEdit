package com.fortune.zg.utils

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.text.TextUtils
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.NetworkInterface
import java.util.*

/**
 * Author: 蔡小树
 * Time: 2019/12/29 10:09
 * Description:
 */

object MacUtils {
    private const val TAG = "GetMac"

    fun getAdresseMAC(context: Context?): String? {
        var strMac: String? = "02:00:00:00:00:00"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            strMac = getLocalMacAddressFromWifiInfo(context)
            Log.d(TAG, "6.0以下 MAC = $strMac")
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            strMac = getMacfromMarshmallow()
            Log.d(TAG, "6.0以上7.0以下:MAC = $strMac")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            strMac = getMacFromHardware()
            Log.d("MAC", "7.0以上:MAC = $strMac")
        }
        return strMac
    }

    /**
     * 6.0以下 ,根据wifi信息获取本地mac
     *
     * @param context
     * @return
     */
    fun getLocalMacAddressFromWifiInfo(context: Context?): String? {
        var mac = "02:00:00:00:00:00"
        if (context == null) {
            return mac
        }
        val wifi = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        var info: WifiInfo? = null
        try {
            info = wifi.connectionInfo
        } catch (e: Exception) {
        }
        if (info == null) {
            return null
        }
        mac = info.macAddress
        if (!TextUtils.isEmpty(mac)) {
            mac = mac.toUpperCase(Locale.ENGLISH)
        }
        return mac
    }

    /**
     * android 6.0及以上、7.0以下 获取mac地址
     * 如果是6.0以下，直接通过wifimanager获取
     *
     * @return
     */
    fun getMacfromMarshmallow(): String? {
        var WifiAddress: String? = "02:00:00:00:00:00"
        try {
            WifiAddress =
                BufferedReader(FileReader(File("/sys/class/net/wlan0/address")))
                    .readLine()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return WifiAddress
    }


    /**
     * 7.0 以上 遍历循环所有的网络接口，找到接口是 wlan0
     * 必须的权限 <uses-permission android:name="android.permission.INTERNET"></uses-permission>
     *
     * @return
     */
    private fun getMacFromHardware(): String? {
        try {
            val all: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.name.equals("wlan0", ignoreCase = true)) continue
                val macBytes = nif.hardwareAddress ?: return ""
                val res1 = StringBuilder()
                for (b in macBytes) {
                    res1.append(String.format("%02X:", b))
                }
                if (res1.isNotEmpty()) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "02:00:00:00:00:00"
    }
}