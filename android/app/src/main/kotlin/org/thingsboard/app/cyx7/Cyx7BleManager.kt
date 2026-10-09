package org.thingsboard.app.cyx7

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CYX7 采样箱 BLE 管理器。
 *
 * 与 RTU 侧保持同一套连接策略（原 SampleBox 行为）：
 *  - 连接失败最多重试 5 次，间隔 500ms，单次 8s 超时；
 *  - 服务发现优先用力合 UUID，找不到时按特征值属性回退；
 *  - 订阅接收通道通知后协商 MTU=255；
 *  - 指令 15s 无响应即超时；无自动重传（升级分包除外，重试 3 次）。
 */
class Cyx7BleManager(private val context: Context) {
    interface Listener {
        fun onEvent(name: String, data: Any?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private val decoder = Cyx7Protocol.StreamDecoder()
    private val encryptedDecoder = Cyx7Protocol.EncryptedStreamDecoder()
    private val serviceUuid = UUID.fromString(Cyx7Protocol.serviceUuid)
    private val sendUuid = UUID.fromString(Cyx7Protocol.sendCharacteristicUuid)
    private val receiveUuid = UUID.fromString(Cyx7Protocol.receiveCharacteristicUuid)
    private var listener: Listener? = null
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var sendCharacteristic: BluetoothGattCharacteristic? = null
    private var receiveCharacteristic: BluetoothGattCharacteristic? = null
    private var mtuPayload = 20
    private var characteristicWriteType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    private var connectCallback: ((Boolean, String?) -> Unit)? = null
    private var connectDevice: BluetoothDevice? = null
    private var connectAttempts = 0
    private var connectTimeout: Runnable? = null
    private var pendingResponse: ((String?, String?) -> Unit)? = null
    private var pendingTimeout: Runnable? = null
    private var chunks: List<ByteArray> = emptyList()
    private var chunkIndex = 0
    private var pendingWriteCallback: ((Boolean, String?) -> Unit)? = null
    private var notificationDescriptorPending = false
    private val scanning = AtomicBoolean(false)
    private var encrypted = true
    private var scanCompletion: ((Boolean, String?) -> Unit)? = null
    private var scanStopRunnable: Runnable? = null

    fun setListener(value: Listener?) { listener = value }
    fun setEncrypted(value: Boolean) { encrypted = value }

    private fun hasBluetoothPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun hasScanPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startScan(durationMs: Long = 10_000L, callback: (Boolean, String?) -> Unit) {
        if (!hasScanPermission()) { callback(false, "BLUETOOTH_SCAN permission is required"); return }
        val bt = adapter
        if (bt == null || !bt.isEnabled) { callback(false, "Bluetooth is disabled"); return }
        if (scanning.getAndSet(true)) { callback(true, null); return }
        scanCompletion = callback
        scanner = bt.bluetoothLeScanner
        listener?.onEvent("scanStarted", null)
        scanner?.startScan(scanCallback)
        scanStopRunnable = Runnable { stopScan() }
        mainHandler.postDelayed(scanStopRunnable!!, durationMs)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!scanning.getAndSet(false)) return
        scanner?.stopScan(scanCallback)
        scanner = null
        scanStopRunnable?.let(mainHandler::removeCallbacks)
        scanStopRunnable = null
        val completion = scanCompletion
        scanCompletion = null
        listener?.onEvent("scanStopped", null)
        completion?.invoke(true, null)
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String, callback: (Boolean, String?) -> Unit) {
        if (!hasBluetoothPermission()) { callback(false, "BLUETOOTH_CONNECT permission is required"); return }
        val compact = try {
            Cyx7Protocol.validateAddress(address)
        } catch (error: IllegalArgumentException) {
            callback(false, error.message)
            return
        }
        val normalized = compact.chunked(2).joinToString(":")
        val bt = adapter
        if (bt == null || !bt.isEnabled) { callback(false, "Bluetooth is disabled"); return }
        val device = try { bt.getRemoteDevice(normalized) } catch (_: IllegalArgumentException) { null }
        if (device == null) { callback(false, "Invalid Bluetooth address"); return }
        disconnect()
        connectCallback = callback
        connectDevice = device
        connectAttempts = 0
        beginConnect()
    }

