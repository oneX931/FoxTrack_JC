package com.example.foxtrack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foxtrack.R
import com.example.foxtrack.domain.ErrorCode
import com.example.foxtrack.domain.GameStatus
import com.example.foxtrack.ui.theme.FoxTheme
import com.example.foxtrack.ui.util.asString
import com.example.foxtrack.ui.util.labelRes

@Composable
fun StateBlock(
    @DrawableRes icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    isError: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = FoxTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isError) colors.dangerBg else colors.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) {
            FoxIcon(
                id = icon,
                size = 30.dp,
                tint = if (isError) colors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(Modifier.height(4.dp))
            action()
        }
    }
}

@Composable
fun ErrorBlock(
    code: ErrorCode,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val isNetwork = code == ErrorCode.NETWORK
    val retryContent: @Composable () -> Unit = {
        FoxButton(
            text = stringResource(R.string.error_retry),
            onClick = { onRetry?.invoke() },
            style = FoxButtonStyle.Secondary,
            icon = R.drawable.ic_refresh,
        )
    }
    StateBlock(
        icon = if (isNetwork) R.drawable.ic_wifi_off else R.drawable.ic_triangle_alert,
        title = title ?: stringResource(if (isNetwork) R.string.error_network_title else R.string.error_generic_title),
        description = code.asString(),
        isError = true,
        modifier = modifier,
        action = if (onRetry != null) retryContent else null,
    )
}

@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Spinner(size = 32.dp)
    }
}

@Composable
private fun rememberPulseAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 900), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    return alpha
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val colors = FoxTheme.colors
    val alpha = rememberPulseAlpha()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .width(72.dp)
                .height(96.dp)
                .clip(MaterialTheme.shapes.small)
                .background(colors.skeleton),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(colors.skeleton))
            Box(Modifier.fillMaxWidth(0.45f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(colors.skeleton))
            Box(Modifier.fillMaxWidth(0.3f).height(24.dp).clip(RoundedCornerShape(12.dp)).background(colors.skeleton))
        }
    }
}

@Composable
fun SkeletonList(count: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { SkeletonCard() }
    }
}

@Composable
fun StatusBadge(status: GameStatus, modifier: Modifier = Modifier) {
    val colors = FoxTheme.colors
    val (background, foreground) = when (status) {
        GameStatus.WISH -> colors.wishBg to colors.wish
        GameStatus.PLAYING -> colors.playBg to colors.play
        GameStatus.DONE -> colors.doneBg to colors.done
    }
    Text(
        text = stringResource(status.labelRes()),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
fun RatingChip(rating: Int?, modifier: Modifier = Modifier) {
    if (rating == null || rating <= 0) return
    val colors = FoxTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.playBg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FoxIcon(id = R.drawable.ic_star_filled, size = 13.dp, tint = colors.accentInk)
        Text(
            text = "$rating/10",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.play,
        )
    }
}

@Composable
fun Tag(text: String, modifier: Modifier = Modifier, @DrawableRes leading: Int? = null, leadingTint: Color? = null) {
    val colors = FoxTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceSunken)
            .border(1.dp, colors.border, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leading != null) {
            FoxIcon(id = leading, size = 13.dp, tint = leadingTint ?: MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun FoxTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = FoxTheme.colors
    val shape = MaterialTheme.shapes.small
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(colors.surfaceSunken)
        .border(1.dp, colors.border, shape)
        .padding(3.dp)
    val rowModifier = if (scrollable) base.horizontalScroll(rememberScrollState()) else base

    Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            val tabShape = RoundedCornerShape(5.dp)
            Box(
                modifier = (if (scrollable) Modifier else Modifier.weight(1f))
                    .clip(tabShape)
                    .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .selectable(selected = active, enabled = enabled, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, FoxTheme.colors.border),
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
fun FormBanner(text: String, modifier: Modifier = Modifier) {
    val colors = FoxTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.dangerBg)
            .border(1.dp, colors.danger, MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        FoxIcon(id = R.drawable.ic_triangle_alert, size = 18.dp, tint = colors.danger)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = colors.danger)
    }
}
