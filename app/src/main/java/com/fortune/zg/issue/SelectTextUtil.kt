@file:Suppress("ReturnInsideFinallyBlock")

package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.PhoneInfoUtils
import com.fortune.zg.utils.ToastUtils
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.android.synthetic.main.item_video_cut_pic.view.*
import kotlinx.android.synthetic.main.layout_select_text.*
import java.util.concurrent.TimeUnit


object SelectTextUtil {
    private var mDialog: Dialog? = null
    private var mAdapter4Pic: BaseAdapterWithPosition<Bitmap>? = null
    private var picBitmapLists = mutableListOf<Bitmap>()

    private var isGetPic = false
    private const val MAX_PIC = 10
    private var picIndex = 0
    private var mTextInfo: TextInfo? = null

    private var rootWidth = 0.0
    private var rootHeight = 0.0
    private var shouldWidth = 0.0
    private var shouldHeight = 0.0

    private var color = 0
    private var type = Typeface.DEFAULT_BOLD
    private var size = 0f
    private var start = 0
    private var end = 0

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

    interface SelectTextCallback {
        fun cancel()
        fun textInfo(textInfo: TextInfo?)
    }

    data class TextInfo(
        var text: String = "",
        var color: Int = 0,
        var size: Float = 0f,
        var type: Typeface = Typeface.DEFAULT_BOLD,
        var start: Int = 0,
        var end: Int = 0
    )

    /**
     * 显示添加文字
     */
    @SuppressLint("CheckResult", "ClickableViewAccessibility")
    fun showSelectText(
        context: AppCompatActivity,
        filePath: String,
        textInfo: TextInfo?,
        callback: SelectTextCallback
    ) {
        mTextInfo = textInfo
        mDialog = Dialog(context, R.style.BeautifulDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_select_text, null) as LinearLayout
        mDialog?.setContentView(root)

        mDialog?.cutVideoView_selectText?.setType(CutVideoView.CutVideoType.CUT_VIDEO)
        mDialog?.setOnShowListener {
            toGetPic(context, filePath, mTextInfo)
        }
        toInitRecycle4Pic(context)

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(filePath)
        val bitmap = mediaMetadataRetriever.getFrameAtTime(0L)
        mDialog?.iv_selectText_pre?.setImageBitmap(bitmap)
        mediaMetadataRetriever.release()
        mDialog?.cutVideoView_selectText?.setOnCutoutListener(object :
            CutVideoView.OnCutoutListener {
            override fun videoInterval(
                startTimeLong: Long,
                startTime: String,
                endTimeLong: Long,
                endTime: String
            ) {
                start = startTimeLong.toInt()
                end = endTimeLong.toInt()
            }

            override fun videoCurrentIndex(indexLong: Long, index: String) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
                val pic = retriever.getFrameAtTime(indexLong * 1000)
                mDialog?.iv_selectText_pre?.setImageBitmap(pic)
                retriever.release()
            }
        })

        toSelectTextColor(context)
        toSelectTextFont(context)
        toSelectTextSize(context)

        RxView.clicks(mDialog?.iv_selectText_back!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                mDialog?.dismiss()
                callback.cancel()
            }

        mDialog?.setOnCancelListener {
            callback.cancel()
        }

