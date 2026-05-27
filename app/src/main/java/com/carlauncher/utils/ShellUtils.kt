package com.carlauncher.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object ShellUtils {
    
    suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            val error = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }
            
            process.waitFor()
            
            if (error.isNotEmpty()) {
                "Error: ${error.toString().trim()}"
            } else {
                output.toString().trim()
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }
    
    suspend fun executeSuCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su -c $command")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            val error = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }
            
            process.waitFor()
            
            if (error.isNotEmpty()) {
                "Error: ${error.toString().trim()}"
            } else {
                output.toString().trim()
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }
    
    fun executeCommandSync(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val error = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }

            process.waitFor()

            if (error.isNotEmpty()) {
                "Error: ${error.toString().trim()}"
            } else {
                output.toString().trim()
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    fun hasRootPermission(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -c echo test")
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
