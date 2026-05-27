package com.carlauncher.manager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SplitScreenManager(private val context: Context) {

    companion object {
        private const val TAG = "SplitScreenManager"
    }

    private val adbManager = AdbManagerHolder.get(context)

    enum class SplitMode {
        LEFT_RIGHT, TOP_BOTTOM, FREE
    }

    suspend fun startSplitScreen(packageName1: String, packageName2: String, mode: SplitMode = SplitMode.LEFT_RIGHT): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting split screen for $packageName1 and $packageName2")

            val commands = mutableListOf<String>()
            
            commands.add("am start -n $packageName1/.MainActivity")
            
            kotlinx.coroutines.delay(500)
            
            val cmd2 = when (mode) {
                SplitMode.LEFT_RIGHT -> "am start -n $packageName2/.MainActivity --ez split_screen true"
                SplitMode.TOP_BOTTOM -> "am start -n $packageName2/.MainActivity --ez split_screen true --ez top_bottom true"
                SplitMode.FREE -> "am start -n $packageName2/.MainActivity --ez freeform true"
            }
            commands.add(cmd2)
            
            var success = true
            for (cmd in commands) {
                val result = adbManager.executeShell(cmd)
                if (result.contains("Error")) {
                    Log.e(TAG, "Failed to execute: $cmd, result: $result")
                    success = false
                }
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start split screen", e)
            false
        }
    }

    suspend fun exitSplitScreen(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Exiting split screen")
            val result = adbManager.executeShell("am broadcast -a android.intent.action.SPLIT_SCREEN_EXIT")
            !result.contains("Error")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to exit split screen", e)
            false
        }
    }
    
    fun isSplitScreenMode(): Boolean {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                if (context is android.app.Activity) {
                    return context.isInMultiWindowMode
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check split screen mode", e)
        }
        return false
    }
}