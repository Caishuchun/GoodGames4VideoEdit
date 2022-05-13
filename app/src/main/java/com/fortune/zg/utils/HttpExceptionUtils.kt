package com.fortune.zg.utils

import android.content.Context
import com.fortune.zg.R

/**
 * 网络异常提示信息
 */
object HttpExceptionUtils {

    /**
     * 网络异常提示
     */
    fun getExceptionMsg(context: Context, throwable: Throwable?): String {
        if (throwable?.message != null &&
            (throwable.message!!.contains("No address associated with hostname") ||
                    throwable.message!!.contains("No address associated with hostname") ||
                    throwable.message!!.contains("failed to connect") ||
                    throwable.message!!.contains("Failed to connect") ||
                    throwable.message!!.contains("reset"))
        ) {
            return context.getString(R.string.network_fail_to_connect)
        } else if (throwable?.message != null &&
            (throwable.message!!.contains("time out") ||
                    throwable.message!!.contains("timed out") ||
                    throwable.message!!.contains("timeout") ||
                    throwable.message!!.contains("timedout"))
        ) {
            return context.getString(R.string.network_fail_to_connect)
        } else {
            return context.getString(R.string.network_fail_to_request)
        }
    }

}