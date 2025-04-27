package com.example.myplayer.network

import com.example.myplayer.framework.Dao.PlayroomDatabase
import android.content.Context
import androidx.room.Room


object BaseInformation {
    const val HOST = "https://www.myplayer.merlin.xin/api"//这个是服务器的基地址
    var isUserLoggedIn = false//其他变量还暂时用不到
    var currentUserId = ""
    var account = ""
    var password = ""
}



object DatabaseProvider {
    @Volatile
    private var INSTANCE: PlayroomDatabase? = null

    fun getDatabase(context: Context): PlayroomDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                PlayroomDatabase::class.java,
                "playroom_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}