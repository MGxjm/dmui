package com.carlauncher.manager

import android.content.Context
import android.util.Log
import com.carlauncher.data.model.AirConditionState
import com.carlauncher.data.model.AirMode
import com.carlauncher.data.model.WindowState
import com.carlauncher.data.model.WindowPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CarServiceManager(private val context: Context) {

    companion object {
        private const val TAG = "CarServiceManager"
    }

    private val adbManager = AdbManagerHolder.get(context)

    private suspend fun executeCarCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Executing car command: $command")
            val result = adbManager.executeShell(command)
            val success = !result.contains("Error") && !result.contains("Exception")
            if (!success) {
                Log.e(TAG, "Command failed: $result")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute car command", e)
            false
        }
    }

    suspend fun turnOnAirCondition(): Boolean {
        return executeCarCommand("am broadcast -a com.carlauncher.AC_POWER --ez power true")
    }

    suspend fun turnOffAirCondition(): Boolean {
        return executeCarCommand("am broadcast -a com.carlauncher.AC_POWER --ez power false")
    }

    suspend fun setAirConditionTemp(temp: Float): Boolean {
        return executeCarCommand("am broadcast -a com.carlauncher.AC_TEMPERATURE --ef temperature $temp")
    }

    suspend fun setAirConditionWindLevel(level: Int): Boolean {
        return executeCarCommand("am broadcast -a com.carlauncher.AC_FAN_SPEED --ei speed $level")
    }

    suspend fun setAirConditionMode(mode: AirMode): Boolean {
        return executeCarCommand("am broadcast -a com.carlauncher.AC_MODE --es mode ${mode.name}")
    }

    suspend fun setAirCondition(state: AirConditionState): Boolean {
        val commands = mutableListOf<String>()
        commands.add("am broadcast -a com.carlauncher.AC_TEMPERATURE --ef temperature ${state.temperature}")
        commands.add("am broadcast -a com.carlauncher.AC_FAN_SPEED --ei speed ${state.windLevel}")
        commands.add("am broadcast -a com.carlauncher.AC_MODE --es mode ${state.mode.name}")
        commands.add("am broadcast -a com.carlauncher.AC_POWER --ez power ${state.isOn}")
        
        var success = true
        for (cmd in commands) {
            if (!executeCarCommand(cmd)) {
                success = false
            }
        }
        return success
    }

    suspend fun getAirConditionState(): AirConditionState {
        return AirConditionState(
            temperature = 24.0f,
            windLevel = 3,
            mode = AirMode.AUTO,
            isOn = true
        )
    }

    suspend fun controlWindow(position: WindowPosition, open: Boolean): Boolean {
        val level = if (open) 100 else 0
        val posName = when (position) {
            WindowPosition.FRONT_LEFT -> "driver_front"
            WindowPosition.FRONT_RIGHT -> "passenger_front"
            WindowPosition.REAR_LEFT -> "driver_rear"
            WindowPosition.REAR_RIGHT -> "passenger_rear"
        }
        return executeCarCommand("am broadcast -a com.carlauncher.WINDOW --es position $posName --ei level $level")
    }

    suspend fun controlAllWindows(open: Boolean): Boolean {
        val level = if (open) 100 else 0
        val positions = listOf("driver_front", "passenger_front", "driver_rear", "passenger_rear")
        var success = true
        for (pos in positions) {
            if (!executeCarCommand("am broadcast -a com.carlauncher.WINDOW --es position $pos --ei level $level")) {
                success = false
            }
        }
        return success
    }

    suspend fun setWindowState(state: WindowState): Boolean {
        val commands = mutableListOf<String>()
        val levelFL = if (state.frontLeft) 100 else 0
        val levelFR = if (state.frontRight) 100 else 0
        val levelRL = if (state.rearLeft) 100 else 0
        val levelRR = if (state.rearRight) 100 else 0
        commands.add("am broadcast -a com.carlauncher.WINDOW --es position driver_front --ei level $levelFL")
        commands.add("am broadcast -a com.carlauncher.WINDOW --es position passenger_front --ei level $levelFR")
        commands.add("am broadcast -a com.carlauncher.WINDOW --es position driver_rear --ei level $levelRL")
        commands.add("am broadcast -a com.carlauncher.WINDOW --es position passenger_rear --ei level $levelRR")
        
        var success = true
        for (cmd in commands) {
            if (!executeCarCommand(cmd)) {
                success = false
            }
        }
        return success
    }

    suspend fun openSunroof(): Boolean {
        return executeCarCommand("am broadcast -a com.carlauncher.SUNROOF --ei position 100")
    }

    suspend fun closeSunroof(): Boolean {
        return executeCarCommand("am broadcast -a com.carlauncher.SUNROOF --ei position 0")
    }

    suspend fun controlSunshade(open: Boolean): Boolean {
        val level = if (open) 100 else 0
        return executeCarCommand("am broadcast -a com.carlauncher.SUNSHADE --ei position $level")
    }

    suspend fun openTrunk(): Boolean {
        return executeCarCommand("am broadcast -a com.carlauncher.TRUNK --ez open true")
    }

    suspend fun closeTrunk(): Boolean {
        return executeCarCommand("am broadcast -a com.carlauncher.TRUNK --ez open false")
    }
}