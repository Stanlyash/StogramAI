package com.stogram.mesh.crdt

import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты для GSetCrdt
 */
class GSetCrdtTest {

    @Test
    fun `test add element`() {
        val set = GSetCrdt<String>()
        val newSet = set.add("node1")
        
        assertTrue(newSet.contains("node1"))
        assertEquals(1, newSet.getAll().size)
    }

    @Test
    fun `test merge two sets`() {
        val set1 = GSetCrdt<String>().add("node1").add("node2")
        val set2 = GSetCrdt<String>().add("node2").add("node3")
        
        val merged = set1.merge(set2)
        
        assertTrue(merged.contains("node1"))
        assertTrue(merged.contains("node2"))
        assertTrue(merged.contains("node3"))
        assertEquals(3, merged.getAll().size)
    }

    @Test
    fun `test idempotency of merge`() {
        val set1 = GSetCrdt<String>().add("node1")
        val set2 = GSetCrdt<String>().add("node2")
        
        val merged1 = set1.merge(set2)
        val merged2 = set1.merge(set2)
        
        assertEquals(merged1.getAll(), merged2.getAll())
    }

    @Test
    fun `test commutativity of merge`() {
        val set1 = GSetCrdt<String>().add("node1")
        val set2 = GSetCrdt<String>().add("node2")
        
        val merged1 = set1.merge(set2)
        val merged2 = set2.merge(set1)
        
        assertEquals(merged1.getAll(), merged2.getAll())
    }

    @Test
    fun `test serialization and deserialization`() {
        val original = GSetCrdt<String>().add("node1").add("node2")
        val bytes = original.toBytes()
        val restored = GSetCrdt.fromBytes(bytes)
        
        assertEquals(original.getAll(), restored.getAll())
    }
}
