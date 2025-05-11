package com.example.myplayer.network

import com.example.myplayer.framework.Dao.PlayroomDatabase
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.room.Room
import com.example.myplayer.model.playroom.Playroom


object BaseInformation {
    const val HOST = "https://www.myplayer.merlin.xin/api"//这个是服务器的基地址
    var isUserLoggedIn = false//其他变量还暂时用不到
    var currentUserId = ""
    var account = ""
    var password = ""
    var token = ""
    var currentRoom  = Playroom(
        r_id = "0000000",
        r_name = "默认播放室",
        r_avatar = "没有成员",
        r_introduction = "默认播放室",
        current_url = "none"
    ) //创建一个默认的播放室

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