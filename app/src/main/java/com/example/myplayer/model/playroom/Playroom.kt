package com.example.myplayer.model.playroom

import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "playrooms")
data class Playroom(
    @PrimaryKey var r_id: String,
    var r_name: String,
    var r_avatar: String? = "",
    var r_introduction: String? = "",
    var current_url: String? = ""
)