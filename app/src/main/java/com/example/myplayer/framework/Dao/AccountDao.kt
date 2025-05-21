package com.example.myplayer.framework.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplayer.model.LoginAccount


@Dao
interface AccountDao {
    @Query("SELECT * FROM account ORDER BY id DESC LIMIT 1")
    fun getLastInsertedAccount(): LoginAccount

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: LoginAccount):Unit
}