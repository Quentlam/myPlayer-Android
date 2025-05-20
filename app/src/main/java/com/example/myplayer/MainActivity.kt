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
    import com.palankibharat.exo_compose_player.PipInitializer
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
                    PipInitializer(this).initialize()
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
                LoginScreen(
//                    onLoginSuccess =       { navController.navigate("login") },
                    onLoginSuccess =       { },
                    navController  =         navController,
                    onLogout = { isLoggedIn = false }
                )
            }

            composable("register") {
                RegisterScreen(
                    onNavigateToRegister = { navController.navigate("register") }
                )
            }

            composable("main") {
                HomeScreen(onLogout = { isLoggedIn = false })
            }
        }


        if (isLoggedIn)
        {
            HomeScreen(onLogout = { isLoggedIn = false })
        }
        else
        {
            LoginScreen(onLoginSuccess = { isLoggedIn = true }, navController = navController, onLogout = { isLoggedIn = false })
        }

    }

    @Composable
    fun login(
        modifier: Modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    )
    {
        var account by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var responseText by remember { mutableStateOf("") }
        var baseResponse by remember { mutableStateOf<BaseResponseJsonData<String>>(BaseResponseJsonData<String>())}//用来接受响应体


        var response:Response ?= null
        val coroutineScope = rememberCoroutineScope()//创建协程
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(account,onValueChange = {
                currentText->account = currentText//每次都把输入的值给到account
            })
            TextField(password,onValueChange = {
                    currentText->password = currentText//每次都把输入的值给到account
            })

            Button(onClick = {
                val jsonList = listOf(
                    BaseSentJsonData("u_account",account),
                    BaseSentJsonData("u_password",password),
                    )

                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        response = LoginRequest(jsonList,"/login").sendRequest(coroutineScope)
                        if(response!!.isSuccessful)
                        {
                            baseResponse = JsonToBaseResponse<String>(response!!).getResponseData()
                            responseText = response!!.body?.string() ?: ""
                            //baseResponse = JsonToBaseResponse<String>(response!!).getResponseData()
                            BaseInformation.account = account
                            BaseInformation.account = password
                        }
                    }
                }

            })
            {
                Text("登录")
            }

            Spacer(modifier = Modifier.height(8.dp))
            if(baseResponse.code != null)
            {
                Text("${baseResponse?.code}", modifier = Modifier.height(100.dp))
            }
            else
            {
                Text("当前baseResponse为空！", modifier = Modifier.height(100.dp))
            }


        }
    }





    @Composable
    fun Test(
        modifier: Modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        var account by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var enterPassword by remember { mutableStateOf("") }
        var verificationCode by remember { mutableStateOf("") }
        var attributesOfName = listOf("账号", "密码", "确认密码", "验证码")
        var baseResponse by remember { mutableStateOf<BaseResponseJsonData<String>>(BaseResponseJsonData<String>())}//用来接受响应体
        var responseText by remember { mutableStateOf("") }


        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                coroutineScope.launch {
                    //这里使用Dispatchers.IO是为了切换到一个适合I/O操作的线程
                    withContext(Dispatchers.IO) {
                        val response = LoginRequest(
                            listOf(
                                BaseSentJsonData(
                                    "u_account", "1959804282@qq.com"
                                ),
                                BaseSentJsonData(
                                    "u_password", "123456"
                                )
                            ),
                            "/login"
                        ).sendRequest(coroutineScope)//创建一个request对象
                        if (!response!!.isSuccessful) throw IOException("请求失败: ${response.code}")
                        else
                        // 修改后代码片段：
                        if (response != null && response!!.isSuccessful) {
                            try {
                                val responseBody = response!!.body?.string()
                                if (!responseBody.isNullOrEmpty()) {
                                    baseResponse = JsonToBaseResponse<String>(response!!).getResponseData()
                                    withContext(Dispatchers.Main) {
                                        responseText = responseBody
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    responseText = "响应解析异常"
                                }
                            }
                        }
                    }
                }
            })

            {
                Text("测试")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${baseResponse?.code}", modifier = Modifier.height(100.dp))
        }
    }


