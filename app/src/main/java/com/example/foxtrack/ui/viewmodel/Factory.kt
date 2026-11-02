package com.example.foxtrack.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foxtrack.FoxTrackApp
import com.example.foxtrack.data.AppContainer

class FoxViewModelFactory(private val build: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = build() as T
}

@Composable
fun rememberContainer(): AppContainer =
    (LocalContext.current.applicationContext as FoxTrackApp).container
