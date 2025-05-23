package com.example.myplayer.framework

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myplayer.R

object NotificationHelper {
    private const val CHANNEL_ID = "chat_messages"
    private const val CHANNEL_NAME = "聊天消息"
    private const val CHANNEL_DESCRIPTION = "接收新的聊天消息通知"
    private var notificationId = 0

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // 创建通知
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.merlin)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // 发送通知
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId++, builder.build())
        }
    }
}
