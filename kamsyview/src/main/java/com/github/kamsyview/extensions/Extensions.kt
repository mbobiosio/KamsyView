package com.github.kamsyview.extensions


import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.dispose
import coil3.request.ImageRequest
import com.github.kamsyview.core.KamsyStyle
import com.github.kamsyview.core.KamsyView
import com.github.kamsyview.core.KamsyViewConfiguration
import com.github.kamsyview.core.PlaceholderConfiguration
import com.github.kamsyview.core.configure
import com.github.kamsyview.drawables.KamsyPlaceholderDrawable
import com.github.kamsyview.processing.BlurHashDecoder

// =================
// UTILITY FUNCTIONS
// =================

/**
 * Extracts initials from a person's name with intelligent handling of different name formats.
 *
 * This enhanced version provides smart initial extraction that handles various name formats
 * and edge cases commonly encountered in real-world applications.
 *
 * ## Name Format Handling
 * - **Single name**: Returns first character (e.g., "John" → "J")
 * - **Two names**: Returns both initials (e.g., "John Doe" → "JD")
 * - **Three names**: Returns first and last initials (e.g., "John Michael Doe" → "JD")
 * - **Four+ names**: Returns first two initials (e.g., "John Michael Anne Doe" → "JM")
 *
 * ## Input Sanitization
 * - Trims whitespace from input
 * - Handles multiple delimiter types (space, tab, newline)
 * - Filters out empty parts
 * - Returns null for invalid input
 *
 * ## Usage Examples
 * ```kotlin
 * getInitials("John Doe")           // "JD"
 * getInitials("Madonna")            // "M"
 * getInitials("John Michael Doe")   // "JD"
 * getInitials("  Mary   Jane  ")    // "MJ"
 * getInitials("")                   // null
 * getInitials(null)                 // null
 * ```
 *
 * @param name The full name to extract initials from, or null
 * @return Uppercase initials string, or null if name is invalid/empty
 *
 * @see extractInitials for DSL version
 * @see createInitialsPlaceholder
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
fun getInitials(name: String?): String? = name
    ?.trim()
    ?.split(' ', '\t', '\n')
    ?.filter { it.isNotBlank() }
    ?.let { parts ->
        when (parts.size) {
            0 -> null
            1 -> parts[0].firstOrNull()?.toString()?.uppercase()
            2 -> parts.mapNotNull { it.firstOrNull()?.toString()?.uppercase() }
                .joinToString("")

            3 -> {
                val firstInitial = parts[0].firstOrNull()?.toString()?.uppercase()
                val lastInitial = parts[2].firstOrNull()?.toString()?.uppercase()
                listOfNotNull(firstInitial, lastInitial).joinToString("")
            }

            else -> parts.take(2).mapNotNull { it.firstOrNull()?.toString()?.uppercase() }
                .joinToString("")
        }
    }

/**
 * Extension function to extract initials from a string for DSL usage.
 *
 * This version is designed for use within KamsyView DSL blocks and provides
 * a fallback character when no valid initials can be extracted.
 *
 * ## Differences from getInitials()
 * - Always returns a string (never null)
 * - Provides "?" fallback for invalid input
 * - Configurable maximum number of initials
 * - Simpler logic optimized for DSL usage
 *
 * ## Usage in DSL
 * ```kotlin
 * kamsyView.configure {
 *     placeholder {
 *         initials("John Doe".extractInitials()) // "JD"
 *         initials("".extractInitials())         // "?"
 *     }
 * }
 * ```
 *
 * @param maxInitials Maximum number of initials to extract (default: 2)
 * @return Extracted initials in uppercase, or "?" if string is blank
 *
 * @see getInitials for enhanced version
 * @see PlaceholderConfiguration.initials
 */
fun String.extractInitials(maxInitials: Int = 2): String {
    return split(" ")
        .filter { it.isNotBlank() }
        .take(maxInitials)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .takeIf { it.isNotBlank() } ?: "?"
}

