package com.github.kamsyview.utils

import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.github.kamsyview.core.KamsyView
import com.github.kamsyview.core.KamsyViewConfiguration
import com.github.kamsyview.extensions.loadAvatar
import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.kamsyview.core.KamsyStyle

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */

// =================
// COMPOSE INTEROP
// =================

/**
 * Create KamsyView for Compose using AndroidView - Fixed
 */
@Composable
fun KamsyViewCompose(
    avatarUrl: String?,
    name: String?,
    baseUrl: String = "",
    blurHash: String? = null,
    modifier: Modifier = Modifier,
    configuration: (KamsyViewConfiguration.() -> Unit)? = null,
    onImageLoaded: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx: Context ->
            createKamsyView(ctx, configuration, onImageLoaded).apply {
                loadAvatarInternal(avatarUrl, name, baseUrl, blurHash, onImageLoaded)
            }
        },
        update = { view: View ->
            // Cast to KamsyView safely
            (view as? KamsyView)?.let { kamsyView ->
                kamsyView.loadAvatarInternal(avatarUrl, name, baseUrl, blurHash, onImageLoaded)
            }
        }
    )
}

/**
 * Simplified Compose avatar component
 */
@Composable
fun KamsyAvatar(
    avatarUrl: String?,
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    baseUrl: String = "",
    blurHash: String? = null,
    enableVolumetric: Boolean = false,
    enableAnimations: Boolean = false,
    onImageLoaded: ((Boolean) -> Unit)? = null
) {
    val density = LocalDensity.current
    val borderWidthPx = with(density) { borderWidth.toPx().toInt() }

    val configuration = remember(borderWidthPx, borderColor, enableVolumetric, enableAnimations) {
        { config: KamsyViewConfiguration ->
            config.border {
                width(borderWidthPx)
                color(borderColor.toArgb())
            }
            config.volumetric {
                if (enableVolumetric) all() else none()
            }
            if (!enableAnimations) {
                config.animations {
                    stopAll()
                }
            }
        }
    }

    KamsyViewCompose(
        avatarUrl = avatarUrl,
        name = name,
        baseUrl = baseUrl,
        blurHash = blurHash,
        modifier = modifier.size(size),
        configuration = configuration,
        onImageLoaded = onImageLoaded
    )
}

/**
 * Material 3 styled avatar
 */
@Composable
fun KamsyAvatarMaterial3(
    avatarUrl: String?,
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    baseUrl: String = "",
    blurHash: String? = null,
    onImageLoaded: ((Boolean) -> Unit)? = null
) {
    KamsyAvatar(
        avatarUrl = avatarUrl,
        name = name,
        modifier = modifier,
        size = size,
        borderWidth = 2.dp,
        borderColor = Color(0xFF6750A4), // Material 3 primary
        baseUrl = baseUrl,
        blurHash = blurHash,
        enableVolumetric = true,
        onImageLoaded = onImageLoaded
    )
}

/**
 * Gaming style avatar for special use cases
 */
@Composable
fun KamsyAvatarGaming(
    avatarUrl: String?,
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    baseUrl: String = "",
    blurHash: String? = null,
    onImageLoaded: ((Boolean) -> Unit)? = null
) {
    val configuration = remember {
        { config: KamsyViewConfiguration ->
            config.style(KamsyStyle.Gaming)
            config.animations {
                borderRotation(3000L)
                volumetricPulse(cycles = 5)
            }
        }
    }

    KamsyViewCompose(
        avatarUrl = avatarUrl,
        name = name,
        baseUrl = baseUrl,
        blurHash = blurHash,
        modifier = modifier.size(size),
        configuration = configuration,
        onImageLoaded = onImageLoaded
    )
}

// =================
// HELPER FUNCTIONS
// =================

/**
 * Create KamsyView instance for Compose
 */
private fun createKamsyView(
    context: Context,
    configuration: (KamsyViewConfiguration.() -> Unit)?,
    onImageLoaded: ((Boolean) -> Unit)?
): KamsyView {
    return KamsyView(context).apply {
        configuration?.let {
            configure(it)
        }
    }
}

/**
 * Internal avatar loading without Coil dependencies
 */
private fun KamsyView.loadAvatarInternal(
    avatarUrl: String?,
    name: String?,
    baseUrl: String = "",
    blurHash: String? = null,
    onImageLoaded: ((Boolean) -> Unit)? = null
) {
    try {
        // Try to use the extension if available
        loadAvatar(avatarUrl, name, baseUrl, blurHash)
        onImageLoaded?.invoke(true)
    } catch (e: Exception) {
        // Fallback to basic functionality
        when {
            !avatarUrl.isNullOrEmpty() -> {
                val fullUrl = if (baseUrl.isNotEmpty()) baseUrl + avatarUrl else avatarUrl
                try {
                    // Try basic loading
                    loadBasicAvatar(fullUrl, name)
                    onImageLoaded?.invoke(true)
                } catch (loadException: Exception) {
                    showFallbackPlaceholder(name)
                    onImageLoaded?.invoke(false)
                }
            }
            !blurHash.isNullOrEmpty() -> {
                try {
                    this.blurHash = blurHash
                    onImageLoaded?.invoke(true)
                } catch (blurException: Exception) {
                    showFallbackPlaceholder(name)
                    onImageLoaded?.invoke(false)
                }
            }
            else -> {
                showFallbackPlaceholder(name)
                onImageLoaded?.invoke(false)
            }
        }
    }
}

