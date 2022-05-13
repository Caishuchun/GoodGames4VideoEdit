package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Handler
import android.os.Message
import android.view.View
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.service.DeleteFileIntentService
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_make_video.iv_makeVideo_back
import kotlinx.android.synthetic.main.activity_video_main.*
import kotlinx.android.synthetic.main.item_pic_video.view.*
import java.util.concurrent.TimeUnit


class VideoMainActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: VideoMainActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val SELECTED = "selected"
    }

    //文件选择过来的
    private var selected = mutableListOf<MediaData>()
    private var tempData = mutableListOf<MediaData>()

    //传来文件类型 0仅图片,1仅视频,2图片+视频
    private var containType = 0

    private var recommendMusicListObservable: Disposable? = null
    private var recommendMusicList = mutableListOf<MusicListBean.Data.MusicList>()

    private var mAdapter4PicList: BaseAdapterWithPosition<MediaData>? = null

    override fun getLayoutId() = R.layout.activity_video_main

    @SuppressLint("CheckResult")
    override fun doSomething() {
        val intent = Intent(this, DeleteFileIntentService::class.java)
        startService(intent)
        RxView.clicks(iv_makeVideo_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                exit()
            }
        initView()
    }

    override fun onBackPressed() {
        exit()
    }

    /**
     * 退出编辑
     */
    private fun exit() {
        DialogUtils.showDefaultDialog(
            this,
            "推出编辑",
            "退出编辑,将不保存已编辑文件",
            "暂不退出",
            "退出编辑",
            object : DialogUtils.OnDialogListener {
                override fun next() {
                    finish()
                }
            }
        )
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        selected = intent.getSerializableExtra(MakeVideoActivity.SELECTED) as MutableList<MediaData>
        tempData.clear()
        tempData.addAll(selected)
        LogUtils.d("======selected:${Gson().toJson(selected)}")
        var isHaveImage = false
        var isHaveVideo = false
        for (data in selected) {
            if (data.isVideo()) {
                isHaveVideo = true
            } else if (data.isImage()) {
                isHaveImage = true
            }
        }
        containType = if (isHaveImage && isHaveVideo) {
            2
        } else if (isHaveImage && !isHaveVideo) {
            0
        } else {
            1
        }
        LogUtils.d("======selected:containType = $containType")

        //0仅图片,1仅视频,2图片+视频
        when (containType) {
            0 -> {
                rl_videoMain_pic.visibility = View.VISIBLE
                rl_videoMain_video.visibility = View.GONE
                initPicRecyclerView()
                toPlayPic()
            }
            1 -> {
                rl_videoMain_pic.visibility = View.GONE
                rl_videoMain_video.visibility = View.VISIBLE
                toPlayVideo()
            }
            2 -> {
            }
        }

        toGetRecommendMusicList()

        /**
         * 选择配乐
         */
        RxView.clicks(ll_videoMain_music)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                SelectMusicUtil.showMusic(
                    this,
                    video_videoMain,
                    recommendMusicList,
                    object : SelectMusicUtil.SelectMusicCallback {
                        override fun finish(recommendMusic: MutableList<MusicListBean.Data.MusicList>) {
                            recommendMusicList.clear()
                            recommendMusicList.addAll(recommendMusic)
                        }
                    }
                )
            }

        /**
         * 选择风格
         */
        RxView.clicks(ll_videoMain_style)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                SelectStyleUtil.showStyle(this, "")
            }

        /**
         * 裁剪
         */
        RxView.clicks(ll_videoMain_part)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                CutUtil.showCut(this, selected, containType, object : CutUtil.ShowPartCallBack {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun cancel() {
                        mHandler4CuttingVideo.removeMessages(0)
                        when (containType) {
                            0 -> {
                                tempData.clear()
                                tempData.addAll(selected)
                                mAdapter4PicList?.notifyDataSetChanged()
                                toPlayPic()
                            }
                            1 -> {
                                video_videoMain.pause()
                                toPlayVideo()
                            }
                            2 -> {
                            }
                        }
                    }

                    override fun selectVideo(mediaData: MediaData) {
                        mHandler4CuttingVideo.removeMessages(0)
                        isCutting = true
                        val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                        mediaPlayer?.pause()

                        video_videoMain.pause()
                        video_videoMain.setVideoPath(mediaData.filePath)
                        video_videoMain.start()
                    }

                    override fun currentPosition(position: Long) {
                        mHandler4CuttingVideo.removeMessages(0)
                        video_videoMain.pause()
                        video_videoMain.seekTo(position.toInt())
                    }

                    override fun cutVideo(
                        startLong: Long,
                        startTime: String,
                        endLong: Long,
                        endTime: String
                    ) {
                        video_videoMain.pause()
                        video_videoMain.seekTo(startLong.toInt())
                        video_videoMain.start()
                        mHandler4CuttingVideo.removeMessages(0)
                        val message = Message()
                        message.arg1 = endLong.toInt()
                        mHandler4CuttingVideo.sendMessage(message)
                    }

                    @SuppressLint("NotifyDataSetChanged")
                    override fun deletePic(remain: MutableList<MediaData>) {
                        mHandler4PlayPicVideo.removeMessages(0)

                        tempData.clear()
                        tempData.addAll(remain)
                        mAdapter4PicList?.notifyDataSetChanged()

                        toPlayPic()
                    }

                    @SuppressLint("NotifyDataSetChanged")
                    override fun finish(result: MutableList<MediaData>) {
                        mHandler4CuttingVideo.removeMessages(0)
                        when (containType) {
                            0 -> {
                                tempData.clear()
                                tempData.addAll(result)
                                mAdapter4PicList?.notifyDataSetChanged()
                                toPlayPic()
                            }
                            1 -> {
                                video_videoMain.pause()
                                selected.clear()
                                selected.addAll(result)
                                toPlayVideo()
                            }
                            2 -> {
                            }
                        }
                    }
                })
            }

        /**
         * 选择封面
         */
        RxView.clicks(ll_videoMain_cover)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                mediaPlayer?.pause()
                when (containType) {
                    0 -> {
                        mHandler4PlayPicVideo.removeMessages(0)
                        rv_videoMain_pic.scrollToPosition(0)
                    }
                    1 -> {
                        video_videoMain.pause()
                        video_videoMain.seekTo(1)
                    }
                    2 -> {
                    }
                }
                CoverUtil.showSelectCover(
                    this,
                    containType,
                    position = 0,
                    time = null,
                    selected,
                    object : CoverUtil.OnShowSelectCoverCallback {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun cancel() {
                            when (containType) {
                                0 -> {
                                    tempData.clear()
                                    tempData.addAll(selected)
                                    mAdapter4PicList?.notifyDataSetChanged()
                                    toPlayPic()
                                }
                                1 -> {
                                    video_videoMain.pause()
                                    toPlayVideo()
                                }
                                2 -> {
                                }
                            }
                        }

                        override fun selectPicCover(position: Int) {
                            rv_videoMain_pic.smoothScrollToPosition(position)
                        }

                        override fun selectVideo(position: Int) {
                            video_videoMain.visibility = View.GONE
                            video_videoMain.setVideoPath(selected[position].filePath)
                            delayShowVideo()
                            video_videoMain.seekTo(1)
                        }

                        override fun selectVideoCover(time: Int) {
                            video_videoMain.seekTo(time)
                        }

                        @SuppressLint("NotifyDataSetChanged")
                        override fun finish(position: Int, time: Int?) {
                            when (containType) {
                                0 -> {
                                    tempData.clear()
                                    tempData.addAll(selected)
                                    mAdapter4PicList?.notifyDataSetChanged()
                                    toPlayPic()
                                }
                                1 -> {
                                    video_videoMain.pause()
                                    toPlayVideo()
                                }
                                2 -> {
                                }
                            }
                        }
                    })
            }

        /**
         * 设置文字
         */
        RxView.clicks(ll_videoMain_font)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                mediaPlayer?.pause()
                when (containType) {
                    0 -> {
                        mHandler4PlayPicVideo.removeMessages(0)
                        rv_videoMain_pic.scrollToPosition(0)
                    }
                    1 -> {
                        video_videoMain.pause()
                        video_videoMain.seekTo(1)
                    }
                    2 -> {
                    }
                }

                setDrawViewSize(drawView,containType)
                drawView.canMove(true)
                if (!mEditTextLists.isNullOrEmpty()) {
                    for (edit in mEditTextLists!!) {
                        edit.showOrHide(true)
                    }
                }

                AddTextUtil.showText(
                    this,
                    containType,
                    selected,
                    mEditTextLists,
                    drawView,
                    object : AddTextUtil.OnAddTextListener {
                        override fun play(type: Int, etLists: MutableList<ScaleEditText>) {
                            when (type) {
                                0 -> {
                                    //
                                    textType = 1
                                    drawView.canMove(false)
                                    mTempEditTextLists = etLists
                                    toPlayPic()
                                }
                            }
                        }

                        override fun save(type: Int, etLists: MutableList<ScaleEditText>) {
                            when (type) {
                                0 -> {
                                    //保存
                                    textType = 2
                                    drawView.canMove(false)
                                    mEditTextLists = etLists
                                    toPlayPic()
                                }
                            }
                        }

                        override fun cancel(type: Int) {
                            when (type) {
                                0 -> {
                                    //取消
                                    textType = 0
                                    drawView.canMove(false)
                                    mEditTextLists?.clear()
                                    mTempEditTextLists?.clear()
                                    toPlayPic()
                                }
                            }
                        }
                    }
                )
