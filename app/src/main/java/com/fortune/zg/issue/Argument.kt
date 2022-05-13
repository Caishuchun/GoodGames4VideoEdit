package com.fortune.zg.issue

object Argument {
    /**
     * 重置
     */
    fun reset() {
        videoVolume = 0f
        musicVolume = 0.8f
        musicData = null
    }

    // 视频原音音量
    private var videoVolume = 0f

    //配乐音量大小
    private var musicVolume = 0.8f

    //音乐数据
    private var musicData: MusicListBean.Data.MusicList? = null

    /**
     * 音乐数据相关
     */
    fun setMusicData(musicData: MusicListBean.Data.MusicList) {
        this.musicData = musicData
    }

    fun getMusicData() = this.musicData

    /**
     * 配乐音量相关
     */
    fun setMusicVolume(musicVolume: Float) {
        this.musicVolume = musicVolume
    }

    fun getMusicVolume() = this.musicVolume

    /**
     * 视频原音音量相关
     */
    fun setVideoVolume(videoVolume: Float) {
        this.videoVolume = videoVolume
    }

    fun getVideoVolume() = this.videoVolume
}