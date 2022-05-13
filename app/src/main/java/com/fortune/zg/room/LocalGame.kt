package com.fortune.zg.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_game_table")
class LocalGame(
    @ColumnInfo(name = "game_id") val game_id: Int,
    @ColumnInfo(name = "game_name") val game_name: String,
    @ColumnInfo(name = "game_desc") val game_desc: String,
    @ColumnInfo(name = "game_tag") val game_tag: String,
    @ColumnInfo(name = "game_cover") val game_cover: String,
    @ColumnInfo(name = "game_hits") val game_hits: Int,
    @ColumnInfo(name = "game_system") val game_system: Int,
    @ColumnInfo(name = "game_badge") val game_badge: String,
    @ColumnInfo(name = "game_update_time") val game_update_time: Int
){
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0
}