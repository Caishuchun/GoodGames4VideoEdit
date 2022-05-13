package com.fortune.zg.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import com.fortune.zg.R
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.dialog_level_update.*
import java.util.concurrent.TimeUnit

/**
 * 等级升级
 */
object LevelUpdateDialog {

    private var mDialog: Dialog? = null

    /**
     * 干掉框框
     */
    fun dismissDialog() {
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
     * 是否正在显示
     */
    fun isShowing() = mDialog?.isShowing == true

    /**
     * 显示
     */
    @SuppressLint("SetTextI18n")
    fun show(
        context: Context,
        level: Int,
        money: Int,
        listener: OnLevelUpdateListener
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = Dialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.dialog_level_update)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)

        mDialog?.tv_dialog_level?.let {
            it.text = "V$level"
        }
        mDialog?.tv_dialog_money?.let {
            it.text = "+$money"
        }
        mDialog?.tv_dialog_normalGet?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    listener.normalGet()
                }
        }
        mDialog?.tv_dialog_doubleGet?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    listener.doubleGet()
                }
        }

        mDialog?.show()
    }

    interface OnLevelUpdateListener {
        fun normalGet()
        fun doubleGet()
    }
}