/**
 * Generates a deterministic background color from text using hash-based color generation.
 *
 * Creates visually appealing, consistent colors by converting text hash codes into
 * HSV color space. The same input text always produces the same color, making it
 * ideal for user avatar backgrounds that remain consistent across sessions.
 *
 * ## Color Algorithm
 * 1. Calculate hash code of input text
 * 2. Extract hue from hash modulo 360 (full spectrum)
 * 3. Generate saturation between 0.5-1.0 (vibrant colors)
 * 4. Generate lightness between 0.4-0.7 (good contrast)
 * 5. Convert HSV to RGB color integer
 *
 * ## Color Characteristics
 * - **Hue**: Full spectrum (0-360°) for maximum variety
 * - **Saturation**: 0.5-1.0 for vibrant, non-washed-out colors
 * - **Lightness**: 0.4-0.7 for good contrast with white text
 * - **Consistency**: Same text always produces same color
 *
 * ## Usage Examples
 * ```kotlin
 * val johnColor = generateBackgroundColor("John Doe")    // Always same color
 * val janeColor = generateBackgroundColor("Jane Smith")  // Different color
 *
 * // Use in placeholder configuration
 * kamsyView.configure {
 *     placeholder {
 *         backgroundColor(generateBackgroundColor(userName))
 *     }
 * }
 * ```
 *
 * @param text Input text to generate color from (typically a user's name)
 * @return RGB color integer suitable for Android Color APIs
 *
 * @see PlaceholderConfiguration.autoColor
 * @see generateInitialsBackgroundColor for internal version
 * @see Color.HSVToColor
 */
fun generateBackgroundColor(text: String): Int {
    val hash = text.hashCode()
    val hue = (hash % 360).toFloat()
    val saturation = 0.5f + (hash % 50) / 100f
    val lightness = 0.4f + (hash % 30) / 100f

    return Color.HSVToColor(floatArrayOf(hue, saturation, lightness))
}

/**
 * Creates a BlurHash placeholder drawable for progressive image loading.
 *
 * Attempts to decode a BlurHash string into a displayable drawable that can serve
 * as a placeholder while full images load. Handles errors gracefully by returning
 * null if BlurHash processing fails.
 *
 * ## Processing Steps
 * 1. Determine appropriate size (uses view's measured width or fallback)
 * 2. Decode BlurHash string using BlurHashDecoder
 * 3. Convert bitmap to drawable with proper resources
 * 4. Set drawable bounds for correct display
 * 5. Return drawable or null if processing fails
 *
 * ## Error Handling
 * - Invalid BlurHash strings return null
 * - Processing exceptions are caught and return null
 * - Null results allow fallback to other placeholder types
 *
 * ## Usage Example
 * ```kotlin
 * val blurHashDrawable = kamsyView.createBlurHashPlaceholder(blurHashString)
 * if (blurHashDrawable != null) {
 *     imageView.setImageDrawable(blurHashDrawable)
 * } else {
 *     // Fall back to text placeholder
 *     imageView.setImageDrawable(createTextPlaceholder("AB"))
 * }
 * ```
 *
 * @param blurHash The BlurHash string to decode
 * @return Decoded BlurHash drawable, or null if processing fails
 *
 * @see BlurHashDecoder.decode
 * @see createTextPlaceholder
 * @see createInitialsPlaceholder
 */
