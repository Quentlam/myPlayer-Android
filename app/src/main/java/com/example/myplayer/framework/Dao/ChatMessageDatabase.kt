package com.example.myplayer.framework.Dao

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [
        com.example.myplayer.model.chat.ChatMessage::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class ChatMessageDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}


