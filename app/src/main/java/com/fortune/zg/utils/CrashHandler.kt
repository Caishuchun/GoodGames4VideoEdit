package com.fortune.zg.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import com.umeng.umcrash.UMCrash
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 奔溃日志,仅保存最近最新一次奔溃
 */

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    private var mContext: Context? = null
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * 初始化
     *
     * @param context
     */
    fun init(context: Context?) {
        mContext = context
        //获取系统默认的UncaughtException
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        //将自己的Crash放进去
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread?, ex: Throwable) {
        LogUtils.d("捕捉到了异常")
        // 1. 获取信息
// 1.1 崩溃信息
// 1.2 手机信息
// 1.3 版本信息
// 2.写入文件
        val crashFileName = saveInfoToSD(ex)
        // 3. 缓存崩溃日志文件
        cacheCrashFile(crashFileName)
        // 系统默认处理
        mDefaultHandler!!.uncaughtException(t, ex)
    }

    /**
     * 缓存崩溃日志文件
     *
     * @param fileName
     */
    private fun cacheCrashFile(fileName: String?) {
        val sp =
            mContext!!.getSharedPreferences("crash", Context.MODE_PRIVATE)
        sp.edit().putString("CRASH_FILE_NAME", fileName).commit()
    }

    /**
     * 获取崩溃文件名称
     *
     * @return
     */
    fun getCrashFile(): File {
        val crashFileName = mContext!!.getSharedPreferences(
            "crash",
            Context.MODE_PRIVATE
        ).getString("CRASH_FILE_NAME", "")
        return File(crashFileName)
    }

    /**
     * 保存获取的 软件信息，设备信息和出错信息保存在SDcard中
     *
     * @param ex
     * @return
     */
    private fun saveInfoToSD(ex: Throwable): String? {
        val fileName: String? = null
        val sb = StringBuffer()
        for ((key, value) in obtainSimpleInfo(
            mContext
        )) {
            sb.append(key).append(" = ").append(value).append("\n")
        }
        sb.append(obtainExceptionInfo(ex))
        if (Environment.getExternalStorageState() ==
            Environment.MEDIA_MOUNTED
        ) {
            try { //这里我并不需要存很多错误日志，只能存一个
                val path = mContext!!.getExternalFilesDir("log")?.path
                val dir = File(path)
                if (!dir.exists()) {
                    dir.mkdirs()
                } else {
                    val files = dir.listFiles()
                    for (file in files) {
                        file.delete()
                    }
                }
                val fos = FileOutputStream(
                    path + "/error" + getAssignTime("yyyyMMdd-HH:mm") + ".log",
                    true
                )
                fos.write(sb.toString().toByteArray())
                fos.flush()
                fos.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }finally {
                UMCrash.generateCustomLog(sb.toString(),"UMException")
            }
        }
        return fileName
    }

    /**
     * 返回当前日期根据格式
     */
    private fun getAssignTime(dateFormatStr: String): String {
        val dataFormat: DateFormat = SimpleDateFormat(dateFormatStr)
        val currentTime = System.currentTimeMillis()
        return dataFormat.format(currentTime)
    }

    /**
     * 获取一些简单的信息,软件版本，手机版本，型号等信息存放在HashMap中
     *
     * @return
     */
    private fun obtainSimpleInfo(context: Context?): HashMap<String, String> {
        val map =
            HashMap<String, String>()
        val mPackageManager = context!!.packageManager
        var mPackageInfo: PackageInfo? = null
        try {
            mPackageInfo = mPackageManager.getPackageInfo(
                context.packageName, PackageManager.GET_ACTIVITIES
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        map["versionName"] = mPackageInfo!!.versionName
        map["versionCode"] = "" + mPackageInfo.versionCode
        map["MODEL"] = "" + Build.MODEL
        map["SDK_INT"] = "" + Build.VERSION.SDK_INT
        map["PRODUCT"] = "" + Build.PRODUCT
        map["MOBLE_INFO"] = mobileInfo
        return map
    }

    /**
     * 获取系统未捕捉的错误信息
     *
     * @param throwable
     * @return
     */
    private fun obtainExceptionInfo(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }

    companion object {
        private var crashUtils: CrashHandler? = null
        private const val TAG = "CrashHandler"
        val instance: CrashHandler?
            get() {
                if (crashUtils == null) {
                    crashUtils = CrashHandler()
                }
                return crashUtils
            }

        /**
         * Cell phone information
         *
         * @return
         */
        val mobileInfo: String
            get() {
                val sb = StringBuffer()
                try {
                    val fields =
                        Build::class.java.declaredFields
                    for (field in fields) {
                        field.isAccessible = true
                        val name = field.name
                        val value = field[null].toString()
                        sb.append("$name=$value")
                        sb.append("\n")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return sb.toString()
            }
    }
}