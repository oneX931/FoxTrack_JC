package com.example.foxtrack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.foxtrack.R
import com.example.foxtrack.ui.theme.FoxTheme

@Composable
fun foxTextFieldColors(): TextFieldColors {
    val colors = FoxTheme.colors
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        disabledTextColor = scheme.onSurfaceVariant,
        errorTextColor = scheme.onSurface,
        focusedContainerColor = colors.field,
        unfocusedContainerColor = colors.field,
        disabledContainerColor = colors.surfaceSunken,
        errorContainerColor = colors.field,
        cursorColor = colors.focus,
        errorCursorColor = colors.danger,
        focusedBorderColor = colors.focus,
        unfocusedBorderColor = colors.borderStrong,
        disabledBorderColor = colors.border,
        errorBorderColor = colors.danger,
        focusedLabelColor = colors.focus,
        unfocusedLabelColor = scheme.onSurfaceVariant,
        disabledLabelColor = scheme.onSurfaceVariant,
        errorLabelColor = colors.danger,
        focusedLeadingIconColor = scheme.onSurfaceVariant,
        unfocusedLeadingIconColor = scheme.onSurfaceVariant,
        errorLeadingIconColor = colors.danger,
        focusedSupportingTextColor = scheme.onSurfaceVariant,
        unfocusedSupportingTextColor = scheme.onSurfaceVariant,
        errorSupportingTextColor = colors.danger,
    )
}

@Composable
private fun SupportRow(error: String?, hint: String?, counter: String?, counterOver: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = error ?: hint.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        if (counter != null) {
            Text(
                text = counter,
                style = MaterialTheme.typography.bodySmall,
                color = if (counterOver) FoxTheme.colors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FoxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    placeholder: String? = null,
    counter: String? = null,
    counterOver: Boolean = false,
    @DrawableRes leadingIcon: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val hasSupport = error != null || hint != null || counter != null
    val supportContent: @Composable () -> Unit = { SupportRow(error, hint, counter, counterOver) }
    val leadingContent: @Composable () -> Unit = {
        if (leadingIcon != null) FoxIcon(id = leadingIcon, size = 20.dp)
    }
    val placeholderContent: @Composable () -> Unit = { Text(placeholder.orEmpty()) }
    val actions = KeyboardActions(
        onNext = { onImeAction?.invoke() },
        onDone = { onImeAction?.invoke() },
        onSearch = { onImeAction?.invoke() },
        onGo = { onImeAction?.invoke() },
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        placeholder = if (placeholder != null) placeholderContent else null,
        leadingIcon = if (leadingIcon != null) leadingContent else null,
        trailingIcon = trailing,
        supportingText = if (hasSupport) supportContent else null,
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = if (onImeAction != null) actions else KeyboardActions.Default,
        shape = MaterialTheme.shapes.small,
        colors = foxTextFieldColors(),
    )
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    @DrawableRes leadingIcon: Int? = R.drawable.ic_lock,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
) {
    FoxTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        error = error,
        hint = hint,
        leadingIcon = leadingIcon,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            IconButton(onClick = onToggleVisible) {
                FoxIcon(
                    id = if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye,
                    size = 20.dp,
                    contentDescription = stringResource(if (visible) R.string.auth_hide_password else R.string.auth_show_password),
                )
            }
        },
    )
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    submitDescription: String,
    clearDescription: String,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val hasError = error != null
    val supportContent: @Composable () -> Unit = {
        Text(text = error.orEmpty(), style = MaterialTheme.typography.bodySmall)
    }
    val clearContent: @Composable () -> Unit = {
        IconButton(onClick = onClear) {
            FoxIcon(id = R.drawable.ic_x, size = 18.dp, contentDescription = clearDescription)
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = {
            IconButton(onClick = onSubmit) {
                FoxIcon(id = R.drawable.ic_search, size = 20.dp, contentDescription = submitDescription)
            }
        },
        trailingIcon = if (value.isNotEmpty()) clearContent else null,
        supportingText = if (hasError) supportContent else null,
        isError = hasError,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        shape = MaterialTheme.shapes.medium,
        colors = foxTextFieldColors(),
    )
}
