package com.example.foxtrack.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.foxtrack.R
import com.example.foxtrack.data.local.SettingsStore
import com.example.foxtrack.data.local.ThemePreference
import com.example.foxtrack.data.local.AppLanguage
import com.example.foxtrack.ui.theme.FoxTheme
import com.example.foxtrack.ui.util.applyLanguage
import java.time.Year

@Composable
fun ThemeToggleButton(settings: SettingsStore, modifier: Modifier = Modifier) {
    val dark = FoxTheme.colors.isDark
    FoxIconButton(
        icon = if (dark) R.drawable.ic_sun else R.drawable.ic_moon,
        contentDescription = stringResource(if (dark) R.string.theme_to_light else R.string.theme_to_dark),
        onClick = { settings.setTheme(if (dark) ThemePreference.LIGHT else ThemePreference.DARK) },
        modifier = modifier,
    )
}

@Composable
fun LanguageToggleButton(settings: SettingsStore, language: AppLanguage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val target = if (language == AppLanguage.RU) AppLanguage.EN else AppLanguage.RU
    FoxButton(
        text = target.tag.uppercase(),
        onClick = { applyLanguage(context, settings, target) },
        modifier = modifier,
        style = FoxButtonStyle.Ghost,
        compact = true,
    )
}

@Composable
fun PlatformCheckRow(
    name: String,
    year: Int?,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedColor = FoxTheme.colors.borderStrong,
            ),
        )
        Spacer(Modifier.width(8.dp))
        PlatformIcon(name = name, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (year != null) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RatingStars(
    value: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = FoxTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (n in 1..10) {
                val filled = n <= (value ?: 0)
                val description = stringResource(R.string.detail_rating_set, n)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .selectable(
                            selected = value == n,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onChange(if (value == n) null else n) },
                        )
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    FoxIcon(
                        id = if (filled) R.drawable.ic_star_filled else R.drawable.ic_star,
                        size = 24.dp,
                        tint = if (filled) colors.accentInk else colors.borderStrong,
                    )
                }
            }
        }
        if (value != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.detail_rating_value, value),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FoxButton(
                    text = stringResource(R.string.detail_rating_clear),
                    onClick = { onChange(null) },
                    style = FoxButtonStyle.Ghost,
                    compact = true,
                    enabled = enabled,
                    icon = R.drawable.ic_x,
                )
            }
        }
    }
}

@Composable
fun Attribution(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "© ${Year.now().value} ${stringResource(R.string.app_name)} · ${stringResource(R.string.footer_data)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "TheGamesDB",
            modifier = Modifier
                .clickable { uriHandler.openUri("https://thegamesdb.net") }
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = FoxTheme.colors.accentInk,
            textDecoration = TextDecoration.Underline,
        )
    }
}
