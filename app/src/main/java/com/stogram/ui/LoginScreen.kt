package com.stogram.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.stogram.util.PermissionHelper
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    permissionsGranted: StateFlow<Boolean>? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val permissionsGrantedValue by permissionsGranted?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Блок статуса разрешений
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Статус Mesh сети",
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusItem(
                        label = "NFC",
                        isAvailable = PermissionHelper.isNfcAvailable(context)
                    )
                    StatusItem(
                        label = "BLE",
                        isAvailable = PermissionHelper.isBleAvailable(context)
                    )
                    StatusItem(
                        label = "Wi-Fi",
                        isAvailable = true // Wi-Fi Direct проверяется отдельно
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!permissionsGrantedValue) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Предоставить разрешения")
                    }
                } else {
                    Text(
                        text = "✓ Все разрешения предоставлены",
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = permissionsGrantedValue
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) "Скрыть" else "Показать"
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(icon)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = permissionsGrantedValue
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = permissionsGrantedValue
        ) {
            Text("Войти")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onRegisterClick) {
            Text("Ещё нет аккаунта? Зарегистрируйтесь")
        }
    }
}

@Composable
private fun StatusItem(label: String, isAvailable: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption
        )
        Text(
            text = if (isAvailable) "✓" else "✗",
            color = if (isAvailable) MaterialTheme.colors.primary else MaterialTheme.colors.error,
            style = MaterialTheme.typography.h6
        )
    }
}