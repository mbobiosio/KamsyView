package com.github.kamsyview.core

import android.graphics.drawable.Drawable
/**
 * Sealed class hierarchy representing the different UI states for KamsyView components.
 *
 * KamsyUiState follows the standard Loading → Success/Error pattern with additional
 * specialized states for BlurHash processing and placeholder content. This state system
 * enables reactive UI updates and proper error handling throughout the view lifecycle.
 *
 * ## State Flow Architecture
 * ```
 * Loading → Success (content loaded)
 *        ↓
 *        → BlurHashLoaded (BlurHash processed)
 *        ↓
 *        → Placeholder (fallback content)
 *        ↓
 *        → Error (processing failed)
 * ```
 *
 * ## Usage with StateFlow
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     private val _uiState = MutableStateFlow<KamsyUiState>(KamsyUiState.Loading)
 *     val uiState: StateFlow<KamsyUiState> = _uiState.asStateFlow()
 *
 *     fun loadContent() {
 *         _uiState.value = KamsyUiState.Loading
 *         // ... async processing
 *         _uiState.value = KamsyUiState.Success(drawable, ContentSource.CACHE)
 *     }
 * }
 * ```
 *
 * ## State Observation
 * ```kotlin
 * viewLifecycleOwner.lifecycleScope.launch {
 *     viewModel.uiState.collect { state ->
 *         when (state) {
 *             is KamsyUiState.Loading -> showLoadingIndicator()
 *             is KamsyUiState.Success -> displayContent(state.drawable)
 *             is KamsyUiState.Error -> showError(state.error.getUserMessage())
 *             is KamsyUiState.BlurHashLoaded -> displayBlurHash(state.drawable)
 *             is KamsyUiState.Placeholder -> displayPlaceholder(state.drawable)
 *         }
 *     }
 * }
 * ```
 *
 * ## Functional Programming Support
 * ```kotlin
 * val result = uiState.fold(
 *     onLoading = { "Loading..." },
 *     onSuccess = { "Loaded from ${it.source}" },
 *     onError = { "Error: ${it.error.getUserMessage()}" },
 *     onBlurHashLoaded = { "BlurHash ready" },
 *     onPlaceholder = { "Showing placeholder" }
 * )
 * ```
 *
 * @see KamsyView.uiState
 * @see KamsyError
 * @see ContentSource
 * @see PlaceholderReason
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
sealed class KamsyUiState {

    /**
     * Loading state indicating that content is being processed or loaded.
     *
     * This state is typically shown while:
     * - BlurHash strings are being decoded into bitmaps
     * - Network images are being downloaded (future feature)
     * - Complex drawable processing is occurring
     * - View initialization is in progress
     *
     * ## UI Recommendations
     * - Show loading indicators or skeleton views
     * - Disable user interactions that depend on content
     * - Display progress information if available
     * - Consider showing BlurHash placeholders during loading
     *
     * ## State Transitions
     * From Loading, the view can transition to:
     * - [Success] when content loads successfully
     * - [BlurHashLoaded] when BlurHash processing completes
     * - [Placeholder] when falling back to placeholder content
     * - [Error] when processing fails
     *
     * @see KamsyView.handleLoadingState
     */
    data object Loading : KamsyUiState()

    /**
     * Success state indicating that content has been loaded and is ready for display.
     *
     * This state contains the successfully loaded drawable and optional source information
     * for analytics, debugging, and optimization purposes.
     *
     * ## Content Sources
     * The [source] parameter helps track where content originated:
     * - Cache hits for performance monitoring
     * - Network loads for bandwidth tracking
     * - Local resources for asset management
     * - Generated content for processing metrics
     *
     * ## Usage Examples
     * ```kotlin
     * // Image loaded from cache
     * KamsyUiState.Success(drawable, ContentSource.CACHE)
     *
     * // Vector drawable from resources
     * KamsyUiState.Success(vectorDrawable, ContentSource.VECTOR_DRAWABLE)
     *
     * // Network image (future feature)
     * KamsyUiState.Success(networkDrawable, ContentSource.NETWORK)
     * ```
     *
     * @param drawable The successfully loaded drawable ready for display
     * @param source The source where the content originated from (for analytics/debugging)
     *
     * @see ContentSource
     * @see KamsyView.handleSuccessState
     */
    data class Success(
        val drawable: Drawable,
        val source: ContentSource = ContentSource.UNKNOWN
    ) : KamsyUiState()

    /**
     * Error state indicating that something went wrong during content loading or processing.
     *
     * This state provides structured error information that can be used for:
     * - User-friendly error messaging
     * - Error analytics and monitoring
     * - Debugging and troubleshooting
     * - Retry logic and fallback handling
     *
     * ## Error Handling Strategies
     * ```kotlin
     * when (val error = errorState.error) {
     *     is KamsyError.Network.NoInternet -> showRetryButton()
     *     is KamsyError.BlurHash.InvalidHash -> fallbackToPlaceholder()
     *     is KamsyError.General.OutOfMemory -> clearCacheAndRetry()
     *     else -> showGenericError()
     * }
     * ```
     *
     * ## Debugging Support
     * The optional [throwable] provides additional context for debugging:
     * ```kotlin
     * if (BuildConfig.DEBUG && errorState.throwable != null) {
     *     Log.e("KamsyView", "Error details", errorState.throwable)
     * }
     * ```
     *
     * @param error The structured error information describing what went wrong
     * @param throwable Optional throwable for detailed debugging information
     *
     * @see KamsyError
     * @see KamsyError.getUserMessage
     * @see KamsyView.handleErrorState
     */
    data class Error(
        val error: KamsyError,
        val throwable: Throwable? = null
    ) : KamsyUiState()

    /**
     * BlurHash loaded state indicating that BlurHash processing has completed successfully.
     *
     * This specialized state represents the completion of BlurHash decoding, where
     * a compact BlurHash string has been converted into a displayable bitmap.
     * BlurHash images serve as progressive loading placeholders that provide
     * meaningful content representation while full images load.
     *
     * ## BlurHash Benefits
     * - **Progressive Loading**: Shows meaningful content immediately
     * - **Smooth Transitions**: Gradual replacement with full images
     * - **Bandwidth Efficiency**: Compact string representation (~20-30 characters)
     * - **Visual Continuity**: Maintains layout and color scheme during loading
     *
     * ## Usage Context
     * ```kotlin
     * // Set BlurHash and observe state changes
     * kamsyView.blurHash = "LGFFaXYk^6#M@-5c,1J5@[or[Q6."
     *
     * // Handle BlurHash loaded state
     * is KamsyUiState.BlurHashLoaded -> {
     *     displayBlurHash(state.drawable)
     *     // Continue loading full image in background
     *     loadFullImage(imageUrl)
     * }
     * ```
     *
     * ## Transition Patterns
     * BlurHashLoaded can transition to:
     * - [Success] when full image loads
     * - [Error] if full image loading fails
     * - Remain as BlurHashLoaded if no full image is provided
     *
     * @param drawable The processed BlurHash bitmap as a drawable
     * @param hash The original BlurHash string for reference and debugging
     *
     * @see KamsyView.blurHash
     * @see KamsyView.blurHashProcessor
     * @see KamsyView.handleBlurHashLoaded
     */
    data class BlurHashLoaded(
        val drawable: Drawable,
        val hash: String
    ) : KamsyUiState()

    /**
     * Placeholder state indicating that placeholder content is being displayed.
     *
     * This state is used when no primary content is available and the view
     * falls back to showing placeholder content such as initials, generic icons,
     * or default imagery. The [reason] parameter provides context about why
     * the placeholder is being shown.
     *
     * ## Placeholder Types
     * - **Text Placeholders**: User initials, custom text, symbols
     * - **Icon Placeholders**: Generic icons or fallback images
     * - **Generated Placeholders**: Auto-generated content based on user data
     * - **Error Placeholders**: Fallback content when errors occur
     *
     * ## Common Placeholder Scenarios
     * ```kotlin
     * // Default placeholder (no image provided)
     * KamsyUiState.Placeholder(drawable, PlaceholderReason.DEFAULT)
     *
     * // Loading placeholder while processing
     * KamsyUiState.Placeholder(drawable, PlaceholderReason.LOADING)
     *
     * // Error placeholder when BlurHash fails
     * KamsyUiState.Placeholder(drawable, PlaceholderReason.BLUR_HASH_INVALID)
     *
     * // Network error placeholder
     * KamsyUiState.Placeholder(drawable, PlaceholderReason.NETWORK_ERROR)
     * ```
     *
     * ## Analytics and Monitoring
     * The [reason] parameter enables tracking placeholder usage patterns:
     * ```kotlin
     * analytics.track("placeholder_shown", mapOf(
     *     "reason" to state.reason.name,
     *     "view_id" to kamsyView.id
     * ))
     * ```
     *
     * @param drawable The placeholder drawable to display
     * @param reason The reason why the placeholder is being shown (for analytics/debugging)
     *
     * @see PlaceholderReason
     * @see KamsyView.placeholderText
     * @see KamsyView.createPlaceholderDrawable
     * @see KamsyView.handlePlaceholderState
     */
    data class Placeholder(
        val drawable: Drawable,
        val reason: PlaceholderReason = PlaceholderReason.DEFAULT
    ) : KamsyUiState()
}

