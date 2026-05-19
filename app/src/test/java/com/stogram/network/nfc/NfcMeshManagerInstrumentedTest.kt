package com.stogram.network.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.Ndef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*

/**
 * Инструментальные тесты для NfcMeshManager с моками Android компонентов
 */
class NfcMeshManagerInstrumentedTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockNfcManager: NfcManager
    private lateinit var mockNfcAdapter: NfcAdapter
    private lateinit var nfcMeshManager: NfcMeshManager
    
    @Before
    fun setup() {
        // Создаем моки
        mockContext = mock(Context::class.java)
        mockNfcManager = mock(NfcManager::class.java)
        mockNfcAdapter = mock(NfcAdapter::class.java)
        
        // Настраиваем поведение моков
        `when`(mockContext.getSystemService(Context.NFC_SERVICE))
            .thenReturn(mockNfcManager)
        `when`(mockNfcManager.defaultAdapter)
            .thenReturn(mockNfcAdapter)
        
        nfcMeshManager = NfcMeshManager(mockContext)
    }
    
    @Test
    fun `isNfcAvailable возвращает true когда адаптер существует`() {
        `when`(mockNfcAdapter).thenReturn(mockNfcAdapter)
        
        val result = nfcMeshManager.isNfcAvailable()
        
        assertTrue(result)
    }
    
    @Test
    fun `isNfcEnabled возвращает true когда NFC включен`() {
        `when`(mockNfcAdapter.isEnabled).thenReturn(true)
        
        val result = nfcMeshManager.isNfcEnabled()
        
        assertTrue(result)
    }
    
    @Test
    fun `isNfcEnabled возвращает false когда NFC выключен`() {
        `when`(mockNfcAdapter.isEnabled).thenReturn(false)
        
        val result = nfcMeshManager.isNfcEnabled()
        
        assertFalse(result)
    }
    
    @Test
    fun `updateNfcState обновляет состояние потока`() = runTest {
        `when`(mockNfcAdapter.isEnabled).thenReturn(true)
        
        nfcMeshManager.updateNfcState()
        
        assertTrue(nfcMeshManager.nfcEnabled.first())
    }
    
    @Test
    fun `handleTag обрабатывает новый тег`() = runTest {
        val mockTag = mock(Tag::class.java)
        val tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        `when`(mockTag.id).thenReturn(tagId)
        
        nfcMeshManager.handleTag(mockTag)
        
        assertEquals(mockTag, nfcMeshManager.lastTag.first())
    }
    
    @Test
    fun `getTagFromIntent извлекает Tag из Intent`() {
        val mockTag = mock(Tag::class.java)
        val intent = Intent(NfcAdapter.ACTION_TAG_DISCOVERED).apply {
            putExtra(NfcAdapter.EXTRA_TAG, mockTag as android.os.Parcelable)
        }
        
        val result = nfcMeshManager.getTagFromIntent(intent)
        
        assertEquals(mockTag, result)
    }
    
    @Test
    fun `getTagFromIntent возвращает null для неправильного Intent`() {
        val intent = Intent(Intent.ACTION_MAIN)
        
        val result = nfcMeshManager.getTagFromIntent(intent)
        
        assertNull(result)
    }
}
