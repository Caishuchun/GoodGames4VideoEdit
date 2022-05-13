package com.fortune.zg.room

import androidx.room.*

@Dao
interface SearchHisDao {

    @get:Query("SELECT * FROM search_his_table")
    val all: List<SearchHis>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addHis(his: SearchHis)

    @Delete
    fun deleteHis(his: SearchHis)

    @Query("DELETE FROM search_his_table")
    fun deleteAll()

}