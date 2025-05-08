package com.example.myplayer.model.playroom

import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "playrooms")
data class Playroom(
    @PrimaryKey val r_id: String,
    val r_name: String,
    val r_avatar: String,
    val r_introduction: String,
    var current_url: String
)