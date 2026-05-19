package com.stogram.network.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Parcelable
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Менеджер NFC для Mesh сети Stogram.
 * 
 * Отвечает за:
 * - Проверку доступности NFC на устройстве
 * - Чтение и запись NDEF сообщений
 * - Передачу данных между устройствами через NFC теги
 * - Интеграцию с CRDT для синхронизации без сервера
 */
class NfcMeshManager(private val context: Context) {
    
    companion object {
        private const val TAG = "NfcMeshManager"
        const val MIME_TYPE = "application/com.stogram.mesh"
    }
    
    private val nfcManager: NfcManager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
    private val nfcAdapter: NfcAdapter? = nfcManager.defaultAdapter
    
    // Состояние NFC
    private val _nfcEnabled = MutableStateFlow(false)
    val nfcEnabled: StateFlow<Boolean> = _nfcEnabled.asStateFlow()
    
    // Последний прочитанный тег
    private val _lastTag = MutableStateFlow<Tag?>(null)
    val lastTag: StateFlow<Tag?> = _lastTag.asStateFlow()
    
    // Полученные данные из Mesh сети
    private val _receivedData = MutableStateFlow<ByteArray?>(null)
    val receivedData: StateFlow<ByteArray?> = _receivedData.asStateFlow()
    
    /**
     * Проверка доступности NFC на устройстве
     */
    fun isNfcAvailable(): Boolean {
        return nfcAdapter != null
    }
    
    /**
     * Проверка включен ли NFC
     */
    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
    
    /**
     * Обновление состояния NFC
     */
    fun updateNfcState() {
        _nfcEnabled.value = isNfcEnabled()
        Log.d(TAG, "NFC enabled: ${_nfcEnabled.value}")
    }
    
    /**
     * Обработка нового тега
     */
    fun handleTag(tag: Tag) {
        Log.d(TAG, "New tag discovered: ${tag.id.joinToString(":") { "%02X".format(it) }}")
        _lastTag.value = tag
        
        // Запускаем чтение данных в фоновом потоке
        readTagAsync(tag)
    }
    
    /**
     * Асинхронное чтение тега
     */
    private fun readTagAsync(tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            
            if (ndef != null) {
                // Тег уже отформатирован как NDEF
                readNdefTag(ndef, tag)
            } else {
                // Пытаемся отформатировать тег как NDEF
                formatAndWriteTag(tag)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tag", e)
        }
    }
    
    /**
     * Чтение NDEF тега
     */
    private fun readNdefTag(ndef: Ndef, tag: Tag) {
        try {
            ndef.connect()
            
            if (!ndef.isWritable) {
                Log.w(TAG, "Tag is not writable")
                ndef.close()
                return
            }
            
            val ndefMessage = ndef.ndefMessage
            
            if (ndefMessage != null) {
                for (record in ndefMessage.records) {
                    val payload = record.payload
                    Log.d(TAG, "Read NDEF record: ${payload.size} bytes")
                    
                    // Проверяем тип записи
                    if (record.type == MIME_TYPE.toByteArray(charset("US-ASCII"))) {
                        _receivedData.value = payload
                        Log.d(TAG, "Received mesh data: ${payload.size} bytes")
                    }
                }
            }
            
            ndef.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading NDEF tag", e)
        }
    }
    
    /**
     * Форматирование и запись тега
     */
    private fun formatAndWriteTag(tag: Tag) {
        try {
            val ndefFormatable = NdefFormatable.get(tag)
            
            if (ndefFormatable != null) {
                ndefFormatable.connect()
                
                // Создаем тестовое сообщение для инициализации
                val message = createNdefMessage("Stogram Mesh Node Initialized".toByteArray())
                ndefFormatable.format(message)
                
                Log.d(TAG, "Tag formatted and initialized")
                ndefFormatable.close()
            } else {
                Log.w(TAG, "Tag cannot be formatted as NDEF")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting tag", e)
        }
    }
    
    /**
     * Запись данных в тег
     */
    fun writeToTag(tag: Tag, data: ByteArray): Boolean {
        return try {
            val ndef = Ndef.get(tag)
            
            if (ndef == null) {
                Log.w(TAG, "Tag does not support NDEF")
                return false
            }
            
            ndef.connect()
            
            if (!ndef.isWritable) {
                Log.w(TAG, "Tag is not writable")
                ndef.close()
                return false
            }
            
            val message = createNdefMessage(data)
            ndef.writeNdefMessage(message)
            
            Log.d(TAG, "Successfully wrote ${data.size} bytes to tag")
            ndef.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to tag", e)
            false
        }
    }
    
    /**
     * Создание NDEF сообщения с данными Mesh сети
     */
    private fun createNdefMessage(data: ByteArray): android.nfc.NdefMessage {
        val record = android.nfc.NdefRecord(
            android.nfc.NdefRecord.TNF_MIME_MEDIA,
            MIME_TYPE.toByteArray(charset("US-ASCII")),
            byteArrayOf(), // ID
            data
        )
        
        return android.nfc.NdefMessage(arrayOf(record))
    }
    
    /**
     * Создание Intent для настройки foreground dispatch
     */
    fun createForegroundDispatchIntent(activity: Activity): PendingIntent {
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
    }
    
    /**
     * Настройка foreground dispatch для получения уведомлений о тегах
     */
    fun enableForegroundDispatch(activity: Activity) {
        if (!isNfcAvailable()) {
            Log.w(TAG, "NFC not available")
            return
        }
        
        try {
            val pendingIntent = createForegroundDispatchIntent(activity)
            val filters = arrayOf(
                android.content.IntentFilter(android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED)
            )
            
            val techLists = arrayOf(
                arrayOf<String>(android.nfc.tech.Ndef::class.java.name),
                arrayOf<String>(android.nfc.tech.NdefFormatable::class.java.name)
            )
            
            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
            Log.d(TAG, "Foreground dispatch enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling foreground dispatch", e)
        }
    }
    
    /**
     * Отключение foreground dispatch
     */
    fun disableForegroundDispatch(activity: Activity) {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
            Log.d(TAG, "Foreground dispatch disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling foreground dispatch", e)
        }
    }
    
    /**
     * Преобразование Intent в Tag
     */
    fun getTagFromIntent(intent: Intent): Tag? {
        return if (intent.action == android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED) {
            intent.getParcelableExtra<Tag>(android.nfc.NfcAdapter.EXTRA_TAG)
        } else {
            null
        }
    }
}
