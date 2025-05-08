package com.example.myplayer.framework.playroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.flow.collect

@Composable
fun roomScreen(
    roomId: String,
    videoUrl: String
) {
//    LaunchedEffect(websocketClient) {
//
//    }
    var currentTab by remember { mutableStateOf(0) }
    var messageInput by remember { mutableStateOf("") }
    val danmuList = remember { mutableStateListOf<String>() }
    val members = remember { mutableStateListOf("用户1", "用户2") }

    Column() {
        Box(modifier = Modifier.weight(0.6f)) {
            ExoPlayerView(
                context = LocalContext.current,
                videoUrl = videoUrl,  // 使用参数传入的视频地址
                lifecycleOwner = LocalLifecycleOwner.current
            )
        }

        // 中层切换面板
        Column(modifier = Modifier.weight(0.3f)) {
            TabRow(selectedTabIndex = currentTab) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text("弹幕") }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text("在线成员") }
                )
            }

            when (currentTab) {
                0 -> LazyColumn {
                    items(danmuList.size) { index ->
                        Text(danmuList[index], modifier = Modifier.padding(8.dp))
                    }
                }
                1 -> LazyColumn {
                    items(members.size) { member ->
                        Text(members[member], modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        // 下层输入框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入弹幕...") }
            )
            Button(
                onClick = {
                    danmuList.add(messageInput)
                    messageInput = ""
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("发送")
            }
        }
    }
    

    

}