package com.carlauncher.manager

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class AdbManager(private val context: Context) {

    companion object {
        private const val TAG = "AdbManager"
        private const val ADB_PORT = 5555
        private const val ADB_VERSION = 0x01000000
        private const val MAX_PAYLOAD = 4096

        private const val CMD_CNXN = "CNXN"
        private const val CMD_AUTH = "AUTH"
        private const val CMD_OPEN = "OPEN"
        private const val CMD_OKAY = "OKAY"
        private const val CMD_WRTE = "WRTE"
        private const val CMD_CLSE = "CLSE"

        private const val AUTH_TYPE_TOKEN = 1
        private const val AUTH_TYPE_SIGNATURE = 2
        private const val AUTH_TYPE_RSAPUBLICKEY = 3

        private var pairingCallback: ((String) -> Unit)? = null
    }

    private var isAdbConnected = false
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    fun setPairingCallback(callback: (String) -> Unit) {
        pairingCallback = callback
    }

    suspend fun connectWirelessAdb(ip: String? = null, port: Int = ADB_PORT): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetIp = ip ?: "127.0.0.1"
            Log.d(TAG, "Connecting to ADB at $targetIp:$port")

            disconnectAdb()

            socket = Socket()
            socket?.connect(InetSocketAddress(targetIp, port), 5000)
            outputStream = socket?.getOutputStream()
            inputStream = socket?.getInputStream()

            if (!handshake()) {
                Log.e(TAG, "ADB handshake failed")
                disconnectAdb()
                return@withContext false
            }

            Log.d(TAG, "ADB connected successfully")
            isAdbConnected = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect ADB", e)
            disconnectAdb()
            false
        }
    }

    private suspend fun handshake(): Boolean = withContext(Dispatchers.IO) {
        try {
            var attempt = 0
            val maxAttempts = 3

            while (attempt < maxAttempts) {
                sendCNXN()

                val response = ByteArray(24)
                val bytesRead = inputStream?.read(response) ?: 0
                if (bytesRead < 16) {
                    Log.e(TAG, "Invalid response length: $bytesRead")
                    return@withContext false
                }

                val cmd = String(response, 0, 4)
                Log.d(TAG, "Received command: $cmd")

                when (cmd) {
                    CMD_CNXN -> {
                        val version = bytesToInt(response, 4)
                        Log.d(TAG, "ADB version: $version")
                        return@withContext true
                    }
                    CMD_AUTH -> {
                        val authType = bytesToInt(response, 4)
                        val dataLength = bytesToInt(response, 8)
                        Log.d(TAG, "AUTH required, type: $authType, dataLength: $dataLength")

                        if (authType == AUTH_TYPE_TOKEN) {
                            val token = ByteArray(dataLength)
                            inputStream?.read(token)
                            val tokenString = token.toHexString()
                            Log.d(TAG, "Received token: $tokenString")

                            pairingCallback?.invoke(tokenString)

                            sendAuthResponse(AUTH_TYPE_TOKEN, token)
                            attempt++
                        } else {
                            Log.w(TAG, "Unsupported auth type: $authType")
                            return@withContext false
                        }
                    }
                    else -> {
                        Log.e(TAG, "Unexpected command: $cmd")
                        return@withContext false
                    }
                }
            }
            Log.e(TAG, "Handshake failed after $maxAttempts attempts")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Handshake failed", e)
            false
        }
    }

    private fun sendCNXN() {
        try {
            val payload = "host::\u0000"
            outputStream?.write(CMD_CNXN.toByteArray())
            outputStream?.write(intToBytes(ADB_VERSION))
            outputStream?.write(intToBytes(payload.length))
            outputStream?.write(intToBytes(0))
            outputStream?.write(payload.toByteArray())
            outputStream?.flush()
            Log.d(TAG, "Sent CNXN")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send CNXN", e)
        }
    }

    private fun sendAuthResponse(authType: Int, data: ByteArray) {
        try {
            outputStream?.write(CMD_AUTH.toByteArray())
            outputStream?.write(intToBytes(authType))
            outputStream?.write(intToBytes(data.size))
            outputStream?.write(intToBytes(0))
            outputStream?.write(data)
            outputStream?.flush()
            Log.d(TAG, "Sent AUTH response, type: $authType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send AUTH response", e)
        }
    }

    suspend fun executeAdbCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!isAdbConnected) {
            Log.w(TAG, "ADB not connected, attempting to reconnect")
            if (!connectWirelessAdb()) {
                Log.w(TAG, "ADB connection failed, trying fallback methods")
                return@withContext executeFallbackCommand(command)
            }
        }

        try {
            val localId = generateId()
            val remoteId = generateId()

            val destination = "shell:${command}\u0000"
            sendOPEN(localId, destination)

            val openResponse = ByteArray(16)
            inputStream?.read(openResponse)
            val openCmd = String(openResponse, 0, 4)

            if (openCmd != CMD_OKAY) {
                Log.e(TAG, "Expected OKAY, got $openCmd")
                return@withContext "Error: Connection failed"
            }

            val buffer = ByteArray(8192)
            val result = StringBuilder()
            var bytesRead: Int

            while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                result.append(String(buffer, 0, bytesRead))
            }

            sendCLSE(localId, remoteId)

            val output = result.toString()
            Log.d(TAG, "Command result: ${output.take(200)}")
            output
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command", e)
            isAdbConnected = false
            executeFallbackCommand(command)
        }
    }

    private fun sendOPEN(localId: Int, destination: String) {
        try {
            outputStream?.write(CMD_OPEN.toByteArray())
            outputStream?.write(intToBytes(localId))
            outputStream?.write(intToBytes(0))
            outputStream?.write(intToBytes(destination.length))
            outputStream?.write(destination.toByteArray())
            outputStream?.flush()
            Log.d(TAG, "Sent OPEN to: $destination")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send OPEN", e)
        }
    }

    private fun sendCLSE(localId: Int, remoteId: Int) {
        try {
            outputStream?.write(CMD_CLSE.toByteArray())
            outputStream?.write(intToBytes(localId))
            outputStream?.write(intToBytes(remoteId))
            outputStream?.write(intToBytes(0))
            outputStream?.flush()
            Log.d(TAG, "Sent CLSE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send CLSE", e)
        }
    }

    private suspend fun executeFallbackCommand(command: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.append(line)
                result.append("\n")
            }
            process.waitFor()
            result.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Fallback command failed", e)
            "Error: ${e.message}"
        }
    }

    fun hasAdbPermission(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "adb_enabled", 0) == 1
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check ADB permission", e)
            false
        }
    }

    fun getDeviceIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val hostAddress = address.hostAddress ?: continue
                        Log.d(TAG, "Found IP address: $hostAddress")
                        return hostAddress
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get device IP address", e)
            "127.0.0.1"
        }
    }

    fun disconnectAdb() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
            isAdbConnected = false
            Log.d(TAG, "ADB disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect ADB", e)
        }
    }

    fun isConnected(): Boolean = isAdbConnected

    private fun generateId(): Int {
        return (Math.random() * Int.MAX_VALUE).toInt()
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private fun bytesToInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
               ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
               ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
               (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { String.format("%02x", it) }
    }
}