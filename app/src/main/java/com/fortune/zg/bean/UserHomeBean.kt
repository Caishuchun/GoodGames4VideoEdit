package com.fortune.zg.bean

data class UserHomeBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val is_follow: Int,
        val user_avatar: String,
        val user_desc: String,
        val user_fans: Int,
        val user_follow: Int,
        val user_id: Int,
        val user_name: String,
        val user_video_like: Int,
        val video_list: List<Video>
    ) {
        data class Video(
            val game_url_short: String,
            val imageList: List<Image>,
            val is_ad: Int,
            val item_type: String,
            val total_comment: Int,
            val total_like: Int,
            val total_share: Int,
            val total_view: Int,
            val videoList: List<Video>,
            val video_cover: String,
            val video_cover_height: Int,
            val video_cover_width: Int,
            val video_desc: String,
            val video_file: String,
            val video_id: Int,
            val video_name: String,
            val video_updaare: Int,
            val video_update_time: Int
        ) {
            data class Image(
                val index: Int,
                val url: String
            )

            data class Video(
                val index: Int,
                val video_id: Int
            )
        }
    }
}