package com.fortune.zg.issue

import android.media.MediaPlayer

/**
 * 配乐播放器工具类,确保仅有唯一实例
 */
object MusicMediaPlayerUtil {

    private var mMediaPlayer: MediaPlayer? = null

    /**
     * 获取MediaPlayer
     */
    fun getMediaPlayer() = mMediaPlayer

    /**
     * 初始化MediaPlayer
     */
    fun initMediaPlayer(): MediaPlayer? {
        release()
        mMediaPlayer = MediaPlayer()
        mMediaPlayer?.setVolume(0.8f, 0.8f)
        return mMediaPlayer
    }

    /**
     * 资源回收
     */
    fun release() {
        mMediaPlayer?.pause()
        mMediaPlayer?.release()
        mMediaPlayer = null
    }
}