package com.github.kamsyview.processing

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.core.graphics.drawable.toDrawable
import com.github.kamsyview.core.KamsyError
import com.github.kamsyview.models.KamsyConfiguration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Core BlurHash processing implementation with async support, caching, and lifecycle management.
 *
 * BlurHashProcessor provides a high-performance, production-ready implementation for converting
 * BlurHash strings into displayable bitmap drawables. It integrates with the existing BlurHashDecoder
 * while adding modern coroutines support, intelligent caching, concurrency control, and comprehensive
 * error handling.
 *
 * ## Key Features
 * - **Asynchronous Processing**: Coroutine-based with configurable concurrency limits
 * - **Intelligent Caching**: LRU cache with automatic eviction and statistics
 * - **Reactive Support**: Flow-based API for reactive programming patterns
 * - **Concurrency Control**: Channel-based limiting to prevent resource exhaustion
 * - **Lifecycle Management**: Proper cleanup and resource management
 * - **Comprehensive Monitoring**: Built-in statistics and performance tracking
 *
 * ## Basic Usage
 * ```kotlin
 * val processor = BlurHashProcessor(context, cacheSize = 100, maxConcurrentJobs = 4)
 *
 * processor.processBlurHash("LGFFaXYk^6#M@-5c,1J5@[or[Q6.", 30, 30) { result ->
 *     when (result) {
 *         is BlurHashResult.Success -> imageView.setImageDrawable(result.drawable)
 *         is BlurHashResult.Error -> showError(result.error.getUserMessage())
 *     }
 * }
 * ```
 *
 * @param context Android context for drawable creation and resource access
 * @param cacheSize Maximum number of processed BlurHash results to cache (default: 50)
 * @param maxConcurrentJobs Maximum number of simultaneous processing operations (default: 3)
 *
 * @see BlurHashResult
 * @see CacheStats
 * @see createBlurHashProcessor
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
class BlurHashProcessor(
    private val context: Context,
    private val cacheSize: Int = DEFAULT_CACHE_SIZE,
    private val maxConcurrentJobs: Int = DEFAULT_MAX_CONCURRENT_JOBS
) {

    companion object {
        /** Default cache size balancing memory usage and performance */
        private const val DEFAULT_CACHE_SIZE = 50

        /** Default concurrency limit suitable for most devices */
        private const val DEFAULT_MAX_CONCURRENT_JOBS = 3

        /** Minimum allowed dimension to prevent invalid processing */
        private const val MIN_DIMENSION = 1

        /** Maximum allowed dimension to prevent excessive memory usage */
        private const val MAX_DIMENSION = 1000
    }

    /**
     * LRU cache for storing processed BlurHash drawables.
     *
     * Cache key format: "blurHash_widthxheight_punch"
     */
    private val drawableCache = LruCache<String, BitmapDrawable>(cacheSize)

    /**
     * Dedicated coroutine scope for BlurHash processing operations.
     */
    private val processingScope = CoroutineScope(
        SupervisorJob() +
                Dispatchers.Default +
                CoroutineName("BlurHashProcessor")
    )

    /**
     * Channel-based semaphore to limit concurrent processing operations.
     */
    private val processingChannel = Channel<Unit>(capacity = maxConcurrentJobs)

    /**
     * Tracks active processing jobs to prevent duplicate work and enable cancellation.
     */
    private val activeJobs = ConcurrentHashMap<String, Job>()

    // Performance monitoring statistics
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    private val processingErrors = AtomicInteger(0)

    init {
        // Initialize processing permits by filling the channel
        repeat(maxConcurrentJobs) {
            processingChannel.trySend(Unit)
        }
    }

    /**
     * Processes a BlurHash string asynchronously and delivers the result via callback.
     *
     * This method provides the primary interface for BlurHash processing with the following workflow:
     * 1. **Input Validation**: Validates BlurHash format and parameters
     * 2. **Cache Check**: Looks for existing processed result in cache
     * 3. **Duplicate Prevention**: Cancels any existing processing for the same parameters
     * 4. **Async Processing**: Processes BlurHash on background thread with concurrency control
     * 5. **Result Delivery**: Calls callback with success or error result
     *
     * ## Error Handling
     * All errors are caught and converted to appropriate KamsyError types:
     * - Invalid BlurHash format → KamsyError.BlurHash.InvalidHash
     * - Invalid dimensions → KamsyError.BlurHash.InvalidDimensions
     * - Processing failures → KamsyError.BlurHash.DecodingFailed
     *
     * @param blurHash The BlurHash string to decode (6+ characters, valid BlurHash format)
     * @param width Target bitmap width in pixels (1-1000)
     * @param height Target bitmap height in pixels (1-1000)
     * @param punch Contrast/saturation multiplier (0.0-10.0, default: 1.0)
     * @param callback Function called with processing result (Success or Error)
     *
     * @see BlurHashResult
     * @see validateBlurHashInput
     * @see getCachedDrawable
     */
    suspend fun processBlurHash(
        blurHash: String,
        width: Int,
        height: Int,
        punch: Float = 1f,
        callback: (BlurHashResult) -> Unit
    ) {
        // Validate inputs
        validateBlurHashInput(blurHash, width, height, punch)?.let { error ->
            callback(BlurHashResult.Error(error))
            return
        }

        val cacheKey = createCacheKey(blurHash, width, height, punch)

        // Check cache first
        getCachedDrawable(cacheKey)?.let { drawable ->
            callback(BlurHashResult.Success(drawable))
            return
        }

        // Cancel any existing job for this hash
        activeJobs[cacheKey]?.cancel()

        // Start new processing job
        val job = processingScope.launch {
            try {
                val result = processBlurHashInternal(blurHash, width, height, punch, cacheKey)
                callback(result)
            } catch (e: CancellationException) {
                // Job was cancelled, don't call callback
                throw e
            } catch (e: Exception) {
                processingErrors.incrementAndGet()
                callback(BlurHashResult.Error(e.toKamsyError()))
            } finally {
                activeJobs.remove(cacheKey)
            }
        }

        activeJobs[cacheKey] = job
    }

    /**
     * Processes a BlurHash string and returns a Flow for reactive programming patterns.
     *
     * This method provides a reactive interface that integrates well with modern Android
     * architectures using Flow, StateFlow, and reactive UI patterns.
     *
     * ## Usage Example
     * ```kotlin
     * processor.processBlurHashFlow(blurHash, 30, 30)
     *     .collect { result ->
     *         when (result) {
     *             is BlurHashResult.Success -> updateUI(result.drawable)
     *             is BlurHashResult.Error -> showError(result.error)
     *         }
     *     }
     * ```
     *
     * @param blurHash The BlurHash string to decode
     * @param width Target bitmap width in pixels
     * @param height Target bitmap height in pixels
     * @param punch Contrast/saturation multiplier (default: 1.0)
     * @return Flow that emits exactly one BlurHashResult and completes
     *
     * @see processBlurHash
     * @see BlurHashResult
     */
    fun processBlurHashFlow(
        blurHash: String,
        width: Int,
        height: Int,
        punch: Float = 1f
    ): Flow<BlurHashResult> = flow {
        // Validate inputs
        validateBlurHashInput(blurHash, width, height, punch)?.let { error ->
            emit(BlurHashResult.Error(error))
            return@flow
        }

        val cacheKey = createCacheKey(blurHash, width, height, punch)

        // Check cache first
        getCachedDrawable(cacheKey)?.let { drawable ->
            emit(BlurHashResult.Success(drawable))
            return@flow
        }

        // Process and emit result
        try {
            val result = processBlurHashInternal(blurHash, width, height, punch, cacheKey)
            emit(result)
        } catch (e: Exception) {
            processingErrors.incrementAndGet()
            emit(BlurHashResult.Error(e.toKamsyError()))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Cancels processing for a specific BlurHash with the given parameters.
     *
     * @param blurHash The BlurHash string of the processing to cancel
     * @param width The width parameter of the processing to cancel
     * @param height The height parameter of the processing to cancel
     * @param punch The punch parameter of the processing to cancel (default: 1.0)
     *
     * @see cancelAllProcessing
     */
    fun cancelProcessing(blurHash: String, width: Int, height: Int, punch: Float = 1f) {
        val cacheKey = createCacheKey(blurHash, width, height, punch)
        activeJobs[cacheKey]?.cancel()
        activeJobs.remove(cacheKey)
    }

    /**
     * Cancels all active processing jobs immediately.
     *
     * ## Use Cases
     * - **Activity/Fragment Lifecycle**: Cancel all processing when component is destroyed
     * - **Memory Pressure**: Free up processing resources during low memory conditions
     * - **App Background**: Cancel processing when app goes to background
     *
     * @see cancelProcessing
     * @see cleanup
     */
    fun cancelAllProcessing() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }

    /**
     * Retrieves a cached drawable if available for the given BlurHash parameters.
     *
     * @param blurHash The BlurHash string to look up
     * @param width The width parameter used for processing
     * @param height The height parameter used for processing
     * @param punch The punch parameter used for processing (default: 1.0)
     * @return Cached BitmapDrawable if available, null if not cached
     *
     * @see processBlurHash
     * @see getCacheStats
     */
    fun getCachedDrawable(blurHash: String, width: Int, height: Int, punch: Float = 1f): BitmapDrawable? {
        val cacheKey = createCacheKey(blurHash, width, height, punch)
        return getCachedDrawable(cacheKey)
    }

    /**
     * Clears all cached BlurHash processing results and underlying decoder cache.
     *
     * @see getCacheStats
     * @see cleanup
     */
    fun clearCache() {
        drawableCache.evictAll()
        BlurHashDecoder.clearCache()
    }

    /**
     * Retrieves comprehensive statistics about cache performance and processor state.
     *
     * @return CacheStats object containing comprehensive performance and state information
     *
     * @see CacheStats
     */
    fun getCacheStats(): CacheStats = CacheStats(
        size = drawableCache.size(),
        maxSize = drawableCache.maxSize(),
        hitCount = cacheHits.get(),
        missCount = cacheMisses.get(),
        errorCount = processingErrors.get(),
        activeJobs = activeJobs.size
    )

    /**
     * Performs comprehensive cleanup of all processor resources and state.
     *
     * This method should be called when the processor is no longer needed to ensure
     * proper resource deallocation and prevent memory leaks.
     *
     * @see cancelAllProcessing
     * @see clearCache
     */
    fun cleanup() {
        cancelAllProcessing()
        processingScope.cancel()
        processingChannel.close()
        clearCache()
    }

    /**
     * Internal method that performs the actual BlurHash processing with concurrency control.
     *
     * This method handles the core processing logic including:
     * - Acquiring processing permits from the channel
     * - Delegating to BlurHashDecoder for actual decoding
     * - Converting bitmap to BitmapDrawable
     * - Caching successful results
     * - Releasing processing permits
     *
     * ## Processing Flow
     * 1. **Acquire Permit**: Get permission to process (blocks if at limit)
     * 2. **Decode BlurHash**: Use BlurHashDecoder to convert string to bitmap
     * 3. **Create Drawable**: Wrap bitmap in BitmapDrawable with proper resources
     * 4. **Cache Result**: Store successful result for future use
     * 5. **Release Permit**: Return processing permission to pool
     *
     * ## Concurrency Control
     * The method uses a channel-based semaphore to limit concurrent processing:
     * - `processingChannel.receive()` blocks until a permit is available
     * - `processingChannel.trySend(Unit)` returns the permit in finally block
     * - This prevents resource exhaustion from too many simultaneous operations
     *
     * ## Error Handling
     * - Null bitmap from decoder → BlurHashResult.Error(DecodingFailed)
     * - Any exception → Propagated to caller for handling
     * - Permits are always released in finally block
     *
     * @param blurHash The validated BlurHash string
     * @param width The validated width parameter
     * @param height The validated height parameter
     * @param punch The validated punch parameter
     * @param cacheKey The cache key for storing/retrieving results
     * @return BlurHashResult containing success or error information
     *
     * @see BlurHashDecoder.decode
     * @see cacheDrawable
     */
    private suspend fun processBlurHashInternal(
        blurHash: String,
        width: Int,
        height: Int,
        punch: Float,
        cacheKey: String
    ): BlurHashResult {
        // Acquire processing permit
        processingChannel.receive()

        return try {
            withContext(Dispatchers.Default) {
                // Use existing BlurHashDecoder
                val bitmap = BlurHashDecoder.decode(
                    blurHash = blurHash,
                    width = width,
                    height = height,
                    punch = punch,
                    useCache = true
                )

                if (bitmap != null) {
                    val drawable = bitmap.toDrawable(context.resources)
                    cacheDrawable(cacheKey, drawable)
                    BlurHashResult.Success(drawable)
                } else {
                    BlurHashResult.Error(KamsyError.BlurHash.DecodingFailed)
                }
            }
        } finally {
            // Release processing permit
            processingChannel.trySend(Unit)
        }
    }

    /**
     * Validates BlurHash input parameters and returns appropriate error if invalid.
     *
     * Performs comprehensive validation of all input parameters to ensure they meet
     * the requirements for successful BlurHash processing. This prevents invalid
     * operations and provides clear error messages for debugging.
     *
     * ## Validation Rules
     *
     * ### BlurHash String
     * - Must not be blank or empty
     * - Must be at least 6 characters long (minimum valid BlurHash)
     * - Characters are validated by BlurHashDecoder during actual processing
     *
     * ### Dimensions
     * - Width must be between MIN_DIMENSION (1) and MAX_DIMENSION (1000)
     * - Height must be between MIN_DIMENSION (1) and MAX_DIMENSION (1000)
     * - Prevents zero-dimension bitmaps and excessive memory usage
     *
     * ### Punch Value
     * - Must be between 0.0 and 10.0
     * - Values outside this range produce unpredictable results
     * - Typical useful range is 0.0-2.0
     *
     * ## Error Mapping
     * - Invalid BlurHash → KamsyError.BlurHash.InvalidHash
     * - Invalid dimensions → KamsyError.BlurHash.InvalidDimensions
     * - Invalid punch → KamsyError.BlurHash.ProcessingError
     *
     * @param blurHash The BlurHash string to validate
     * @param width The width parameter to validate
     * @param height The height parameter to validate
     * @param punch The punch parameter to validate
     * @return KamsyError if validation fails, null if all parameters are valid
     *
     * @see MIN_DIMENSION
     * @see MAX_DIMENSION
     * @see KamsyError.BlurHash
     */
    private fun validateBlurHashInput(
        blurHash: String,
        width: Int,
        height: Int,
        punch: Float
    ): KamsyError? = when {
        blurHash.isBlank() -> KamsyError.BlurHash.InvalidHash
        blurHash.length < 6 -> KamsyError.BlurHash.InvalidHash
        width < MIN_DIMENSION || width > MAX_DIMENSION -> KamsyError.BlurHash.InvalidDimensions
        height < MIN_DIMENSION || height > MAX_DIMENSION -> KamsyError.BlurHash.InvalidDimensions
        punch < 0f || punch > 10f -> KamsyError.BlurHash.ProcessingError("Invalid punch value: $punch")
        else -> null
    }

    /**
     * Creates a unique cache key from BlurHash parameters.
     *
     * The cache key format ensures that different parameter combinations
     * don't collide while remaining readable for debugging purposes.
     *
     * ## Key Format
     * Pattern: `"blurHash_widthxheight_punch"`
     *
     * Examples:
     * - `"LGFFaXYk^6#M@-5c,1J5@[or[Q6._30x30_1.0"`
     * - `"L6PZfSi_.AyE_3t7t7R**0o#DgR4_2.8?bIU%M;0?fQ._40x40_1.2"`
     *
     * ## Design Considerations
     * - **Uniqueness**: Different parameter combinations produce different keys
     * - **Readability**: Human-readable format for debugging
     * - **Collision-Free**: Parameter order and separators prevent collisions
     * - **Performance**: Simple string concatenation for speed
     *
     * @param blurHash The BlurHash string
     * @param width The width parameter
     * @param height The height parameter
     * @param punch The punch parameter
     * @return Unique cache key string
     *
     * @see getCachedDrawable
     * @see cacheDrawable
     */
    private fun createCacheKey(
        blurHash: String,
        width: Int,
        height: Int,
        punch: Float
    ): String = "${blurHash}_${width}x${height}_${punch}"

    /**
     * Retrieves a cached drawable by cache key and updates statistics.
     *
     * This method handles both cache retrieval and statistics tracking in a single
     * operation, ensuring that cache performance metrics are always accurate.
     *
     * ## Statistics Impact
     * - **Cache Hit**: Increments `cacheHits` counter and returns drawable
     * - **Cache Miss**: Increments `cacheMisses` counter and returns null
     * - **Hit Rate**: Automatically calculated from hit/miss counters
     *
     * ## LRU Cache Behavior
     * When a cache hit occurs:
     * - The accessed item is moved to the "most recently used" position
     * - This protects it from eviction longer than unused items
     * - Cache ordering is automatically maintained by Android's LruCache
     *
     * ## Usage Pattern
     * ```kotlin
     * val cached = getCachedDrawable(cacheKey)
     * if (cached != null) {
     *     // Cache hit - return immediately
     *     return BlurHashResult.Success(cached)
     * } else {
     *     // Cache miss - proceed with processing
     *     // ... processing logic
     * }
     * ```
     *
     * @param cacheKey The cache key to look up
     * @return Cached BitmapDrawable if found, null otherwise
     *
     * @see cacheDrawable
     * @see getCacheStats
     * @see LruCache.get
     */
    private fun getCachedDrawable(cacheKey: String): BitmapDrawable? {
        return drawableCache.get(cacheKey)?.also {
            cacheHits.incrementAndGet()
        } ?: run {
            cacheMisses.incrementAndGet()
            null
        }
    }

    /**
     * Stores a drawable in the cache with the specified key.
     *
     * Uses Android's LruCache for automatic memory management with least-recently-used
     * eviction policy. When the cache reaches its maximum size, the least recently
     * accessed items are automatically removed to make room for new entries.
     *
     * ## LRU Cache Behavior
     * - **Automatic Eviction**: Removes oldest unused items when cache is full
     * - **Memory Management**: Bounds total memory usage to configured limit
     * - **Access Ordering**: Recently accessed items are protected from eviction
     * - **Thread Safety**: LruCache operations are synchronized internally
     *
     * ## Cache Strategy
     * - **Key Format**: Uses consistent key format from `createCacheKey()`
     * - **Duplicate Keys**: Storing same key replaces previous value
     * - **Null Values**: LruCache doesn't store null values (ignored)
     * - **Size Calculation**: Each entry counts as 1 toward cache size limit
     *
     * ## Performance Impact
     * - **Memory Usage**: Each cached drawable uses bitmap memory
     * - **Access Speed**: Cached items return immediately (no processing)
     * - **Eviction Cost**: Minimal overhead for LRU maintenance
     *
     * @param cacheKey The cache key to store under
     * @param drawable The BitmapDrawable to cache
     *
     * @see getCachedDrawable
     * @see createCacheKey
     * @see LruCache.put
     */
    private fun cacheDrawable(cacheKey: String, drawable: BitmapDrawable) {
        drawableCache.put(cacheKey, drawable)
    }
}

/**
 * Sealed class representing the result of BlurHash processing operations.
 *
 * BlurHashResult provides a type-safe way to handle both successful and failed
 * BlurHash processing operations. This follows the Result pattern commonly used
 * in Kotlin for error handling without exceptions.
 *
 * ## Usage Patterns
 *
 * ### Pattern Matching
 * ```kotlin
 * when (result) {
 *     is BlurHashResult.Success -> {
 *         imageView.setImageDrawable(result.drawable)
 *         analytics.track("blurhash_success")
 *     }
 *     is BlurHashResult.Error -> {
 *         showError(result.error.getUserMessage())
 *         analytics.track("blurhash_error", mapOf("error" to result.error))
 *     }
 * }
 * ```
 *
 * ### Functional Approach
 * ```kotlin
 * val uiUpdate = result.fold(
 *     onSuccess = { drawable -> "Image loaded successfully" },
 *     onError = { error -> "Failed to load: ${error.getUserMessage()}" }
 * )
 * ```
 *
 * @see BlurHashProcessor.processBlurHash
 * @see BlurHashProcessor.processBlurHashFlow
 * @see KamsyError
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
sealed class BlurHashResult {
    /**
     * Successful BlurHash processing result containing the processed drawable.
     *
     * @param drawable The BitmapDrawable created from the BlurHash processing
     */
    data class Success(val drawable: BitmapDrawable) : BlurHashResult()

    /**
     * Failed BlurHash processing result containing error information.
     *
     * @param error The KamsyError describing what went wrong during processing
     */
    data class Error(val error: KamsyError) : BlurHashResult()
}

