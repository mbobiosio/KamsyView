package com.github.kamsyview.extensions

import android.util.Log
import coil3.ImageLoader
import coil3.dispose
import coil3.load
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import coil3.request.target
import com.github.kamsyview.core.KamsyView

/**
 * Coil 3 integration extensions for KamsyView with smart URL handling and resource support.
 *
 * These extensions integrate KamsyView with Coil 3 image loading library, providing
 * intelligent URL handling, resource drawable support, BlurHash placeholder support,
 * and automatic initials generation for avatar loading scenarios.
 *
 * ## Key Features
 * - **Smart URL Detection**: Automatically detects complete URLs vs relative paths
 * - **Resource Drawable Support**: Load from Android drawable resources
 * - **Smart Fallback Chain**: URL → BlurHash → Initials → Empty
 * - **BlurHash Integration**: Automatic BlurHash placeholder generation
 * - **Initials Generation**: Auto-generated initials from user names
 * - **Error Handling**: Graceful degradation with meaningful fallbacks
 * - **Custom ImageLoader**: Support for custom Coil ImageLoader instances
 * - **Crossfade Animation**: Smooth transitions between states
 *
 * ## Basic Usage
 * ```kotlin
 * // Complete URL - loads directly
 * kamsyView.loadAvatar(
 *     avatarUrl = "https://example.com/avatar.jpg",
 *     name = "John Doe"
 * )
 *
 * // Relative path - uses baseUrl
 * kamsyView.loadAvatar(
 *     avatarUrl = "pexels-photo-14653174.jpeg",
 *     name = "Notification",
 *     baseUrl = "https://images.pexels.com/photos/14653174/"
 * )
 *
 * // Resource drawable
 * kamsyView.loadAvatar(
 *     resourceId = R.drawable.default_avatar,
 *     name = "Default User"
 * )
 * ```
 *
 * @see ImageRequest
 * @see ImageLoader
 * @see KamsyView
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */

/**
 * Loads an avatar image using Coil 3 with intelligent URL handling and fallback support.
 *
 * This is the primary extension function for loading avatar images with [KamsyView].
 * It provides smart URL detection and a complete fallback chain that gracefully
 * handles various failure scenarios.
 *
 * ## Smart URL Handling
 * - **Complete URLs**: URLs starting with "http://" or "https://" are used directly
 * - **Relative Paths**: Other URLs are prepended with [baseUrl] if provided
 * - **URL Validation**: Malformed URLs are handled gracefully with fallbacks
 *
 * ## Fallback Chain
 * 1. **Primary URL**: Attempts to load the full avatar URL
 * 2. **BlurHash Placeholder**: Shows BlurHash placeholder during loading
 * 3. **Initials Fallback**: Generates initials placeholder on URL failure
 * 4. **Empty State**: Clears view if no fallback options available
 *
 * ## Usage Examples
 *
 * ### Complete URL (loads directly)
 * ```kotlin
 * kamsyView.loadAvatar(
 *     avatarUrl = "https://example.com/user/123/avatar.jpg",
 *     name = "Jane Smith"
 * )
 * ```
 *
 * ### Relative Path (uses baseUrl)
 * ```kotlin
 * kamsyView.loadAvatar(
 *     avatarUrl = "pexels-photo-14653174.jpeg",
 *     name = "John Doe",
 *     baseUrl = "https://images.pexels.com/photos/14653174/"
 * )
 * // Results in: "https://images.pexels.com/photos/14653174/pexels-photo-14653174.jpeg"
 * ```
 *
 * ### With BlurHash Placeholder
 * ```kotlin
 * kamsyView.loadAvatar(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName,
 *     blurHash = user.avatarBlurHash
 * )
 * ```
 *
 * ### With Custom Configuration
 * ```kotlin
 * kamsyView.loadAvatar(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName
 * ) {
 *     size(150, 150)
 *     transformations(CircleCrop(), BlurTransformation())
 *     memoryCacheKey("avatar_${user.id}")
 *     diskCacheKey("avatar_${user.id}")
 * }
 * ```
 *
 * @param avatarUrl The URL of the avatar image to load (can be null)
 * @param name The user's name for generating initials fallback (can be null)
 * @param baseUrl Base URL to prepend to avatarUrl if it's a relative path
 * @param blurHash BlurHash string for placeholder generation during loading
 * @param builder Lambda for custom Coil ImageRequest configuration
 *
 * @see loadAvatar with ResourceId
 * @see loadAvatar with custom ImageLoader
 * @see loadBlurHash
 * @see ImageRequest.Builder
 *
 * @author Mbuodile Obiosio
 * @since 1.0.0
 */
