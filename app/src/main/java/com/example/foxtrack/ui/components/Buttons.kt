package com.example.foxtrack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foxtrack.ui.theme.FoxTheme

enum class FoxButtonStyle { Primary, Secondary, Danger, Ghost }

@Composable
fun Spinner(modifier: Modifier = Modifier, size: Dp = 18.dp, color: Color = MaterialTheme.colorScheme.primary) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = 2.dp,
    )
}

@Composable
fun FoxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FoxButtonStyle = FoxButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
    compact: Boolean = false,
) {
    val foxColors = FoxTheme.colors
    val scheme = MaterialTheme.colorScheme
    val contentColor = when (style) {
        FoxButtonStyle.Primary -> scheme.onPrimary
        FoxButtonStyle.Secondary -> scheme.onSurface
        FoxButtonStyle.Danger -> foxColors.danger
        FoxButtonStyle.Ghost -> scheme.onSurface
    }
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            Spinner(size = 16.dp, color = contentColor)
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            FoxIcon(id = icon, size = 18.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    val padding = if (compact) PaddingValues(horizontal = 14.dp, vertical = 6.dp) else PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    val sized = modifier.heightIn(min = if (compact) 36.dp else 48.dp)
    val guardedClick = { if (!loading) onClick() }
    val shape = MaterialTheme.shapes.small

    when (style) {
        FoxButtonStyle.Primary -> Button(
            onClick = guardedClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                disabledContainerColor = scheme.primary.copy(alpha = 0.4f),
                disabledContentColor = scheme.onPrimary.copy(alpha = 0.7f),
            ),
            content = content,
        )

        FoxButtonStyle.Secondary -> OutlinedButton(
            onClick = guardedClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            border = BorderStroke(1.dp, foxColors.borderStrong),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = scheme.onSurface,
                disabledContentColor = scheme.onSurface.copy(alpha = 0.45f),
            ),
            content = content,
        )

        FoxButtonStyle.Danger -> OutlinedButton(
            onClick = guardedClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            border = BorderStroke(1.dp, foxColors.danger),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = foxColors.dangerBg,
                contentColor = foxColors.danger,
                disabledContentColor = foxColors.danger.copy(alpha = 0.5f),
            ),
            content = content,
        )

        FoxButtonStyle.Ghost -> TextButton(
            onClick = guardedClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            colors = ButtonDefaults.textButtonColors(
                contentColor = scheme.onSurface,
                disabledContentColor = scheme.onSurface.copy(alpha = 0.45f),
            ),
            content = content,
        )
    }
}

@Composable
fun FoxIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 22.dp,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        FoxIcon(id = icon, size = size, tint = tint, contentDescription = contentDescription)
    }
}
