package com.fortune.zg.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LookHis::class], version = 100, exportSchema = false)
abstract class LookHisDataBase : RoomDatabase() {
    abstract fun lookHisDao(): LookHisDao

    companion object {

        @Volatile
        private var INSTANCE: LookHisDataBase? = null

        fun getDataBase(context: Context): LookHisDataBase {
            return INSTANCE ?: synchronized(this) {
                var instance = Room.databaseBuilder(
                    context.applicationContext,
                    LookHisDataBase::class.java,
                    "look_his.db"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }

    }
}