/**
 * Enumeration of different sources where content can originate from.
 *
 * ContentSource provides valuable metadata for analytics, performance monitoring,
 * and debugging by tracking where drawable content was loaded from. This information
 * helps optimize caching strategies, monitor network usage, and debug content issues.
 *
 * ## Analytics Applications
 * ```kotlin
 * when (successState.source) {
 *     ContentSource.CACHE -> analytics.increment("cache_hit")
 *     ContentSource.NETWORK -> analytics.increment("network_load")
 *     ContentSource.BLUR_HASH -> analytics.increment("blurhash_used")
 * }
 * ```
 *
 * ## Performance Monitoring
 * ```kotlin
 * val loadTime = measureTimeMillis {
 *     loadContent()
 * }
 * metrics.recordLoadTime(source, loadTime)
 * ```
 *
 * @see KamsyUiState.Success.source
 */
enum class ContentSource {
    /** Content loaded from Android drawable resources */
    DRAWABLE_RESOURCE,

    /** Content created from Bitmap objects */
    BITMAP,

    /** Content from vector drawable resources or generated vectors */
    VECTOR_DRAWABLE,

    /** Content from animated drawables (GIFs, animated vectors, etc.) */
    ANIMATED_DRAWABLE,

    /** Content generated from BlurHash processing */
    BLUR_HASH,

