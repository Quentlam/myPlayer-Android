package com.example.myplayer
    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.enableEdgeToEdge
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.wrapContentSize
    import androidx.compose.material3.Button
    import androidx.compose.material3.Text
    import androidx.compose.material3.TextField
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.rememberCoroutineScope
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.unit.dp
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    import com.example.myplayer.jsonToModel.JsonToBaseResponse
    import com.example.myplayer.model.BaseResponseJsonData
    import com.example.myplayer.model.BaseSentJsonData
    import com.example.myplayer.model.BaseInformation
    import com.example.myplayer.network.LoginRequest
    import com.example.myplayer.ui.theme.MyPlayerTheme
    import com.example.myplayer.userInfo.isConnected
    import com.example.myplayer.userInfo.isLogin
    import kotlinx.coroutines.launch
    import okio.IOException
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.withContext
    import okhttp3.Response


    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
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
                LoginScreen(onLoginSuccess = { isLoggedIn = true }, navController = navController,
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
            LoginScreen(onLoginSuccess = { isLoggedIn = true }, navController = navController,
                onLogout = {
                    isLogin = false
                isLoggedIn = false
                isConnected = false
                })
        }

    }