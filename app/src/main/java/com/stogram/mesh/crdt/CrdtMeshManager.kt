package com.stogram.mesh.crdt

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * MeshNodeState - агрегированное состояние узла на основе CRDT.
 * Содержит:
 * - ID узла (G-Set для уникальности)
 * - Имя узла (LWW-Register)
 * - Список соседей (G-Set)
 * - Статус активности (LWW-Register)
 */
@Serializable
data class MeshNodeState(
    val nodeId: String,
    val nodeName: LwwRegisterCrdt<String> = LwwRegisterCrdt(),
    val neighbors: GSetCrdt<String> = GSetCrdt(),
    val isActive: LwwRegisterCrdt<Boolean> = LwwRegisterCrdt()
) {
    fun updateName(name: String): MeshNodeState {
        return copy(nodeName = nodeName.set(name))
    }

    fun addNeighbor(neighborId: String): MeshNodeState {
        return copy(neighbors = neighbors.add(neighborId))
    }

    fun setActive(active: Boolean): MeshNodeState {
        return copy(isActive = isActive.set(active))
    }

    fun merge(other: MeshNodeState): MeshNodeState {
        // Merge всех CRDT полей
        return MeshNodeState(
            nodeId = this.nodeId, // ID не меняется
            nodeName = this.nodeName.merge(other.nodeName),
            neighbors = this.neighbors.merge(other.neighbors),
            isActive = this.isActive.merge(other.isActive)
        )
    }

    fun toBytes(): ByteArray {
        val json = Json.encodeToString(serializer(), this)
        return json.toByteArray()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): MeshNodeState {
            val json = String(bytes)
            return Json.decodeFromString(serializer(), json)
        }
        
        private fun serializer() = kotlinx.serialization.serializer<MeshNodeState>()
    }
}

/**
 * CrdtMeshManager - центральный менеджер синхронизации CRDT across всех транспортов (NFC, BLE, WiFi Direct).
 * Оптимизирован для минимального энергопотребления и максимальной скорости.
 */
class CrdtMeshManager(private val localNodeId: String) {
    
    private val _localState = MutableStateFlow(MeshNodeState(localNodeId))
    val localState: StateFlow<MeshNodeState> = _localState.asStateFlow()
    
    private val _globalState = MutableStateFlow<Map<String, MeshNodeState>>(emptyMap())
    val globalState: StateFlow<Map<String, MeshNodeState>> = _globalState.asStateFlow()

    /**
     * Обновление локального состояния
     */
    fun updateLocalName(name: String) {
        _localState.value = _localState.value.updateName(name)
        syncToGlobal(localNodeId, _localState.value)
    }

    fun addNeighbor(neighborId: String) {
        _localState.value = _localState.value.addNeighbor(neighborId)
        syncToGlobal(localNodeId, _localState.value)
    }

    fun setLocalActive(active: Boolean) {
        _localState.value = _localState.value.setActive(active)
        syncToGlobal(localNodeId, _localState.value)
    }

    /**
     * Синхронизация с глобальным состоянием (CRDT merge)
     */
    fun syncToGlobal(nodeId: String, state: MeshNodeState) {
        val currentMap = _globalState.value.toMutableMap()
        val existingState = currentMap[nodeId]
        
        val mergedState = if (existingState != null) {
            existingState.merge(state)
        } else {
            state
        }
        
        currentMap[nodeId] = mergedState
        _globalState.value = currentMap
    }

    /**
     * Получение данных от другого узла (через NFC/BLE/WiFi) и merge
     */
    fun receiveRemoteState(remoteState: MeshNodeState) {
        syncToGlobal(remoteState.nodeId, remoteState)
        
        // Если это новый сосед, добавляем его в локальный список
        if (remoteState.nodeId != localNodeId && !_localState.value.neighbors.contains(remoteState.nodeId)) {
            addNeighbor(remoteState.nodeId)
        }
    }

    /**
     * Сериализация всего глобального состояния для передачи
     * Оптимизация: передаем только дельту (изменения) если возможно
     */
    fun getSyncPayload(): ByteArray {
        val statesList = _globalState.values.toList()
        val json = Json.encodeToString(kotlinx.serialization.serializer<List<MeshNodeState>>(), statesList)
        return json.toByteArray()
    }

    /**
     * Десериализация и merge полученного состояния
     */
    fun applySyncPayload(payload: ByteArray) {
        try {
            val json = String(payload)
            val statesList: List<MeshNodeState> = Json.decodeFromString(
                kotlinx.serialization.serializer<List<MeshNodeState>>(), 
                json
            )
            
            statesList.forEach { remoteState ->
                receiveRemoteState(remoteState)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Очистка неактивных узлов (опционально, для экономии памяти)
     */
    fun cleanupInactiveNodes(thresholdMs: Long = 300_000) { // 5 минут по умолчанию
        val now = System.currentTimeMillis()
        val currentMap = _globalState.value.toMutableMap()
        
        currentMap.entries.removeAll { (_, state) ->
            state.isActive.get() == false && (now - state.isActive.getTimestamp()) > thresholdMs
        }
        
        _globalState.value = currentMap
    }
}
