package com.carlauncher.manager

import android.content.Context
import android.util.Base64
import com.carlauncher.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.zip.CRC32

object AdbManagerHolder {
    private var instance: AdbManager? = null

    fun get(context: Context): AdbManager {
        return instance ?: AdbManager(context.applicationContext).also { instance = it }
    }

    fun isConnected(): Boolean = instance?.isConnected() == true
}

class AdbManager(private val context: Context) {

    companion object {
        private const val TAG = "AdbManager"
        private const val ADB_HOST = "127.0.0.1"
        private const val ADB_PORT = 5555
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val ADB_VERSION = 0x01000000
        private const val MAX_PAYLOAD = 4096
        private const val HEADER_SIZE = 24

        private const val CMD_CNXN = "CNXN"
        private const val CMD_AUTH = "AUTH"
        private const val CMD_OPEN = "OPEN"
        private const val CMD_OKAY = "OKAY"
        private const val CMD_WRTE = "WRTE"
        private const val CMD_CLSE = "CLSE"

        private const val AUTH_TYPE_TOKEN = 1
        private const val AUTH_TYPE_SIGNATURE = 2
        private const val AUTH_TYPE_RSAPUBLICKEY = 3
    }

    private var rsaKeyPair: KeyPair? = null
    @Volatile
    private var connected = false
    private var socket: Socket? = null
    private var output: OutputStream? = null
    private var input: InputStream? = null
    private var localId = 1
    private val connectMutex = Mutex()
    private var connectAttempted = false

    init {
        ensureRsaKey()
    }

    private fun ensureRsaKey() {
        if (rsaKeyPair == null) {
            try {
                val generator = KeyPairGenerator.getInstance("RSA")
                generator.initialize(2048)
                rsaKeyPair = generator.generateKeyPair()
                Logger.d(TAG, "RSA key pair generated")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to generate RSA key", e)
            }
        }
    }

    fun shouldAutoConnect(): Boolean = !connectAttempted && !connected

    suspend fun connect(): Boolean = connectMutex.withLock {
        return withContext(Dispatchers.IO) {
            if (connected) {
                Logger.d(TAG, "Already connected")
                return@withContext true
            }

            connectAttempted = true

            try {
                Logger.d(TAG, "Connecting to $ADB_HOST:$ADB_PORT")
                disconnect()

                socket = Socket()
                socket?.tcpNoDelay = true
                socket?.connect(InetSocketAddress(ADB_HOST, ADB_PORT), CONNECT_TIMEOUT_MS)
                output = socket?.getOutputStream()
                input = socket?.getInputStream()

                Logger.d(TAG, "TCP connected, starting handshake")

                if (!handshake()) {
                    Logger.e(TAG, "ADB handshake failed")
                    disconnect()
                    return@withContext false
                }

                connected = true
                Logger.i(TAG, "ADB connected successfully")
                true
            } catch (e: Exception) {
                Logger.e(TAG, "ADB connect failed", e)
                disconnect()
                false
            }
        }
    }

    private fun handshake(): Boolean {
        try {
            val identity = "host::carlauncher\u0000"
            writeMessage(CMD_CNXN, ADB_VERSION, MAX_PAYLOAD, identity.toByteArray())
            Logger.d(TAG, "Sent CNXN")

            var attempt = 0
            val maxAttempts = 3

            while (attempt < maxAttempts) {
                val header = readMessageHeader()
                if (header == null) {
                    Logger.e(TAG, "Failed to read response header")
                    return false
                }

                val cmd = header.command
                Logger.d(TAG, "Received: $cmd (arg0=${header.arg0}, dataLen=${header.dataLength})")

                when (cmd) {
                    CMD_CNXN -> {
                        Logger.i(TAG, "ADB connection accepted")
                        readMessageData(header.dataLength)
                        return true
                    }
                    CMD_AUTH -> {
                        val authType = header.arg0
                        val token = readMessageData(header.dataLength)
                        Logger.d(TAG, "AUTH type=$authType, tokenLen=${token.size}")

                        when (authType) {
                            AUTH_TYPE_TOKEN -> {
                                val result = handleAuthToken(token)
                                if (result) return true
                                attempt++
                                if (attempt >= maxAttempts) {
                                    Logger.e(TAG, "Auth failed after $maxAttempts attempts")
                                    return false
                                }
                            }
                            else -> {
                                Logger.e(TAG, "Unsupported auth type: $authType")
                                return false
                            }
                        }
                    }
                    else -> {
                        Logger.e(TAG, "Unexpected command: $cmd")
                        return false
                    }
                }
            }
            return false
        } catch (e: Exception) {
            Logger.e(TAG, "Handshake exception", e)
            return false
        }
    }

