package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Handler
import android.os.Message
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.activity.IssueMvActivity
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.ToastUtils
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.fortune.zg.widget.SafeStaggeredGridLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.item_pop.view.*
import kotlinx.android.synthetic.main.item_select_material.view.*
import kotlinx.android.synthetic.main.item_select_material_selected.view.*
import kotlinx.android.synthetic.main.layout_pop.view.*
import kotlinx.android.synthetic.main.layout_select_material.*
import java.io.File
import java.io.Serializable
import java.util.concurrent.TimeUnit

/**
 * 视频/图片资源选择器
 */
object SelectMaterialUtil {
    private var mDialog: Dialog? = null

    //当前选项 1全部,2视频,3图片
    private var currentType = 1

    //手机中数据的适配器相关
    private var mAdapter: BaseAdapterWithPosition<MediaData>? = null
    private var mData = mutableListOf<MediaData>()

    //手机中的数据
    private var all = mutableListOf<MediaData>()
    private var videos = mutableListOf<MediaData>()
    private var images = mutableListOf<MediaData>()

    //选中的文件适配器相关
    private var selected = mutableListOf<MediaData>()
    private var mAdapter4Selected: BaseAdapterWithPosition<MediaData>? = null

    //目录是否显示
    private var isShowFolder = false

    //目录选择使用popupWindow
    private var popupWindow: PopupWindow? = null

    //目录选择适配器相关
    private var folderData = mutableListOf<FolderBean>()
    private var mAdapter4Folder: BaseAdapterWithPosition<FolderBean>? = null

    //目录下所有数据
    private var all4Folder = mutableListOf<MediaData>()
    private var videos4Folder = mutableListOf<MediaData>()
    private var images4Folder = mutableListOf<MediaData>()

    //选择文件最大数
    private const val MAX_NUM = 10

    //可不可以同时选择视频和照片
    private const val CAN_SELECT_IMG_AND_VIDEO = false

    //第一个选项是不是图片
    private var firstSelectTypeIsImg: Boolean? = null

    private var isGetImagesOver = false
    private var isGetVideosOver = false
    private var isRunning = false

    /**
     * 获取提示信息
     */
    private fun getTips() =
        if (CAN_SELECT_IMG_AND_VIDEO) {
            "可同时选择视频和照片"
        } else {
            "不可同时选择视频和照片"
        }

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

    interface CancelCallBack {
        fun cancel()
    }

