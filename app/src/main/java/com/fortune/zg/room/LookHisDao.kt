package com.fortune.zg.room

import androidx.room.*

@Dao
interface LookHisDao {

    @get:Query("SELECT * FROM look_his_table ORDER BY id DESC")
    val all: List<LookHis>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addHis(his: LookHis)

    @Delete
    fun deleteHis(his: LookHis)

    @Query("DELETE FROM look_his_table")
    fun deleteAll()
}