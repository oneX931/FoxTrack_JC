package com.example.foxtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.foxtrack.domain.PlatformEntry
import com.example.foxtrack.ui.theme.FoxTheme
import kotlin.math.max

@Composable
private fun ChipShell(content: @Composable () -> Unit) {
    val colors = FoxTheme.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.surfaceSunken)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
fun PlatformChip(platform: PlatformEntry) {
    ChipShell {
        PlatformIcon(name = platform.name, size = 13.dp)
        Spacer(Modifier.width(5.dp))
        Text(
            text = platform.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val year = platform.year
        if (year != null) {
            Spacer(Modifier.width(5.dp))
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MoreChip(count: Int) {
    ChipShell {
        Text(
            text = "+$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
fun PlatformOverflowRow(platforms: List<PlatformEntry>, modifier: Modifier = Modifier) {
    if (platforms.isEmpty()) return
    SubcomposeLayout(modifier = modifier) { constraints ->
        val gap = 6.dp.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val available = constraints.maxWidth

        val measured: List<Placeable> = platforms.mapIndexed { index, platform ->
            subcompose("chip-$index") { PlatformChip(platform) }.first().measure(loose)
        }
        val widestMore = subcompose("more-probe") { MoreChip(max(platforms.size - 1, 1)) }.first().measure(loose)

        var used = 0
        var count = 0
        for (index in measured.indices) {
            val width = measured[index].width + if (index > 0) gap else 0
            val reserve = if (index < measured.size - 1) gap + widestMore.width else 0
            if (used + width + reserve > available) break
            used += width
            count = index + 1
        }
        val visible = max(1, count)
        val hidden = platforms.size - visible
        val more: Placeable? = if (hidden > 0) {
            subcompose("more") { MoreChip(hidden) }.first().measure(loose)
        } else {
            null
        }

        val shown = measured.take(visible)
        val height = max(shown.maxOf { it.height }, more?.height ?: 0)
        var total = shown.sumOf { it.width } + gap * (shown.size - 1)
        if (more != null) total += gap + more.width

        layout(width = total.coerceAtMost(available), height = height) {
            var x = 0
            shown.forEach { placeable ->
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width + gap
            }
            more?.placeRelative(x, (height - more.height) / 2)
        }
    }
}
