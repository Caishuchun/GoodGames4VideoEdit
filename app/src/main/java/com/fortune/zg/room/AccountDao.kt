package com.fortune.zg.room

import androidx.room.*

@Dao
interface AccountDao {
    @get:Query("SELECT * FROM account_table")
    val all: List<Account>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAccount(account: Account)

    @Delete
    fun deleteAccount(account: Account)

    @Query("DELETE FROM account_table")
    fun deleteAll()
}