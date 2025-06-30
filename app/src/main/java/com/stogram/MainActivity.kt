package com.stogram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.stogram.ui.LoginScreen
import com.stogram.ui.theme.StogramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StogramTheme {
                LoginScreen()
            }
        }
    }
}