    /** Content from placeholder generation (text, initials, icons) */
    PLACEHOLDER,

    /** Content loaded from network sources (future feature) */
    NETWORK,

    /** Content retrieved from local cache storage */
    CACHE,

    /** Content source is unknown or not tracked */
    UNKNOWN
}

/**
 * Enumeration of reasons why placeholder content is being displayed.
 *
 * PlaceholderReason provides context about placeholder usage, enabling better
 * user experience decisions, analytics tracking, and debugging support.
 * Understanding why placeholders are shown helps optimize the user experience
 * and identify potential issues.
 *
 * ## UX Decision Making
 * ```kotlin
 * when (placeholderState.reason) {
 *     PlaceholderReason.LOADING -> showSubtleLoadingAnimation()
 *     PlaceholderReason.ERROR -> showRetryButton()
 *     PlaceholderReason.NETWORK_ERROR -> showOfflineMessage()
 *     PlaceholderReason.DEFAULT -> showNormalPlaceholder()
 * }
 * ```
 *
 * ## Error Recovery
 * ```kotlin
 * if (reason == PlaceholderReason.BLUR_HASH_INVALID) {
 *     // BlurHash was malformed, try alternative approach
 *     fallbackToDefaultPlaceholder()
 * }
 * ```
 *
 * @see KamsyUiState.Placeholder.reason
 */
enum class PlaceholderReason {
    /** Default placeholder shown when no specific reason applies */
    DEFAULT,

