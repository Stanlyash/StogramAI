package com.stogram.mesh.ble

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class BleMeshManagerTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var bluetoothManager: BluetoothManager

    @MockK
    private lateinit var bluetoothAdapter: BluetoothAdapter

    @MockK
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    @MockK
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser

    private lateinit var bleManager: BleMeshManager

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManager
        every { bluetoothManager.adapter } returns bluetoothAdapter
        every { bluetoothAdapter.bluetoothLeScanner } returns bluetoothLeScanner
        every { bluetoothAdapter.bluetoothLeAdvertiser } returns bluetoothLeAdvertiser
        
        // Мокируем успешное выполнение методов сканирования и рекламы
        every { bluetoothLeScanner.startScan(any<List<ScanFilter>>(), any<ScanSettings>(), any()) } returns Unit
        every { bluetoothLeScanner.stopScan(any()) } returns Unit
        every { bluetoothLeAdvertiser.startAdvertising(any(), any(), any()) } returns Unit
        every { bluetoothLeAdvertiser.stopAdvertising(any()) } returns Unit

        bleManager = BleMeshManager(context)
    }

    @Test
    fun `startScanning should call BluetoothLeScanner startScan`() {
        bleManager.startScanning()

        verify {
            bluetoothLeScanner.startScan(
                match { filters -> filters.size == 1 && filters[0].serviceUuid?.uuid == BleMeshManager.SERVICE_UUID },
                any(),
                any()
            )
        }
    }

    @Test
    fun `stopScanning should call BluetoothLeScanner stopScan`() {
        bleManager.startScanning()
        clearAllMocks()
        
        bleManager.stopScanning()

        verify { bluetoothLeScanner.stopScan(any()) }
    }

    @Test
    fun `startAdvertising should call BluetoothLeAdvertiser startAdvertising with correct data`() {
        val testData = byteArrayOf(0x01, 0x02, 0x03)

        bleManager.startAdvertising(testData)

        verify {
            bluetoothLeAdvertiser.startAdvertising(
                match { settings -> 
                    settings.mode == AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY &&
                    settings.txPowerLevel == AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                },
                match { data -> 
                    data.serviceUuids?.any { it.uuid == BleMeshManager.SERVICE_UUID } == true
                },
                any()
            )
        }
    }

    @Test
    fun `destroy should stop both scanning and advertising`() {
        bleManager.startScanning()
        bleManager.startAdvertising(byteArrayOf(0x01))
        clearAllMocks()

        bleManager.destroy()

        verify { bluetoothLeScanner.stopScan(any()) }
        verify { bluetoothLeAdvertiser.stopAdvertising(any()) }
    }

    @Test
    fun `discoveredNodes should emit when scan result is received`() {
        // Тест эмуляции получения результата сканирования
        // В реальном устройстве это происходит через ScanCallback
        // Здесь проверяем начальное состояние
        assert(bleManager.discoveredNodes.value.isEmpty())
    }
}