        RxView.clicks(mDialog?.iv_selectText_right!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val trim = mDialog?.et_selectText_text?.text.toString().trim()
                if (trim.isNotEmpty()) {
                    mTextInfo = TextInfo(
                        text = trim,
                        color = color,
                        type = type,
                        size = size,
                        start = start,
                        end = end
                    )
                    ToastUtils.show("保存文字信息成功")
                    callback.textInfo(mTextInfo)
                    mDialog?.dismiss()
                } else {
                    callback.textInfo(null)
                    mDialog?.dismiss()
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
     * 选择字体大小
     */
    private fun toSelectTextSize(context: AppCompatActivity) {
        val screenWidth = PhoneInfoUtils.getWidth(context)
        val scale = 360f / screenWidth
        mDialog?.rg_selectText_size?.setOnCheckedChangeListener { group, checkedId ->
            size = when (checkedId) {
                R.id.rb_selectText_moreSmall -> {
                    24 * scale
                }
                R.id.rb_selectText_small -> {
                    48 * scale
                }
                R.id.rb_selectText_normalSize -> {
                    72 * scale
                }
                R.id.rb_selectText_big -> {
                    96 * scale
                }
                R.id.rb_selectText_moreBig -> {
                    128 * scale
                }
                else -> {
                    0f
                }
            }
            mDialog?.et_selectText_text?.textSize = size
        }
        mDialog?.rg_selectText_size?.check(R.id.rb_selectText_normalSize)
    }

    /**
     * 选择字体
     */
    private fun toSelectTextFont(context: AppCompatActivity) {
        val typeface4PTY = Typeface.createFromAsset(context.assets, "FZ_PTY.TTF")
        mDialog?.rb_selectText_pty?.typeface = typeface4PTY

        val typeface4SK = Typeface.createFromAsset(context.assets, "FZ_SK.TTF")
        mDialog?.rb_selectText_sk?.typeface = typeface4SK

        val typeface4WK = Typeface.createFromAsset(context.assets, "FZ_WK.TTF")
        mDialog?.rb_selectText_wk?.typeface = typeface4WK

        val typeface4BZYH = Typeface.createFromAsset(context.assets, "FZ_BZYH.TTF")
        mDialog?.rb_selectText_bzyh?.typeface = typeface4BZYH

        val typeface4FGCS = Typeface.createFromAsset(context.assets, "FZ_FGCS.TTF")
        mDialog?.rb_selectText_fgcs?.typeface = typeface4FGCS
        mDialog?.rg_selectText_font?.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_selectText_normal -> {
                    type = Typeface.DEFAULT_BOLD
                    mDialog?.et_selectText_text?.setTypeface(Typeface.DEFAULT_BOLD)
                }
                R.id.rb_selectText_pty -> {
                    type = typeface4PTY
                    mDialog?.et_selectText_text?.setTypeface(typeface4PTY)
                }
                R.id.rb_selectText_sk -> {
                    type = typeface4SK
                    mDialog?.et_selectText_text?.setTypeface(typeface4SK)
                }
                R.id.rb_selectText_wk -> {
                    type = typeface4WK
                    mDialog?.et_selectText_text?.setTypeface(typeface4WK)
                }
                R.id.rb_selectText_bzyh -> {
                    type = typeface4BZYH
                    mDialog?.et_selectText_text?.setTypeface(typeface4BZYH)
                }
                R.id.rb_selectText_fgcs -> {
                    type = typeface4FGCS
                    mDialog?.et_selectText_text?.setTypeface(typeface4FGCS)
                }
            }
        }
        mDialog?.rg_selectText_font?.check(R.id.rb_selectText_normal)
    }

