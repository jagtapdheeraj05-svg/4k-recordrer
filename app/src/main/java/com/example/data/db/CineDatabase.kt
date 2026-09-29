package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.VideoProject

@Database(entities = [VideoProject::class], version = 1, exportSchema = false)
abstract class CineDatabase : RoomDatabase() {
    abstract fun videoProjectDao(): VideoProjectDao

    companion object {
        @Volatile
        private var INSTANCE: CineDatabase? = null

        fun getDatabase(context: Context): CineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CineDatabase::class.java,
                    "cinepro_video_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
