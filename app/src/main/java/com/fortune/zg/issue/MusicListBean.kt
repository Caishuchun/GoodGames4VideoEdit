package com.fortune.zg.issue

data class MusicListBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val list: List<MusicList>,
        val paging: Paging
    ) {
        data class MusicList(
            val author: String,
            val cover: String,
            val duration: Int,
            var is_fav: Int,
            val music_id: Int,
            val name: String,
            val path: String
        )

        data class Paging(
            val count: Int,
            val limit: Int,
            val page: Int
        )
    }
}