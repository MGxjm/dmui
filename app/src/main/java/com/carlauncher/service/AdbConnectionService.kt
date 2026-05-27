package com.carlauncher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.carlauncher.manager.AdbManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AdbConnectionService : Service() {
    
    companion object {
        private const val TAG = "AdbConnectionService"
        const val ACTION_CONNECT = "com.carlauncher.action.CONNECT_ADB"
        const val ACTION_DISCONNECT = "com.carlauncher.action.DISCONNECT_ADB"
        const val ACTION_PAIR = "com.carlauncher.action.PAIR_ADB"
        const val EXTRA_IP = "ip"
        const val EXTRA_PORT = "port"
        const val EXTRA_PAIRING_CODE = "pairing_code"
    }
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var adbManager: AdbManager
    
    override fun onCreate() {
        super.onCreate()
        adbManager = AdbManager(this)
        adbManager.setPairingCallback { token ->
            showToast("需要配对，请在设备上确认授权")
            Log.d(TAG, "Pairing token received: $token")
        }
        Log.d(TAG, "AdbConnectionService created")
        Log.d(TAG, "ADB permission enabled: ${adbManager.hasAdbPermission()}")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_CONNECT -> {
                    val ip = it.getStringExtra(EXTRA_IP)
                    val port = it.getIntExtra(EXTRA_PORT, 5555)
                    scope.launch {
                        try {
                            showToast("正在连接ADB...")
                            Log.d(TAG, "Attempting to connect to ADB at ${ip ?: "127.0.0.1"}:$port")
                            
                            val adbEnabled = adbManager.hasAdbPermission()
                            if (!adbEnabled) {
                                showToast("请先在开发者选项中开启无线调试")
                                Log.e(TAG, "ADB permission not enabled")
                                sendConnectionResult(false, "ADB无线调试未开启")
                                return@launch
                            }
                            
                            val success = adbManager.connectWirelessAdb(ip, port)
                            if (success) {
                                showToast("ADB连接成功")
                                Log.d(TAG, "ADB connected successfully")
                                sendConnectionResult(true, "连接成功")
                            } else {
                                showToast("ADB连接失败，请检查设备是否已配对")
                                Log.e(TAG, "ADB connection failed")
                                sendConnectionResult(false, "连接失败，请检查无线调试是否已配对")
                            }
                        } catch (e: Exception) {
                            showToast("ADB连接异常: ${e.message}")
                            Log.e(TAG, "ADB connection error", e)
                            sendConnectionResult(false, "连接异常: ${e.message}")
                        }
                    }
                }
                ACTION_DISCONNECT -> {
                    adbManager.disconnectAdb()
                    showToast("已断开ADB连接")
                    sendBroadcast(Intent("com.carlauncher.action.ADB_DISCONNECTED"))
                }
                ACTION_PAIR -> {
                    showToast("请在开发者选项中手动配对设备")
                    Log.w(TAG, "Pairing requires manual setup in developer options")
                    sendPairingResult(false)
                }
                else -> {
                    Log.w(TAG, "Unknown action: ${it.action}")
                }
            }
        }
        return START_STICKY
    }
    
    private fun sendConnectionResult(success: Boolean, message: String) {
        val intent = Intent("com.carlauncher.action.ADB_CONNECTION_RESULT")
        intent.putExtra("success", success)
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }
    
    private fun sendPairingResult(success: Boolean) {
        val intent = Intent("com.carlauncher.action.ADB_PAIRING_RESULT")
        intent.putExtra("success", success)
        sendBroadcast(intent)
    }
    
    private fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(this@AdbConnectionService, message, Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        adbManager.disconnectAdb()
        Log.d(TAG, "AdbConnectionService destroyed")
    }
}