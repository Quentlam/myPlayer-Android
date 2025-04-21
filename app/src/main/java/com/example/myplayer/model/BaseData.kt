package com.example.myplayer.model


//这个是对应发送给服务器时每个Json变量
//用来发送
data class BaseSentJsonData(
    val name: String? = null,
    val value: Any? = null
)

//这个是对应服务器返回来的每个Json变量
//用来接收
data class BaseResponseJsonData<T>(
    val message: String? = null,
    val code: Int? = null,
    val data: T? = null,
    val token: String? = null
)

