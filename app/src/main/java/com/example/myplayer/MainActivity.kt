package com.example.myplayer
    import android.content.pm.PackageManager
    import android.os.Build
    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.enableEdgeToEdge
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.core.content.ContextCompat
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    import com.example.myplayer.framework.NotificationHelper
    import com.example.myplayer.ui.theme.MyPlayerTheme
    import com.example.myplayer.userInfo.isConnected
    import com.example.myplayer.userInfo.isLogin


    class MainActivity : ComponentActivity() {
        private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // 权限获取成功
            } else {
                // 权限被拒绝
            }
        }


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // 请求通知权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            NotificationHelper.createNotificationChannel(this)
            enableEdgeToEdge()
            setContent {
                MyPlayerTheme {
                    AppNavigation()
                }
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        var isLoggedIn by remember { mutableStateOf(false) }

        NavHost(
            navController = navController,
            startDestination = "login"
        ) {
            composable("login") {
                LoginScreen(onLoginSuccess = {
                    isLoggedIn = true
                    isLogin = true
                                             }, navController = navController,
                    onLogout = {
                        isLogin = false
                        isLoggedIn = false
                        isConnected = false
                    })
            }

            composable("register") {
                RegisterScreen(
                    onNavigateToLogin = { navController.navigate("Login") }
                )
            }

            composable("main") {
                HomeScreen(onLogout = {
                    isLogin = false
                    isLoggedIn = false
                    isConnected = false
                })
            }
        }


        if (isLoggedIn)
        {
            HomeScreen(onLogout = {
                isLogin = false
                isLoggedIn = false
                isConnected = false
            })
        }
        else
        {
            LoginScreen(onLoginSuccess = {
                isLoggedIn = true
                isLogin = true
                                         }, navController = navController,
                onLogout = {
                    isLogin = false
                isLoggedIn = false
                isConnected = false
                })
        }

    }