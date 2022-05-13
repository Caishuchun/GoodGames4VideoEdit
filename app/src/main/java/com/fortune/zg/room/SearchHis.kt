package com.fortune.zg.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_his_table")
class SearchHis(
    @PrimaryKey
    @ColumnInfo(name = "str") val str: String
)