package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.view.View
import com.fortune.zg.R
import com.fortune.zg.activity.IssueMvActivity
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.service.DeleteFileIntentService
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.microshow.rxffmpeg.RxFFmpegInvoke
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_make_video.*
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.*
import java.util.concurrent.TimeUnit

class MakeVideoActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: MakeVideoActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val SELECTED = "selected"
    }

    //文件选择过来的
    private var selected = mutableListOf<MediaData>()

    //传来文件类型 0仅图片,1仅视频,2图片+视频
    private var containType = 0

    private var currentIndex = 0

    private var recommendMusicListObservable: Disposable? = null
    private var recommendMusicList = mutableListOf<MusicListBean.Data.MusicList>()

    private var musicReady = false
    private var videoReady = false

    private var videoPath = "" //视频路径
    private var videoViewMediaPlayer: MediaPlayer? = null

    private var coverPosition = 0L
    private var mTextInfo: SelectTextUtil.TextInfo? = null
    private var mShowTextStart = 0
    private var mShowTextEnd = 0

    override fun getLayoutId() = R.layout.activity_make_video

    @SuppressLint("CheckResult")
    override fun doSomething() {
        instance = this

        val intent = Intent(this, DeleteFileIntentService::class.java)
        startService(intent)
        MusicMediaPlayerUtil.release()
        RxFFmpegInvoke.getInstance().onClean()
        RxFFmpegInvoke.getInstance().onDestroy()
        RxFFmpegInvoke.getInstance().exit()

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
        Argument.reset()
        selected = intent.getSerializableExtra(SELECTED) as MutableList<MediaData>
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

        toFirstNormalMakeVideo()
        toGetRecommendMusicList()

        /**
         * 选择配乐
         */
        RxView.clicks(ll_makeVideo_music)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                SelectMusicUtil.showMusic(
                    this,
                    video_makeVideo_preview,
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
        RxView.clicks(ll_makeVideo_style)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
//                SelectStyleUtil.showStyle(this, videoPath)
            }

        /**
         * 视频剪辑
         */
        RxView.clicks(ll_makeVideo_part)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (containType == 0) {
                    ToastUtils.show("图片合成视频暂不支持剪辑")
                } else {
                    VideoPartUtil.showPart(this, selected, object : VideoPartUtil.ShowPartCallBack {
                        override fun cancel() {
                            LogUtils.d("VideoPartUtil=>cancel()")
                            mHandler.removeMessages(0)
                            video_makeVideo_preview.pause()
                            video_makeVideo_preview.setVideoPath(videoPath)
                            video_makeVideo_preview.start()
                        }

                        override fun selectVideo(mediaData: MediaData) {
                            LogUtils.d("VideoPartUtil=>selectVideo()")
                            video_makeVideo_preview.pause()
                            video_makeVideo_preview.setVideoPath(mediaData.filePath)
                            video_makeVideo_preview.start()
                        }

                        override fun currentPosition(position: Long) {
                            LogUtils.d("VideoPartUtil=>currentPosition()")
                            mHandler.removeMessages(0)
                            video_makeVideo_preview.pause()
                            video_makeVideo_preview.seekTo(position.toInt())
                            video_makeVideo_preview.start()
                        }

                        override fun cutVideo(
                            startLong: Long,
                            startTime: String,
                            endLong: Long,
                            endTime: String
                        ) {
                            LogUtils.d("VideoPartUtil=>cutVideo()")
                            mHandler.removeMessages(0)
                            video_makeVideo_preview.pause()
                            video_makeVideo_preview.seekTo(startLong.toInt())
                            video_makeVideo_preview.start()
                            val message = Message()
                            message.obj =
                                VideoInterval(startLong.toInt(), startLong.toInt(), endLong.toInt())
                            mHandler.sendMessage(message)
                        }

                        override fun allVideoCutSuccess(
                            mediaInfo: MutableList<MediaData>,
                            videoFilePath: String
                        ) {
                            LogUtils.d("VideoPartUtil=>allVideoCutSuccess()")
                            mHandler.removeMessages(0)
                            videoPath = videoFilePath
                            selected.clear()
                            selected.addAll(mediaInfo)
                            video_makeVideo_preview.pause()
                            video_makeVideo_preview.setVideoPath(videoPath)
                            video_makeVideo_preview.start()
                            val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                            mediaPlayer?.pause()
                            mediaPlayer?.seekTo(0)
                            mediaPlayer?.start()
                        }
                    })
                }
            }
        /**
         * 选封面
         */
        RxView.clicks(ll_makeVideo_cover)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                video_makeVideo_preview.pause()
                video_makeVideo_preview.seekTo(coverPosition.toInt())
                SelectCoverUtil.showSelectCover(
                    this,
                    videoPath,
                    coverPosition,
                    containType == 0,
                    object : SelectCoverUtil.SelectCoverCallBack {
                        override fun cancel() {
                            video_makeVideo_preview.seekTo(0)
                            video_makeVideo_preview.start()
                            val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                            mediaPlayer?.pause()
                            mediaPlayer?.seekTo(0)
                            mediaPlayer?.start()
                        }

                        override fun currentPosition(position: Long) {
                            video_makeVideo_preview.seekTo(position.toInt())
                        }

                        override fun cover(position: Long) {
                            ToastUtils.show("选择封面成功")
                            coverPosition = position
                            video_makeVideo_preview.pause()
                            video_makeVideo_preview.seekTo(0)
                            video_makeVideo_preview.start()
                            val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                            mediaPlayer?.pause()
                            mediaPlayer?.seekTo(0)
                            mediaPlayer?.start()
                        }
                    })
            }
        /**
         * 选择文字
         */
        RxView.clicks(ll_makeVideo_font)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                video_makeVideo_preview.pause()
                video_makeVideo_preview.seekTo(0)
