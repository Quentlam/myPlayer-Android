package com.example.myplayer.framework.playroom

import android.app.Activity
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.privacysandbox.tools.core.model.Type
import com.example.myplayer.model.BaseInformation.currentRoom

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun PlayroomNavigation(
    onEnterRoom: () -> Unit = {},
    onExitRoom: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "playroomList") {
        composable("playroomList") {
            // 退出任何房间时调用
            LaunchedEffect(Unit) {
                onExitRoom()
            }
            playroomListScreen(
                onJoinRoom = { room ->
                    onEnterRoom()
                    currentRoom = room
                    navController.navigate("roomScreen/${room.r_id}")
                }
            )
        }
        composable("roomScreen/{roomId}") { backStackEntry ->
            DisposableEffect(Unit) {
                onEnterRoom()
                onDispose {
                    onExitRoom()
                }
            }
            roomScreen(
                room = currentRoom,
                onBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}







@Composable
fun customTopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSearchFriendPlayroomClick: () -> Unit = {},
    onInviteCodeClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF19E9E)) // 粉色背景
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(color = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isEmpty()) {
                Text(
                    "搜索已经加入的播放室",
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 40.dp, top = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = {
                onSearchClick()
            }
        ) {
            Text("搜索")
        }
        Spacer(modifier = Modifier.width(16.dp))

        // 新增部分：白色加号按钮和菜单
        var expanded by remember { mutableStateOf(false) }
        Box {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp)
                    .background(Color.White, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新增菜单",
                    tint = Color.Black,
                    modifier = Modifier.width(24.dp)
                        .height(24.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onSearchFriendPlayroomClick()
                    },
                    text = { Text("搜索朋友播放室") },
                )
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onInviteCodeClick()
                    },
                    text = { Text("邀请码进入") },
                )

            }
        }
    }
}



