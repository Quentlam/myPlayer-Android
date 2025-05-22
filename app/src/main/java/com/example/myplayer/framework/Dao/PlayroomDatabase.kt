package com.example.myplayer.framework.Dao

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [
        com.example.myplayer.model.playroom.Playroom::class,
        com.example.myplayer.model.playroom.PlayroomContent::class,
        com.example.myplayer.model.LoginAccount::class
               ],
    version = 6,
    exportSchema = false
)
abstract class PlayroomDatabase : RoomDatabase() {
    abstract fun playroomDao(): PlayroomDao
    abstract fun playroomContentDao(): PlayroomContentDao
    abstract fun accountDao(): AccountDao
}


