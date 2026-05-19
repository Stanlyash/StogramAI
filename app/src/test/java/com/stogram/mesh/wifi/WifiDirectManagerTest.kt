package com.stogram.mesh.wifi

import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты для WifiDirectManager
 */
class WifiDirectManagerTest {

    @Test
    fun `WifiDirectPeer creation with default values`() {
        val peer = WifiDirectPeer(
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            deviceName = "Test Device",
            primaryDeviceType = "10-0050F204-5"
        )

        assertEquals("AA:BB:CC:DD:EE:FF", peer.deviceAddress)
        assertEquals("Test Device", peer.deviceName)
        assertEquals("10-0050F204-5", peer.primaryDeviceType)
        assertFalse(peer.isGroupOwner)
        assertEquals(WifiDirectPeer.PeerStatus.AVAILABLE, peer.status)
    }

    @Test
    fun `WifiDirectPeer creation with custom values`() {
        val peer = WifiDirectPeer(
            deviceAddress = "11:22:33:44:55:66",
            deviceName = "Group Owner",
            primaryDeviceType = "10-0050F204-5",
            isGroupOwner = true,
            status = WifiDirectPeer.PeerStatus.CONNECTED
        )

        assertTrue(peer.isGroupOwner)
        assertEquals(WifiDirectPeer.PeerStatus.CONNECTED, peer.status)
    }

    @Test
    fun `WifiDirectPeer all status values exist`() {
        val statuses = WifiDirectPeer.PeerStatus.values()
        
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(WifiDirectPeer.PeerStatus.AVAILABLE))
        assertTrue(statuses.contains(WifiDirectPeer.PeerStatus.CONNECTED))
        assertTrue(statuses.contains(WifiDirectPeer.PeerStatus.UNAVAILABLE))
        assertTrue(statuses.contains(WifiDirectPeer.PeerStatus.FAILED))
    }

    @Test
    fun `WifiDirectConnectionResult Success is singleton`() {
        val result1 = WifiDirectConnectionResult.Success
        val result2 = WifiDirectConnectionResult.Success
        
        assertSame(result1, result2)
    }

    @Test
    fun `WifiDirectConnectionResult Failure contains reason`() {
        val failure = WifiDirectConnectionResult.Failure("Test error")
        
        assertEquals("Test error", failure.reason)
    }

    @Test
    fun `WifiDirectConnectionResult Failure different reasons are different instances`() {
        val failure1 = WifiDirectConnectionResult.Failure("Error 1")
        val failure2 = WifiDirectConnectionResult.Failure("Error 2")
        
        assertNotEquals(failure1, failure2)
    }

    @Test
    fun `WifiDirectConnectionResult Cancelled is singleton`() {
        val result1 = WifiDirectConnectionResult.Cancelled
        val result2 = WifiDirectConnectionResult.Cancelled
        
        assertSame(result1, result2)
    }

    @Test
    fun `WifiDirectPeer equals and hashCode work correctly`() {
        val peer1 = WifiDirectPeer(
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            deviceName = "Device 1",
            primaryDeviceType = "Type 1"
        )
        
        val peer2 = WifiDirectPeer(
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            deviceName = "Device 1",
            primaryDeviceType = "Type 1"
        )
        
        val peer3 = WifiDirectPeer(
            deviceAddress = "AA:BB:CC:DD:EE:00",
            deviceName = "Device 1",
            primaryDeviceType = "Type 1"
        )
        
        assertEquals(peer1, peer2)
        assertEquals(peer1.hashCode(), peer2.hashCode())
        assertNotEquals(peer1, peer3)
    }

    @Test
    fun `WifiDirectPeer copy works correctly`() {
        val original = WifiDirectPeer(
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            deviceName = "Original",
            primaryDeviceType = "Type 1",
            isGroupOwner = false,
            status = WifiDirectPeer.PeerStatus.AVAILABLE
        )
        
        val modified = original.copy(
            deviceName = "Modified",
            isGroupOwner = true,
            status = WifiDirectPeer.PeerStatus.CONNECTED
        )
        
        assertEquals("AA:BB:CC:DD:EE:FF", modified.deviceAddress)
        assertEquals("Modified", modified.deviceName)
        assertEquals("Type 1", modified.primaryDeviceType)
        assertTrue(modified.isGroupOwner)
        assertEquals(WifiDirectPeer.PeerStatus.CONNECTED, modified.status)
        
        // Original should be unchanged
        assertEquals("Original", original.deviceName)
        assertFalse(original.isGroupOwner)
        assertEquals(WifiDirectPeer.PeerStatus.AVAILABLE, original.status)
    }

    @Test
    fun `WifiDirectPeer toString contains device name`() {
        val peer = WifiDirectPeer(
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            deviceName = "My Device",
            primaryDeviceType = "Type 1"
        )
        
        val toString = peer.toString()
        
        assertTrue(toString.contains("My Device"))
        assertTrue(toString.contains("AA:BB:CC:DD:EE:FF"))
    }
}
