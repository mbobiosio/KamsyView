package com.github.kamsyview.processing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.withSign

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * BlurHashDecoder - Modern implementation for decoding BlurHash strings into bitmaps
 * Optimized for performance with coroutines support and improved caching
 */
object BlurHashDecoder {

    // Thread-safe cache for Math.cos() calculations to improve performance
    private val cacheCosinesX = ConcurrentHashMap<Int, DoubleArray>()
    private val cacheCosinesY = ConcurrentHashMap<Int, DoubleArray>()

    // Character mapping for base83 decoding
    private val charMap = mapOf(
        '0' to 0, '1' to 1, '2' to 2, '3' to 3, '4' to 4, '5' to 5, '6' to 6, '7' to 7, '8' to 8, '9' to 9,
        'A' to 10, 'B' to 11, 'C' to 12, 'D' to 13, 'E' to 14, 'F' to 15, 'G' to 16, 'H' to 17, 'I' to 18,
        'J' to 19, 'K' to 20, 'L' to 21, 'M' to 22, 'N' to 23, 'O' to 24, 'P' to 25, 'Q' to 26, 'R' to 27,
        'S' to 28, 'T' to 29, 'U' to 30, 'V' to 31, 'W' to 32, 'X' to 33, 'Y' to 34, 'Z' to 35,
        'a' to 36, 'b' to 37, 'c' to 38, 'd' to 39, 'e' to 40, 'f' to 41, 'g' to 42, 'h' to 43, 'i' to 44,
        'j' to 45, 'k' to 46, 'l' to 47, 'm' to 48, 'n' to 49, 'o' to 50, 'p' to 51, 'q' to 52, 'r' to 53,
        's' to 54, 't' to 55, 'u' to 56, 'v' to 57, 'w' to 58, 'x' to 59, 'y' to 60, 'z' to 61,
        '#' to 62, '$' to 63, '%' to 64, '*' to 65, '+' to 66, ',' to 67, '-' to 68, '.' to 69, ':' to 70,
        ';' to 71, '=' to 72, '?' to 73, '@' to 74, '[' to 75, ']' to 76, '^' to 77, '_' to 78, '{' to 79,
        '|' to 80, '}' to 81, '~' to 82
    )

    // Constants for validation and optimization
    private const val MIN_HASH_LENGTH = 6
    private const val MIN_DIMENSION = 1
    private const val MAX_DIMENSION = 4096
    private const val MIN_PUNCH = 0.1f
    private const val MAX_PUNCH = 10.0f
    private const val DEFAULT_PUNCH = 1.0f

    /**
     * Clear all cached calculations to free memory
     */
    fun clearCache() {
        cacheCosinesX.clear()
        cacheCosinesY.clear()
    }

    /**
     * Get cache statistics for monitoring
     */
    fun getCacheStatistics(): CacheStatistics {
        return CacheStatistics(
            cosinesXCacheSize = cacheCosinesX.size,
            cosinesYCacheSize = cacheCosinesY.size,
            totalCacheSize = cacheCosinesX.size + cacheCosinesY.size
        )
    }

