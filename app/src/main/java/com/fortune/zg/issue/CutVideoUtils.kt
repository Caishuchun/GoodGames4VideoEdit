package com.fortune.zg.issue

import com.fortune.zg.utils.LogUtils
import io.microshow.rxffmpeg.RxFFmpegInvoke
import io.microshow.rxffmpeg.RxFFmpegSubscriber

/**
 * 视频处理Utils
 */
object CutVideoUtils {

    private var mRxFFmpegSubscriber: MyRxFFmpegSubscriber? = null

    /**
     * 视频截取
     * @param inputFile 要截取的文件地址
     * @param outputFile 结果文件保存地址
     * @param startTime 视频截取的开始时间
     * @param endTime 视频截取的结束时间
     */
    fun cutOutVideo(
        inputFile: String,
        outputFile: String,
        startTime: Long,
        endTime: Long,
        listener: CompressorListener
    ) {
        if (mRxFFmpegSubscriber != null) {
            mRxFFmpegSubscriber?.dispose()
            mRxFFmpegSubscriber = null
        }
        mRxFFmpegSubscriber = MyRxFFmpegSubscriber(listener)
        val start = TimeFormat.format(startTime, true)
        val interval = endTime - startTime
        val to = if (interval < 1) {
            "00:00:01"
        } else {
            TimeFormat.format(interval, true)
        }
        val command =
            "ffmpeg -ss $start -i $inputFile -vcodec copy -acodec copy -t $to $outputFile"
        val commands = command.split(" ")
        listener.onStart()
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands.toTypedArray())
            .subscribe(mRxFFmpegSubscriber)
    }

    /**
     * TODO 页面销毁的时候记得调用
     */
    fun cancel() {
        if (mRxFFmpegSubscriber != null) {
            mRxFFmpegSubscriber?.dispose()
            mRxFFmpegSubscriber = null
        }
        RxFFmpegInvoke.getInstance().onClean()
        RxFFmpegInvoke.getInstance().onDestroy()
    }

    interface CompressorListener {
        fun onStart()
        fun onProgress(progress: Int, progressTime: Long)
        fun onSuccess()
        fun onError(message: String?)
        fun onCancel()
    }

    class MyRxFFmpegSubscriber(listener: CompressorListener) : RxFFmpegSubscriber() {
        private val mListener = listener
        override fun onError(message: String?) {
            LogUtils.d("${javaClass.simpleName}=>FFmpeg=>onError:message=$message")
            mListener.onError(message)
        }

        override fun onFinish() {
            LogUtils.d("${javaClass.simpleName}=>FFmpeg=>onFinish")
            mListener.onSuccess()
        }

        override fun onProgress(progress: Int, progressTime: Long) {
            LogUtils.d("${javaClass.simpleName}=>FFmpeg=>onProgress:progress=$progress")
            mListener.onProgress(progress, progressTime)
        }

        override fun onCancel() {
            LogUtils.d("${javaClass.simpleName}=>FFmpeg=>onCancel")
            mListener.onCancel()
        }
    }
}