/**
 * Comprehensive statistics about BlurHash processor cache performance and operational state.
 *
 * CacheStats provides detailed metrics for monitoring processor performance, optimizing
 * configuration, and diagnosing issues. All statistics are calculated from the processor's
 * internal counters and state.
 *
 * ## Key Metrics
 * - **Hit Rate**: Percentage of requests served from cache (higher is better)
 * - **Utilization**: How full the cache is (size/maxSize)
 * - **Health Status**: Overall processor health assessment
 *
 * ## Usage Example
 * ```kotlin
 * val stats = processor.getCacheStats()
 *
 * Log.d("BlurHashStats", """
 *     Cache Utilization: ${stats.size}/${stats.maxSize} (${stats.size * 100 / stats.maxSize}%)
 *     Hit Rate: ${String.format("%.1f%%", stats.hitRate * 100)}
 *     Total Requests: ${stats.hitCount + stats.missCount}
 *     Error Count: ${stats.errorCount}
 *     Active Jobs: ${stats.activeJobs}
 *     Health Status: ${if (stats.isHealthy) "Healthy" else "Needs Attention"}
 * """.trimIndent())
 * ```
 *
 * @param size Current number of items stored in cache
 * @param maxSize Maximum capacity of the cache
 * @param hitCount Total number of cache hits since processor creation
 * @param missCount Total number of cache misses since processor creation
 * @param errorCount Total number of processing errors encountered
 * @param activeJobs Current number of active processing operations
 *
 * @see BlurHashProcessor.getCacheStats
 * @see hitRate
 * @see isHealthy
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val hitCount: Int,
    val missCount: Int,
    val errorCount: Int,
    val activeJobs: Int
) {
    /**
     * Cache hit rate as a percentage (0.0-1.0).
     *
     * Calculated as: hitCount / (hitCount + missCount)
     *
     * - **0.0**: No cache hits (all requests required processing)
     * - **0.5**: Half of requests served from cache
     * - **1.0**: All requests served from cache (perfect hit rate)
     *
     * Higher values indicate better cache performance and lower processing overhead.
     */
    val hitRate: Float
        get() = if (hitCount + missCount > 0) {
            hitCount.toFloat() / (hitCount + missCount)
        } else 0f

    /**
     * Indicates whether the processor is operating in a healthy state.
     *
     * Health is determined by:
     * - **Hit Rate**: Should be > 0.5 for good performance
     * - **Active Jobs**: Should be < maxSize/2 to avoid overload
     *
     * Use this property for automated health monitoring and alerting.
     */
    val isHealthy: Boolean
        get() = hitRate > 0.5f && activeJobs < maxSize / 2
}

