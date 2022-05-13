package com.fortune.zg.utils

import android.app.Dialog
import android.content.Context
import com.fortune.zg.R
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.dialog_get_success_all.*
import kotlinx.android.synthetic.main.dialog_get_success_experience.*
import kotlinx.android.synthetic.main.dialog_get_success_integral.*

/**
 * 积分和经验领取成功Dialog
 */
object GetSuccessDialog {
    private var mDialog: Dialog? = null

    /**
     * 显示积分Dialog
     */
    fun showIntegralDialog(context: Context, integral: Int, listener: OnCancelListener) {
        showDialog(context, TYPE.ONLY_INTEGRAL, integral, 0, listener)
    }

    /**
     * 显示经验Dialog
     */
    fun showExperienceDialog(context: Context, experience: Int, listener: OnCancelListener) {
        showDialog(context, TYPE.ONLY_EXPERIENCE, 0, experience, listener)
    }

    /**
     * 显示经验+积分Dialog
     */
    fun showDialog(context: Context, integral: Int, experience: Int, listener: OnCancelListener) {
        showDialog(context, TYPE.ALL, integral, experience, listener)
    }

    /**
     * 显示Dialog
     * @param type 仅积分,仅经验,都有三种
     * @param integral 积分,有的话显示
     * @param experience 经验,有的话显示
     */
    private fun showDialog(
        context: Context,
        type: TYPE,
        integral: Int,
        experience: Int,
        listener: OnCancelListener
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = Dialog(context, R.style.new_circle_progress)
        when (type) {
            //仅仅只有积分
            TYPE.ONLY_INTEGRAL -> {
                mDialog?.setContentView(R.layout.dialog_get_success_integral)
                mDialog?.tv_dialog_get_success_integral?.text =
                    context.getString(R.string.get_integral).replace("X", integral.toString())
                mDialog?.root_dialog_get_success_integral?.let {
                    RxView.clicks(it)
                        .subscribe {
                            listener.setOnCancel()
                            dismissLoading()
                        }
                }
            }
            //仅仅只有经验
            TYPE.ONLY_EXPERIENCE -> {
                mDialog?.setContentView(R.layout.dialog_get_success_experience)
                mDialog?.tv_dialog_get_success_experience?.text =
                    context.getString(R.string.get_experience).replace("X", experience.toString())
                mDialog?.root_dialog_get_success_experience?.let {
                    RxView.clicks(it)
                        .subscribe {
                            listener.setOnCancel()
                            dismissLoading()
                        }
                }
            }
            TYPE.ALL -> {
                mDialog?.setContentView(R.layout.dialog_get_success_all)
                mDialog?.tv_dialog_get_success_all_experience?.text =
                    context.getString(R.string.get_experience).replace("X", experience.toString())
                mDialog?.tv_dialog_get_success_all_integral?.text =
                    context.getString(R.string.get_integral).replace("X", integral.toString())
                mDialog?.root_dialog_get_success_all?.let {
                    RxView.clicks(it)
                        .subscribe {
                            listener.setOnCancel()
                            dismissLoading()
                        }
                }
            }
        }
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)
        mDialog?.setOnCancelListener {
            listener.setOnCancel()
            dismissLoading()
        }
        mDialog?.show()
    }


    /**
     * 取消加载框
     */
    private fun dismissLoading() {
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
     * 枚举,显示类型
     */
    enum class TYPE {
        ONLY_INTEGRAL,//单独只有积分
        ONLY_EXPERIENCE,//单独只有经验
        ALL,//都有
    }

    /**
     * 取消监听,方便弹框取消之后的后续操作
     */
    interface OnCancelListener {
        fun setOnCancel()
    }

}