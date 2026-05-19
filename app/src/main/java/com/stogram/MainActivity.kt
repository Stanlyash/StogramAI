package com.stogram

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.stogram.ui.LoginScreen
import com.stogram.ui.theme.StogramTheme
import com.stogram.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        _permissionsGranted.value = allGranted
        
        if (allGranted) {
            Toast.makeText(this, "Все разрешения предоставлены", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Некоторые разрешения не предоставлены", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Запрос разрешений при запуске
        checkAndRequestPermissions()
        
        setContent {
            StogramTheme {
                LaunchedEffect(Unit) {
                    permissionsGranted.collect { granted ->
                        if (granted) {
                            // Разрешения получены, можно инициализировать mesh менеджеры
                            initializeMeshManagers()
                        }
                    }
                }
                
                LoginScreen(
                    onRequestPermissions = { checkAndRequestPermissions() },
                    permissionsGranted = permissionsGranted
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (PermissionHelper.hasAllPermissions(this)) {
            _permissionsGranted.value = true
        } else {
            val missingPermissions = PermissionHelper.getMissingPermissions(this)
            if (missingPermissions.isNotEmpty()) {
                requestPermissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }
    }

    private fun initializeMeshManagers() {
        lifecycleScope.launch {
            // Здесь будет инициализация NFC, BLE и Wi-Fi Direct менеджеров
            // NfcMeshManager, BleMeshManager, WifiDirectManager
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        _permissionsGranted.value = allGranted
        
        if (!allGranted) {
            Toast.makeText(this, "Разрешения необходимы для работы Mesh сети", Toast.LENGTH_LONG).show()
        }
    }
}