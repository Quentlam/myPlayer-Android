package com.example.myplayer.framework.Dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myplayer.model.playroom.Playroom
import com.example.myplayer.framework.Dao.PlayroomDao
@Database(
    entities = [com.example.myplayer.model.playroom.Playroom::class],
    version = 1,
    exportSchema = false
)
abstract class PlayroomDatabase : RoomDatabase() {
    abstract fun playroomDao(): PlayroomDao
}