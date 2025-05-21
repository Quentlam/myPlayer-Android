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
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import com.example.myplayer.model.BaseInformation.currentRoom


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






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun customTopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSearchFriendPlayroomClick: () -> Unit = {},
    onInviteCodeClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.primaryContainer, // 主题色容器
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp), // 恢复默认高度
                placeholder = {
                    Text(
                        "搜索已经加入的播放室",
                        style = MaterialTheme.typography.bodyLarge // 统一字体风格
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索图标"
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )


            Spacer(modifier = Modifier.width(12.dp))

            // 搜索按钮
            Button(
                onClick = onSearchClick,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "搜索按钮")
                Spacer(modifier = Modifier.width(6.dp))
                Text("搜索")
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 加号按钮，带阴影圆形背景
            Box {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .shadow(4.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新增菜单",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onSearchFriendPlayroomClick()
                        },
                        text = { Text("搜索朋友播放室") },
                        leadingIcon = {
                            Icon(Icons.Default.PersonSearch, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onInviteCodeClick()
                        },
                        text = { Text("邀请码进入") },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}



