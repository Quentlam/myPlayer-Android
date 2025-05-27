package com.example.myplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "account")
data class LoginAccount(
    @PrimaryKey(autoGenerate = true)val id: Int? = 0,//自增id
    var account: String?,
    var password: String?,
    var isLogin : Boolean?
)
