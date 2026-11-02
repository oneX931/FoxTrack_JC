package com.example.foxtrack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foxtrack.R
import com.example.foxtrack.domain.PlatformIconKind
import com.example.foxtrack.domain.platformColor
import com.example.foxtrack.domain.platformIconKind

@Composable
fun FoxIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

@DrawableRes
fun platformIconRes(name: String?): Int = when (platformIconKind(name)) {
    PlatformIconKind.MONITOR -> R.drawable.ic_monitor
    PlatformIconKind.SMARTPHONE -> R.drawable.ic_smartphone
    PlatformIconKind.JOYSTICK -> R.drawable.ic_joystick
    PlatformIconKind.GAMEPAD -> R.drawable.ic_gamepad
}

@Composable
fun PlatformIcon(name: String?, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    FoxIcon(id = platformIconRes(name), modifier = modifier, size = size, tint = Color(platformColor(name)))
}
