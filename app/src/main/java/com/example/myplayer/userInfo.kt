package com.example.myplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myplayer.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow


object userInfo {
    var currentFriend by  mutableStateOf("")
    var u_name by  mutableStateOf("")
    var u_introduction by mutableStateOf("")
    var u_avatar by  mutableStateOf("")
    var u_id by mutableStateOf("")
    var friendList by mutableStateOf<List<UserInfo>>(emptyList())
    var isConnected by mutableStateOf(false)
}

