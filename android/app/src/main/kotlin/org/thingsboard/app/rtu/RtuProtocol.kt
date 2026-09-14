package org.thingsboard.app.rtu

import java.util.Locale

/** Pure implementation of the Lihe RTU application protocol. */
object RtuProtocol {
    const val serviceUuid = "6c696865-726f-aabb-eeff-10736c696865"
    const val sendCharacteristicUuid = "6c696865-726f-aabb-eeff-12776c696865"
    const val receiveCharacteristicUuid = "6c696865-726f-aabb-eeff-11726c696865"
    const val eventCharacteristicUuid = "6c696865-726f-aabb-eeff-136e6c696865"

    private val utf8 = Charsets.UTF_8

    fun buildCommand(command: String, targetId: String = "12345678"): ByteArray {
        require(targetId.toByteArray(utf8).size == 8) { "targetId must be exactly 8 UTF-8 bytes" }
        return buildFrame(command.toByteArray(utf8), targetId.toByteArray(utf8))
    }

    fun buildFrame(payload: ByteArray, targetId: ByteArray = "12345678".toByteArray(utf8)): ByteArray {
        require(targetId.size == 8) { "targetId must be exactly 8 bytes" }
        val totalLength = payload.size + 17
        require(totalLength <= 0xffff) { "RTU frame is too large" }
        val frame = ByteArray(totalLength)
        frame[0] = 0xaa.toByte()
        frame[1] = 0xbb.toByte()
        putU16(frame, 2, totalLength)
        targetId.copyInto(frame, 4)
        putU16(frame, 12, payload.size)
        payload.copyInto(frame, 14)
        frame[14 + payload.size] = xor(frame, 2, totalLength - 5)
        frame[15 + payload.size] = 0xee.toByte()
        frame[16 + payload.size] = 0xff.toByte()
        return frame
    }

    /** Matches ProtocolUtil.transformNS from the original Android application. */
    fun encrypt(frame: ByteArray): ByteArray {
        var key = 256
        val result = ByteArray(frame.size * 2)
        frame.forEachIndexed { index, value ->
            val cipher = (value.toInt() xor (key shr 8)) and 0xff
            key = ((cipher + key) * 6 + 9) and 0xffff
            result[index * 2] = (65 + cipher / 26).toByte()
            result[index * 2 + 1] = (65 + cipher % 26).toByte()
        }
        return result
    }

    fun decrypt(encoded: ByteArray): ByteArray? {
        if (encoded.size % 2 != 0) return null
        var key = 256
        val result = ByteArray(encoded.size / 2)
        for (index in result.indices) {
            val high = encoded[index * 2].toInt() - 65
            val low = encoded[index * 2 + 1].toInt() - 65
            if (high !in 0..9 || low !in 0..25) return null
            val cipher = high * 26 + low
            if (cipher > 255) return null
            result[index] = (cipher xor (key shr 8)).toByte()
            key = ((cipher + key) * 6 + 9) and 0xffff
        }
        return result
    }

    fun buildChunk(command: String, number: Int, count: Int): ByteArray {
        return buildChunk(command.toByteArray(utf8), number, count)
    }

    fun buildChunk(payload: ByteArray, number: Int, count: Int): ByteArray {
        require(number in 0 until count && count > 0)
        val totalLength = payload.size + 17
        val frame = ByteArray(totalLength)
        frame[0] = 0xaa.toByte()
        frame[1] = 0xbb.toByte()
        putU16(frame, 2, totalLength)
        "lihe".toByteArray(utf8).copyInto(frame, 4)
        putU16(frame, 8, number)
        putU16(frame, 10, count)
        putU16(frame, 12, payload.size)
        payload.copyInto(frame, 14)
        frame[14 + payload.size] = xor(frame, 2, totalLength - 5)
        frame[15 + payload.size] = 0xee.toByte()
        frame[16 + payload.size] = 0xff.toByte()
        return frame
    }

