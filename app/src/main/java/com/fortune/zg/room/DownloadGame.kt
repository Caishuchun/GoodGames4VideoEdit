package com.fortune.zg.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_game_table")
class DownloadGame(
    @ColumnInfo(name = "video_id") val video_id: Int,
    @ColumnInfo(name = "video_name") val video_name: String,
    @ColumnInfo(name = "game_icon") val game_icon: String,
    @ColumnInfo(name = "game_size") val game_size: Long,
    @ColumnInfo(name = "game_download_url") val game_download_url: String,
    @ColumnInfo(name = "game_package_name") val game_package_name: String
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0
}