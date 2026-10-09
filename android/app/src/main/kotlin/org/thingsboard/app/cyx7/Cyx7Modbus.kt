package org.thingsboard.app.cyx7

import java.util.Locale

/**
 * CYX7 参数校准用的 ModBus-RTU 拼帧与解析（与 SampleBox 的 ProtocolUtil 完全一致）。
 *
 * - 读保持寄存器：`getReadHexData(adr, firstNo40001, count)`
 *   → `[adr, 0x03, hi(firstNo-40001), lo, hi(count), lo] + CRC(小端)`
 * - 写多个寄存器：`getWriteHexDataEndLength(adr, value40001, data)`
 *   → `[adr, 0x10, hi(addr), lo(addr), hi(count), lo(count), byteCount=2*count, data…] + CRC(小端)`
 *     （地址 > 40000 时先减 40000，对应原实现里的力合/通用两种基址）
 * - CRC-16/MODBUS：初值 0xFFFF、多项式 0xA001（反射），追加时低字节在前。
 *
 * 拼好的十六进制直接作为 `Write DIY=<hex>` 的 payload 下发。
 */
object Cyx7Modbus {
    private const val modbusPortBase = 40001

    /** CRC-16/MODBUS。 */
    fun crc16(bytes: ByteArray, from: Int = 0, length: Int = bytes.size): Int {
        var crc = 0xFFFF
        for (index in from until from + length) {
            crc = crc xor (bytes[index].toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc shr 1) xor 0xA001 else crc shr 1
            }
        }
        return crc and 0xFFFF
    }

    fun crcHex(body: ByteArray): String {
        val crc = crc16(body)
        return "%02X%02X".format(Locale.US, crc and 0xFF, (crc shr 8) and 0xFF)
    }

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02X".format(Locale.US, it.toInt() and 0xFF) }

    fun buildRead(address40001: Int, firstNo40001: Int, count: Int): String {
        val first = firstNo40001 - modbusPortBase
        val body = byteArrayOf(
            (address40001 and 0xFF).toByte(),
            0x03,
            ((first shr 8) and 0xFF).toByte(),
            (first and 0xFF).toByte(),
            ((count shr 8) and 0xFF).toByte(),
            (count and 0xFF).toByte(),
        )
        return hex(body) + crcHex(body)
    }

    fun buildWrite(address40001: Int, value40001: Int, values: List<Int>): String {
        require(values.isNotEmpty()) { "写入寄存器不能为空" }
        require(values.size <= 127) { "一次最多写入 127 个寄存器" }
        val address = if (value40001 > 40000) value40001 - 40000 else value40001
        val count = values.size
        val body = ByteArray(7 + count * 2)
        body[0] = address40001.toByte()
        body[1] = 0x10
        body[2] = ((address shr 8) and 0xFF).toByte()
        body[3] = (address and 0xFF).toByte()
        body[4] = ((count shr 8) and 0xFF).toByte()
        body[5] = (count and 0xFF).toByte()
        body[6] = ((count * 2) and 0xFF).toByte()
        values.forEachIndexed { index, value ->
            body[7 + index * 2] = ((value shr 8) and 0xFF).toByte()
            body[8 + index * 2] = (value and 0xFF).toByte()
        }
        return hex(body) + crcHex(body)
    }

    /** float → 两个寄存器（大端在前），对应 Float.floatToIntBits。 */
    fun floatRegisters(value: Double): List<Int> {
        val bits = java.lang.Float.floatToIntBits(value.toFloat())
        return listOf((bits shr 16) and 0xFFFF, bits and 0xFFFF)
    }

    fun registersToFloat(high: Int, low: Int): Float =
        java.lang.Float.intBitsToFloat(((high and 0xFFFF) shl 16) or (low and 0xFFFF))

    /**
     * 解析应答（十六进制文本）：
     * - 功能码 0x03 → 返回寄存器列表
     * - 功能码 0x10 → 返回写入确认
     * 同时返回 CRC 校验结果（原程序 CRC 不通过时也会弹提示，这里如实回传）。
     */
    fun parse(response: String): Map<String, Any?>? {
        val text = response.filter { it.isLetterOrDigit() }.uppercase(Locale.US)
        if (text.length < 10 || text.length % 2 != 0) return null
        if (text.any { it !in "0123456789ABCDEF" }) return null
        val bytes = ByteArray(text.length / 2) { text.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        if (bytes.size < 5) return null
        val expected = crc16(bytes, 0, bytes.size - 2)
        val actual = (bytes[bytes.size - 2].toInt() and 0xFF) or ((bytes[bytes.size - 1].toInt() and 0xFF) shl 8)
        val order = bytes[1].toInt() and 0xFF
        val result = linkedMapOf<String, Any?>(
            "order" to order,
            "crcOk" to (expected == actual),
            "raw" to text,
        )
        if (order == 0x03 && bytes.size >= 5) {
            val byteCount = bytes[2].toInt() and 0xFF
            val registers = (0 until byteCount / 2).map { index ->
                ((bytes[3 + index * 2].toInt() and 0xFF) shl 8) or (bytes[4 + index * 2].toInt() and 0xFF)
            }
            result["registers"] = registers
        }
        return result
    }
}
