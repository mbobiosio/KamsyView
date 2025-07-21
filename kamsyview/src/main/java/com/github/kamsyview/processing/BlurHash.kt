package com.github.kamsyview.processing

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * BlurHash - High-level API for BlurHash processing with caching and lifecycle management
 * Provides both synchronous and asynchronous APIs with comprehensive caching
 */
@Singleton
class BlurHash @Inject constructor(
    private val context: Context,
    lruSize: Int = DEFAULT_LRU_SIZE,
    @param:FloatRange(from = 0.1, to = 10.0) private var punch: Float = DEFAULT_PUNCH
) {

    companion object {
        const val DEFAULT_LRU_SIZE = 50
        const val DEFAULT_PUNCH = 1f
        const val DEFAULT_PROCESSING_TIMEOUT = 10000L // 10 seconds
        const val MAX_CONCURRENT_OPERATIONS = 4
    }

    // LRU cache for processed drawables
    private val drawableCache = LruCache<String, BitmapDrawable>(lruSize)

    // Coroutine scope for async operations
    private val processingScope = CoroutineScope(
        SupervisorJob() +
                Dispatchers.Default +
                CoroutineName("BlurHashProcessor")
    )

    // Channel for limiting concurrent operations
    private val operationChannel = Channel<Unit>(capacity = MAX_CONCURRENT_OPERATIONS)

    // Track ongoing operations to prevent duplicates
    private val ongoingOperations = ConcurrentHashMap<String, Deferred<BitmapDrawable?>>()

    // Statistics for monitoring
    private val stats = BlurHashStats()

    // Configuration
    private var isEnabled = true
    private var processingTimeout = DEFAULT_PROCESSING_TIMEOUT

    init {
        // Initialize operation permits
        repeat(MAX_CONCURRENT_OPERATIONS) {
            operationChannel.trySend(Unit)
        }
    }

    /**
     * Execute BlurHash processing with callback
     */
    fun execute(
        blurString: String,
        @IntRange(from = 1, to = 4096) width: Int,
        @IntRange(from = 1, to = 4096) height: Int,
        response: (drawable: BitmapDrawable?) -> Unit
    ) {
        if (!isEnabled) {
            response(null)
            return
        }

        // Check cache first
        getCachedDrawable(blurString, width, height)?.let { cached ->
            stats.recordCacheHit()
            response(cached)
            return
        }

        // Process asynchronously
        processingScope.launch {
            try {
                val drawable = processBlurHashInternal(blurString, width, height)
                response(drawable)
            } catch (e: Exception) {
                stats.recordError(e)
                response(null)
            }
        }
    }

    /**
     * Execute BlurHash processing synchronously
     */
    fun executeSync(
        blurString: String,
        @IntRange(from = 1, to = 4096) width: Int,
        @IntRange(from = 1, to = 4096) height: Int
    ): BitmapDrawable? {
        if (!isEnabled) return null

        // Check cache first
        getCachedDrawable(blurString, width, height)?.let { cached ->
            stats.recordCacheHit()
            return cached
        }

        return runBlocking {
            processBlurHashInternal(blurString, width, height)
        }
    }

    /**
     * Execute BlurHash processing as Flow
     */
    fun executeFlow(
        blurString: String,
        @IntRange(from = 1, to = 4096) width: Int,
        @IntRange(from = 1, to = 4096) height: Int
    ): Flow<BlurHashFlowResult> = flow {
        if (!isEnabled) {
            emit(BlurHashFlowResult.Disabled)
            return@flow
        }

        emit(BlurHashFlowResult.Loading)

        // Check cache first
        getCachedDrawable(blurString, width, height)?.let { cached ->
            stats.recordCacheHit()
            emit(BlurHashFlowResult.Success(cached))
            return@flow
        }

        try {
            val drawable = processBlurHashInternal(blurString, width, height)
            if (drawable != null) {
                emit(BlurHashFlowResult.Success(drawable))
            } else {
                emit(BlurHashFlowResult.Error("Failed to decode BlurHash"))
            }
        } catch (e: Exception) {
            stats.recordError(e)
            emit(BlurHashFlowResult.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Execute with timeout
     */
    suspend fun executeWithTimeout(
        blurString: String,
        @IntRange(from = 1, to = 4096) width: Int,
        @IntRange(from = 1, to = 4096) height: Int,
        timeoutMs: Long = processingTimeout
    ): BitmapDrawable? {
        if (!isEnabled) return null

        // Check cache first
        getCachedDrawable(blurString, width, height)?.let { cached ->
            stats.recordCacheHit()
            return cached
        }

        return try {
            withTimeout(timeoutMs) {
                processBlurHashInternal(blurString, width, height)
            }
        } catch (e: TimeoutCancellationException) {
            stats.recordTimeout()
            null
        }
    }

    /**
     * Preload BlurHash into cache
     */
    fun preload(
        blurString: String,
        @IntRange(from = 1, to = 4096) width: Int,
        @IntRange(from = 1, to = 4096) height: Int,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (!isEnabled) {
            onComplete?.invoke(false)
            return
        }

        val cacheKey = createCacheKey(blurString, width, height)

        // Skip if already cached
        if (drawableCache.get(cacheKey) != null) {
            onComplete?.invoke(true)
            return
        }

        processingScope.launch {
            try {
                val drawable = processBlurHashInternal(blurString, width, height)
                onComplete?.invoke(drawable != null)
            } catch (e: Exception) {
                stats.recordError(e)
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Batch preload multiple BlurHashes
     */
    fun preloadBatch(
        blurHashes: List<BlurHashRequest>,
        onProgress: ((Int, Int) -> Unit)? = null,
        onComplete: ((Int, Int) -> Unit)? = null
    ) {
        if (!isEnabled || blurHashes.isEmpty()) {
            onComplete?.invoke(0, 0)
            return
        }

        processingScope.launch {
            var successCount = 0
            var errorCount = 0

            blurHashes.forEachIndexed { index, request ->
                try {
                    val drawable = processBlurHashInternal(request.blurHash, request.width, request.height)
                    if (drawable != null) {
                        successCount++
                    } else {
                        errorCount++
                    }
                } catch (e: Exception) {
                    stats.recordError(e)
                    errorCount++
                }

                onProgress?.invoke(index + 1, blurHashes.size)
            }

            onComplete?.invoke(successCount, errorCount)
        }
    }

    /**
     * Cancel ongoing operation for specific BlurHash
     */
    fun cancelOperation(blurString: String, width: Int, height: Int) {
        val cacheKey = createCacheKey(blurString, width, height)
        ongoingOperations[cacheKey]?.cancel()
        ongoingOperations.remove(cacheKey)
    }

    /**
     * Cancel all ongoing operations
     */
    fun cancelAllOperations() {
        ongoingOperations.values.forEach { it.cancel() }
        ongoingOperations.clear()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        cancelAllOperations()
        processingScope.cancel()
        operationChannel.close()
        drawableCache.evictAll()
        BlurHashDecoder.clearCache()
        stats.reset()
    }

    /**
     * Configure BlurHash processing
     */
    fun configure(block: BlurHashConfiguration.() -> Unit) {
        BlurHashConfiguration().apply(block)
    }

    /**
     * Get processing statistics
     */
    fun getStats(): BlurHashStats = stats.copy()

    /**
     * Clear all caches
     */
    fun clearCache() {
        drawableCache.evictAll()
        BlurHashDecoder.clearCache()
        stats.recordCacheClear()
    }

    /**
     * Get cache size and usage
     */
    fun getCacheInfo(): CacheInfo {
        return CacheInfo(
            currentSize = drawableCache.size(),
            maxSize = drawableCache.maxSize(),
            hitCount = drawableCache.hitCount(),
            missCount = drawableCache.missCount(),
            evictionCount = drawableCache.evictionCount()
        )
    }

    /**
     * Enable/disable BlurHash processing
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            cancelAllOperations()
        }
    }

    /**
     * Set processing timeout
     */
    fun setProcessingTimeout(timeoutMs: Long) {
        processingTimeout = timeoutMs.coerceAtLeast(1000L)
    }

    /**
     * Update punch value
     */
    fun setPunch(@FloatRange(from = 0.1, to = 10.0) newPunch: Float) {
        punch = newPunch.coerceIn(0.1f, 10.0f)
    }

    // Private methods

    private suspend fun processBlurHashInternal(
        blurString: String,
        width: Int,
        height: Int
    ): BitmapDrawable? {
        val cacheKey = createCacheKey(blurString, width, height)

        // Check if operation is already in progress
        ongoingOperations[cacheKey]?.let { ongoing ->
            return ongoing.await()
        }

        // Start new operation
        val operation = processingScope.async {
            // Acquire processing permit
            operationChannel.receive()

            try {
                stats.recordProcessingStart()
                val startTime = System.currentTimeMillis()

                val bitmap = BlurHashDecoder.decodeAsync(
                    blurHash = blurString,
                    width = width,
                    height = height,
                    punch = punch,
                    useCache = true
                )

                val processingTime = System.currentTimeMillis() - startTime
                stats.recordProcessingTime(processingTime)

                if (bitmap != null) {
                    val drawable = bitmap.toDrawable(context.resources)
                    cacheDrawable(cacheKey, drawable)
                    stats.recordSuccess()
                    drawable
                } else {
                    stats.recordDecodingFailure()
                    null
                }
            } catch (e: Exception) {
                stats.recordError(e)
                null
            } finally {
                // Release processing permit
                operationChannel.trySend(Unit)
                ongoingOperations.remove(cacheKey)
            }
        }

        ongoingOperations[cacheKey] = operation
        return operation.await()
    }

    private fun getCachedDrawable(blurString: String, width: Int, height: Int): BitmapDrawable? {
        val cacheKey = createCacheKey(blurString, width, height)
        return drawableCache.get(cacheKey)?.also {
            stats.recordCacheHit()
        } ?: run {
            stats.recordCacheMiss()
            null
        }
    }

    private fun cacheDrawable(cacheKey: String, drawable: BitmapDrawable) {
        drawableCache.put(cacheKey, drawable)
    }

    private fun createCacheKey(blurString: String, width: Int, height: Int): String {
        return "${blurString}_${width}x${height}_${punch}"
    }

    /**
     * Configuration class for BlurHash
     */
    inner class BlurHashConfiguration {
        fun punch(@FloatRange(from = 0.1, to = 10.0) punch: Float) {
            this@BlurHash.punch = punch
        }

        fun timeout(timeoutMs: Long) {
            processingTimeout = timeoutMs.coerceAtLeast(1000L)
        }

        fun enabled(enabled: Boolean) {
            isEnabled = enabled
        }
    }
}

/**
 * Result sealed class for Flow operations
 */
sealed class BlurHashFlowResult {
    data object Loading : BlurHashFlowResult()
    data object Disabled : BlurHashFlowResult()
    data class Success(val drawable: BitmapDrawable) : BlurHashFlowResult()
    data class Error(val message: String) : BlurHashFlowResult()
}

/**
 * Request data class for batch operations
 */
data class BlurHashRequest(
    val blurHash: String,
    val width: Int,
    val height: Int,
    val id: String? = null
)

/**
 * Statistics tracking class
 */
data class BlurHashStats(
    private val processedCount: AtomicInteger = AtomicInteger(0),
    private val successCount: AtomicInteger = AtomicInteger(0),
    private val errorCount: AtomicInteger = AtomicInteger(0),
    private val cacheHits: AtomicInteger = AtomicInteger(0),
    private val cacheMisses: AtomicInteger = AtomicInteger(0),
    private val timeouts: AtomicInteger = AtomicInteger(0),
    private val totalProcessingTime: AtomicInteger = AtomicInteger(0),
    private val decodingFailures: AtomicInteger = AtomicInteger(0),
    private val cacheClears: AtomicInteger = AtomicInteger(0)
) {

    fun recordProcessingStart() = processedCount.incrementAndGet()
    fun recordSuccess() = successCount.incrementAndGet()
    fun recordError(error: Exception) = errorCount.incrementAndGet()
    fun recordCacheHit() = cacheHits.incrementAndGet()
    fun recordCacheMiss() = cacheMisses.incrementAndGet()
    fun recordTimeout() = timeouts.incrementAndGet()
    fun recordProcessingTime(timeMs: Long) = totalProcessingTime.addAndGet(timeMs.toInt())
    fun recordDecodingFailure() = decodingFailures.incrementAndGet()
    fun recordCacheClear() = cacheClears.incrementAndGet()

    fun reset() {
        processedCount.set(0)
        successCount.set(0)
        errorCount.set(0)
        cacheHits.set(0)
        cacheMisses.set(0)
        timeouts.set(0)
        totalProcessingTime.set(0)
        decodingFailures.set(0)
        cacheClears.set(0)
    }

    fun copy(): BlurHashStats = BlurHashStats(
        AtomicInteger(processedCount.get()),
        AtomicInteger(successCount.get()),
        AtomicInteger(errorCount.get()),
        AtomicInteger(cacheHits.get()),
        AtomicInteger(cacheMisses.get()),
        AtomicInteger(timeouts.get()),
        AtomicInteger(totalProcessingTime.get()),
        AtomicInteger(decodingFailures.get()),
        AtomicInteger(cacheClears.get())
    )

    // Computed properties
    val totalProcessed: Int get() = processedCount.get()
    val totalSuccess: Int get() = successCount.get()
    val totalErrors: Int get() = errorCount.get()
    val totalCacheHits: Int get() = cacheHits.get()
    val totalCacheMisses: Int get() = cacheMisses.get()
    val totalTimeouts: Int get() = timeouts.get()
    val totalProcessingTimeMs: Int get() = totalProcessingTime.get()
    val totalDecodingFailures: Int get() = decodingFailures.get()
    val totalCacheClears: Int get() = cacheClears.get()

    val successRate: Float
        get() = if (totalProcessed > 0) totalSuccess.toFloat() / totalProcessed else 0f

    val cacheHitRate: Float
        get() = if (totalCacheHits + totalCacheMisses > 0) {
            totalCacheHits.toFloat() / (totalCacheHits + totalCacheMisses)
        } else 0f

    val averageProcessingTime: Float
        get() = if (totalSuccess > 0) totalProcessingTimeMs.toFloat() / totalSuccess else 0f

    val isHealthy: Boolean
        get() = successRate > 0.8f && cacheHitRate > 0.5f && totalTimeouts < totalProcessed * 0.1f
}

/**
 * Cache information data class
 */
data class CacheInfo(
    val currentSize: Int,
    val maxSize: Int,
    val hitCount: Int,
    val missCount: Int,
    val evictionCount: Int
) {
    val hitRate: Float
        get() = if (hitCount + missCount > 0) {
            hitCount.toFloat() / (hitCount + missCount)
        } else 0f

    val isFull: Boolean get() = currentSize >= maxSize
    val utilizationRate: Float get() = currentSize.toFloat() / maxSize
}

/**
 * Extension functions for easier usage
 */
fun BlurHash.executeAsync(
    blurString: String,
    width: Int,
    height: Int
): Deferred<BitmapDrawable?> {
    return CoroutineScope(Dispatchers.Default).async {
        executeWithTimeout(blurString, width, height)
    }
}

fun BlurHash.isHealthy(): Boolean = getStats().isHealthy

fun BlurHash.getCacheUtilization(): Float = getCacheInfo().utilizationRate

/**
 * Factory function for creating BlurHash instances
 */
fun createBlurHash(
    context: Context,
    lruSize: Int = BlurHash.DEFAULT_LRU_SIZE,
    punch: Float = BlurHash.DEFAULT_PUNCH
): BlurHash = BlurHash(context, lruSize, punch)

/**
 * Utility functions for BlurHash management
 */
object BlurHashManager {
    private val instances = mutableMapOf<String, BlurHash>()

    fun getInstance(
        context: Context,
        key: String = "default",
        lruSize: Int = BlurHash.DEFAULT_LRU_SIZE,
        punch: Float = BlurHash.DEFAULT_PUNCH
    ): BlurHash {
        return instances.getOrPut(key) {
            BlurHash(context, lruSize, punch)
        }
    }

    fun clearAll() {
        instances.values.forEach { it.cleanup() }
        instances.clear()
    }

    fun getStats(): Map<String, BlurHashStats> {
        return instances.mapValues { it.value.getStats() }
    }
}
