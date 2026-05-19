package com.stogram.mesh.wifi

import android.content.Context
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetAddress

/**
 * Модель данных для узла Wi-Fi Direct сети
 */
data class WifiDirectPeer(
    val deviceAddress: String,
    val deviceName: String,
    val primaryDeviceType: String,
    val isGroupOwner: Boolean = false,
    val status: PeerStatus = PeerStatus.AVAILABLE
) {
    enum class PeerStatus {
        AVAILABLE,
        CONNECTED,
        UNAVAILABLE,
        FAILED
    }
}

/**
 * Результат подключения Wi-Fi Direct
 */
sealed class WifiDirectConnectionResult {
    object Success : WifiDirectConnectionResult()
    data class Failure(val reason: String) : WifiDirectConnectionResult()
    object Cancelled : WifiDirectConnectionResult()
}

/**
 * Менеджер Wi-Fi Direct для Mesh сети Stogram
 * 
 * Отвечает за:
 * - Обнаружение устройств через Wi-Fi Direct
 * - Создание и присоединение к группам
 * - Управление P2P соединениями
 * - Интеграцию с CRDT для синхронизации данных
 */
class WifiDirectManager(private val context: Context) {

    private val wifiP2pManager: WifiP2pManager by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }
    
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: WifiDirectBroadcastReceiver? = null
    
    private val _availablePeers = mutableListOf<WifiDirectPeer>()
    private val _connectedPeers = mutableListOf<WifiDirectPeer>()
    private val _groupInfo = mutableMapOf<String, Any>()
    
    /**
     * Поток доступных пиров (обнаруженных устройств)
     */
    val availablePeers: Flow<List<WifiDirectPeer>> = callbackFlow {
        val listener = object : WifiP2pManager.PeerListListener {
            override fun onPeersAvailable(peers: WifiP2pDeviceList) {
                val peerList = peers.deviceList.map { device ->
                    WifiDirectPeer(
                        deviceAddress = device.deviceAddress ?: "",
                        deviceName = device.deviceName ?: "Unknown",
                        primaryDeviceType = device.primaryDeviceType?.toString() ?: "",
                        isGroupOwner = false,
                        status = when (device.status) {
                            WifiP2pDevice.CONNECTED -> WifiDirectPeer.PeerStatus.CONNECTED
                            WifiP2pDevice.FAILED -> WifiDirectPeer.PeerStatus.FAILED
                            WifiP2pDevice.UNAVAILABLE -> WifiDirectPeer.PeerStatus.UNAVAILABLE
                            else -> WifiDirectPeer.PeerStatus.AVAILABLE
                        }
                    )
                }
                _availablePeers.clear()
                _availablePeers.addAll(peerList)
                trySend(peerList)
            }
        }
        
        // Инициализация слушателя
        trySend(emptyList())
        
        awaitClose {
            // Очистка ресурсов
        }
    }
    
    /**
     * Поток подключенных пиров
     */
    val connectedPeers: Flow<List<WifiDirectPeer>> = callbackFlow {
        trySend(_connectedPeers.toList())
        
        awaitClose {
            // Очистка ресурсов
        }
    }
    
    /**
     * Инициализация Wi-Fi Direct менеджера
     */
    fun initialize() {
        channel = wifiP2pManager.initialize(context, context.mainLooper, null)
        receiver = WifiDirectBroadcastReceiver(wifiP2pManager, channel!!, this)
    }
    
    /**
     * Запуск обнаружения устройств
     */
    fun startDiscovery() {
        channel?.let { ch ->
            wifiP2pManager.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Обнаружение запущено успешно
                }
                
                override fun onFailure(reason: Int) {
                    // Ошибка обнаружения
                }
            })
        }
    }
    
    /**
     * Остановка обнаружения устройств
     */
    fun stopDiscovery() {
        channel?.let { ch ->
            wifiP2pManager.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Остановка успешна
                }
                
                override fun onFailure(reason: Int) {
                    // Ошибка остановки
                }
            })
        }
    }
    
    /**
     * Подключение к устройству
     */
    suspend fun connectToDevice(deviceAddress: String): WifiDirectConnectionResult {
        return suspendCancellableCoroutine { continuation ->
            val config = WifiP2pConfig().apply {
                deviceAddress = deviceAddress
                wps.setup = WpsInfo.PBC
            }
            
            channel?.let { ch ->
                wifiP2pManager.connect(ch, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        continuation.resume(WifiDirectConnectionResult.Success) {}
                    }
                    
                    override fun onFailure(reason: Int) {
                        val errorReason = when (reason) {
                            WifiP2pManager.ERROR -> "Общая ошибка"
                            WifiP2pManager.NO_SERVICE_REQUESTS -> "Нет запросов сервиса"
                            else -> "Неизвестная ошибка: $reason"
                        }
                        continuation.resume(WifiDirectConnectionResult.Failure(errorReason)) {}
                    }
                })
            } ?: continuation.resume(WifiDirectConnectionResult.Failure("Канал не инициализирован")) {}
        }
    }
    
    /**
     * Отключение от устройства
     */
    suspend fun disconnect(): WifiDirectConnectionResult {
        return suspendCancellableCoroutine { continuation ->
            channel?.let { ch ->
                wifiP2pManager.removeGroup(ch, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        _connectedPeers.clear()
                        continuation.resume(WifiDirectConnectionResult.Success) {}
                    }
                    
                    override fun onFailure(reason: Int) {
                        continuation.resume(WifiDirectConnectionResult.Failure("Ошибка отключения: $reason")) {}
                    }
                })
            } ?: continuation.resume(WifiDirectConnectionResult.Failure("Канал не инициализирован")) {}
        }
    }
    
    /**
     * Создание группы (стать Group Owner)
     */
    suspend fun createGroup(): WifiDirectConnectionResult {
        return suspendCancellableCoroutine { continuation ->
            channel?.let { ch ->
                wifiP2pManager.createGroup(ch, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        continuation.resume(WifiDirectConnectionResult.Success) {}
                    }
                    
                    override fun onFailure(reason: Int) {
                        continuation.resume(WifiDirectConnectionResult.Failure("Ошибка создания группы: $reason")) {}
                    }
                })
            } ?: continuation.resume(WifiDirectConnectionResult.Failure("Канал не инициализирован")) {}
        }
    }
    
    /**
     * Получение информации о группе
     */
    suspend fun getGroupInfo(): WifiP2pGroup? {
        return suspendCancellableCoroutine { continuation ->
            channel?.let { ch ->
                wifiP2pManager.requestGroupInfo(ch) { group ->
                    continuation.resume(group, {})
                }
            } ?: continuation.resume(null, {})
        }
    }
    
    /**
     * Получение информации о подключении
     */
    suspend fun getConnectionInfo(): WifiP2pInfo? {
        return suspendCancellableCoroutine { continuation ->
            channel?.let { ch ->
                wifiP2pManager.requestConnectionInfo(ch) { info ->
                    continuation.resume(info, {})
                }
            } ?: continuation.resume(null, {})
        }
    }
    
    /**
     * Освобождение ресурсов
     */
    fun release() {
        stopDiscovery()
        receiver = null
        channel = null
    }
    
    /**
     * Внутренний метод для обновления списка подключенных пиров
     */
    internal fun updateConnectedPeers(peers: List<WifiDirectPeer>) {
        _connectedPeers.clear()
        _connectedPeers.addAll(peers)
    }
}

/**
 * BroadcastReceiver для обработки событий Wi-Fi Direct
 */
class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val wifiDirectManager: WifiDirectManager
) : android.content.BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: android.content.Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                when (state) {
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                        // Wi-Fi Direct включен
                    }
                    WifiP2pManager.WIFI_P2P_STATE_DISABLED -> {
                        // Wi-Fi Direct выключен
                    }
                }
            }
            
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Список пиров изменился
                manager.requestPeerList(channel) { peerList ->
                    // Обновление списка пиров
                }
            }
            
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Состояние подключения изменилось
                val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(
                    WifiP2pManager.EXTRA_NETWORK_INFO
                )
                
                if (networkInfo?.isConnected == true) {
                    manager.requestConnectionInfo(channel) { connectionInfo ->
                    }
                }
            }
            
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Информация об этом устройстве изменилась
            }
        }
    }
}
