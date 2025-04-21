package com.example.myplayer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myplayer.framework.chat.ChatScreen
import com.example.myplayer.framework.friend.FriendsScreen
import com.example.myplayer.framework.me.SettingsScreen
import com.example.myplayer.framework.playroom.playroomScreen

////11
@Composable
fun HomeScreen() {
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
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
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
            composable("Me")       { SettingsScreen() }
            composable("playroom") { playroomScreen() }
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



