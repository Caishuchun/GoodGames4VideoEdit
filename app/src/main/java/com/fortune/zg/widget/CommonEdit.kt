package com.fortune.zg.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.bean.CommonContentBean
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.language.LanguageConfig
import com.luck.picture.lib.listener.OnResultCallbackListener
import com.luck.picture.lib.style.PictureCropParameterStyle
import com.luck.picture.lib.style.PictureParameterStyle
import kotlinx.android.synthetic.main.item_common_pic.view.*
import kotlinx.android.synthetic.main.layout_common_edit.view.*
import java.util.concurrent.TimeUnit

/**
 * 评论输入框
 */
class CommonEdit : LinearLayout {
    private var mView: View? = null
    private var mSelectData = mutableListOf<LocalMedia>()
    private var mAdapter: BaseAdapterWithPosition<LocalMedia>? = null
    private var isShowSoftKeyBoard = false
    private var mOnCommonSubmit: OnCommonSubmit? = null

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        initView(context)
    }

    @SuppressLint("SetTextI18n")
    private fun initView(context: Context) {
        mView = LayoutInflater.from(context).inflate(R.layout.layout_common_edit, this, true)

        mView?.iv_common_edit_pic?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    toGetData(context)
                }
        }
        mView?.iv_common_edit_below_pic?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    toGetData(context)
                }
        }
        mView?.tv_common_edit_submit?.let {
            RxView.clicks(it)
                .throttleFirst(
                    if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                    TimeUnit.MILLISECONDS
                )
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        toSubmit(context)
                    } else {
                        LoginUtils.toQuickLogin(context as Activity)
                    }
                }
        }
    }

    private fun toSubmit(context: Context) {
        if (!checkHasInfo()) {
            ToastUtils.show(context.getString(R.string.string_018))
        } else {
            val formatCommon = toFormatCommon()
            mOnCommonSubmit?.submit(formatCommon)
            mView?.et_common_edit_info?.setText("")
            mSelectData.clear()
            mAdapter?.notifyDataSetChanged()
            mView?.cv_common_edit?.visibility = View.GONE
            mView?.tv_common_edit_submit?.visibility = View.GONE
            mView?.iv_common_edit_pic?.visibility = View.VISIBLE
            mView?.ll_common_edit_below?.visibility = View.GONE
            OtherUtils.hindKeyboard(context, mView?.tv_common_edit_submit!!)
            resetHint(context)
            mView?.et_common_edit_info?.clearFocus()
            mView?.iv_common_edit_pic?.requestFocus()
        }
    }

    /**
     * 格式化数据
     */
    private fun toFormatCommon(): String {
        val commonContentBean = CommonContentBean()
        commonContentBean.setText(mView?.et_common_edit_info?.text?.toString()?.trim())
        if (mSelectData.isNotEmpty()) {
            val list = mutableListOf<CommonContentBean.ListBean>()
            for (index in 0 until mSelectData.size) {
                val listBean = CommonContentBean.ListBean()
                val localMedia = mSelectData[index]
                if (localMedia.mimeType.contains("image")) {
                    //图片
                    listBean.url =
                        if (localMedia.isCompressed) localMedia.compressPath else localMedia.realPath
                } else if (localMedia.mimeType.contains("video")) {
                    //视频
                    listBean.url = localMedia.realPath
                    listBean.cover = "video"
                }
                list.add(listBean)
            }
            commonContentBean.setList(list)
        }
        return Gson().toJson(commonContentBean)
    }

    /**
     * 通知改变软键盘变化
     */
    fun changeSoftKeyBoardState(isShow: Boolean) {
        isShowSoftKeyBoard = isShow
        mHandler.sendEmptyMessage(0)
    }

    private var mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (isShowSoftKeyBoard) {
                mView?.tv_common_edit_submit?.visibility = View.VISIBLE
                mView?.iv_common_edit_pic?.visibility = View.GONE
                mView?.ll_common_edit_below?.visibility = View.VISIBLE
            } else {
                if (checkHasInfo()) {
                    mView?.tv_common_edit_submit?.visibility = View.VISIBLE
                    mView?.iv_common_edit_pic?.visibility = View.GONE
                    mView?.ll_common_edit_below?.visibility = View.VISIBLE
                } else {
                    mView?.tv_common_edit_submit?.visibility = View.GONE
                    mView?.iv_common_edit_pic?.visibility = View.VISIBLE
                    mView?.ll_common_edit_below?.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 检查是否有数据
     */
    private fun checkHasInfo() = when {
        mSelectData.isNotEmpty() -> {
            true
        }
        mView?.et_common_edit_info?.text?.toString()?.isNotEmpty() == true -> {
            true
        }
        isAllSpace() -> {
            false
        }
        else -> false
    }

    /**
     * 判断输入的文本是不是都是空格
     */
    private fun isAllSpace(): Boolean {
        val text = mView?.et_common_edit_info?.text?.toString()?.trim()
        return text?.replace(" ", "")?.length == 0
    }


    /**
     * 去选取数据,图片或者视频
     */
    private fun toGetData(context: Context) {
        val pictureParameterStyle = PictureParameterStyle()
        pictureParameterStyle.isChangeStatusBarFontColor = true
        pictureParameterStyle.pictureStatusBarColor =
            ContextCompat.getColor(context, R.color.white_FFFFFF)
        pictureParameterStyle.isOpenCheckNumStyle = true
        pictureParameterStyle.pictureCheckedStyle = R.drawable.bg_pic_check
        pictureParameterStyle.isOpenCompletedNumStyle = true
        pictureParameterStyle.pictureTitleTextSize = 20
        pictureParameterStyle.pictureTitleTextColor =
            ContextCompat.getColor(context, R.color.black_1A241F)
        pictureParameterStyle.pictureCancelTextColor =
            ContextCompat.getColor(context, R.color.orange_FFC273)
        pictureParameterStyle.pictureLeftBackIcon = R.mipmap.back_black
        pictureParameterStyle.pictureTitleUpResId = R.mipmap.up
        pictureParameterStyle.pictureTitleDownResId = R.mipmap.down
        pictureParameterStyle.pictureBottomBgColor =
            ContextCompat.getColor(context, R.color.black_2A2C36)
        pictureParameterStyle.picturePreviewTextColor =
            ContextCompat.getColor(context, R.color.green_2EA992)
        pictureParameterStyle.pictureUnPreviewTextColor =
            ContextCompat.getColor(context, R.color.gray_F7F7F7)
        pictureParameterStyle.pictureCompleteTextColor =
            ContextCompat.getColor(context, R.color.green_2EA992)
        pictureParameterStyle.pictureUnCompleteTextColor =
            ContextCompat.getColor(context, R.color.gray_F7F7F7)

        val pictureCropParameterStyle = PictureCropParameterStyle(
            ContextCompat.getColor(context, R.color.white_FFFFFF),
            ContextCompat.getColor(context, R.color.white_FFFFFF),
            ContextCompat.getColor(context, R.color.black_2A2C36),
            pictureParameterStyle.isChangeStatusBarFontColor
        )

        PictureSelector.create(context as Activity)
            .openGallery(PictureMimeType.ofAll())
            .isCamera(false)
            .selectionMode(PictureConfig.MULTIPLE)
            .maxSelectNum(9)
            .videoMaxSecond(5 * 60)
            .maxVideoSelectNum(9)
            .loadImageEngine(GlideEngine.createGlideEngine())
            .selectionData(mSelectData)
            .isCompress(true)
            .setPictureStyle(pictureParameterStyle)
            .setPictureCropStyle(pictureCropParameterStyle)
            .isWithVideoImage(true)
            .setLanguage(LanguageConfig.CHINESE)
            .forResult(object : OnResultCallbackListener<LocalMedia> {
                @SuppressLint("SetTextI18n")
                override fun onResult(result: MutableList<LocalMedia>?) {
                    LogUtils.d("${javaClass.simpleName}=PictureSelector==>${Gson().toJson(result)}")
                    mSelectData = result!!
                    mHandler.sendEmptyMessage(0)
                    toSetData(context)
                }

                override fun onCancel() {
                }
            })
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun toSetData(context: Activity) {
        mView?.cv_common_edit?.visibility = VISIBLE
        mAdapter = BaseAdapterWithPosition.Builder<LocalMedia>()
            .setData(mSelectData)
            .setLayoutId(R.layout.item_common_pic)
            .addBindView { itemView, itemData, position ->
                mView?.cv_common_edit?.visibility = VISIBLE
                if (itemData.mimeType != null && itemData.mimeType.startsWith("image")) {
                    //图片
                    itemView.iv_item_common_video.visibility = GONE
                    Glide.with(context)
                        .load(if (itemData.isCompressed) itemData.compressPath else itemData.realPath)
                        .placeholder(R.mipmap.bg_gray_6)
                        .into(itemView.iv_item_common_pic)
                } else if (itemData.mimeType != null && itemData.mimeType.startsWith("video")) {
                    //视频
                    itemView.iv_item_common_video.visibility = VISIBLE
                    Glide.with(context)
                        .load(itemData.realPath)
                        .placeholder(R.mipmap.bg_gray_6)
                        .into(itemView.iv_item_common_pic)
                }
                itemView.tv_item_common_num.text = "${position + 1}"
                RxView.clicks(itemView.iv_item_common_delete)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        mHandler.sendEmptyMessage(0)
                        //删除
                        mSelectData.remove(itemData)
                        if (mSelectData.size == 0) {
                            mView?.cv_common_edit?.visibility = GONE
                        } else {
                            mAdapter?.notifyDataSetChanged()
                        }
                    }
            }
            .create()
        mView?.cv_common_edit?.adapter = mAdapter
        mView?.cv_common_edit?.layoutManager =
            SafeLinearLayoutManager(context, RecyclerView.HORIZONTAL)
    }

    interface OnCommonSubmit {
        fun submit(common: String)
    }

    fun setOnCommonSubmit(onCommonSubmit: OnCommonSubmit) {
        mOnCommonSubmit = onCommonSubmit
    }

    fun setHint(charSequence: CharSequence) {
        mView?.et_common_edit_info?.hint = charSequence
    }

    private fun resetHint(context: Context) {
        mView?.et_common_edit_info?.hint = context.getString(R.string.string_019)
    }

    fun getFocus() {
        mView?.et_common_edit_info?.requestFocus()
    }
}