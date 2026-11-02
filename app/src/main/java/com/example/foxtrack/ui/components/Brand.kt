package com.example.foxtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foxtrack.R
import com.example.foxtrack.ui.theme.PixelFamily

@Composable
fun Logo(modifier: Modifier = Modifier, markSize: Dp = 32.dp, withWordmark: Boolean = true) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier
                .size(markSize)
                .clip(MaterialTheme.shapes.extraSmall),
        )
        if (withWordmark) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.app_name),
                fontFamily = PixelFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
