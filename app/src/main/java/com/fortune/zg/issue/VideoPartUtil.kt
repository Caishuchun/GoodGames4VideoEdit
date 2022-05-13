package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.utils.DialogUtils
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.ToastUtils
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import io.microshow.rxffmpeg.RxFFmpegInvoke
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import kotlinx.android.synthetic.main.item_video_cut_pic.view.*
import kotlinx.android.synthetic.main.item_video_part.view.*
import kotlinx.android.synthetic.main.layout_video_part.*
import java.io.File
import java.util.concurrent.TimeUnit

object VideoPartUtil {
    private var mDialog: Dialog? = null
    private var mAdapter: BaseAdapterWithPosition<MediaData>? = null
    private var isShowCut = false

    private var mAdapter4Pic: BaseAdapterWithPosition<Bitmap>? = null
    private var picBitmapLists = mutableListOf<Bitmap>()

    private const val MAX_PIC = 10
    private var picIndex = 0
    private var mediaDataList = mutableListOf<MediaData>()
    private var mData = mutableListOf<MediaData>()
    private var cutMediaData: MediaData? = null
    private var cutStart = 0L
    private var cutEnd = 0L
    private var isGetPic = false //是否正在获取视频帧
    private var isCutVideo = false //是否剪辑过视频
    private var currentIndex = 0

    /**
     * 取消选择弹框
     */
    fun dismiss() {
        try {
            if (mDialog != null && mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
        } catch (e: Exception) {

        } finally {
            mDialog = null
        }
    }

    /**
     * 视频片段剪辑回调监听
     */
    interface ShowPartCallBack {
        /**
         * 取消
         */
        fun cancel()

        /**
         * 当前选中的文件
         */
        fun selectVideo(mediaData: MediaData)

        /**
         * 当前滑动位置
         */
        fun currentPosition(position: Long)

        /**
         * 当前处理的视频
         */
        fun cutVideo(
            startLong: Long,
            startTime: String,
            endLong: Long,
            endTime: String
        )

        /**
         *剪切成功
         */
        fun allVideoCutSuccess(mediaInfo: MutableList<MediaData>, videoFilePath: String)
    }

    /**
     * 显示视频片段处理
     */
    @SuppressLint("CheckResult", "SetTextI18n")
    fun showPart(
        context: AppCompatActivity,
        mediaInfo: MutableList<MediaData>,
        callback: ShowPartCallBack
    ) {
        picIndex = 0
        isShowCut = false
        isCutVideo = false
        picBitmapLists.clear()
        mediaDataList.clear()
        mediaDataList.addAll(mediaInfo)
        mData.clear()
        mData.addAll(mediaInfo)

        mDialog = Dialog(context, R.style.BeautifulDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_video_part, null) as LinearLayout
        mDialog?.setContentView(root)

        mDialog?.tv_videoPart_totalTime?.text = "总时长 ${getTotalDuration(mData)}"
        mDialog?.tv_videoPart_cutTime?.text = "已裁剪 ${getCutDuration(mediaDataList, mData)}"

        mDialog?.cutVideoView_cut?.setType(CutVideoView.CutVideoType.CUT_VIDEO)

        initRecycleView(context, callback)
        initRecycleView4Pic(context)

        RxView.clicks(mDialog?.iv_videoPart_back!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (!isGetPic) {
                    if (isShowCut) {
                        mDialog?.rv_videoPart?.visibility = View.VISIBLE
                        mDialog?.rl_videoPart_cut?.visibility = View.GONE

                        mDialog?.tv_videoPart_title?.text = "片段"
                        mDialog?.tv_videoPart_totalTime?.text = "总时长 ${getTotalDuration(mData)}"
                        mDialog?.tv_videoPart_cutTime?.text =
                            "已裁剪 ${getCutDuration(mediaDataList, mData)}"
                        isShowCut = false
                    } else {
                        dismiss()
                    }
                }
            }

        RxView.clicks(mDialog?.iv_videoPart_right!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (!isGetPic) {
                    if (isShowCut) {
                        toCutVideo(context, callback)
                    } else {
                        if (mData.size > 1) {
                            if (isCutVideo) {
                                DialogUtils.showDialogWithProgress(context, "合并视频 0%")
                                Thread {
                                    toFormat2Ts(context, callback)
                                }.start()
                            } else {
                                dismiss()
                            }
                        } else {
                            callback.allVideoCutSuccess(mData, mData[0].filePath!!)
                            dismiss()
                        }
                    }
                }
            }

        mDialog?.setOnDismissListener {
            callback.cancel()
        }

        //确定大小位置
        val dialogWindow = mDialog?.window!!
        dialogWindow.setGravity(Gravity.BOTTOM)
        dialogWindow.setWindowAnimations(R.style.dialog_anim)
        val attributes = dialogWindow.attributes!!
        attributes.x = 0
        attributes.y = 0
        attributes.width = context.resources.displayMetrics.widthPixels
        dialogWindow.attributes = attributes
        mDialog?.show()
    }

    /**
     * 视频转成ts文件进行合并
     */
    private fun toFormat2Ts(context: AppCompatActivity, callback: ShowPartCallBack) {
        val videoLists = mutableListOf<String>()
        for (video in mData) {
            videoLists.add(video.filePath!!)
        }
        todoFormat2Ts(videoLists, context, callback)
    }

    /**
     * 循环格式化为ts文件,方便合并
     */
    private fun todoFormat2Ts(
        videoLists: MutableList<String>,
        context: AppCompatActivity,
        callback: ShowPartCallBack
    ) {
        val ts =
            context.getExternalFilesDir("pic2video")
                .toString() + "/cut_ts_${System.currentTimeMillis()}.ts"
        val commands =
            "ffmpeg -y -i ${videoLists[currentIndex]} -c copy -bsf:v h264_mp4toannexb -f mpegts $ts"
                .split(" ")
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands.toTypedArray())
            .subscribe(object : RxFFmpegSubscriber() {
                override fun onError(message: String?) {
                    LogUtils.d("todoFormat2Ts=>error:$message")
                }

                override fun onFinish() {
                    LogUtils.d("todoFormat2Ts=>finish")
                    if (currentIndex == videoLists.size - 1) {
                        currentIndex = 0
                        toMergeVideo(context, callback)
                    } else {
                        currentIndex++
                        todoFormat2Ts(videoLists, context, callback)
                    }
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    LogUtils.d("todoFormat2Ts=>progress=>progress:$progress,progressTime:$progressTime")
                }

                override fun onCancel() {
                    LogUtils.d("todoFormat2Ts=>cancel")
                }
            })
    }

