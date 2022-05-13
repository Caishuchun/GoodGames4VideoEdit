package com.fortune.zg.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Account::class], version = 100, exportSchema = false)
abstract class AccountDataBase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    companion object {

        @Volatile
        private var INSTANCE: AccountDataBase? = null

        fun getDataBase(context: Context): AccountDataBase {
            return INSTANCE ?: synchronized(this) {
                var instance = Room.databaseBuilder(
                    context.applicationContext,
                    AccountDataBase::class.java,
                    "account.db"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }

    }
}