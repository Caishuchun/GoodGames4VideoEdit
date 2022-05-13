package com.fortune.zg.http

import okhttp3.RequestBody

/**
 * Retrofit 文件上传进度工具类,获取RequestBody
 */
object RetrofitUploadProgressUtil {
    fun getProgressRequestBody(
        requestBody: RequestBody,
        retrofitProgressUploadListener: RetrofitProgressUploadListener
    ): ProgressRequestBody {
        return ProgressRequestBody(object : RetrofitProgressUploadListener {
            override fun progress(progress: Int) {
                retrofitProgressUploadListener.progress(progress)
            }

            override fun speedAndTimeLeft(speed: String, timeLeft: String) {
                retrofitProgressUploadListener.speedAndTimeLeft(speed, timeLeft)
            }

        }, requestBody)
    }
}