//                SelectTextUtil.showSelectText(
//                    this,
//                    videoPath,
//                    mTextInfo,
//                    object : SelectTextUtil.SelectTextCallback {
//                        override fun cancel() {
//                            video_makeVideo_preview.seekTo(0)
//                            video_makeVideo_preview.start()
//                            val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
//                            mediaPlayer?.seekTo(0)
//                            mediaPlayer?.start()
//                        }
//
//                        override fun textInfo(textInfo: SelectTextUtil.TextInfo?) {
//                            LogUtils.d("=======111=${Gson().toJson(textInfo)}")
//                            mTextInfo = textInfo
//                            if (null == mTextInfo) {
//                                mHandler4VideoText.removeMessages(0)
//                                tv_makeVideo_text.visibility = View.GONE
//                                video_makeVideo_preview.pause()
//                                video_makeVideo_preview.setVideoPath(videoPath)
//                                video_makeVideo_preview.start()
//                                val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
//                                mediaPlayer?.seekTo(0)
//                                mediaPlayer?.start()
//                            } else {
//                                tv_makeVideo_text.text = textInfo!!.text
//                                tv_makeVideo_text.typeface = textInfo.type
//                                tv_makeVideo_text.setTextColor(textInfo.color)
//                                tv_makeVideo_text.textSize = textInfo.size / 2 * 3
//                                mShowTextStart = textInfo.start
//                                mShowTextEnd = textInfo.end
//                                video_makeVideo_preview.pause()
//                                video_makeVideo_preview.setVideoPath(videoPath)
//                                video_makeVideo_preview.start()
//                                val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
//                                mediaPlayer?.seekTo(0)
//                                mediaPlayer?.start()
//                                mHandler4VideoText.sendEmptyMessage(0)
//                            }
//                        }
//                    })
            }
        /**
         * 下一步
         */
        RxView.clicks(tv_makeVideo_next)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val mediaInfo = RxFFmpegInvoke.getInstance().getMediaInfo(videoPath)
                val duration = MediaInfoUtil.getMediaDuration4Video(mediaInfo)
                if (duration > 5 * 60 * 1000) {
                    ToastUtils.show("发布视频不得超过5分钟!")
                } else {
                    val bitmap = ThumbnailUtils.createVideoThumbnail(
                        videoPath,
                        MediaStore.Video.Thumbnails.MINI_KIND
                    )
                    DialogUtils.showDialog4MakeVideo(this, "正在制作视频 0%", bitmap, null)
                    val format4Video = MediaInfoUtil.format4Video(mediaInfo)
                    if (null == format4Video.audiostream_avcodocname) {
                        Thread {
                            toMergeVideoAndText(false)
                        }.start()
                    } else {
                        Thread {
                            toMergeVideoAndText(true)
                        }.start()
                    }
                }
            }

        video_makeVideo_preview.setOnPreparedListener {
            videoViewMediaPlayer = it
            setVideoViewVolume2Low()
        }
        video_makeVideo_preview.setOnCompletionListener {
            video_makeVideo_preview.start()
            val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
        }

        video_makeVideo_preview.setPlayPauseListener(object : CustomVideoView.PlayPauseListener {
            override fun onPlay() {
                val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                mediaPlayer?.start()
            }

            override fun onPause() {
                val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                mediaPlayer?.pause()
            }
        })
    }

    /**
     * 合并文字和视频
     */
    private fun toMergeVideoAndText(isHaveMusic: Boolean) {
        if (null != mTextInfo) {
            val out = getExternalFilesDir("pic2video")
                .toString() + "/videoWithText_${System.currentTimeMillis()}.mp4"
            val textView2Bitmap = textView2Bitmap()
            val textFile = saveBitmap(textView2Bitmap)

            //overlay=0:0 是位置
            //scale=50:100 是大小
            val screenWidth = PhoneInfoUtils.getWidth(this)
            val mediaInfo = RxFFmpegInvoke.getInstance().getMediaInfo(videoPath)
            val scale = 360f / screenWidth
            val width = tv_makeVideo_text.width * scale
            val height = tv_makeVideo_text.height * scale
            val start = mTextInfo?.start!! / 1000
            val end = mTextInfo?.end!! / 1000
            val move = 80
            LogUtils.d("========>width:$width,height:$height,move:$move")
            val commands =
                "ffmpeg -y -i $videoPath -i $textFile -filter_complex [0:v]scale=iw:ih[outv0];[1:0]scale=$width:$height[outv1];[outv0][outv1]overlay=x=if(gte(t\\,$start)\\,if(lte(t\\,$end)\\,(main_w-overlay_w)/2\\,NAN)\\,NAN):y=if(gte(t\\,$start)\\,if(lte(t\\,$end)\\,main_h/2+$move\\,NAN)\\,NAN) -preset superfast $out"
                    .split(" ")
            RxFFmpegInvoke.getInstance()
                .runCommandRxJava(commands.toTypedArray())
                .subscribe(object : RxFFmpegSubscriber() {
                    override fun onError(message: String?) {
                        LogUtils.d("toMakeVideoWithText=>error:$message")
                        runOnUiThread {
                            DialogUtils.dismissLoading()
                        }
                    }

                    override fun onFinish() {
                        LogUtils.d("toMakeVideoWithText=>finish")
                        toMergeVideoAndMusic(isHaveMusic, out)
                    }

                    override fun onProgress(progress: Int, progressTime: Long) {
                        LogUtils.d("toMakeVideoWithText=>progress=>progress:$progress,progressTime:$progressTime")
                        runOnUiThread {
                            if (progress > 0) {
                                DialogUtils.setDialogMsg("正在制作视频 ${progress / 2}%")
                            }
                        }
                    }

                    override fun onCancel() {
                        LogUtils.d("toMakeVideoWithText=>cancel")
                        runOnUiThread {
                            DialogUtils.dismissLoading()
                        }
                    }
                })
        } else {
            toMergeVideoAndMusic(isHaveMusic)
        }
    }

    /**
     * 保存图片
     */
    private fun saveBitmap(bitmap: Bitmap): String {
        val file =
            getExternalFilesDir("pic2video").toString() + "/text_${System.currentTimeMillis()}.png"
        try {
            //文件输出流
            val fileOutputStream = FileOutputStream(file)
            //压缩图片，如果要保存png，就用Bitmap.CompressFormat.PNG，要保存jpg就用Bitmap.CompressFormat.JPEG,质量是100%，表示不压缩
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            //写入，这里会卡顿，因为图片较大
            fileOutputStream.flush()
            //记得要关闭写入流
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            return file
        }
    }

    /**
     * TextView转Bitmap
     */
    private fun textView2Bitmap(): Bitmap {
        val textView = tv_makeVideo_text
        val width = textView.width
        val height = textView.height

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.TRANSPARENT)
        textView.layout(0, 0, width, height)
        textView.draw(canvas)
        return bitmap
    }

    @SuppressLint("HandlerLeak")
    private var mHandler4VideoText = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (null != video_makeVideo_preview) {
                val currentPosition = video_makeVideo_preview.currentPosition
                if (currentPosition in mShowTextStart..mShowTextEnd) {
                    tv_makeVideo_text.visibility = View.VISIBLE
                } else {
                    tv_makeVideo_text.visibility = View.GONE
                }
            }
            sendEmptyMessageDelayed(0, 100)
        }
    }

    data class VideoInterval(val start: Int, var current: Int, val end: Int)

    @SuppressLint("HandlerLeak")
    private var mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val videoInterval = msg.obj as VideoInterval
