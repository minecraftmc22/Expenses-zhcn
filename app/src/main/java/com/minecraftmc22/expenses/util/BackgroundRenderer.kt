package com.minecraftmc22.expenses.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.minecraftmc22.expenses.common.presentation.BackgroundSettings
import java.io.File

/**
 * Turns the picked picture into the bitmap that is drawn behind an activity.
 *
 * The picture is decoded downsampled, center cropped to the screen aspect ratio, blurred and
 * brightened. The blur is a plain box blur written here instead of RenderScript: RenderScript
 * is deprecated and a Kotlin implementation needs neither a platform dependency nor an extra
 * build flag.
 *
 * The result is cached, so the work happens once per picture / adjustment / size combination.
 */
object BackgroundRenderer {

    /** Bitmap width used when there is nothing to blur; it is scaled up when drawn. */
    private const val MAX_RENDER_WIDTH = 720

    /** Blurring costs time, so a blurred background is rendered smaller. */
    private const val MAX_BLURRED_RENDER_WIDTH = 480

    private const val BLUR_PASSES = 3

    private var cacheKey: String? = null
    private var cacheBitmap: Bitmap? = null

    /**
     * Returns the background for the given screen size, or null when there is nothing to draw.
     * The returned bitmap is never recycled: it may be set as a window background while drawn.
     */
    fun render(settings: BackgroundSettings, width: Int, height: Int): Bitmap? {
        val path = settings.imagePath ?: return null
        if (width <= 0 || height <= 0) return null

        val file = File(path)
        if (!file.exists()) return null

        val key = "$path|${settings.blur}|${settings.brightness}|$width" +
            "x$height|${file.lastModified()}"
        if (key == cacheKey) return cacheBitmap

        val limit = if (settings.blur > 0) MAX_BLURRED_RENDER_WIDTH else MAX_RENDER_WIDTH
        val renderWidth = if (width > limit) limit else width
        val renderHeight = (renderWidth.toLong() * height / width).toInt().coerceAtLeast(1)

        val decoded = decode(path, renderWidth, renderHeight, file.length()) ?: return null
        val cropped = crop(decoded, renderWidth, renderHeight)
        val blurred = if (settings.blur > 0) blur(cropped, settings.blur) else cropped
        val result = brighten(blurred, settings.brightness)

        cacheKey = key
        cacheBitmap = result

        return result
    }

    private fun decode(
        path: String,
        targetWidth: Int,
        targetHeight: Int,
        fileSize: Long
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return BitmapFactory.decodeFile(path, options)
    }

    /** Largest power of two that still keeps the picture at or above the target size. */
    private fun sampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        var sample = 1
        val halfWidth = width / 2
        val halfHeight = height / 2

        while (halfWidth / sample >= targetWidth && halfHeight / sample >= targetHeight) {
            sample *= 2
        }

        return sample
    }

    /** Scales the picture to cover the target and cuts the overflow away, keeping the middle. */
    private fun crop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (source.width == targetWidth && source.height == targetHeight) return source

        val widthScale = targetWidth.toFloat() / source.width
        val heightScale = targetHeight.toFloat() / source.height
        val scale = if (widthScale > heightScale) widthScale else heightScale

        val scaledWidth = (source.width * scale).toInt().coerceAtLeast(targetWidth)
        val scaledHeight = (source.height * scale).toInt().coerceAtLeast(targetHeight)

        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val left = (scaledWidth - targetWidth) / 2
        val top = (scaledHeight - targetHeight) / 2

        return Bitmap.createBitmap(scaled, left, top, targetWidth, targetHeight)
    }

    private fun blur(source: Bitmap, radius: Int): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val scratch = IntArray(width * height)

        repeat(BLUR_PASSES) {
            blurHorizontally(pixels, scratch, width, height, radius)
            blurVertically(scratch, pixels, width, height, radius)
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun blurHorizontally(
        input: IntArray,
        output: IntArray,
        width: Int,
        height: Int,
        radius: Int
    ) {
        val diameter = radius * 2 + 1

        for (y in 0 until height) {
            val row = y * width
            var alpha = 0
            var red = 0
            var green = 0
            var blue = 0

            for (offset in -radius..radius) {
                val color = input[row + offset.coerceIn(0, width - 1)]
                alpha += (color ushr 24) and 0xFF
                red += (color ushr 16) and 0xFF
                green += (color ushr 8) and 0xFF
                blue += color and 0xFF
            }

            for (x in 0 until width) {
                output[row + x] = ((alpha / diameter) shl 24) or
                    ((red / diameter) shl 16) or
                    ((green / diameter) shl 8) or
                    (blue / diameter)

                val leaving = input[row + (x - radius).coerceIn(0, width - 1)]
                val entering = input[row + (x + radius + 1).coerceIn(0, width - 1)]

                alpha += ((entering ushr 24) and 0xFF) - ((leaving ushr 24) and 0xFF)
                red += ((entering ushr 16) and 0xFF) - ((leaving ushr 16) and 0xFF)
                green += ((entering ushr 8) and 0xFF) - ((leaving ushr 8) and 0xFF)
                blue += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }

    private fun blurVertically(
        input: IntArray,
        output: IntArray,
        width: Int,
        height: Int,
        radius: Int
    ) {
        val diameter = radius * 2 + 1

        for (x in 0 until width) {
            var alpha = 0
            var red = 0
            var green = 0
            var blue = 0

            for (offset in -radius..radius) {
                val color = input[offset.coerceIn(0, height - 1) * width + x]
                alpha += (color ushr 24) and 0xFF
                red += (color ushr 16) and 0xFF
                green += (color ushr 8) and 0xFF
                blue += color and 0xFF
            }

            for (y in 0 until height) {
                output[y * width + x] = ((alpha / diameter) shl 24) or
                    ((red / diameter) shl 16) or
                    ((green / diameter) shl 8) or
                    (blue / diameter)

                val leaving = input[(y - radius).coerceIn(0, height - 1) * width + x]
                val entering = input[(y + radius + 1).coerceIn(0, height - 1) * width + x]

                alpha += ((entering ushr 24) and 0xFF) - ((leaving ushr 24) and 0xFF)
                red += ((entering ushr 16) and 0xFF) - ((leaving ushr 16) and 0xFF)
                green += ((entering ushr 8) and 0xFF) - ((leaving ushr 8) and 0xFF)
                blue += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }

    private fun brighten(source: Bitmap, brightness: Int): Bitmap {
        if (brightness == BackgroundSettings.BRIGHTNESS_DEFAULT) return source

        val scale = brightness.toFloat() / 100f
        val colorMatrix = ColorMatrix().apply { setScale(scale, scale, scale, 1f) }

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
            isFilterBitmap = true
        }

        Canvas(result).drawBitmap(source, 0f, 0f, paint)

        return result
    }
}