    /** Placeholder shown during loading operations */
    LOADING,

    /** Placeholder shown due to error conditions */
    ERROR,

    /** Placeholder shown when content is empty or null */
    EMPTY_CONTENT,

    /** Placeholder shown due to network-related errors */
    NETWORK_ERROR,

    /** Placeholder shown when BlurHash processing fails */
    BLUR_HASH_INVALID
}

/**
 * Sealed class hierarchy representing different types of errors that can occur in KamsyView.
 *
 * KamsyError provides a structured approach to error handling with specific error
 * categories that enable targeted error recovery, user messaging, and debugging.
 * Each error category groups related error conditions that can be handled similarly.
 *
 * ## Error Categories
 * - **[BlurHash]**: Errors related to BlurHash processing and decoding
 * - **[Drawable]**: Errors related to drawable creation and manipulation
 * - **[Network]**: Errors related to network operations (future features)
 * - **[Cache]**: Errors related to caching operations
 * - **[General]**: General errors and fallback error types
 *
 * ## Error Handling Patterns
 * ```kotlin
 * fun handleError(error: KamsyError) {
 *     when (error) {
 *         is KamsyError.BlurHash -> handleBlurHashError(error)
 *         is KamsyError.Network -> handleNetworkError(error)
 *         is KamsyError.General.OutOfMemory -> handleMemoryError()
 *         else -> handleGenericError(error.getUserMessage())
 *     }
 * }
 * ```
 *
 * ## User-Friendly Messaging
 * ```kotlin
 * val userMessage = error.getUserMessage()
 * showErrorDialog(userMessage)
 * ```
 *
 * ## Error Analytics
 * ```kotlin
 * analytics.track("kamsy_error", mapOf(
 *     "error_type" to error::class.simpleName,
 *     "error_message" to error.getUserMessage()
 * ))
 * ```
 *
 * @see getUserMessage
 * @see toKamsyError
 * @see KamsyUiState.Error
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
sealed class KamsyError {

    /**
     * Errors related to BlurHash processing and decoding operations.
     *
     * BlurHash errors occur during the conversion of BlurHash strings into
     * displayable bitmaps. These errors help identify issues with BlurHash
     * data quality, processing parameters, or system capabilities.
     */
    sealed class BlurHash : KamsyError() {
        /**
         * The provided BlurHash string is malformed or invalid.
         *
         * This error occurs when:
         * - BlurHash string has incorrect format or length
         * - BlurHash contains invalid characters
         * - BlurHash checksum validation fails
         *
         * **Recovery Strategy**: Fall back to default placeholder
         */
        data object InvalidHash : BlurHash()

        /**
         * The BlurHash specifies invalid or unsupported dimensions.
         *
         * This error occurs when:
         * - Requested decode dimensions are zero or negative
         * - Dimensions exceed system memory limitations
         * - Aspect ratio is incompatible with BlurHash data
         *
         * **Recovery Strategy**: Use default dimensions or fallback placeholder
         */
        data object InvalidDimensions : BlurHash()

        /**
         * BlurHash decoding process failed during bitmap generation.
         *
         * This error occurs when:
         * - System runs out of memory during decoding
         * - BlurHash algorithm encounters unexpected data
         * - Graphics subsystem is unavailable
         *
         * **Recovery Strategy**: Retry with smaller dimensions or use placeholder
         */
        data object DecodingFailed : BlurHash()

        /**
         * General BlurHash processing error with descriptive message.
         *
         * @param message Detailed description of the processing error
         */
        data class ProcessingError(val message: String) : BlurHash()
    }

    /**
     * Errors related to drawable creation, manipulation, and display operations.
     *
     * Drawable errors occur when working with Android drawable objects,
     * including creation, configuration, and rendering operations.
     */
    sealed class Drawable : KamsyError() {
        /**
         * The provided drawable is null, corrupted, or invalid.
         *
         * **Recovery Strategy**: Use default placeholder drawable
         */
        data object InvalidDrawable : Drawable()

        /**
         * The drawable format is not supported by the current system.
         *
         * **Recovery Strategy**: Convert to supported format or use placeholder
         */
        data object UnsupportedFormat : Drawable()

        /**
         * Failed to load or create the drawable.
         *
         * @param message Detailed description of the loading error
         */
        data class LoadingError(val message: String) : Drawable()
    }

    /**
     * Errors related to network operations for future image loading features.
     *
     * Network errors will be used when KamsyView gains network image loading
     * capabilities, providing structured handling of connectivity and server issues.
     */
    sealed class Network : KamsyError() {
        /**
         * No internet connection available.
         *
         * **Recovery Strategy**: Show offline placeholder, enable retry when connected
         */
        data object NoInternet : Network()

        /**
         * Network request timed out.
         *
         * **Recovery Strategy**: Retry with exponential backoff
         */
        data object Timeout : Network()

        /**
         * Requested resource was not found on the server.
         *
         * **Recovery Strategy**: Use placeholder, log for debugging
         */
        data object NotFound : Network()

        /**
         * Server returned an error response.
         *
         * @param code HTTP status code
         * @param message Server error message
         */
        data class ServerError(val code: Int, val message: String) : Network()
    }

    /**
     * Errors related to caching operations and cache management.
     *
     * Cache errors occur during cache storage, retrieval, or maintenance
     * operations for BlurHash processing and future image caching features.
     */
    sealed class Cache : KamsyError() {
        /**
         * Cache data is corrupted and cannot be read.
         *
         * **Recovery Strategy**: Clear corrupted cache, regenerate content
         */
        data object CacheCorrupted : Cache()

        /**
         * Cache has reached its size limit and cannot store more data.
         *
         * **Recovery Strategy**: Evict old entries, continue without caching
         */
        data object CacheFull : Cache()

        /**
         * General cache operation error.
         *
         * @param message Detailed description of the cache error
         */
        data class CacheError(val message: String) : Cache()
    }

    /**
     * General errors and fallback error types for unclassified issues.
     *
     * General errors handle edge cases, system-level issues, and provide
     * fallback error representation for unexpected conditions.
     */
    sealed class General : KamsyError() {
        /**
         * Unknown error with no specific categorization.
         *
         * **Recovery Strategy**: Log for debugging, use default error handling
         */
        data object Unknown : General()

        /**
         * System is out of memory and cannot complete the operation.
         *
         * **Recovery Strategy**: Clear caches, reduce quality, trigger GC
         */
        data object OutOfMemory : General()

        /**
         * Custom error with application-specific message and code.
         *
         * @param message Custom error message
         * @param code Optional error code for categorization
         */
        data class Custom(val message: String, val code: Int = -1) : General()
    }
}

