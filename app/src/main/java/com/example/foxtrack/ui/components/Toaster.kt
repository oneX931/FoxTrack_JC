package com.example.foxtrack.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.foxtrack.ui.theme.FoxTheme
import com.example.foxtrack.ui.util.ToastEvent
import com.example.foxtrack.ui.util.ToastKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private class FoxSnackbarVisuals(
    val kind: ToastKind,
    override val message: String,
    override val actionLabel: String?,
    override val duration: SnackbarDuration,
    override val withDismissAction: Boolean,
) : SnackbarVisuals

class Toaster(
    private val host: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun show(kind: ToastKind, message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val visuals = FoxSnackbarVisuals(
                kind = kind,
                message = message,
                actionLabel = actionLabel,
                duration = if (kind == ToastKind.Error) SnackbarDuration.Long else SnackbarDuration.Short,
                withDismissAction = true,
            )
            val result = host.showSnackbar(visuals)
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

val LocalToaster = staticCompositionLocalOf<Toaster> { error("Toaster is not provided") }

@Composable
fun rememberToaster(host: SnackbarHostState): Toaster {
    val scope = rememberCoroutineScope()
    return remember(host, scope) { Toaster(host, scope) }
}

@Composable
fun FoxSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val colors = FoxTheme.colors
    SnackbarHost(hostState = state, modifier = modifier) { data ->
        val kind = (data.visuals as? FoxSnackbarVisuals)?.kind ?: ToastKind.Info
        val (container, content) = when (kind) {
            ToastKind.Success -> colors.doneBg to colors.done
            ToastKind.Error -> colors.dangerBg to colors.danger
            ToastKind.Info -> colors.wishBg to colors.wish
        }
        Snackbar(
            snackbarData = data,
            shape = MaterialTheme.shapes.medium,
            containerColor = container,
            contentColor = content,
            actionColor = content,
            actionContentColor = content,
            dismissActionContentColor = content,
        )
    }
}

@Composable
fun ToastEffect(events: Flow<ToastEvent>, onAction: (String) -> Unit = {}) {
    val toaster = LocalToaster.current
    val context = LocalContext.current
    val currentOnAction by rememberUpdatedState(onAction)
    LaunchedEffect(events) {
        events.collect { event ->
            val key = event.actionKey
            toaster.show(
                kind = event.kind,
                message = event.text.resolve(context),
                actionLabel = event.actionLabel?.resolve(context),
                onAction = if (key != null) ({ currentOnAction(key) }) else null,
            )
        }
    }
}
