package com.example.foxtrack

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foxtrack.data.local.SettingsStore
import com.example.foxtrack.data.local.ThemePreference
import com.example.foxtrack.ui.FoxTrackRoot
import com.example.foxtrack.ui.theme.FoxTrackTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = SettingsStore.readLanguage(newBase)
        val locale = Locale.forLanguageTag(language.tag)
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocales(LocaleList(locale))
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FoxTrackApp).container
        setContent {
            val theme by container.settings.theme.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (theme) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                null -> systemDark
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            FoxTrackTheme(darkTheme = dark) {
                FoxTrackRoot(container)
            }
        }
    }
}
