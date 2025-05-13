package com.example.myplayer.framework.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myplayer.model.playroom.Playroom
import kotlinx.coroutines.flow.Flow


@Dao
interface PlayroomDao {
    @Query("SELECT * FROM playrooms")
    fun getAllPlayrooms(): Flow<List<Playroom>>

    @Query("SELECT * FROM playrooms WHERE r_id = :r_id")
    suspend fun getPlayroom(r_id: String): Playroom?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayroom(playroom: Playroom):Unit

    @Update
    suspend fun updatePlayroom(playroom: Playroom): Int

    @Query("DELETE FROM playrooms WHERE r_id = :r_id")
    suspend fun deletePlayroom(r_id: String): Int


}
