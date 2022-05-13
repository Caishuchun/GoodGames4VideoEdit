package com.fortune.zg.http

/**
 * Retrofit 上传文件进度监听
 */
interface RetrofitProgressUploadListener {
    /**
     * 进度回调
     * @param progress 进度
     */
    fun progress(progress: Int)

    /**
     * 上传速率和剩余时间
     * @param speed 速度
     * @param timeLeft 剩余时间
     */
    fun speedAndTimeLeft(speed: String, timeLeft: String)

}