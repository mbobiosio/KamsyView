package com.github.kamsyview.models

import android.view.View
import com.github.kamsyview.core.AnimationConfiguration
import com.github.kamsyview.core.HiltKamsyView
import com.github.kamsyview.core.KamsyView
import com.github.kamsyview.interfaces.IBlurHashProcessor
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.interfaces.IKamsyMetrics

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */

/**
 * Global configuration class that defines behavior and performance settings for KamsyView components.
 *
 * KamsyConfiguration provides centralized control over various aspects of KamsyView functionality
 * including BlurHash processing, logging, metrics collection, animations, and performance optimizations.
 * This configuration is typically provided through dependency injection to ensure consistent
 * behavior across all KamsyView instances in an application.
 *
 * ## Configuration Categories
 *
 * ### BlurHash Processing
 * Controls the performance and resource usage of BlurHash image placeholder processing:
 * - Cache size for decoded BlurHash images
 * - Maximum concurrent processing jobs
 *
 * ### Debugging and Monitoring
 * Controls development and production monitoring features:
 * - Debug logging for development and troubleshooting
 * - Metrics collection for performance monitoring
 *
 * ### Performance and Animations
 * Controls visual effects and hardware optimization:
 * - Default animation durations for consistent timing
 * - Hardware acceleration for improved rendering performance
 *
 * ## Usage Examples
 *
 * ### Development Configuration
 * ```kotlin
 * val devConfig = KamsyConfiguration(
 *     enableLogging = true,           // Enable debug logging
 *     enableMetrics = true,           // Enable metrics collection
 *     blurHashCacheSize = 100,        // Larger cache for development
 *     enableHardwareAcceleration = true
 * )
 * ```
 *
 * ### Production Configuration
 * ```kotlin
 * val prodConfig = KamsyConfiguration(
 *     enableLogging = false,          // Disable debug logging
 *     enableMetrics = true,           // Enable production metrics
 *     blurHashCacheSize = 30,         // Smaller cache for memory efficiency
 *     defaultAnimationDuration = 800L // Faster animations
 * )
 * ```
 *
 * ### Memory-Constrained Configuration
 * ```kotlin
 * val lowMemoryConfig = KamsyConfiguration(
 *     blurHashCacheSize = 10,         // Minimal cache
 *     blurHashMaxConcurrentJobs = 1,  // Single threaded processing
 *     enableMetrics = false,          // Disable metrics overhead
 *     enableHardwareAcceleration = false // Software rendering
 * )
 * ```
 *
 * ### High-Performance Configuration
 * ```kotlin
 * val highPerfConfig = KamsyConfiguration(
 *     blurHashCacheSize = 200,        // Large cache for speed
 *     blurHashMaxConcurrentJobs = 6,  // More concurrent processing
 *     enableHardwareAcceleration = true,
 *     defaultAnimationDuration = 600L // Snappy animations
 * )
 * ```
 *
 * ## Hilt Integration
 * ```kotlin
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object KamsyConfigModule {
 *     @Provides
 *     @Singleton
 *     fun provideKamsyConfiguration(): KamsyConfiguration {
 *         return KamsyConfiguration(
 *             enableLogging = BuildConfig.DEBUG,
 *             enableMetrics = !BuildConfig.DEBUG,
 *             blurHashCacheSize = if (BuildConfig.DEBUG) 100 else 50
 *         )
 *     }
 * }
 * ```
 *
 * ## Performance Impact
 *
 * ### Memory Usage
 * - **blurHashCacheSize**: Directly affects memory usage (each cached image ~10-50KB)
 * - **enableMetrics**: Minimal memory overhead for metric storage
 *
 * ### CPU Usage
 * - **blurHashMaxConcurrentJobs**: Higher values use more CPU cores but process faster
 * - **enableHardwareAcceleration**: Offloads rendering to GPU
 *
 * ### Network and Storage
 * - BlurHash processing is CPU-only, no network impact
 * - Metrics may generate analytics data depending on implementation
 *
 * @param blurHashCacheSize Maximum number of decoded BlurHash images to cache in memory
 * @param blurHashMaxConcurrentJobs Maximum number of BlurHash decoding operations to run simultaneously
 * @param enableLogging Whether to enable debug logging for KamsyView operations
 * @param enableMetrics Whether to enable metrics collection for performance monitoring
 * @param defaultAnimationDuration Default duration in milliseconds for KamsyView animations
 * @param enableHardwareAcceleration Whether to enable hardware acceleration for rendering
 *
 * @see IBlurHashProcessor
 * @see IKamsyLogger
 * @see IKamsyMetrics
 * @see HiltKamsyView
 * @see KamsyView
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
data class KamsyConfiguration(
    /**
     * Maximum number of decoded BlurHash images to cache in memory.
     *
     * Controls the size of the LRU cache used to store decoded BlurHash bitmaps.
     * Higher values improve performance by reducing re-decoding but increase memory usage.
     * Each cached BlurHash typically uses 10-50KB depending on the decoded size and format.
     *
     * ## Performance Considerations
     * - **Low values (10-20)**: Minimal memory usage, more CPU work for re-decoding
     * - **Medium values (30-50)**: Balanced performance for most applications
     * - **High values (100+)**: Maximum performance, higher memory usage
     *
     * ## Memory Calculation
     * Approximate memory usage: `cacheSize * averageImageSize`
     * - 50 images × 30KB = ~1.5MB memory usage
     * - 100 images × 30KB = ~3MB memory usage
     *
     * ## Recommended Values
     * - **Mobile apps**: 20-50 depending on available memory
     * - **Tablet apps**: 50-100 for better performance
     * - **Memory-constrained devices**: 10-20
     * - **High-performance apps**: 100-200
     *
     * @see IBlurHashProcessor
     */
    val blurHashCacheSize: Int = 50,

    /**
     * Maximum number of BlurHash decoding operations to run simultaneously.
     *
     * Controls the concurrency level for BlurHash processing to balance performance
     * and resource usage. Higher values can improve throughput but may impact
     * UI responsiveness and battery life.
     *
     * ## Performance Impact
     * - **1**: Single-threaded, minimal CPU usage, slower processing
     * - **2-3**: Good balance for most devices
     * - **4-6**: High performance for powerful devices
     * - **8+**: May cause thread contention and UI lag
     *
     * ## Device-Specific Recommendations
     * - **Low-end devices**: 1-2 jobs
     * - **Mid-range devices**: 2-3 jobs
     * - **High-end devices**: 3-6 jobs
     * - **Tablets/Desktop**: 4-8 jobs
     *
     * ## Thread Safety
     * All BlurHash processing operations are thread-safe and use background threads
     * to avoid blocking the main UI thread.
     *
     * @see IBlurHashProcessor
     */
    val blurHashMaxConcurrentJobs: Int = 3,

    /**
     * Whether to enable debug logging for KamsyView operations.
     *
     * Controls the verbosity of logging output for debugging and development purposes.
     * When enabled, KamsyView components will log detailed information about:
     * - View lifecycle events and state changes
     * - BlurHash processing progress and results
     * - Drawable creation and configuration
     * - Error conditions and recovery attempts
     * - Performance timing information
     *
     * ## Development vs Production
     * ```kotlin
     * val config = KamsyConfiguration(
     *     enableLogging = BuildConfig.DEBUG // Enable only in debug builds
     * )
     * ```
     *
     * ## Log Categories
     * - **DEBUG**: Detailed operation information
     * - **INFO**: General status and lifecycle events
     * - **WARN**: Non-fatal issues and fallback operations
     * - **ERROR**: Error conditions and exceptions
     *
     * ## Performance Impact
     * - Minimal performance overhead when disabled
     * - Some overhead when enabled due to string formatting and I/O
     * - Should typically be disabled in production builds
     *
     * @see IKamsyLogger
     */
    val enableLogging: Boolean = true,

    /**
     * Whether to enable metrics collection for performance monitoring.
     *
     * Controls the collection of performance and usage metrics for monitoring
     * application health and optimizing KamsyView performance. When enabled,
     * metrics are collected for:
     * - View creation and destruction counts
     * - BlurHash processing times and success rates
     * - Error rates and failure modes
     * - Memory usage patterns
     * - Animation performance metrics
     *
     * ## Metrics Categories
     * - **Performance**: Processing times, frame rates, memory usage
     * - **Usage**: Feature utilization, configuration patterns
     * - **Reliability**: Error rates, success rates, fallback usage
     * - **User Experience**: Animation smoothness, loading times
     *
     * ## Data Privacy
     * Metrics collection should comply with privacy policies and regulations.
     * No user-identifiable information should be collected without consent.
     *
     * ## Integration Examples
     * ```kotlin
     * // Firebase Analytics integration
     * val config = KamsyConfiguration(
     *     enableMetrics = true // Metrics sent to Firebase
     * )
     *
     * // Custom analytics platform
     * @Provides
     * fun provideMetrics(): IKamsyMetrics = CustomAnalyticsMetrics()
     * ```
     *
     * ## Performance Impact
     * - Minimal overhead for metric collection
     * - Some network usage for uploading metrics
     * - Consider user's data plan and battery usage
     *
     * @see IKamsyMetrics
     */
    val enableMetrics: Boolean = false,

    /**
     * Default duration in milliseconds for KamsyView animations.
     *
     * Sets the standard timing for various animation effects including:
     * - Border rotation and pulsing animations
     * - Volumetric breathing and pulsing effects
     * - Overlay transitions and state changes
     * - Shape morphing and property transitions
     *
     * ## Animation Types Affected
     * - **Border animations**: Rotation, pulsing, arch movements
     * - **Volumetric effects**: Breathing, intensity changes
     * - **Overlay transitions**: Tint changes, status updates
     * - **State transitions**: Loading to success, error states
     *
     * ## Duration Guidelines
     * - **Fast (200-400ms)**: Quick feedback, micro-interactions
     * - **Medium (400-800ms)**: Standard UI transitions
     * - **Slow (800-1500ms)**: Emphasis, decorative effects
     * - **Very Slow (1500ms+)**: Ambient effects, breathing animations
     *
     * ## Accessibility Considerations
     * ```kotlin
     * val config = KamsyConfiguration(
     *     defaultAnimationDuration = if (accessibilityManager.isReduceMotionEnabled) {
     *         200L // Faster animations for motion sensitivity
     *     } else {
     *         1000L // Standard duration
     *     }
     * )
     * ```
     *
     * ## Platform Guidelines
     * - **Material Design**: 200-300ms for most transitions
     * - **iOS Human Interface Guidelines**: 250-400ms typical
     * - **Web**: 200-500ms depending on interaction type
     *
     * @see AnimationConfiguration
     */
    val defaultAnimationDuration: Long = 1000L,

    /**
     * Whether to enable hardware acceleration for improved rendering performance.
     *
     * Controls the use of GPU acceleration for rendering KamsyView components.
     * Hardware acceleration can significantly improve performance for:
     * - Complex shape rendering and clipping
     * - Gradient and volumetric effects
     * - Animation smoothness and frame rates
     * - Large view sizes and high-resolution displays
     *
     * ## Performance Benefits
     * - **Faster rendering**: GPU is optimized for graphics operations
     * - **Smoother animations**: Higher frame rates, reduced stuttering
     * - **Better effects**: Enhanced gradients, shadows, and blending
     * - **Reduced CPU usage**: Offloads work from main processor
     *
     * ## When to Disable
     * - **Memory-constrained devices**: GPU memory usage may be limited
     * - **Compatibility issues**: Some older devices or custom ROMs
     * - **Battery optimization**: Software rendering may use less power
     * - **Testing purposes**: Isolate rendering-related issues
     *
     * ## Device Compatibility
     * ```kotlin
     * val config = KamsyConfiguration(
     *     enableHardwareAcceleration = when {
     *         Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> true
     *         isLowEndDevice() -> false
     *         else -> true
     *     }
     * )
     * ```
     *
     * ## Impact on Features
     * - **Enabled**: Full effect quality, optimal performance
     * - **Disabled**: Reduced effect quality, fallback rendering
     *
     * ## Debugging
     * Hardware acceleration issues can be debugged using:
     * - `adb shell setprop debug.hwui.disable true` (disable globally)
     * - GPU profiling tools in Android Studio
     * - Visual debugging overlays
     *
     * @see View.setLayerType
     * @see View.LAYER_TYPE_HARDWARE
     * @see View.LAYER_TYPE_SOFTWARE
     */
    val enableHardwareAcceleration: Boolean = true
)
