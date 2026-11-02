package com.example.foxtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.foxtrack.domain.User
import com.example.foxtrack.ui.util.decodeAvatar
import java.util.Locale

private fun initialOf(username: String): String {
    val trimmed = username.trim()
    if (trimmed.isEmpty()) return "?"
    val first = String(Character.toChars(trimmed.codePointAt(0)))
    return first.uppercase(Locale.getDefault())
}

@Composable
fun Avatar(user: User, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(user.avatar) { decodeAvatar(user.avatar) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = initialOf(user.username),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}
