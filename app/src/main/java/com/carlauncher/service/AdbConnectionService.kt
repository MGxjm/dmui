package com.carlauncher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.carlauncher.manager.AdbManager
import com.carlauncher.manager.AdbManagerHolder
import com.carlauncher.utils.Logger
import kotlinx.coroutines.*

class AdbConnectionService : Service() {

    companion object {
        private const val TAG = "AdbConnectionService"
        const val ACTION_CONNECT = "com.carlauncher.action.CONNECT_ADB"
        const val ACTION_DISCONNECT = "com.carlauncher.action.DISCONNECT_ADB"
        const val ACTION_CHECK_STATUS = "com.carlauncher.action.CHECK_STATUS"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var adbManager: AdbManager

    override fun onCreate() {
        super.onCreate()
        Logger.init(this)
        adbManager = AdbManagerHolder.get(this)
        Logger.d(TAG, "AdbConnectionService created, connected=${adbManager.isConnected()}")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_CONNECT -> {
                    if (adbManager.isConnected()) {
                        Logger.d(TAG, "Already connected, skip")
                        sendConnectionResult(true, "ADB已连接")
                    } else {
                        Logger.d(TAG, "Connect action received")
                        scope.launch { connectAdb() }
                    }
                }
                ACTION_DISCONNECT -> {
                    Logger.d(TAG, "Disconnect action received")
                    adbManager.disconnect()
                    showToast("已断开ADB连接")
                    sendBroadcast(Intent("com.carlauncher.action.ADB_DISCONNECTED"))
                }
                ACTION_CHECK_STATUS -> {
                    val connected = adbManager.isConnected()
                    showToast("ADB状态: ${if (connected) "已连接" else "未连接"}")
                    Logger.d(TAG, "Status check: connected=$connected")
                }
                else -> {
                    Logger.w(TAG, "Unknown action: ${it.action}")
                }
            }
        }
        return START_STICKY
    }

    private suspend fun connectAdb() {
        try {
            if (adbManager.isConnected()) {
                Logger.d(TAG, "Already connected")
                sendConnectionResult(true, "ADB已连接")
                return
            }

            showToast("正在连接ADB (127.0.0.1:5555)...")
            Logger.d(TAG, "Connecting to ADB...")

            val success = adbManager.connect()
            if (success) {
                showToast("ADB连接成功！")
                Logger.i(TAG, "ADB connected successfully")
                sendConnectionResult(true, "ADB连接成功")
            } else {
                showToast("ADB连接失败，请确保已通过电脑授权ADB调试")
                Logger.e(TAG, "ADB connection failed")
                sendConnectionResult(false, "连接失败，请确保：\n1. 已开启无线调试\n2. 已通过电脑配对授权")
            }
        } catch (e: Exception) {
            showToast("ADB连接异常: ${e.message}")
            Logger.e(TAG, "ADB connection error", e)
            sendConnectionResult(false, "连接异常: ${e.message}")
        }
    }

    private fun sendConnectionResult(success: Boolean, message: String) {
        val intent = Intent("com.carlauncher.action.ADB_CONNECTION_RESULT")
        intent.putExtra("success", success)
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }

    private fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            try {
                Toast.makeText(this@AdbConnectionService, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show toast", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Logger.d(TAG, "AdbConnectionService destroyed")
    }
}
