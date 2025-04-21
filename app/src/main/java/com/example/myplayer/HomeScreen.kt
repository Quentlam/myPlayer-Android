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
////11
@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem("好友", Icons.Default.Person, "friends"),
        BottomNavItem("聊天", Icons.Default.MailOutline, "chat"),
        BottomNavItem("设置", Icons.Default.Settings, "settings"),
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
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = "friends",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("friends")  { FriendsScreen() }
            composable("chat")     { ChatScreen() }
            composable("settings") { SettingsScreen() }
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

@Composable
fun FriendsScreen() {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("搜索好友") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { /* 搜索好友逻辑 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("搜索")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 好友列表
    }
}

@Composable
fun ChatScreen() {
    // 聊天界面实现
    Text("聊天界面")
}

@Composable
fun SettingsScreen() {
    // 设置界面实现
    Text("设置界面")
}