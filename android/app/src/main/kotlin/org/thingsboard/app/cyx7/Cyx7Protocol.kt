package org.thingsboard.app.cyx7

import java.util.Locale

/**
 * 力合采样箱 CYX7（井下采样器，2026 兼容 RTU 程序）应用层协议。
 *
 * 与 RTU 协议同源：
 *   AA BB | 长度(2,大端) | desID(8,固定 12345678) | payloadLen(2,大端) | payload(ASCII 文本) | XOR | EE FF
 *   XOR = 0xFF ^ Σ(bytes[2 .. len-5])，初值 0xFF。
 * 分包帧把 desID 位置换成 "lihe" + 包序号(2) + 总包数(2)，用于大报文（如升级文件）。
 *
 * 指令全部是 ASCII 文本（如 `Read All`、`Write WorkMode=1`），没有二进制指令码。
 * 本文件只做「编解码 + 字段解析」，不做 UI 取舍。
 */
object Cyx7Protocol {
    const val serviceUuid = "6c696865-726f-aabb-eeff-10736c696865"
    const val sendCharacteristicUuid = "6c696865-726f-aabb-eeff-12776c696865"
    const val receiveCharacteristicUuid = "6c696865-726f-aabb-eeff-11726c696865"
    const val eventCharacteristicUuid = "6c696865-726f-aabb-eeff-136e6c696865"

    /** 原程序 HJBleApplication.setDesID("12345678")。 */
    const val defaultTargetId = "12345678"

    /** 采样箱系列 MAC 前缀；CYX7 程序对应 0D:03 与 0D:07。 */
    const val addressPrefix = "0D"
    private val cyx7Prefixes = listOf("0D:03", "0D:07")

    private val utf8 = Charsets.UTF_8

    // ------------------------------------------------------------------
    // 组帧 / 解帧
    // ------------------------------------------------------------------

    fun buildCommand(command: String, targetId: String = defaultTargetId): ByteArray =
        buildFrame(command.toByteArray(utf8), targetId.toByteArray(utf8))

    fun buildFrame(payload: ByteArray, targetId: ByteArray = defaultTargetId.toByteArray(utf8)): ByteArray {
        require(targetId.size == 8) { "targetId must be exactly 8 bytes" }
        val totalLength = payload.size + 17
        require(totalLength <= 0xffff) { "CYX7 frame is too large" }
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

    /** 与 ProtocolUtil.transformNS 完全一致（HJBleApplication.isEncrypt() 打开时使用）。 */
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

    private fun putU16(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value ushr 8).toByte()
        buffer[offset + 1] = value.toByte()
    }

    private fun xor(buffer: ByteArray, start: Int, length: Int): Byte {
        var result = 0xff
        for (i in start until start + length) result = result xor (buffer[i].toInt() and 0xff)
        return result.toByte()
    }

    private fun u16(buffer: ByteArray, offset: Int): Int =
        ((buffer[offset].toInt() and 0xff) shl 8) or (buffer[offset + 1].toInt() and 0xff)

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

    /** 处理 BLE 通知的分片与「一次通知多帧」。 */
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

    /** 加密流按帧切分后再交给明文解码器（每帧重置密钥）。 */
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

    // ------------------------------------------------------------------
    // 字段解析（分隔符 #，无效值以 N 结尾 → 默认值）
    // ------------------------------------------------------------------

    fun fields(response: String): List<String> = response.split("#", limit = Int.MAX_VALUE)

    private fun String.number(): String = trim().removeSuffix("N")
    private fun List<String>.at(index: Int): String = getOrNull(index)?.trim().orEmpty()
    private fun List<String>.intAt(index: Int, fallback: Int = -999): Int = at(index).number().toIntOrNull() ?: fallback
    private fun List<String>.doubleAt(index: Int, fallback: Double = -99999.0): Double = at(index).number().toDoubleOrNull() ?: fallback