/**
 * Checks if the result represents a successful processing operation.
 *
 * @return true if the result is BlurHashResult.Success
 */
fun BlurHashResult.isSuccess(): Boolean = this is BlurHashResult.Success

/**
 * Checks if the result represents a failed processing operation.
 *
 * @return true if the result is BlurHashResult.Error
 */
fun BlurHashResult.isError(): Boolean = this is BlurHashResult.Error

/**
 * Extracts the drawable from successful results.
 *
 * @return BitmapDrawable if result is Success, null if result is Error
 */
fun BlurHashResult.getDrawable(): BitmapDrawable? = when (this) {
    is BlurHashResult.Success -> drawable
    is BlurHashResult.Error -> null
}

/**
 * Extracts the error from failed results.
 *
 * @return KamsyError if result is Error, null if result is Success
 */
fun BlurHashResult.getError(): KamsyError? = when (this) {
    is BlurHashResult.Success -> null
    is BlurHashResult.Error -> error
}

/**
 * Functional approach to handle both success and error cases.
 *
 * Similar to Kotlin's Result.fold(), this method provides a way to handle
 * both success and error cases in a single expression.
 *
 * ## Usage Example
 * ```kotlin
 * val message = result.fold(
 *     onSuccess = { drawable -> "Successfully loaded BlurHash" },
 *     onError = { error -> "Failed to load: ${error.getUserMessage()}" }
 * )
 * ```
 *
 * @param T The return type for both callback functions
 * @param onSuccess Callback function for successful results
 * @param onError Callback function for error results
 * @return The result of the appropriate callback function
 */
