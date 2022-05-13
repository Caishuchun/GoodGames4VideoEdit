package com.fortune.zg.http

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * 自定义实现上传进度时使用的RequestBody
 */
class ProgressRequestBody(
    retrofitProgressUploadListener: RetrofitProgressUploadListener,
    requestBody: RequestBody
) : RequestBody() {

    private val mListener = retrofitProgressUploadListener
    private val mRequestBody = requestBody
    private var mBufferedSink: BufferedSink? = null

    override fun contentType(): MediaType? {
        return mRequestBody.contentType()
    }

    override fun contentLength(): Long {
        return mRequestBody.contentLength()
    }

    override fun writeTo(sink: BufferedSink) {
        if (sink is Buffer) {
            return
        }
        if (null == mBufferedSink) {
            mBufferedSink = Okio.buffer(sink(sink))
        }
        mRequestBody.writeTo(mBufferedSink!!)
        mBufferedSink?.flush()
    }

    private fun sink(sink: Sink): Sink {
        return object : ForwardingSink(sink) {
            var bytesWriting = 0L
            var contentLength = 0L
            var startTime = System.currentTimeMillis()
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                if (0L == contentLength) {
                    contentLength = contentLength()
                }
                bytesWriting += byteCount
                mListener.progress((bytesWriting.toDouble() / contentLength * 100).toInt())

                val currentTime = System.currentTimeMillis()
                val useTime = currentTime - startTime
                //每秒的速度
                val speed = bytesWriting.toDouble() / (useTime / 1000)
                val formatSpeed = formatSpeed(speed)

                //剩余大小
                val lastLength = contentLength - bytesWriting
                val timeLeft = lastLength.toDouble() / speed
                //剩余时间
                val formatTimeLeft = formatTimeLeft(timeLeft.toInt())
                mListener.speedAndTimeLeft(formatSpeed, formatTimeLeft)
            }
        }
    }

    /**
     * 格式化剩余时间
     */
    private fun formatTimeLeft(timeLeft: Int): String {
        var hour = 0
        var minute = 0
        var second = 0
        if (timeLeft <= 0) {
            return "00:00"
        } else {
            minute = timeLeft / 60
            if (minute < 60) {
                second = timeLeft % 60
                return "${unitFormat(minute)}:${unitFormat(second)}"
            } else {
                hour = minute / 60
                if (hour > 99)
                    return "99:59:59"
                minute %= 60
                second = timeLeft - hour * 3600 - minute * 60
                return "${unitFormat(hour)}:${unitFormat(minute)}:${unitFormat(second)}"
            }
        }
    }

    /**
     * 时间格式化,补0
     */
    private fun unitFormat(time: Int) =
        if (time < 10) {
            "0$time"
        } else {
            time.toString()
        }


    /**
     * 格式化速度
     */
    private fun formatSpeed(speed: Double): String {
        val decimalFormat = DecimalFormat("0.##")
        decimalFormat.roundingMode = RoundingMode.FLOOR
        return when {
            speed < 1024 -> {
                "${decimalFormat.format(speed)}byte/s,"
            }
            speed < 1024 * 1024 -> {
                "${decimalFormat.format(speed / 1024)}Kb/s,"
            }
            speed < 1024 * 1024 * 1024 -> {
                "${decimalFormat.format(speed / 1024 / 1024)}Mb/s,"
            }
            speed < 1024 * 1024 * 1024 * 1024 -> {
                "${decimalFormat.format(speed / 1024 / 1024 / 1024)}Gb/s,"
            }
            else -> ""
        }
    }
}