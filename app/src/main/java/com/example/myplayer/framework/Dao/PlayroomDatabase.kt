package com.example.myplayer.framework.Dao

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [
        com.example.myplayer.model.playroom.Playroom::class,
        com.example.myplayer.model.playroom.PlayroomContent::class
               ],
    version = 4,
    exportSchema = false
)
abstract class PlayroomDatabase : RoomDatabase() {
    abstract fun playroomDao(): PlayroomDao
    abstract fun playroomContentDao(): PlayroomContentDao
}