    /** 12 位 yyMMddHHmmss → `20yy-MM-dd HH:mm:ss`。 */
    fun displayTime(value: String): String = if (value.matches(Regex("\\d{12}"))) {
        "20${value.substring(0, 2)}-${value.substring(2, 4)}-${value.substring(4, 6)} " +
            "${value.substring(6, 8)}:${value.substring(8, 10)}:${value.substring(10, 12)}"
    } else value

    /** 12 位数字 IP（每 3 位一组）→ 点分十进制。 */
    fun decodeIp(encoded: String): String = if (encoded.length == 12 && encoded.all { it.isDigit() }) {
        encoded.chunked(3).joinToString(".") { it.toIntOrNull()?.toString() ?: it }
    } else encoded

    /** 17 位 = 12 位 IP + 5 位端口。 */
    private fun decodeIpPort(value: String): Map<String, String> {
        val trimmed = value.trim()
        if (trimmed.length < 12) return mapOf("encoded" to trimmed, "host" to trimmed, "port" to "")
        return mapOf(
            "encoded" to trimmed,
            "host" to decodeIp(trimmed.substring(0, 12)),
            "port" to trimmed.substring(12).trim(),
        )
    }

    /** Read All 应答。 */
    fun parseBasic(response: String): MutableMap<String, Any?> {
        val f = fields(response)
        return linkedMapOf(
            "version" to f.at(0),
            "systemTime" to displayTime(f.at(1)),
            "systemTimeRaw" to f.at(1),
            "deviceName" to f.at(2),
            "bleAddress" to formatMac(f.at(3)),
            "mn" to f.at(4),
            "batteryVoltage" to f.doubleAt(5, 0.0),
            "deviceNumber" to f.at(6),
            "sampleDeviceType" to f.intAt(8),
            "iccid" to f.at(11),
            "onlineState" to f.at(13),
            "uploadInterval" to f.intAt(20, 0),
            "hostEncoded" to f.at(21),
            "host" to decodeIp(f.at(21)),
            "port" to f.at(22),
            "powerEnabled" to f.intAt(38, 0),
            "deviceStatus" to f.at(39),
            "raw" to response,
        )
    }

    /** Read ProInfo 应答。 */
    fun parseProInfo(response: String): MutableMap<String, Any?> {
        val f = fields(response)
        val bottles = (0..5).map { index ->
            val base = 12 + index * 3
            linkedMapOf<String, Any?>(
                "index" to index + 1,
                "bleAddress" to formatMac(f.at(base)),
                "sampleId" to f.at(base + 1).replace("_", "").trim(),
                "sampleTime" to displayTime(f.at(base + 2)),
                "sampleTimeRaw" to f.at(base + 2),
            )
        }
        val volumes = f.at(58).split("-").map { it.trim() }.filter { it.isNotEmpty() }
        val step = f.at(4)
        val stepParts = step.split("/")
        return linkedMapOf(
            "lockBottles" to f.at(0).padStart(6, '0'),
            "sampleMode" to f.at(1),
            "currentBottle" to f.at(2),
            "cycleTime" to f.intAt(3, 0),
            "step" to step,
            "stepIndex" to (stepParts.getOrNull(0)?.number()?.toIntOrNull() ?: -999),
            "stepTotal" to (stepParts.getOrNull(1)?.number()?.toIntOrNull() ?: -999),
            "rainNum" to f.doubleAt(5) / 10.0,
            "waterHeight" to f.doubleAt(6) / 1000.0,
            "jyTime" to f.intAt(7, 0),
            "pyTime" to f.intAt(8, 0),
            "washTime" to f.intAt(9, 0),
            "cyNum" to f.intAt(10, 0),
            "nextSampleTime" to displayTime(f.at(11)),
            "nextSampleTimeRaw" to f.at(11),
            "bottles" to bottles,
            "requestCycle" to f.intAt(30, 0),
            "gnss" to f.intAt(31, 0),
            "batLow" to f.intAt(32, 0),
            "batteryPercent" to f.intAt(33, -1),
            "currentNum" to f.intAt(34, -1),
            "llj" to f.intAt(35, -998),
            "cypErr" to f.at(36),
            "cypIpPort" to decodeIpPort(f.at(37)),
            "trigger" to f.at(38),
            "triggerBits" to bitMap(f.at(38)),
            "phThMin" to f.intAt(39) / 100.0,
            "phThMax" to f.intAt(40) / 100.0,
            "condTh" to f.intAt(41) / 100.0,
            "turbTh" to f.intAt(42) / 100.0,
            "oxygenTh" to f.intAt(43) / 100.0,
            "waterTempTh" to f.intAt(44) / 100.0,
            "flowTh" to f.intAt(45) / 100.0,
            "pipeLen" to f.intAt(54) / 1000.0,
            "capacity" to f.intAt(55),
            "fzkgState" to f.at(57),
            "bottleVolumes" to volumes,
            "topCoefficient" to f.intAt(60) / 100.0,
            "bottomCoefficient" to f.intAt(61) / 100.0,
            "raw" to response,
        )
    }

