package com.example.foxtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.foxtrack.R
import com.example.foxtrack.ui.theme.FoxTheme

@Composable
fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var failed by remember(url) { mutableStateOf(false) }
    Box(
        modifier = modifier.background(FoxTheme.colors.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null && !failed) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onError = { failed = true },
            )
        } else {
            FoxIcon(
                id = R.drawable.ic_image_off,
                size = iconSize,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = stringResource(R.string.cover_missing),
            )
        }
    }
}
