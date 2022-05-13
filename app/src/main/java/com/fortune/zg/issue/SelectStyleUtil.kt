package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.OrientationHelper
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.item_style.view.*
import kotlinx.android.synthetic.main.layout_select_style.*
import java.util.concurrent.TimeUnit

object SelectStyleUtil {
    private var mDialog: Dialog? = null
    private var mAdapter: BaseAdapterWithPosition<String>? = null
    private var mData = mutableListOf<String>()

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
     * 展示风格选择样式
     */
    @SuppressLint("CheckResult")
    fun showStyle(context: AppCompatActivity, filePath: String) {
        mDialog = Dialog(context, R.style.BeautifulDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_select_style, null) as LinearLayout
        mDialog?.setContentView(root)

        initRecyclerView(context)
        toCheckItem(context)

        RxView.clicks(mDialog?.iv_selectStyle_back!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
            }

        RxView.clicks(mDialog?.iv_selectStyle_right!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {

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
     * 切换
     */
    @SuppressLint("CheckResult")
    private fun toCheckItem(context: AppCompatActivity) {
        RxView.clicks(mDialog?.tv_selectStyle_recommend!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItem(context, 0)
            }
        RxView.clicks(mDialog?.tv_selectStyle_normal!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItem(context, 1)
            }
        RxView.clicks(mDialog?.tv_selectStyle_lyric!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItem(context, 2)
            }
        RxView.clicks(mDialog?.tv_selectStyle_cool!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItem(context, 3)
            }
        RxView.clicks(mDialog?.tv_selectStyle_lovely!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeItem(context, 4)
            }

    }

    /**
     * @param type 0推荐,1日常,2抒情,3酷炫,4可爱
     */
    private fun changeItem(context: AppCompatActivity, type: Int) {
        mDialog?.tv_selectStyle_recommend?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        mDialog?.tv_selectStyle_normal?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        mDialog?.tv_selectStyle_lyric?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        mDialog?.tv_selectStyle_cool?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        mDialog?.tv_selectStyle_lovely?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        when (type) {
            0 -> {
                mDialog?.tv_selectStyle_recommend?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
            }
            1 -> {
                mDialog?.tv_selectStyle_normal?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
            }
            2 -> {
                mDialog?.tv_selectStyle_lyric?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
            }
            3 -> {
                mDialog?.tv_selectStyle_cool?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
            }
            4 -> {
                mDialog?.tv_selectStyle_lovely?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
            }
        }
    }

    private fun initRecyclerView(context: AppCompatActivity) {
        mData.add("12")
        mData.add("12")
        mData.add("12")
        mData.add("12")
        mData.add("12")
        mAdapter = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.item_style)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                itemView.tv_item_style_name.text = itemData
                when (position) {
                    0 -> {
                        itemView.iv_item_style_pic.setBackgroundColor(Color.RED)
                    }
                    1 -> {
                        itemView.iv_item_style_pic.setBackgroundColor(Color.GREEN)
                    }
                    2 -> {
                        itemView.iv_item_style_pic.setBackgroundColor(Color.WHITE)
                    }
                    3 -> {
                        itemView.iv_item_style_pic.setBackgroundColor(Color.BLUE)
                    }
                    4 -> {
                        itemView.iv_item_style_pic.setBackgroundColor(Color.MAGENTA)
                    }
                }
            }
            .create()

        mDialog?.rv_selectStyle?.adapter = mAdapter
        mDialog?.rv_selectStyle?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }

}