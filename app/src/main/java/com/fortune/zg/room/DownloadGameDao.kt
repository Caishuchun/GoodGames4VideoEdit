package com.fortune.zg.room

import androidx.room.*

@Dao
interface DownloadGameDao {
    @get:Query("SELECT * FROM download_game_table ORDER BY id DESC")
    val all: List<DownloadGame>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(downloadGame: DownloadGame)

    @Delete
    fun delete(downloadGame: DownloadGame)

    @Query("DELETE FROM download_game_table")
    fun deleteAll()
}