// Extension Functions for KamsyUiState

/**
 * Checks if the current state is [KamsyUiState.Loading].
 *
 * @return true if the state represents a loading condition
 */
fun KamsyUiState.isLoading(): Boolean = this is KamsyUiState.Loading

/**
 * Checks if the current state is [KamsyUiState.Success].
 *
 * @return true if the state represents successful content loading
 */
fun KamsyUiState.isSuccess(): Boolean = this is KamsyUiState.Success

/**
 * Checks if the current state is [KamsyUiState.Error].
 *
 * @return true if the state represents an error condition
 */
fun KamsyUiState.isError(): Boolean = this is KamsyUiState.Error

/**
 * Checks if the current state is [KamsyUiState.BlurHashLoaded].
 *
 * @return true if the state represents successfully loaded BlurHash content
 */
fun KamsyUiState.isBlurHashLoaded(): Boolean = this is KamsyUiState.BlurHashLoaded

/**
 * Checks if the current state is [KamsyUiState.Placeholder].
 *
 * @return true if the state represents placeholder content display
 */
fun KamsyUiState.isPlaceholder(): Boolean = this is KamsyUiState.Placeholder

/**
 * Extracts the drawable from states that contain displayable content.
 *
 * Returns the drawable from Success, BlurHashLoaded, or Placeholder states.
 * Returns null for Loading or Error states that don't contain displayable content.
 *
 * ## Usage Example
 * ```kotlin
 * val drawable = uiState.getDrawable()
 * if (drawable != null) {
 *     imageView.setImageDrawable(drawable)
 * }
 * ```
 *
 * @return The drawable if available, null otherwise
 */
