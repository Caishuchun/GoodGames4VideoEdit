package com.fortune.zg.bean

import java.io.Serializable

data class VideoIdListBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) : Serializable {
    data class Data(
        val default_video_id: Int,
        val position: Int,
        val video_id: String,
        val video_list: List<Video>
    ) : Serializable {
        data class Video(
            val video_cover: String,
            val video_cover_height: Int,
            val video_cover_width: Int,
            val video_id: Int
        ) : Serializable
    }
}