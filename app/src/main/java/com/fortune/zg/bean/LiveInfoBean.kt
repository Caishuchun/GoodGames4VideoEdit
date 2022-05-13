package com.fortune.zg.bean

data class LiveInfoBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val anchor_name: String,
        val cover: String,
        val game_down_info: GameDownInfo,
        val game_id: Int,
        val game_system: Int,
        val im_group_id: Long,
        val intro: String,
        val live_id: Int,
        val online_user: Int,
        val play_url: String,
        val status: Int,
        val tag: String,
        val game_cover: String,
        val game_badge: String
    ) {
        data class GameDownInfo(
            val android_down_url: String,
            val android_package_name: String,
            val android_package_size: String,
            val android_version_name: String,
            val android_version_number: String,
            val ios_down_url: String,
            val windows_domain: String
        )
    }
}