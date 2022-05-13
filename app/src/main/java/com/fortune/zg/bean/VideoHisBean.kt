package com.fortune.zg.bean

data class VideoHisBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val list: List<ListBean>,
        val paging: Paging
    ) {
        data class ListBean(
            val user_name: String,
            val user_avatar: String,
            val check_status: Int,
            val game_url: String,
            val game_url_short: String,
            val is_ad: Int,
            val total_comment: Int,
            val total_like: Int,
            val total_share: Int,
            val total_view: Int,
            val video_cover: String,
            val video_cover_height: Int,
            val video_cover_width: Int,
            val video_desc: String,
            val video_file: String,
            val video_id: Int,
            val video_name: String,
            val video_update_time: String,
            val refuse_reason: String,
            val pc_gift: String,
            val videoList: List<videoListBean>,
            val imageList: List<imageListBean>
        ) {
            data class videoListBean(
                var index: Int,
                var video_id: String
            )

            data class imageListBean(
                var index: Int,
                var url: String
            )
        }

        data class Paging(
            val count: Int,
            val limit: Int,
            val page: Int
        )
    }
}