    /**
     * 合并视频
     */
    private fun toMergeVideo(context: AppCompatActivity, callback: ShowPartCallBack) {
        val folder = context.getExternalFilesDir("pic2video").toString()
        val folderFile = File(folder)
        var videoLists = ""
        var count = 0
        if (folderFile.exists() && folderFile.isDirectory && folderFile.listFiles().isNotEmpty()) {
            for (index in folderFile.listFiles().indices) {
                val path = folderFile.listFiles()[index].path
                val fileName = path.substring(path.lastIndexOf("/") + 1, path.length)
                if (fileName.startsWith("cut_ts_") && fileName.endsWith(".ts")) {
                    count++
                    videoLists += path
                    videoLists += "|"
                }
            }
        }
        if (count > mData.size) {
            val difference = count - mData.size
            for (index in 0 until difference) {
                videoLists = videoLists.substring(videoLists.indexOf("|") + 1, videoLists.length)
            }
        }
        videoLists = videoLists.substring(0, videoLists.length - 1)
        LogUtils.d("videoLists=>$videoLists")
        val out = context.getExternalFilesDir("pic2video")
            .toString() + "/cutOutOver_${System.currentTimeMillis()}.mp4"
        val commands = "ffmpeg -i concat:$videoLists -c copy -bsf:a aac_adtstoasc $out"
            .split(" ")
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands.toTypedArray())
            .subscribe(object : RxFFmpegSubscriber() {
                override fun onError(message: String?) {
                    LogUtils.d("toMergeVideo=>error:$message")
                    context.runOnUiThread {
                        DialogUtils.dismissLoading()
                    }
                }

                override fun onFinish() {
                    LogUtils.d("toMergeVideo=>finish")
                    context.runOnUiThread {
                        ToastUtils.show("合并裁剪视频成功!")
                        DialogUtils.dismissLoading()
                        callback.allVideoCutSuccess(mData, out)
                        mDialog?.dismiss()
                    }
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    LogUtils.d("toMergeVideo=>progress=>progress:$progress,progressTime:$progressTime")
                    context.runOnUiThread {
                        if (progress > 0) {
                            DialogUtils.setDialogMsg("合并视频 ${progress}%")
                        }
                    }
                }

                override fun onCancel() {
                    LogUtils.d("toMergeVideo=>cancel")
                    context.runOnUiThread {
                        DialogUtils.dismissLoading()
                    }
                }
            })
    }

    /**
     * 裁剪视频
     */
    @SuppressLint("SetTextI18n")
    private fun toCutVideo(context: AppCompatActivity, callback: ShowPartCallBack) {
        if (cutEnd - cutStart < 1000) {
            ToastUtils.show("视频裁剪至少需要保留1s时长")
            return
        } else if (cutStart == 0L && cutEnd == cutMediaData?.duration!!) {
            mDialog?.rv_videoPart?.visibility = View.VISIBLE
            mDialog?.rl_videoPart_cut?.visibility = View.GONE

            mDialog?.tv_videoPart_title?.text = "片段"
            mDialog?.tv_videoPart_totalTime?.text = "总时长 ${getTotalDuration(mData)}"
            mDialog?.tv_videoPart_cutTime?.text =
                "已裁剪 ${getCutDuration(mediaDataList, mData)}"
            isShowCut = false
            return
        }
        val cutPath =
            context.getExternalFilesDir("pic2video")
                .toString() + "/${cutMediaData?.id}_cut_${System.currentTimeMillis()}.mp4"
        CutVideoUtils.cutOutVideo(
            cutMediaData?.filePath!!,
            cutPath,
            cutStart,
            cutEnd,
            object : CutVideoUtils.CompressorListener {
                override fun onStart() {
                    LogUtils.d("CutVideo=>onStart()")
                    DialogUtils.showDialogWithProgress(context, "裁剪视频 0%")
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    LogUtils.d("CutVideo=>onProgress(),progress:$progress")
                    if (progress > 0)
                        DialogUtils.showDialogWithProgress(context, "裁剪视频 $progress%")
                }

                @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
                override fun onSuccess() {
                    LogUtils.d("CutVideo=>onSuccess()")
                    DialogUtils.dismissLoading()
                    isCutVideo = true
                    mDialog?.rl_videoPart_cut?.visibility = View.GONE
                    mDialog?.rv_videoPart?.visibility = View.VISIBLE
                    val mediaInfo = RxFFmpegInvoke.getInstance().getMediaInfo(cutPath)
                    val videoInfo = MediaInfoUtil.format4Video(mediaInfo)
                    val newMediaData4Cut = MediaData(
                        cutMediaData?.id,
                        cutMediaData?.createTime!!,
                        videoInfo.duration.split(" ")[0].split(".")[0].toLong(),
                        cutMediaData?.albumName,
                        cutPath,
                        cutMediaData?.thumbNailPath,
                        cutMediaData?.mimeType,
                        cutMediaData?.latitude,
                        cutMediaData?.longitude,
                        cutMediaData?.isSelected!!
                    )
                    for (index in 0 until mData.size) {
                        if (mData[index].id == newMediaData4Cut.id) {
                            mData[index] = newMediaData4Cut
                            mAdapter?.notifyDataSetChanged()
                        }
                    }

                    mDialog?.tv_videoPart_title?.text = "片段"
                    mDialog?.tv_videoPart_totalTime?.text = "总时长 ${getTotalDuration(mData)}"
                    mDialog?.tv_videoPart_cutTime?.text =
                        "已裁剪 ${getCutDuration(mediaDataList, mData)}"
                    isShowCut = false
                    cutMediaData = newMediaData4Cut
                    ToastUtils.show("裁剪视频成功!")
                }

                override fun onError(message: String?) {
                    LogUtils.d("CutVideo=>onError(),message:$message")
                    DialogUtils.dismissLoading()
                }

                override fun onCancel() {
                    LogUtils.d("CutVideo=>onCancel()")
                    DialogUtils.dismissLoading()
                }
            }
        )
    }

    private fun initRecycleView4Pic(context: AppCompatActivity) {
        mAdapter4Pic = BaseAdapterWithPosition.Builder<Bitmap>()
            .setLayoutId(R.layout.item_video_cut_pic)
            .setData(picBitmapLists)
            .addBindView { itemView, itemData, position ->
                Glide.with(context)
                    .load(itemData)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(itemView.iv_item_videoCut_pic)
            }
            .create()
        mDialog?.rv_videoPart_pic?.adapter = mAdapter4Pic
        mDialog?.rv_videoPart_pic?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }

    /**
     * 计算总时长
     */
    private fun getTotalDuration(mediaInfo: MutableList<MediaData>): String {
        var total = 0L
        for (data in mediaInfo) {
            total += data.duration!!
        }
        return TimeFormat.formatWithMS(total)
    }

    /**
     * 计算已裁剪时长
     */
    private fun getCutDuration(
        startMediaInfo: MutableList<MediaData>,
        cutMediaInfo: MutableList<MediaData>
    ): String {
        var total = 0L
        for (data in startMediaInfo) {
            total += data.duration!!
        }
        var cut = 0L
        for (data in cutMediaInfo) {
            cut += data.duration!!
        }
        return TimeFormat.formatWithMS(total - cut)
    }

    /**
     * 初始化recycleView
     */
    @SuppressLint("CheckResult", "SetTextI18n", "NotifyDataSetChanged")
    private fun initRecycleView(
        context: AppCompatActivity,
        callback: ShowPartCallBack
    ) {
        mAdapter = BaseAdapterWithPosition.Builder<MediaData>()
            .setLayoutId(R.layout.item_video_part)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                Glide.with(context)
                    .load(itemData.thumbNailPath)
                    .into(itemView.iv_item_videoPart_cover)
                itemView.tv_item_videoPart_duration.text =
                    TimeFormat.format(itemData.duration!!, true)

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        picBitmapLists.clear()
                        mAdapter4Pic?.notifyDataSetChanged()

                        mDialog?.tv_videoPart_title?.text = "时长"
                        mDialog?.rv_videoPart?.visibility = View.GONE
                        mDialog?.rl_videoPart_cut?.visibility = View.VISIBLE
                        isShowCut = true
                        callback.selectVideo(itemData)
                        mDialog?.tv_videoPart_totalTime?.text =
                            "已选择 ${TimeFormat.formatWithMS(itemData.duration!!)}"
                        mDialog?.tv_videoPart_cutTime?.text =
                            "最长可选 ${TimeFormat.formatWithMS(itemData.duration!!)}"
                        toShowCut(context, itemData, callback)
                    }
            }
            .create()

        mDialog?.rv_videoPart?.adapter = mAdapter
        mDialog?.rv_videoPart?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }

    /**
     * 展示剪切界面
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun toShowCut(
        context: AppCompatActivity,
        mediaData: MediaData,
        callback: ShowPartCallBack
    ) {
        cutMediaData = mediaData
        cutStart = 0L
        cutEnd = mediaData.duration!!
        mDialog?.cutVideoView_cut?.reset()
        mDialog?.cutVideoView_cut?.setDuration(mediaData.duration!!)
        mDialog?.cutVideoView_cut?.setOnCutoutListener(
            object : CutVideoView.OnCutoutListener {
                @SuppressLint("SetTextI18n")
                override fun videoInterval(
                    startTimeLong: Long,
                    startTime: String,
                    endTimeLong: Long,
                    endTime: String
                ) {
                    cutStart = startTimeLong
                    cutEnd = endTimeLong
                    callback.cutVideo(startTimeLong, startTime, endTimeLong, endTime)
                    mDialog?.tv_videoPart_totalTime?.text =
                        "已选择 ${TimeFormat.formatWithMS(cutEnd - cutStart)}"
                    mDialog?.tv_videoPart_cutTime?.text =
                        "最长可选 ${TimeFormat.formatWithMS(mediaData.duration!!)}"
                }

                override fun videoCurrentIndex(indexLong: Long, index: String) {
                    callback.currentPosition(indexLong)
                }
            }
        )

        isGetPic = true
        picIndex = 0
        val step = mediaData.duration!! / MAX_PIC
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(mediaData.filePath)
        toCycleCutPic(context, mediaMetadataRetriever, step, mediaData.duration!!)
    }

    /**
     * 循环获取帧图片
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun toCycleCutPic(
        context: AppCompatActivity,
        mediaMetadataRetriever: MediaMetadataRetriever,
        step: Long,
        duration: Long
    ) {
        if (picIndex < MAX_PIC) {
            Thread {
                val currentTime =
                    if (step * picIndex > duration) duration * 1000 - 10 else step * picIndex * 1000
                val frameAtIndex = mediaMetadataRetriever.getFrameAtTime(
                    currentTime,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )!!
                val bitmap = Bitmap.createScaledBitmap(
                    frameAtIndex,
                    frameAtIndex.width / 8,
                    frameAtIndex.height / 8,
                    false
                )
                frameAtIndex.recycle()
                picBitmapLists.add(bitmap)
                Thread.sleep(50)
                context.runOnUiThread {
                    mAdapter4Pic?.notifyDataSetChanged()
                    picIndex++
                    toCycleCutPic(context, mediaMetadataRetriever, step, duration)
                }
            }.start()
        } else {
            picIndex = 0
            isGetPic = false
            mediaMetadataRetriever.release()
        }
    }
}