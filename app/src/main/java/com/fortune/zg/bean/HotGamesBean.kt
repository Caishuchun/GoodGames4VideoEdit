package com.fortune.zg.bean

data class HotGamesBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val list: List<Game>
    ) {
        data class Game(
            val video_id: Int,
            val video_name: String,
            val game_info: GameInfo
        ) {
            data class GameInfo(
                val android_down_url: String,
                val android_package_name: String,
                val android_package_size: String,
                val android_version_name: String,
                val android_version_number: String,
                val game_icon: String,
                val ios_down_url: String
            )
        }
    }
}