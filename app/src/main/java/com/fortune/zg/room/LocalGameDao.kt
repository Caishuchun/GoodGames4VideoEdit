package com.fortune.zg.room

import androidx.room.*

@Dao
interface LocalGameDao {
    @get:Query("SELECT * FROM local_game_table ORDER BY id DESC")
    val all: List<LocalGame>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(localGame: LocalGame)

    @Delete
    fun delete(localGame: LocalGame)

    @Query("DELETE FROM local_game_table")
    fun deleteAll()
}