    private fun handleAuthToken(token: ByteArray): Boolean {
        rsaKeyPair?.let { keyPair ->
            val signature = signWithRsa(keyPair.private, token)
            if (signature != null) {
                Logger.d(TAG, "Sending AUTH_SIGNATURE (sigLen=${signature.size})")
                writeMessage(CMD_AUTH, AUTH_TYPE_SIGNATURE, 0, signature)

                val header = readMessageHeader()
                if (header != null) {
                    Logger.d(TAG, "After signature: received ${header.command}")
                    when (header.command) {
                        CMD_CNXN -> {
                            Logger.i(TAG, "ADB authorized via signature")
                            readMessageData(header.dataLength)
                            return true
                        }
                        CMD_AUTH -> {
                            readMessageData(header.dataLength)
                            Logger.d(TAG, "Signature rejected, sending public key")
                            val publicKey = encodeAdbPublicKey(keyPair)
                            writeMessage(CMD_AUTH, AUTH_TYPE_RSAPUBLICKEY, 0, publicKey)
                            Logger.d(TAG, "Sent AUTH_RSAPUBLICKEY (keyLen=${publicKey.size})")

                            val header2 = readMessageHeader()
                            if (header2 != null) {
                                Logger.d(TAG, "After public key: received ${header2.command}")
                                readMessageData(header2.dataLength)
                                if (header2.command == CMD_CNXN) {
                                    Logger.i(TAG, "ADB authorized via public key")
                                    return true
                                }
                                if (header2.command == CMD_AUTH) {
                                    Logger.w(TAG, "Public key rejected - user needs to authorize on device")
                                    return false
                                }
                            }
                        }
                    }
                }
            }
        }

        Logger.e(TAG, "ADB auth failed")
        return false
    }

    private fun signWithRsa(privateKey: java.security.PrivateKey, data: ByteArray): ByteArray? {
        return try {
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initSign(privateKey)
            sig.update(data)
            sig.sign()
        } catch (e: Exception) {
            Logger.e(TAG, "RSA signing failed", e)
            null
        }
    }

    private fun encodeAdbPublicKey(keyPair: KeyPair): ByteArray {
        val publicKey = keyPair.public as java.security.interfaces.RSAPublicKey
        val modulus = publicKey.modulus
        val exponent = publicKey.publicExponent

        val modBytes = modulus.toByteArray()
        val expBytes = exponent.toByteArray()

        val buf = ByteArrayOutputStream()
        val dos = DataOutputStream(buf)

        dos.writeLeInt(modBytes.size)
        dos.write(modBytes)
        dos.writeLeInt(expBytes.size)
        dos.write(expBytes)
        dos.flush()

        val keyData = buf.toByteArray()
        val keyStr = Base64.encodeToString(keyData, Base64.NO_WRAP)
        val result = "$keyStr carlauncher@carlauncher\n"
        Logger.d(TAG, "Public key encoded, length=${result.length}")
        return result.toByteArray()
    }

    private fun DataOutputStream.writeLeInt(value: Int) {
        writeByte(value and 0xFF)
        writeByte((value shr 8) and 0xFF)
        writeByte((value shr 16) and 0xFF)
        writeByte((value shr 24) and 0xFF)
    }

    private data class AdbMessageHeader(
        val command: String,
        val arg0: Int,
        val arg1: Int,
        val dataLength: Int,
        val dataCrc32: Int,
        val magic: Int
    )

    private fun readMessageHeader(): AdbMessageHeader? {
        return try {
            val header = ByteArray(HEADER_SIZE)
            var totalRead = 0
            while (totalRead < HEADER_SIZE) {
                val read = input?.read(header, totalRead, HEADER_SIZE - totalRead) ?: -1
                if (read < 0) {
                    Logger.e(TAG, "Connection closed while reading header (read $totalRead/$HEADER_SIZE)")
                    return null
                }
                totalRead += read
            }

            val cmd = String(header, 0, 4)
            val arg0 = readIntLE(header, 4)
            val arg1 = readIntLE(header, 8)
            val dataLength = readIntLE(header, 12)
            val dataCrc32 = readIntLE(header, 16)
            val magic = readIntLE(header, 20)

            val commandValue = readIntLE(header, 0)
            val expectedMagic = commandValue xor 0xFFFFFFFF.toInt()
            if (magic != expectedMagic) {
                Logger.e(TAG, "Magic mismatch for $cmd: got 0x${Integer.toHexString(magic)}, expected 0x${Integer.toHexString(expectedMagic)}")
            }

            AdbMessageHeader(cmd, arg0, arg1, dataLength, dataCrc32, magic)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to read message header", e)
            null
        }
    }

