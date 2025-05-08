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

@Composable
fun roomScreen(
    roomId: String,
    videoUrl: String
) {
    var currentTab by remember { mutableStateOf(0) }
    var messageInput by remember { mutableStateOf("") }
    val danmuList = remember { mutableStateListOf<String>() }
    val members = remember { mutableStateListOf("用户1", "用户2") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 视频播放器区域 - 设置固定高度或者合适的比例
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f/9f)  // 使用16:9的视频比例
        ) {
            ExoPlayerView(
                context = LocalContext.current,
                videoUrl = videoUrl,
                lifecycleOwner = LocalLifecycleOwner.current
            )
        }

        // 中层切换面板
        Column(
            modifier = Modifier
                .weight(1f)  // 让中间部分填充剩余空间
                .fillMaxWidth()
        ) {
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

            // 列表内容区域
            Box(modifier = Modifier.weight(1f)) {
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
        }

        // 底部输入框
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
                    if (messageInput.isNotBlank()) {
                        danmuList.add(messageInput)
                        messageInput = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("发送")
            }
        }
    }
}
