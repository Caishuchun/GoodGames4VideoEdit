package com.fortune.zg.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.fortune.zg.myapp.MyApp

/**
 * Author: 蔡小树
 * Time: 2019/12/27 17:57
 * Description:
 */

@SuppressLint("StaticFieldLeak")
object LocalUtils {
    private var mContext: Context? = null
    private var mInstance: LocalUtils? = null
    private var mLocation: Location? = null
    private var mLocationProvider: String? = null
    private var mLocationManager: LocationManager? = null


    init {
        mContext = MyApp.getInstance()
        getLocal()
    }

    /**
     * 获取经纬度
     */
    @SuppressLint("MissingPermission")
    private fun getLocal() {
        //1.获取位置管理器
        mLocationManager =
            mContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //2.获取位置管理器，GPS或者网络
        val providers = mLocationManager!!.getProviders(true)
        mLocationProvider = when {
            providers.contains(LocationManager.NETWORK_PROVIDER) -> {
                LogUtils.d("网络定位")
                LocationManager.NETWORK_PROVIDER
            }
            providers.contains(LocationManager.GPS_PROVIDER) -> {
                LogUtils.d("GPS定位")
                LocationManager.GPS_PROVIDER
            }
            else -> {
                LogUtils.d("无法进行定位")
                return
            }
        }
        //3.检查权限
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(
                mContext!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                mContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        //4.获取上次定位的location
        val location =
            mLocationManager!!.getLastKnownLocation(mLocationProvider!!)
        location?.let { setLocation(it) }
        //监听位置变化，参数2：每隔多久，毫秒；参数3：距离，米
//        Looper.prepare()
//        mLocationManager!!.requestLocationUpdates(mLocationProvider!!, 200, 0f, mLocationListener!!)
    }

    /**
     * 设置地理位置
     *
     * @param location
     */
    private fun setLocation(location: Location) {
        mLocation = location
        val address =
            "纬度：" + location.latitude + "，经度：" + location.longitude
        LogUtils.d("location--->$address")
    }

    /**
     * 获取纬度
     *
     * @return
     */
    fun getLatitude(): String? {
        return if (mLocation != null) {
            mLocation!!.latitude.toString() + ""
        } else {
            ""
        }
    }

    /**
     * 获取经度
     *
     * @return
     */
    fun getLongitude(): String? {
        return if (mLocation != null) {
            mLocation!!.longitude.toString() + ""
        } else {
            ""
        }
    }

    /**
     * 移除对于定位的监听
     */
    @SuppressLint("MissingPermission")
    fun removeLocationUpdatesListener() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(
                mContext!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                mContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (mLocationListener != null && mLocationManager != null) {
            mInstance = null
            mLocationManager!!.removeUpdates(mLocationListener)
        }
    }

    /**
     * 定位的监听器
     */
    private val mLocationListener: LocationListener? = object : LocationListener {
        //位置发生变化，重新设置
        override fun onLocationChanged(location: Location) {
            if (location != null) {
                setLocation(location)
                removeLocationUpdatesListener()
            }
        }

        override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
        override fun onProviderEnabled(s: String) {}
        override fun onProviderDisabled(s: String) {}
    }
}