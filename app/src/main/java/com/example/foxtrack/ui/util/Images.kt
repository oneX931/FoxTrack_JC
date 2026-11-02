package com.example.foxtrack.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.foxtrack.domain.AppException
import com.example.foxtrack.domain.ErrorCode
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val AVATAR_SIZE = 256
private const val AVATAR_QUALITY = 85
private const val DECODE_LIMIT = 512

fun decodeAvatar(encoded: String?): ImageBitmap? {
    if (encoded.isNullOrEmpty()) return null
    return try {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: IllegalArgumentException) {
        null
    }
}

suspend fun prepareAvatarImage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val shortest = min(info.size.width, info.size.height)
            decoder.setTargetSampleSize(max(1, shortest / DECODE_LIMIT))
        }
        val side = min(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
        val size = min(AVATAR_SIZE, side)
        val scaled = Bitmap.createScaledBitmap(square, size, size, true)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, AVATAR_QUALITY, output)
        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
        throw AppException(ErrorCode.AVATAR_UNREADABLE)
    }
}
