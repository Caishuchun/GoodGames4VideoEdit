package com.fortune.zg.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SearchHis::class], version = 100, exportSchema = false)
abstract class SearchHisDataBase : RoomDatabase() {
    abstract fun searchHisDao(): SearchHisDao

    companion object {

        @Volatile
        private var INSTANCE: SearchHisDataBase? = null

        fun getDataBase(context: Context): SearchHisDataBase {
            return INSTANCE ?: synchronized(this) {
                var instance = Room.databaseBuilder(
                    context.applicationContext,
                    SearchHisDataBase::class.java,
                    "search_his.db"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }

    }
}