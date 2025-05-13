package com.example.myplayer.framework.friend

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.network.networkAPI.GetRequest
import com.example.myplayer.model.UserInfo
import com.example.myplayer.userInfo
import com.example.myplayer.webSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberImagePainter

val TAG = "FriendsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun FriendsScreen() {
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var friendList by remember { mutableStateOf<List<UserInfo>?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    //搜索栏状态
    var active by remember { mutableStateOf(false) }

    // 加载好友列表
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                getFriendList(coroutineScope)
                friendList = userInfo.friendList
            }
        } finally {
            isLoading = false
        }
    }
    /*
        coroutineScope.launch {
        withContext(Dispatchers.IO){
            com.example.myplayer.framework.chat.getFriendList(coroutineScope)
        }
    }
     */

    Scaffold (
        topBar = {
            Column {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { active = false },
                    active = active,
                    onActiveChange = { active = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text("搜索好友")
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                    },
                    trailingIcon = {
                        if(active){
                            Icon(
                                modifier = Modifier.clickable {
                                    if (searchQuery.isNotEmpty()){
                                        searchQuery = ""
                                    } else {
                                        active = false
                                    }
                                },
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Icon")
                        }
                    }
                ) {
                    // 搜索建议内容（可为空）
                }
            }
        }
    ){ innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            /*
                TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("搜索好友") },
                modifier = Modifier.fillMaxWidth()
            )

                Button(
                onClick = { /* 搜索好友逻辑 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("搜索")
            }
            */

            //Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // 好友列表显示
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                friendList == null -> {
                    Text("加载失败", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                friendList!!.isEmpty() -> {
                    Text("暂无好友", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                else -> {
                    FriendListView(
                        friends = friendList!!.filter {
                            it.u_name.contains(searchQuery, ignoreCase = true)
                        },
                        onFriendClick = { friend ->
                            // 处理好友点击
                            // navigateToFriendDetail(friend.u_id)
                        }
                    )
                }
            }
        }
        /*
                // 好友列表
        FriendListView(
            friends = userInfo.friendList // 使用真实数据
            /*
                        onFriendClick = { friend ->
                selectedFriend = friend
                currentScreen = ChatScreenState.CHAT_DETAIL
            }
            */

        )
        */

    }
}

@Composable
private fun FriendListView(
    friends: List<UserInfo>, // 修改参数类型
    onFriendClick: (UserInfo) -> Unit // 修改参数类型
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(friends) { friend ->
            FriendListItem(friend = friend, onClick = { onFriendClick(friend) })
        }
    }
}

@Composable
private fun FriendListItem(friend: UserInfo, onClick: () -> Unit) {//好友列表显示
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberImagePainter(
                data = friend.u_avatar,
                builder = {
                    crossfade(true)
                }
            ),
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(text = friend.u_name, style = MaterialTheme.typography.bodyLarge)
    }
}

suspend fun getFriendList(coroutineScope: CoroutineScope){//好友列表逻辑
    try {
        val response = GetRequest(
            interfaceName = "/friend/getfriends",
            queryParams = mapOf()
        ).execute(coroutineScope)
        val type = object : TypeToken<BaseResponseJsonData<List<UserInfo>>>() {}.type
        val data = Gson().fromJson<BaseResponseJsonData<List<UserInfo>>>(response.body?.string(), type)

        if (data.data != null) {
            userInfo.friendList = data.data.also {
                Log.d(TAG, "好友列表更新：${it.size}条记录")
            }
            // 若需要多属性设置才使用apply：
            /*
            userInfo.apply {
                friendList = data.data
                version++
            }
            */
            Log.d(TAG, "好友列表详情：\n${userInfo.friendList?.joinToString("\n") {
                "好友ID：${it.u_id} 姓名：${it.u_name} 头像：${it.u_avatar}"
            } ?: "空列表"}")
        } else {
            Log.e(TAG, "获取好友列表失败：${data.msg}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取好友列表异常：${e.message}")
        throw e
    }
}