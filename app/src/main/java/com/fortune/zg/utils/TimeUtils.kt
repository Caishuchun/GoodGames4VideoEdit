package com.fortune.zg.utils

/**
 * 时间格式化工具
 */
object TimeUtils {

    /**
     * 格式化时间到HH:mm:ss
     */
    fun formatHms(time: Int): String {
        if (time < 10) {
            return "00:00:0$time"
        }
        if (time < 60) {
            return "00:00:$time"
        }
        if (time < 3600) {
            val minute = time / 60
            val second = time - minute * 60
            if (minute < 10) {
                if (second < 10) {
                    return "00:0$minute:0$second"
                }
                return "00:0$minute:$second"
            } else {
                if (second < 10) {
                    return "00:$minute:0$second"
                }
                return "00:$minute:$second"
            }
        }
        val hour = time / 3600
        val minute = (time - hour * 3600) / 60
        val second = time - hour * 3600 - minute * 60
        if (hour < 10) {
            if (minute < 10) {
                if (second < 10) {
                    return "0$hour:0$minute:0$second"
                }
                return "0$hour:0$minute:$second"
            } else {
                if (second < 10) {
                    return "0$hour:$minute:0$second"
                }
                return "0$hour:$minute:$second"
            }
        } else {
            if (minute < 10) {
                if (second < 10) {
                    return "$hour:0$minute:0$second"
                }
                return "$hour:0$minute:$second"
            } else {
                if (second < 10) {
                    return "$hour:$minute:0$second"
                }
                return "$hour:$minute:$second"
            }
        }
    }

    /**
     * 格式化时间到mm:ss
     */
    fun formatMs(time: Int): String {
        if (time < 10) {
            return "00:0$time"
        }
        if (time < 60) {
            return "00:$time"
        }
        val minute = time / 60
        val second = time - minute * 60
        if (minute < 10) {
            if (second < 10) {
                return "0$minute:0$second"
            }
            return "0$minute:$second"
        } else {
            if (second < 10) {
                return "$minute:0$second"
            }
            return "$minute:$second"
        }
    }
}