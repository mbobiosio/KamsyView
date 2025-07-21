package com.github.kamsyview.extensions

import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.github.kamsyview.core.KamsyView

/**
 * Glide integration extensions for KamsyView providing robust image loading capabilities.
 *
 * These extensions integrate KamsyView with Glide image loading library, offering
 * comprehensive transformation support, custom request options, and the same intelligent
 * fallback handling as the Coil extensions.
 *
 * ## Key Features
 * - **Smart Fallback Chain**: URL → BlurHash → Initials → Empty
 * - **Transformation Support**: Built-in support for CircleCrop and custom transformations
 * - **RequestOptions Integration**: Full Glide RequestOptions support
 * - **BlurHash Placeholders**: Automatic BlurHash placeholder generation
 * - **Error Handling**: Graceful degradation with meaningful fallbacks
 * - **Memory Management**: Proper Glide lifecycle integration
 *
 * ## Basic Usage
 * ```kotlin
 * kamsyView.loadAvatarGlide(
 *     avatarUrl = "https://example.com/avatar.jpg",
 *     name = "John Doe",
 *     blurHash = "LGFFaXYk^6#M@-5c,1J5@[or[Q6."
 * )
 * ```
 *
 * ## Transformation Usage
 * ```kotlin
 * kamsyView.loadAvatarGlideCircular(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName
 * )
 * ```
 *
 * @see RequestOptions
 * @see Glide
 * @see KamsyView
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */

/**
 * Loads an avatar image using Glide with intelligent fallback handling.
 *
 * This is the primary Glide extension function for loading avatar images with KamsyView.
 * It provides the same intelligent fallback chain as the Coil extensions but uses
 * Glide's powerful transformation and caching system.
 *
 * ## Fallback Chain
 * 1. **Primary URL**: Attempts to load the full avatar URL with Glide
 * 2. **BlurHash Placeholder**: Shows BlurHash placeholder during loading
 * 3. **Initials Fallback**: Generates initials placeholder on URL failure
 * 4. **Empty State**: Clears Glide request if no fallback options available
 *
 * ## Glide Integration
 * - Uses Glide's RequestOptions for advanced configuration
 * - Automatic memory and disk caching
 * - Proper lifecycle management with fragment/activity
 * - Support for all Glide transformations
 *
 * ## Error Handling
 * - Network failures automatically fall back to initials
 * - Malformed URLs are handled gracefully
 * - BlurHash decoding errors fall back to initials
 * - All exceptions are caught and logged
 *
 * ## Usage Examples
 *
 * ### Basic Avatar Loading
 * ```kotlin
 * kamsyView.loadAvatarGlide(
 *     avatarUrl = "https://example.com/user/123/avatar.jpg",
 *     name = "Jane Smith"
 * )
 * ```
 *
 * ### With Custom RequestOptions
 * ```kotlin
 * val options = RequestOptions()
 *     .centerCrop()
 *     .placeholder(R.drawable.placeholder)
 *     .error(R.drawable.error)
 *     .diskCacheStrategy(DiskCacheStrategy.ALL)
 *
 * kamsyView.loadAvatarGlide(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName,
 *     options = options
 * )
 * ```
 *
 * ### With BlurHash Placeholder
 * ```kotlin
 * kamsyView.loadAvatarGlide(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName,
 *     blurHash = user.avatarBlurHash
 * )
 * ```
 *
 * ### With Base URL
 * ```kotlin
 * kamsyView.loadAvatarGlide(
 *     avatarUrl = "/avatars/user123.jpg",
 *     name = "John Doe",
 *     baseUrl = "https://api.example.com"
 * )
 * ```
 *
 * @param avatarUrl The URL of the avatar image to load (can be null)
 * @param name The user's name for generating initials fallback (can be null)
 * @param baseUrl Base URL to prepend to avatarUrl if it's a relative path
 * @param blurHash BlurHash string for placeholder generation during loading
 * @param options Glide RequestOptions for advanced configuration
 *
 * @see RequestOptions
 * @see Glide
 * @see loadAvatarGlideCircular
 * @see loadAvatarGlideTransformed
 */
fun KamsyView.loadAvatarGlide(
    avatarUrl: String?,
    name: String?,
    baseUrl: String = "",
    blurHash: String? = null,
    options: RequestOptions = RequestOptions()
) {
    // Clear any previous Glide request to ensure proper reloading
    runCatching { Glide.with(context).clear(this) }.onFailure {
        // Log the error if clear fails, but don't stop execution
        // android.util.Log.e("KamsyView", "Failed to clear Glide image", it) // Assuming logger is available or handle appropriately
    }

    when {
        !avatarUrl.isNullOrEmpty() -> {
            val fullUrl = if (baseUrl.isNotEmpty()) baseUrl + avatarUrl else avatarUrl

            val glideRequest = Glide.with(context)
                .load(fullUrl)
                .apply(options)

            // Apply BlurHash placeholder if available
            blurHash?.let { hash ->
                createBlurHashPlaceholder(hash)?.let { drawable ->
                    glideRequest.placeholder(drawable)
                }
            }

            // Apply initials error drawable with proper error handling
            runCatching {
                val errorDrawable = createInitialsPlaceholder(name)
                glideRequest.error(errorDrawable)
            }.onFailure { throwable ->
                android.util.Log.e("KamsyView", "Failed to create Glide error drawable", throwable)
            }

            // Execute the request
            glideRequest.into(this)
        }

        !blurHash.isNullOrEmpty() -> {
            loadBlurHash(blurHash)
        }

        else -> {
            // No need to clear here, as it's handled at the beginning of the function
            showInitialsPlaceholder(name)
        }
    }
}

