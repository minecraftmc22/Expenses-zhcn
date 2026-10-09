package com.minecraftmc22.expenses.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Decodes a small square preview of a picture stored in the app's private storage.
 * Returns null when the file is gone or cannot be decoded, so callers can fall back to an icon.
 */
fun loadThumbnail(path: String, size: Int): Bitmap? {
    if (size <= 0 || !File(path).exists()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, size)
    }

    val decoded = BitmapFactory.decodeFile(path, options) ?: return null

    return cropToSquare(decoded, size)
}

private fun sampleSize(width: Int, height: Int, target: Int): Int {
    var sample = 1
    val halfWidth = width / 2
    val halfHeight = height / 2

    while (halfWidth / sample >= target && halfHeight / sample >= target) {
        sample *= 2
    }

    return sample
}

private fun cropToSquare(source: Bitmap, size: Int): Bitmap {
    val edge = if (source.width < source.height) source.width else source.height
    val left = (source.width - edge) / 2
    val top = (source.height - edge) / 2

    val square = Bitmap.createBitmap(source, left, top, edge, edge)

    return if (square.width == size) square else Bitmap.createScaledBitmap(square, size, size, true)
}
