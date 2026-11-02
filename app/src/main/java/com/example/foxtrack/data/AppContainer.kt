package com.example.foxtrack.data

import android.content.Context
import com.example.foxtrack.data.db.FoxDatabase
import com.example.foxtrack.data.local.SettingsStore
import com.example.foxtrack.data.remote.TgdbClient
import com.example.foxtrack.data.repo.AuthRepository
import com.example.foxtrack.data.repo.LibraryRepository
import com.example.foxtrack.data.repo.SearchRepository
import com.example.foxtrack.data.repo.SessionManager

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val settings = SettingsStore(appContext)
    val database = FoxDatabase(appContext)
    val tgdb = TgdbClient(database = database, apiKey = { settings.effectiveApiKey() })
    val auth = AuthRepository(database, settings)
    val library = LibraryRepository(database, tgdb)
    val search = SearchRepository(database, tgdb)
    val session = SessionManager(auth)
}
