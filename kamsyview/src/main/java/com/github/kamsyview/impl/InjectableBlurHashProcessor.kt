package com.github.kamsyview.impl

import android.content.Context
import com.github.kamsyview.core.getUserMessage
import com.github.kamsyview.core.toKamsyError
import com.github.kamsyview.di.BlurHashCacheSize
import com.github.kamsyview.di.BlurHashMaxConcurrentJobs
import com.github.kamsyview.di.BlurHashScope
import com.github.kamsyview.interfaces.IBlurHashProcessor
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.interfaces.IKamsyMetrics
import com.github.kamsyview.models.KamsyConfiguration
import com.github.kamsyview.processing.BlurHashProcessor
import com.github.kamsyview.processing.BlurHashResult
import com.github.kamsyview.processing.CacheStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable implementation of [IBlurHashProcessor] with comprehensive dependency injection support.
 *
 * InjectableBlurHashProcessor provides a production-ready BlurHash processing implementation
 * designed for use with Dagger Hilt. It wraps the core BlurHashProcessor with additional
 * functionality including logging, metrics collection, and proper dependency management.
 *
 * ## Dependency Injection Benefits
 * - **Automatic Configuration**: All dependencies injected automatically by Hilt
 * - **Testability**: Easy mocking and testing with dependency injection
 * - **Consistency**: Shared configuration across all KamsyView instances
 * - **Lifecycle Management**: Proper resource management via DI scopes
 * - **Observability**: Built-in logging and metrics collection
 *
 * ## Architecture
 * ```
 * InjectableBlurHashProcessor (Injectable Wrapper)
 *          ↓
 * BlurHashProcessor (Core Implementation)
 *          ↓
 * Native BlurHash Decoder
 * ```
 *
 * ## Hilt Module Setup
 * ```kotlin
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object BlurHashModule {
 *
 *     @Provides
 *     @BlurHashCacheSize
 *     fun provideBlurHashCacheSize(): Int = 50
 *
 *     @Provides
 *     @BlurHashMaxConcurrentJobs
 *     fun provideMaxConcurrentJobs(): Int = 3
 *
 *     @Provides
 *     @BlurHashScope
 *     fun provideBlurHashScope(): CoroutineScope {
 *         return CoroutineScope(
 *             SupervisorJob() +
 *             Dispatchers.Default +
 *             CoroutineName("BlurHashProcessor")
 *         )
 *     }
 *
 *     @Binds
 *     abstract fun bindBlurHashProcessor(
 *         processor: InjectableBlurHashProcessor
 *     ): IBlurHashProcessor
 * }
 * ```
 *
 * ## Configuration Parameters
 * All configuration is provided via constructor injection using qualifier annotations:
 *
 * ### @BlurHashCacheSize
 * Controls the maximum number of BlurHash results cached in memory.
 * Higher values improve performance but use more memory.
 *
 * ### @BlurHashMaxConcurrentJobs
 * Limits the number of simultaneous BlurHash processing operations.
 * Prevents thread pool exhaustion and excessive CPU usage.
 *
 * ### @BlurHashScope
 * Provides the coroutine scope for processing operations.
 * Should use Dispatchers.Default for CPU-intensive work.
 *
 * ## Observability Features
 *
 * ### Comprehensive Logging
 * - Debug logs for all processing operations
 * - Error logs with detailed failure information
 * - Performance timing for optimization
 * - Cache operation logging
 *
 * ### Metrics Collection
 * - Processing duration tracking
 * - Cache hit/miss ratios
 * - Error rate monitoring
 * - Memory usage patterns
 *
 * ## Error Handling Strategy
 * ```
 * Try Processing → Log Start → Measure Time → Process → Handle Result
 *      ↓                                                      ↓
 * Catch Exception → Log Error → Record Metrics → Rethrow Exception
 * ```
 *
 * ## Usage Examples
 *
 * ### In KamsyView
 * ```kotlin
 * @AndroidEntryPoint
 * class HiltKamsyView @JvmOverloads constructor(
 *     context: Context,
 *     attrs: AttributeSet? = null
 * ) : KamsyView(context, attrs) {
 *
 *     @Inject
 *     lateinit var blurHashProcessor: IBlurHashProcessor // InjectableBlurHashProcessor injected
 * }
 * ```
 *
 * ### In ViewModel
 * ```kotlin
 * @HiltViewModel
 * class ImageViewModel @Inject constructor(
 *     private val blurHashProcessor: IBlurHashProcessor
 * ) : ViewModel() {
 *
 *     fun loadBlurHash(hash: String) {
 *         viewModelScope.launch {
 *             blurHashProcessor.processBlurHash(hash, 30, 30) { result ->
 *                 // Handle result with full logging and metrics
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ### Testing Support
 * ```kotlin
 * @HiltAndroidTest
 * class BlurHashTest {
 *
 *     @BindValue
 *     @JvmField
 *     val mockProcessor: IBlurHashProcessor = mockk()
 *
 *     @Test
 *     fun testBlurHashProcessing() {
 *         // Test with mocked processor
 *     }
 * }
 * ```
 *
 * ## Performance Characteristics
 * - **Processing Time**: Logged and tracked via metrics
 * - **Memory Usage**: Bounded by cache size configuration
 * - **Thread Usage**: Limited by maxConcurrentJobs parameter
 * - **Cache Efficiency**: Monitored via hit/miss ratios
 *
 * @param context Application context for Android-specific operations
 * @param cacheSize Maximum number of BlurHash results to cache
 * @param maxConcurrentJobs Maximum number of simultaneous processing operations
 * @param processingScope Coroutine scope for background processing operations
 * @param logger Logger instance for debug and error logging
 * @param metrics Metrics collector for performance monitoring
 *
 * @see IBlurHashProcessor
 * @see BlurHashProcessor
 * @see KamsyConfiguration
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
@Singleton
class InjectableBlurHashProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:BlurHashCacheSize private val cacheSize: Int,
    @param:BlurHashMaxConcurrentJobs private val maxConcurrentJobs: Int,
    @param:BlurHashScope private val processingScope: CoroutineScope,
    private val logger: IKamsyLogger,
    private val metrics: IKamsyMetrics
) : IBlurHashProcessor {

    /**
     * Core BlurHash processor implementation that handles the actual decoding operations.
     *
     * This processor is configured with the injected parameters and provides the
     * low-level BlurHash processing capabilities. The injectable wrapper adds
     * logging, metrics, and error handling on top of this core functionality.
     */
    private val processor = BlurHashProcessor(
        context = context,
        cacheSize = cacheSize,
        maxConcurrentJobs = maxConcurrentJobs
    )

    /**
     * Processes a BlurHash string with comprehensive logging and metrics collection.
     *
     * This implementation wraps the core processing with observability features:
     * - Debug logging of processing start with parameters
     * - Processing duration measurement and recording
     * - Success/failure metrics collection
     * - Error logging with detailed context
     * - Exception handling and rethrowing
     *
     * ## Logging Output Examples
     * ```
     * DEBUG: Processing BlurHash: LGFFaXYk^6#M@-5c,1J5@[or[Q6. (30x30)
     * DEBUG: BlurHash processed successfully in 45ms
     * ERROR: BlurHash processing failed: Invalid BlurHash format
     * ```
     *
     * ## Metrics Collected
     * - `recordBlurHashProcessingTime(duration)`: Processing duration in milliseconds
     * - `recordCacheHit()`: Successful cache retrievals
     * - `recordError(error)`: Processing failures and exceptions
     *
     * @param blurHash The BlurHash string to process
     * @param width Target bitmap width in pixels
     * @param height Target bitmap height in pixels
     * @param punch Contrast/saturation multiplier
     * @param callback Function to receive the processing result
     *
     * @throws Exception Any exception from the underlying processor is logged and rethrown
     *
     * @see IBlurHashProcessor.processBlurHash
     */
    override suspend fun processBlurHash(
        blurHash: String,
        width: Int,
        height: Int,
        punch: Float,
        callback: (BlurHashResult) -> Unit
    ) {
        logger.debug("Processing BlurHash: $blurHash (${width}x${height})")
        val startTime = System.currentTimeMillis()

        try {
            processor.processBlurHash(blurHash, width, height, punch) { result ->
                val duration = System.currentTimeMillis() - startTime
                metrics.recordBlurHashProcessingTime(duration)

                when (result) {
                    is BlurHashResult.Success -> {
                        metrics.recordCacheHit()
                        logger.debug("BlurHash processed successfully in ${duration}ms")
                    }

                    is BlurHashResult.Error -> {
                        metrics.recordError(result.error)
                        logger.error("BlurHash processing failed: ${result.error.getUserMessage()}")
                    }
                }

                callback(result)
            }
        } catch (e: Exception) {
            logger.error("BlurHash processing exception", e)
            metrics.recordError(e.toKamsyError())
            throw e
        }
    }

    /**
     * Retrieves cache statistics from the underlying processor.
     *
     * Delegates to the core processor's cache statistics without additional processing.
     * The statistics reflect the current state of the cache managed by the core processor.
     *
     * @return Current cache statistics
     * @see IBlurHashProcessor.getCacheStats
     */
    override fun getCacheStats(): CacheStats = processor.getCacheStats()

    /**
     * Clears the cache with debug logging.
     *
     * Logs the cache clearing operation for debugging and troubleshooting purposes.
     * This helps track when and why cache clearing occurs in production environments.
     *
     * @see IBlurHashProcessor.clearCache
     */
    override fun clearCache() {
        logger.debug("Clearing BlurHash cache")
        processor.clearCache()
    }

    /**
     * Performs cleanup with debug logging.
     *
     * Logs the cleanup operation to track processor lifecycle and resource management.
     * This is particularly useful for debugging memory leaks and resource cleanup issues.
     *
     * @see IBlurHashProcessor.cleanup
     */
    override fun cleanup() {
        logger.debug("Cleaning up BlurHash processor")
        processor.cleanup()
    }
}
