package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.utils.DialogUtils
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.ToastUtils
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.android.synthetic.main.item_cut_pic.view.*
import kotlinx.android.synthetic.main.item_video_cut_pic.view.*
import kotlinx.android.synthetic.main.item_video_part.view.*
import kotlinx.android.synthetic.main.layout_cut.*
import java.util.concurrent.TimeUnit

object CutUtil {
    private var mDialog: Dialog? = null

    private var mAdapter: BaseAdapterWithPosition<MediaData>? = null
    private var isShowCut = false

    private var mAdapter4Pic: BaseAdapterWithPosition<Bitmap>? = null
    private var picBitmapLists = mutableListOf<Bitmap>()

    private var mAdapter4PicList: BaseAdapterWithPosition<MediaData>? = null

    private const val MAX_PIC = 10
    private var picIndex = 0
    private var mediaDataList = mutableListOf<MediaData>()
    private var mData = mutableListOf<MediaData>()
    private var cutMediaData: MediaData? = null
    private var cutStart = 0L
    private var cutEnd = 0L
    private var isCutVideo = false //是否剪辑过视频

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
         * 删除图片,返回剩余数据
         */
        fun deletePic(
            remain: MutableList<MediaData>
        )

        /**
         *剪切结束
         */
        fun finish(result: MutableList<MediaData>)
    }

    /**
     * 显示裁剪界面
     * @param type 0仅图片,1仅视频,2图片+视频
     */
    @SuppressLint("SetTextI18n", "CheckResult")
    fun showCut(
        context: AppCompatActivity,
        mediaInfo: MutableList<MediaData>,
        type: Int,
        callback: ShowPartCallBack
    ) {
        picIndex = 0
        isShowCut = false
        isCutVideo = false
        mediaDataList.clear()
        mediaDataList.addAll(mediaInfo)
        mData.clear()
        mData.addAll(mediaInfo)

        mDialog = Dialog(context, R.style.BeautifulDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_cut, null) as LinearLayout
        mDialog?.setContentView(root)

        mDialog?.setOnCancelListener {
            callback.cancel()
        }

        RxView.clicks(mDialog?.iv_cut_back!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (isShowCut) {
                    mDialog?.rv_cut?.visibility = View.VISIBLE
                    mDialog?.rl_cut_cut?.visibility = View.GONE

                    mDialog?.tv_cut_title?.text = "片段"
                    mDialog?.tv_cut_totalTime?.text = "总时长 ${
                        getTotalDuration(
                            mData
                        )
                    }"
                    mDialog?.tv_cut_cutTime?.text =
                        "已裁剪 ${
                            getCutDuration(
                                mediaDataList,
                                mData
                            )
                        }"
                    isShowCut = false
                } else {
                    dismiss()
                    callback.cancel()
                }
            }

        RxView.clicks(mDialog?.iv_cut_right!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (isShowCut) {
                    // 剪辑
                    toCutVideo(context, callback)
                } else {
                    callback.finish(mData)
                    dismiss()
                }
            }

        when (type) {
            0 -> {
                mDialog?.rl_cut_video?.visibility = View.GONE
                mDialog?.rl_cut_pic?.visibility = View.VISIBLE
                mDialog?.tv_cut_totalTime?.text = "总时长 ${mediaInfo.size * 3.0}s"
                mDialog?.tv_cut_cutTime?.text = "已裁剪 0.0s"
                initRecycleView4PicList(context, callback)
            }
            1 -> {
                mDialog?.rl_cut_video?.visibility = View.VISIBLE
                mDialog?.rl_cut_pic?.visibility = View.GONE
                mDialog?.tv_cut_totalTime?.text = "总时长 ${
                    getTotalDuration(
                        mData
                    )
                }"
                mDialog?.tv_cut_cutTime?.text = "已裁剪 ${
                    getCutDuration(
                        mediaDataList,
                        mData
                    )
                }"

                mDialog?.cutVideoView_cut?.setType(CutVideoView.CutVideoType.CUT_VIDEO)

                initRecycleView(context, callback)
                initRecycleView4Pic(context)
            }
            2 -> {
            }
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
     * 裁剪视频
     */
    @SuppressLint("SetTextI18n")
    private fun toCutVideo(context: AppCompatActivity, callback: ShowPartCallBack) {
        if (cutEnd - cutStart < 1000) {
            ToastUtils.show("视频裁剪至少需要保留1s时长")
            return
        } else if (cutStart == 0L && cutEnd == cutMediaData?.duration!!) {
            mDialog?.rv_cut?.visibility = View.VISIBLE
            mDialog?.rl_cut_cut?.visibility = View.GONE

            mDialog?.tv_cut_title?.text = "片段"
            mDialog?.tv_cut_totalTime?.text = "总时长 ${
                getTotalDuration(
                    mData
                )
            }"
            mDialog?.tv_cut_cutTime?.text =
                "已裁剪 ${
                    getCutDuration(
                        mediaDataList,
                        mData
                    )
                }"
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
                    mDialog?.rl_cut_cut?.visibility = View.GONE
                    mDialog?.rv_cut?.visibility = View.VISIBLE
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

                    mDialog?.tv_cut_title?.text = "片段"
                    mDialog?.tv_cut_totalTime?.text = "总时长 ${
                        getTotalDuration(
                            mData
                        )
                    }"
                    mDialog?.tv_cut_cutTime?.text =
                        "已裁剪 ${
                            getCutDuration(
                                mediaDataList,
                                mData
                            )
                        }"
                    isShowCut = false
                    cutMediaData = newMediaData4Cut
                    for (index in mData.indices) {
                        if (mData[index].id == cutMediaData!!.id) {
                            mData[index] = cutMediaData!!
                        }
                    }
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

    /**
     * 初始化照片集合
     */
    private var currentSelectPicPosition = -1 //当前选择删除图片的位置

    @SuppressLint("CheckResult", "SetTextI18n", "NotifyDataSetChanged")
    private fun initRecycleView4PicList(context: AppCompatActivity, callback: ShowPartCallBack) {
        currentSelectPicPosition = -1
        mAdapter4PicList = BaseAdapterWithPosition.Builder<MediaData>()
            .setLayoutId(R.layout.item_cut_pic)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                if (position == currentSelectPicPosition) {
                    itemView.fl_item_cutPic_delete.visibility = View.VISIBLE
                } else {
                    itemView.fl_item_cutPic_delete.visibility = View.GONE
                }
                Glide.with(context)
                    .load(itemData.filePath)
                    .into(itemView.iv_item_cutPic_pic)

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        currentSelectPicPosition = position
                        itemView.fl_item_cutPic_delete.visibility = View.VISIBLE
                        mAdapter4PicList?.notifyDataSetChanged()
                    }
                RxView.clicks(itemView.iv_item_cutPic_delete)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (mData.size > 1) {
                            currentSelectPicPosition = -1
                            mData.remove(itemData)
                            callback.deletePic(mData)
                            mDialog?.tv_cut_cutTime?.text =
                                "已裁剪 ${(mediaDataList.size - mData.size) * 3.0f}s"
                            mAdapter4PicList?.notifyDataSetChanged()
                        } else {
                            ToastUtils.show("至少需要保留一张图片")
                        }
                    }
            }
            .create()
        mDialog?.rv_cut_picList?.adapter = mAdapter4PicList
        mDialog?.rv_cut_picList?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
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

                        mDialog?.tv_cut_title?.text = "时长"
                        mDialog?.rv_cut?.visibility = View.GONE
                        mDialog?.rl_cut_cut?.visibility = View.VISIBLE
                        isShowCut = true
                        callback.selectVideo(itemData)
                        mDialog?.tv_cut_totalTime?.text =
                            "已选择 ${TimeFormat.formatWithMS(itemData.duration!!)}"
                        mDialog?.tv_cut_cutTime?.text =
                            "最长可选 ${TimeFormat.formatWithMS(itemData.duration!!)}"
                        toShowCut(context, itemData, callback)
                    }
            }
            .create()

        mDialog?.rv_cut?.adapter = mAdapter
        mDialog?.rv_cut?.layoutManager =
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
                    mDialog?.tv_cut_totalTime?.text =
                        "已选择 ${TimeFormat.formatWithMS(cutEnd - cutStart)}"
                    mDialog?.tv_cut_cutTime?.text =
                        "最长可选 ${TimeFormat.formatWithMS(mediaData.duration!!)}"
                }

                override fun videoCurrentIndex(indexLong: Long, index: String) {
                    callback.currentPosition(indexLong)
                }
            }
        )

        picIndex = 0
        val step = mediaData.duration!! / MAX_PIC
        toGetPic(context, mediaData, step, mediaData.duration!!)
    }

    /**
     * 获取图片
     */
    @SuppressLint("CheckResult")
    private fun toGetPic(
        context: AppCompatActivity,
        mediaData: MediaData,
        step: Long,
        duration: Long
    ) {
        val currentTime =
            if (step * picIndex > duration) duration * 1000 - 10 else step * picIndex * 1000
        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .frame(currentTime)
            .centerCrop()
        val target = object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (picIndex < MAX_PIC) {
                    picBitmapLists.add(resource)
                    mAdapter4Pic?.notifyItemChanged(picBitmapLists.size - 1)
                    picIndex++
                    toGetPic(context, mediaData, step, duration)
                } else {
                    picIndex = 0
                }
            }
        }
        Glide.with(context)
            .setDefaultRequestOptions(options)
            .asBitmap()
            .load(mediaData.filePath)
            .into(target)
    }

    /**
     * 初始化帧图片
     */
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
        mDialog?.rv_cut_pic?.adapter = mAdapter4Pic
        mDialog?.rv_cut_pic?.layoutManager =
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
}