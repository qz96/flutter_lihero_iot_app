package org.thingsboard.app.cyx7

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * CYX7（井下采样器 / 采样箱 2026 兼容 RTU 程序）Flutter 通道。
 *
 * 通道名与 RTU 完全隔离：`lihero_cyx7` / `lihero_cyx7/events`，
 * 不改动 `lihero_rtu` 的任何行为。
 * 所有指令文本与缩放比例都按 APP_SampleBox_sum 的 LiherCYX7Activity 原样实现。
 */
class Cyx7MethodChannel private constructor(context: Context, messenger: BinaryMessenger) :
    MethodChannel.MethodCallHandler, EventChannel.StreamHandler, Cyx7BleManager.Listener {

    companion object {
        /** 原工程 Constant.SERVICE_API_ADDRESS + sample/bottle/log/insertLog。 */
        private const val defaultLogUrl = "http://city.lihero.com:3001/sample/bottle/log/insertLog"

        fun register(context: Context, messenger: BinaryMessenger): Cyx7MethodChannel {
            return Cyx7MethodChannel(context, messenger)
        }
    }

    private val manager = Cyx7BleManager(context.applicationContext)
    private val channel = MethodChannel(messenger, "lihero_cyx7")
    private val mainHandler = Handler(Looper.getMainLooper())
    private var eventSink: EventChannel.EventSink? = null

    init {
        channel.setMethodCallHandler(this)
        EventChannel(messenger, "lihero_cyx7/events").setStreamHandler(this)
        manager.setListener(this)
    }

    override fun onListen(arguments: Any?, sink: EventChannel.EventSink?) { eventSink = sink }
    override fun onCancel(arguments: Any?) { eventSink = null }

    override fun onEvent(name: String, data: Any?) {
        val event = linkedMapOf<String, Any?>("event" to name)
        if (data is Map<*, *>) data.forEach { (key, value) -> if (key != null) event[key.toString()] = value }
        else if (data != null) event["data"] = data
        mainHandler.post { eventSink?.success(event) }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                // ---------------- 连接 ----------------
                "startScan" -> manager.startScan(call.argument<Number>("durationMs")?.toLong() ?: 10_000L) { ok, error ->
                    if (ok) result.success(true) else result.error("SCAN_FAILED", error, null)
                }
                "stopScan" -> { manager.stopScan(); result.success(true) }
                "connect" -> manager.connect(call.requireString("address")) { ok, error ->
                    if (ok) result.success(true) else result.error("CONNECT_FAILED", error, null)
                }
                "disconnect" -> { manager.disconnect(); result.success(true) }
                "isConnected" -> result.success(manager.isConnected())
                "setEncryption" -> {
                    require(!manager.isConnected()) { "请先断开采样箱再切换加密模式" }
                    manager.setEncrypted(call.argument<Boolean>("enabled") ?: true)
                    result.success(true)
                }
                "sendCommand" -> sendCommand(call.requireString("command"), result)

                // ---------------- 读取 ----------------
                "readAll" -> sendAndParse("Read All", result) { Cyx7Protocol.parseBasic(it) }
                "readProInfo" -> sendAndParse("Read ProInfo", result) { Cyx7Protocol.parseProInfo(it) }
                "readSamInf" -> sendAndParse("Read SamInf", result) { Cyx7Protocol.parseSamInf(it) }
                "readCid" -> sendAndParse("Read CID", result) { mapOf("iccid" to it.trim()) }
                "readPlan" -> sendAndParse("Read SamPoint", result) { mapOf("segments" to Cyx7Protocol.parsePlan(it), "raw" to it) }

                // ---------------- 开关类 ----------------
                "writeAlarm" -> sendCommand("Write Alarm=${if (call.argument<Boolean>("enabled") == true) 1 else 0}", result)
                "writeGnss" -> sendCommand("Write gnss=${if (call.argument<Boolean>("enabled") == true) 1 else 0}", result)
                "writeWorkMode" -> {
                    val mode = call.requireInt("mode").also { require(it in 0..13) { "工作模式必须在 0..13" } }
                    sendCommand("Write WorkMode=$mode", result)
                }
                "writePlan" -> writePlan(call, result)
                "writeSysTime" -> sendCommand("Write SysTime=${Cyx7Protocol.validateSystemTime(call.requireString("value"))}", result)
                "writeEffective" -> sendCommand("Write Effective=${Cyx7Protocol.validateSystemTime(call.requireString("value"))}", result)

                // ---------------- 网络 ----------------
                "writeNet" -> sendCommand("Write net=${Cyx7Protocol.ipToProtocolValue(call.requireString("host"))}", result)
                "writePort" -> sendCommand("Write port=${Cyx7Protocol.pad(call.requirePort("port"), 5)}", result)
                "writeCypIpPort" -> sendCommand(
                    "Write CypIpPort=${Cyx7Protocol.cypIpPortValue(call.requireString("host"), call.requirePort("port"))}",
                    result,
                )
                "writeBleAddr" -> sendCommand("Write BleAddr=${Cyx7Protocol.validateAddress(call.requireString("value"))}", result)

                // ---------------- 采样流程参数 ----------------
                "writeJyTime" -> {
                    // 采样高程与进样阀值共用 `Write jyTime=`，由 Dart 侧决定语义与范围
                    val seconds = call.requireInt("seconds").also { require(it in 5..6000) { "进样时长/阀值必须在 5..6000" } }
                    sendCommand("Write jyTime=$seconds", result)
                }
                "writePyTime" -> {
                    val seconds = call.requireInt("seconds").also { require(it in 10..300) { "排样时长必须在 10..300 秒" } }
                    sendCommand("Write pyTime=$seconds", result)
                }
                "writeWashTime" -> {
                    val seconds = call.requireInt("seconds").also { require(it in 0..600) { "润洗时长必须在 0..600 秒" } }
                    sendCommand("Write RxTime=$seconds", result)
                }
                "writePipeLen" -> {
                    val meters = call.requireDouble("meters").also { require(it >= 0) { "采样管长不能为负" } }
                    sendCommand("Write PipeLen=${Cyx7Protocol.scaledInt(meters, 1000)}", result)
                }
                "writeSamflow" -> {
                    val top = Cyx7Protocol.scaledInt(call.requireDouble("top"), 100)
                    val bottom = Cyx7Protocol.scaledInt(call.requireDouble("bottom"), 100)
                    sendCommand("Write Samflow=$top-$bottom", result)
                }
                "writeSamNum" -> {
                    val bottle = call.requireInt("bottle").also { require(it in 1..6) { "瓶号必须在 1..6" } }
                    sendCommand("Write samnum=$bottle", result)
                }
                "writeSamDev" -> {
                    // LiherCYX7Activity L1334：Dialog_Box_Sample_Bottle
                    val bottle = call.requireInt("bottle").also { require(it in 1..6) { "瓶号必须在 1..6" } }
                    sendCommand("Write samdev=$bottle", result)
                }
                "writeSubaddr" -> {
                    val bottle = call.requireInt("bottle").also { require(it in 1..6) { "瓶号必须在 1..6" } }
                    sendCommand("Write Subaddr=${Cyx7Protocol.pad(bottle, 2)}000000000000-0000000000000000", result)
                }
                "writeSampleNo" -> sendCommand(
                    Cyx7Protocol.buildSampleSubaddr(
                        call.requireInt("bottle").also { require(it in 1..6) { "瓶号必须在 1..6" } },
                        call.requireString("mac"),
                        call.requireString("sampleNo"),
                    ),
                    result,
                )
                "writeCapacity" -> {
                    // LiherCYX7Activity L1051：采样管容量（mL）
                    val ml = call.requireInt("ml").also { require(it > 0) { "管容量必须是大于 0 的整数" } }
                    sendCommand("Write Capacity=$ml", result)
                }
                "writeBanned" -> {
                    // 掩码语义：0 启用 / 1 禁用 / 2 留样（LiherCYX7Activity L1504/1513/1522）
                    val mask = call.requireString("mask")
                    require(mask.length == 6 && mask.all { it in '0'..'2' }) { "瓶位掩码必须是 6 位（0 启用/1 禁用/2 留样）" }
                    sendCommand("Write Banned=$mask", result)
                }

                // ---------------- 触发与阈值 ----------------
                "writeTrigger" -> {
                    val bitmap = call.requireString("bitmap")
                    require(bitmap.length == 16 && bitmap.all { it == '0' || it == '1' }) { "触发位图必须是 16 位 0/1" }
                    sendCommand("Write Trigger=${bitmap.toLong(2)}", result)
                }
                "writePhTh" -> {
                    val min = Cyx7Protocol.scaledInt(call.requireDouble("min"), 100)
                    val max = Cyx7Protocol.scaledInt(call.requireDouble("max"), 100)
                    require(min <= max) { "pH 下限不能大于上限" }
                    sendCommand("Write PHTh=$min#$max", result)
                }
                "writeThreshold" -> writeThreshold(call, result)
                "writeRainNum" -> sendCommand("Write rainNum=${Cyx7Protocol.scaledInt(call.requireDouble("mm"), 10)}", result)
                "writeWaterHeight" -> sendCommand("Write waterHeight=${Cyx7Protocol.scaledInt(call.requireDouble("meters"), 1000)}", result)
                "writeBatLow" -> {
                    val volts = call.requireDouble("volts")
                    val scaled = Cyx7Protocol.scaledInt(volts, 100)
                    require(scaled in 1000..2400) { "低压阈值必须在 10.00~24.00V" }
                    sendCommand("Write batLow=$scaled", result)
                }
                "writeVin" -> sendCommand("Write vin=${Cyx7Protocol.scaledInt(call.requireDouble("volts"), 100)}", result)

                // ---------------- 周期 ----------------
                "writeCycleTime" -> sendCommand("Write cycleTime=${call.requireRange("minutes", 0, 10080)}", result)
                "writeCyNum" -> {
                    val count = call.requireInt("count").also { require(it in 1..24) { "等比例次数必须在 1..24" } }
                    sendCommand("Write cyNum=$count", result)
                }
                "writeRevpd" -> sendCommand("Write revpd=${call.requireRange("minutes", 0, 10080)}", result)
                "writeReppd" -> sendCommand("Write reppd=${call.requireRange("minutes", 0, 10080)}", result)

                // ---------------- 现场参数 ----------------
                "writeDiy" -> {
                    val hex = call.requireString("hex").replace(" ", "")
                    require(hex.length % 2 == 0 && hex.all { it.isDigit() || it in "abcdefABCDEF" }) { "ModBus 报文必须是偶数长度十六进制" }
                    sendCommand("Write DIY=$hex", result)
                }

                // ---------------- RTU 功能入口子页（LiherCYXRTUActivity） ----------------
                "readRtuInf" -> sendAndParse("Read RtuInf", result) { Cyx7Protocol.parseRtuInf(it) }
                "readLotInf" -> sendAndParse("Read LotInf", result) { Cyx7Protocol.parseLotInf(it) }
                "setStart" -> sendCommand("Set Start", result)
                "setStop" -> sendCommand("Set Stop", result)
                "setZeroInit" -> sendCommand("Set ZeroInit", result)
                "setDataUp" -> sendCommand("Set DataUp", result)
                "writePwrEn" -> sendCommand("Write PwrEn=${if (call.argument<Boolean>("enabled") == true) 1 else 0}", result)
                "writeSapInterval" -> sendCommand("Write SapInterval=${Cyx7Protocol.pad(call.requireRange("minutes", 0, 99999), 5)}", result)
                "writeUpInterval" -> sendCommand("Write UpInterval=${Cyx7Protocol.pad(call.requireRange("minutes", 0, 99999), 5)}", result)
                "writeSamWait" -> sendCommand("Write SamWait=${Cyx7Protocol.pad(call.requireRange("seconds", 1, 99999), 5)}", result)
                "writeDevST" -> sendCommand("Write DevST=${call.requireString("value")}", result)
                "writeDevMN" -> sendCommand("Write DevMN=${call.requireString("value")}", result)
                "writeLotType" -> {
                    val type = call.requireInt("type").also { require(it in 0..1) { "通信模组必须是 0(NB)/1(4G)" } }
                    sendCommand("Write LotType=$type", result)
                }
                "writeDevIp" -> sendCommand(
                    "Write DevIp=${Cyx7Protocol.pad(call.requireServiceIndex(), 2)}#" +
                        Cyx7Protocol.ipToProtocolValue(call.requireString("host")),
                    result,
                )
                "writeDevPort" -> sendCommand(
                    "Write DevPort=${Cyx7Protocol.pad(call.requireServiceIndex(), 2)}#" +
                        Cyx7Protocol.pad(call.requirePort("port"), 5),
                    result,
                )
                "writeProtocol" -> {
                    val index = call.requireServiceIndex()
                    val protocol = call.requireInt("protocol").also { require(it in 0..1) { "上传协议必须是 0(212)/1(Mqtt)" } }
                    sendCommand("Write Protocol=${Cyx7Protocol.pad(index, 2)}#$protocol", result)
                }
                "writeLotIpEn" -> sendCommand("Write LotIpEn=${Cyx7Protocol.pad(call.requireRange("bits", 0, 999), 3)}", result)
                "writeSamdevType" -> sendCommand("Write SamdevType=${Cyx7Protocol.pad(call.requireRange("value", 0, 99999), 5)}", result)

                // ---------------- 参数校准：ModBus 读写（Write DIY=<hex>） ----------------
                "modbusRead" -> {
                    val adr = call.requireRange("adr", 0, 255)
                    val address = call.requireRange("address", 0, 49999)
                    val count = call.requireRange("count", 1, 125)
                    sendAndParse("Write DIY=${Cyx7Modbus.buildRead(adr, address, count)}", result) {
                        Cyx7Modbus.parse(it) ?: mapOf("order" to -1, "raw" to it)
                    }
                }
                "modbusWrite" -> {
                    val adr = call.requireRange("adr", 0, 255)
                    val address = call.requireRange("address", 0, 49999)
                    val values = call.argument<List<Number>>("values")?.map { value -> value.toInt() }
                        ?: throw IllegalArgumentException("values is required")
                    sendAndParse("Write DIY=${Cyx7Modbus.buildWrite(adr, address, values)}", result) {
                        Cyx7Modbus.parse(it) ?: mapOf("order" to -1, "raw" to it)
                    }
                }

                // ---------------- 采样日志上报（原 RetrofitUtils.insertLog） ----------------
                "insertLog" -> insertLog(call, result)

                else -> result.notImplemented()
            }
        } catch (error: IllegalArgumentException) {
            result.error("INVALID_ARGUMENT", error.message, null)
        } catch (error: Exception) {
            result.error("CYX7_ERROR", error.message, null)
        }
    }

    private fun writePlan(call: MethodCall, result: MethodChannel.Result) {
        val raw = call.argument<List<Map<String, Any?>>>("segments")
            ?: throw IllegalArgumentException("segments is required")
        sendCommand(Cyx7Protocol.buildPlanCommand(raw), result)
    }

    private fun writeThreshold(call: MethodCall, result: MethodChannel.Result) {
        val kind = call.requireString("kind").lowercase()
        val command = when (kind) {
            "cond" -> "Write CondTh="
            "turb" -> "Write TurbTh="
            "oxygen" -> "Write OxygenTh="
            "watertemp" -> "Write WaterTempTh="
            "flow" -> "Write FlowTh="
            else -> throw IllegalArgumentException("unsupported threshold kind: $kind")
        }
        sendCommand(command + Cyx7Protocol.scaledInt(call.requireDouble("value"), 100), result)
    }

    /**
     * 采样日志上报，等价于原 RetrofitUtils.insertLog：
     * `POST <url>`，`Content-Type: application/json;charset=UTF-8`，`Authorization: Bearer <token>`，
     * body 为 `{blueMac, sampleId, operator, logContent}`。
     *
     * 与原程序一致的语义：只要 `blueMac` 非空就上报；
     * token 仅在非空时作为 `Authorization` 头携带（原工程该值来自其自身登录态）。
     */
    private fun insertLog(call: MethodCall, result: MethodChannel.Result) {
        val blueMac = call.argument<String>("blueMac")?.trim().orEmpty()
        if (blueMac.isEmpty()) {
            result.success(false)
            return
        }
        val url = call.argument<String>("url")?.trim()?.takeIf { it.isNotEmpty() } ?: defaultLogUrl
        val token = call.argument<String>("token")?.trim().orEmpty()
        val body = linkedMapOf<String, Any?>(
            "blueMac" to blueMac,
            "sampleId" to (call.argument<String>("sampleId") ?: ""),
            "operator" to (call.argument<String>("operator") ?: ""),
            "logContent" to (call.argument<String>("logContent") ?: ""),
        )
        Thread {
            var connection: java.net.HttpURLConnection? = null
            try {
                connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                    if (token.isNotEmpty()) setRequestProperty("Authorization", "Bearer $token")
                }
                connection.outputStream.use { output -> output.write(toJson(body).toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                mainHandler.post { result.success(code in 200..299) }
            } catch (error: Exception) {
                mainHandler.post { result.error("LOG_UPLOAD_FAILED", error.message, null) }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun toJson(values: Map<String, Any?>): String =
        values.entries.joinToString(",", "{", "}") { (key, value) ->
            val text = (value ?: "").toString().replace("\\", "\\\\").replace("\"", "\\\"")
            "\"$key\":\"$text\""
        }

    private fun sendCommand(command: String, result: MethodChannel.Result) {
        manager.sendCommand(command) { response, error ->
            if (error != null) result.error("COMMAND_FAILED", error, null)
            else result.success(linkedMapOf("ok" to true, "command" to command, "response" to response))
        }
    }

    private fun <T> sendAndParse(command: String, result: MethodChannel.Result, parser: (String) -> T) {
        manager.sendCommand(command) { response, error ->
            if (error != null) result.error("COMMAND_FAILED", error, null)
            else try { result.success(parser(response.orEmpty())) } catch (parseError: Exception) {
                result.error("PARSE_FAILED", parseError.message, mapOf("response" to response))
            }
        }
    }

    private fun MethodCall.requireString(name: String): String = argument<String>(name)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("$name is required")

    private fun MethodCall.requireInt(name: String): Int = argument<Number>(name)?.toInt()
        ?: throw IllegalArgumentException("$name is required")

    private fun MethodCall.requireDouble(name: String): Double = argument<Number>(name)?.toDouble()
        ?: throw IllegalArgumentException("$name is required")

    private fun MethodCall.requireRange(name: String, min: Int, max: Int): Int =
        requireInt(name).also { require(it in min..max) { "$name must be in $min..$max" } }

    private fun MethodCall.requireServiceIndex(): Int =
        requireRange("index", 0, 4)

    private fun MethodCall.requirePort(name: String): Int = requireRange(name, 1, 99999)
}