    @SuppressLint("MissingPermission")
    private fun beginConnect() {
        val device = connectDevice ?: return
        connectAttempts++
        listener?.onEvent("connecting", mapOf("address" to device.address, "attempt" to connectAttempts))
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else device.connectGatt(context, false, gattCallback)
        connectTimeout = Runnable { retryOrFailConnection("BLE connection timeout") }
        mainHandler.postDelayed(connectTimeout!!, 8_000L)
    }

    @SuppressLint("MissingPermission")
    private fun retryOrFailConnection(error: String) {
        connectTimeout?.let(mainHandler::removeCallbacks)
        connectTimeout = null
        gatt?.close()
        gatt = null
        sendCharacteristic = null
        receiveCharacteristic = null
        if (connectCallback != null && connectAttempts < 5) {
            mainHandler.postDelayed({ beginConnect() }, 500L)
        } else {
            val callback = connectCallback
            connectCallback = null
            callback?.invoke(false, "$error (attempts: $connectAttempts)")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        connectTimeout?.let(mainHandler::removeCallbacks)
        connectTimeout = null
        connectCallback = null
        connectDevice = null
        clearPending("Disconnected")
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        sendCharacteristic = null
        receiveCharacteristic = null
        listener?.onEvent("disconnected", null)
    }

    fun isConnected(): Boolean = gatt != null && sendCharacteristic != null && receiveCharacteristic != null

    fun sendCommand(command: String, callback: (String?, String?) -> Unit) {
        listener?.onEvent("commandStarted", mapOf("command" to command))
        val bytes = command.toByteArray(Charsets.UTF_8)
        if (bytes.size <= 50) {
            sendBytes(Cyx7Protocol.buildCommand(command), callback)
            return
        }
        sendChunkedPayload(bytes, callback)
    }

    /** 长报文按 50 字节分包（与 ProtocolUtil 分包版一致），升级文件也走这里。 */
    fun sendChunkedPayload(payload: ByteArray, callback: (String?, String?) -> Unit) {
        val payloadChunks = payload.asList().chunked(50).map { it.toByteArray() }
        require(payloadChunks.size <= 0xffff) { "CYX7 chunk count exceeds protocol limit" }
        val frames = payloadChunks.mapIndexed { index, chunk -> Cyx7Protocol.buildChunk(chunk, index, payloadChunks.size) }
        sendFrameSequence(frames, callback)
    }

    private fun sendFrameSequence(
        frames: List<ByteArray>,
        callback: (String?, String?) -> Unit,
        index: Int = 0,
        attempts: Int = 0,
    ) {
        if (index >= frames.size) { callback("OK", null); return }
        sendBytes(frames[index]) { response, error ->
            val acknowledged = index == frames.lastIndex || response?.contains("$index#${frames.size}#OK") == true
            if (error == null && acknowledged) {
                if (index == frames.lastIndex) callback(response, null)
                else sendFrameSequence(frames, callback, index + 1)
            } else if (attempts < 3) {
                mainHandler.postDelayed({ sendFrameSequence(frames, callback, index, attempts + 1) }, 500L)
            } else {
                callback(null, error ?: "采样箱未确认分包 $index/${frames.size}")
            }
        }
    }

    fun sendBytes(bytes: ByteArray, callback: (String?, String?) -> Unit) {
        if (!isConnected()) { callback(null, "采样箱未连接"); return }
        if (pendingResponse != null) { callback(null, "上一条指令仍在等待响应"); return }
        pendingResponse = { response, error ->
            listener?.onEvent("commandResult", mapOf(
                "ok" to (error == null),
                "response" to (response ?: ""),
                "error" to (error ?: ""),
            ))
            callback(response, error)
        }
        pendingTimeout = Runnable { clearPending("采样箱响应超时") }
        mainHandler.postDelayed(pendingTimeout!!, 15_000L)
        val wireBytes = if (encrypted) Cyx7Protocol.encrypt(bytes) else bytes
        listener?.onEvent("sendRaw", mapOf(
            "encrypted" to encrypted,
            "length" to wireBytes.size,
            "hex" to wireBytes.joinToString("") { "%02X".format(it.toInt() and 0xff) },
        ))
        chunks = wireBytes.asList().chunked(mtuPayload.coerceAtLeast(20)).map { it.toByteArray() }
        chunkIndex = 0
        writeNextChunk()
    }

    @SuppressLint("MissingPermission")
    private fun writeNextChunk() {
        val localGatt = gatt
        val characteristic = sendCharacteristic
        if (localGatt == null || characteristic == null || chunkIndex >= chunks.size) {
            if (chunkIndex >= chunks.size) pendingWriteCallback?.invoke(true, null)
            return
        }
        characteristic.writeType = characteristicWriteType
        characteristic.value = chunks[chunkIndex]
        pendingWriteCallback = writeCallback@{ ok, error ->
            if (!ok) { clearPending(error ?: "BLE write failed"); return@writeCallback }
            chunkIndex++
            if (chunkIndex < chunks.size) {
                // 设备侧多为 UART 桥接，背靠背写入会丢包，按原程序节奏留 15ms。
                mainHandler.postDelayed({ writeNextChunk() }, 15L)
            } else pendingWriteCallback = null
        }
        if (!localGatt.writeCharacteristic(characteristic)) {
            pendingWriteCallback?.invoke(false, "BLE writeCharacteristic returned false")
        } else if (characteristicWriteType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            pendingWriteCallback?.invoke(true, null)
        }
    }

    private fun clearPending(error: String?) {
        pendingTimeout?.let(mainHandler::removeCallbacks)
        pendingTimeout = null
        val callback = pendingResponse
        pendingResponse = null
        chunks = emptyList()
        chunkIndex = 0
        pendingWriteCallback = null
        callback?.invoke(null, error)
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(localGatt: BluetoothGatt): Boolean {
        notificationDescriptorPending = false
        val characteristic = receiveCharacteristic ?: return false
        if (!localGatt.setCharacteristicNotification(characteristic, true)) return false
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (descriptor == null) return true
        descriptor.value = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0 &&
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        notificationDescriptorPending = localGatt.writeDescriptor(descriptor)
        return notificationDescriptorPending
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address.uppercase()
            listener?.onEvent("scanResult", mapOf(
                "address" to address,
                "name" to (device.name ?: result.scanRecord?.deviceName ?: ""),
                "rssi" to result.rssi,
                "isBox" to address.startsWith(Cyx7Protocol.addressPrefix),
                "isCyx7" to Cyx7Protocol.isCyx7Address(address),
            ))
        }

        override fun onScanFailed(errorCode: Int) {
            scanning.set(false)
            scanStopRunnable?.let(mainHandler::removeCallbacks)
            scanStopRunnable = null
            val completion = scanCompletion
            scanCompletion = null
            listener?.onEvent("scanError", mapOf("code" to errorCode, "message" to "BLE scan failed"))
            completion?.invoke(false, "BLE scan failed: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(localGatt: BluetoothGatt, status: Int, newState: Int) {
            if (localGatt !== gatt) { localGatt.close(); return }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectTimeout?.let(mainHandler::removeCallbacks)
                connectTimeout = Runnable { retryOrFailConnection("采样箱服务初始化超时") }
                mainHandler.postDelayed(connectTimeout!!, 8_000L)
                listener?.onEvent("connected", mapOf("status" to status))
                localGatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                listener?.onEvent("disconnected", mapOf("status" to status))
                clearPending("BLE disconnected")
                if (connectCallback != null) retryOrFailConnection("BLE disconnected (status $status)")
                else {
                    localGatt.close()
                    gatt = null
                    sendCharacteristic = null
                    receiveCharacteristic = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(localGatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                retryOrFailConnection("Service discovery failed: $status")
                return
            }
            // 优先使用力合固定 UUID，找不到时按「可写 + 可通知」属性回退，
            // 避免把只支持 notify 的特征值当成发送通道。
            val preferredService = localGatt.getService(serviceUuid)
            val service = preferredService ?: localGatt.services.firstOrNull { candidate ->
                candidate.characteristics.any { it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 } &&
                    candidate.characteristics.any { it.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 }
            }
            val preferredTx = service?.getCharacteristic(sendUuid)
            val preferredRx = service?.getCharacteristic(receiveUuid)
            sendCharacteristic = preferredTx?.takeIf {
                it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
            } ?: service?.characteristics?.firstOrNull {
                it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
            }
            receiveCharacteristic = preferredRx?.takeIf {
                it.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            } ?: service?.characteristics?.firstOrNull {
                it.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            }
            val tx = sendCharacteristic
            val txProperties = tx?.properties ?: 0
            characteristicWriteType = when {
                txProperties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                txProperties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0 -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                else -> -1
            }
            listener?.onEvent("characteristicsReady", mapOf(
                "serviceUuid" to (service?.uuid?.toString() ?: ""),
                "sendUuid" to (tx?.uuid?.toString() ?: ""),
                "sendProperties" to txProperties,
                "sendWriteType" to characteristicWriteType,
                "receiveUuid" to (receiveCharacteristic?.uuid?.toString() ?: ""),
                "receiveProperties" to (receiveCharacteristic?.properties ?: 0),
            ))
            if (tx == null || receiveCharacteristic == null || characteristicWriteType < 0 || !enableNotifications(localGatt)) {
                retryOrFailConnection("采样箱服务或特征值未找到")
                return
            }
            if (!notificationDescriptorPending) requestMtuOrFinish(localGatt)
        }

        override fun onDescriptorWrite(localGatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && descriptor.characteristic.uuid == receiveCharacteristic?.uuid) {
                notificationDescriptorPending = false
                requestMtuOrFinish(localGatt)
            } else if (descriptor.characteristic.uuid == receiveCharacteristic?.uuid) {
                retryOrFailConnection("订阅采样箱通知失败: $status")
            }
        }

        override fun onMtuChanged(localGatt: BluetoothGatt, mtu: Int, status: Int) {
            mtuPayload = (if (status == BluetoothGatt.GATT_SUCCESS) mtu else 23).minus(3).coerceAtLeast(20)
            listener?.onEvent("mtuChanged", mapOf("mtu" to (mtuPayload + 3), "payload" to mtuPayload))
            finishConnected()
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicWrite(localGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid == sendCharacteristic?.uuid && characteristicWriteType != BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                pendingWriteCallback?.invoke(status == BluetoothGatt.GATT_SUCCESS, "BLE write failed: $status")
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(localGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleNotification(characteristic, characteristic.value ?: return)
        }

        override fun onCharacteristicChanged(localGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handleNotification(characteristic, value)
        }

        private fun handleNotification(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid != receiveCharacteristic?.uuid) return
            listener?.onEvent("receiveRaw", mapOf(
                "length" to value.size,
                "hex" to value.joinToString("") { "%02X".format(it.toInt() and 0xff) },
            ))
            val plainResponses = decoder.append(value)
            val encryptedResponses = if (encrypted) encryptedDecoder.append(value) else emptyList()
            val responses = (plainResponses + encryptedResponses).distinct()
            responses.forEach { response ->
                val callback = pendingResponse
                if (callback != null) {
                    listener?.onEvent("response", mapOf("text" to response))
                    pendingTimeout?.let(mainHandler::removeCallbacks)
                    pendingTimeout = null
                    pendingResponse = null
                    chunks = emptyList()
                    callback(response, null)
                } else {
                    // 采样箱会主动推送计划采样结果，原程序不把它当成指令应答。
                    listener?.onEvent("unsolicitedResponse", mapOf("text" to response))
                }
            }
        }
    }

    private fun finishConnected() {
        connectTimeout?.let(mainHandler::removeCallbacks)
        connectTimeout = null
        connectCallback?.invoke(true, null)
        connectCallback = null
        connectDevice = null
        listener?.onEvent("ready", mapOf("payload" to mtuPayload))
    }

    @SuppressLint("MissingPermission")
    private fun requestMtuOrFinish(localGatt: BluetoothGatt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && localGatt.requestMtu(255)) return
        mtuPayload = 20
        finishConnected()
    }
}
