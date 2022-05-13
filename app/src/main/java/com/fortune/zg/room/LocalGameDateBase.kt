package com.fortune.zg.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocalGame::class], version = 100, exportSchema = false)
abstract class LocalGameDateBase : RoomDatabase() {
    abstract fun localGameDao(): LocalGameDao

    companion object {

        @Volatile
        private var INSTANCE: LocalGameDateBase? = null

        fun getDataBase(context: Context): LocalGameDateBase {
            return INSTANCE ?: synchronized(this) {
                var instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalGameDateBase::class.java,
                    "local_game.db"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }

    }
}