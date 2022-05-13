package com.fortune.zg.issue

/**
 * 格式化视频回调
 */
interface FormatVideoCallback {
    /**
     * 格式化成功
     */
    fun formatSuccess(path: String)

    /**
     * 格式化失败
     */
    fun formatFailed(error: String?)

    /**
     * 格式进度
     */
    fun formatProgress(progress: Int)

    /**
     * 取消格式化
     */
    fun formatCancel()
}