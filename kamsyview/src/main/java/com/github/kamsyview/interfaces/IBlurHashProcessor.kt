package com.github.kamsyview.interfaces

import com.github.kamsyview.processing.BlurHashResult
import com.github.kamsyview.processing.CacheStats
import kotlin.coroutines.cancellation.*
import com.github.kamsyview.models.*
import com.github.kamsyview.impl.*

/**
 * Interface for BlurHash processing operations in KamsyView components.
 *
 * IBlurHashProcessor defines the contract for converting BlurHash strings into displayable
 * bitmap drawables. BlurHash is a compact representation of images that provides meaningful
 * placeholders during image loading, offering better user experience than generic placeholders.
 *
 * ## BlurHash Overview
 * BlurHash encodes images into short strings (20-30 characters) that can be decoded into
 * low-resolution placeholders. These placeholders:
 * - Maintain the overall color scheme and structure of the original image
 * - Load instantly without network requests
 * - Provide smooth transitions when the full image loads
 * - Use minimal bandwidth and storage
 *
 * ## Processing Flow
 * ```
 * BlurHash String → Decode Algorithm → Bitmap → Drawable → Display
 *                     ↓
 *                 Cache Result
 * ```
 *
 * ## Implementation Requirements
 * Implementations must provide:
 * - **Thread Safety**: Multiple concurrent processing operations
 * - **Caching**: Efficient storage and retrieval of decoded results
 * - **Error Handling**: Graceful handling of invalid BlurHash strings
 * - **Resource Management**: Proper cleanup and memory management
 * - **Performance Monitoring**: Metrics and statistics collection
 *
 * ## Usage Examples
 *
 * ### Basic Processing
 * ```kotlin
 * class MyBlurHashProcessor : IBlurHashProcessor {
 *     override suspend fun processBlurHash(
 *         blurHash: String,
 *         width: Int,
 *         height: Int,
 *         punch: Float,
 *         callback: (BlurHashResult) -> Unit
 *     ) {
 *         // Implementation
 *     }
 * }
 * ```
 *
 * ### With Dependency Injection
 * ```kotlin
 * class MyViewModel @Inject constructor(
 *     private val blurHashProcessor: IBlurHashProcessor
 * ) {
 *     fun loadBlurHash(hash: String) {
 *         viewModelScope.launch {
 *             blurHashProcessor.processBlurHash(hash, 50, 50) { result ->
 *                 when (result) {
 *                     is BlurHashResult.Success -> displayBlurHash(result.drawable)
 *                     is BlurHashResult.Error -> handleError(result.error)
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ### Cache Management
 * ```kotlin
 * // Monitor cache performance
 * val stats = processor.getCacheStats()
 * if (stats.hitRate < 0.8) {
 *     // Consider increasing cache size
 * }
 *
 * // Clear cache when memory is low
 * if (isMemoryLow()) {
 *     processor.clearCache()
 * }
 * ```
 *
 * ## Performance Considerations
 * - **Caching Strategy**: Implement LRU cache for frequently accessed BlurHashes
 * - **Thread Pool**: Use dedicated thread pool for CPU-intensive decoding
 * - **Memory Management**: Monitor bitmap memory usage and implement pressure handling
 * - **Batch Processing**: Consider batching requests for efficiency
 *
 * @see BlurHashResult
 * @see CacheStats
 * @see KamsyConfiguration
 * @see InjectableBlurHashProcessor
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
interface IBlurHashProcessor {

    /**
     * Processes a BlurHash string into a displayable drawable asynchronously.
     *
     * This method converts a BlurHash string representation into a bitmap drawable
     * that can be displayed as a placeholder. The processing is performed on a
     * background thread to avoid blocking the UI, with results delivered via callback.
     *
     * ## Processing Steps
     * 1. **Validation**: Verify BlurHash format and parameters
     * 2. **Cache Check**: Look for existing decoded result in cache
     * 3. **Decoding**: Convert BlurHash string to bitmap (if not cached)
     * 4. **Drawable Creation**: Wrap bitmap in appropriate drawable
     * 5. **Caching**: Store result for future use
     * 6. **Callback**: Deliver result via provided callback
     *
     * ## BlurHash Format
     * BlurHash strings follow a specific format:
     * - First character: Number of components (base 83)
     * - Remaining characters: Encoded color components
     * - Typical length: 20-30 characters
     * - Example: "LGFFaXYk^6#M@-5c,1J5@[or[Q6."
     *
     * ## Parameter Guidelines
     *
     * ### Dimensions (width/height)
     * - **Small (10-20px)**: Fast processing, minimal detail
     * - **Medium (20-50px)**: Balanced performance and quality
     * - **Large (50-100px)**: Higher quality, slower processing
     * - **Recommended**: 20-30px for most use cases
     *
     * ### Punch Factor
     * - **0.0-0.8**: Muted, subtle colors
     * - **0.8-1.2**: Normal contrast (recommended range)
     * - **1.2-2.0**: Vibrant, high contrast colors
     * - **Default**: 1.0 for standard appearance
     *
     * ## Error Handling
     * The callback will receive [BlurHashResult.Error] for:
     * - Invalid BlurHash format or characters
     * - Zero or negative dimensions
     * - System out of memory conditions
     * - Processing timeouts or interruptions
     *
     * ## Threading and Lifecycle
     * ```kotlin
     * // Safe to call from any thread
     * lifecycleScope.launch {
     *     processor.processBlurHash("LGFFaXYk^6#M...", 30, 30) { result ->
     *         // Callback executed on main thread
     *         when (result) {
     *             is BlurHashResult.Success -> {
     *                 imageView.setImageDrawable(result.drawable)
     *             }
     *             is BlurHashResult.Error -> {
     *                 showError(result.error.getUserMessage())
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * ## Performance Optimization
     * - Results are automatically cached based on hash + dimensions
     * - Subsequent requests for same parameters return cached results
     * - Processing is throttled based on maxConcurrentJobs configuration
     * - Memory usage is monitored and bounded by cache size limits
     *
     * @param blurHash The BlurHash string to decode (e.g., "LGFFaXYk^6#M@-5c,1J5@[or[Q6.")
     * @param width Target width in pixels for the decoded bitmap (typically 20-50)
     * @param height Target height in pixels for the decoded bitmap (typically 20-50)
     * @param punch Contrast/saturation multiplier (0.0-2.0, default 1.0)
     * @param callback Callback function to receive the processing result
     *
     * @throws IllegalArgumentException if blurHash is empty or dimensions are invalid
     * @throws CancellationException if the coroutine is cancelled during processing
     *
     * @see BlurHashResult
     * @see KamsyConfiguration.blurHashMaxConcurrentJobs
     */
    suspend fun processBlurHash(
        blurHash: String,
        width: Int,
        height: Int,
        punch: Float = 1f,
        callback: (BlurHashResult) -> Unit
    )

    /**
     * Retrieves current cache statistics for performance monitoring and optimization.
     *
     * Cache statistics provide insights into processing performance, memory usage,
     * and cache effectiveness. This information helps optimize cache configuration
     * and identify performance bottlenecks.
     *
     * ## Statistics Included
     * - **Hit Rate**: Percentage of requests served from cache
     * - **Miss Rate**: Percentage of requests requiring new processing
     * - **Total Requests**: Number of processing requests made
     * - **Cache Size**: Current number of cached items
     * - **Memory Usage**: Approximate memory consumed by cache
     *
     * ## Usage Examples
     *
     * ### Performance Monitoring
     * ```kotlin
     * val stats = processor.getCacheStats()
     *
     * // Log performance metrics
     * Log.d("BlurHash", """
     *     Cache Stats:
     *     Hit Rate: ${stats.hitRate}%
     *     Total Requests: ${stats.totalRequests}
     *     Cache Size: ${stats.cacheSize}/${stats.maxCacheSize}
     *     Memory Usage: ${stats.memoryUsage / 1024}KB
     * """.trimIndent())
     *
     * // Monitor cache effectiveness
     * if (stats.hitRate < 0.7) {
     *     // Consider increasing cache size
     *     analytics.track("low_cache_hit_rate", mapOf("rate" to stats.hitRate))
     * }
     * ```
     *
     * ### Automatic Cache Management
     * ```kotlin
     * fun manageCache() {
     *     val stats = processor.getCacheStats()
     *
     *     when {
     *         stats.memoryUsage > maxMemoryThreshold -> {
     *             processor.clearCache()
     *             logger.info("Cache cleared due to memory pressure")
     *         }
     *         stats.hitRate < 0.5 && stats.cacheSize < stats.maxCacheSize -> {
     *             // Cache underutilized, consider increasing size
     *             logger.info("Cache hit rate low, consider tuning")
     *         }
     *     }
     * }
     * ```
     *
     * ### Analytics Integration
     * ```kotlin
     * // Periodic cache reporting
     * fun reportCacheMetrics() {
     *     val stats = processor.getCacheStats()
     *     analytics.track("blurhash_cache_stats", mapOf(
     *         "hit_rate" to stats.hitRate,
     *         "total_requests" to stats.totalRequests,
     *         "cache_utilization" to stats.cacheSize.toFloat() / stats.maxCacheSize
     *     ))
     * }
     * ```
     *
     * @return Current cache statistics snapshot
     *
     * @see CacheStats
     * @see clearCache
     */
    fun getCacheStats(): CacheStats

    /**
     * Clears all cached BlurHash processing results to free memory.
     *
     * This method removes all cached bitmap results from memory, forcing
     * subsequent processing requests to re-decode BlurHash strings. Use this
     * method to manage memory usage during low-memory conditions or when
     * cache invalidation is required.
     *
     * ## When to Clear Cache
     * - **Memory Pressure**: When system is running low on memory
     * - **Cache Corruption**: When cache statistics indicate corruption
     * - **Configuration Changes**: When cache size limits are reduced
     * - **Testing**: When resetting state for testing purposes
     * - **User Action**: When user explicitly requests cache clearing
     *
     * ## Impact of Cache Clearing
     * - **Immediate**: Memory usage reduction
     * - **Short-term**: Slower BlurHash processing until cache rebuilds
     * - **Performance**: Temporary increase in CPU usage for re-processing
     * - **User Experience**: Potential delays in placeholder display
     *
     * ## Usage Examples
     *
     * ### Memory Management
     * ```kotlin
     * class MemoryManager {
     *     fun onLowMemory() {
     *         // Clear non-essential caches
     *         blurHashProcessor.clearCache()
     *         imageCache.clearCache()
     *
     *         // Trigger garbage collection
     *         System.gc()
     *     }
     * }
     * ```
     *
     * ### User-Initiated Clearing
     * ```kotlin
     * // Settings screen
     * fun onClearCacheClicked() {
     *     val statsBefore = processor.getCacheStats()
     *     processor.clearCache()
     *     val statsAfter = processor.getCacheStats()
     *
     *     val freedMemory = statsBefore.memoryUsage - statsAfter.memoryUsage
     *     showToast("Cleared ${freedMemory / 1024}KB from BlurHash cache")
     * }
     * ```
     *
     * ### Automatic Cache Management
     * ```kotlin
     * fun monitorAndManageCache() {
     *     val stats = processor.getCacheStats()
     *
     *     when {
     *         stats.memoryUsage > MEMORY_THRESHOLD -> {
     *             processor.clearCache()
     *             logger.info("Cache cleared: memory usage exceeded threshold")
     *         }
     *         stats.hitRate < 0.3 && stats.cacheSize > 10 -> {
     *             processor.clearCache()
     *             logger.info("Cache cleared: low efficiency detected")
     *         }
     *     }
     * }
     * ```
     *
     * ## Thread Safety
     * This method is thread-safe and can be called from any thread. Cache clearing
     * is performed atomically to ensure no processing operations are left in
     * inconsistent states.
     *
     * @see getCacheStats
     * @see CacheStats
     */
    fun clearCache()

    /**
     * Performs cleanup operations and releases resources used by the processor.
     *
     * This method should be called when the processor is no longer needed,
     * typically during application shutdown, activity destruction, or when
     * switching to a different processor implementation. It ensures proper
     * resource deallocation and prevents memory leaks.
     *
     * ## Cleanup Operations Performed
     * - **Thread Pool Shutdown**: Terminates background processing threads
     * - **Cache Clearing**: Removes all cached bitmap data
     * - **Memory Release**: Releases internal data structures and buffers
     * - **Callback Cleanup**: Cancels pending callbacks and notifications
     * - **Resource Deallocation**: Frees native resources if any
     *
     * ## Lifecycle Integration
     *
     * ### Activity Lifecycle
     * ```kotlin
     * class MainActivity : AppCompatActivity() {
     *     @Inject lateinit var blurHashProcessor: IBlurHashProcessor
     *
     *     override fun onDestroy() {
     *         super.onDestroy()
     *         if (isFinishing) {
     *             blurHashProcessor.cleanup()
     *         }
     *     }
     * }
     * ```
     *
     * ### ViewModel Cleanup
     * ```kotlin
     * class MyViewModel @Inject constructor(
     *     private val blurHashProcessor: IBlurHashProcessor
     * ) : ViewModel() {
     *
     *     override fun onCleared() {
     *         super.onCleared()
     *         blurHashProcessor.cleanup()
     *     }
     * }
     * ```
     *
     * ### Application Lifecycle
     * ```kotlin
     * class MyApplication : Application() {
     *     @Inject lateinit var blurHashProcessor: IBlurHashProcessor
     *
     *     override fun onTerminate() {
     *         super.onTerminate()
     *         blurHashProcessor.cleanup()
     *     }
     * }
     * ```
     *
     * ## Dependency Injection Lifecycle
     * ```kotlin
     * @Module
     * @InstallIn(SingletonComponent::class)
     * class BlurHashModule {
     *
     *     @Provides
     *     @Singleton
     *     fun provideBlurHashProcessor(): IBlurHashProcessor {
     *         return BlurHashProcessor().also { processor ->
     *             // Register cleanup with application lifecycle
     *             ProcessLifecycleOwner.get().lifecycle.addObserver(
     *                 object : DefaultLifecycleObserver {
     *                     override fun onDestroy(owner: LifecycleOwner) {
     *                         processor.cleanup()
     *                     }
     *                 }
     *             )
     *         }
     *     }
     * }
     * ```
     *
     * ## Post-Cleanup Behavior
     * After cleanup is called:
     * - All subsequent processing requests should fail gracefully
     * - Cache operations should return empty/default results
     * - Resource usage should drop to near zero
     * - The processor should not accept new work
     *
     * ## Error Handling
     * ```kotlin
     * fun safeCleanup(processor: IBlurHashProcessor) {
     *     try {
     *         processor.cleanup()
     *     } catch (e: Exception) {
     *         // Log but don't crash - cleanup is best effort
     *         Log.w("BlurHash", "Cleanup failed", e)
     *     }
     * }
     * ```
     *
     * ## Thread Safety
     * This method is thread-safe and idempotent. Multiple calls to cleanup()
     * should not cause errors or unexpected behavior.
     *
     * @see clearCache
     * @see getCacheStats
     */
    fun cleanup()
}
