package com.example.myplayer.framework.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplayer.model.chat.ChatMessage
import com.example.myplayer.model.playroom.PlayroomContent
import kotlinx.coroutines.flow.Flow


@Dao
interface ChatMessageDao {
    @Query("""
        SELECT * FROM chatMessage 
        WHERE (sender_id = :u_id AND accpet_id = :friendId) 
           OR (sender_id = :friendId AND accpet_id = :u_id)
        ORDER BY chat_id ASC
    """)
    fun getUserMessageById(friendId :String,u_id : String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chatMessage ORDER BY chat_id ASC")
    fun getAllMessage(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUserMessageById(chatMessage: ChatMessage):Long
}