package com.stogram.network.nfc

import android.nfc.NdefRecord

/**
 * Модель данных для представления узла Mesh сети
 * 
 * @property nodeId Уникальный идентификатор узла
 * @property nodeName Имя узла
 * @property timestamp Время создания/обновления записи
 * @property crdtVector Векторные часы для CRDT синхронизации
 * @property payload Полезная нагрузка данных
 */
data class MeshNodeData(
    val nodeId: String,
    val nodeName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val crdtVector: Map<String, Long> = emptyMap(),
    val payload: ByteArray = byteArrayOf()
) {
    
    /**
     * Сериализация данных в байтовый массив для передачи через NFC
     */
    fun toByteArray(): ByteArray {
        // Простая бинарная сериализация
        // Формат: [длина nodeId][nodeId][длина nodeName][nodeName][timestamp][длина crdt][crdt][длина payload][payload]
        
        val nodeIdBytes = nodeId.toByteArray(Charsets.UTF_8)
        val nodeNameBytes = nodeName.toByteArray(Charsets.UTF_8)
        val crdtBytes = serializeCrdtVector(crdtVector)
        
        return buildByteArray {
            writeByte(nodeIdBytes.size.toByte())
            write(nodeIdBytes)
            writeByte(nodeNameBytes.size.toByte())
            write(nodeNameBytes)
            writeLong(timestamp)
            writeInt(crdtBytes.size)
            write(crdtBytes)
            writeInt(payload.size)
            write(payload)
        }
    }
    
    /**
     * Десериализация из байтового массива
     */
    companion object {
        fun fromByteArray(data: ByteArray): MeshNodeData? {
            return try {
                var offset = 0
                
                // Читаем nodeId
                val nodeIdLen = data[offset++].toInt() and 0xFF
                val nodeId = String(data.sliceArray(offset until offset + nodeIdLen), Charsets.UTF_8)
                offset += nodeIdLen
                
                // Читаем nodeName
                val nodeNameLen = data[offset++].toInt() and 0xFF
                val nodeName = String(data.sliceArray(offset until offset + nodeNameLen), Charsets.UTF_8)
                offset += nodeNameLen
                
                // Читаем timestamp
                val timestamp = ((data[offset + 7].toLong() and 0xFF) shl 56) or
                               ((data[offset + 6].toLong() and 0xFF) shl 48) or
                               ((data[offset + 5].toLong() and 0xFF) shl 40) or
                               ((data[offset + 4].toLong() and 0xFF) shl 32) or
                               ((data[offset + 3].toLong() and 0xFF) shl 24) or
                               ((data[offset + 2].toLong() and 0xFF) shl 16) or
                               ((data[offset + 1].toLong() and 0xFF) shl 8) or
                               (data[offset].toLong() and 0xFF)
                offset += 8
                
                // Читаем CRDT вектор
                val crdtLen = ((data[offset + 3].toInt() and 0xFF) shl 24) or
                             ((data[offset + 2].toInt() and 0xFF) shl 16) or
                             ((data[offset + 1].toInt() and 0xFF) shl 8) or
                             (data[offset].toInt() and 0xFF)
                offset += 4
                val crdtBytes = data.sliceArray(offset until offset + crdtLen)
                val crdtVector = deserializeCrdtVector(crdtBytes)
                offset += crdtLen
                
                // Читаем payload
                val payloadLen = ((data[offset + 3].toInt() and 0xFF) shl 24) or
                                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                                (data[offset].toInt() and 0xFF)
                offset += 4
                val payload = if (payloadLen > 0) {
                    data.sliceArray(offset until offset + payloadLen)
                } else {
                    byteArrayOf()
                }
                
                MeshNodeData(nodeId, nodeName, timestamp, crdtVector, payload)
            } catch (e: Exception) {
                null
            }
        }
        
        private fun serializeCrdtVector(vector: Map<String, Long>): ByteArray {
            // Простая сериализация: [количество записей][длина ключа][ключ][значение]...
            val result = mutableListOf<Byte>()
            
            // Записываем количество элементов (4 байта)
            val size = vector.size
            result.add((size and 0xFF).toByte())
            result.add(((size shr 8) and 0xFF).toByte())
            result.add(((size shr 16) and 0xFF).toByte())
            result.add(((size shr 24) and 0xFF).toByte())
            
            // Записываем каждую запись
            for ((key, value) in vector) {
                val keyBytes = key.toByteArray(Charsets.UTF_8)
                result.add(keyBytes.size.toByte())
                result.addAll(keyBytes.map { it.toByte() })
                
                // Записываем значение (8 байт)
                for (i in 0..7) {
                    result.add(((value shr (i * 8)) and 0xFF).toByte())
                }
            }
            
            return result.toByteArray()
        }
        
        private fun deserializeCrdtVector(data: ByteArray): Map<String, Long> {
            if (data.isEmpty()) return emptyMap()
            
            val result = mutableMapOf<String, Long>()
            var offset = 0
            
            // Читаем количество элементов
            val size = ((data[3].toInt() and 0xFF) shl 24) or
                      ((data[2].toInt() and 0xFF) shl 16) or
                      ((data[1].toInt() and 0xFF) shl 8) or
                      (data[0].toInt() and 0xFF)
            offset += 4
            
            // Читаем каждую запись
            repeat(size) {
                val keyLen = data[offset++].toInt() and 0xFF
                val key = String(data.sliceArray(offset until offset + keyLen), Charsets.UTF_8)
                offset += keyLen
                
                // Читаем значение
                var value = 0L
                for (i in 0..7) {
                    value = value or ((data[offset++].toLong() and 0xFF) shl (i * 8))
                }
                
                result[key] = value
            }
            
            return result
        }
        
        private inline fun buildByteArray(builder: BuildByteArrayList.() -> Unit): ByteArray {
            return BuildByteArrayList().apply(builder).toByteArray()
        }
        
        private class BuildByteArrayList {
            private val bytes = mutableListOf<Byte>()
            
            fun writeByte(byte: Byte) {
                bytes.add(byte)
            }
            
            fun write(array: ByteArray) {
                bytes.addAll(array.map { it.toByte() })
            }
            
            fun writeInt(value: Int) {
                for (i in 0..3) {
                    bytes.add(((value shr (i * 8)) and 0xFF).toByte())
                }
            }
            
            fun writeLong(value: Long) {
                for (i in 0..7) {
                    bytes.add(((value shr (i * 8)) and 0xFF).toByte())
                }
            }
            
            fun toByteArray(): ByteArray {
                return bytes.toByteArray()
            }
        }
    }
}
