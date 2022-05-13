package com.fortune.zg.issue

import java.text.DecimalFormat

/**
 * 时间格式化
 */
object TimeFormat {

    /**
     * 格式化的方法
     * @param needDivision 需不需要除以1000换算到秒
     */
    fun format(time: Long, needDivision: Boolean = false) = time.let {
        val totalSeconds = if (needDivision) it / 1000 else it
        val hour = totalSeconds / 3600

        val totalMinutes = totalSeconds % 3600
        val minute = totalMinutes / 60
        val second = totalMinutes % 60

        hour.toString().run {
            return@run if (this.length < 2) "0".plus(this) else this
        }.plus(":").plus(
            minute.toString().run {
                return@run if (this.length < 2) "0".plus(this) else this
            }
        ).plus(":").plus(
            second.toString().run {
                return@run if (this.length < 2) "0".plus(this) else this
            }
        )
    }

    /**
     * 格式化
     * 1m 10.4s
     */
    fun formatWithMS(time: Long): String {
        val totalSeconds = time.toDouble() / 1000
        val hour = (totalSeconds / 3600).toInt()

        val totalMinutes = totalSeconds % 3600
        val minute = (totalMinutes / 60).toInt()
        val second = totalMinutes % 60
        val df = DecimalFormat("0.0")
        return if (hour > 0) {
            if (minute > 0) {
                if (second > 0) {
                    "${hour}h ${minute}m ${df.format(second)}s"
                } else {
                    "${minute}m 0.0s"
                }
            } else {
                if (second > 0) {
                    "${df.format(second)}s"
                } else {
                    "0.0s"
                }
            }
        } else {
            if (minute > 0) {
                if (second > 0) {
                    "${minute}m ${df.format(second)}s"
                } else {
                    "${minute}0.0s"
                }
            } else {
                if (second > 0) {
                    "${df.format(second)}s"
                } else {
                    "0.0s"
                }
            }
        }
    }

    /**
     * 1分钟前、5分钟前、10分钟前、15分钟前、半小时前、x小时前、1天前、7天前、半个月前、1个月前、2个月前、3个月前、半年前、1年前。
     */
    fun formatFont(time: Long): String {
        val currentTimeMillis = System.currentTimeMillis()
        val videoTime = time * 1000
        val poor = (currentTimeMillis - videoTime) / 1000
        return when {
            poor <= 60 * 5 -> {
                "1分钟前"
            }
            poor <= 60 * 10 -> {
                "5分钟前"
            }
            poor <= 60 * 15 -> {
                "10分钟前"
            }
            poor <= 60 * 30 -> {
                "15分钟前"
            }
            poor <= 60 * 60 -> {
                "半小时前"
            }
            poor <= 60 * 60 * 24 -> {
                "${poor / 60 / 60}小时前"
            }
            poor <= 60 * 60 * 24 * 7 -> {
                "1天前"
            }
            poor <= 60 * 60 * 24 * 15 -> {
                "7天前"
            }
            poor <= 60 * 60 * 24 * 30 -> {
                "半个月前"
            }
            poor <= 60 * 60 * 24 * 30 * 2 -> {
                "1个月前"
            }
            poor <= 60 * 60 * 24 * 30 * 3 -> {
                "2个月前"
            }
            poor <= 60 * 60 * 24 * 30 * 6 -> {
                "3个月前"
            }
            poor <= 60 * 60 * 24 * 30 * 12 -> {
                "半年前"
            }
            else -> {
                "1年前"
            }
        }
    }
}