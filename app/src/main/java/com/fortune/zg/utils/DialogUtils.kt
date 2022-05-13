package com.fortune.zg.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fortune.zg.R
import com.fortune.zg.issue.BlurTransformation
import kotlinx.android.synthetic.main.dialog_beautiful.*
import kotlinx.android.synthetic.main.dialog_loading.tv_dialog_message
import kotlinx.android.synthetic.main.dialog_make_video.*
import kotlinx.android.synthetic.main.layout_dialog_default.*


object DialogUtils {
    private var mDialog: Dialog? = null

    /**
     * 取消加载框
     */
    fun dismissLoading() {
        try {
            if (mDialog != null && mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
        } catch (e: Exception) {

        } finally {
            mDialog = null
        }
    }

    interface OnShowListener {
        fun onShowing()
    }

    /**
     * 合成视频Dialog
     */
    fun showDialog4MakeVideo(
        context: AppCompatActivity,
        msg: String,
        bg: Bitmap?,
        bgPath: String?,
        listener: OnShowListener? = null
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = Dialog(context, R.style.BeautifulDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.dialog_make_video, null) as LinearLayout
        mDialog?.setContentView(root)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)
        mDialog?.tv_dialog_message?.text = msg
        Glide.with(context)
            .load(bgPath ?: bg)
            .apply(
                RequestOptions.bitmapTransform(
                    BlurTransformation(
                        context,
                        if (null == bgPath) 5 else 20,
                        1
                    )
                )
            )
            .into(object : SimpleTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    mDialog?.iv_dialog_bg?.setImageDrawable(resource)
                    listener?.onShowing()
                }
            })

        val screenHeight = PhoneInfoUtils.getHeight(context)
        val layoutParams = root.layoutParams
        layoutParams.height = screenHeight
        root.layoutParams = layoutParams
        val dialogWindow = mDialog?.window!!
        dialogWindow.setWindowAnimations(R.style.dialog_anim)
        dialogWindow.setGravity(Gravity.BOTTOM)
        val attributes = dialogWindow.attributes!!
        attributes.x = 0
        attributes.y = 0
        attributes.width = context.resources.displayMetrics.widthPixels
        dialogWindow.attributes = attributes

        mDialog?.show()
    }

    /**
     * 显示带有进度条的Dialog
     */
    fun showDialogWithProgress(context: Context, msg: String) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = Dialog(context, R.style.BeautifulDialog)
        mDialog?.setContentView(R.layout.dialog_progress)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)
        mDialog?.tv_dialog_message?.text = msg
        mDialog?.setOnCancelListener {
            dismissLoading()
        }
        mDialog?.show()
    }

    /**
     * 显示带有loading的Dialog
     */
    fun showDialogWithLoading(context: Context, msg: String) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = Dialog(context, R.style.BeautifulDialog)
        mDialog?.setContentView(R.layout.dialog_loading)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)
        mDialog?.tv_dialog_message?.text = msg
        mDialog?.setOnCancelListener {
            dismissLoading()
        }
        mDialog?.show()
    }

    /**
     * 设置提示信息
     */
    fun setDialogMsg(msg: String) {
        if (mDialog != null && mDialog?.isShowing == true)
            mDialog?.tv_dialog_message?.text = msg
    }

    /**
     * 花里胡哨加载条
     */
    fun showBeautifulDialog(context: Context) {
        (context as Activity).window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = Dialog(context, R.style.BeautifulDialog)
        mDialog?.setContentView(R.layout.dialog_beautiful)
        mDialog?.setCancelable(false)
        mDialog?.av_dialog?.show()
        mDialog?.setOnCancelListener {
            mDialog?.av_dialog?.hide()
        }
        mDialog?.show()
    }

    /**
     * 显示普通Dialog
     */
    fun showDefaultDialog(
        context: Context,
        title: String,
        msg: String,
        cancel: String,
        sure: String,
        listener: OnDialogListener
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = Dialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_default)
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)
        mDialog?.tv_dialog_default_title?.text = title
        mDialog?.tv_dialog_default_cancel?.text = cancel
        mDialog?.tv_dialog_default_sure?.text = sure
        mDialog?.tv_dialog_default_message?.text = msg
        mDialog?.tv_dialog_default_sure?.setOnClickListener {
            dismissLoading()
            listener.next()
        }
        mDialog?.tv_dialog_default_cancel?.setOnClickListener {
            dismissLoading()
        }
        mDialog?.setOnCancelListener {
            dismissLoading()
        }
        mDialog?.show()
    }

    /**
     * 显示停止下载Dialog
     */
    fun showStopDownloadDialog(
        context: Context,
        title: String,
        msg: String,
        cancel: String,
        sure: String,
        listener: OnStopDownloadListener
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = Dialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_default)
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)
        mDialog?.tv_dialog_default_title?.text = title
        mDialog?.tv_dialog_default_cancel?.text = cancel
        mDialog?.tv_dialog_default_sure?.text = sure
        mDialog?.tv_dialog_default_message?.text = msg
        mDialog?.tv_dialog_default_sure?.setOnClickListener {
            dismissLoading()
            listener.next()
        }
        mDialog?.tv_dialog_default_cancel?.setOnClickListener {
            dismissLoading()
            listener.cancel()
        }
        mDialog?.setOnCancelListener {
            dismissLoading()
            listener.cancel()
        }
        mDialog?.show()
    }

    interface OnStopDownloadListener {
        fun cancel() //取消
        fun next() //确认
    }

    interface OnDialogListener {
        fun next()
    }
}