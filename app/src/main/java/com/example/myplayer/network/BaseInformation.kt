package com.example.myplayer.network

import com.example.myplayer.framework.Dao.PlayroomDatabase
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.room.Room
import com.example.myplayer.framework.playroom.PlayroomMessageHandler
import com.example.myplayer.framework.playroom.PlayroomWebSocketManager
import com.example.myplayer.model.playroom.Inviting
import com.example.myplayer.model.playroom.RequestDetails
import com.example.myplayer.model.playroom.Member
import com.example.myplayer.model.playroom.Playroom


object BaseInformation {
    const val HOST = "https://www.myplayer.merlin.xin/api"//这个是服务器的基地址
    var isUserLoggedIn = false//其他变量还暂时用不到
    var currentUserId = ""
    var account = ""
    var password = ""
    var token = ""
    var roomList = emptyList<Playroom>()
    var currentRoom by mutableStateOf<Playroom>(
        Playroom(
            r_id = "0000000",
            r_name = "Default Play Room",
            r_avatar = "No profile picture",
            r_introduction = "Default Play Room",
            current_url = "none"
        )
    )
    var testUrl2 = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    var testUrl1 = "http://10.60.13.48:9990/Broken Soul Clown 3.mp4"
    var currentMemberList by mutableStateOf<List<Member>>(emptyList())
    var currentRequestList by mutableStateOf<List<RequestDetails>>(emptyList())
}



object DatabaseProvider {
    @Volatile
    private var INSTANCE: PlayroomDatabase? = null

    fun getPlayRoomDatabase(context: Context): PlayroomDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                PlayroomDatabase::class.java,
                "playroom_database"
            )
                .fallbackToDestructiveMigration(true)
                .build()
            INSTANCE = instance
            instance
        }
    }
}