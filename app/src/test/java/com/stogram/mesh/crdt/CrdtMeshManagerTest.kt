package com.stogram.mesh.crdt

import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты для CrdtMeshManager
 */
class CrdtMeshManagerTest {

    @Test
    fun `test update local name`() {
        val manager = CrdtMeshManager("node1")
        
        manager.updateLocalName("My Node")
        
        assertEquals("My Node", manager.localState.value.nodeName.get())
    }

    @Test
    fun `test add neighbor`() {
        val manager = CrdtMeshManager("node1")
        
        manager.addNeighbor("node2")
        
        assertTrue(manager.localState.value.neighbors.contains("node2"))
    }

    @Test
    fun `test merge remote state`() {
        val manager1 = CrdtMeshManager("node1")
        val manager2 = CrdtMeshManager("node2")
        
        manager1.updateLocalName("Node 1")
        manager2.updateLocalName("Node 2")
        
        // Синхронизация между узлами
        val payload1 = manager1.getSyncPayload()
        manager2.applySyncPayload(payload1)
        
        val payload2 = manager2.getSyncPayload()
        manager1.applySyncPayload(payload2)
        
        // Оба узла должны знать о друг друге
        assertTrue(manager1.globalState.value.containsKey("node1"))
        assertTrue(manager1.globalState.value.containsKey("node2"))
        assertTrue(manager2.globalState.value.containsKey("node1"))
        assertTrue(manager2.globalState.value.containsKey("node2"))
    }

    @Test
    fun `test CRDT convergence`() {
        val manager1 = CrdtMeshManager("node1")
        val manager2 = CrdtMeshManager("node2")
        val manager3 = CrdtMeshManager("node3")
        
        // Узел 1 обновляет имя
        manager1.updateLocalName("Node 1 - Updated")
        
        // Узел 2 обновляет имя позже (больший timestamp)
        Thread.sleep(10)
        manager2.updateLocalName("Node 2 - Updated")
        
        // Синхронизация 1 <-> 2
        manager2.applySyncPayload(manager1.getSyncPayload())
        manager1.applySyncPayload(manager2.getSyncPayload())
        
        // Синхронизация с узлом 3
        manager3.applySyncPayload(manager1.getSyncPayload())
        
        // Все узлы должны иметь одинаковое глобальное состояние (конвергенция)
        val state1Names = manager1.globalState.value.mapValues { it.value.nodeName.get() }
        val state2Names = manager2.globalState.value.mapValues { it.value.nodeName.get() }
        val state3Names = manager3.globalState.value.mapValues { it.value.nodeName.get() }
        
        assertEquals(state1Names, state2Names)
        assertEquals(state2Names, state3Names)
    }

    @Test
    fun `test receive remote state adds neighbor automatically`() {
        val manager = CrdtMeshManager("node1")
        val remoteState = MeshNodeState("node2").updateName("Remote Node")
        
        manager.receiveRemoteState(remoteState)
        
        assertTrue(manager.localState.value.neighbors.contains("node2"))
    }

    @Test
    fun `test set active status`() {
        val manager = CrdtMeshManager("node1")
        
        manager.setLocalActive(true)
        assertTrue(manager.localState.value.isActive.get() == true)
        
        manager.setLocalActive(false)
        assertTrue(manager.localState.value.isActive.get() == false)
    }

    @Test
    fun `test cleanup inactive nodes`() {
        val manager = CrdtMeshManager("node1")
        
        // Создаем удаленный узел и делаем его неактивным
        val remoteState = MeshNodeState("node2")
            .updateName("Old Node")
            .setActive(false)
        
        manager.receiveRemoteState(remoteState)
        assertTrue(manager.globalState.value.containsKey("node2"))
        
        // Очищаем неактивные узлы (порог 0 мс для теста)
        manager.cleanupInactiveNodes(thresholdMs = 0L)
        
        // Узел должен быть удален
        assertFalse(manager.globalState.value.containsKey("node2"))
    }

    @Test
    fun `test serialization and deserialization of MeshNodeState`() {
        val original = MeshNodeState("node1")
            .updateName("Test Node")
            .addNeighbor("node2")
            .setActive(true)
        
        val bytes = original.toBytes()
        val restored = MeshNodeState.fromBytes(bytes)
        
        assertEquals(original.nodeId, restored.nodeId)
        assertEquals(original.nodeName.get(), restored.nodeName.get())
        assertEquals(original.neighbors.getAll(), restored.neighbors.getAll())
        assertEquals(original.isActive.get(), restored.isActive.get())
    }
}
