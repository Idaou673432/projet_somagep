package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LeakReport::class], version = 4, exportSchema = false)
abstract class LeakDatabase : RoomDatabase() {
    abstract fun leakReportDao(): LeakReportDao

    companion object {
        @Volatile
        private var INSTANCE: LeakDatabase? = null

        fun getDatabase(context: Context): LeakDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeakDatabase::class.java,
                    "somagep_leak_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
