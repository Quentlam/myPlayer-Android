package com.example.myplayer.framework.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 好友数据类
data class Friend(
    val id: String,
    val name: String,
    val avatarUrl: String = ""
)

// 界面状态管理
enum class ChatScreenState {
    FRIEND_LIST, CHAT_DETAIL
}

@Composable
fun ChatScreen() {
    var currentScreen by remember { mutableStateOf(ChatScreenState.FRIEND_LIST) }
    var selectedFriend by remember { mutableStateOf<Friend?>(null) }

    when (currentScreen) {
        ChatScreenState.FRIEND_LIST -> {
            FriendListView(
                friends = remember { generateSampleFriends() },
                onFriendClick = { friend ->
                    selectedFriend = friend
                    currentScreen = ChatScreenState.CHAT_DETAIL
                }
            )
        }
        ChatScreenState.CHAT_DETAIL -> {
            ChatDetailScreen(
                friend = selectedFriend,
                onBack = { currentScreen = ChatScreenState.FRIEND_LIST }
            )
        }
    }
}

@Composable
private fun FriendListView(
    friends: List<Friend>,
    onFriendClick: (Friend) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(friends) { friend ->
            FriendListItem(friend = friend, onClick = { onFriendClick(friend) })
        }
    }
}

@Composable
private fun FriendListItem(friend: Friend, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像占位（实际项目应使用Coil/Glide加载图片）
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(text = friend.name, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailScreen(friend: Friend?, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(friend?.name ?: "聊天") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("与 ${friend?.name} 的聊天界面")
        }
    }
}

// 生成示例好友数据
private fun generateSampleFriends(): List<Friend> {
    return listOf(
        Friend("1", "张三"),
        Friend("2", "李四"),
        Friend("3", "王五"),
        Friend("4", "赵六"),
        Friend("5", "陈七"),
        Friend("6", "林八"),
        Friend("7", "周九"),
        Friend("8", "吴十"),
        Friend("9", "黄十一"),
        Friend("10", "郑十二"),
        // 新增以下好友
        Friend("11", "孙十三"),
        Friend("12", "朱十四"),
        Friend("13", "马十五"),
        Friend("14", "胡十六"),
        Friend("15", "郭十七"),
        Friend("16", "何十八"),
        Friend("17", "高十九"),
        Friend("18", "罗二十"),
        Friend("19", "梁二十一"),
        Friend("20", "宋二十二")
    )
}