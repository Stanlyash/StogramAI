package com.stogram.mesh.crdt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * LWW-Register (Last-Writer-Wins Register) CRDT.
 * Используется для синхронизации скалярных значений (имя узла, статус, метаданные).
 * Конфликты разрешаются по временной метке: побеждает значение с наибольшим timestamp.
 */
@Serializable
data class LwwRegisterCrdt<T>(
    private val value: T? = null,
    private val timestamp: Long = 0L
) {
    fun set(newValue: T, newTimestamp: Long = System.currentTimeMillis()): LwwRegisterCrdt<T> {
        return if (newTimestamp >= this.timestamp) {
            copy(value = newValue, timestamp = newTimestamp)
        } else {
            this
        }
    }

    fun merge(other: LwwRegisterCrdt<T>): LwwRegisterCrdt<T> {
        return if (other.timestamp > this.timestamp) {
            other
        } else if (other.timestamp == this.timestamp && other.value != null) {
            // При одинаковом timestamp используем детерминированное правило (например, лексикографическое сравнение или просто other)
            other
        } else {
            this
        }
    }

    fun get(): T? = value

    fun getTimestamp(): Long = timestamp

    fun toBytes(): ByteArray {
        val json = Json.encodeToString(serializer(), this)
        return json.toByteArray()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): LwwRegisterCrdt<String> {
            val json = String(bytes)
            return Json.decodeFromString(serializer(), json)
        }
        
        private fun serializer() = kotlinx.serialization.serializer<LwwRegisterCrdt<String>>()
    }
}
