package com.fortune.zg.utils

import android.widget.Toast
import com.fortune.zg.myapp.MyApp

/**
 * Author: 蔡小树
 * Time: 2020/4/14 16:41
 * Description: Toast 工具类
 */

object ToastUtils {
    private var toast: Toast? = null

    /**
     * 显示短时间Toast
     * @param message 要显示的信息
     */
    fun show(message: String) {
        if (toast != null) {
            toast!!.cancel()
        }
        toast = Toast.makeText(MyApp.getInstance(), message, Toast.LENGTH_SHORT)
        toast!!.show()
    }

    /**
     * 显示长时间Toast
     * @param message 要显示的信息
     */
    fun showLong(message: String) {
        if (toast != null) {
            toast!!.cancel()
        }
        toast = Toast.makeText(MyApp.getInstance(), message, Toast.LENGTH_LONG)
        toast!!.show()
    }

    /**
     * 任意时间Toast
     * @param message 要显示的信息
     * @param duration 时间,毫秒值
     */
    fun show(message: String, duration: Int) {
        if (toast != null) {
            toast!!.cancel()
        }
        toast = Toast.makeText(MyApp.getInstance(), message, duration)
        toast!!.show()
    }
}