    private fun readMessageData(length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        return try {
            val data = ByteArray(length)
            var totalRead = 0
            while (totalRead < length) {
                val read = input?.read(data, totalRead, length - totalRead) ?: -1
                if (read < 0) break
                totalRead += read
            }
            data
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to read message data", e)
            ByteArray(0)
        }
    }

    private fun writeMessage(cmd: String, arg0: Int, arg1: Int, data: ByteArray) {
        val crc32 = crc32(data)
        val cmdValue = commandToInt(cmd)
        val magic = cmdValue xor 0xFFFFFFFF.toInt()

        val message = ByteArray(HEADER_SIZE + data.size)
        val cmdBytes = cmd.toByteArray(Charsets.US_ASCII)
        System.arraycopy(cmdBytes, 0, message, 0, 4)
        writeIntLE(message, 4, arg0)
        writeIntLE(message, 8, arg1)
        writeIntLE(message, 12, data.size)
        writeIntLE(message, 16, crc32)
        writeIntLE(message, 20, magic)
        if (data.isNotEmpty()) {
            System.arraycopy(data, 0, message, HEADER_SIZE, data.size)
        }

        output?.write(message)
        output?.flush()
    }

    suspend fun executeShell(command: String): String = connectMutex.withLock {
        return withContext(Dispatchers.IO) {
            if (!connected) {
                Logger.d(TAG, "Not connected, attempting to connect")
                if (!connect()) {
                    Logger.w(TAG, "Cannot connect, trying direct exec")
                    return@withContext execDirect(command)
                }
            }

            try {
                val id = localId++
                val destination = "shell:$command\u0000"
                writeMessage(CMD_OPEN, id, 0, destination.toByteArray())
                Logger.d(TAG, "Sent OPEN for: $command")

                val okHeader = readMessageHeader()
                if (okHeader == null || okHeader.command != CMD_OKAY) {
                    Logger.e(TAG, "Expected OKAY, got ${okHeader?.command}")
                    connected = false
                    return@withContext execDirect(command)
                }
                val remoteId = okHeader.arg0

                val result = StringBuilder()
                var done = false

                while (!done) {
                    val header = readMessageHeader()
                    if (header == null) {
                        done = true
                        continue
                    }

                    when (header.command) {
                        CMD_WRTE -> {
                            val data = readMessageData(header.dataLength)
                            result.append(String(data))
                            writeMessage(CMD_OKAY, id, remoteId, ByteArray(0))
                        }
                        CMD_OKAY -> { }
                        CMD_CLSE -> {
                            readMessageData(header.dataLength)
                            writeMessage(CMD_CLSE, id, remoteId, ByteArray(0))
                            done = true
                        }
                        else -> {
                            Logger.w(TAG, "Unexpected: ${header.command}")
                            done = true
                        }
                    }
                }

                Logger.d(TAG, "Shell result: ${result.toString().take(200)}")
                result.toString()
            } catch (e: Exception) {
                Logger.e(TAG, "Shell exec failed", e)
                connected = false
                execDirect(command)
            }
        }
    }

    private fun execDirect(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readText()
            process.waitFor()
            Logger.d(TAG, "Direct exec result: ${result.take(100)}")
            result
        } catch (e: Exception) {
            Logger.e(TAG, "Direct exec failed", e)
            "Error: ${e.message}"
        }
    }

    fun disconnect() {
        try {
            input?.close()
            output?.close()
            socket?.close()
        } catch (_: Exception) {}
        connected = false
        socket = null
        output = null
        input = null
    }

    fun isConnected(): Boolean = connected

    private fun writeIntLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun readIntLE(buf: ByteArray, offset: Int): Int {
        return (buf[offset].toInt() and 0xFF) or
               ((buf[offset + 1].toInt() and 0xFF) shl 8) or
               ((buf[offset + 2].toInt() and 0xFF) shl 16) or
               ((buf[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun crc32(data: ByteArray): Int {
        if (data.isEmpty()) return 0
        val crc = CRC32()
        crc.update(data)
        return crc.value.toInt()
    }

    private fun commandToInt(cmd: String): Int {
        val bytes = cmd.toByteArray(Charsets.US_ASCII)
        return (bytes[0].toInt() and 0xFF) or
               ((bytes[1].toInt() and 0xFF) shl 8) or
               ((bytes[2].toInt() and 0xFF) shl 16) or
               ((bytes[3].toInt() and 0xFF) shl 24)
    }
}
