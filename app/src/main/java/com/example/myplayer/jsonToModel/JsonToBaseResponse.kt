package com.example.myplayer.jsonToModel

import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.BaseSentJsonData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Response

class JsonToBaseResponse<T>(val response: Response) {
    fun getResponseData(): BaseResponseJsonData<T> {
        val responseBody = response.body?.string()
        val gson = Gson()
        val type = object : TypeToken<BaseResponseJsonData<T>>() {}.type
        // 解析 JSON
        return gson.fromJson(responseBody, type)
    }
}
//这个类时为了把响应体的Json数据，转换成我们BaseData里的BaseResponseJsonData这个类。

//简单来说，就是把响应体转换成我们自己的类