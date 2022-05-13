package com.fortune.zg.utils

import android.content.Context
import android.content.SharedPreferences
import com.fortune.zg.myapp.MyApp

/**
 * Author: 蔡小树
 * Time: 2020/4/14 9:53
 * Description: SP 工具类
 */

object SPUtils {

    private const val SPFileName = "hfdd_sp_config"

    private val sp: SharedPreferences by lazy {
        MyApp.getInstance().getSharedPreferences(SPFileName, Context.MODE_PRIVATE)
    }

    /**
     * 存值
     */
    fun putValue(key: String, value: Any?) = with(sp.edit()) {
        when (value) {
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Boolean -> putBoolean(key, value)
            is String, null -> putString(key, value?.toString())
            else -> throw IllegalArgumentException("SPUtils putValue is error")
        }
    }.apply()

    private fun getValue(key: String, default: Any?): Any? = with(sp) {
        return@with when (default) {
            is Int -> getInt(key, default)
            is Long -> getLong(key, default)
            is Float -> getFloat(key, default)
            is Boolean -> getBoolean(key, default)
            is String, null -> getString(key, default?.toString())
            else -> throw java.lang.IllegalArgumentException("SPUtils getValue is error")
        }
    }

    /**
     * 取值Int
     */
    fun getInt(key: String, default: Int = 0) = getValue(key, default) as Int

    /**
     * 取值Long
     */
    fun getLong(key: String, default: Long = 0L) = getValue(key, default) as Long

    /**
     * 取值Float
     */
    fun getFloat(key: String, default: Float = 0f) = getValue(key, default) as Float

    /**
     * 取值Boolean
     */
    fun getBoolean(key: String, default: Boolean = false) = getValue(key, default) as Boolean

    /**
     * 取值String?
     */
    fun getString(key: String, default: String? = null) = getValue(key, default) as String?

    /**
     * 移除当前key
     */
    fun remove(key: String) {
        sp.edit().remove(key).apply()
    }

    /**
     * 清空sp
     */
    fun clear() {
        sp.edit().clear().apply()
    }

}