inline fun <T> BlurHashResult.fold(
    onSuccess: (BitmapDrawable) -> T,
    onError: (KamsyError) -> T
): T = when (this) {
    is BlurHashResult.Success -> onSuccess(drawable)
    is BlurHashResult.Error -> onError(error)
}

/**
 * Converts exceptions to appropriate KamsyError types for consistent error handling.
 *
 * This extension function maps common exceptions to specific KamsyError types,
 * enabling structured error handling throughout the BlurHash processing pipeline.
 *
 * ## Exception Mapping
 * - [OutOfMemoryError] → [KamsyError.General.OutOfMemory]
 * - [IllegalArgumentException] → [KamsyError.BlurHash.InvalidHash]
 * - [CancellationException] → Rethrown (preserves coroutine cancellation)
 * - All other exceptions → [KamsyError.BlurHash.ProcessingError]
 *
 * @return Appropriate KamsyError for the exception type
 * @throws CancellationException if the original exception is CancellationException
 */
private fun Exception.toKamsyError(): KamsyError = when (this) {
    is OutOfMemoryError -> KamsyError.General.OutOfMemory
    is IllegalArgumentException -> KamsyError.BlurHash.InvalidHash
    is CancellationException -> throw this // Re-throw cancellation
    else -> KamsyError.BlurHash.ProcessingError(
        message ?: "Unknown processing error"
    )
}

