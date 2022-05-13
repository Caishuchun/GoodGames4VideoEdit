@file:Suppress("DEPRECATION")

package com.fortune.zg.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Build.*
import android.os.Build.VERSION.RELEASE
import android.provider.Settings
import android.telephony.TelephonyManager
import com.fortune.zg.R
import com.fortune.zg.myapp.MyApp
import com.google.gson.Gson
import com.umeng.commonsdk.statistics.common.DeviceConfig
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil


/**
 * Author: 蔡小树
 * Time: 2019/12/26 11:20
 * Description:
 */

object PhoneInfoUtils {
    fun getTestDeviceInfo(context: Context?): Array<String?>? {
        val deviceInfo = arrayOfNulls<String>(2)
        try {
            if (context != null) {
                deviceInfo[0] = DeviceConfig.getDeviceIdForGeneral(context)
                deviceInfo[1] = DeviceConfig.getMac(context)
            }
        } catch (e: java.lang.Exception) {
        }
        return deviceInfo
    }

    /**
     * 通过反射获取状态栏高度
     *
     * @param context
     * @return
     */
    fun getStatusBarByReflex(context: Context) =
        ceil((25 * context.resources.displayMetrics.density).toDouble()).toInt()

    /**
     * 获取屏幕高度
     */
    fun getHeight(context: Activity): Int {
        val windowManager = context.windowManager
        return windowManager.defaultDisplay.height
    }

    /**
     * 获取屏幕宽度
     */
    fun getWidth(context: Activity): Int {
        val windowManager = context.windowManager
        return windowManager.defaultDisplay.width
    }

    /**
     * 获取手机信息
     */
    @SuppressLint("ServiceCast", "MissingPermission", "HardwareIds", "SimpleDateFormat")
    fun getPhoneInfo(): String {
        val telephonyManager =
            MyApp.getInstance().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var mobileID: String
        var temNo: String
        try {
            mobileID = telephonyManager.deviceId//imei
            temNo = telephonyManager.subscriberId//imsi
        } catch (e: Exception) {
            mobileID = getMobileID()
            temNo = "hfdd$mobileID"
        }
        val appVersion = MyApp.getInstance().getVersion()
        val mobileType = BOARD + TYPE
        val sysVersion = RELEASE
        val longitude = LocalUtils.getLongitude()
        val latitude = LocalUtils.getLatitude()
        var timeZone = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
        timeZone = timeZone.substring(0, timeZone.indexOf(":"))
        val appTime = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val appName = MyApp.getInstance().resources.getString(R.string.app_name)

        val phoneInfo = mutableMapOf<String, String>()
        phoneInfo["sysType"] = "android"
        phoneInfo["mobileID"] = mobileID
        phoneInfo["temNo"] = temNo
        phoneInfo["appVersion"] = appVersion
        phoneInfo["mobileType"] = mobileType
        phoneInfo["sysVersion"] = sysVersion
        phoneInfo["longitude"] = longitude!!
        phoneInfo["latitude"] = latitude!!
        phoneInfo["timeZone"] = timeZone
        phoneInfo["appTime"] = appTime
        phoneInfo["appName"] = appName
        return Gson().toJson(phoneInfo)
    }


    private fun getMobileID(): String {
        var serial = ""
        val m_szDevIDShort =
            "35" + BOARD.length % 10 + BRAND.length % 10 + CPU_ABI.length % 10
        +DEVICE.length % 10 + DISPLAY.length % 10 + HOST.length % 10 + ID.length % 10
        +MANUFACTURER.length % 10 + MODEL.length % 10 + PRODUCT.length % 10 + TAGS.length % 10
        +TYPE.length % 10 + USER.length % 10 //13 位
        try {
            serial = Build::class.java.getField("SERIAL")[null].toString()
            //API>=9 使用serial号
            return UUID(m_szDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
        } catch (exception: java.lang.Exception) {
            //serial需要一个初始化
            serial = "serial" // 随便一个初始化
        }
        //使用硬件信息拼凑出来的15位号码
        return UUID(m_szDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
    }


    @SuppressLint("MissingPermission", "HardwareIds", "SimpleDateFormat")
    fun getPhoneInfoNew(): String {
        val telephonyManager =
            MyApp.getInstance().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var imei = ""
        var imsi = "unKnown"
        if (VERSION.SDK_INT < 29) {
            imei = telephonyManager.deviceId//imei
            imsi = telephonyManager.subscriberId//imsi
        } else {
            imei = Settings.System.getString(
                MyApp.getInstance().contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }
        val appVersion = MyApp.getInstance().getVersion()
        val model = MODEL
        val brand = BRAND
        val sysVersion = RELEASE
        val longitude = LocalUtils.getLongitude()
        val latitude = LocalUtils.getLatitude()
        val mac = MacUtils.getAdresseMAC(MyApp.getInstance())
        val appName = MyApp.getInstance().resources.getString(R.string.app_name)
        return "$appVersion==$imei==$sysVersion==$model-$brand==$imsi==$longitude==$latitude==$mac==$appName"
    }
}