    private fun putU16(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value ushr 8).toByte()
        buffer[offset + 1] = value.toByte()
    }

    private fun xor(buffer: ByteArray, start: Int, length: Int): Byte {
        var result = 0xff
        for (i in start until start + length) result = result xor (buffer[i].toInt() and 0xff)
        return result.toByte()
    }

    data class DecodedFrame(val payload: ByteArray, val chunkNumber: Int?, val chunkCount: Int?)

    fun decodeFrame(frame: ByteArray): DecodedFrame? {
        if (frame.size < 17 || (frame[0].toInt() and 0xff) != 0xaa || (frame[1].toInt() and 0xff) != 0xbb) return null
        val declared = u16(frame, 2)
        if (declared != frame.size || frame[frame.size - 2] != 0xee.toByte() || frame[frame.size - 1] != 0xff.toByte()) return null
        if (frame[frame.size - 3] != xor(frame, 2, frame.size - 5)) return null
        val dataLength = u16(frame, 12)
        if (dataLength < 0 || 14 + dataLength > frame.size - 3) return null
        val payload = frame.copyOfRange(14, 14 + dataLength)
        val type = frame.copyOfRange(4, 8).toString(utf8).trim()
        if (type.equals("lihe", ignoreCase = true)) {
            return DecodedFrame(payload, u16(frame, 8), u16(frame, 10))
        }
        return DecodedFrame(payload, null, null)
    }

    private fun u16(buffer: ByteArray, offset: Int): Int =
        ((buffer[offset].toInt() and 0xff) shl 8) or (buffer[offset + 1].toInt() and 0xff)

    /** Handles BLE notification fragmentation and multiple frames in one notification. */
    class StreamDecoder {
        private var buffer = ByteArray(0)
        private val chunks = mutableMapOf<Int, ByteArray>()
        private var chunkCount = 0

        @Synchronized
        fun append(bytes: ByteArray): List<String> {
            if (bytes.isEmpty()) return emptyList()
            buffer += bytes
            val results = mutableListOf<String>()
            while (true) {
                val start = findHeader(buffer)
                if (start < 0) {
                    buffer = if (buffer.size > 1) buffer.copyOfRange(buffer.size - 1, buffer.size) else buffer
                    break
                }
                if (start > 0) buffer = buffer.copyOfRange(start, buffer.size)
                if (buffer.size < 4) break
                val length = u16(buffer, 2)
                if (length < 17) {
                    buffer = buffer.copyOfRange(2, buffer.size)
                    continue
                }
                if (buffer.size < length) break
                val frame = buffer.copyOfRange(0, length)
                buffer = buffer.copyOfRange(length, buffer.size)
                val decoded = decodeFrame(frame) ?: continue
                if (decoded.chunkCount != null && decoded.chunkCount > 0) {
                    chunkCount = decoded.chunkCount
                    chunks[decoded.chunkNumber ?: -1] = decoded.payload
                    if (chunks.size == chunkCount && (0 until chunkCount).all { chunks.containsKey(it) }) {
                        val merged = (0 until chunkCount).flatMap { chunks[it]!!.asIterable() }.toByteArray()
                        results += merged.toString(utf8)
                        chunks.clear()
                        chunkCount = 0
                    }
                } else {
                    results += decoded.payload.toString(utf8)
                }
            }
            return results
        }

        private fun findHeader(value: ByteArray): Int {
            for (i in 0 until value.size - 1) {
                if (value[i] == 0xaa.toByte() && value[i + 1] == 0xbb.toByte()) return i
            }
            return -1
        }
    }

    /** Splits the ASCII encrypted byte stream before resetting its key for every RTU frame. */
    class EncryptedStreamDecoder {
        private var buffer = ByteArray(0)
        private val plainDecoder = StreamDecoder()

        @Synchronized
        fun append(bytes: ByteArray): List<String> {
            buffer += bytes
            val results = mutableListOf<String>()
            while (buffer.size >= 8) {
                val header = decrypt(buffer.copyOfRange(0, 8))
                if (header == null || header[0] != 0xaa.toByte() || header[1] != 0xbb.toByte()) {
                    buffer = buffer.copyOfRange(1, buffer.size)
                    continue
                }
                val frameLength = u16(header, 2)
                val encodedLength = frameLength * 2
                if (frameLength < 17) { buffer = buffer.copyOfRange(2, buffer.size); continue }
                if (buffer.size < encodedLength) break
                val frame = decrypt(buffer.copyOfRange(0, encodedLength))
                buffer = buffer.copyOfRange(encodedLength, buffer.size)
                if (frame != null) results += plainDecoder.append(frame)
            }
            return results
        }
    }

    fun fields(response: String): List<String> = response.split("#", limit = Int.MAX_VALUE)

    private fun String.number(): String = trim().removeSuffix("N")
    private fun List<String>.at(index: Int): String = getOrNull(index)?.trim().orEmpty()
    private fun List<String>.intAt(index: Int, fallback: Int = -999): Int = at(index).number().toIntOrNull() ?: fallback
    private fun List<String>.doubleAt(index: Int, fallback: Double = -99999.0): Double = at(index).number().toDoubleOrNull() ?: fallback
    private fun displayTime(value: String): String = if (value.matches(Regex("\\d{12}"))) {
        "20${value.substring(0, 2)}-${value.substring(2, 4)}-${value.substring(4, 6)} " +
            "${value.substring(6, 8)}:${value.substring(8, 10)}:${value.substring(10, 12)}"
    } else value

    fun parseBasic(response: String): MutableMap<String, Any?> {
        val f = fields(response)
        return linkedMapOf(
            "version" to f.at(0), "systemTime" to displayTime(f.at(1)), "systemTimeRaw" to f.at(1), "deviceName" to f.at(2),
            "bleAddress" to f.at(3), "mn" to f.at(4), "batteryVoltage" to f.doubleAt(5, 0.0),
            "deviceNumber" to f.at(6), "deviceState" to f.intAt(7), "sensorDeviceType" to f.intAt(8),
            "powerEnabled" to f.intAt(9, 0), "raw" to response,
        )
    }

    fun parseLot(response: String): MutableMap<String, Any?> {
        val f = fields(response)
        val enabled = f.intAt(27, 0)
        val services = (0..4).map { index ->
            val base = 12 + index * 3
            val encodedHost = f.at(base)
            val host = if (encodedHost.length == 12 && encodedHost.all { it.isDigit() }) {
                encodedHost.chunked(3).joinToString(".") { it.toIntOrNull()?.toString() ?: it }
            } else encodedHost
            linkedMapOf<String, Any?>(
                "index" to index, "host" to host, "encodedHost" to encodedHost, "port" to f.at(base + 1),
                "protocol" to f.intAt(base + 2), "enabled" to (((enabled shr index) and 1) == 1),
            )
        }
        return linkedMapOf(
            "moduleType" to f.intAt(0), "localIp" to f.at(1), "ccid" to f.at(2), "imsi" to f.at(3),
            "signal" to f.intAt(4), "pendingCount" to f.intAt(5), "uploadState" to f.intAt(6),
            "uploadError" to f.intAt(7), "uploadEndTime" to displayTime(f.at(8)), "uploadEndTimeRaw" to f.at(8), "uploadStep" to f.intAt(9),
            "sampleInterval" to f.intAt(10), "uploadInterval" to f.intAt(11),
            "serviceEnabledBits" to enabled, "systemSt" to f.at(28), "services" to services,
            "raw" to response,
        )
    }

    private fun parseSensorFields(response: String, radar: Boolean): MutableMap<String, Any?> {
        val f = fields(response)
        val cumulative = f.doubleAt(9)
        val cumulativeHigh = f.intAt(10)
        val secondary = f.doubleAt(11)
        val secondaryHigh = f.intAt(12)
        val night = f.doubleAt(13)
        val nightHigh = f.intAt(14)
        val absolute = f.doubleAt(15)
        val absoluteHigh = f.intAt(16)
        return linkedMapOf(
            "sensorType" to if (radar) "swj" else "llj", "sampleWait" to f.intAt(0),
            "waterLevel" to f.doubleAt(1), "depth" to f.doubleAt(2), "height" to f.doubleAt(3),
            "direction" to f.intAt(4), "surfaceVelocity" to f.doubleAt(5), "sectionVelocity" to f.doubleAt(6),
            "rateFlow" to f.doubleAt(7), "intervalFlow" to f.doubleAt(8), "cumulativeFlow" to cumulative,
            "cumulativeFlowHigh" to cumulativeHigh, "cumulativeFlowTotal" to cumulativeHigh * 10_000_000 + cumulative,
            "secondaryCumulativeFlow" to secondary, "secondaryCumulativeFlowHigh" to secondaryHigh,
            "secondaryCumulativeFlowTotal" to secondaryHigh * 10_000_000 + secondary, "nightCumulativeFlow" to night,
            "nightCumulativeFlowHigh" to nightHigh, "nightCumulativeFlowTotal" to nightHigh * 10_000_000 + night,
            "absoluteCumulativeFlow" to absolute, "absoluteCumulativeFlowHigh" to absoluteHigh,
            "absoluteCumulativeFlowTotal" to absoluteHigh * 10_000_000 + absolute, "roll" to f.doubleAt(if (radar) 17 else 18),
            "pitch" to f.doubleAt(if (radar) 18 else 17), "batteryVoltage" to f.doubleAt(19),
            "thickness" to f.doubleAt(20), "area" to f.doubleAt(21),
            "ph" to f.doubleAt(22), "oxygen" to f.doubleAt(23), "conductivity" to f.doubleAt(24),
            "turbidity" to f.doubleAt(25), "orp" to f.doubleAt(26), "rain" to f.doubleAt(27), "raw" to response,
        )
    }

    fun parseSensor(response: String, type: String): MutableMap<String, Any?> =
        parseSensorFields(response, type.equals("swj", ignoreCase = true) || type.equals("radar", ignoreCase = true))

    fun pad(value: Int, width: Int): String = value.toString().padStart(width, '0')

    fun validateRtuAddress(value: String): String {
        val normalized = value.replace(":", "").replace("-", "").uppercase(Locale.US)
        require(normalized.length == 12 && normalized.startsWith("15") && normalized.all { it in "0123456789ABCDEF" }) {
            "RTU Bluetooth address must be 12 hex characters and start with 15"
        }
        return normalized
    }

    fun ipToProtocolValue(host: String): String {
        val trimmed = host.trim()
        val parts = trimmed.split('.')
        if (parts.size == 4 && parts.all { it.toIntOrNull() != null && it.toInt() in 0..255 }) {
            return parts.joinToString("") { pad(it.toInt(), 3) }
        }
        require(trimmed.toByteArray(utf8).size <= 12) { "IP/domain must be at most 12 bytes" }
        return trimmed.padEnd(12, '0')
    }

    fun commandForNetwork(index: Int, host: String, port: Int, protocol: Int, enabled: Boolean): List<String> {
        require(index in 0..4) { "service index must be 0..4" }
        require(port in 1..99999) { "port must be between 1 and 99999" }
        require(protocol >= 0) { "protocol must be non-negative" }
        val prefix = pad(index, 2)
        val commands = mutableListOf(
            "Write DevIp=$prefix#${if (enabled) ipToProtocolValue(host) else "000000000000"}",
            "Write DevPort=$prefix#${pad(port, 5)}",
            "Write Protocol=$prefix#$protocol",
        )
        return commands
    }
}