    /** Read SamInf 应答（现场参数/RtuLljInfo），与 RTU 的传感器报文同构。 */
    fun parseSamInf(response: String): MutableMap<String, Any?> {
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
            "sampleWait" to f.intAt(0),
            "waterLevel" to f.doubleAt(1),
            "depth" to f.doubleAt(2),
            "height" to f.doubleAt(3),
            "direction" to f.intAt(4),
            "surfaceVelocity" to f.doubleAt(5),
            "sectionVelocity" to f.doubleAt(6),
            "rateFlow" to f.doubleAt(7),
            "intervalFlow" to f.doubleAt(8),
            "cumulativeFlow" to cumulative,
            "cumulativeFlowHigh" to cumulativeHigh,
            "cumulativeFlowTotal" to cumulativeHigh * 10_000_000 + cumulative,
            "secondaryCumulativeFlow" to secondary,
            "secondaryCumulativeFlowHigh" to secondaryHigh,
            "secondaryCumulativeFlowTotal" to secondaryHigh * 10_000_000 + secondary,
            "nightCumulativeFlow" to night,
            "nightCumulativeFlowHigh" to nightHigh,
            "nightCumulativeFlowTotal" to nightHigh * 10_000_000 + night,
            "absoluteCumulativeFlow" to absolute,
            "absoluteCumulativeFlowHigh" to absoluteHigh,
            "absoluteCumulativeFlowTotal" to absoluteHigh * 10_000_000 + absolute,
            "pitch" to f.doubleAt(17),
            "roll" to f.doubleAt(18),
            "batteryVoltage" to f.doubleAt(19),
            "thickness" to f.doubleAt(20),
            "area" to f.doubleAt(21),
            "ph" to f.doubleAt(22),
            "oxygen" to f.doubleAt(23),
            "conductivity" to f.doubleAt(24),
            "turbidity" to f.doubleAt(25),
            "orp" to f.doubleAt(26),
            "rain" to f.doubleAt(27),
            "raw" to response,
        )
    }

    /** 16 位位图文本 → 布尔列表（bit0 在最高位，与 StringUtils.getDateBit16 一致）。 */
    fun bitMap(value: String): List<Boolean> {
        val number = value.trim().toLongOrNull() ?: 0L
        return (0..15).map { bit -> (number shr (15 - bit)) and 1L == 1L }
    }

    /** 本地采样计划文本 → 6 段 `[启用, 时, 分]`。 */
    fun parsePlan(response: String): List<Map<String, Any?>> {
        val parts = response.trim().trimEnd('#').split("#")
        return (0..5).map { index ->
            val segment = parts.getOrNull(index)?.trim().orEmpty()
            val digits = segment.filter { it.isDigit() }
            linkedMapOf(
                "index" to index + 1,
                "enabled" to segment.startsWith("1"),
                "hour" to (digits.drop(2).take(2).toIntOrNull() ?: 0),
                "minute" to (digits.drop(4).take(2).toIntOrNull() ?: 0),
                "raw" to segment,
            )
        }
    }

    /** 采样计划写入串：`Write SamPoint=SSHHmm#...#`（6 段）。 */
    fun buildPlanCommand(plan: List<Map<String, Any?>>): String {
        require(plan.size == 6) { "sampling plan requires exactly 6 segments" }
        val text = plan.joinToString("#") { segment ->
            val enabled = if (segment["enabled"] == true) 1 else 0
            val hour = (segment["hour"] as? Number)?.toInt() ?: 0
            val minute = (segment["minute"] as? Number)?.toInt() ?: 0
            require(hour in 0..23 && minute in 0..59) { "hour/minute out of range" }
            "$enabled${pad(hour, 2)}${pad(minute, 2)}"
        }
        return "Write SamPoint=$text#"
    }

    // ------------------------------------------------------------------
    // 数值格式化 / 校验（缩放比例与原程序一致）
    // ------------------------------------------------------------------

    fun pad(value: Int, width: Int): String = value.toString().padStart(width, '0')

    /**
     * 装瓶录入样品编号（LiherCYX7Activity L1168-1178）。
     * 原文：`"00" + 瓶号 + 瓶MAC + "-" + addCharForNum(样品编号, 16, '_')`，长度 33 时去掉首位。
     */
    fun buildSampleSubaddr(bottle: Int, mac: String, sampleNo: String): String {
        require(bottle in 1..6) { "瓶号必须在 1..6" }
        val trimmed = sampleNo.trim()
        require(trimmed.length > 2 && trimmed.length < 16) { "样品编号长度必须在 3..15" }
        val compactMac = mac.replace(":", "").replace("-", "").trim().uppercase(Locale.US)
        require(compactMac.length == 12 && compactMac.all { it in "0123456789ABCDEF" }) { "瓶蓝牙地址必须是 12 位十六进制" }
        var value = "00$bottle$compactMac-" + trimmed.padEnd(16, '_')
        if (value.length == 33) value = value.substring(1)
        return "Write Subaddr=$value"
    }

    /** 12 位 MAC 地址（去分隔符、大写、两位一组补冒号）。 */
    fun formatMac(value: String): String {
        val compact = value.replace(":", "").replace("-", "").trim().uppercase(Locale.US)
        if (compact.length != 12 || compact.any { it !in "0123456789ABCDEF" }) return value.trim()
        return compact.chunked(2).joinToString(":")
    }

    fun validateAddress(value: String): String {
        val normalized = value.replace(":", "").replace("-", "").trim().uppercase(Locale.US)
        require(normalized.length == 12 && normalized.all { it in "0123456789ABCDEF" }) {
            "采样箱蓝牙地址必须是 12 位十六进制"
        }
        require(normalized.startsWith(addressPrefix)) { "采样箱蓝牙地址必须以 0D 开头" }
        return normalized
    }

    fun isCyx7Address(value: String): Boolean {
        val normalized = value.replace(":", "").replace("-", "").trim().uppercase(Locale.US)
        return cyx7Prefixes.any { normalized.startsWith(it.replace(":", "")) }
    }

    /** IP → 12 位协议值（每段补 3 位），支持域名（≤12 字节）。 */
    fun ipToProtocolValue(host: String): String {
        val trimmed = host.trim()
        val parts = trimmed.split('.')
        if (parts.size == 4 && parts.all { it.toIntOrNull() != null && it.toInt() in 0..255 }) {
            return parts.joinToString("") { pad(it.toInt(), 3) }
        }
        require(trimmed.toByteArray(utf8).size <= 12) { "IP/域名长度不能超过 12 字节" }
        return trimmed.padEnd(12, '0')
    }

    /** `Write CypIpPort=<12 位 IP><5 位端口>`（采样通道 212 平台）。 */
    fun cypIpPortValue(host: String, port: Int): String {
        require(port in 1..99999) { "端口必须在 1..99999" }
        return ipToProtocolValue(host) + pad(port, 5)
    }

    /** 采样高程(m) ↔ 进样时长(s)：height = (jyTime - 10.787) / 2.9626。 */
    fun heightFromJyTime(jyTime: Int): Double = (jyTime - 10.787) / 2.9626

    fun jyTimeFromHeight(height: Double): Int = (height * 2.9626 + 10.787).toInt()

    fun scaledInt(value: Double, factor: Int): Int = (value * factor).toInt()

    fun validateSystemTime(value: String): String {
        val trimmed = value.trim()
        if (trimmed.matches(Regex("\\d{12}"))) return trimmed
        val digits = trimmed.filter { it.isDigit() }
        require(digits.length == 14) { "系统时间格式应为 yyMMddHHmmss 或 yyyy-MM-dd HH:mm:ss" }
        return digits.substring(2)
    }

    // ------------------------------------------------------------------
    // RTU 功能入口子页用到的两个应答
    // ------------------------------------------------------------------

    /** `Read RtuInf`（LiherCYXRTUActivity L1203-1238）。 */
    fun parseRtuInf(response: String): MutableMap<String, Any?> {
        val f = fields(response)
        return linkedMapOf(
            "version" to f.at(0),
            "systemTimeRaw" to f.at(1),
            "systemTime" to displayTime(f.at(1)),
            "deviceName" to f.at(2),
            "bleAddress" to formatMac(f.at(3)),
            "mn" to f.at(4),
            "batteryVoltage" to f.doubleAt(5, 0.0),
            "deviceNumber" to f.at(6),
            "deviceState" to f.intAt(7, 0),
            "sampleDeviceType" to f.intAt(8, 0),
            "raw" to response,
        )
    }

    /** `Read LotInf`（LiherCYXRTUActivity L1239-1287 / L1411-1497），含 5 路通讯。 */
    fun parseLotInf(response: String): MutableMap<String, Any?> {
        val f = fields(response)
        val enabledBits = f.intAt(27, 0)
        val services = (0..4).map { index ->
            val base = 12 + index * 3
            val encodedHost = f.at(base)
            linkedMapOf<String, Any?>(
                "index" to index,
                "encodedHost" to encodedHost,
                "host" to decodeIp(encodedHost),
                "port" to f.at(base + 1),
                "protocol" to f.intAt(base + 2, -1),
                "enabled" to (((enabledBits shr index) and 1) == 1),
                // 原程序 NetInfo.checkIp2En：非法或全 0 的 IP 视为该路无效（整行隐藏）
                "valid" to (encodedHost.length == 12 && encodedHost.all { it.isDigit() } && encodedHost != "000000000000"),
            )
        }
        return linkedMapOf(
            "moduleType" to f.intAt(0, -1),
            "localIp" to f.at(1),
            "ccid" to f.at(2),
            "imsi" to f.at(3),
            "signal" to f.intAt(4, -1),
            "pendingCount" to f.intAt(5, 0),
            "uploadState" to f.intAt(6, 0),
            "uploadError" to f.intAt(7, 0),
            "uploadEndTimeRaw" to f.at(8),
            "uploadEndTime" to displayTime(f.at(8)),
            "uploadStep" to f.intAt(9, 0),
            "sampleInterval" to f.intAt(10, 0),
            "uploadInterval" to f.intAt(11, 0),
            "services" to services,
            "serviceEnabledBits" to enabledBits,
            "systemSt" to f.at(28),
            "powerEnabled" to f.intAt(29, 0),
            "raw" to response,
        )
    }
}
