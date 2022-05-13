package com.fortune.zg.service

import android.app.IntentService
import android.content.Intent
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.SPUtils
import java.io.File

class DeleteFileIntentService : IntentService("DeleteFileIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        SPUtils.putValue(SPArgument.IS_DELETE_FILE_OVER, false)
        val folder = getExternalFilesDir("pic2video").toString()
        val file = File(folder)
        if (file.exists() && file.isDirectory) {
            val listFiles = file.listFiles()
            Thread{
                for (deleteFile in listFiles) {
                    deleteFile.delete()
                }
                SPUtils.putValue(SPArgument.IS_DELETE_FILE_OVER, true)
                LogUtils.d("===================deleteFileOver")
            }.start()
        }else{
            SPUtils.putValue(SPArgument.IS_DELETE_FILE_OVER, true)
        }
    }
}