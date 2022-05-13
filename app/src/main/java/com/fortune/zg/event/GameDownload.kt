package com.fortune.zg.event

import com.arialyy.aria.core.task.DownloadTask

/**
 * 游戏下载的通知
 */
data class GameDownload(
    val task: DownloadTask?, //下载时的进度啥信息
    val state: STATE //当前下载状态
) {
    enum class STATE {
        START, RESUME, PAUSE, CANCEL, FAIL, COMPLETE, RUNNING
    }
}
