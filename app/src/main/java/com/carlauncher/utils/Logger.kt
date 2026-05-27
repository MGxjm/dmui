package com.carlauncher.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val TAG = "CarLauncher"
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    fun init(context: Context) {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        logFile = File(logDir, "app_${fileDateFormat.format(Date())}.log")
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLog("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLog("INFO", tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        writeLog("WARN", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val fullMessage = if (throwable != null) {
            "$message\n${Log.getStackTraceString(throwable)}"
        } else {
            message
        }
        writeLog("ERROR", tag, fullMessage)
    }

    private fun writeLog(level: String, tag: String, message: String) {
        try {
            logFile?.let { file ->
                PrintWriter(FileWriter(file, true)).use { writer ->
                    writer.println("${dateFormat.format(Date())} [$level] $tag: $message")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    fun getLogContent(): String {
        return try {
            logFile?.readText() ?: ""
        } catch (e: Exception) {
            "无法读取日志: ${e.message}"
        }
    }

    fun clearLogs() {
        try {
            logFile?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
}
