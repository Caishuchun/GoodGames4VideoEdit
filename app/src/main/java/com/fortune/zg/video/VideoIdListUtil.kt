package com.fortune.zg.video

import com.fortune.zg.bean.VideoIdListBean

/**
 * 由于Intent传递数据过大,防止报错Transaction too large
 */
object VideoIdListUtil {

    private var videoList = mutableListOf<VideoIdListBean.Data.Video>()

    /**
     * 取数据
     */
    fun getVideoIdList() = videoList

    /**
     * 存数据
     */
    fun setVideoIdList(videoList: MutableList<VideoIdListBean.Data.Video>) {
        this.videoList.clear()
        this.videoList.addAll(videoList)
    }
}