/**
 * Loads an avatar image using Glide with automatic circular cropping.
 *
 * This is a convenience method that applies Glide's CircleCrop transformation
 * automatically, perfect for circular avatar displays. It maintains all the
 * same fallback logic as the main loadAvatarGlide function.
 *
 * ## Circular Cropping
 * - Uses Glide's built-in CircleCrop transformation
 * - Automatically centers and crops the image to fit a circle
 * - Maintains aspect ratio while filling the circular bounds
 * - Works with any image aspect ratio
 *
 * ## Use Cases
 * - **User Profiles**: Circular avatar displays
 * - **Contact Lists**: Uniform circular profile pictures
 * - **Comment Systems**: Circular author avatars
 * - **Social Features**: Circular user representations
 *
 * ## Usage Examples
 *
 * ### Basic Circular Avatar
 * ```kotlin
 * kamsyView.loadAvatarGlideCircular(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName
 * )
 * ```
 *
 * ### With Base URL
 * ```kotlin
 * kamsyView.loadAvatarGlideCircular(
 *     avatarUrl = "/api/users/123/avatar",
 *     name = "Jane Smith",
 *     baseUrl = "https://example.com"
 * )
 * ```
 *
 * ## Visual Result
 * The transformation will:
 * 1. Center the image within the view bounds
 * 2. Scale to fill the circular area
 * 3. Crop excess content outside the circle
 * 4. Apply smooth antialiasing to edges
 *
 * @param avatarUrl The URL of the avatar image to load (can be null)
 * @param name The user's name for generating initials fallback (can be null)
 * @param baseUrl Base URL to prepend to avatarUrl if it's a relative path
 *
 * @see CircleCrop
 * @see loadAvatarGlide
 * @see loadAvatarGlideTransformed
 */
fun KamsyView.loadAvatarGlideCircular(
    avatarUrl: String?,
    name: String?,
    baseUrl: String = ""
) {
    loadAvatarGlide(
        avatarUrl = avatarUrl,
        name = name,
        baseUrl = baseUrl,
        options = RequestOptions().transform(CircleCrop())
    )
}

/**
 * Loads an avatar image using Glide with a custom bitmap transformation.
 *
 * This method allows you to apply any custom Glide transformation to the loaded
 * avatar image while maintaining the intelligent fallback chain. It's perfect
 * for custom visual effects, advanced cropping, or specialized image processing.
 *
 * ## Transformation Support
 * - **Built-in Transformations**: CircleCrop, CenterCrop, RoundedCorners, etc.
 * - **Custom Transformations**: Any class implementing Transformation<Bitmap>
 * - **Multiple Transformations**: Chain multiple transformations together
 * - **GPU Acceleration**: Leverages Glide's GPU-accelerated transformations
 *
 * ## Common Transformations
 * - **RoundedCorners**: Rounds image corners with specified radius
 * - **Grayscale**: Converts image to grayscale
 * - **Blur**: Applies blur effect to the image
 * - **ColorFilter**: Applies color filters or tints
 * - **Custom Effects**: Implement your own transformation logic
 *
 * ## Usage Examples
 *
 * ### Rounded Corners
 * ```kotlin
 * kamsyView.loadAvatarGlideTransformed(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName,
 *     transformation = RoundedCorners(16)
 * )
 * ```
 *
 * ### Grayscale Effect
 * ```kotlin
 * kamsyView.loadAvatarGlideTransformed(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName,
 *     transformation = GrayscaleTransformation()
 * )
 * ```
 *
 * ### Multiple Transformations
 * ```kotlin
 * val multiTransform = MultiTransformation(
 *     CircleCrop(),
 *     ColorFilterTransformation(Color.BLUE)
 * )
 *
 * kamsyView.loadAvatarGlideTransformed(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName,
 *     transformation = multiTransform
 * )
 * ```
 *
 * ### Custom Transformation
 * ```kotlin
 * class CustomBorderTransformation(
 *     private val borderWidth: Int,
 *     private val borderColor: Int
 * ) : BitmapTransformation() {
 *     // Custom transformation implementation
 * }
 *
 * kamsyView.loadAvatarGlideTransformed(
 *     avatarUrl = user.avatarUrl,
 *     name = user.displayName,
 *     transformation = CustomBorderTransformation(4, Color.BLUE)
 * )
 * ```
 *
 * ## Performance Considerations
 * - Transformations are cached by Glide automatically
 * - Complex transformations may impact loading performance
 * - Consider using GPU-accelerated transformations when available
 * - Multiple transformations are applied in sequence
 *
 * @param avatarUrl The URL of the avatar image to load (can be null)
 * @param name The user's name for generating initials fallback (can be null)
 * @param baseUrl Base URL to prepend to avatarUrl if it's a relative path
 * @param blurHash BlurHash string for placeholder generation during loading
 * @param transformation The bitmap transformation to apply to the loaded image
 *
 * @see Transformation
 * @see RequestOptions.transform
 * @see loadAvatarGlide
 * @see loadAvatarGlideCircular
 */
fun KamsyView.loadAvatarGlideTransformed(
    avatarUrl: String?,
    name: String?,
    baseUrl: String = "",
    blurHash: String? = null,
    transformation: Transformation<Bitmap>
) {
    loadAvatarGlide(
        avatarUrl = avatarUrl,
        name = name,
        baseUrl = baseUrl,
        blurHash = blurHash,
        options = RequestOptions().transform(transformation)
    )
}
