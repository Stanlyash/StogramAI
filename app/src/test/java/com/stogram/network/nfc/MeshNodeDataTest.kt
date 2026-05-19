package com.stogram.network.nfc

import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты для MeshNodeData - сериализация и десериализация
 */
class MeshNodeDataTest {
    
    @Test
    fun `сериализация и десериализация простого узла`() {
        val original = MeshNodeData(
            nodeId = "node-001",
            nodeName = "Test Node",
            timestamp = 1234567890L,
            crdtVector = emptyMap(),
            payload = byteArrayOf(0x01, 0x02, 0x03)
        )
        
        val bytes = original.toByteArray()
        val restored = MeshNodeData.fromByteArray(bytes)
        
        assertNotNull(restored)
        assertEquals(original.nodeId, restored!!.nodeId)
        assertEquals(original.nodeName, restored.nodeName)
        assertEquals(original.timestamp, restored.timestamp)
        assertArrayEquals(original.payload, restored.payload)
    }
    
    @Test
    fun `сериализация и десериализация с CRDT вектором`() {
        val crdtVector = mapOf(
            "node-001" to 1L,
            "node-002" to 5L,
            "node-003" to 3L
        )
        
        val original = MeshNodeData(
            nodeId = "node-001",
            nodeName = "Test Node",
            timestamp = 1234567890L,
            crdtVector = crdtVector,
            payload = byteArrayOf(0x0A, 0x0B)
        )
        
        val bytes = original.toByteArray()
        val restored = MeshNodeData.fromByteArray(bytes)
        
        assertNotNull(restored)
        assertEquals(original.crdtVector.size, restored!!.crdtVector.size)
        assertEquals(original.crdtVector["node-001"], restored.crdtVector["node-001"])
        assertEquals(original.crdtVector["node-002"], restored.crdtVector["node-002"])
        assertEquals(original.crdtVector["node-003"], restored.crdtVector["node-003"])
    }
    
    @Test
    fun `сериализация с пустым payload`() {
        val original = MeshNodeData(
            nodeId = "node-001",
            nodeName = "Empty Payload Node",
            payload = byteArrayOf()
        )
        
        val bytes = original.toByteArray()
        val restored = MeshNodeData.fromByteArray(bytes)
        
        assertNotNull(restored)
        assertTrue(restored!!.payload.isEmpty())
    }
    
    @Test
    fun `сериализация с большим payload`() {
        val largePayload = ByteArray(1000) { it.toByte() }
        
        val original = MeshNodeData(
            nodeId = "node-001",
            nodeName = "Large Payload Node",
            payload = largePayload
        )
        
        val bytes = original.toByteArray()
        val restored = MeshNodeData.fromByteArray(bytes)
        
        assertNotNull(restored)
        assertArrayEquals(original.payload, restored!!.payload)
        assertEquals(original.payload.size, restored.payload.size)
    }
    
    @Test
    fun `десериализация некорректных данных возвращает null`() {
        val invalidData = byteArrayOf(0x01, 0x02, 0x03) // Слишком мало данных
        
        val result = MeshNodeData.fromByteArray(invalidData)
        
        assertNull(result)
    }
    
    @Test
    fun `десериализация пустого массива возвращает null`() {
        val result = MeshNodeData.fromByteArray(byteArrayOf())
        
        assertNull(result)
    }
    
    @Test
    fun `CRDT вектор по умолчанию пустой`() {
        val node = MeshNodeData(
            nodeId = "node-001",
            nodeName = "Default CRDT Node"
        )
        
        assertTrue(node.crdtVector.isEmpty())
    }
    
    @Test
    fun `timestamp по умолчанию текущее время`() {
        val before = System.currentTimeMillis()
        val node = MeshNodeData(
            nodeId = "node-001",
            nodeName = "Timestamp Node"
        )
        val after = System.currentTimeMillis()
        
        assertTrue(node.timestamp in before..after)
    }
}