    /**
     * 选择字体颜色
     */
    private fun toSelectTextColor(context: AppCompatActivity) {
        mDialog?.rg_selectText_color?.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_selectText_red -> {
                    color = context.resources.getColor(R.color.red_F03D3D)
                    mDialog?.et_selectText_text?.setTextColor(context.resources.getColor(R.color.red_F03D3D))
                    mDialog?.et_selectText_text?.setHintTextColor(context.resources.getColor(R.color.red_F03D3D))
                }
                R.id.rb_selectText_green -> {
                    color = context.resources.getColor(R.color.green_2EC8AC)
                    mDialog?.et_selectText_text?.setTextColor(context.resources.getColor(R.color.green_2EC8AC))
                    mDialog?.et_selectText_text?.setHintTextColor(context.resources.getColor(R.color.green_2EC8AC))
                }
                R.id.rb_selectText_blue -> {
                    color = context.resources.getColor(R.color.blue_4F6CED)
                    mDialog?.et_selectText_text?.setTextColor(context.resources.getColor(R.color.blue_4F6CED))
                    mDialog?.et_selectText_text?.setHintTextColor(context.resources.getColor(R.color.blue_4F6CED))
                }
                R.id.rb_selectText_purple -> {
                    color = context.resources.getColor(R.color.purple_6F00D2)
                    mDialog?.et_selectText_text?.setTextColor(context.resources.getColor(R.color.purple_6F00D2))
                    mDialog?.et_selectText_text?.setHintTextColor(context.resources.getColor(R.color.purple_6F00D2))
                }
                R.id.rb_selectText_yellow -> {
                    color = context.resources.getColor(R.color.yellow_FFD306)
                    mDialog?.et_selectText_text?.setTextColor(context.resources.getColor(R.color.yellow_FFD306))
                    mDialog?.et_selectText_text?.setHintTextColor(context.resources.getColor(R.color.yellow_FFD306))
                }
                R.id.rb_selectText_brown -> {
                    color = context.resources.getColor(R.color.brown_804040)
                    mDialog?.et_selectText_text?.setTextColor(context.resources.getColor(R.color.brown_804040))
                    mDialog?.et_selectText_text?.setHintTextColor(context.resources.getColor(R.color.brown_804040))
                }
                R.id.rb_selectText_yellowGreen -> {
                    color = context.resources.getColor(R.color.green_yellow_808040)
                    mDialog?.et_selectText_text?.setTextColor(context.resources.getColor(R.color.green_yellow_808040))
                    mDialog?.et_selectText_text?.setHintTextColor(context.resources.getColor(R.color.green_yellow_808040))
                }
                R.id.rb_selectText_black -> {
                    color = context.resources.getColor(R.color.black_000000)
                    mDialog?.et_selectText_text?.setTextColor(context.resources.getColor(R.color.black_000000))
                    mDialog?.et_selectText_text?.setHintTextColor(context.resources.getColor(R.color.black_000000))
                }
                R.id.rb_selectText_white -> {
                    color = context.resources.getColor(R.color.white_FFFFFF)
                    mDialog?.et_selectText_text?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
                    mDialog?.et_selectText_text?.setHintTextColor(context.resources.getColor(R.color.white_FFFFFF))
                }
            }
        }
        mDialog?.rg_selectText_color?.check(R.id.rb_selectText_red)
    }

    /**
     * 获取帧图片
     */
    private fun toGetPic(context: AppCompatActivity, filePath: String, textInfo: TextInfo?) {
        val mediaInfo = RxFFmpegInvoke.getInstance().getMediaInfo(filePath)
        val mediaDuration4Video = MediaInfoUtil.getMediaDuration4Video(mediaInfo)
        end = mediaDuration4Video.toInt()
        val mediaWidth4Video = MediaInfoUtil.getMediaWidth4Video(mediaInfo)
        val mediaHeight4Video = MediaInfoUtil.getMediaHeight4Video(mediaInfo)
        val screenWidth = PhoneInfoUtils.getWidth(context)
        val screenHeight = PhoneInfoUtils.getHeight(context)
        rootWidth = screenWidth.toDouble()
        rootHeight = screenHeight - screenWidth.toDouble() * (270.0 / 360)
        shouldWidth = rootWidth / 3 * 2
        shouldHeight = rootWidth / 3 * 2 / mediaWidth4Video * mediaHeight4Video
        LogUtils.d("========mediaWidth4Video:$mediaWidth4Video,mediaHeight4Video:$mediaHeight4Video,rootWidth:$rootWidth,rootHeight:$rootHeight,shouldWidth:$shouldWidth,shouldHeight:$shouldHeight")
        val layoutParams = mDialog?.rl_selectText_root_root!!.layoutParams
        layoutParams.width = rootWidth.toInt()
        layoutParams.height = rootHeight.toInt()
        mDialog?.rl_selectText_root_root!!.layoutParams = layoutParams

        val layoutParams1 = mDialog?.rl_selectText_root!!.layoutParams
        layoutParams1.width = shouldWidth.toInt()
        layoutParams1.height = shouldHeight.toInt()
        mDialog?.rl_selectText_root!!.layoutParams = layoutParams1

        mDialog?.cutVideoView_selectText?.setDuration(mediaDuration4Video)
        mDialog?.cutVideoView_selectText?.setIndex(0)
        if (null != textInfo) {
            mDialog?.et_selectText_text?.setText(textInfo.text)
            mDialog?.et_selectText_text?.setTextColor(textInfo.color)
            mDialog?.et_selectText_text?.textSize = textInfo.size
            mDialog?.et_selectText_text?.typeface = textInfo.type

            LogUtils.d("=======111=duration:$mediaDuration4Video")
            mDialog?.cutVideoView_selectText?.setStart(textInfo.start.toLong())
            mDialog?.cutVideoView_selectText?.setEnd(textInfo.end.toLong())

            color = textInfo.color
            type = textInfo.type
            size = textInfo.size
            when (color) {
                context.resources.getColor(R.color.red_F03D3D) -> {
                    mDialog?.rg_selectText_color?.check(R.id.rb_selectText_red)
                }
                context.resources.getColor(R.color.green_2EC8AC) -> {
                    mDialog?.rg_selectText_color?.check(R.id.rb_selectText_green)
                }
                context.resources.getColor(R.color.blue_4F6CED) -> {
                    mDialog?.rg_selectText_color?.check(R.id.rb_selectText_blue)
                }
                context.resources.getColor(R.color.purple_6F00D2) -> {
                    mDialog?.rg_selectText_color?.check(R.id.rb_selectText_purple)
                }
                context.resources.getColor(R.color.yellow_FFD306) -> {
                    mDialog?.rg_selectText_color?.check(R.id.rb_selectText_yellow)
                }
                context.resources.getColor(R.color.brown_804040) -> {
                    mDialog?.rg_selectText_color?.check(R.id.rb_selectText_brown)
                }
                context.resources.getColor(R.color.green_yellow_808040) -> {
                    mDialog?.rg_selectText_color?.check(R.id.rb_selectText_yellowGreen)
                }
                context.resources.getColor(R.color.black_000000) -> {
                    mDialog?.rg_selectText_color?.check(R.id.rb_selectText_black)
                }
                context.resources.getColor(R.color.white_FFFFFF) -> {
                    mDialog?.rg_selectText_color?.check(R.id.rb_selectText_white)
                }
            }
            when (type) {
                Typeface.DEFAULT_BOLD -> {
                    mDialog?.rg_selectText_font?.check(R.id.rb_selectText_normal)
                }
                Typeface.createFromAsset(context.assets, "FZ_PTY.TTF") -> {
                    mDialog?.rg_selectText_font?.check(R.id.rb_selectText_pty)
                }
                Typeface.createFromAsset(context.assets, "FZ_SK.TTF") -> {
                    mDialog?.rg_selectText_font?.check(R.id.rb_selectText_sk)
                }
                Typeface.createFromAsset(context.assets, "FZ_WK.TTF") -> {
                    mDialog?.rg_selectText_font?.check(R.id.rb_selectText_wk)
                }
                Typeface.createFromAsset(context.assets, "FZ_BZYH.TTF") -> {
                    mDialog?.rg_selectText_font?.check(R.id.rb_selectText_bzyh)
                }
                Typeface.createFromAsset(context.assets, "FZ_FGCS.TTF") -> {
                    mDialog?.rg_selectText_font?.check(R.id.rb_selectText_fgcs)
                }
            }

            val screenWidth = PhoneInfoUtils.getWidth(context)
            val scale = 360f / screenWidth
            when (size) {
                24 * scale -> {
                    mDialog?.rg_selectText_size?.check(R.id.rb_selectText_moreSmall)
                }
                48 * scale -> {
                    mDialog?.rg_selectText_size?.check(R.id.rb_selectText_small)
                }
                72 * scale -> {
                    mDialog?.rg_selectText_size?.check(R.id.rb_selectText_normalSize)
                }
                96 * scale -> {
                    mDialog?.rg_selectText_size?.check(R.id.rb_selectText_big)
                }
                128 * scale -> {
                    mDialog?.rg_selectText_size?.check(R.id.rb_selectText_moreBig)
                }
            }
        }

        picBitmapLists.clear()
        val step = mediaDuration4Video / MAX_PIC
        isGetPic = true
        picIndex = 0

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(filePath)
        Thread {
            toCycleCutPic(context, mediaMetadataRetriever, step, mediaDuration4Video)
        }.start()
    }

    /**
     * 循环获取帧图片
     */
    private fun toCycleCutPic(
        context: AppCompatActivity,
        mediaMetadataRetriever: MediaMetadataRetriever,
        step: Long,
        duration: Long
    ) {
        if (picIndex < MAX_PIC) {
            val currentTime =
                if (step * picIndex > duration) duration * 1000 - 10 else step * picIndex * 1000
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
            Thread.sleep(100)
            context.runOnUiThread {
                mAdapter4Pic?.notifyItemChanged(picIndex)
                picIndex++
                Thread {
                    toCycleCutPic(context, mediaMetadataRetriever, step, duration)
                }.start()
            }
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
        mDialog?.rv_selectText_pic?.adapter = mAdapter4Pic
        mDialog?.rv_selectText_pic?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }
}