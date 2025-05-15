package com.example.myplayer.framework.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplayer.model.playroom.PlayroomContent
import kotlinx.coroutines.flow.Flow


@Dao
interface PlayroomContentDao {
    @Query("SELECT * FROM playroomContent WHERE r_id = :r_id ORDER BY time ASC")
    fun getCurrentPlayroomContent(r_id : String): Flow<List<PlayroomContent>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCurrentPlayroomContent(playroomContent: PlayroomContent):Unit

}