fun KamsyView.createBlurHashPlaceholder(blurHash: String): Drawable? {
    return runCatching {
        val size = measuredWidth.takeIf { it > 0 } ?: 100
        val bitmap = BlurHashDecoder.decode(blurHash, size, size)
        bitmap?.toDrawable(context.resources)?.apply {
            // CRITICAL FIX: Set bounds on BlurHash drawable too
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }.getOrNull()
}

/**
 * Creates an initials placeholder drawable with auto-generated background color.
 *
 * Generates a placeholder showing user initials on a deterministic background color
 * derived from the person's name. The background color is automatically generated
 * to be visually appealing and consistent for the same name.
 *
 * ## Features
 * - **Auto-generated background**: Color derived from name for consistency
 * - **Smart initials**: Uses enhanced initial extraction logic
 * - **Proper sizing**: Scales to view's current dimensions
 * - **Fallback handling**: Shows "?" if name is invalid
 * - **Bounds management**: Correctly sets drawable bounds
 *
 * ## Background Color Generation
 * The background color is generated using the same algorithm as [generateBackgroundColor]
 * but applied specifically to the initials text for color consistency.
 *
 * ## Usage Example
 * ```kotlin
 * // Create initials placeholder for user
 * val initialsDrawable = kamsyView.createInitialsPlaceholder("John Doe")
 * imageView.setImageDrawable(initialsDrawable) // Shows "JD" on generated background
 *
 * // Handles edge cases gracefully
 * val fallbackDrawable = kamsyView.createInitialsPlaceholder("") // Shows "?"
 * ```
 *
 * @param name The person's name to extract initials from (can be null/empty)
 * @return Drawable showing initials on auto-generated background color
 *
 * @see getInitials
 * @see generateInitialsBackgroundColor
 * @see createTextPlaceholder
 * @see KamsyPlaceholderDrawable
 */
fun KamsyView.createInitialsPlaceholder(name: String?): Drawable {
    val initials = getInitials(name) ?: "?"
    val size = measuredWidth.takeIf { it > 0 } ?: 100
    val backgroundColor = generateInitialsBackgroundColor(initials)

    return KamsyPlaceholderDrawable(
        size = size,
        backgroundColor = backgroundColor,
        text = initials,
        textColor = textColor,
        textSize = size / 3f,
        typeface = textTypeface,
        textSizePercentage = textSizePercentage,
        avatarMargin = avatarMargin
    ).apply {
        setBounds(0, 0, size, size)
    }
}

/**
 * Creates a text placeholder drawable with custom or default text content.
 *
 * Generates a placeholder drawable displaying specified text (or fallback text)
 * using the view's current placeholder styling configuration. This is the most
 * basic placeholder type, suitable for custom text, symbols, or generic placeholders.
 *
 * ## Text Handling
 * - **Primary text**: Uses provided text parameter if not null/empty
 * - **Fallback text**: Uses view's placeholderText property
 * - **Default fallback**: Uses "?" if no text is available
 * - **Styling**: Applies all current text styling (color, size, typeface)
 *
 * ## Configuration Respect
 * The drawable respects all current view configuration:
 * - Background color from [backgroundPlaceholderColor]
 * - Text color from [textColor]
 * - Text size calculated from [textSizePercentage]
 * - Custom typeface from [textTypeface]
 * - Margins from [avatarMargin]
 *
 * ## Usage Examples
 * ```kotlin
 * // Custom text placeholder
 * val customDrawable = kamsyView.createTextPlaceholder("AB")
 *
 * // Use view's configured placeholder text
 * val defaultDrawable = kamsyView.createTextPlaceholder(null)
 *
 * // Symbol placeholder
 * val symbolDrawable = kamsyView.createTextPlaceholder("👤")
 * ```
 *
 * @param text Custom text to display, or null to use view's placeholder text
 * @return Drawable displaying the text with current styling configuration
 *
 * @see createInitialsPlaceholder
 * @see createBlurHashPlaceholder
 * @see KamsyPlaceholderDrawable
 * @see placeholderText
 */
fun KamsyView.createTextPlaceholder(text: String?): Drawable? {
    val displayText = text ?: placeholderText ?: "?"
    val size = measuredWidth.takeIf { it > 0 } ?: 100

    return KamsyPlaceholderDrawable(
        size = size,
        backgroundColor = backgroundPlaceholderColor,
        text = displayText,
        textColor = textColor,
        textSize = calculateTextSize(),
        typeface = textTypeface,
        textSizePercentage = textSizePercentage,
        avatarMargin = avatarMargin
    ).apply {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }
}

/**
 * Creates an initials placeholder drawable with auto-generated background color (internal version).
 *
 * Internal implementation used by other placeholder creation functions. Provides
 * fine-grained control over all placeholder parameters while maintaining the
 * auto-generated background color feature.
 *
 * @param name The name to generate color from and extract initials
 * @param size The size of the drawable in pixels
 * @param textColor The color for the text content
 * @param typeface The typeface for text rendering
 * @param textSizePercentage The text size as percentage of drawable size
 * @param avatarMargin The margin around the content
 * @return Configured placeholder drawable with proper bounds
 *
 * @see createInitialsPlaceholder
 * @see generateInitialsBackgroundColor
 */
private fun KamsyView.createInitialsPlaceholderDrawable(
    name: String,
    size: Int,
    textColor: Int,
    typeface: Typeface?,
    textSizePercentage: Float,
    avatarMargin: Int
): Drawable {
    val backgroundColor = generateInitialsBackgroundColor(name)
    return KamsyPlaceholderDrawable(
        size = size,
        backgroundColor = backgroundColor,
        text = name,
        textColor = textColor,
        textSize = size / 3f,
        typeface = typeface,
        textSizePercentage = textSizePercentage,
        avatarMargin = avatarMargin
    ).apply {
        // CRITICAL: Always set bounds
        setBounds(0, 0, size, size)
    }
}

/**
 * Generates background color from text hash (internal version).
 *
 * Internal implementation of background color generation with identical algorithm
 * to [generateBackgroundColor]. Separated for internal use in placeholder creation.
 *
 * @param text Input text to generate color from
 * @return RGB color integer
 *
 * @see generateBackgroundColor
 */
private fun generateInitialsBackgroundColor(text: String): Int {
    val hash = text.hashCode()
    val hue = (hash % 360).toFloat()
    val saturation = 0.5f + (hash % 50) / 100f
    val lightness = 0.4f + (hash % 30) / 100f
    return Color.HSVToColor(floatArrayOf(hue, saturation, lightness))
}

/**
 * Sets the view to display an initials placeholder for the given name.
 *
 * Convenience method that creates and sets an initials placeholder in a single call.
 * Updates both the placeholder text property and the displayed drawable.
 *
 * ## Operations Performed
 * 1. Extracts initials from the provided name
 * 2. Updates view's placeholderText property
 * 3. Creates initials placeholder drawable
 * 4. Sets the drawable as the view's image
 *
 * @param name The person's name to create initials from
 *
 * @see createInitialsPlaceholder
 * @see getInitials
 */
fun KamsyView.showInitialsPlaceholder(name: String?) {
    val initials = getInitials(name)
    placeholderText = initials

    val drawable = createInitialsPlaceholder(name)
    setImageDrawable(drawable)
}

/**
 * Sets the view to display a text placeholder with the specified content.
 *
 * Convenience method that creates and sets a text placeholder in a single call.
 * Updates both the placeholder text property and the displayed drawable.
 *
 * ## Operations Performed
 * 1. Updates view's placeholderText property
 * 2. Creates text placeholder drawable
 * 3. Sets the drawable as the view's image
 *
 * @param text The text content to display in the placeholder
 *
 * @see createTextPlaceholder
 */
fun KamsyView.showTextPlaceholder(text: String?) {
    placeholderText = text

    val drawable = createTextPlaceholder(text)
    setImageDrawable(drawable)
}

// =================
// CONFIGURATION EXTENSIONS
// =================

/**
 * Configures KamsyView with optimized settings for avatar display use cases.
 *
 * This extension provides a convenient way to apply avatar-specific configuration
 * that covers the most common avatar display requirements. The configuration
 * is applied using the KamsyView DSL system with sensible defaults that can
 * be customized via the configuration block.
 *
 * ## Default Avatar Optimizations
 * - **Border**: Configurable border for profile emphasis
 * - **Placeholder**: Optimized colors and sizing for initials/text
 * - **Volumetric Effects**: Optional 3D effects for enhanced appearance
 * - **Overlay Support**: Prepared for status indicators and badges
 *
 * ## Usage Examples
 *
 * ### Basic Avatar Configuration
 * ```kotlin
 * kamsyView.configureForAvatars()
 * // Applies default avatar settings with no border, gray background
 * ```
 *
 * ### Custom Avatar Configuration
 * ```kotlin
 * kamsyView.configureForAvatars {
 *     borderWidth = 4
 *     borderColor = Color.BLUE
 *     placeholderBackground = Color.parseColor("#E3F2FD")
 *     placeholderTextColor = Color.parseColor("#1976D2")
 *     enableVolumetric = true
 *     enableOverlay = true
 * }
 * ```
 *
 * ### Professional Avatar Style
 * ```kotlin
 * kamsyView.configureForAvatars {
 *     borderWidth = 2
 *     borderColor = Color.parseColor("#2196F3")
 *     placeholderBackground = Color.parseColor("#E3F2FD")
 *     placeholderTextColor = Color.parseColor("#1976D2")
 *     textSizePercentage = 0.9f
 *     enableVolumetric = false
 *     enableOverlay = true
 * }
 * ```
 *
 * ### Gaming Avatar Style
 * ```kotlin
 * kamsyView.configureForAvatars {
 *     borderWidth = 6
 *     borderColor = Color.CYAN
 *     placeholderBackground = Color.parseColor("#1A1A1A")
 *     placeholderTextColor = Color.CYAN
 *     textSizePercentage = 1.2f
 *     enableVolumetric = true
 *     enableOverlay = true
 * }
 * ```
 *
 * ## Integration with Other Features
 * This configuration works seamlessly with:
 * - Image loading (Coil, Glide, etc.)
 * - BlurHash placeholders
 * - Status indicators
 * - Notification badges
 * - Animation effects
 *
 * @param block Configuration block for customizing avatar settings
 *
 * @see AvatarConfiguration
 * @see configure for full DSL access
 * @see KamsyStyle for predefined complete styles
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
fun KamsyView.configureForAvatars(block: AvatarConfiguration.() -> Unit = {}) {
    val config = AvatarConfiguration().apply(block)

    configure {
        border {
            width(config.borderWidth)
            color(config.borderColor)
        }

        placeholder {
            backgroundColor(config.placeholderBackground)
            textColor(config.placeholderTextColor)
            textSize(config.textSizePercentage)
        }

        volumetric {
            if (config.enableVolumetric) all() else none()
        }

        overlay {
            enabled(config.enableOverlay)
        }
    }
}

/**
 * Configuration data class for avatar-specific settings.
 *
 * AvatarConfiguration provides a structured way to define avatar appearance
 * settings that can be applied to KamsyView instances. This configuration
 * focuses on the most commonly needed avatar display properties.
 *
 * ## Configuration Categories
 *
 * ### Border Settings
 * - **borderWidth**: Thickness of border around avatar (0 = no border)
 * - **borderColor**: Color of the border (transparent = invisible)
 *
 * ### Placeholder Settings
 * - **placeholderBackground**: Background color for text/initials placeholders
 * - **placeholderTextColor**: Text color for initials and text content
 * - **textSizePercentage**: Relative size of placeholder text (1.0 = default)
 *
 * ### Visual Effects
 * - **enableVolumetric**: Whether to apply 3D depth effects
 * - **enableOverlay**: Whether overlay features (status, badges) are enabled
 *
 * ## Preset Configurations
 *
 * ### Minimal Avatar
 * ```kotlin
 * val minimal = AvatarConfiguration(
 *     borderWidth = 0,
 *     placeholderBackground = Color.LTGRAY,
 *     placeholderTextColor = Color.DKGRAY,
 *     enableVolumetric = false,
 *     enableOverlay = false
 * )
 * ```
 *
 * ### Professional Avatar
 * ```kotlin
 * val professional = AvatarConfiguration(
 *     borderWidth = 2,
 *     borderColor = Color.parseColor("#2196F3"),
 *     placeholderBackground = Color.parseColor("#E3F2FD"),
 *     placeholderTextColor = Color.parseColor("#1976D2"),
 *     textSizePercentage = 0.9f,
 *     enableVolumetric = false,
 *     enableOverlay = true
 * )
 * ```
 *
 * ### Gaming Avatar
 * ```kotlin
 * val gaming = AvatarConfiguration(
 *     borderWidth = 6,
 *     borderColor = Color.CYAN,
 *     placeholderBackground = Color.parseColor("#1A1A1A"),
 *     placeholderTextColor = Color.CYAN,
 *     textSizePercentage = 1.2f,
 *     enableVolumetric = true,
 *     enableOverlay = true
 * )
 * ```
 *
 * ## Customization Examples
 * ```kotlin
 * // Create base configuration
 * val baseConfig = AvatarConfiguration()
 *
 * // Customize for specific use case
 * val customConfig = baseConfig.copy(
 *     borderColor = myBrandColor,
 *     placeholderBackground = myBackgroundColor
 * )
 *
 * // Apply to view
 * kamsyView.configureForAvatars {
 *     borderColor = customConfig.borderColor
 *     placeholderBackground = customConfig.placeholderBackground
 * }
 * ```
 *
 * @param borderWidth Border thickness in pixels (default: 0 = no border)
 * @param borderColor Border color (default: transparent)
 * @param placeholderBackground Background color for placeholder content (default: gray)
 * @param placeholderTextColor Text color for placeholder content (default: white)
 * @param textSizePercentage Text size relative to view size (default: 1.0)
 * @param enableVolumetric Whether to enable 3D volumetric effects (default: false)
 * @param enableOverlay Whether to enable overlay features (default: false)
 *
 * @see configureForAvatars
 * @see KamsyViewConfiguration for full configuration options
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
data class AvatarConfiguration(
    var borderWidth: Int = 0,
    var borderColor: Int = Color.TRANSPARENT,
    var placeholderBackground: Int = Color.GRAY,
    var placeholderTextColor: Int = Color.WHITE,
    var textSizePercentage: Float = 1.0f,
    var enableVolumetric: Boolean = false,
    var enableOverlay: Boolean = false
)

/**
 * Preloads an avatar image into the cache without displaying it immediately.
 *
 * This function enables cache warming strategies where images are loaded into
 * memory before they're needed for display. This improves perceived performance
 * by reducing loading times when the image is actually requested for display.
 *
 * ## Cache Warming Benefits
 * - **Faster Display**: Cached images appear instantly when needed
 * - **Reduced Loading States**: Less time showing placeholders to users
 * - **Better UX**: Smoother scrolling and navigation experiences
 * - **Predictive Loading**: Load images for likely-to-be-viewed content
 *
 * ## Integration with Coil 3
 * Uses Coil 3's ImageLoader for efficient image loading and caching:
 * - Respects Coil's cache configuration
 * - Integrates with memory and disk caches
 * - Supports all image formats Coil supports
 * - Follows Coil's lifecycle management
 *
 * ## Usage Examples
 *
 * ### Basic Preloading
 * ```kotlin
 * kamsyView.preloadAvatar("https://api.example.com/avatars/user123.jpg") { success ->
 *     if (success) {
 *         Log.d("Cache", "Avatar preloaded successfully")
 *     }
 * }
 * ```
 *
 * ### Batch Preloading
 * ```kotlin
 * val upcomingAvatars = listOf("avatar1.jpg", "avatar2.jpg", "avatar3.jpg")
 * upcomingAvatars.forEach { avatarUrl ->
 *     kamsyView.preloadAvatar(avatarUrl, baseUrl = "https://cdn.example.com/")
 * }
 * ```
 *
 * ### Custom ImageLoader
 * ```kotlin
 * val customLoader = ImageLoader.Builder(context)
 *     .memoryCache {
 *         MemoryCache.Builder(context)
 *             .maxSizePercent(0.25)
 *             .build()
 *     }
 *     .build()
 *
 * kamsyView.preloadAvatar(
 *     avatarUrl = avatarUrl,
 *     imageLoader = customLoader
 * ) { success ->
 *     handlePreloadResult(success)
 * }
 * ```
 *
 * ### Predictive Preloading
 * ```kotlin
 * // Preload avatars for next page of users
 * fun preloadNextPageAvatars(nextPageUsers: List<User>) {
 *     nextPageUsers.forEach { user ->
 *         avatarView.preloadAvatar(user.avatarUrl) { success ->
 *             if (!success) {
 *                 analytics.track("preload_failed", mapOf("userId" to user.id))
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Error Handling
 * - Network failures are handled gracefully
 * - Invalid URLs result in onComplete(false)
 * - Null/empty URLs immediately call onComplete(false)
 * - Does not throw exceptions or crash the app
 *
 * ## Performance Impact
 * - Uses background threads for loading
 * - Does not block UI operations
 * - Respects device memory limits
 * - Can be cancelled if needed
 *
 * @param avatarUrl The URL of the avatar image to preload (null/empty = no operation)
 * @param baseUrl Base URL to prepend to relative URLs (default: empty)
 * @param imageLoader Custom ImageLoader instance (default: creates new instance)
 * @param onComplete Callback with success/failure result (optional)
 *
 * @see loadAvatar for immediate display
 * @see clearAvatarCache for cache management
 * @see ImageLoader for Coil configuration
 */
fun KamsyView.preloadAvatar(
    avatarUrl: String?,
    baseUrl: String = "",
    imageLoader: ImageLoader? = null,
    onComplete: ((Boolean) -> Unit)? = null
) {
    if (!avatarUrl.isNullOrEmpty()) {
        val fullUrl = if (baseUrl.isNotEmpty()) baseUrl + avatarUrl else avatarUrl
        val loader = imageLoader ?: ImageLoader(context)

        // Preload with Coil 3
        val request = ImageRequest.Builder(context)
            .data(fullUrl)
            .listener(
                onSuccess = { _, _ -> onComplete?.invoke(true) },
                onError = { _, _ -> onComplete?.invoke(false) }
            )
            .build()

        loader.enqueue(request)
    } else {
        onComplete?.invoke(false)
    }
}

/**
 * Clears the avatar image cache to free memory and reset cached content.
 *
 * This function provides cache management by clearing both the view's current
 * image and the underlying Coil image cache. Useful for memory management,
 * testing, or when cache invalidation is needed.
 *
 * ## Cache Clearing Operations
 * 1. **View Disposal**: Cancels any active loading requests for this view
 * 2. **Memory Cache**: Clears Coil's in-memory image cache
 * 3. **Resource Cleanup**: Frees memory used by cached images
 *
 * ## When to Clear Cache
 * - **Memory Pressure**: When device is running low on memory
 * - **User Logout**: Clear cached personal images when user signs out
 * - **Theme Changes**: Clear cache when switching between themes
 * - **Testing**: Reset cache state during testing
 * - **Manual Refresh**: When user explicitly requests cache refresh
 *
 * ## Usage Examples
 *
 * ### Basic Cache Clearing
 * ```kotlin
 * kamsyView.clearAvatarCache()
 * ```
 *
 * ### Memory Management
 * ```kotlin
 * class AvatarManager {
 *     fun onLowMemory() {
 *         // Clear all avatar caches
 *         avatarViews.forEach { it.clearAvatarCache() }
 *
 *         // Force garbage collection
 *         System.gc()
 *     }
 * }
 * ```
 *
 * ### User Session Management
 * ```kotlin
 * fun onUserLogout() {
 *     // Clear user-specific cached content
 *     profileAvatarView.clearAvatarCache()
 *     headerAvatarView.clearAvatarCache()
 *
 *     // Clear other user data...
 * }
 * ```
 *
 * ### Testing Support
 * ```kotlin
 * @Before
 * fun setUp() {
 *     // Start with clean cache for each test
 *     testAvatarView.clearAvatarCache()
 * }
 * ```
 *
 * ## Impact and Side Effects
 * - **Memory**: Immediate reduction in memory usage
 * - **Performance**: Next image loads will be slower (no cache benefit)
 * - **Network**: Subsequent loads may require network requests
 * - **Storage**: Disk cache may also be affected depending on Coil configuration
 *
 * ## Error Handling
 * - Disposal errors are caught and ignored (safe to call)
 * - Cache clearing failures don't crash the app
 * - Method is safe to call multiple times
 *
 * @see preloadAvatar for cache warming
 * @see ImageLoader.memoryCache for Coil cache configuration
 */
fun KamsyView.clearAvatarCache() {
    runCatching { dispose() }
    // Coil 3 cache clearing
    ImageLoader(context).memoryCache?.clear()
}
