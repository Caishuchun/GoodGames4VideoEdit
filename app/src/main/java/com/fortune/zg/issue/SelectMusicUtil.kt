package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.app.Dialog
import android.media.MediaPlayer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.item_music.view.*
import kotlinx.android.synthetic.main.layout_select_music.*
import java.util.concurrent.TimeUnit

object SelectMusicUtil {
    private var mDialog: Dialog? = null

    private var mTopItem = 0
    private var mBottomItem = 0

    private var mAdapter: BaseAdapterWithPosition<MusicListBean.Data.MusicList>? = null

    private var favMusicListObservable: Disposable? = null

    private var recommendList = mutableListOf<MusicListBean.Data.MusicList>()
    private var favList = mutableListOf<MusicListBean.Data.MusicList>()
    private var mData = mutableListOf<MusicListBean.Data.MusicList>()

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

    interface SelectMusicCallback {
        fun finish(recommendMusic: MutableList<MusicListBean.Data.MusicList>)
    }

    /**
     * 显示配乐条框
     */
    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    fun showMusic(
        context: AppCompatActivity,
        videoView: VideoView,
        recommendMusic: MutableList<MusicListBean.Data.MusicList>,
        callback: SelectMusicCallback
    ) {
        mTopItem = 0
        mBottomItem = 0
        recommendList.clear()
        val moreMusic = MusicListBean.Data.MusicList(
            "", "", 0, 0, -1, "更多音乐", ""
        )
        recommendList.add(moreMusic)
        recommendList.addAll(recommendMusic)
        mData.clear()
        mData.addAll(recommendList)

        mDialog = Dialog(context, R.style.BeautifulDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_select_music, null) as LinearLayout
        mDialog?.setContentView(root)

        mDialog?.ll_selectMusic_music?.visibility = View.VISIBLE
        mDialog?.ll_selectMusic_volume?.visibility = View.GONE
        mDialog?.view_selectMusic_music_item?.visibility = View.VISIBLE
        mDialog?.view_selectMusic_volume_item?.visibility = View.GONE

        mDialog?.setOnDismissListener {
            recommendList.remove(moreMusic)
            callback.finish(recommendList)
        }

        initRecycleView(context)

        val videoVolume = Argument.getVideoVolume()
        if (videoVolume == 0f) {
            mDialog?.iv_selectMusic_videoMusic?.setImageResource(R.mipmap.check_in)
        } else {
            mDialog?.iv_selectMusic_videoMusic?.setImageResource(R.mipmap.check_un)
        }

        RxView.clicks(mDialog?.ll_selectMusic_videoMusic!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val videoVolume1 = Argument.getVideoVolume()
                if (videoVolume1 == 0f) {
                    Argument.setVideoVolume(0.8f)
                    mDialog?.iv_selectMusic_videoMusic?.setImageResource(R.mipmap.check_un)
                    setVideoViewVolume(videoView, 0.8f)
                } else {
                    Argument.setVideoVolume(0f)
                    mDialog?.iv_selectMusic_videoMusic?.setImageResource(R.mipmap.check_in)
                    setVideoViewVolume(videoView, 0f)
                }
                mDialog?.sb_selectMusic_old?.progress =
                    (Argument.getVideoVolume() * 100).toInt()
                mDialog?.tv_selectMusic_sb_old?.text =
                    (Argument.getVideoVolume() * 100).toInt().toString()
            }

        RxView.clicks(mDialog?.tv_selectMusic_recommend!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //推荐
                if (mTopItem != 0) {
                    mTopItem = 0
                    toChaneTopItem(context)
                    mDialog?.ll_selectMusic_noting?.visibility = View.GONE
                    mData.clear()
                    mData.addAll(recommendList)
                    mAdapter?.notifyDataSetChanged()
                }
            }
        RxView.clicks(mDialog?.tv_selectMusic_collection!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //收藏
                if (mTopItem != 1) {
                    mTopItem = 1
                    toChaneTopItem(context)
                    toGetFavMusicList(context)
                }
            }
        RxView.clicks(mDialog?.tv_selectMusic_local!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //本地
                if (mTopItem != 2) {
                    mTopItem = 2
                    toChaneTopItem(context)
                    mDialog?.ll_selectMusic_noting?.visibility = View.GONE
                    val localMust = LocalMusicUtil.getLocalMust(context)
                    mData.clear()
                    mData.addAll(localMust)
                    mAdapter?.notifyDataSetChanged()
                }
            }

        RxView.clicks(mDialog?.ll_selectMusic_music_item!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //配乐
                if (mBottomItem != 0) {
                    mBottomItem = 0
                    toChangeBottomItem(context)
                }
            }

        RxView.clicks(mDialog?.ll_selectMusic_volume_item!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //音量
                if (mBottomItem != 1) {
                    mBottomItem = 1
                    toChangeBottomItem(context)

                    //设置音量
                    mDialog?.sb_selectMusic_old?.progress =
                        (Argument.getVideoVolume() * 100).toInt()
                    mDialog?.sb_selectMusic_new?.progress =
                        (Argument.getMusicVolume() * 100).toInt()
                    mDialog?.tv_selectMusic_sb_old?.text =
                        (Argument.getVideoVolume() * 100).toInt().toString()
                    mDialog?.tv_selectMusic_sb_new?.text =
                        (Argument.getMusicVolume() * 100).toInt().toString()
                }
            }

        //原音音量调节
        mDialog?.sb_selectMusic_old?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                LogUtils.d("原音音量progress=$progress")
                val volume = progress.toFloat() / 100
                Argument.setVideoVolume(volume)
                setVideoViewVolume(videoView, volume)
                mDialog?.tv_selectMusic_sb_old?.text =
                    (volume * 100).toInt().toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        //配乐音量调节
        mDialog?.sb_selectMusic_new?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                LogUtils.d("配乐音量progress=$progress")
                val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                val volume = progress.toFloat() / 100
                Argument.setMusicVolume(volume)
                mediaPlayer?.setVolume(volume, volume)
                mDialog?.tv_selectMusic_sb_new?.text =
                    (volume * 100).toInt().toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        RxView.clicks(mDialog?.tv_selectMusic_toSelect!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                SelectMoreMusicUtil.showMoreMusic(context,
                    object : SelectMoreMusicUtil.SelectMoreMusicCallBack {
                        override fun finish() {
                            toChangeRecyclerViewItem()
                        }
                    })
                mTopItem = 0
                toChaneTopItem(context)
                mDialog?.ll_selectMusic_noting?.visibility = View.GONE
                mData.clear()
                mData.addAll(recommendList)
                mAdapter?.notifyDataSetChanged()
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
     * 更换位置
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun toChangeRecyclerViewItem() {
        val musicData = Argument.getMusicData()
        val mediaPlayer = MusicMediaPlayerUtil.initMediaPlayer()
        mediaPlayer?.setDataSource(musicData?.path)
        mediaPlayer?.prepareAsync()
        mediaPlayer?.setOnPreparedListener {
            mediaPlayer.start()
        }
        if (!recommendList.contains(musicData)) {
            //选择的不在,则添加到前面
            if (recommendList.size > 6) {
                recommendList[1] = musicData!!
            } else {
                recommendList.add(musicData!!)
                val tempList = mutableListOf<MusicListBean.Data.MusicList>()
                tempList.add(recommendList[0])
                tempList.add(recommendList[recommendList.size - 1])
                for (index in 2 until recommendList.size) {
                    tempList.add(recommendList[index - 1])
                }
                recommendList.clear()
                recommendList.addAll(tempList)
            }
            mData.clear()
            mData.addAll(recommendList)
            mAdapter?.notifyDataSetChanged()
        } else {
            val temp = recommendList[1]
            val indexOf = recommendList.indexOf(musicData)
            if (temp != musicData) {
                if (indexOf == 2) {
                    recommendList[1] = recommendList[indexOf]
                    recommendList[indexOf] = temp
                    mData.clear()
                    mData.addAll(recommendList)
                    mAdapter?.notifyDataSetChanged()
                } else {
                    val tempList = mutableListOf<MusicListBean.Data.MusicList>()
                    tempList.add(recommendList[0])
                    tempList.add(recommendList[indexOf])
                    for (index in 1 until indexOf) {
                        tempList.add(recommendList[index])
                    }
                    for (index in indexOf until recommendList.size - 1) {
                        tempList.add(recommendList[index + 1])
                    }
                    recommendList.clear()
                    recommendList.addAll(tempList)
                    mData.clear()
                    mData.addAll(recommendList)
                    mAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    /**
     * 调节VideoView的音量
     */
    @SuppressLint("DiscouragedPrivateApi")
    private fun setVideoViewVolume(videoView: VideoView, volume: Float) {
        try {
            val forName = Class.forName("android.widget.VideoView")
            val field = forName.getDeclaredField("mMediaPlayer")
            field.isAccessible = true
            val mMediaPlayer = field.get(videoView) as MediaPlayer
            mMediaPlayer.setVolume(volume, volume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取关注列表
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun toGetFavMusicList(context: AppCompatActivity) {
        val favMusicList = RetrofitUtils.builder().favMusicList(1)
        favMusicListObservable = favMusicList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            mData.clear()
                            if (it.data.list.isNotEmpty()) {
                                mDialog?.ll_selectMusic_noting?.visibility = View.GONE
                                favList.clear()
                                favList.addAll(it.data.list)
                                mData.addAll(favList)
                            } else {
                                mDialog?.ll_selectMusic_noting?.visibility = View.VISIBLE
                            }
                            mAdapter?.notifyDataSetChanged()
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(context)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    DialogUtils.dismissLoading()
                    ToastUtils.show(context.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(context, it))
            })
    }

    /**
     * 0 配乐, 1 音量
     */
    private fun toChangeBottomItem(context: AppCompatActivity) {
        mDialog?.tv_selectMaterial_music_item?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        mDialog?.tv_selectMaterial_volume_item?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        when (mBottomItem) {
            0 -> {
                mDialog?.tv_selectMaterial_music_item?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
                mDialog?.ll_selectMusic_music?.visibility = View.VISIBLE
                mDialog?.ll_selectMusic_volume?.visibility = View.GONE
                mDialog?.view_selectMusic_music_item?.visibility = View.VISIBLE
                mDialog?.view_selectMusic_volume_item?.visibility = View.GONE
            }
            1 -> {
                mDialog?.tv_selectMaterial_volume_item?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
                mDialog?.ll_selectMusic_music?.visibility = View.GONE
                mDialog?.ll_selectMusic_volume?.visibility = View.VISIBLE
                mDialog?.view_selectMusic_music_item?.visibility = View.GONE
                mDialog?.view_selectMusic_volume_item?.visibility = View.VISIBLE
            }
        }
    }

    /**
     * 0 推荐, 1 收藏, 2 本地
     */
    private fun toChaneTopItem(context: AppCompatActivity) {
        mDialog?.tv_selectMusic_recommend?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        mDialog?.tv_selectMusic_collection?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        mDialog?.tv_selectMusic_local?.setTextColor(context.resources.getColor(R.color.white_60FFFFFF))
        when (mTopItem) {
            0 -> {
                mDialog?.tv_selectMusic_recommend?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
            }
            1 -> {
                mDialog?.tv_selectMusic_collection?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
            }
            2 -> {
                mDialog?.tv_selectMusic_local?.setTextColor(context.resources.getColor(R.color.white_FFFFFF))
            }
        }
    }

    /**
     * 初始化RecycleView
     */
    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initRecycleView(context: AppCompatActivity) {
        mAdapter = BaseAdapterWithPosition.Builder<MusicListBean.Data.MusicList>()
            .setLayoutId(R.layout.item_music)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                itemView.tv_item_music_name.text = itemData.name
                if (itemData.music_id != -1) {
                    if (itemData.music_id == -2) {
                        val cover = LocalMusicUtil.getCover(context, itemData.path)
                        itemView.iv_item_music_cover.setImageBitmap(cover)
                    } else {
                        Glide.with(context)
                            .load(itemData.cover)
                            .into(itemView.iv_item_music_cover)
                    }
                }

                if (itemData == Argument.getMusicData()) {
                    itemView.iv_item_music_selected.visibility = View.VISIBLE
                } else {
                    itemView.iv_item_music_selected.visibility = View.GONE
                }

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.music_id == -1) {
                            //更多音乐
                            SelectMoreMusicUtil.showMoreMusic(context,
                                object : SelectMoreMusicUtil.SelectMoreMusicCallBack {
                                    override fun finish() {
                                        toChangeRecyclerViewItem()
                                    }
                                })
                        } else {
                            //推荐
                            val mMediaPlayer = MusicMediaPlayerUtil.initMediaPlayer()
                            mMediaPlayer?.setDataSource(itemData.path)
                            mMediaPlayer?.prepareAsync()
                            itemView.pb_item_music.visibility = View.VISIBLE
                            mMediaPlayer?.setOnPreparedListener {
                                itemView.pb_item_music.visibility = View.GONE
                                mMediaPlayer.start()
                                itemView.iv_item_music_selected.visibility = View.VISIBLE
                                Argument.setMusicData(itemData)
                                mAdapter?.notifyDataSetChanged()
                            }
                        }
                    }
            }
            .create()

        mDialog?.rv_selectMusic?.adapter = mAdapter
        mDialog?.rv_selectMusic?.layoutManager =
            SafeLinearLayoutManager(context, OrientationHelper.HORIZONTAL)
    }
}