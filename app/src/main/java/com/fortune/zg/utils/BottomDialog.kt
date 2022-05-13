package com.fortune.zg.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapter
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.BottomDialog.shareLive
import com.fortune.zg.utils.BottomDialog.shareMV
import com.fortune.zg.utils.BottomDialog.showSelectDialog
import com.fortune.zg.utils.BottomDialog.showShare
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.item_bottom_select.view.*
import kotlinx.android.synthetic.main.layout_bottom_select.view.*
import kotlinx.android.synthetic.main.layout_share.view.*
import java.util.concurrent.TimeUnit

/**
 * 底部弹出栏
 * @sample showSelectDialog 选择展示框,比如性别之类
 * @sample showShare 展示正常分享
 * @sample shareMV 分享视频
 * @sample shareLive 分享直播间
 */
object BottomDialog {

    private var dialog: Dialog? = null

    private fun dismiss() {
        try {
            if (dialog != null && dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        } catch (e: Exception) {

        } finally {
            dialog = null
        }
    }

    /**
     * 展示选择框
     */
    fun showSelectDialog(
        context: Context,
        resourceId: Int,
        listener: OnBottomDialog4MustListener
    ) {
        dialog = Dialog(context, R.style.BottomDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_bottom_select, null) as LinearLayout

        val data = context.resources.getStringArray(resourceId).toList()

        val adapter = BaseAdapter.Builder<String>()
            .setData(data)
            .setLayoutId(R.layout.item_bottom_select)
            .addBindView { itemView, itemData ->
                itemView.tv_item_bottom_select.text = itemData
                itemView.tv_item_bottom_select.setOnClickListener {
                    listener.select(itemData)
                    listener.index(data.indexOf(itemData))
                    dismiss()
                }
            }.create()

        root.cv_bottom_select.adapter = adapter
        root.cv_bottom_select.layoutManager = SafeLinearLayoutManager(context)

        dialog?.setContentView(root)
        val dialogWindow = dialog?.window!!
        dialogWindow.setGravity(Gravity.BOTTOM)
        val attributes = dialogWindow.attributes!!
        attributes.x = 0
        attributes.y = 0
        attributes.width = context.resources.displayMetrics.widthPixels
        attributes.alpha = 9f
        dialogWindow.attributes = attributes
        dialog?.show()
    }

    /**
     * 底部分享选择器
     * @param needTips 是否需要提示信息
     * @param needSpaceAndMoment 是否需要分享到朋友圈和qq控件
     */
    @SuppressLint("CheckResult")
    fun showShare(
        context: Context,
        url: String,
        title: String,
        tags: String,
        needTips: Boolean = true,
        needSpaceAndMoment: Boolean = true
    ) {
        dialog = Dialog(context, R.style.BottomDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_share, null) as LinearLayout
        if (!needSpaceAndMoment) {
            root.ll_share_moment.visibility = View.GONE
            root.ll_share_space.visibility = View.GONE
        }

        RxView.clicks(root.ll_share_wechat)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
                if (needTips) {
                    MyApp.getInstance().shareToWechat(
                        url,
                        "${context.getString(R.string.i_paly_now)}《$title》",
                        "$tags${context.getString(R.string.to_paly_with)}",
                        false
                    )
                } else {
                    MyApp.getInstance().shareToWechat(
                        url,
                        title,
                        tags,
                        false
                    )
                }
            }
        RxView.clicks(root.ll_share_moment)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
                if (needTips) {
                    MyApp.getInstance().shareToWechat(
                        url,
                        "${context.getString(R.string.i_paly_now)}《$title》",
                        "$tags${context.getString(R.string.to_paly_with)}",
                        true
                    )
                } else {
                    MyApp.getInstance().shareToWechat(
                        url,
                        title,
                        tags,
                        false
                    )
                }
            }
        RxView.clicks(root.ll_share_qq)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
                if (needTips) {
                    MyApp.getInstance().shareToQQ(
                        url,
                        "${context.getString(R.string.i_paly_now)}《$title》",
                        "$tags${context.getString(R.string.to_paly_with)}",
                        context as Activity,
                        false
                    )
                } else {
                    MyApp.getInstance().shareToQQ(
                        url,
                        title,
                        tags,
                        context as Activity,
                        false
                    )
                }
            }
        RxView.clicks(root.ll_share_space)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
                if (needTips) {
                    MyApp.getInstance().shareToQQ(
                        url,
                        "${context.getString(R.string.i_paly_now)}《$title》",
                        "$tags${context.getString(R.string.to_paly_with)}",
                        context as Activity,
                        true
                    )
                } else {
                    MyApp.getInstance().shareToQQ(
                        url,
                        title,
                        tags,
                        context as Activity,
                        true
                    )
                }
            }
        RxView.clicks(root.tv_share_cancel)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
            }

        dialog?.setContentView(root)
        val dialogWindow = dialog?.window!!
        dialogWindow.setGravity(Gravity.BOTTOM)
        val attributes = dialogWindow.attributes!!
        attributes.x = 0
        attributes.y = 0
        attributes.width = context.resources.displayMetrics.widthPixels
        attributes.alpha = 9f
        dialogWindow.attributes = attributes
        dialog?.show()
    }

    /**
     * 分享视频
     * @param url 传值视频id
     * @param cover 封面图
     */
    @SuppressLint("CheckResult")
    fun shareMV(
        context: Context,
        url: String,
        title: String,
        des: String,
        cover: String,
        isClickItem: IsClickItem
    ) {
        dialog = Dialog(context, R.style.BottomDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_share, null) as LinearLayout
        root.ll_share_moment.visibility = View.GONE
        root.ll_share_space.visibility = View.GONE
        RxView.clicks(root.ll_share_wechat)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                isClickItem.isClickItem()
                dismiss()
                MyApp.getInstance().shareMVtoWechat(url, title, des, cover)
            }

        RxView.clicks(root.ll_share_qq)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                isClickItem.isClickItem()
                dismiss()
                MyApp.getInstance()
                    .shareMVtoQQ(context as Activity, url, title, des, cover)
            }

        RxView.clicks(root.tv_share_cancel)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
            }
        dialog?.setContentView(root)
        val dialogWindow = dialog?.window!!
        dialogWindow.setGravity(Gravity.BOTTOM)
        val attributes = dialogWindow.attributes!!
        attributes.x = 0
        attributes.y = 0
        attributes.width = context.resources.displayMetrics.widthPixels
        attributes.alpha = 9f
        dialogWindow.attributes = attributes
        dialog?.show()
    }

    interface IsClickItem {
        fun isClickItem()
    }

    /**
     * 分享视频
     * @param cover 封面图
     */
    @SuppressLint("CheckResult")
    fun shareLive(context: Context, url: String, title: String, liver: String, cover: String) {
        dialog = Dialog(context, R.style.BottomDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_share, null) as LinearLayout
        root.ll_share_moment.visibility = View.GONE
        root.ll_share_space.visibility = View.GONE
        RxView.clicks(root.ll_share_wechat)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
                MyApp.getInstance().shareLiveToWechat(url, title, liver, cover)
            }

        RxView.clicks(root.ll_share_qq)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
                MyApp.getInstance()
                    .shareLiveToQQ(context as Activity, url, title, liver, cover)
            }
        RxView.clicks(root.tv_share_cancel)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
            }
        dialog?.setContentView(root)
        val dialogWindow = dialog?.window!!
        dialogWindow.setGravity(Gravity.BOTTOM)
        val attributes = dialogWindow.attributes!!
        attributes.x = 0
        attributes.y = 0
        attributes.width = context.resources.displayMetrics.widthPixels
        attributes.alpha = 9f
        dialogWindow.attributes = attributes
        dialog?.show()
    }

    interface OnBottomDialog4MustListener {
        fun index(index: Int)
        fun select(data: String)
    }

}