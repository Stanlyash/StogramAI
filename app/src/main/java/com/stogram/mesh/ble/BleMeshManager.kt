package com.stogram.mesh.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Менеджер BLE для Mesh сети.
 * Отвечает за сканирование соседей и рекламу собственного узла.
 */
class BleMeshManager(private val context: Context) {

    companion object {
        private const val TAG = "BleMeshManager"
        
        // UUID службы Stogram Mesh
        val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        // UUID характеристики для обмена данными
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        
        // Manufacturer ID для кастомных данных в Advertising Packet
        private const val MANUFACTURER_ID = 0x004C 
    }

    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
        }
    }

    private val advertisingCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertising failed with error: $errorCode")
        }
    }

    // Поток найденных устройств (Address -> Data)
    private val _discoveredNodes = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val discoveredNodes: StateFlow<Map<String, ByteArray>> = _discoveredNodes

    private var isScanning = false
    private var isAdvertising = false

    init {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth")
        }
    }

    /**
     * Запуск сканирования узлов Mesh
     */
    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (bluetoothAdapter == null || isScanning) return

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bluetoothAdapter.bluetoothLeScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
            isScanning = true
            Log.d(TAG, "Started scanning for Stogram Mesh nodes")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for scanning", e)
        }
    }

    /**
     * Остановка сканирования
     */
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (bluetoothAdapter == null || !isScanning) return

        try {
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "Stopped scanning")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to stop scanning", e)
        }
    }

    /**
     * Запуск рекламы узла
     * @param nodeData Данные текущего узла для передачи в пакете рекламы
     */
    @SuppressLint("MissingPermission")
    fun startAdvertising(nodeData: ByteArray) {
        if (bluetoothAdapter == null || isAdvertising) return

        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val parcelUuid = ParcelUuid(SERVICE_UUID)
        
        // Формируем данные для рекламы
        val manufacturerData = byteArrayOf(0x01) + nodeData.take(20).toByteArray()

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(parcelUuid)
            .addManufacturerData(MANUFACTURER_ID, manufacturerData)
            .build()

        try {
            bluetoothAdapter.bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertisingCallback)
            isAdvertising = true
            Log.d(TAG, "Started advertising as Stogram Node")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start advertising", e)
        }
    }

    /**
     * Остановка рекламы
     */
    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (bluetoothAdapter == null || !isAdvertising) return

        try {
            bluetoothAdapter.bluetoothLeAdvertiser.stopAdvertising(advertisingCallback)
            isAdvertising = false
            Log.d(TAG, "Stopped advertising")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop advertising", e)
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val scanRecord = result.scanRecord ?: return
        
        val manufacturerData = scanRecord.getManufacturerSpecificData(MANUFACTURER_ID)
        
        if (manufacturerData != null) {
            Log.d(TAG, "Found Mesh Node: ${device.address}, Data: ${manufacturerData.size} bytes")
            
            val currentMap = _discoveredNodes.value.toMutableMap()
            currentMap[device.address] = manufacturerData
            _discoveredNodes.value = currentMap
        }
    }
    
    /**
     * Очистка ресурсов
     */
    fun destroy() {
        stopScanning()
        stopAdvertising()
    }
}
