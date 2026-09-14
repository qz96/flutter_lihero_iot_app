package org.thingsboard.app.rtu

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RtuMethodChannel private constructor(context: Context, messenger: BinaryMessenger) :
    MethodChannel.MethodCallHandler, EventChannel.StreamHandler, RtuBleManager.Listener {

    companion object {
        fun register(context: Context, messenger: BinaryMessenger): RtuMethodChannel {
            return RtuMethodChannel(context, messenger)
        }
    }

    private val manager = RtuBleManager(context.applicationContext)
    private val channel = MethodChannel(messenger, "lihero_rtu")
    private val mainHandler = Handler(Looper.getMainLooper())
    private var eventSink: EventChannel.EventSink? = null

    init {
        channel.setMethodCallHandler(this)
        EventChannel(messenger, "lihero_rtu/events").setStreamHandler(this)
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
                "startScan" -> manager.startScan((call.argument<Number>("durationMs")?.toLong() ?: 10_000L)) { ok, error ->
                    if (ok) result.success(true) else result.error("SCAN_FAILED", error, null)
                }
                "stopScan" -> { manager.stopScan(); result.success(true) }
                "connect" -> manager.connect(call.requireString("address")) { ok, error ->
                    if (ok) result.success(true) else result.error("CONNECT_FAILED", error, null)
                }
                "disconnect" -> { manager.disconnect(); result.success(true) }
                "isConnected" -> result.success(manager.isConnected())
                "setEncryption" -> {
                    require(!manager.isConnected()) { "Disconnect RTU before changing encryption mode" }
                    manager.setEncrypted(call.argument<Boolean>("enabled") ?: true)
                    result.success(true)
                }
                "sendCommand" -> sendCommand(call.requireString("command"), result)
                "readBasic" -> sendAndParse("Read RtuInf", result) { RtuProtocol.parseBasic(it) }
                "readLot" -> sendAndParse("Read LotInf", result) { RtuProtocol.parseLot(it) }
                "readSensor" -> {
                    val type = call.argument<String>("type")?.lowercase() ?: "llj"
                    sendAndParse("Read SamInf", result) { RtuProtocol.parseSensor(it, type) }
                }
                "start" -> sendCommand("Set Start", result)
                "stop" -> sendCommand("Set Stop", result)
                "zero" -> sendCommand("Set ZeroInit", result)
                "upload" -> sendCommand("Set DataUp", result)
                "setPower" -> sendCommand("Write PwrEn=${if (call.argument<Boolean>("enabled") == true) 1 else 0}", result)
                "setSampleInterval" -> sendCommand("Write SapInterval=${RtuProtocol.pad(call.requireNonNegativeInt("minutes"), 5)}", result)
                "setUploadInterval" -> sendCommand("Write UpInterval=${RtuProtocol.pad(call.requireNonNegativeInt("minutes"), 5)}", result)
                "setSystemTime" -> sendCommand("Write SysTime=${normalizeSystemTime(call.requireString("value"))}", result)
                "setMn" -> sendCommand("Write DevMN=${call.requireString("value").trim()}", result)
                "setBleAddress" -> sendCommand("Write BleAddr=${RtuProtocol.validateRtuAddress(call.requireString("value"))}", result)
                "setSystemSt" -> sendCommand("Write DevST=${call.requireString("value").trim()}", result)
                "setLotType" -> {
                    val type = call.requireInt("type").also {
                        require(it in 0..1) { "module type must be 0 (NB) or 1 (4G)" }
                    }
                    sendCommand("Write LotType=$type", result)
                }
                "setSensorDeviceType" -> sendCommand("Write SamdevType=${RtuProtocol.pad(call.requireInt("value"), 5)}", result)
                "setSensorWait" -> sendCommand("Write SamWait=${RtuProtocol.pad(call.requirePositiveInt("seconds"), 5)}", result)
                "setNetwork" -> setNetwork(call, result)
                "setNetworkIp" -> setNetworkIp(call, result)
                "setNetworkPort" -> setNetworkPort(call, result)
                "setNetworkProtocol" -> setNetworkProtocol(call, result)
                "setNetworkEnabled" -> setNetworkEnabled(call, result)
                "removeNetwork" -> removeNetwork(call, result)
                "upgradeBin" -> upgradeBin(call.requireString("path"), result)
                else -> result.notImplemented()
            }
        } catch (error: IllegalArgumentException) {
            result.error("INVALID_ARGUMENT", error.message, null)
        } catch (error: Exception) {
            result.error("RTU_ERROR", error.message, null)
        }
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

    private fun setNetwork(call: MethodCall, result: MethodChannel.Result) {
        val index = call.requireInt("index")
        val host = call.requireString("host")
        val port = call.requireInt("port")
        val protocol = call.requireInt("protocol")
        val enabled = call.argument<Boolean>("enabled") ?: true
        val currentBits = call.argument<Int>("currentEnabledBits") ?: 0
        val desiredBits = if (enabled) currentBits or (1 shl index) else currentBits and (1 shl index).inv()
        // Keep a disabled service's IP/port/protocol.  SampleBox only clears
        // DevIp in the explicit delete flow; setting a service to "停用" must
        // only change the LotIpEn bitmap so it can be enabled again later.
        val networkCommands = RtuProtocol.commandForNetwork(index, host, port, protocol, true)
        val enableCommand = "Write LotIpEn=${RtuProtocol.pad(desiredBits, 3)}"
        // This follows LiherRTUActivity's add-service chain exactly: IP,
        // port, protocol, then the LotIpEn bitmap.  A disabled entry keeps
        // its values and is simply not selected in that bitmap.
        sendCommands(networkCommands + enableCommand, result)
    }

    /**
     * The SampleBox UI edits every communications-service field independently.
     * Keep that behavior here so changing an IP does not overwrite the port,
     * protocol, or enabled state with stale values from Flutter.
     */
    private fun setNetworkIp(call: MethodCall, result: MethodChannel.Result) {
        val index = call.requireServiceIndex()
        val host = call.requireString("host")
        sendCommand("Write DevIp=${RtuProtocol.pad(index, 2)}#${RtuProtocol.ipToProtocolValue(host)}", result)
    }

    private fun setNetworkPort(call: MethodCall, result: MethodChannel.Result) {
        val index = call.requireServiceIndex()
        val port = call.requireInt("port").also { require(it in 1..99999) { "port must be between 1 and 99999" } }
        sendCommand("Write DevPort=${RtuProtocol.pad(index, 2)}#${RtuProtocol.pad(port, 5)}", result)
    }

    private fun setNetworkProtocol(call: MethodCall, result: MethodChannel.Result) {
        val index = call.requireServiceIndex()
        val protocol = call.requireInt("protocol").also { require(it in 0..1) { "protocol must be 0 (212) or 1 (MQTT)" } }
        sendCommand("Write Protocol=${RtuProtocol.pad(index, 2)}#$protocol", result)
    }

    private fun setNetworkEnabled(call: MethodCall, result: MethodChannel.Result) {
        val index = call.requireServiceIndex()
        val enabled = call.argument<Boolean>("enabled") ?: true
        val currentBits = call.argument<Number>("currentEnabledBits")?.toInt() ?: 0
        val desiredBits = if (enabled) currentBits or (1 shl index) else currentBits and (1 shl index).inv()
        sendCommand("Write LotIpEn=${RtuProtocol.pad(desiredBits, 3)}", result)
    }

    /** Matches the demo: disable the slot first, then clear its IP value. */
    private fun removeNetwork(call: MethodCall, result: MethodChannel.Result) {
        val index = call.requireServiceIndex()
        val currentBits = call.argument<Number>("currentEnabledBits")?.toInt() ?: 0
        val desiredBits = currentBits and (1 shl index).inv()
        sendCommands(
            listOf(
                "Write LotIpEn=${RtuProtocol.pad(desiredBits, 3)}",
                "Write DevIp=${RtuProtocol.pad(index, 2)}#000000000000",
            ),
            result,
        )
    }

    private fun sendCommands(commands: List<String>, result: MethodChannel.Result, index: Int = 0, responses: MutableList<String> = mutableListOf()) {
        if (index >= commands.size) {
            result.success(linkedMapOf("ok" to true, "commands" to commands, "responses" to responses))
            return
        }
        manager.sendCommand(commands[index]) { response, error ->
            if (error != null) result.error("COMMAND_FAILED", error, mapOf("command" to commands[index]))
            else { responses += response.orEmpty(); sendCommands(commands, result, index + 1, responses) }
        }
    }

    private fun upgradeBin(path: String, result: MethodChannel.Result) {
        val file = File(path)
        require(file.isFile) { "Upgrade file not found: $path" }
        require(file.length() > 0) { "Upgrade file is empty" }
        require(file.length() <= 50L * 0xffff) { "Upgrade file exceeds RTU chunk-number limit" }
        // Preserve file bytes and use the RTU application's numbered 50-byte packets.
        manager.sendChunkedPayload(file.readBytes(), { response, error ->
            if (error != null) result.error("UPGRADE_FAILED", error, null)
            else result.success(mapOf("ok" to true, "bytes" to file.length(), "response" to response))
        })
    }

    private fun normalizeSystemTime(value: String): String {
        val trimmed = value.trim()
        if (trimmed.matches(Regex("\\d{14}"))) return trimmed.substring(2)
        val parsed = LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return parsed.format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
    }

    private fun MethodCall.requireString(name: String): String = argument<String>(name)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("$name is required")

    private fun MethodCall.requireInt(name: String): Int = argument<Number>(name)?.toInt()
        ?: throw IllegalArgumentException("$name is required")

    private fun MethodCall.requireServiceIndex(): Int = requireInt("index").also {
        require(it in 0..4) { "service index must be 0..4" }
    }

    private fun MethodCall.requireNonNegativeInt(name: String): Int = requireInt(name).also {
        require(it >= 0) { "$name must be non-negative" }
    }

    private fun MethodCall.requirePositiveInt(name: String): Int = requireInt(name).also {
        require(it > 0) { "$name must be greater than zero" }
    }
}