fun KamsyView.loadAvatar(
    avatarUrl: String?,
    name: String? = null,
    baseUrl: String = "",
    blurHash: String? = null,
    builder: ImageRequest.Builder.() -> Unit = {}
) {
    // Dispose any previous image request to ensure proper reloading
    runCatching { dispose() }.onFailure {
        logger.error("Failed to dispose previous image request", it)
    }

    when {
        !avatarUrl.isNullOrEmpty() -> {
            val fullUrl = buildImageUrl(avatarUrl, baseUrl)

            load(fullUrl) {
                // Apply BlurHash placeholder if available
                blurHash?.let { hash ->
                    createBlurHashPlaceholder(hash)?.let { placeholder ->
                        placeholder(placeholder)
                    }
                }

                listener(
                    onError = { _, _ ->
                        post { showFallbackPlaceholder(name) }
                    }
                )

                crossfade(true)
                runCatching { builder() }
            }
        }

        !blurHash.isNullOrEmpty() -> {
            runCatching {
                loadBlurHash(blurHash)
            }.onFailure {
                showFallbackPlaceholder(name)
            }
        }

        else -> {
            setImageDrawable(null)
            showFallbackPlaceholder(name)
        }
    }
}

/**
 * Loads an avatar from Android drawable resources with fallback support.
 *
 * This variant allows loading from app's drawable resources (R.drawable.*) while
 * maintaining the same fallback logic for error scenarios. Useful for default
 * avatars, placeholder images, or offline scenarios.
 *
 * ## Resource Loading Benefits
 * - **Immediate Loading**: No network delay for resource images
 * - **Offline Support**: Works without internet connection
 * - **Consistent Sizing**: Resources are properly sized for different densities
 * - **App Bundle**: Resources are included in app installation
 *
 * ## Usage Examples
 *
 * ### Default Avatar Resource
 * ```kotlin
 * kamsyView.loadAvatar(
 *     resourceId = R.drawable.default_user_avatar,
 *     name = "Guest User"
 * )
 * ```
 *
 * ### Fallback with Custom Configuration
 * ```kotlin
 * kamsyView.loadAvatar(
 *     resourceId = R.drawable.placeholder_avatar,
 *     name = userName
 * ) {
 *     transformations(CircleCrop())
 *     memoryCacheKey("default_avatar")
 * }
 * ```
 *
 * ### Conditional Resource Loading
 * ```kotlin
 * val resourceId = when (userType) {
 *     UserType.ADMIN -> R.drawable.admin_avatar
 *     UserType.MODERATOR -> R.drawable.mod_avatar
 *     else -> R.drawable.user_avatar
 * }
 *
 * kamsyView.loadAvatar(
 *     resourceId = resourceId,
 *     name = user.name
 * )
 * ```
 *
 * @param resourceId Android drawable resource ID (e.g., R.drawable.avatar)
 * @param name The user's name for generating initials fallback (can be null)
 * @param blurHash BlurHash string for placeholder generation during loading
 * @param builder Lambda for custom Coil ImageRequest configuration
 *
 * @see loadAvatar with URL
 * @see loadAvatar with custom ImageLoader
 *
 * @author Mbuodile Obiosio
 * @since 1.0.0
 */
fun KamsyView.loadAvatar(
    resourceId: Int,
    name: String? = null,
    blurHash: String? = null,
    builder: ImageRequest.Builder.() -> Unit = {}
) {
    runCatching { dispose() }.onFailure {
        logger.error("Failed to dispose previous image request", it)
    }

    load(resourceId) {
        // Apply BlurHash placeholder if available
        blurHash?.let { hash ->
            createBlurHashPlaceholder(hash)?.let { placeholder ->
                placeholder(placeholder)
            }
        }

        listener(
            onError = { _, _ ->
                post { showFallbackPlaceholder(name) }
            }
        )

        crossfade(true)
        runCatching { builder() }
    }
}

