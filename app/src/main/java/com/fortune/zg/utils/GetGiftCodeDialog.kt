package com.fortune.zg.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import com.fortune.zg.R
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.dialog_get_gift_code.*
import java.util.concurrent.TimeUnit

/**
 * 获取礼包码
 */
object GetGiftCodeDialog {
    private var mDialog: Dialog? = null

    /**
     * 取消加载框
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
     * 显示
     */
    fun show(
        context: Context,
        code: String
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = Dialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.dialog_get_gift_code)

        mDialog?.tv_get_gift_code_code?.let {
            it.text = code
            val width = PhoneInfoUtils.getWidth(context as Activity)
            it.spacing = 40 * (360f / width)
        }
        mDialog?.view_get_gift_code_sure?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    dismiss()
                }
        }

        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)
        mDialog?.setOnCancelListener {
            dismiss()
        }
        mDialog?.show()
    }
}