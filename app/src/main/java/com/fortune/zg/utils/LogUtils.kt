package com.fortune.zg.utils

import com.fortune.zg.base.BaseAppUpdateSetting
import com.orhanobut.logger.Logger

/**
 * Author: 蔡小树
 * Time: 2020/4/14 9:48
 * Description: log工具类
 */

object LogUtils {
    const val isDebug = BaseAppUpdateSetting.isDebug

    /**
     * debug 日志
     */
    fun d(log: Any) {
        if (isDebug)
            Logger.d(log)
    }

    /**
     * error 日志
     */
    fun e(log: Any) {
        if (isDebug)
            Logger.e(log.toString())
    }

    /**
     * info 日志
     */
    fun i(log: Any) {
        if (isDebug)
            Logger.i(log.toString())
    }

}