/**
 * Loads an avatar image using a custom Coil ImageLoader instance.
 *
 * This variant allows you to use a custom configured ImageLoader instead of the default
 * singleton instance. Includes the same smart URL handling as the primary loadAvatar method.
 *
 * ## Custom ImageLoader Benefits
 * - **Isolated Caching**: Separate cache for different image types
 * - **Custom Networking**: Different OkHttp clients for different endpoints
 * - **Transformation Pipelines**: Pre-configured transformations
 * - **Memory Management**: Custom memory cache sizes
 *
 * ## Usage Examples
 *
 * ### With Custom Cache Configuration
 * ```kotlin
 * val customLoader = ImageLoader.Builder(context)
 *     .memoryCache {
 *         MemoryCache.Builder(context)
 *             .maxSizePercent(0.25) // Use 25% of available memory
 *             .build()
 *     }
 *     .diskCache {
 *         DiskCache.Builder()
 *             .directory(context.cacheDir.resolve("avatar_cache"))
 *             .maxSizeBytes(50 * 1024 * 1024) // 50MB
 *             .build()
 *     }
 *     .build()
 *
 * kamsyView.loadAvatar(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName,
 *     imageLoader = customLoader
 * )
 * ```
 *
 * ### With Authentication
 * ```kotlin
 * val authenticatedLoader = ImageLoader.Builder(context)
 *     .okHttpClient {
 *         OkHttpClient.Builder()
 *             .addInterceptor(AuthenticationInterceptor())
 *             .build()
 *     }
 *     .build()
 *
 * kamsyView.loadAvatar(
 *     avatarUrl = "secure-avatar.jpg", // Relative path
 *     name = user.displayName,
 *     baseUrl = "https://secure-api.example.com/",
 *     imageLoader = authenticatedLoader
 * )
 * ```
 *
 * @param avatarUrl The URL of the avatar image to load (can be null)
 * @param name The user's name for generating initials fallback (can be null)
 * @param baseUrl Base URL to prepend to avatarUrl if it's a relative path
 * @param blurHash BlurHash string for placeholder generation during loading
 * @param imageLoader Custom ImageLoader instance to use for loading
 * @param builder Lambda for custom Coil ImageRequest configuration
 *
 * @see ImageLoader
 * @see ImageRequest.Builder
 * @see loadAvatar with default ImageLoader
 *
 * @author Mbuodile Obiosio
 * @since 1.0.0
 */
fun KamsyView.loadAvatar(
    avatarUrl: String?,
    name: String? = null,
    baseUrl: String = "",
    blurHash: String? = null,
    imageLoader: ImageLoader,
    builder: ImageRequest.Builder.() -> Unit = {}
) {
    when {
        !avatarUrl.isNullOrEmpty() -> {
            val fullUrl = buildImageUrl(avatarUrl, baseUrl)

            val request = ImageRequest.Builder(context)
                .data(fullUrl)
                .target(this)
                .apply {
                    // BlurHash placeholder
                    blurHash?.let { hash ->
                        createBlurHashPlaceholder(hash)?.let { placeholder ->
                            placeholder(placeholder)
                        }
                    }

                    // Fallback to initials
                    if (blurHash == null) {
                        runCatching {
                            val errorDrawable = createInitialsPlaceholder(name)
                            error(errorDrawable)
                        }.onFailure { throwable ->
                            Log.e("KamsyView", "Failed to create initials placeholder", throwable)
                        }
                    }

                    // Default crossfade
                    crossfade(true)

                    // Apply custom configuration safely
                    runCatching { builder() }
                }
                .build()

            imageLoader.enqueue(request)
        }

        !blurHash.isNullOrEmpty() -> {
            runCatching {
                loadBlurHash(blurHash)
            }.onFailure {
                showFallbackPlaceholder(name)
            }
        }

        else -> {
            runCatching { dispose() }
                .onFailure {
                    runCatching { load(null as String?) }
                }

            setImageDrawable(null)
            showFallbackPlaceholder(name)
        }
    }
}

/**
 * Loads a BlurHash string directly into the KamsyView.
 *
 * This method provides direct BlurHash loading without any URL fallback logic.
 * It's useful when you want to show only a BlurHash placeholder or when
 * implementing custom loading logic.
 *
 * @param blurHash The BlurHash string to decode and display
 *
 * @see KamsyView.blurHash
 * @see loadAvatar
 */