    /**
     * Decode BlurHash string into a Bitmap
     *
     * @param blurHash The BlurHash string to decode
     * @param width Desired width of the output bitmap
     * @param height Desired height of the output bitmap
     * @param punch Color punch value (default: 1.0)
     * @param useCache Whether to use cosine calculation cache (default: true)
     * @return Decoded Bitmap or null if decoding fails
     */
    fun decode(
        blurHash: String?,
        @IntRange(from = 1, to = 4096) width: Int,
        @IntRange(from = 1, to = 4096) height: Int,
        @FloatRange(from = 0.1, to = 10.0) punch: Float = DEFAULT_PUNCH,
        useCache: Boolean = true
    ): Bitmap? {
        // Input validation
        val validationResult = validateInput(blurHash, width, height, punch)
        if (validationResult != null) {
            return null
        }

        val hash = blurHash!! // Safe after validation

        return try {
            decodeInternal(hash, width, height, punch, useCache)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Suspend function for decoding BlurHash asynchronously
     */
    suspend fun decodeAsync(
        blurHash: String?,
        @IntRange(from = 1, to = 4096) width: Int,
        @IntRange(from = 1, to = 4096) height: Int,
        @FloatRange(from = 0.1, to = 10.0) punch: Float = DEFAULT_PUNCH,
        useCache: Boolean = true
    ): Bitmap? = withContext(Dispatchers.Default) {
        decode(blurHash, width, height, punch, useCache)
    }

    /**
     * Validate BlurHash and decode components
     */
    fun validateBlurHash(blurHash: String?): BlurHashValidationResult {
        if (blurHash.isNullOrBlank()) {
            return BlurHashValidationResult.Invalid("BlurHash is null or empty")
        }

        if (blurHash.length < MIN_HASH_LENGTH) {
            return BlurHashValidationResult.Invalid("BlurHash is too short (minimum $MIN_HASH_LENGTH characters)")
        }

        return try {
            val numCompEnc = decode83(blurHash, 0, 1)
            val numCompX = (numCompEnc % 9) + 1
            val numCompY = (numCompEnc / 9) + 1
            val expectedLength = 4 + 2 * numCompX * numCompY

            if (blurHash.length != expectedLength) {
                return BlurHashValidationResult.Invalid("Invalid BlurHash length. Expected $expectedLength, got ${blurHash.length}")
            }

            BlurHashValidationResult.Valid(
                componentsX = numCompX,
                componentsY = numCompY,
                expectedLength = expectedLength
            )
        } catch (e: Exception) {
            BlurHashValidationResult.Invalid("Invalid BlurHash format: ${e.message}")
        }
    }

    /**
     * Internal decoding implementation
     */
    private fun decodeInternal(
        blurHash: String,
        width: Int,
        height: Int,
        punch: Float,
        useCache: Boolean
    ): Bitmap? {
        // Parse BlurHash components
        val numCompEnc = decode83(blurHash, 0, 1)
        val numCompX = (numCompEnc % 9) + 1
        val numCompY = (numCompEnc / 9) + 1

        // Validate expected length
        val expectedLength = 4 + 2 * numCompX * numCompY
        if (blurHash.length != expectedLength) {
            return null
        }

        // Parse maximum AC component
        val maxAcEnc = decode83(blurHash, 1, 2)
        val maxAc = (maxAcEnc + 1) / 166f

        // Decode color components
        val colors = Array(numCompX * numCompY) { i ->
            when (i) {
                0 -> {
                    // DC component (average color)
                    val colorEnc = decode83(blurHash, 2, 6)
                    decodeDc(colorEnc)
                }
                else -> {
                    // AC components (detail colors)
                    val from = 4 + i * 2
                    val colorEnc = decode83(blurHash, from, from + 2)
                    decodeAc(colorEnc, maxAc * punch)
                }
            }
        }

        // Generate bitmap
        return composeBitmap(width, height, numCompX, numCompY, colors, useCache)
    }

    /**
     * Validate input parameters
     */
    private fun validateInput(
        blurHash: String?,
        width: Int,
        height: Int,
        punch: Float
    ): String? {
        return when {
            blurHash.isNullOrBlank() -> "BlurHash is null or empty"
            blurHash.length < MIN_HASH_LENGTH -> "BlurHash is too short"
            width < MIN_DIMENSION || width > MAX_DIMENSION -> "Invalid width: $width (must be $MIN_DIMENSION-$MAX_DIMENSION)"
            height < MIN_DIMENSION || height > MAX_DIMENSION -> "Invalid height: $height (must be $MIN_DIMENSION-$MAX_DIMENSION)"
            punch < MIN_PUNCH || punch > MAX_PUNCH -> "Invalid punch: $punch (must be $MIN_PUNCH-$MAX_PUNCH)"
            else -> null
        }
    }

    /**
     * Decode base83 encoded string
     */
    fun decode83(str: String, from: Int = 0, to: Int = str.length): Int {
        var result = 0
        for (i in from until to) {
            val char = str[i]
            val value = charMap[char] ?: throw IllegalArgumentException("Invalid character in BlurHash: '$char'")
            result = result * 83 + value
        }
        return result
    }

    /**
     * Decode DC (average color) component
     */
    private fun decodeDc(colorEnc: Int): FloatArray {
        val r = colorEnc shr 16
        val g = (colorEnc shr 8) and 255
        val b = colorEnc and 255
        return floatArrayOf(srgbToLinear(r), srgbToLinear(g), srgbToLinear(b))
    }

    /**
     * Decode AC (detail) component
     */
    private fun decodeAc(value: Int, maxAc: Float): FloatArray {
        val r = value / (19 * 19)
        val g = (value / 19) % 19
        val b = value % 19
        return floatArrayOf(
            signedPow2((r - 9) / 9.0f) * maxAc,
            signedPow2((g - 9) / 9.0f) * maxAc,
            signedPow2((b - 9) / 9.0f) * maxAc
        )
    }

    /**
     * Convert sRGB color value to linear RGB
     */
    private fun srgbToLinear(colorEnc: Int): Float {
        val v = colorEnc / 255f
        return if (v <= 0.04045f) {
            v / 12.92f
        } else {
            ((v + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    /**
     * Convert linear RGB to sRGB color value
     */
    private fun linearToSrgb(value: Float): Int {
        val v = value.coerceIn(0f, 1f)
        return if (v <= 0.0031308f) {
            (v * 12.92f * 255f + 0.5f).toInt()
        } else {
            ((1.055f * v.pow(1 / 2.4f) - 0.055f) * 255 + 0.5f).toInt()
        }
    }

    /**
     * Signed power function for AC component calculation
     */
    private fun signedPow2(value: Float): Float = value.pow(2f).withSign(value)

    /**
     * Compose the final bitmap from color components
     */
    private fun composeBitmap(
        width: Int,
        height: Int,
        numCompX: Int,
        numCompY: Int,
        colors: Array<FloatArray>,
        useCache: Boolean
    ): Bitmap {
        val imageArray = IntArray(width * height)

        // Get or calculate cosine arrays
        val cosinesX = getCosineArray(width, numCompX, useCache, cacheCosinesX)
        val cosinesY = getCosineArray(height, numCompY, useCache, cacheCosinesY)

        // Calculate each pixel
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f
                var g = 0f
                var b = 0f

                // Sum contributions from all components
                for (j in 0 until numCompY) {
                    for (i in 0 until numCompX) {
                        val cosX = cosinesX.getCosine(i, numCompX, x, width)
                        val cosY = cosinesY.getCosine(j, numCompY, y, height)
                        val basis = (cosX * cosY).toFloat()
                        val color = colors[j * numCompX + i]

                        r += color[0] * basis
                        g += color[1] * basis
                        b += color[2] * basis
                    }
                }

                // Convert to sRGB and store
                imageArray[x + width * y] = Color.rgb(
                    linearToSrgb(r),
                    linearToSrgb(g),
                    linearToSrgb(b)
                )
            }
        }

        return Bitmap.createBitmap(imageArray, width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Get cosine array for dimension and component count
     */
    private fun getCosineArray(
        dimension: Int,
        numComp: Int,
        useCache: Boolean,
        cache: ConcurrentHashMap<Int, DoubleArray>
    ): CosineArray {
        val cacheKey = dimension * numComp

        return if (useCache) {
            val cached = cache.getOrPut(cacheKey) { DoubleArray(dimension * numComp) }
            CosineArray(cached, needsCalculation = cached.all { it == 0.0 })
        } else {
            CosineArray(DoubleArray(dimension * numComp), needsCalculation = true)
        }
    }

    /**
     * Wrapper for cosine array with calculation state
     */
    private class CosineArray(
        private val array: DoubleArray,
        private val needsCalculation: Boolean
    ) {
        fun getCosine(comp: Int, numComp: Int, pos: Int, dimension: Int): Double {
            val index = comp + numComp * pos

            if (needsCalculation) {
                array[index] = cos(PI * pos * comp / dimension)
            }

            return array[index]
        }
    }
}

/**
 * Result of BlurHash validation
 */
sealed class BlurHashValidationResult {
    data class Valid(
        val componentsX: Int,
        val componentsY: Int,
        val expectedLength: Int
    ) : BlurHashValidationResult()

    data class Invalid(val reason: String) : BlurHashValidationResult()
}

/**
 * Cache statistics for monitoring
 */
data class CacheStatistics(
    val cosinesXCacheSize: Int,
    val cosinesYCacheSize: Int,
    val totalCacheSize: Int
) {
    val memoryUsageApprox: Long
        get() = totalCacheSize * 8L // 8 bytes per double
}

/**
 * Extension functions for easier usage
 */
fun String.decodeBlurHash(
    width: Int,
    height: Int,
    punch: Float = 1.0f,
    useCache: Boolean = true
): Bitmap? = BlurHashDecoder.decode(this, width, height, punch, useCache)

suspend fun String.decodeBlurHashAsync(
    width: Int,
    height: Int,
    punch: Float = 1.0f,
    useCache: Boolean = true
): Bitmap? = BlurHashDecoder.decodeAsync(this, width, height, punch, useCache)

fun String.validateBlurHash(): BlurHashValidationResult = BlurHashDecoder.validateBlurHash(this)

/**
 * Utility functions for BlurHash manipulation
 */
object BlurHashUtils {

    /**
     * Calculate optimal dimensions for a given aspect ratio
     */
    fun calculateOptimalDimensions(
        targetWidth: Int,
        targetHeight: Int,
        maxDimension: Int = 32
    ): Pair<Int, Int> {
        val aspectRatio = targetWidth.toFloat() / targetHeight.toFloat()

        return when {
            aspectRatio > 1f -> {
                // Landscape
                val width = minOf(maxDimension, targetWidth)
                val height = (width / aspectRatio).toInt().coerceAtLeast(1)
                width to height
            }
            aspectRatio < 1f -> {
                // Portrait
                val height = minOf(maxDimension, targetHeight)
                val width = (height * aspectRatio).toInt().coerceAtLeast(1)
                width to height
            }
            else -> {
                // Square
                val size = minOf(maxDimension, maxOf(targetWidth, targetHeight))
                size to size
            }
        }
    }

    /**
     * Get dominant color from BlurHash (DC component)
     */
    fun getDominantColor(blurHash: String): Int? {
        return try {
            if (blurHash.length < 6) return null

            val colorEnc = BlurHashDecoder.decode83(blurHash, 2, 6)
            val r = colorEnc shr 16
            val g = (colorEnc shr 8) and 255
            val b = colorEnc and 255

            Color.rgb(r, g, b)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if BlurHash represents a mostly light or dark image
     */
    fun isLightImage(blurHash: String): Boolean? {
        val dominantColor = getDominantColor(blurHash) ?: return null

        val r = Color.red(dominantColor) / 255f
        val g = Color.green(dominantColor) / 255f
        val b = Color.blue(dominantColor) / 255f

        // Calculate relative luminance
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        return luminance > 0.5f
    }
}
