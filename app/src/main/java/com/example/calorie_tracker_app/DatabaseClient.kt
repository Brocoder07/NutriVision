package com.example.calorie_tracker_app

import android.content.Context
import androidx.room.Room

class DatabaseClient private constructor(context: Context) {

    private val appDatabase: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "AppDatabase"
    ).build()
    companion object {
        @Volatile private var INSTANCE: DatabaseClient? = null

        fun getInstance(context: Context): DatabaseClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseClient(context).also { INSTANCE = it }
            }
        }
    }
    fun getAppDatabase(): AppDatabase {
        return appDatabase
    }
}
