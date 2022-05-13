package com.fortune.zg.utils

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import com.fortune.zg.R
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.dialog_input.*
import java.util.concurrent.TimeUnit

object InputTextDialog {
    private var mDialog: Dialog? = null

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
     * 显示文本输入框
     */
    fun showInput(
        context: Context,
        text: String?,
        listener: InputTextListener
    ) {
        if (mDialog != null) {
            dismiss()
        }
        mDialog = Dialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.dialog_input)
        if (text != null) {
            mDialog?.et_inputText_enter?.setText(text)
        }

        mDialog?.tv_inputText_cancel?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    dismiss()
                }
        }

        mDialog?.tv_inputText_sure?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    listener.enter(mDialog?.et_inputText_enter?.text.toString().trim())
                    dismiss()
                }
        }

        val attributes = mDialog?.window?.attributes!!
        try {
            attributes.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mDialog?.window?.attributes = attributes
        mDialog?.show()
    }

    interface InputTextListener {
        fun enter(text: String?)
    }
}