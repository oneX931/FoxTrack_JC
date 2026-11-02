package com.example.foxtrack.ui.util

import android.content.Context
import com.example.foxtrack.data.local.AppLanguage
import com.example.foxtrack.data.local.SettingsStore

fun applyLanguage(context: Context, settings: SettingsStore, language: AppLanguage) {
    if (settings.language.value == language) return
    settings.setLanguage(language)
    context.findActivity()?.recreate()
}
