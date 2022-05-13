package com.fortune.zg.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DownloadGame::class], version = 100, exportSchema = false)
abstract class DownloadGameDataBase : RoomDatabase() {
    abstract fun downloadGameDao(): DownloadGameDao

    companion object {

        @Volatile
        private var INSTANCE: DownloadGameDataBase? = null

        fun getDataBase(context: Context): DownloadGameDataBase {
            return INSTANCE ?: synchronized(this) {
                var instance = Room.databaseBuilder(
                    context.applicationContext,
                    DownloadGameDataBase::class.java,
                    "download_game.db"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }

    }
}