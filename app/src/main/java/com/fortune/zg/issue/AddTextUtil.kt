package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.utils.InputTextDialog
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.ToastUtils
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.item_cut_pic.view.*
import kotlinx.android.synthetic.main.item_video_cut_pic.view.*
import kotlinx.android.synthetic.main.layout_select_text.*
import java.util.concurrent.TimeUnit

object AddTextUtil {
    private var mDialog: Dialog? = null
    private var color = -1
    private var type = Typeface.DEFAULT_BOLD
    private var currentEditText: ScaleEditText? = null
    private var editTextList = mutableListOf<ScaleEditText>()
    private var oldEditTextList = mutableListOf<ScaleEditText>()

    private var tempData = mutableListOf<MediaData>()
    private var mAdapter4OnlyPic: BaseAdapterWithPosition<MediaData>? = null
    private var mAdapter4Pic: BaseAdapterWithPosition<Bitmap>? = null
    private var mDrawView: DragView? = null

    private var picIndex = 0
    private var MAX_PIC = 10
    private var picBitmapLists = mutableListOf<Bitmap>()

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
     * 显示文字选择框
     */
    fun showText(
        context: AppCompatActivity,
        type: Int,
        data: MutableList<MediaData>,
        oldTextList: MutableList<ScaleEditText>?,
        drawView: DragView,
        listener: OnAddTextListener
    ) {
        mDrawView = drawView
        if (oldTextList != null) {
            oldEditTextList = oldTextList
            editTextList = oldTextList
        }
        mDialog = Dialog(context, R.style.BeautifulDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_select_text, null) as RelativeLayout
        mDialog?.setContentView(root)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)
        tempData = data

        when (type) {
            0 -> {
                mDialog?.rv_selectText_pic?.visibility = View.GONE
                mDialog?.cutVideoView_selectText?.visibility = View.GONE
                mDialog?.rv_selectText_onlyPic?.visibility = View.VISIBLE
                initRecyclerView(context, type)
            }
            1 -> {
                mDialog?.rv_selectText_pic?.visibility = View.VISIBLE
                mDialog?.cutVideoView_selectText?.visibility = View.GONE
                mDialog?.rv_selectText_onlyPic?.visibility = View.GONE
                initRecyclerView(context, type)
            }
            else -> {
            }
        }

