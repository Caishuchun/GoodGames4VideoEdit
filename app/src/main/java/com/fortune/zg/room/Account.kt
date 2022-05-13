package com.fortune.zg.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_table")
class Account(
    @PrimaryKey
    @ColumnInfo(name = "account") val account: String
)