package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.android.synthetic.main.item_video_cut_pic.view.*
import kotlinx.android.synthetic.main.layout_select_cover.*
import java.util.concurrent.TimeUnit

object SelectCoverUtil {

    private var mDialog: Dialog? = null

    private var mAdapter4Pic: BaseAdapterWithPosition<Bitmap>? = null
    private var picBitmapLists = mutableListOf<Bitmap>()

    private var isGetPic = false
    private const val MAX_PIC = 10
    private var picIndex = 0
    private var currentPosition = 0L

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
     * 选择封面监听
     */
    interface SelectCoverCallBack {
        fun cancel()
        fun currentPosition(position: Long)
        fun cover(position: Long)
    }

    /**
     * 显示选择封面
     */
    @SuppressLint("CheckResult")
    fun showSelectCover(
        context: AppCompatActivity,
        filePath: String,
        position: Long,
        isPicVideo: Boolean = false,
        callback: SelectCoverCallBack
    ) {
        picIndex = 0
        picBitmapLists.clear()
        currentPosition = position

        mDialog = Dialog(context, R.style.BeautifulDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_select_cover, null) as LinearLayout
        mDialog?.setContentView(root)
        mDialog?.cutVideoView_selectCover?.setType(CutVideoView.CutVideoType.SELECT_COVER)

        mDialog?.setOnShowListener {
            toGetPic(context, filePath, isPicVideo)
        }
        toInitRecycle4Pic(context)
        mDialog?.cutVideoView_selectCover?.setOnCutoutListener(object :
            CutVideoView.OnCutoutListener {
            override fun videoInterval(
                startTimeLong: Long,
                startTime: String,
                endTimeLong: Long,
                endTime: String
            ) {

            }

            override fun videoCurrentIndex(indexLong: Long, index: String) {
                currentPosition = indexLong
                callback.currentPosition(indexLong)
            }
        })

        RxView.clicks(mDialog?.iv_selectCover_back!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                callback.cancel()
                mDialog?.dismiss()
            }

        mDialog?.setOnCancelListener {
            callback.cancel()
        }

        RxView.clicks(mDialog?.iv_selectCover_right!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                callback.cover(currentPosition)
                mDialog?.dismiss()
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
     * 获取帧图片
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun toGetPic(context: AppCompatActivity, filePath: String, isPicVideo: Boolean) {
        val mediaInfo = RxFFmpegInvoke.getInstance().getMediaInfo(filePath)
        val mediaDuration4Video = MediaInfoUtil.getMediaDuration4Video(mediaInfo)
        picBitmapLists.clear()
        mAdapter4Pic?.notifyDataSetChanged()
        mDialog?.cutVideoView_selectCover?.setDuration(mediaDuration4Video)
        mDialog?.cutVideoView_selectCover?.setIndex(if (currentPosition > mediaDuration4Video) 0 else currentPosition)
        val step = mediaDuration4Video / MAX_PIC
        isGetPic = true
        picIndex = 0

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(filePath)
        toCycleCutPic(context, mediaMetadataRetriever, step, mediaDuration4Video, isPicVideo)
    }

    /**
     * 循环获取帧图片
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun toCycleCutPic(
        context: AppCompatActivity,
        mediaMetadataRetriever: MediaMetadataRetriever,
        step: Long,
        duration: Long,
        isPicVideo: Boolean
    ) {
        if (picIndex < MAX_PIC) {
            Thread {
                val currentTime = if (isPicVideo) {
                    if (step * picIndex >= duration) duration * 1000 - 500 * 1000 else step * picIndex * 1000 + 500 * 1000
                } else {
                    if (step * picIndex > duration) duration * 1000 - 10 else step * picIndex * 1000
                }
                val frameAtIndex = mediaMetadataRetriever.getFrameAtTime(
                    currentTime,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )!!
                val bitmap = Bitmap.createScaledBitmap(
                    frameAtIndex,
                    frameAtIndex.width / 4,
                    frameAtIndex.height / 4,
                    false
                )
                frameAtIndex.recycle()
                picBitmapLists.add(bitmap)
                Thread.sleep(50)
                context.runOnUiThread {
                    mAdapter4Pic?.notifyDataSetChanged()
                    picIndex++
                    toCycleCutPic(context, mediaMetadataRetriever, step, duration, isPicVideo)
                }
            }.start()
        } else {
            picIndex = 0
            isGetPic = false
            mediaMetadataRetriever.release()
        }
    }

    /**
     * 初始化RecycleView
     */
    private fun toInitRecycle4Pic(context: AppCompatActivity) {
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
        mDialog?.rv_selectCover_pic?.adapter = mAdapter4Pic
        mDialog?.rv_selectCover_pic?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }
}