        mDialog?.tv_selectText_add?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    addText(context,type)
                }
        }

        mDialog?.iv_selectText_back?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    editTextList.clear()
                    mDrawView?.removeAll()
                    listener.cancel(type)
                    dismiss()
                }
        }

        mDialog?.iv_selectText_right?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    listener.save(type, editTextList)
                    dismiss()
                }
        }

        mDialog?.iv_selectText_play?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    listener.play(type, editTextList)
                }
        }

        mDrawView?.setOnMovingListener(object : DragView.OnMovingListener {
            override fun moving() {
                LogUtils.d("++++++++++++++++moving")
                mDialog?.hide()
            }

            override fun moveEnd(isDelete: Boolean, index: Int) {
                LogUtils.d("++++++++++++++++moveEnd_$isDelete")
                mDialog?.show()
                currentEditText = editTextList[index]
                toSelectTextColor(context, currentEditText?.currentTextColor)
                toSelectTextFont(context, currentEditText?.typeface)
                LogUtils.d("============pages:${currentEditText?.getPages()}")
                val pages = currentEditText?.getPages()!!
                for (index in 0 until tempData.size) {
                    tempData[index].isSelected = !pages.contains(index)
                }
                LogUtils.d("============tempData:${Gson().toJson(tempData)}")
                mAdapter4OnlyPic?.notifyDataSetChanged()
            }

            override fun doubleClick(index: Int) {
                currentEditText = editTextList[index]
                InputTextDialog.showInput(context,
                    currentEditText?.text?.toString()?.trim(),
                    object : InputTextDialog.InputTextListener {
                        override fun enter(text: String?) {
                            LogUtils.d("++++++++++++++++text:$text")
                            if (text == null) {
                                currentEditText?.setText("")
                            } else {
                                currentEditText?.setText(text)
                            }
                        }
                    })
            }
        })

        //确定大小位置
        val dialogWindow = mDialog?.window!!
        dialogWindow.setGravity(Gravity.BOTTOM)
        dialogWindow.setWindowAnimations(R.style.dialog_anim)
        val attributes = dialogWindow.attributes!!
        attributes.x = 0
        attributes.y = 0
        attributes.width = context.resources.displayMetrics.widthPixels
        try {
            attributes.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dialogWindow.attributes = attributes
        mDialog?.show()
    }

    /**
     * 添加文字
     */
    private fun addText(context: AppCompatActivity,type: Int) {
        val et = ScaleEditText(context)
        currentEditText = et
        editTextList.add(et)
        mDrawView?.addDragView(
            et,
            500, mDrawView?.height!! / 2 - 100, 900, mDrawView?.height!! / 2 + 100, true, true
        )
        toSelectTextColor(context)
        toSelectTextFont(context)
        when(type){
            0->{
                currentEditText?.setPage(0)
                for (index in 0 until tempData.size) {
                    tempData[index].isSelected = index != 0
                }
                mAdapter4OnlyPic?.notifyDataSetChanged()
            }
            1->{
                //TODO 笑口常开好彩自然来
                currentEditText?.setInterval("0_0")
                mDialog?.cutVideoView_selectText?.visibility = View.VISIBLE
                mDialog?.cutVideoView_selectText?.setDuration(tempData[0].duration!!)
                mDialog?.cutVideoView_selectText?.setStart(0L)
                mDialog?.cutVideoView_selectText?.setEnd(tempData[0].duration!!)
                mDialog?.cutVideoView_selectText?.setType(CutVideoView.CutVideoType.CUT_VIDEO)
            }
        }
    }

    /**
     * 初始化RecyclerView
     */
    @SuppressLint("CheckResult")
    private fun initRecyclerView(
        context: AppCompatActivity,
        type: Int
    ) {
        when (type) {
            0 -> {
                mAdapter4OnlyPic = BaseAdapterWithPosition.Builder<MediaData>()
                    .setLayoutId(R.layout.item_cut_pic)
                    .setData(tempData)
                    .addBindView { itemView, itemData, position ->
                        Glide.with(context)
                            .load(itemData.filePath)
                            .into(itemView.iv_item_cutPic_pic)
                        //倒反天罡,这里isSelected初始就是选中的,所以,判断需要反着来
                        if (!itemData.isSelected) {
                            itemView.rl_item_cutPic_root.setBackgroundResource(R.drawable.bg_rectangle_red_line)
                        } else {
                            itemView.rl_item_cutPic_root.setBackgroundResource(R.drawable.bg_rectangle_green_line)
                        }
                        RxView.clicks(itemView.rootView)
                            .throttleFirst(200, TimeUnit.MILLISECONDS)
                            .subscribe {
                                var count = 0
                                for (info in tempData) {
                                    if (!info.isSelected) {
                                        count++
                                    }
                                }
                                if (!itemData.isSelected) {
                                    if (count <= 1) {
                                        //至少得留下一个作为承载页面
                                        ToastUtils.show("至少需要选择一张图片显示文字!")
                                    } else {
                                        currentEditText?.setPage(position)
                                        itemData.isSelected = !itemData.isSelected
                                        if (!itemData.isSelected) {
                                            itemView.rl_item_cutPic_root.setBackgroundResource(R.drawable.bg_rectangle_red_line)
                                        } else {
                                            itemView.rl_item_cutPic_root.setBackgroundResource(R.drawable.bg_rectangle_green_line)
                                        }
                                    }
                                } else {
                                    currentEditText?.setPage(position)
                                    itemData.isSelected = !itemData.isSelected
                                    if (!itemData.isSelected) {
                                        itemView.rl_item_cutPic_root.setBackgroundResource(R.drawable.bg_rectangle_red_line)
                                    } else {
                                        itemView.rl_item_cutPic_root.setBackgroundResource(R.drawable.bg_rectangle_green_line)
                                    }
                                }
                            }
                    }
                    .create()
                mDialog?.rv_selectText_onlyPic?.adapter = mAdapter4OnlyPic
                mDialog?.rv_selectText_onlyPic?.layoutManager =
                    SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
            }
            1 -> {
                mAdapter4Pic = BaseAdapterWithPosition.Builder<Bitmap>()
                    .setLayoutId(R.layout.item_video_cut_pic)
                    .setData(picBitmapLists)
                    .addBindView { itemView, itemData, position ->
                        Glide.with(context)
                            .load(itemData)
                            .into(itemView.iv_item_videoCut_pic)
                    }
                    .create()
                mDialog?.rv_selectText_pic?.adapter = mAdapter4Pic
                mDialog?.rv_selectText_pic?.layoutManager =
                    SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
                currentVideoIndex = 0
                toCycleGetPic(context)
            }
        }
    }

    /**
     * 循环获取所有视频的帧图片
     */
    private var currentVideoIndex = 0
    private fun toCycleGetPic(context: AppCompatActivity) {
        val mediaData = tempData[currentVideoIndex]
        val step = mediaData.duration!! / MAX_PIC
        toGetPic(context, mediaData, step, mediaData.duration!!)
    }

    /**
     * 获取视频帧图片
     */
    @SuppressLint("CheckResult")
    private fun toGetPic(
        context: AppCompatActivity,
        mediaData: MediaData,
        step: Long,
        duration: Long
    ) {
        val currentTime =
            if (step * picIndex > duration) duration * 1000 - 10 else step * picIndex * 1000
        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .frame(currentTime)
            .centerCrop()
        val target = object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (picIndex < MAX_PIC) {
                    picBitmapLists.add(resource)
                    mAdapter4Pic?.notifyItemChanged(picBitmapLists.size - 1)
                    picIndex++
                    toGetPic(context, mediaData, step, duration)
                } else {
                    picIndex = 0
                    if (currentVideoIndex < tempData.size - 1) {
                        currentVideoIndex++
                        toCycleGetPic(context)
                    } else {
                        currentVideoIndex = 0
                    }
                }
            }
        }
        Glide.with(context)
            .setDefaultRequestOptions(options)
            .asBitmap()
            .load(mediaData.filePath)
            .into(target)
    }

    /**
     * 选择字体颜色
     */
    private fun toSelectTextColor(context: AppCompatActivity, textColor: Int? = null) {
        mDialog?.rg_selectText_color?.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_selectText_red -> {
                    color = context.resources.getColor(R.color.red_F03D3D)
                    currentEditText?.setTextColor(context.resources.getColor(R.color.red_F03D3D))
                    currentEditText?.setHintTextColor(context.resources.getColor(R.color.red_F03D3D))
                }
                R.id.rb_selectText_green -> {
                    color = context.resources.getColor(R.color.green_2EC8AC)
                    currentEditText?.setTextColor(context.resources.getColor(R.color.green_2EC8AC))
                    currentEditText?.setHintTextColor(context.resources.getColor(R.color.green_2EC8AC))
                }
                R.id.rb_selectText_blue -> {
                    color = context.resources.getColor(R.color.blue_4F6CED)
                    currentEditText?.setTextColor(context.resources.getColor(R.color.blue_4F6CED))
                    currentEditText?.setHintTextColor(context.resources.getColor(R.color.blue_4F6CED))
                }
                R.id.rb_selectText_purple -> {
                    color = context.resources.getColor(R.color.purple_6F00D2)
                    currentEditText?.setTextColor(context.resources.getColor(R.color.purple_6F00D2))
                    currentEditText?.setHintTextColor(context.resources.getColor(R.color.purple_6F00D2))
                }
                R.id.rb_selectText_yellow -> {
                    color = context.resources.getColor(R.color.yellow_FFD306)
                    currentEditText?.setTextColor(context.resources.getColor(R.color.yellow_FFD306))
                    currentEditText?.setHintTextColor(context.resources.getColor(R.color.yellow_FFD306))
                }
                R.id.rb_selectText_brown -> {
                    color = context.resources.getColor(R.color.brown_804040)
                    currentEditText?.setTextColor(context.resources.getColor(R.color.brown_804040))
                    currentEditText?.setHintTextColor(context.resources.getColor(R.color.brown_804040))
                }
                R.id.rb_selectText_yellowGreen -> {
                    color = context.resources.getColor(R.color.green_yellow_808040)
                    currentEditText?.setTextColor(context.resources.getColor(R.color.green_yellow_808040))
                    currentEditText?.setHintTextColor(context.resources.getColor(R.color.green_yellow_808040))
                }
                R.id.rb_selectText_black -> {
                    color = context.resources.getColor(R.color.black_000000)
                    currentEditText?.setTextColor(context.resources.getColor(R.color.black_000000))
                    currentEditText?.setHintTextColor(context.resources.getColor(R.color.black_000000))
                }
                R.id.rb_selectText_white -> {
                    color = context.resources.getColor(R.color.white_FFFFFF)
                    currentEditText?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
                    currentEditText?.setHintTextColor(context.resources.getColor(R.color.white_FFFFFF))
                }
            }
        }
        mDialog?.rg_selectText_color?.check(
            if (textColor != null) getIdUseTextColor(context, textColor) else R.id.rb_selectText_red
        )
    }

    /**
     * 根据颜色获取id
     */
    private fun getIdUseTextColor(context: AppCompatActivity, textColor: Int): Int {
        return when (textColor) {
            context.resources.getColor(R.color.red_F03D3D) -> {
                R.id.rb_selectText_red
            }
            context.resources.getColor(R.color.green_2EC8AC) -> {
                R.id.rb_selectText_green
            }
            context.resources.getColor(R.color.blue_4F6CED) -> {
                R.id.rb_selectText_blue
            }
            context.resources.getColor(R.color.purple_6F00D2) -> {
                R.id.rb_selectText_purple
            }
            context.resources.getColor(R.color.yellow_FFD306) -> {
                R.id.rb_selectText_yellow
            }
            context.resources.getColor(R.color.brown_804040) -> {
                R.id.rb_selectText_brown
            }
            context.resources.getColor(R.color.green_yellow_808040) -> {
                R.id.rb_selectText_yellowGreen
            }
            context.resources.getColor(R.color.black_000000) -> {
                R.id.rb_selectText_black
            }
            context.resources.getColor(R.color.white_FFFFFF) -> {
                R.id.rb_selectText_white
            }
            else -> R.id.rb_selectText_red
        }
    }

    /**
     * 选择字体
     */
    private fun toSelectTextFont(context: AppCompatActivity, textTypeface: Typeface? = null) {
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
                    currentEditText?.setTypeface(Typeface.DEFAULT_BOLD)
                }
                R.id.rb_selectText_pty -> {
                    type = typeface4PTY
                    currentEditText?.setTypeface(typeface4PTY)
                }
                R.id.rb_selectText_sk -> {
                    type = typeface4SK
                    currentEditText?.setTypeface(typeface4SK)
                }
                R.id.rb_selectText_wk -> {
                    type = typeface4WK
                    currentEditText?.setTypeface(typeface4WK)
                }
                R.id.rb_selectText_bzyh -> {
                    type = typeface4BZYH
                    currentEditText?.setTypeface(typeface4BZYH)
                }
                R.id.rb_selectText_fgcs -> {
                    type = typeface4FGCS
                    currentEditText?.setTypeface(typeface4FGCS)
                }
            }
        }
        mDialog?.rg_selectText_font?.check(
            if (textTypeface != null) getIdUseTextTypeface(context, textTypeface)
            else R.id.rb_selectText_normal
        )
    }

    /**
     * 根据字体样式获取id
     */
    private fun getIdUseTextTypeface(context: AppCompatActivity, textTypeface: Typeface): Int {
        return when (textTypeface) {
            Typeface.createFromAsset(context.assets, "FZ_PTY.TTF") -> {
                R.id.rb_selectText_pty
            }
            Typeface.createFromAsset(context.assets, "FZ_SK.TTF") -> {
                R.id.rb_selectText_sk
            }
            Typeface.createFromAsset(context.assets, "FZ_WK.TTF") -> {
                R.id.rb_selectText_wk
            }
            Typeface.createFromAsset(context.assets, "FZ_BZYH.TTF") -> {
                R.id.rb_selectText_bzyh
            }
            Typeface.createFromAsset(context.assets, "FZ_FGCS.TTF") -> {
                R.id.rb_selectText_fgcs
            }
            else -> {
                R.id.rb_selectText_normal
            }
        }
    }


    interface OnAddTextListener {
        fun play(type: Int, etLists: MutableList<ScaleEditText>)
        fun save(type: Int, etLists: MutableList<ScaleEditText>)
        fun cancel(type: Int)
    }
}