fun KamsyView.loadBlurHash(blurHash: String) {
    this.blurHash = blurHash
}

/**
 * Loads an image with custom placeholder text using Coil.
 *
 * This method provides a simplified interface for loading images with text-based
 * placeholders. It's useful for non-avatar images where you want to show
 * descriptive text instead of user initials.
 *
 * @param url The URL of the image to load (can be null)
 * @param placeholderText The text to show as placeholder (uses view's placeholderText if null)
 * @param builder Lambda for custom Coil ImageRequest configuration
 *
 * @see ImageRequest.Builder
 * @see loadAvatar
 */
fun KamsyView.loadWithPlaceholder(
    url: String?,
    placeholderText: String? = null,
    builder: ImageRequest.Builder.() -> Unit = {}
) {
    url.takeUnless { it.isNullOrEmpty() }?.let { validUrl ->
        load(validUrl) {
            createTextPlaceholder(placeholderText)?.let { placeholderDrawable ->
                placeholder(placeholderDrawable)
                error(placeholderDrawable)
            }
            crossfade(true)
            builder()
        }
    } ?: showTextPlaceholder(placeholderText)
}

/**
 * Builds the complete image URL by intelligently handling complete URLs vs relative paths.
 *
 * ## URL Detection Logic
 * - URLs starting with "http://" or "https://" are considered complete
 * - Complete URLs are returned as-is (baseUrl is ignored)
 * - Relative paths are prepended with baseUrl if provided
 * - Empty baseUrl with relative path returns the relative path unchanged
 *
 * ## Examples
 * ```kotlin
 * buildImageUrl("https://example.com/image.jpg", "https://api.com/")
 * // → "https://example.com/image.jpg"
 *
 * buildImageUrl("image.jpg", "https://api.com/uploads/")
 * // → "https://api.com/uploads/image.jpg"
 *
 * buildImageUrl("image.jpg", "")
 * // → "image.jpg"
 * ```
 *
 * @param imageUrl The image URL to process (complete or relative)
 * @param baseUrl The base URL to prepend for relative paths
 * @return The complete URL ready for loading
 *
 * @author Mbuodile Obiosio
 * @since 1.0.0
 */
private fun buildImageUrl(imageUrl: String, baseUrl: String): String {
    return when {
        imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> {
            // Complete URL - use as-is
            imageUrl
        }
        baseUrl.isNotEmpty() -> {
            // Relative path with base URL - combine them
            if (baseUrl.endsWith("/") || imageUrl.startsWith("/")) {
                baseUrl + imageUrl.removePrefix("/")
            } else {
                "$baseUrl/$imageUrl"
            }
        }
        else -> {
            // Relative path without base URL - use as-is
            imageUrl
        }
    }
}

/**
 * Shows appropriate fallback placeholder based on whether a name is available.
 *
 * This function intelligently chooses between initials and generic placeholders:
 * - **With name**: Shows initials placeholder (user avatars, profiles)
 * - **Without name**: Shows generic placeholder (notifications, content lists)
 *
 * ## Use Cases
 * - **User Avatars**: When name is provided, show user initials
 * - **Notifications**: When name is null, show generic icon/text
 * - **Content Lists**: For non-personal content, show simple placeholder
 * - **System Images**: For app-generated content, avoid showing initials
 *
 * ## Examples
 * ```kotlin
 * // User avatar context - shows initials
 * showFallbackPlaceholder("John Doe") // Shows "JD"
 *
 * // Notification context - shows generic placeholder
 * showFallbackPlaceholder(null) // Shows "?" or configured placeholder
 *
 * // Content list context - shows generic placeholder
 * showFallbackPlaceholder(null) // Shows view's default placeholderText
 * ```
 *
 * @param name The user's name for initials generation (null for generic placeholder)
 *
 * @see showInitialsPlaceholder for user-specific avatars
 * @see showTextPlaceholder for generic content
 *
 * @author Mbuodile Obiosio
 * @since 1.0.0
 */
private fun KamsyView.showFallbackPlaceholder(name: String?) {
    if (!name.isNullOrBlank()) {
        // Name provided - show user initials
        showInitialsPlaceholder(name)
    } else {
        // No name - show generic placeholder using view's configured text
        showTextPlaceholder(placeholderText?.toString() ?: "?")
    }
}
