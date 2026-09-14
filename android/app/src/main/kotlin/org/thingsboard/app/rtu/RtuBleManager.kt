package org.thingsboard.app.rtu

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

class RtuBleManager(private val context: Context) {
    interface Listener {
        fun onEvent(name: String, data: Any?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private val decoder = RtuProtocol.StreamDecoder()
    private val encryptedDecoder = RtuProtocol.EncryptedStreamDecoder()
    private val serviceUuid = UUID.fromString(RtuProtocol.serviceUuid)
    private val sendUuid = UUID.fromString(RtuProtocol.sendCharacteristicUuid)
    private val receiveUuid = UUID.fromString(RtuProtocol.receiveCharacteristicUuid)
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
        val compact = address.replace(":", "").replace("-", "").trim().uppercase()
        if (!compact.startsWith("15") || compact.length != 12 || compact.any { it !in "0123456789ABCDEF" }) {
            callback(false, "Only 12-digit Lihe RTU addresses with prefix 15 are supported"); return
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
            sendBytes(RtuProtocol.buildCommand(command), callback)
            return
        }
        sendChunkedPayload(bytes, callback)
    }

    fun sendChunkedPayload(payload: ByteArray, callback: (String?, String?) -> Unit) {
        val payloadChunks = payload.asList().chunked(50).map { it.toByteArray() }
        require(payloadChunks.size <= 0xffff) { "RTU chunk count exceeds protocol limit" }
        val frames = payloadChunks.mapIndexed { index, chunk -> RtuProtocol.buildChunk(chunk, index, payloadChunks.size) }
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
                callback(null, error ?: "RTU did not acknowledge chunk $index/${frames.size}")
            }
        }
    }

    fun sendBytes(bytes: ByteArray, callback: (String?, String?) -> Unit) {
        if (!isConnected()) { callback(null, "RTU is not connected"); return }
        if (pendingResponse != null) { callback(null, "Another RTU command is waiting for a response"); return }
        pendingResponse = { response, error ->
            listener?.onEvent("commandResult", mapOf(
                "ok" to (error == null),
                "response" to (response ?: ""),
                "error" to (error ?: ""),
            ))
            callback(response, error)
        }
        pendingTimeout = Runnable { clearPending("RTU response timeout") }
        mainHandler.postDelayed(pendingTimeout!!, 15_000L)
        val wireBytes = if (encrypted) RtuProtocol.encrypt(bytes) else bytes
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
                // A number of RTU firmwares use a small UART bridge behind
                // BLE and cannot consume back-to-back write-without-response
                // packets. Keep the no-response mode, but pace packets like
                // the original demo's write/ack loop.
                mainHandler.postDelayed({ writeNextChunk() }, 15L)
            } else pendingWriteCallback = null
        }
        if (!localGatt.writeCharacteristic(characteristic)) {
            pendingWriteCallback?.invoke(false, "BLE writeCharacteristic returned false")
        } else if (characteristicWriteType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            // WRITE_TYPE_NO_RESPONSE does not reliably produce
            // onCharacteristicWrite on all Android/device combinations.
            // Advance immediately after the framework accepts the write.
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
                "isRtu" to address.startsWith("15"),
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
                connectTimeout = Runnable { retryOrFailConnection("RTU service setup timeout") }
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
            // The 20260709 protocol document describes the frame format but
            // does not publish UUIDs. Keep the known UUIDs as the preference,
            // then fall back to the characteristic properties advertised by
            // the device. This prevents selecting a notify-only attribute as
            // the TX characteristic (which produces GATT status 3).
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
                retryOrFailConnection("RTU service or characteristic not found")
                return
            }
            if (!notificationDescriptorPending) requestMtuOrFinish(localGatt)
        }

        override fun onDescriptorWrite(localGatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            // The receive characteristic may be selected via the advertised
            // properties when a device does not expose the preferred UUID.
            // Match the characteristic actually configured above rather than
            // the preferred constant, otherwise setup times out on such devices.
            if (status == BluetoothGatt.GATT_SUCCESS && descriptor.characteristic.uuid == receiveCharacteristic?.uuid) {
                notificationDescriptorPending = false
                requestMtuOrFinish(localGatt)
            } else if (descriptor.characteristic.uuid == receiveCharacteristic?.uuid) {
                retryOrFailConnection("Enabling RTU notifications failed: $status")
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
            // Use the UUID selected during service discovery; fallback
            // characteristic mapping is supported when firmware UUIDs differ.
            if (characteristic.uuid != receiveCharacteristic?.uuid) return
            // The demo encrypts the outgoing frame in ProtocolUtil, but its
            // receive callback passes the notification directly to
            // getReturnMsgForReceive(), which expects a raw AA BB frame.  In
            // other firmware revisions the response is encrypted as well.
            // Decode both forms so either device revision works (and avoid a
            // false timeout when a perfectly valid plaintext response arrives).
            listener?.onEvent("receiveRaw", mapOf(
                "length" to value.size,
                "hex" to value.joinToString("") { "%02X".format(it.toInt() and 0xff) },
            ))
            val plainResponses = decoder.append(value)
            val encryptedResponses = if (encrypted) encryptedDecoder.append(value) else emptyList()
            val responses = (plainResponses + encryptedResponses).distinct()
            responses.forEach { response ->
                // When the RTU is started with the top-level device switch it
                // may push its scheduled sampling result without a request
                // from the app.  The SampleBox keeps those unsolicited
                // reports out of the command panel.  Only a response that
                // belongs to the command currently waiting for a reply is
                // forwarded as the normal `response` event.
                val callback = pendingResponse
                if (callback != null) {
                    listener?.onEvent("response", mapOf("text" to response))
                    pendingTimeout?.let(mainHandler::removeCallbacks)
                    pendingTimeout = null
                    pendingResponse = null
                    chunks = emptyList()
                    callback(response, null)
                } else {
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
