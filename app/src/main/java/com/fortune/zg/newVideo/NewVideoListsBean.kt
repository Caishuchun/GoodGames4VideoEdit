package com.fortune.zg.newVideo

import com.fortune.zg.bean.MvDetailBean

data class NewVideoListsBean(
    val code: Int,
    val `data`: Data?,
    val msg: String = ""
) {
    data class Data(
        val list: List<VideoBean>?,
        val paging: Paging?
    ) {
        data class VideoBean(
            val game_info: GameInfo?,
            val game_url_short: String?,
            val gm_id: Int?,
            val imageList: List<Any>?,
            val is_ad: Int?,
            val is_fav: Int?,
            val is_like: Int?,
            val platform: String?,
            val total_comment: Int?,
            val total_like: Int?,
            val total_share: Int?,
            val total_view: Int?,
            val user_avatar: String?,
            val user_name: String?,
            val videoList: List<Any>?,
            val video_cover: String?,
            val video_cover_height: Int?,
            val video_cover_width: Int?,
            val video_desc: String?,
            val video_file: String?,
            val video_id: Int?,
            val video_name: String?,
            val video_pos: Int?,
            val video_type: Int?,
            val video_update_time: String?,
            var game_gift: MvDetailBean.DataBean.GameGiftBean?
        ) {

            data class GameGiftBean(
                var id: Int?,
                var desc: String?,
                var last: Int?,
                var total: Int?,
                var count_down: Int?
            )

            data class GameInfo(
                val android_down_url: String?,
                val android_package_name: String?,
                val android_package_size: String?,
                val android_version_name: String?,
                val android_version_number: String?,
                val game_icon: String?,
                val ios_down_url: String?,
                val pc_gift:String?
            )

            data class Image(
                val index: Int?,
                val url: String?
            )
        }

        data class Paging(
            val count: Int?,
            val limit: Int?,
            val page: Int?
        )
    }
}