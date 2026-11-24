package com.haoalert.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TaskEntity::class, TaskEventEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class HaoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        fun create(context: Context): HaoDatabase =
            Room.databaseBuilder(context, HaoDatabase::class.java, "hao_alert.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
