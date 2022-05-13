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
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.item_cut_pic.view.*
import kotlinx.android.synthetic.main.item_video_cut_pic.view.*
import kotlinx.android.synthetic.main.item_video_part.view.*
import kotlinx.android.synthetic.main.layout_select_cover.*
import java.util.concurrent.TimeUnit

object CoverUtil {
    private var mDialog: Dialog? = null
    private var mData = mutableListOf<MediaData>()
    private var mAdapter4PicList: BaseAdapterWithPosition<MediaData>? = null

    private var mAdapter: BaseAdapterWithPosition<MediaData>? = null
    private const val MAX_PIC = 10
    private var picIndex = 0
    private var isSelectCover = false
    private var tempVideoPosition = 0
    private var videoPosition = 0
    private var coverPosition = 1

    private var mAdapter4Pic: BaseAdapterWithPosition<Bitmap>? = null
    private var picBitmapLists = mutableListOf<Bitmap>()

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

    interface OnShowSelectCoverCallback {
        fun cancel()
        fun selectPicCover(position: Int)
        fun selectVideo(position: Int)
        fun selectVideoCover(time: Int)
        fun finish(position: Int, time: Int? = null)
    }

    /**
     * 选封面
     */
    @SuppressLint("CheckResult")
    fun showSelectCover(
        context: AppCompatActivity,
        type: Int,
        position: Int,
        time: Int? = null,
        selected: MutableList<MediaData>,
        callback: OnShowSelectCoverCallback
    ) {
        videoPosition = 0
        coverPosition = 1
        mData.clear()
        mData.addAll(selected)

        mDialog = Dialog(context, R.style.BeautifulDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_select_cover, null) as LinearLayout
        mDialog?.setContentView(root)

        RxView.clicks(mDialog?.iv_selectCover_back!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (isSelectCover) {
                    isSelectCover = false
                    mDialog?.rv_selectCover?.visibility = View.VISIBLE
                    mDialog?.rl_selectCover_cut?.visibility = View.GONE
                } else {
                    dismiss()
                    callback.cancel()
                }
            }

        mDialog?.setOnCancelListener {
            callback.cancel()
        }

        RxView.clicks(mDialog?.iv_selectCover_right!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                when (type) {
                    0 -> {
                        callback.finish(currentSelectPicPosition)
                        dismiss()
                    }
                    1 -> {
                        if (isSelectCover) {
                            isSelectCover = false
                            mDialog?.rv_selectCover?.visibility = View.VISIBLE
                            mDialog?.rl_selectCover_cut?.visibility = View.GONE
                            videoPosition = tempVideoPosition
                        } else {
                            callback.finish(videoPosition, coverPosition)
                            dismiss()
                        }
                    }
                    2 -> {
                    }
                }
            }

        //0仅图片,1仅视频,2图片+视频
        when (type) {
            0 -> {
                mDialog?.rl_selectCover_picList?.visibility = View.VISIBLE
                mDialog?.rl_selectCover_video?.visibility = View.GONE
                initRecycleView4PicList(context, callback)
                currentSelectPicPosition = position
            }
            1 -> {
                mDialog?.rl_selectCover_picList?.visibility = View.GONE
                mDialog?.rl_selectCover_video?.visibility = View.VISIBLE
                mDialog?.cutVideoView_selectCover?.setType(CutVideoView.CutVideoType.SELECT_COVER)
                videoPosition = position
                coverPosition = time ?: 0
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
        mDialog?.rv_selectCover_pic?.adapter = mAdapter4Pic
        mDialog?.rv_selectCover_pic?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }

    /**
     * 初始化recycleView
     */
    @SuppressLint("CheckResult", "SetTextI18n", "NotifyDataSetChanged")
    private fun initRecycleView(
        context: AppCompatActivity,
        callback: OnShowSelectCoverCallback
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

                        mDialog?.rv_selectCover?.visibility = View.GONE
                        mDialog?.rl_selectCover_cut?.visibility = View.VISIBLE

                        callback.selectVideo(position)
                        toShowCover(context, position, callback)
                    }
            }
            .create()

        mDialog?.rv_selectCover?.adapter = mAdapter
        mDialog?.rv_selectCover?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }

    /**
     * 展示封面选择界面
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun toShowCover(
        context: AppCompatActivity,
        position: Int,
        callback: OnShowSelectCoverCallback
    ) {
        isSelectCover = true
        tempVideoPosition = position
        if (videoPosition == position) {
            mDialog?.cutVideoView_selectCover?.setIndex(coverPosition.toLong())
        } else {
            mDialog?.cutVideoView_selectCover?.setIndex(0L)
        }
        val mediaData = mData[position]
        mDialog?.cutVideoView_selectCover?.reset()
        mDialog?.cutVideoView_selectCover?.setDuration(mediaData.duration!!)
        mDialog?.cutVideoView_selectCover?.setOnCutoutListener(
            object : CutVideoView.OnCutoutListener {
                @SuppressLint("SetTextI18n")
                override fun videoInterval(
                    startTimeLong: Long,
                    startTime: String,
                    endTimeLong: Long,
                    endTime: String
                ) {
                }

                override fun videoCurrentIndex(indexLong: Long, index: String) {
                    coverPosition = indexLong.toInt()
                    callback.selectVideoCover(indexLong.toInt())
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
     * 初始化图片
     */
    private var currentSelectPicPosition = 0

    @SuppressLint("CheckResult", "SetTextI18n", "NotifyDataSetChanged")
    private fun initRecycleView4PicList(
        context: AppCompatActivity,
        callback: OnShowSelectCoverCallback
    ) {
        currentSelectPicPosition = 0
        mAdapter4PicList = BaseAdapterWithPosition.Builder<MediaData>()
            .setLayoutId(R.layout.item_cut_pic)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                if (position == currentSelectPicPosition) {
                    itemView.rl_item_cutPic_root.setBackgroundResource(R.drawable.bg_rectangle_red_line)
                } else {
                    itemView.rl_item_cutPic_root.setBackgroundResource(R.drawable.bg_rectangle_green_line)
                }
                Glide.with(context)
                    .load(itemData.filePath)
                    .into(itemView.iv_item_cutPic_pic)

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        currentSelectPicPosition = position
                        callback.selectPicCover(currentSelectPicPosition)
                        itemView.rl_item_cutPic_root.setBackgroundResource(R.drawable.bg_rectangle_red_line)
                        mAdapter4PicList?.notifyDataSetChanged()
                    }
            }
            .create()
        mDialog?.rv_selectCover_picList?.adapter = mAdapter4PicList
        mDialog?.rv_selectCover_picList?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }
}