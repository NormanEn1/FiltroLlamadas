package com.norman.filtrollamadas.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.norman.filtrollamadas.FiltroApp
import com.norman.filtrollamadas.data.settings.AppSettings
import com.norman.filtrollamadas.data.settings.ThemeMode
import com.norman.filtrollamadas.ui.theme.FiltroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FiltroApp).container
        setContent {
            val settings by container.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.SISTEMA -> isSystemInDarkTheme()
                ThemeMode.CLARO -> false
                ThemeMode.OSCURO -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            FiltroTheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
                FiltroRoot()
            }
        }
    }
}
