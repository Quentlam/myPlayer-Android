package com.example.myplayer

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myplayer.framework.chat.ChatScreen
import com.example.myplayer.framework.chat.GlobalMessageNotifier
import com.example.myplayer.framework.chat.TopMessageBanner
import com.example.myplayer.framework.friend.FriendsScreen
import com.example.myplayer.framework.me.SettingScreen
import com.example.myplayer.framework.playroom.PlayroomNavigation
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val window = (context as? Activity)?.window
    val windowInsetsController = remember(context) {
        window?.let { WindowInsetsControllerCompat(it, it.decorView) }
    }

    val items = listOf(
        BottomNavItem("聊天", Icons.Default.MailOutline, "chat"),
        BottomNavItem("好友", Icons.Default.Person, "friends"),
        BottomNavItem("播放室", Icons.Default.PlayArrow, "playroom"),
        BottomNavItem("我的", Icons.Default.Person, "Me")
    )

    // 状态：是否隐藏底部导航栏，初始false
    var hideBottomBar by remember { mutableStateOf(false) }


    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar {
                    val currentRoute = currentRoute(navController)
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            label = { Text(item.label, fontSize = 11.sp) },
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
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = "chat",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("friends") { FriendsScreen() }
            composable("chat") { ChatScreen() }
            composable("Me") { SettingScreen(onLogout) }
            composable("playroom") {
                PlayroomNavigation(
                    onEnterRoom = {
                        // 进入房间，隐藏底部导航栏
                        windowInsetsController?.hide(WindowInsetsCompat.Type.navigationBars())
                        hideBottomBar = true
                    },
                    onExitRoom = {
                        // 退出房间，显示底部导航栏
                        windowInsetsController?.show(WindowInsetsCompat.Type.navigationBars())
                        hideBottomBar = false
                    }
                )
            }
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