//                val inputStream = assets.open("FZ_PTY.TTF")
//                val ttf = getExternalFilesDir("pic2video").toString() + "/fz_pty.ttf"
//                if(!File(ttf).exists()){
//                    val fos = FileOutputStream(ttf)
//                    val buffer = ByteArray(inputStream.available())
//                    var lenght = 0
//                    while (inputStream.read(buffer)
//                            .also { lenght = it } != -1
//                    ) { // 循环从输入流读取buffer字节
//                        // 将Buffer中的数据写到outputStream对象中
//                        fos.write(buffer, 0, lenght)
//                    }
//                    fos.flush() // 刷新缓冲区
//                    // 4.关闭流
//                    fos.close()
//                    inputStream.close()
//                }
//                val out = getExternalFilesDir("pic2video")
//                    .toString() + "/new_${System.currentTimeMillis()}.mp4"
//                FFmpeg.executeAsync("-i ${selected[0].filePath} -vf drawtext=\"text='真的会乱码?':fontfile=$ttf:fontcolor=#ff0000:fontsize=55\",drawtext=\"text='11111':fontfile=$ttf:fontcolor=#00ff00:fontsize=55\" -y $out"
//                ) { executionId, returnCode ->
//                    when (returnCode) {
//                        Config.RETURN_CODE_SUCCESS -> {
//                            LogUtils.d("Command execution completed successfully.")
//                        }
//                        Config.RETURN_CODE_CANCEL -> {
//                            LogUtils.d("Command execution cancelled by user.")
//                        }
//                        else -> {
//                            LogUtils.d(
//                                String.format(
//                                    "Command execution failed with rc=%d and the output below.",
//                                    returnCode
//                                )
//                            )
//                            Config.printLastCommandOutput(Log.INFO)
//                        }
//                    }
//                }
            }
    }

    /**
     * 设置drawView的大小
     */
    private fun setDrawViewSize(drawView: DragView?, containType: Int) {
        val maxImageArea = getMaxImageArea(containType)
        val maxWidth = maxImageArea[0]
        val maxHeight = maxImageArea[1]
        LogUtils.d("======maxImageArea=>maxWidth:$maxWidth,maxHeight:$maxHeight")
        val layoutParams = drawView?.layoutParams
        layoutParams?.width = maxWidth
        layoutParams?.height = maxHeight
        drawView?.layoutParams = layoutParams
    }

    /**
     * 获取图片集合最大的宽高区域
     */
    private fun getMaxImageArea(containType: Int): ArrayList<Int> {
        val screenWidth = PhoneInfoUtils.getWidth(this)
        val screenHeight = PhoneInfoUtils.getHeight(this)
        LogUtils.d("======imageSize=>screenWidth:$screenWidth,screenHeight:$screenHeight")
        if(containType==1){
            return arrayListOf(screenWidth, screenHeight)
        }
        var maxWidth = 0
        var maxHeight = 0
        for (data in selected) {
            val imgSize = getImgSize(data.filePath)
            if (imgSize != null) {
                var imgWidth = imgSize[0]
                var imgHeight = imgSize[1]
                LogUtils.d("======imageSize=>width:$imgWidth,height:$imgHeight")
                if (imgWidth >= imgHeight) {
                    //宽图片
                    if (imgWidth >= screenWidth) {
                        maxWidth = screenWidth
                        imgHeight =
                            (imgHeight / (imgWidth.toFloat() / screenWidth)).toInt()
                    } else {
                        if (maxWidth < imgWidth) {
                            maxWidth = imgWidth
                        }
                    }
                    maxHeight = if (maxHeight >= imgHeight) maxHeight else imgHeight
                    if (maxWidth != screenWidth) {
                        maxHeight = (screenHeight.toFloat() / maxWidth * maxHeight).toInt()
                        if (maxHeight > screenHeight) {
                            maxHeight = screenHeight
                        }
                        maxWidth = screenWidth
                    }
                } else {
                    //长图片
                    if (imgHeight >= screenHeight) {
                        maxHeight = screenHeight
                        imgWidth =
                            (imgWidth / (imgHeight.toFloat() / screenHeight)).toInt()
                    } else {
                        if (maxHeight < imgHeight) {
                            maxHeight = imgHeight
                        }
                    }
                    maxWidth = if (maxWidth >= imgWidth) maxWidth else imgWidth
                    if (maxHeight != screenHeight) {
                        maxWidth = (screenHeight.toFloat() / maxHeight * maxWidth).toInt()
                        if (maxWidth > screenWidth) {
                            maxWidth = screenWidth
                        }
                        maxHeight = screenHeight
                    }
                }
            }
        }
        return arrayListOf(maxWidth, maxHeight)
    }

    /**
     * 获取图片的大小
     */
    private fun getImgSize(filePath: String?): ArrayList<Int>? {
        if (filePath == null) {
            return null
        }
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val bitmap = BitmapFactory.decodeFile(filePath, options)
        return arrayListOf(options.outWidth, options.outHeight)
    }

    /**
     * 剪辑时播放
     */
    private val mHandler4CuttingVideo = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val currentPosition = video_videoMain.currentPosition
            if (currentPosition >= msg.arg1) {
                video_videoMain.pause()
            } else {
                val message = Message()
                message.arg1 = msg.arg1
                sendMessageDelayed(message, 100)
            }
        }
    }

    /**
     * 初始化图片
     */
    private fun initPicRecyclerView() {
        mAdapter4PicList = BaseAdapterWithPosition.Builder<MediaData>()
            .setData(tempData)
            .setLayoutId(R.layout.item_pic_video)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData.filePath)
                    .into(itemView.iv_item_picVideo)
            }
            .create()

        rv_videoMain_pic.adapter = mAdapter4PicList
        rv_videoMain_pic.layoutManager =
            SafeLinearLayoutManager(this, OrientationHelper.HORIZONTAL)
    }

    /**
     * 播放图片
     */
    private var textType = 0  //0_nothing 1_play 2_save
    private var currentPicPosition = 0
    private var mEditTextLists: MutableList<ScaleEditText>? = null
    private var mTempEditTextLists: MutableList<ScaleEditText>? = null
    private fun toPlayPic() {
        mHandler4PlayPicVideo.removeMessages(0)
        currentPicPosition = 0
        rv_videoMain_pic.scrollToPosition(0)
        mHandler4PlayPicVideo.sendEmptyMessage(0)
        val musicPlayer = MusicMediaPlayerUtil.getMediaPlayer()
        musicPlayer?.seekTo(0)
        musicPlayer?.start()
    }

    /**
     * 图片播放
     */
    private val mHandler4PlayPicVideo = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            LogUtils.d("==============currentPicPosition:$currentPicPosition")
            val textLists = when (textType) {
                1 -> mTempEditTextLists
                else -> mEditTextLists
            }
            if (textLists != null) {
                for (et in textLists) {
                    val pages = et.getPages()
                    LogUtils.d("==============pages:${Gson().toJson(pages)}")
                    if (pages.contains(currentPicPosition)) {
                        et.showOrHide(true,false)
                    } else {
                        et.showOrHide(false)
                    }
                }
            }
            if (currentPicPosition == 0) {
                rv_videoMain_pic.scrollToPosition(currentPicPosition)
            } else {
                rv_videoMain_pic.smoothScrollToPosition(currentPicPosition)
            }
            currentPicPosition++
            if (currentPicPosition >= tempData.size) {
                if (textLists == null || textType == 2) {
                    currentPicPosition = 0
                    val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                    mediaPlayer?.seekTo(0)
                    mediaPlayer?.start()
                    sendEmptyMessageDelayed(0, 3000)
                } else {
                    Handler().postDelayed({
                        currentPicPosition = 0
                        rv_videoMain_pic.scrollToPosition(currentPicPosition)
                        for (et in textLists) {
                            et.showOrHide(true)
                        }
                    }, 3000)
                }
            } else {
                sendEmptyMessageDelayed(0, 3000)
            }
        }
    }

    /**
     * 播放视频
     */
    private var currentVideoPosition = 0
    private var isCutting = false  //是否正在裁剪
    private fun toPlayVideo() {
        video_videoMain.setOnPreparedListener {
            setVideoViewVolume2Low()
        }
        currentVideoPosition = 0
        isCutting = false
        video_videoMain.visibility = View.GONE
        video_videoMain.setVideoPath(selected[currentVideoPosition].filePath)
        delayShowVideo()
        video_videoMain.start()
        val musicPlayer = MusicMediaPlayerUtil.getMediaPlayer()
        musicPlayer?.seekTo(0)
        musicPlayer?.start()
        video_videoMain.setOnCompletionListener {
            if (!isCutting) {
                video_videoMain.visibility = View.GONE
                currentVideoPosition++
                if (currentVideoPosition >= selected.size) {
                    currentVideoPosition = 0
                    video_videoMain.setVideoPath(selected[currentVideoPosition].filePath)
                    delayShowVideo()
                    video_videoMain.start()
                    val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                    mediaPlayer?.seekTo(0)
                    mediaPlayer?.start()
                } else {
                    video_videoMain.setVideoPath(selected[currentVideoPosition].filePath)
                    delayShowVideo()
                    video_videoMain.start()
                }
            }
        }
    }

    /**
     * 延时展示播放器
     */
    private fun delayShowVideo() {
        Handler().postDelayed({
            video_videoMain.visibility = View.VISIBLE
        }, 1)
    }


    /**
     * 获取推荐音乐列表
     */
    private fun toGetRecommendMusicList() {
        val recommendMusic = RetrofitUtils.builder().recommendMusicList()
        recommendMusicListObservable = recommendMusic
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (it.data.list.isNotEmpty()) {
                                recommendMusicList.clear()
                                recommendMusicList.addAll(it.data.list)
                                Argument.setMusicData(recommendMusicList[0])
                                toPlayMusic(recommendMusicList[0].path)
                            }
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    DialogUtils.dismissLoading()
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 播放配乐
     */
    private fun toPlayMusic(path: String) {
        val mMediaPlayer = MusicMediaPlayerUtil.initMediaPlayer()
        mMediaPlayer?.setDataSource(path)
        mMediaPlayer?.prepareAsync()
        mMediaPlayer?.setOnPreparedListener {
            mMediaPlayer.seekTo(0)
            mMediaPlayer.start()
        }
        mMediaPlayer?.setOnCompletionListener {
            mMediaPlayer.seekTo(0)
            mMediaPlayer.start()
        }
    }

    /**
     * 调节VideoView的音量为0
     */
    @SuppressLint("DiscouragedPrivateApi")
    private fun setVideoViewVolume2Low() {
        try {
            val forName = Class.forName("android.widget.VideoView")
            val field = forName.getDeclaredField("mMediaPlayer")
            field.isAccessible = true
            val mMediaPlayer = field.get(video_videoMain) as MediaPlayer
            val videoVolume = Argument.getVideoVolume()
            mMediaPlayer.setVolume(videoVolume, videoVolume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
        if (isPause) {
            isPause = false
            video_videoMain.setVideoPath(selected[currentVideoPosition].filePath)
            video_videoMain.seekTo(currentVideoPlayPosition)
            video_videoMain.start()
            val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
            mediaPlayer?.seekTo(currentMusicPosition)
            mediaPlayer?.start()
        }
    }

    private var isPause = false
    private var currentMusicPosition = 0
    private var currentVideoPlayPosition = 0
    override fun onPause() {
        super.onPause()
        isPause = true
        MobclickAgent.onPause(this)
        currentVideoPlayPosition = video_videoMain.currentPosition
        video_videoMain.pause()
        val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
        currentMusicPosition = mediaPlayer?.currentPosition ?: 0
        mediaPlayer?.pause()
    }

    override fun destroy() {
        video_videoMain.pause()
        MusicMediaPlayerUtil.release()
    }
}