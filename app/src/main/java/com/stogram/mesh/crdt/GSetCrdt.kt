package com.stogram.mesh.crdt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * G-Set (Grow-only Set) CRDT.
 * Идеально подходит для синхронизации списков узлов, сообщений или ключей.
 * Операция: Только добавление. Объединение = union множеств.
 * Конфликты: Невозможны (математически гарантировано).
 */
@Serializable
data class GSetCrdt<T>(
    private val elements: Set<T> = emptySet()
) {
    fun add(element: T): GSetCrdt<T> {
        return copy(elements = elements + element)
    }

    fun merge(other: GSetCrdt<T>): GSetCrdt<T> {
        return copy(elements = this.elements + other.elements)
    }

    fun contains(element: T): Boolean = elements.contains(element)

    fun getAll(): Set<T> = elements

    fun toBytes(): ByteArray {
        val json = Json.encodeToString(serializer(), this)
        return json.toByteArray()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): GSetCrdt<String> {
            val json = String(bytes)
            return Json.decodeFromString(serializer(), json)
        }
        
        // Специализированный сериализатор для String (для узлов)
        private fun serializer() = kotlinx.serialization.serializer<GSetCrdt<String>>()
    }
}
