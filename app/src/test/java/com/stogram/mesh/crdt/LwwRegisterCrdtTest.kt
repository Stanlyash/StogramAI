package com.stogram.mesh.crdt

import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты для LwwRegisterCrdt
 */
class LwwRegisterCrdtTest {

    @Test
    fun `test set value with timestamp`() {
        val register = LwwRegisterCrdt<String>()
        val newRegister = register.set("value1", 1000L)
        
        assertEquals("value1", newRegister.get())
        assertEquals(1000L, newRegister.getTimestamp())
    }

    @Test
    fun `test merge with newer timestamp wins`() {
        val register1 = LwwRegisterCrdt<String>().set("old", 1000L)
        val register2 = LwwRegisterCrdt<String>().set("new", 2000L)
        
        val merged = register1.merge(register2)
        
        assertEquals("new", merged.get())
        assertEquals(2000L, merged.getTimestamp())
    }

    @Test
    fun `test merge with older timestamp loses`() {
        val register1 = LwwRegisterCrdt<String>().set("new", 2000L)
        val register2 = LwwRegisterCrdt<String>().set("old", 1000L)
        
        val merged = register1.merge(register2)
        
        assertEquals("new", merged.get())
        assertEquals(2000L, merged.getTimestamp())
    }

    @Test
    fun `test merge with same timestamp - other wins`() {
        val register1 = LwwRegisterCrdt<String>().set("value1", 1000L)
        val register2 = LwwRegisterCrdt<String>().set("value2", 1000L)
        
        val merged = register1.merge(register2)
        
        assertEquals("value2", merged.get())
    }

    @Test
    fun `test idempotency of merge`() {
        val register1 = LwwRegisterCrdt<String>().set("value1", 1000L)
        val register2 = LwwRegisterCrdt<String>().set("value2", 2000L)
        
        val merged1 = register1.merge(register2)
        val merged2 = register1.merge(register2)
        
        assertEquals(merged1.get(), merged2.get())
        assertEquals(merged1.getTimestamp(), merged2.getTimestamp())
    }

    @Test
    fun `test serialization and deserialization`() {
        val original = LwwRegisterCrdt<String>().set("test", 12345L)
        val bytes = original.toBytes()
        val restored = LwwRegisterCrdt.fromBytes(bytes)
        
        assertEquals(original.get(), restored.get())
        assertEquals(original.getTimestamp(), restored.getTimestamp())
    }
}
