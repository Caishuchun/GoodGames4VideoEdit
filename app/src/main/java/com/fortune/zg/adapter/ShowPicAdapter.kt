package com.fortune.zg.adapter

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.fortune.zg.R
import com.fortune.zg.activity.ShowPicActivity
import com.fortune.zg.utils.PhoneInfoUtils
import kotlinx.android.synthetic.main.fragment_mv_detail.view.*
import kotlinx.android.synthetic.main.layout_item_show_pic.view.*

class ShowPicAdapter(context: Context, picLists: List<String>) : PagerAdapter() {

    private var mContext: Context = context
    private var mLists: List<String> = picLists

    override fun getCount(): Int {
        return mLists.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view =
            LayoutInflater.from(mContext).inflate(R.layout.layout_item_show_pic, container, false)
        Glide.with(mContext)
            .load(mLists[position])
            .placeholder(R.mipmap.bg_gray_6)
            .override(Target.SIZE_ORIGINAL,Target.SIZE_ORIGINAL)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(view.iv_pic)

        Glide.with(mContext)
            .asBitmap()
            .load(mLists[position])
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    val width = resource.width
                    val height = resource.height
                    val screenWidth =
                        PhoneInfoUtils.getWidth(mContext as ShowPicActivity).toDouble()
                    val screenHeight =
                        PhoneInfoUtils.getHeight(mContext as ShowPicActivity).toDouble()
                    val picHeight = height * (screenWidth / width)
//                    LogUtils.d("${javaClass.simpleName}=height=$height,width=$width,screenWidth=$screenWidth,videoHeight=$videoHeight")
                    val layoutParams = view.fl_pic.layoutParams
                    layoutParams.width = screenWidth.toInt()
                    layoutParams.height = Math.min(picHeight, screenHeight).toInt()
                    view.fl_pic.layoutParams = layoutParams
                }
            })
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}