fun KamsyUiState.getDrawable(): Drawable? = when (this) {
    is KamsyUiState.Success -> drawable
    is KamsyUiState.BlurHashLoaded -> drawable
    is KamsyUiState.Placeholder -> drawable
    else -> null
}

/**
 * Extracts the error information from Error states.
 *
 * @return The KamsyError if the state is Error, null otherwise
 */
fun KamsyUiState.getError(): KamsyError? = when (this) {
    is KamsyUiState.Error -> error
    else -> null
}

/**
 * Functional approach to handle all possible UI states with type-safe callbacks.
 *
 * This fold operation provides a comprehensive way to handle every possible state
 * in a single expression, ensuring all cases are covered and preventing
 * unhandled state scenarios.
 *
 * ## Usage Example
 * ```kotlin
 * val statusMessage = uiState.fold(
 *     onLoading = { "Loading content..." },
 *     onSuccess = { "Content loaded from ${it.source}" },
 *     onError = { "Error: ${it.error.getUserMessage()}" },
 *     onBlurHashLoaded = { "BlurHash preview ready" },
 *     onPlaceholder = { "Showing ${it.reason} placeholder" }
 * )
 * ```
 *
 * @param T The return type for all callback functions
 * @param onLoading Callback for Loading state
 * @param onSuccess Callback for Success state
 * @param onError Callback for Error state
 * @param onBlurHashLoaded Callback for BlurHashLoaded state
 * @param onPlaceholder Callback for Placeholder state
 * @return The result of the appropriate callback function
 */
inline fun <T> KamsyUiState.fold(
    onLoading: () -> T,
    onSuccess: (KamsyUiState.Success) -> T,
    onError: (KamsyUiState.Error) -> T,
    onBlurHashLoaded: (KamsyUiState.BlurHashLoaded) -> T,
    onPlaceholder: (KamsyUiState.Placeholder) -> T
): T = when (this) {
    is KamsyUiState.Loading -> onLoading()
    is KamsyUiState.Success -> onSuccess(this)
    is KamsyUiState.Error -> onError(this)
    is KamsyUiState.BlurHashLoaded -> onBlurHashLoaded(this)
    is KamsyUiState.Placeholder -> onPlaceholder(this)
}

/**
 * Transforms the drawable content in states that contain displayable content.
 *
 * Applies the transform function to drawables in Success, BlurHashLoaded, and
 * Placeholder states while leaving Loading and Error states unchanged.
 *
 * ## Usage Example
 * ```kotlin
 * val tintedState = uiState.mapSuccess { drawable ->
 *     drawable.apply {
 *         setTint(Color.BLUE)
 *     }
 * }
 * ```
 *
 * @param transform Function to transform the drawable
 * @return New state with transformed drawable, or original state if no drawable present
 */
inline fun KamsyUiState.mapSuccess(transform: (Drawable) -> Drawable): KamsyUiState = when (this) {
    is KamsyUiState.Success -> copy(drawable = transform(drawable))
    is KamsyUiState.BlurHashLoaded -> copy(drawable = transform(drawable))
    is KamsyUiState.Placeholder -> copy(drawable = transform(drawable))
    else -> this
}

// Extension Functions for KamsyError

/**
 * Checks if the error is related to BlurHash processing.
 *
 * @return true if the error is a BlurHash-related error
 */
fun KamsyError.isBlurHashError(): Boolean = this is KamsyError.BlurHash

/**
 * Checks if the error is related to drawable operations.
 *
 * @return true if the error is a drawable-related error
 */
fun KamsyError.isDrawableError(): Boolean = this is KamsyError.Drawable

/**
 * Checks if the error is related to network operations.
 *
 * @return true if the error is a network-related error
 */
fun KamsyError.isNetworkError(): Boolean = this is KamsyError.Network

