package com.example.foxtrack.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ConfirmDialog(
    title: String,
    description: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    busy: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        title = { Text(text = title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text = description) },
        confirmButton = {
            FoxButton(
                text = confirmLabel,
                onClick = onConfirm,
                style = if (danger) FoxButtonStyle.Danger else FoxButtonStyle.Primary,
                loading = busy,
                compact = true,
            )
        },
        dismissButton = {
            FoxButton(
                text = cancelLabel,
                onClick = onDismiss,
                style = FoxButtonStyle.Ghost,
                enabled = !busy,
                compact = true,
            )
        },
    )
}
