package com.carlauncher.data.model

data class NotificationInfo(
    val id: Int,
    val title: String,
    val content: String,
    val appName: String,
    val postTime: Long,
    val priority: Int
)