/**
 * Factory function for creating BlurHashProcessor instances with custom configuration.
 *
 * This factory function provides a convenient way to create BlurHashProcessor instances
 * with explicit configuration parameters, making it easier to test different configurations
 * and integrate with dependency injection frameworks.
 *
 * ## Configuration Guidelines
 *
 * ### Cache Size Selection
 * - **Small apps**: 20-50 items
 * - **Medium apps**: 50-100 items
 * - **Large apps**: 100-200 items
 * - **Memory-constrained**: 10-30 items
 *
 * ### Concurrency Selection
 * - **Low-end devices**: 1-2 jobs
 * - **Mid-range devices**: 2-3 jobs
 * - **High-end devices**: 3-6 jobs
 * - **Background processing**: 1 job
 *
 * ## Usage Examples
 *
 * ### Default Configuration
 * ```kotlin
 * val processor = createBlurHashProcessor(context)
 * ```
 *
 * ### Custom Configuration
 * ```kotlin
 * val processor = createBlurHashProcessor(
 *     context = applicationContext,
 *     cacheSize = 100,          // Large cache for better performance
 *     maxConcurrentJobs = 4     // Higher concurrency for powerful device
 * )
 * ```
 *
 * @param context Android context for drawable creation and resource access
 * @param cacheSize Maximum number of processed results to cache (default: 50)
 * @param maxConcurrentJobs Maximum number of simultaneous processing operations (default: 3)
 * @return Configured BlurHashProcessor instance
 *
 * @see BlurHashProcessor
 * @see KamsyConfiguration
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
fun createBlurHashProcessor(
    context: Context,
    cacheSize: Int = 50,
    maxConcurrentJobs: Int = 3
): BlurHashProcessor = BlurHashProcessor(
    context = context,
    cacheSize = cacheSize,
    maxConcurrentJobs = maxConcurrentJobs
)
