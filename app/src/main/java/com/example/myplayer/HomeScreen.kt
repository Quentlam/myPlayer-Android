package com.example.myplayer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myplayer.framework.chat.ChatScreen
import com.example.myplayer.framework.friend.FriendsScreen
import com.example.myplayer.framework.me.SettingScreen
import com.example.myplayer.framework.playroom.PlayroomNavigation

////11
@Composable
fun HomeScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem("好友", Icons.Default.Person, "friends"),
        BottomNavItem("聊天", Icons.Default.MailOutline, "chat"),
        BottomNavItem("播放室", Icons.Default.PlayArrow, "playroom"),
        BottomNavItem("我的", Icons.Default.Person, "Me")
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = currentRoute(navController)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(20.dp)) }, // 缩小图标尺寸
                        label = { Text(item.label, fontSize = 11.sp) }, // 缩小字体
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) {
        innerPadding ->
        NavHost(
            navController,
            startDestination = "friends",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("friends")  { FriendsScreen() }
            composable("chat")     { ChatScreen() }
            composable("Me")       { SettingScreen(onLogout) }
            composable("playroom") { PlayroomNavigation() }
        }
    }
}

@Composable
private fun currentRoute(navController: NavHostController): String? {
    return navController.currentBackStackEntry?.destination?.route
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)