    @SuppressLint("CheckResult")
    fun show(context: AppCompatActivity, callback: CancelCallBack?) {
        isGetImagesOver = false
        isGetVideosOver = false
        isRunning = false
        val folder = context.getExternalFilesDir("pic2video").toString()
        val file = File(folder)
        if (file.exists() && file.isDirectory) {
            val listFiles = file.listFiles()
            for (deleteFile in listFiles) {
                deleteFile.delete()
            }
        }
        currentType = 1
        images.clear()
        videos.clear()
        all.clear()
        selected.clear()
        mData.clear()
        folderData.clear()
        all4Folder.clear()
        videos4Folder.clear()
        images4Folder.clear()
        isShowFolder = false

        mDialog = Dialog(context, R.style.BottomDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_select_material, null) as LinearLayout
        mDialog?.setContentView(root)
        mDialog?.tv_selectMaterial_tips?.text = getTips()

        RxView.clicks(mDialog?.iv_selectMaterial_cancel!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
            }

        mDialog?.setOnDismissListener {
            mHandler.removeMessages(0)
            callback?.cancel()
        }

        initRecycleView(context)
        initRecycleView4Selected(context)
        initPopupWindow(context)
        changeType(currentType)

        RxView.clicks(mDialog?.rl_selectMaterial_all!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentType != 1) {
                    currentType = 1
                    changeType(currentType)
                }
            }
        RxView.clicks(mDialog?.rl_selectMaterial_video!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentType != 2) {
                    currentType = 2
                    changeType(currentType)
                }
            }
        RxView.clicks(mDialog?.rl_selectMaterial_picture!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentType != 3) {
                    currentType = 3
                    changeType(currentType)
                }
            }

        RxView.clicks(mDialog?.tv_selectMaterial_title!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeTitleFlag(context)
            }
        RxView.clicks(mDialog?.iv_selectMaterial_flag!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                changeTitleFlag(context)
            }
        RxView.clicks(mDialog?.tv_selectMaterial_next!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val intent = Intent(context, VideoMainActivity::class.java)
                intent.putExtra(VideoMainActivity.SELECTED, selected as Serializable)
                context.startActivity(intent)
            }
        RxView.clicks(mDialog?.tv_selectMaterial_next2!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val intent = Intent(context,IssueMvActivity::class.java)
                intent.putExtra(IssueMvActivity.FROM_HIS, false)
                intent.putExtra(IssueMvActivity.VIDEO_PATH, selected[0].filePath)
                intent.putExtra(IssueMvActivity.VIDEO_COVER_PAHT, "")
                context.startActivity(intent)
            }

        //确定大小位置
        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val attributes = mDialog?.window?.attributes
        attributes?.width = displayMetrics.widthPixels
        attributes?.height = displayMetrics.heightPixels
        attributes?.gravity = Gravity.BOTTOM
        mDialog?.window?.attributes = attributes
        mDialog?.show()

        toGetInfo(context)
    }

    /**
     * 初始化popupWindow
     */
    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initPopupWindow(context: AppCompatActivity) {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_pop, null)
        popupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        mAdapter4Folder = BaseAdapterWithPosition.Builder<FolderBean>()
            .setData(folderData)
            .setLayoutId(R.layout.item_pop)
            .addBindView { itemView, itemData, position ->
                Glide.with(context)
                    .load(itemData.cover)
                    .placeholder(R.mipmap.bg_gray_6)
                    .into(itemView.iv_item_pop_pic)
                itemView.tv_item_pop_folder.text = itemData.folderName
                itemView.tv_item_pop_num.text = "${itemData.number}"

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        mDialog?.tv_selectMaterial_title?.text = itemData.folderName
                        popupWindow?.dismiss()
                        all4Folder.clear()
                        videos4Folder.clear()
                        images4Folder.clear()
                        if (itemData.folderName == "所有照片") {
                            all4Folder.addAll(all)
                            videos4Folder.addAll(videos)
                            images4Folder.addAll(images)
                        } else {
                            for (data in all) {
                                if (data.albumName == itemData.folderName) {
                                    all4Folder.add(data)
                                }
                            }
                            for (data in videos) {
                                if (data.albumName == itemData.folderName) {
                                    videos4Folder.add(data)
                                }
                            }
                            for (data in images) {
                                if (data.albumName == itemData.folderName) {
                                    images4Folder.add(data)
                                }
                            }
                        }
                        mData.clear()
                        mData.addAll(
                            when (currentType) {
                                1 -> {
                                    all4Folder
                                }
                                2 -> {
                                    videos4Folder
                                }
                                3 -> {
                                    images4Folder
                                }
                                else -> {
                                    all4Folder
                                }
                            }
                        )
                        mAdapter?.notifyDataSetChanged()
                    }
            }
            .create()
        view.rv_pop.adapter = mAdapter4Folder
        view.rv_pop.layoutManager = SafeLinearLayoutManager(context)

        popupWindow?.isFocusable = true
        popupWindow?.isOutsideTouchable = false
        popupWindow?.animationStyle = R.style.style_pop_animation
        popupWindow?.setOnDismissListener {
            if (isShowFolder) {
                val flag = mDialog!!.iv_selectMaterial_flag
                val up2Down = AnimationUtils.loadAnimation(context, R.anim.up_down)
                val linearInterpolator = LinearInterpolator()
                up2Down.interpolator = linearInterpolator
                flag.startAnimation(up2Down)
                isShowFolder = !isShowFolder
            }
        }
    }

    /**
     * 改变title上的上下图标
     */
    private fun changeTitleFlag(context: AppCompatActivity) {
        val flag = mDialog!!.iv_selectMaterial_flag
        if (isShowFolder) {
            //正在展示的话,则缩回去
            val up2Down = AnimationUtils.loadAnimation(context, R.anim.up_down)
            val linearInterpolator = LinearInterpolator()
            up2Down.interpolator = linearInterpolator
            flag.startAnimation(up2Down)
            popupWindow?.dismiss()
        } else {
            //展开目录列表
            val down2Up = AnimationUtils.loadAnimation(context, R.anim.down_up)
            val linearInterpolator = LinearInterpolator()
            down2Up.interpolator = linearInterpolator
            flag.startAnimation(down2Up)
            popupWindow?.showAsDropDown(mDialog!!.tv_selectMaterial_title)
        }
        isShowFolder = !isShowFolder
    }

    /**
     * 初始化选中数据recycleView
     */
    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initRecycleView4Selected(context: AppCompatActivity) {
        mAdapter4Selected = BaseAdapterWithPosition.Builder<MediaData>()
            .setData(selected)
            .setLayoutId(R.layout.item_select_material_selected)
            .addBindView { itemView, itemData, position ->
                if (itemData.isVideo()) {
                    Glide.with(context)
                        .load(itemData.thumbNailPath!!)
                        .placeholder(R.mipmap.bg_gray_6)
                        .into(itemView.iv_item_selected_picture)
                    itemView.tv_item_selected_duration.text = itemData.formatDuration()
                } else {
                    Glide.with(context)
                        .load(itemData.filePath!!)
                        .placeholder(R.mipmap.bg_gray_6)
                        .into(itemView.iv_item_selected_picture)
                    itemView.tv_item_selected_duration.text = ""
                }

                RxView.clicks(itemView.iv_item_selected_delete)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        for (data in mData) {
                            if (data == itemData) {
                                data.isSelected = !itemData.isSelected
                            }
                        }
                        when (currentType) {
                            1 -> {
                                all4Folder.clear()
                                all4Folder.addAll(mData)
                            }
                            2 -> {
                                videos4Folder.clear()
                                videos4Folder.addAll(mData)
                            }
                            3 -> {
                                images4Folder.clear()
                                images4Folder.addAll(mData)
                            }
                        }
                        selected.remove(itemData)
                        mAdapter4Selected?.notifyDataSetChanged()
                        mAdapter?.notifyDataSetChanged()

                        if (selected.isEmpty()) {
                            mDialog?.tv_selectMaterial_next!!.visibility = View.GONE
                            mDialog?.tv_selectMaterial_tips!!.text = getTips()
                            mDialog?.tv_selectMaterial_next2!!.visibility = View.GONE
                        } else {
                            mDialog?.tv_selectMaterial_next!!.visibility = View.VISIBLE
                            mDialog?.tv_selectMaterial_tips!!.text = getTotalDuration()
                            if (selected.size == 1) {
                                if (selected[0].isVideo()) {
                                    mDialog?.tv_selectMaterial_next2!!.visibility = View.VISIBLE
                                } else {
                                    mDialog?.tv_selectMaterial_next2!!.visibility = View.GONE
                                }
                            } else {
                                mDialog?.tv_selectMaterial_next2!!.visibility = View.GONE
                            }
                        }
                    }
            }
            .create()

        mDialog?.rv_selectMaterial_selected?.adapter = mAdapter4Selected
        mDialog?.rv_selectMaterial_selected?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }

    /**
     * 获取数据
     */
    private fun toGetInfo(context: AppCompatActivity) {
        ImageScanHelper.start(context, @SuppressLint("HandlerLeak")
        object : Handler() {
            @SuppressLint("NotifyDataSetChanged")
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.what != -1) {
                    val image = msg.obj as MediaData
                    if (image.albumName != "temp") {
                        LogUtils.d("===${image.filePath}")
                        images.add(image)
                        all.add(image)
                    }
                    images.sortByDescending { it.createTime }
                    all.sortByDescending { it.createTime }
                    mData.clear()
                    mData.addAll(all)
                    if (!isRunning) {
                        isRunning = true
                        mHandler.sendEmptyMessage(0)
                    }
                } else {
                    isGetImagesOver = true
                }
            }
        })
        VideoScanHelper.start(context, @SuppressLint("HandlerLeak")
        object : Handler() {
            @SuppressLint("NotifyDataSetChanged")
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.what != -1) {
                    val video = msg.obj as MediaData
                    videos.add(video)
                    videos.sortByDescending { it.createTime }
                    all.add(video)
                    all.sortByDescending { it.createTime }
                    mData.clear()
                    mData.addAll(all)
                    if (!isRunning) {
                        isRunning = true
                        mHandler.sendEmptyMessage(0)
                    }
                } else {
                    isGetVideosOver = true
                }
            }
        })
    }

    @SuppressLint("HandlerLeak", "NotifyDataSetChanged")
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            mAdapter?.notifyDataSetChanged()
            mAdapter4Selected?.notifyDataSetChanged()
            toSetFolderMap()
            if (isGetImagesOver && isGetVideosOver) {

            } else {
                sendEmptyMessageDelayed(0, 500)
            }
        }
    }

    /**
     * 填充文件夹数据
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun toSetFolderMap() {
        folderData.clear()
        all4Folder.clear()
        videos4Folder.clear()
        images4Folder.clear()
        all4Folder.addAll(all)
        videos4Folder.addAll(videos)
        images4Folder.addAll(images)
        val temp = mutableMapOf<String, Int>()
        for (data in all) {
            val albumName = data.albumName!!
            if (temp.containsKey(albumName)) {
                temp[albumName] = temp[albumName]!! + 1
            } else {
                temp[albumName] = 1
            }
        }
        folderData.add(FolderBean("所有照片", all.size, all[0].thumbNailPath ?: all[0].filePath!!))
        for (key in temp.keys) {
            var cover = ""
            for (data in all) {
                if (key == data.albumName) {
                    cover = if (data.isVideo()) data.thumbNailPath!! else data.filePath!!
                    break
                }
            }
            val folderBean = FolderBean(key, temp[key]!!, cover)
            folderData.add(folderBean)
        }
        mAdapter4Folder?.notifyDataSetChanged()
    }

    /**
     * 初始化recycleView
     */
    @SuppressLint("CheckResult", "NotifyDataSetChanged", "SetTextI18n")
    private fun initRecycleView(context: AppCompatActivity) {
        mAdapter = BaseAdapterWithPosition.Builder<MediaData>()
            .setData(mData)
            .setLayoutId(R.layout.item_select_material)
            .addBindView { itemView, itemData, position ->
                if (itemData.isVideo()) {
                    Glide.with(context)
                        .load(itemData.thumbNailPath!!)
                        .placeholder(R.mipmap.bg_gray_6)
                        .into(itemView.iv_item_selectMaterial_picture)
                    itemView.tv_item_selectMaterial_duration.text = itemData.formatDuration()
                } else {
                    Glide.with(context)
                        .load(itemData.filePath!!)
                        .placeholder(R.mipmap.bg_gray_6)
                        .into(itemView.iv_item_selectMaterial_picture)
                    itemView.tv_item_selectMaterial_duration.text = ""
                }

                if (itemData.isSelected) {
                    itemView.tv_item_selectMaterial_num.setBackgroundResource(R.drawable.bg_select_num_in)
                } else {
                    itemView.tv_item_selectMaterial_num.setBackgroundResource(R.drawable.bg_select_num_un)
                }
                if (selected.contains(itemData)) {
                    itemView.tv_item_selectMaterial_num.text = "${selected.indexOf(itemData) + 1}"
                }

                RxView.clicks(itemView.rl_item_selectMaterial_num)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.isSelected) {
                            itemView.tv_item_selectMaterial_num.setBackgroundResource(R.drawable.bg_select_num_un)
                            selected.remove(itemData)
                            for (data in mData) {
                                if (data == itemData) {
                                    data.isSelected = !itemData.isSelected
                                }
                            }
                            if (selected.isEmpty()) {
                                firstSelectTypeIsImg = null
                            }
                        } else {
                            if (selected.size >= MAX_NUM) {
                                ToastUtils.show("最多只能选择10个文件")
                            } else {
                                if (selected.isEmpty()) {
                                    //没有数据
                                    firstSelectTypeIsImg = itemData.isImage()
                                } else {
                                    if (firstSelectTypeIsImg != itemData.isImage()) {
                                        ToastUtils.show(getTips())
                                        return@subscribe
                                    }
                                }
                                itemView.tv_item_selectMaterial_num.setBackgroundResource(R.drawable.bg_select_num_un)
                                selected.add(itemData)
                                mDialog?.rv_selectMaterial_selected!!.smoothScrollToPosition(
                                    selected.size - 1
                                )
                                for (data in mData) {
                                    if (data == itemData) {
                                        data.isSelected = !itemData.isSelected
                                    }
                                }
                            }
                        }
                        when (currentType) {
                            1 -> {
                                all4Folder.clear()
                                all4Folder.addAll(mData)
                            }
                            2 -> {
                                videos4Folder.clear()
                                videos4Folder.addAll(mData)
                            }
                            3 -> {
                                images4Folder.clear()
                                images4Folder.addAll(mData)
                            }
                        }
                        mAdapter?.notifyDataSetChanged()
                        mAdapter4Selected?.notifyDataSetChanged()

                        if (selected.isEmpty()) {
                            mDialog?.tv_selectMaterial_next!!.visibility = View.GONE
                            mDialog?.tv_selectMaterial_tips!!.text = getTips()
                            mDialog?.tv_selectMaterial_next2!!.visibility = View.GONE
                        } else {
                            mDialog?.tv_selectMaterial_next!!.visibility = View.VISIBLE
                            mDialog?.tv_selectMaterial_tips!!.text = getTotalDuration()
                            if (selected.size == 1) {
                                if (selected[0].isVideo()) {
                                    mDialog?.tv_selectMaterial_next2!!.visibility = View.VISIBLE
                                } else {
                                    mDialog?.tv_selectMaterial_next2!!.visibility = View.GONE
                                }
                            } else {
                                mDialog?.tv_selectMaterial_next2!!.visibility = View.GONE
                            }
                        }
                    }
                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val intent = Intent(context, PreviewActivity::class.java)
                        intent.putExtra(PreviewActivity.IS_VIDEO, itemData.isVideo())
                        intent.putExtra(PreviewActivity.FILE_PATH, itemData.filePath)
                        intent.putExtra(PreviewActivity.VIDEO_COVER, itemData.thumbNailPath)
                        context.startActivity(intent)
                    }
            }
            .create()

        mDialog?.rv_selectMaterial?.adapter = mAdapter
        mDialog?.rv_selectMaterial?.layoutManager =
            SafeStaggeredGridLayoutManager(4, OrientationHelper.VERTICAL)
    }

    /**
     * 获取选择后视频总时长
     */
    private fun getTotalDuration(): String {
        var total = 0L
        for (select in selected) {
            if (select.isVideo()) {
                total += select.duration!!
            } else {
                total += 3 * 1000
            }
        }
        return total.let {
            val totalSeconds = it / 1000
            val hour = totalSeconds / 3600

            val totalMinutes = totalSeconds % 3600
            val minute = totalMinutes / 60
            val second = totalMinutes % 60

            hour.toString().run {
                return@run if (this.length < 2) "0".plus(this) else this
            }.plus(":").plus(
                minute.toString().run {
                    return@run if (this.length < 2) "0".plus(this) else this
                }
            ).plus(":").plus(
                second.toString().run {
                    return@run if (this.length < 2) "0".plus(this) else this
                }
            )
        }
    }

    /**
     * 修改类型
     * @param index 1全部,2视频,3图片
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun changeType(index: Int) {
        mDialog?.tv_selectMaterial_all!!.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
        mDialog?.view_selectMaterial_all!!.visibility = View.GONE
        mDialog?.tv_selectMaterial_video!!.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
        mDialog?.view_selectMaterial_video!!.visibility = View.GONE
        mDialog?.tv_selectMaterial_picture!!.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
        mDialog?.view_selectMaterial_picture!!.visibility = View.GONE
        when (index) {
            1 -> {
                mDialog?.tv_selectMaterial_all!!.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                mDialog?.view_selectMaterial_all!!.visibility = View.VISIBLE
                mData.clear()
                mData.addAll(all4Folder)
                mAdapter?.notifyDataSetChanged()
                mAdapter4Selected?.notifyDataSetChanged()
            }
            2 -> {
                mDialog?.tv_selectMaterial_video!!.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                mDialog?.view_selectMaterial_video!!.visibility = View.VISIBLE
                mData.clear()
                mData.addAll(videos4Folder)
                mAdapter?.notifyDataSetChanged()
                mAdapter4Selected?.notifyDataSetChanged()
            }
            3 -> {
                mDialog?.tv_selectMaterial_picture!!.setTypeface(
                    Typeface.DEFAULT_BOLD,
                    Typeface.BOLD
                )
                mDialog?.view_selectMaterial_picture!!.visibility = View.VISIBLE
                mData.clear()
                mData.addAll(images4Folder)
                mAdapter?.notifyDataSetChanged()
                mAdapter4Selected?.notifyDataSetChanged()
            }
        }
    }
}