//            LogUtils.d("====start:${videoInterval.start},current:${videoInterval.current},end:${videoInterval.end}")
            if (videoInterval.current >= videoInterval.end) {
                video_makeVideo_preview.pause()
                video_makeVideo_preview.seekTo(videoInterval.start)
                video_makeVideo_preview.start()
                val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()
                val message = Message()
                message.obj =
                    VideoInterval(videoInterval.start, videoInterval.start, videoInterval.end)
                sendMessageDelayed(message, 100)
            } else {
                videoInterval.current += 100
                val message = Message()
                message.obj =
                    VideoInterval(videoInterval.start, videoInterval.current, videoInterval.end)
                sendMessageDelayed(message, 100)
            }
        }
    }

    /**
     * 视频和音频的合成
     */
    private fun toMergeVideoAndMusic(isHaveMusic: Boolean, filePath: String? = null) {
        val musicData = Argument.getMusicData()!!
        val path = musicData.path
        val videoWithMusic =
            getExternalFilesDir("pic2video").toString() + "/videoWithMusic_${System.currentTimeMillis()}.mp4"
        val realVideoPath = filePath ?: videoPath
        val commands = if (!isHaveMusic) {
            //没有音频
            "ffmpeg * -i * $path * -i * $realVideoPath * -t * ${3 * selected.size} * -y * $videoWithMusic"
                .split(" * ")
        } else {
            //有音频
            "ffmpeg * -y * -i * $realVideoPath * -i * $path * -filter_complex * [0:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo,volume=${Argument.getVideoVolume()}[a0];[1:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo,volume=${Argument.getMusicVolume()}[a1];[a0][a1]amix=inputs=2:duration=first[aout] * -map * [aout] * -ac * 2 * -c:v * copy * -map * 0:v:0 * -preset * superfast * $videoWithMusic"
                .split(" * ")
        }

        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands.toTypedArray())
            .subscribe(object : RxFFmpegSubscriber() {
                override fun onError(message: String?) {
                    LogUtils.d("toMergeVideoAndMusic=>error:$message")
                    runOnUiThread {
                        DialogUtils.dismissLoading()
                    }
                }

                override fun onFinish() {
                    LogUtils.d("toMergeVideoAndMusic=>finish")
                    DialogUtils.setDialogMsg("正在制作封面...")
                    videoPath = videoWithMusic
                    val mediaInfo1 = RxFFmpegInvoke.getInstance().getMediaInfo(videoPath)
                    val mediaDuration4Video = MediaInfoUtil.getMediaDuration4Video(mediaInfo1)
                    runOnUiThread {
                        toMakeCover(mediaDuration4Video)
                    }
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    LogUtils.d("toMergeVideoAndMusic=>progress=>progress:$progress,progressTime:$progressTime")
                    runOnUiThread {
                        if (progress > 0)
                            DialogUtils.setDialogMsg("正在制作视频 ${if (null != filePath) 50 + progress / 2 else progress}%")
                    }
                }

                override fun onCancel() {
                    LogUtils.d("toMergeVideoAndMusic=>cancel")
                    runOnUiThread {
                        DialogUtils.dismissLoading()
                    }
                }
            })
    }

    /**
     * 制作封面
     */
    private fun toMakeCover(mediaDuration4Video: Long) {
        Thread {
            val media = MediaMetadataRetriever()
            media.setDataSource(videoPath)
            Thread.sleep(200)
            if (coverPosition > mediaDuration4Video) {
                coverPosition = 0
            }
            val frameAtIndex = media.getFrameAtTime(
                if (coverPosition < 1L) 1L else coverPosition * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )!!
            val bitmap = Bitmap.createScaledBitmap(
                frameAtIndex,
                frameAtIndex.width / 2,
                frameAtIndex.height / 2,
                false
            )
            frameAtIndex.recycle()
            val stream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            val tempPath = getExternalFilesDir("pic2video").toString()
            var file = File("$tempPath/new.png")
            val fos: FileOutputStream
            try {
                if (file.exists() && file.length() > 100) {
                    file.delete()
                    Thread.sleep(200)
                }
                file = File("$tempPath/new.png")
                fos = FileOutputStream(file)
                fos.write(byteArray, 0, byteArray.size)
                stream.flush()
                fos.flush()
                bitmap?.recycle()
                media.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            runOnUiThread {
                toIssueActivity(file.path)
            }
        }.start()
    }

    /**
     * 去发布页
     */
    private fun toIssueActivity(path: String) {
        LogUtils.d("toMergeVideoJump=>path=$path")
        DialogUtils.dismissLoading()
        val intent = Intent(this, IssueMvActivity::class.java)
        intent.putExtra(IssueMvActivity.FROM_HIS, false)
        intent.putExtra(IssueMvActivity.VIDEO_PATH, videoPath)
        intent.putExtra(IssueMvActivity.VIDEO_COVER_PAHT, path)
        startActivity(intent)
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
            musicReady = true
            toPlay()
        }
    }

    private fun toPlay() {
        if (musicReady && videoReady) {
            val mediaPlayer = MusicMediaPlayerUtil.getMediaPlayer()
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
        }
    }

    /**
     * 获取视频控件
     */
    fun getVideoView() = video_makeVideo_preview

    /**
     * 先来一次默认处理
     */
    private fun toFirstNormalMakeVideo() {
        //0仅图片,1仅视频,2图片+视频
        when (containType) {
            0 -> {
                DialogUtils.showDialog4MakeVideo(
                    this,
                    "合成视频中 0%",
                    null,
                    selected[0].filePath!!,
                    object : DialogUtils.OnShowListener {
                        override fun onShowing() {
                            mHanlder4WaitDeleteFile.sendEmptyMessage(0)
                        }
                    }
                )
            }
            1 -> {
//                if (selected.size > 1) {
                val bitmap = ThumbnailUtils.createVideoThumbnail(
                    selected[0].filePath!!,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )
                DialogUtils.showDialog4MakeVideo(this, "合成视频中 0%", bitmap, null)
                mHanlder4WaitDeleteFile.sendEmptyMessage(1)
//                } else {
//                    fl_makeVideo_preview.visibility = View.VISIBLE
//                    videoPath = selected[0].filePath!!
//                    toSetRootSize()
//                    video_makeVideo_preview.setVideoPath(selected[0].filePath)
//                    video_makeVideo_preview.start()
//                    videoReady = true
//                    toPlay()
//                }
            }
            2 -> {
                toMergePicAndVideo()
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val mHanlder4WaitDeleteFile = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val isDeleteFileOver = SPUtils.getBoolean(SPArgument.IS_DELETE_FILE_OVER, false)
            if (isDeleteFileOver) {
                when (msg.what) {
                    0 -> {
                        Thread {
                            toMakePic2Video()
                        }.start()
                    }
                    1 -> {
                        Thread {
                            toCopyVideo()
                        }.start()
                    }
                    2 -> {
                    }
                }
            } else {
                sendEmptyMessageDelayed(msg.what, 100)
            }
        }
    }


    /**
     * 复制文件
     */
    private fun toCopyVideo() {
        toCopyVideo(0)
    }

    /**
     * 复制文件进行操作
     */
    private fun toCopyVideo(index: Int) {
        val copyVideo = getExternalFilesDir("pic2video").toString() + "/copy_$index.mp4"
        try {
            val fis = FileInputStream(selected[index].filePath)
            val fos = FileOutputStream(copyVideo)

            val bis = BufferedInputStream(fis)
            val bos = BufferedOutputStream(fos)
            val buf = ByteArray(1024)
            var len = 0
            while (true) {
                len = bis.read(buf)
                if (len == -1) break;
                bos.write(buf, 0, len)
            }
            fis.close()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            selected[index].filePath = copyVideo
            if (index == selected.size - 1) {
                toCycleFormatVideo(toGetVideoMaxHeight())
            } else {
                toCopyVideo(index + 1)
            }
        }
    }

    /**
     * 将图片制作成视频
     */
    private fun toMakePic2Video() {
        val pic =
            getExternalFilesDir("pic2video").toString() + "/pic_${System.currentTimeMillis()}.mp4"
        val filePath = selected[currentIndex].filePath!!
        val picMaxHeight = getPicMaxHeight()
        val fade = when (currentIndex) {
            0 -> {
                "fade=out:st=2.7:d=0.3"
            }
            else -> {
                "fade=in:st=0:d=0.3,fade=out:st=2.7:d=0.3"
            }
        }
        val commands =
            "ffmpeg -y -loop 1 -i $filePath -vf scale=360:(360.0/iw*ih),pad=width=360:height=$picMaxHeight:x=0:y=(oh-ih)/2:color=black,$fade -t 3 -pix_fmt yuv420p -r 30 -b 500k -c:v libx264 -x264opts keyint=30 $pic"
                .split(" ")
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands.toTypedArray())
            .subscribe(object : RxFFmpegSubscriber() {
                override fun onError(message: String?) {
                    LogUtils.d("toMakePic2Video=>error:$message")
                    runOnUiThread {
                        DialogUtils.dismissLoading()
                    }
                }

                override fun onFinish() {
                    LogUtils.d("toMakePic2Video=>finish")
                    if (currentIndex != selected.size - 1) {
                        currentIndex++
                        toMakePic2Video()
                    } else {
                        currentIndex = 0
                        toFormat2Ts(true)
                    }
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    LogUtils.d("toMakePic2Video=>progress=>progress:$progress,progressTime:$progressTime")
                    runOnUiThread {
                        if (progressTime > 0) {
                            DialogUtils.setDialogMsg("合成视频中 ${(progressTime.toFloat() * 100 / 3000000 * 0.9 / selected.size + currentIndex * 90.0 / selected.size).toInt()}%")
                        }
                    }
                }

                override fun onCancel() {
                    LogUtils.d("toMakePic2Video=>cancel")
                    runOnUiThread {
                        DialogUtils.dismissLoading()
                    }
                }
            })
    }

    /**
     * 获取图片的最大高度
     */
    private fun getPicMaxHeight(): Int {
        var maxHeight = 0
        for (index in selected.indices) {
            val picWidthAndHeight = getPicWidthAndHeight(selected[index].filePath!!)
            val width = picWidthAndHeight.split(":")[0].toInt()
            val height = picWidthAndHeight.split(":")[1].toInt()
            val scale = 360.0 / width
            val scaleHeight = height * scale
            if (scaleHeight > maxHeight) {
                maxHeight = scaleHeight.toInt()
            }
        }
        return maxHeight + 2
    }

    /**
     * 获取图片的宽高
     */
    private fun getPicWidthAndHeight(picFilePath: String): String {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val bitmap = BitmapFactory.decodeFile(picFilePath, options)
        return "${options.outWidth}:${options.outHeight}"
    }

    /**
     * 设置根的大小
     */
    private fun toSetRootSize() {
        val mediaInfo = RxFFmpegInvoke.getInstance().getMediaInfo(videoPath)
        val videoWidth = MediaInfoUtil.getMediaWidth4Video(mediaInfo)
        val videoHeight = MediaInfoUtil.getMediaHeight4Video(mediaInfo)
        val screenWidth = PhoneInfoUtils.getWidth(this)
        var shouldHeight = (screenWidth.toFloat() / videoWidth * videoHeight).toInt()
        shouldHeight = Math.min(shouldHeight, shouldHeight)
        val layoutParams = fl_makeVideo_preview.layoutParams
        layoutParams.width = screenWidth
        layoutParams.height = shouldHeight
        LogUtils.d("=======fl_makeVideo_preview,width=$screenWidth,height=$shouldHeight,videoWidth=$videoWidth,videoHeight=$videoHeight")
        fl_makeVideo_preview.layoutParams = layoutParams
    }

    /**
     * 将图片添加到视频里去
     */
    private fun toMergePicAndVideo() {

    }

    /**
     * 合并视频
     */
    private fun toMergeVideoUseConcat() {

    }

    /**
     * 合并视频
     */
    private fun toMergeVideo() {
        val folder = getExternalFilesDir("pic2video").toString()
        val folderFile = File(folder)
        var videoLists = ""
        if (folderFile.exists() && folderFile.isDirectory && folderFile.listFiles().isNotEmpty()) {
            for (index in folderFile.listFiles().indices) {
                val path = folderFile.listFiles()[index].path
                val fileName = path.substring(path.lastIndexOf("/") + 1, path.length)
                if (fileName.startsWith("ts_") && fileName.endsWith(".ts")) {
                    videoLists += path
                    if (currentIndex != selected.size - 1) {
                        currentIndex++
                        videoLists += "|"
                    } else {
                        currentIndex = 0
                    }
                }
            }
        }
        val out = getExternalFilesDir("pic2video").toString() + "/out.mp4"
        val commands =
            "ffmpeg -i concat:$videoLists -c copy -bsf:a aac_adtstoasc $out"
                .split(" ")
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands.toTypedArray())
            .subscribe(object : RxFFmpegSubscriber() {
                override fun onError(message: String?) {
                    LogUtils.d("toMergeVideo=>error:$message")
                    runOnUiThread {
                        DialogUtils.dismissLoading()
                    }
                }

                override fun onFinish() {
                    LogUtils.d("toMergeVideo=>finish")
                    runOnUiThread {
                        DialogUtils.dismissLoading()
                        fl_makeVideo_preview.visibility = View.VISIBLE
                        videoPath = out
                        toSetRootSize()
                        video_makeVideo_preview.setVideoPath(out)
                        video_makeVideo_preview.start()
                        videoReady = true
                        toPlay()
                    }
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    LogUtils.d("toMergeVideo=>progress=>progress:$progress,progressTime:$progressTime")
                    runOnUiThread {
                        if (progress > 0)
                            DialogUtils.setDialogMsg("合成视频中 ${progress / 10 + 90}%")
                    }
                }

                override fun onCancel() {
                    LogUtils.d("toMergeVideo=>cancel")
                    runOnUiThread {
                        DialogUtils.dismissLoading()
                    }
                }
            })
    }

    /**
     * 循环执行格式化视频文件
     */
    private fun toCycleFormatVideo(maxHeight: Int) {
        toFormatVideo(maxHeight, selected[currentIndex].filePath!!, object : FormatVideoCallback {
            override fun formatSuccess(path: String) {
                selected[currentIndex].filePath = path
                if (currentIndex == selected.size - 1) {
                    currentIndex = 0
                    runOnUiThread {
                        DialogUtils.setDialogMsg("合成视频中 90%")
                    }
                    toFormat2Ts()
                } else {
                    currentIndex++
                    toCycleFormatVideo(maxHeight)
                }
            }

            override fun formatFailed(error: String?) {
            }

            override fun formatProgress(progress: Int) {
                runOnUiThread {
                    if (progress > 0)
                        DialogUtils.setDialogMsg("合成视频中 ${(100 / selected.size * currentIndex + progress / selected.size) / 10 * 9 + (100 / selected.size * currentIndex + progress / selected.size) % 10}%")
                }
            }

            override fun formatCancel() {
            }
        })
    }

    /**
     * 视频转成ts文件进行合并
     */
    private fun toFormat2Ts(isPic: Boolean = false) {
        val videoLists = mutableListOf<String>()
        val folder = getExternalFilesDir("pic2video").toString()
        val folderFile = File(folder)
        if (folderFile.exists() && folderFile.isDirectory && folderFile.listFiles().isNotEmpty()) {
            for (file in folderFile.listFiles()) {
                val path = file.path!!
                val fileName = path.substring(path.lastIndexOf("/") + 1, path.length)
                if (isPic) {
                    if (fileName.startsWith("pic_") && fileName.endsWith(".mp4")) {
                        videoLists.add(path)
                    }
                } else {
                    if (fileName.startsWith("out_") && fileName.endsWith(".mp4")) {
                        videoLists.add(path)
                    }
                }
            }
        }
        todoFormat2Ts(videoLists)
    }

    /**
     * 循环格式化为ts文件,方便合并
     */
    private fun todoFormat2Ts(videoLists: MutableList<String>) {
        val ts =
            getExternalFilesDir("pic2video").toString() + "/ts_${System.currentTimeMillis()}.ts"
        val commands =
            "ffmpeg -y -i ${videoLists[currentIndex]} -c copy -bsf:v h264_mp4toannexb -f mpegts $ts"
                .split(" ")
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands.toTypedArray())
            .subscribe(object : RxFFmpegSubscriber() {
                override fun onError(message: String?) {
                    LogUtils.d("todoFormat2Ts=>error:$message")
                }

                override fun onFinish() {
                    LogUtils.d("todoFormat2Ts=>finish")
                    if (currentIndex == videoLists.size - 1) {
                        currentIndex = 0
                        toMergeVideo()
                    } else {
                        currentIndex++
                        todoFormat2Ts(videoLists)
                    }
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    LogUtils.d("todoFormat2Ts=>progress=>progress:$progress,progressTime:$progressTime")
                }

                override fun onCancel() {
                    LogUtils.d("todoFormat2Ts=>cancel")
                }
            })
    }

    /**
     * 格式化视频,360*XXX,不足区域填充黑色
     */
    private fun toFormatVideo(
        maxHeight: Int,
        path: String,
        callback: FormatVideoCallback,
        result: String? = null
    ) {
        val out = result
            ?: getExternalFilesDir("pic2video").toString() + "/out_${System.currentTimeMillis()}.mp4"
        val mediaInfo = RxFFmpegInvoke.getInstance().getMediaInfo(path)
        val width = MediaInfoUtil.getMediaWidth4Video(mediaInfo)
        val height = MediaInfoUtil.getMediaHeight4Video(mediaInfo)
        val resultHeight = (height * (360.0 / width)).toInt()
        val resultTop = (maxHeight - resultHeight) / 2
        LogUtils.d("=====1111>${Gson().toJson(MediaInfoUtil.format4Video(mediaInfo))}")
        LogUtils.d("=====1111>width:$width,height:$height,resultHeight:$resultHeight,maxHeight:$maxHeight")
        val commands =
            "ffmpeg -i $path -vf scale=360:$resultHeight,pad=width=360:height=$maxHeight:x=0:y=$resultTop:color=black -r 30 -b:v 500k -c:v libx264 -x264opts keyint=30 $out"
                .split(" ")
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands.toTypedArray())
            .subscribe(object : RxFFmpegSubscriber() {
                override fun onError(message: String?) {
                    LogUtils.d("toFormatVideo=>error:$message")
                    callback.formatFailed(message)
                }

                override fun onFinish() {
                    LogUtils.d("toFormatVideo=>finish")
                    callback.formatSuccess(out)
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    LogUtils.d("toFormatVideo=>progress=>progress:$progress,progressTime:$progressTime")
                    if (progress > 0) {
                        callback.formatProgress(progress)
                    }
                }

                override fun onCancel() {
                    LogUtils.d("toFormatVideo=>cancel")
                    callback.formatCancel()
                }
            })
    }

    /**
     * 格式化数据,获取最大高度
     * @return +1的原因是防止计算四舍五入出现误差,让最大值确保最大
     */
    private fun toGetVideoMaxHeight(): Int {
        var maxHeight = 480
        for (data in selected) {
            val mediaInfo = RxFFmpegInvoke.getInstance().getMediaInfo(data.filePath!!)
            val mediaHeight4Video = MediaInfoUtil.getMediaHeight4Video(mediaInfo)
            val mediaWidth4Video = MediaInfoUtil.getMediaWidth4Video(mediaInfo)
            val currentHeight = ((360.0 / mediaWidth4Video) * mediaHeight4Video).toInt()
            if (currentHeight > maxHeight) {
                maxHeight = currentHeight
            }
        }
        return maxHeight + 2
    }

    /**
     * 调节VideoView的音量
     */
    @SuppressLint("DiscouragedPrivateApi")
    private fun setVideoViewVolume2Low() {
        try {
            val forName = Class.forName("android.widget.VideoView")
            val field = forName.getDeclaredField("mMediaPlayer")
            field.isAccessible = true
            val mMediaPlayer = field.get(video_makeVideo_preview) as MediaPlayer
            val videoVolume = Argument.getVideoVolume()
            mMediaPlayer.setVolume(videoVolume, videoVolume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var isPause = false
    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
        if (isPause) {
            isPause = false
            video_makeVideo_preview.seekTo(0)
            video_makeVideo_preview.start()
        }
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
        isPause = true
        video_makeVideo_preview.pause()
        MusicMediaPlayerUtil.getMediaPlayer()?.pause()
    }

    override fun destroy() {
        MusicMediaPlayerUtil.release()
        val intent = Intent(this, DeleteFileIntentService::class.java)
        startService(intent)
        MusicMediaPlayerUtil.release()
        mHandler.removeMessages(0)
        mHandler4VideoText.removeMessages(0)
        mHanlder4WaitDeleteFile.removeMessages(0)
        RxFFmpegInvoke.getInstance().onClean()
        RxFFmpegInvoke.getInstance().onDestroy()
        RxFFmpegInvoke.getInstance().exit()
    }
}