/**
 * Checks if the error is related to cache operations.
 *
 * @return true if the error is a cache-related error
 */
fun KamsyError.isCacheError(): Boolean = this is KamsyError.Cache

/**
 * Checks if the error is a general/unclassified error.
 *
 * @return true if the error is a general error
 */
fun KamsyError.isGeneralError(): Boolean = this is KamsyError.General

/**
 * Converts KamsyError to a user-friendly error message suitable for display.
 *
 * Provides localized, user-friendly error messages that can be shown in UI
 * without exposing technical implementation details. Messages are designed
 * to be helpful and actionable when possible.
 *
 * ## Localization Support
 * ```kotlin
 * fun KamsyError.getLocalizedMessage(context: Context): String {
 *     return when (this) {
 *         is KamsyError.Network.NoInternet -> context.getString(R.string.error_no_internet)
 *         else -> getUserMessage()
 *     }
 * }
 * ```
 *
 * @return User-friendly error message string
 */
fun KamsyError.getUserMessage(): String = when (this) {
    is KamsyError.BlurHash.InvalidHash -> "Invalid BlurHash format"
    is KamsyError.BlurHash.InvalidDimensions -> "Invalid image dimensions"
    is KamsyError.BlurHash.DecodingFailed -> "Failed to decode BlurHash"
    is KamsyError.BlurHash.ProcessingError -> "BlurHash processing error: $message"

    is KamsyError.Drawable.InvalidDrawable -> "Invalid drawable"
    is KamsyError.Drawable.UnsupportedFormat -> "Unsupported image format"
    is KamsyError.Drawable.LoadingError -> "Failed to load image: $message"

    is KamsyError.Network.NoInternet -> "No internet connection"
    is KamsyError.Network.Timeout -> "Request timeout"
    is KamsyError.Network.NotFound -> "Image not found"
    is KamsyError.Network.ServerError -> "Server error ($code): $message"

    is KamsyError.Cache.CacheCorrupted -> "Cache corrupted"
    is KamsyError.Cache.CacheFull -> "Cache full"
    is KamsyError.Cache.CacheError -> "Cache error: $message"

    is KamsyError.General.Unknown -> "Unknown error"
    is KamsyError.General.OutOfMemory -> "Out of memory"
    is KamsyError.General.Custom -> message.ifBlank { "Error occurred" }
}

/**
 * Converts any Throwable to an appropriate KamsyError for consistent error handling.
 *
 * This extension function enables converting standard Java/Kotlin exceptions
 * into the structured KamsyError hierarchy, ensuring all errors can be handled
 * consistently within the KamsyView error system.
 *
 * ## Usage Example
 * ```kotlin
 * try {
 *     processBlurHash(hash)
 * } catch (e: Exception) {
 *     _uiState.value = KamsyUiState.Error(e.toKamsyError(), e)
 * }
 * ```
 *
 * ## Supported Throwable Types
 * - [OutOfMemoryError] → [KamsyError.General.OutOfMemory]
 * - All other throwables → [KamsyError.General.Custom] with message and hash code
 *
 * ## Custom Error Mapping
 * ```kotlin
 * fun Throwable.toKamsyError(): KamsyError = when (this) {
 *     is OutOfMemoryError -> KamsyError.General.OutOfMemory
 *     is SocketTimeoutException -> KamsyError.Network.Timeout
 *     is UnknownHostException -> KamsyError.Network.NoInternet
 *     is IllegalArgumentException -> KamsyError.BlurHash.InvalidHash
 *     else -> KamsyError.General.Custom(
 *         message = this.message ?: "Unknown error",
 *         code = this.hashCode()
 *     )
 * }
 * ```
 *
 * @return Appropriate KamsyError representing the throwable
 * @see KamsyError.General.Custom
 * @see KamsyError.General.OutOfMemory
 */
fun Throwable.toKamsyError(): KamsyError = when (this) {
    is OutOfMemoryError -> KamsyError.General.OutOfMemory
    else -> KamsyError.General.Custom(
        message = this.message ?: "Unknown error",
        code = this.hashCode()
    )
}