/**
 * Basic avatar loading without external dependencies
 */
private fun KamsyView.loadBasicAvatar(url: String, name: String?) {
    // This would need to be implemented based on your available image loading library
    // For now, show placeholder
    showFallbackPlaceholder(name)
}

/**
 * Show fallback placeholder
 */
private fun KamsyView.showFallbackPlaceholder(name: String?) {
    val initials = getInitialsBasic(name) ?: "?"
    placeholderText = initials
    setImageDrawable(null)
}

/**
 * Basic initials extraction without external dependencies
 */
private fun getInitialsBasic(name: String?): String? = name
    ?.trim()
    ?.split(' ')
    ?.filter { it.isNotBlank() }
    ?.take(2)
    ?.joinToString("") { it.first().uppercaseChar().toString() }
    ?.takeIf { it.isNotBlank() }

// =================
// COMPOSE PREVIEWS
// =================

/**
 * Preview for KamsyAvatar
 */
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun KamsyAvatarPreview() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Simple avatar
        KamsyAvatar(
            avatarUrl = null,
            name = "John Doe",
            size = 60.dp
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

        // Material 3 avatar
        KamsyAvatarMaterial3(
            avatarUrl = null,
            name = "Jane Smith",
            size = 60.dp
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

        // Gaming avatar
        KamsyAvatarGaming(
            avatarUrl = null,
            name = "Player One",
            size = 80.dp
        )
    }
}

// =================
// STATE MANAGEMENT
// =================

/**
 * Avatar state for Compose
 */
data class AvatarState(
    val url: String? = null,
    val name: String? = null,
    val blurHash: String? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false
)

/**
 * Stateful avatar component
 */
@Composable
fun KamsyAvatarStateful(
    state: AvatarState,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    baseUrl: String = "",
    enableVolumetric: Boolean = false,
    onStateChanged: ((AvatarState) -> Unit)? = null
) {
    KamsyAvatar(
        avatarUrl = state.url,
        name = state.name,
        modifier = modifier,
        size = size,
        borderWidth = borderWidth,
        borderColor = borderColor,
        baseUrl = baseUrl,
        blurHash = state.blurHash,
        enableVolumetric = enableVolumetric,
        onImageLoaded = { success ->
            onStateChanged?.invoke(
                state.copy(
                    isLoading = false,
                    hasError = !success
                )
            )
        }
    )
}

// =================
// GRID LAYOUTS
// =================

/**
 * Avatar grid for multiple users
 */
@Composable
fun KamsyAvatarGrid(
    users: List<Pair<String?, String?>>, // (avatarUrl, name) pairs
    modifier: Modifier = Modifier,
    columns: Int = 3,
    avatarSize: Dp = 48.dp,
    spacing: Dp = 8.dp,
    baseUrl: String = "",
    onAvatarClick: ((Int) -> Unit)? = null
) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing)
    ) {
        items(users.size) { index ->
            val (avatarUrl, name) = users[index]

            androidx.compose.foundation.clickable.clickable(
                enabled = onAvatarClick != null
            ) {
                onAvatarClick?.invoke(index)
            }.let { clickableModifier ->
                KamsyAvatar(
                    avatarUrl = avatarUrl,
                    name = name,
                    baseUrl = baseUrl,
                    size = avatarSize,
                    modifier = clickableModifier
                )
            }
        }
    }
}

// =================
// ANIMATION HELPERS
// =================

/**
 * Animated avatar with state transitions
 */
@Composable
fun KamsyAvatarAnimated(
    avatarUrl: String?,
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    baseUrl: String = "",
    blurHash: String? = null,
    animateChanges: Boolean = true
) {
    val animatedSize by androidx.compose.animation.core.animateDpAsState(
        targetValue = size,
        animationSpec = androidx.compose.animation.core.spring(),
        label = "avatar_size"
    )

    if (animateChanges) {
        androidx.compose.animation.AnimatedContent(
            targetState = avatarUrl to name,
            transitionSpec = {
                androidx.compose.animation.fadeIn() with androidx.compose.animation.fadeOut()
            },
            label = "avatar_content"
        ) { (url, displayName) ->
            KamsyAvatar(
                avatarUrl = url,
                name = displayName,
                modifier = modifier,
                size = animatedSize,
                baseUrl = baseUrl,
                blurHash = blurHash
            )
        }
    } else {
        KamsyAvatar(
            avatarUrl = avatarUrl,
            name = name,
            modifier = modifier,
            size = animatedSize,
            baseUrl = baseUrl,
            blurHash = blurHash
        )
    }
}
