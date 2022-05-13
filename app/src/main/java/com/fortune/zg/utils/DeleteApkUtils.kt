package com.fortune.zg.utils

import java.io.File

/**
 * 删除软件包操作
 */
object DeleteApkUtils {
    fun deleteApk(file: File) {
        if (file.exists() && file.isFile) {
            try {
                file.delete()
            } catch (e: Exception) {
            } finally {
                LogUtils.d("${file.name} is delete...")
            }
        } else {
            return
        }
    }
}