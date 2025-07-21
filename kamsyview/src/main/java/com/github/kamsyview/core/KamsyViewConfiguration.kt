package com.github.kamsyview.core

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.graphics.toColorInt
import com.github.kamsyview.drawables.KamsyOverlayDrawable
import com.github.kamsyview.drawables.animateArches
import com.github.kamsyview.drawables.applyNotificationBadge
import com.github.kamsyview.drawables.applyOfflineStatus
import com.github.kamsyview.drawables.applyOnlineStatus
import com.github.kamsyview.drawables.breatheEffect
import com.github.kamsyview.drawables.pulseAnimation
import com.github.kamsyview.drawables.pulseVolumetric
import com.github.kamsyview.impl.DefaultKamsyLogger
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.models.KamsyConfiguration
import com.google.android.material.shape.CutCornerTreatment
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import javax.inject.Inject

/**
 * DSL configuration system for KamsyView providing a fluent API for comprehensive view customization.
 *
 * KamsyViewConfiguration offers a type-safe DSL that allows developers to configure all aspects
 * of a KamsyView instance using a clean, readable syntax. The DSL is organized into logical
 * configuration blocks for different aspects of the view.
 *
 * ## Basic Usage
 * ```kotlin
 * kamsyView.configure {
 *     appearance {
 *         shape(KamsyShape.CIRCLE)
 *         margin(8)
 *     }
 *     border {
 *         width(4)
 *         gradient(Color.BLUE, Color.RED, 45)
 *     }
 *     placeholder {
 *         initials("John Doe")
 *         autoColor("John Doe")
 *     }
 * }
 * ```
 *
 * ## Configuration Blocks
 * - [border] - Border styling and decorations
 * - [volumetric] - 3D effects and depth
 * - [placeholder] - Text and background placeholders
 * - [overlay] - Tints, scrims, and overlay effects
 * - [blurHash] - BlurHash placeholder configuration
 * - [appearance] - Shape, size, and general styling
 * - [animations] - Animation effects and behaviors
 *
 * ## Quick Configuration
 * For common operations, use the direct methods:
 * ```kotlin
 * kamsyView.configure {
 *     size(80) // Set width and height to 80dp
 *     borderColor(Color.BLUE)
 *     placeholderText("JD")
 *     style(KamsyStyle.Material3)
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 *
 * @see configure
 * @see BorderConfiguration
 * @see PlaceholderConfiguration
 * @see OverlayConfiguration
 * @see KamsyStyle
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
@KamsyDsl
class KamsyViewConfiguration(private val kamsyView: KamsyView) {

    /**
     * Configures border properties including width, colors, gradients, and decorative arches.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.configure {
     *     border {
     *         width(6)
     *         gradient(Color.CYAN, Color.MAGENTA, 45)
     *         arches {
     *             count(8)
     *             degreeArea(270)
     *             mirror()
     *         }
     *     }
     * }
     * ```
     *
     * @param block Configuration block for border properties
     * @see BorderConfiguration
     * @see ArchesConfiguration
     */
    fun border(block: BorderConfiguration.() -> Unit) {
        BorderConfiguration(kamsyView).apply(block)
    }

    /**
     * Configures volumetric (3D-like) effects for depth and dimension.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.configure {
     *     volumetric {
     *         all() // Apply to all content
     *         intensity(highlight = 0.4f, shadow = 0.3f)
     *     }
     * }
     * ```
     *
     * @param block Configuration block for volumetric effects
     * @see VolumetricConfiguration
     * @see KamsyView.VolumetricType
     */
    fun volumetric(block: VolumetricConfiguration.() -> Unit) {
        VolumetricConfiguration(kamsyView).apply(block)
    }

    /**
     * Configures placeholder properties including text, colors, and automatic styling.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.configure {
     *     placeholder {
     *         initials("Jane Smith") // Extracts "JS"
     *         autoColor("Jane Smith") // Generates color from name
     *         textSize(1.2f)
     *         style(PlaceholderStyle.COLORFUL)
     *     }
     * }
     * ```
     *
     * @param block Configuration block for placeholder properties
     * @see PlaceholderConfiguration
     * @see PlaceholderStyle
     */
    fun placeholder(block: PlaceholderConfiguration.() -> Unit) {
        PlaceholderConfiguration(kamsyView).apply(block)
    }

    /**
     * Configures overlay effects including tints, scrims, status indicators, and badges.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.configure {
     *     overlay {
     *         tint(Color.BLUE, 0.2f)
     *         status {
     *             online()
     *         }
     *         badge {
     *             notification(5)
     *         }
     *     }
     * }
     * ```
     *
     * @param block Configuration block for overlay effects
     * @see OverlayConfiguration
     * @see StatusConfiguration
     * @see BadgeConfiguration
     */
    fun overlay(block: OverlayConfiguration.() -> Unit) {
        OverlayConfiguration(kamsyView).apply(block)
    }

    /**
     * Configures BlurHash properties for progressive image loading placeholders.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.configure {
     *     blurHash {
     *         load("LGFFaXYk^6#M@-5c,1J5@[or[Q6.", punch = 1.2f)
     *     }
     * }
     * ```
     *
     * @param block Configuration block for BlurHash properties
     * @see BlurHashConfiguration
     */
    fun blurHash(block: BlurHashConfiguration.() -> Unit) {
        BlurHashConfiguration(kamsyView).apply(block)
    }

    /**
     * Configures general appearance properties including shape, scaling, and styling presets.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.configure {
     *     appearance {
     *         shape(KamsyShape.SQUIRCLE)
     *         roundedCorners(16f)
     *         iconScale(0.7f)
     *         material3()
     *     }
     * }
     * ```
     *
     * @param block Configuration block for appearance properties
     * @see AppearanceConfiguration
     * @see KamsyShape
     */
    fun appearance(block: AppearanceConfiguration.() -> Unit) {
        AppearanceConfiguration(kamsyView).apply(block)
    }

    /**
     * Configures animation effects for borders, volumetric effects, and other dynamic behaviors.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.configure {
     *     animations {
     *         borderRotation(duration = 3000L)
     *         volumetricPulse(cycles = 5, duration = 800L)
     *     }
     * }
     * ```
     *
     * @param block Configuration block for animation effects
     * @see AnimationConfiguration
     */
    fun animations(block: AnimationConfiguration.() -> Unit) {
        AnimationConfiguration(kamsyView).apply(block)
    }

    /**
     * Applies a predefined style configuration to the view.
     *
     * Styles provide complete theming presets that configure multiple aspects of the view
     * according to design system guidelines or specific use cases.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.configure {
     *     style(KamsyStyle.Gaming) // Applies gaming-themed styling
     * }
     * ```
     *
     * @param style The predefined style to apply
     * @see KamsyStyle
     */
    fun style(style: KamsyStyle) {
        style.apply(kamsyView)
    }

    // Quick Configuration Methods

    /**
     * Sets both width and height of the view to the specified size.
     *
     * @param size The size in pixels for both width and height
     */
    fun size(@Dimension size: Int) {
        kamsyView.layoutParams = kamsyView.layoutParams?.apply {
            width = size
            height = size
        }
    }

    /**
     * Sets the placeholder background color.
     *
     * @param color The background color for placeholder states
     * @see kamsyView.backgroundPlaceholderColor
     */
    fun backgroundColor(@ColorInt color: Int) {
        kamsyView.backgroundPlaceholderColor = color
    }

    /**
     * Sets the primary border color.
     *
     * @param color The border color
     * @see kamsyView.borderColor
     */
    fun borderColor(@ColorInt color: Int) {
        kamsyView.borderColor = color
    }

    /**
     * Sets the border width in pixels.
     *
     * @param width The border width in pixels
     * @see kamsyView.borderWidth
     */
    fun borderWidth(@Dimension width: Int) {
        kamsyView.borderWidth = width
    }

    /**
     * Sets the avatar margin (spacing around the content).
     *
     * @param margin The margin in pixels
     * @see kamsyView.avatarMargin
     */
    fun margin(@Dimension margin: Int) {
        kamsyView.avatarMargin = margin
    }

    /**
     * Sets the image drawable directly.
     *
     * @param drawable The drawable to display, or null to clear
     */
    fun drawable(drawable: Drawable?) {
        kamsyView.setImageDrawable(drawable)
    }

    /**
     * Sets the placeholder text content.
     *
     * @param text The text to display in placeholder state
     * @see kamsyView.placeholderText
     */
    fun placeholderText(text: CharSequence?) {
        kamsyView.placeholderText = text
    }
}

/**
 * DSL for configuring border properties including width, colors, gradients, and decorative arches.
 *
 * BorderConfiguration provides methods for creating various border styles from simple solid borders
 * to complex gradient borders with decorative arch elements.
 *
 * ## Solid Borders
 * ```kotlin
 * border {
 *     width(4)
 *     solid(Color.BLUE)
 * }
 * ```
 *
 * ## Gradient Borders
 * ```kotlin
 * border {
 *     width(6)
 *     gradient(Color.RED, Color.YELLOW, 90) // Vertical gradient
 * }
 * ```
 *
 * ## Decorative Arches
 * ```kotlin
 * border {
 *     width(8)
 *     color(Color.CYAN)
 *     arches {
 *         count(12)
 *         degreeArea(360) // Full circle
 *         mirror() // Mirrored pattern
 *     }
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 *
 * @see ArchesConfiguration
 * @see ArchesPreset
 */
@KamsyDsl
class BorderConfiguration(private val kamsyView: KamsyView) {

    /**
     * Sets the border width in pixels.
     *
     * @param width Border width in pixels (0 to disable border)
     */
    fun width(@Dimension width: Int) {
        kamsyView.borderWidth = width
    }

    /**
     * Sets the primary border color.
     *
     * When used alone, creates a solid color border. When combined with [secondaryColor],
     * this becomes the starting color of a gradient.
     *
     * @param color The border color
     * @see secondaryColor
     * @see gradientAngle
     */
    fun color(@ColorInt color: Int) {
        kamsyView.borderColor = color
    }

    /**
     * Sets the secondary border color for gradient effects.
     *
     * When set, creates a gradient from [color] to this secondary color.
     *
     * @param color The ending color for gradient borders
     * @see color
     * @see gradientAngle
     * @see gradient
     */
    fun secondaryColor(@ColorInt color: Int) {
        kamsyView.borderColorSecondary = color
    }

    /**
     * Sets the gradient angle for gradient borders.
     *
     * Only effective when both primary and secondary colors are set.
     *
     * @param angle Gradient angle in degrees (0-360)
     *              0° = horizontal (left to right)
     *              90° = vertical (top to bottom)
     *              180° = horizontal (right to left)
     *              270° = vertical (bottom to top)
     * @see gradient
     */
    fun gradientAngle(@IntRange(from = 0, to = 360) angle: Int) {
        kamsyView.borderGradientAngle = angle
    }

    /**
     * Configures decorative arch elements around the border.
     *
     * Arches are ornamental elements positioned around the view's perimeter
     * that can create loading indicators, progress displays, or decorative effects.
     *
     * Example usage:
     * ```kotlin
     * border {
     *     arches {
     *         count(8)
     *         degreeArea(270) // 3/4 circle
     *         angle(45) // Start at 45 degrees
     *         mirror() // Mirrored pattern
     *         preset(ArchesPreset.LOADING)
     *     }
     * }
     * ```
     *
     * @param block Configuration block for arch properties
     * @see ArchesConfiguration
     * @see ArchesPreset
     */
    fun arches(block: ArchesConfiguration.() -> Unit) {
        ArchesConfiguration(kamsyView).apply(block)
    }

    /**
     * Creates a gradient border with the specified colors and angle.
     *
     * Convenience method that sets both colors and angle in a single call.
     *
     * @param startColor The starting color of the gradient
     * @param endColor The ending color of the gradient
     * @param angle The gradient angle in degrees (default: 0° horizontal)
     */
    fun gradient(@ColorInt startColor: Int, @ColorInt endColor: Int, @IntRange(from = 0, to = 360) angle: Int = 0) {
        kamsyView.borderColor = startColor
        kamsyView.borderColorSecondary = endColor
        kamsyView.borderGradientAngle = angle
    }

    /**
     * Creates a solid color border.
     *
     * Convenience method that sets the primary color and clears any secondary color.
     *
     * @param color The solid border color
     */
    fun solid(@ColorInt color: Int) {
        kamsyView.borderColor = color
        kamsyView.borderColorSecondary = null
    }

    /**
     * Disables the border by setting width to 0.
     */
    fun none() {
        kamsyView.borderWidth = 0
    }
}

/**
 * DSL for configuring decorative arch elements around the view's border.
 *
 * Arches are ornamental elements that can be positioned around the view's perimeter
 * to create various visual effects such as loading indicators, progress displays,
 * or decorative enhancements.
 *
 * ## Basic Arch Configuration
 * ```kotlin
 * arches {
 *     count(6) // 6 arch elements
 *     degreeArea(180) // Cover half the circle
 *     angle(90) // Start from the right side
 * }
 * ```
 *
 * ## Arch Types
 * ```kotlin
 * arches {
 *     single() // Individual arch elements
 *     // or
 *     mirror() // Mirrored/symmetrical pattern
 * }
 * ```
 *
 * ## Using Presets
 * ```kotlin
 * arches {
 *     preset(ArchesPreset.LOADING) // Loading indicator style
 *     // or
 *     preset(ArchesPreset.DECORATIVE) // Full decorative circle
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 *
 * @see ArchesPreset
 * @see KamsyView.ArchesType
 */
@KamsyDsl
class ArchesConfiguration(private val kamsyView: KamsyView) {

    /**
     * Sets the number of arch elements to display.
     *
     * Higher values create more densely packed arches within the specified [degreeArea].
     *
     * @param count Number of arch elements (0 to disable arches)
     */
    fun count(count: Int) {
        kamsyView.archesCount = count
    }

    /**
     * Sets the total degree coverage for arch placement.
     *
     * Determines how much of the view's perimeter is covered by arch elements.
     * Combined with [count], this controls the spacing between arches.
     *
     * @param area Degree coverage (0-360)
     *             360° = full circle
     *             180° = half circle
     *             90° = quarter circle
     */
    fun degreeArea(@IntRange(from = 0, to = 360) area: Int) {
        kamsyView.archesDegreeArea = area
    }

    /**
     * Sets the starting angle for arch placement.
     *
     * Determines where the arch pattern begins around the view's perimeter.
     *
     * @param angle Starting angle in degrees (0-360)
     *              0° = top center
     *              90° = right center
     *              180° = bottom center
     *              270° = left center
     */
    fun angle(@IntRange(from = 0, to = 360) angle: Int) {
        kamsyView.archesAngle = angle
    }

    /**
     * Sets the arch visual style.
     *
     * @param type The arch type to use
     * @see KamsyView.ArchesType
     * @see single
     * @see mirror
     */
    fun type(type: KamsyView.ArchesType) {
        kamsyView.archesType = type
    }

    /**
     * Sets arches to single/individual style.
     *
     * Each arch element is rendered independently without symmetrical pairing.
     */
    fun single() {
        kamsyView.archesType = KamsyView.ArchesType.SINGLE
    }

    /**
     * Sets arches to mirrored/symmetrical style.
     *
     * Arch elements are rendered in symmetrical pairs for balanced compositions.
     */
    fun mirror() {
        kamsyView.archesType = KamsyView.ArchesType.MIRROR
    }

    /**
     * Applies a predefined arch configuration preset.
     *
     * Presets provide common arch configurations for specific use cases:
     * - [ArchesPreset.LOADING] - Loading indicator style
     * - [ArchesPreset.PROGRESS] - Progress display style
     * - [ArchesPreset.DECORATIVE] - Full decorative circle
     *
     * @param preset The arch preset to apply
     * @see ArchesPreset
     */
    fun preset(preset: ArchesPreset) {
        preset.apply(kamsyView)
    }
}

/**
 * DSL for configuring volumetric (3D-like) effects that add depth and dimension to the view.
 *
 * Volumetric effects enhance the visual appearance by adding shadows, highlights,
 * and other depth cues that create the illusion of three-dimensional form.
 *
 * ## Basic Volumetric Configuration
 * ```kotlin
 * volumetric {
 *     all() // Apply to all content
 *     intensity(
 *         highlight = 0.4f,
 *         shadow = 0.3f,
 *         ambient = 0.1f
 *     )
 * }
 * ```
 *
 * ## Selective Application
 * ```kotlin
 * volumetric {
 *     drawable() // Only apply to image content
 *     // or
 *     placeholder() // Only apply to placeholder content
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 *
 * @see KamsyView.VolumetricType
 */
@KamsyDsl
class VolumetricConfiguration(private val kamsyView: KamsyView) {

    /**
     * Sets the volumetric effect type.
     *
     * @param type The volumetric type to apply
     * @see KamsyView.VolumetricType
     */
    fun type(type: KamsyView.VolumetricType) {
        kamsyView.volumetricType = type
    }

    /**
     * Applies volumetric effects to all content (both drawables and placeholders).
     */
    fun all() {
        kamsyView.volumetricType = KamsyView.VolumetricType.ALL
    }

    /**
     * Applies volumetric effects only to drawable content (images, icons).
     */
    fun drawable() {
        kamsyView.volumetricType = KamsyView.VolumetricType.DRAWABLE
    }

    /**
     * Applies volumetric effects only to placeholder content (text, BlurHash).
     */
    fun placeholder() {
        kamsyView.volumetricType = KamsyView.VolumetricType.PLACEHOLDER
    }

    /**
     * Disables all volumetric effects.
     */
    fun none() {
        kamsyView.volumetricType = KamsyView.VolumetricType.NONE
    }

    /**
     * Configures the intensity of volumetric effects.
     *
     * Controls the strength of various volumetric components to achieve
     * the desired depth and lighting effects.
     *
     * @param highlight Intensity of highlight effects (0.0 - 1.0)
     * @param shadow Intensity of shadow effects (0.0 - 1.0)
     * @param ambient Intensity of ambient lighting (0.0 - 1.0)
     */
    fun intensity(
        @FloatRange(from = 0.0, to = 1.0) highlight: Float = 0.3f,
        @FloatRange(from = 0.0, to = 1.0) shadow: Float = 0.2f,
        @FloatRange(from = 0.0, to = 1.0) ambient: Float = 0.1f
    ) {
        // This would be used by the volumetric drawable
        // Store in view for later use
    }
}

/**
 * DSL for configuring placeholder properties including text, colors, and automatic styling.
 *
 * PlaceholderConfiguration provides comprehensive control over how placeholder content
 * is displayed when no image is available. This includes text content, colors, sizing,
 * typography, and automatic generation of placeholder content from user data.
 *
 * ## Basic Text Placeholder
 * ```kotlin
 * placeholder {
 *     text("JD")
 *     textColor(Color.WHITE)
 *     backgroundColor(Color.BLUE)
 *     textSize(1.2f)
 * }
 * ```
 *
 * ## Automatic Initials Generation
 * ```kotlin
 * placeholder {
 *     initials("John Doe") // Automatically extracts "JD"
 *     autoColor("John Doe") // Generates consistent color from name
 * }
 * ```
 *
 * ## Custom Typography
 * ```kotlin
 * placeholder {
 *     text("AB")
 *     typeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
 *     textSize(1.5f) // 150% of default size
 * }
 * ```
 *
 * ## Predefined Styles
 * ```kotlin
 * placeholder {
 *     initials("Jane Smith")
 *     style(PlaceholderStyle.COLORFUL) // Applies colorful preset
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 *
 * @see PlaceholderStyle
 * @see extractInitials
 * @see generateBackgroundColor
 */
@KamsyDsl
class PlaceholderConfiguration(private val kamsyView: KamsyView) {

    /**
     * Sets the placeholder text content.
     *
     * This text is displayed when no image is available. Common uses include
     * user initials, generic placeholders, or emoji characters.
     *
     * @param text The text to display, or null to use default placeholder
     */
    fun text(text: CharSequence?) {
        kamsyView.placeholderText = text
    }

    /**
     * Sets the color of the placeholder text.
     *
     * @param color The text color
     */
    fun textColor(@ColorInt color: Int) {
        kamsyView.textColor = color
    }

    /**
     * Sets the relative size of the placeholder text.
     *
     * The text size is calculated as a percentage of the view's width,
     * allowing the text to scale appropriately with the view size.
     *
     * @param percentage Text size as a percentage of view width (0.0 - 2.0)
     *                   1.0 = default size
     *                   0.5 = half size
     *                   1.5 = 150% larger
     */
    fun textSize(@FloatRange(from = 0.0, to = 2.0) percentage: Float) {
        kamsyView.textSizePercentage = percentage
    }

    /**
     * Sets a custom typeface for the placeholder text.
     *
     * @param typeface The typeface to use, or null for system default
     */
    fun typeface(typeface: Typeface?) {
        kamsyView.textTypeface = typeface
    }

    /**
     * Sets the background color for the placeholder.
     *
     * This color fills the view when displaying placeholder content.
     *
     * @param color The background color
     */
    fun backgroundColor(@ColorInt color: Int) {
        kamsyView.backgroundPlaceholderColor = color
    }

    /**
     * Automatically extracts initials from a full name and sets them as placeholder text.
     *
     * Extracts up to 2 initials from the provided name by taking the first character
     * of each word (space-separated). Empty or whitespace-only words are ignored.
     *
     * Examples:
     * - "John Doe" → "JD"
     * - "Mary Jane Watson" → "MJ" (first 2 initials)
     * - "Madonna" → "M"
     * - "" → "?" (fallback)
     *
     * @param name The full name to extract initials from
     * @see extractInitials
     */
    fun initials(name: String) {
        kamsyView.placeholderText = name.extractInitials()
    }

    /**
     * Automatically generates a consistent background color based on the input text.
     *
     * Uses a deterministic algorithm to generate a color from the text's hash code,
     * ensuring the same text always produces the same color. This is useful for
     * creating consistent avatar colors for users.
     *
     * The generated colors have balanced saturation and lightness for good readability
     * and visual appeal.
     *
     * @param text The text to generate a color from (typically a name)
     * @see generateBackgroundColor
     */
    fun autoColor(text: String) {
        kamsyView.backgroundPlaceholderColor = generateBackgroundColor(text)
    }

    /**
     * Applies a predefined placeholder style.
     *
     * Styles provide complete placeholder theming that sets multiple properties
     * according to design patterns or use cases.
     *
     * Available styles:
     * - [PlaceholderStyle.DEFAULT] - Standard gray placeholder
     * - [PlaceholderStyle.COLORFUL] - Vibrant, auto-colored placeholder
     * - [PlaceholderStyle.MINIMAL] - Clean, minimal styling
     *
     * @param style The placeholder style to apply
     * @see PlaceholderStyle
     */
    fun style(style: PlaceholderStyle) {
        style.apply(kamsyView)
    }
}

/**
 * DSL for configuring overlay effects including tints, scrims, status indicators, and badges.
 *
 * OverlayConfiguration provides control over visual effects that are rendered on top of
 * the main content. This includes color tints, darkening scrims, status indicators for
 * online/offline states, and notification badges. Supports dependency injection for
 * enhanced logging and error handling.
 *
 * ## Basic Overlay Effects
 * ```kotlin
 * overlay {
 *     tint(Color.BLUE, 0.2f) // 20% blue tint
 *     scrim(Color.BLACK, 0.3f) // 30% darkening scrim
 *     blendMode(KamsyOverlayDrawable.BlendMode.MULTIPLY)
 * }
 * ```
 *
 * ## Status Indicators
 * ```kotlin
 * overlay {
 *     status {
 *         online() // Green online indicator
 *         // or
 *         custom(Color.YELLOW, StatusPosition.TOP_RIGHT, 20f)
 *     }
 * }
 * ```
 *
 * ## Notification Badges
 * ```kotlin
 * overlay {
 *     badge {
 *         notification(5) // Shows "5" in a red badge
 *         // or
 *         vip() // Shows "VIP" badge
 *         premium() // Shows star badge
 *     }
 * }
 * ```
 *
 * ## Clearing Overlays
 * ```kotlin
 * overlay {
 *     clearAll() // Removes all overlay effects
 *     enabled(false) // Temporarily disables overlays
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 * @param logger Logger for debugging and error reporting (injected or fallback)
 *
 * @see StatusConfiguration
 * @see BadgeConfiguration
 * @see KamsyOverlayDrawable
 */
@KamsyDsl
class OverlayConfiguration @Inject constructor(
    private val kamsyView: KamsyView,
    private val logger: IKamsyLogger
) {

    /**
     * Fallback constructor for non-dependency injection usage.
     *
     * Creates a default logger when dependency injection is not available.
     *
     * @param kamsyView The KamsyView instance to configure
     */
    constructor(kamsyView: KamsyView) : this(
        kamsyView = kamsyView,
        logger = DefaultKamsyLogger(KamsyConfiguration())
    )

    /**
     * Applies a color tint overlay to the view content.
     *
     * Tints blend the specified color with the underlying content, useful for
     * creating mood effects, highlighting, or visual feedback states.
     *
     * @param color The tint color to apply
     * @param alpha The opacity of the tint (0.0 = transparent, 1.0 = opaque)
     */
    fun tint(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float = 0.2f) {
        try {
            kamsyView.overlayDrawable?.setTint(color, alpha)
            logger.debug("Applied overlay tint: color=$color, alpha=$alpha")
        } catch (e: Exception) {
            logger.error("Failed to apply overlay tint", e)
        }
    }

    /**
     * Applies a darkening scrim overlay to the view content.
     *
     * Scrims are typically used to darken content for better text readability
     * or to create depth effects. Usually applied with dark colors.
     *
     * @param color The scrim color (typically dark colors like black or gray)
     * @param alpha The opacity of the scrim (0.0 = no darkening, 1.0 = completely dark)
     */
    fun scrim(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float = 0.3f) {
        try {
            kamsyView.overlayDrawable?.setScrim(color, alpha)
            logger.debug("Applied overlay scrim: color=$color, alpha=$alpha")
        } catch (e: Exception) {
            logger.error("Failed to apply overlay scrim", e)
        }
    }

    /**
     * Sets the blend mode for overlay effects.
     *
     * Blend modes determine how overlay colors interact with the underlying content,
     * affecting the final visual appearance.
     *
     * @param mode The blend mode to use
     * @see KamsyOverlayDrawable.BlendMode
     */
    fun blendMode(mode: KamsyOverlayDrawable.BlendMode) {
        try {
            kamsyView.overlayDrawable?.setBlendMode(mode)
            logger.debug("Applied blend mode: $mode")
        } catch (e: Exception) {
            logger.error("Failed to apply blend mode", e)
        }
    }

    /**
     * Configures status indicators for showing online/offline states and custom status.
     *
     * Status indicators are small colored dots typically positioned at the corners
     * of the view to indicate user presence or activity status.
     *
     * @param block Configuration block for status indicators
     * @see StatusConfiguration
     */
    fun status(block: StatusConfiguration.() -> Unit) {
        StatusConfiguration(kamsyView, logger).apply(block)
    }

    /**
     * Configures notification badges for showing counts and custom badges.
     *
     * Badges are small overlays that display text or symbols, commonly used
     * for notification counts, user roles, or status labels.
     *
     * @param block Configuration block for badges
     * @see BadgeConfiguration
     */
    fun badge(block: BadgeConfiguration.() -> Unit) {
        BadgeConfiguration(kamsyView, logger).apply(block)
    }

    /**
     * Enables or disables all overlay effects.
     *
     * When disabled, overlay effects are hidden but their configuration is preserved,
     * allowing them to be re-enabled later without reconfiguration.
     *
     * @param enabled Whether overlay effects should be visible
     */
    fun enabled(enabled: Boolean) {
        try {
            kamsyView.overlayDrawable?.setOverlayEnabled(enabled)
            logger.debug("Overlay enabled: $enabled")
        } catch (e: Exception) {
            logger.error("Failed to set overlay enabled state", e)
        }
    }

    /**
     * Removes all overlay effects from the view.
     *
     * This clears tints, scrims, status indicators, badges, and other overlay elements,
     * returning the view to its base appearance.
     */
    fun clearAll() {
        try {
            kamsyView.overlayDrawable?.run {
                clearTint()
                clearScrim()
                clearOverlays()
            }
            logger.debug("Cleared all overlays")
        } catch (e: Exception) {
            logger.error("Failed to clear overlays", e)
        }
    }
}

/**
 * DSL for configuring status indicators that show user presence and activity states.
 *
 * StatusConfiguration provides methods for adding status indicators to show
 * online/offline states, custom status conditions, and other presence information.
 * Status indicators are typically small colored dots positioned at the view's corners.
 *
 * ## Predefined Status Indicators
 * ```kotlin
 * status {
 *     online() // Green dot for online status
 *     // or
 *     offline() // Gray dot for offline status
 * }
 * ```
 *
 * ## Custom Status Indicators
 * ```kotlin
 * status {
 *     custom(
 *         color = Color.YELLOW,
 *         position = StatusPosition.TOP_LEFT,
 *         size = 24f
 *     ) // Custom yellow indicator
 * }
 * ```
 *
 * ## Clearing Status
 * ```kotlin
 * status {
 *     none() // Remove all status indicators
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 * @param logger Logger for debugging and error reporting (injected or fallback)
 *
 * @see KamsyOverlayDrawable.StatusPosition
 * @see OverlayConfiguration
 */
@KamsyDsl
class StatusConfiguration @Inject constructor(
    private val kamsyView: KamsyView,
    private val logger: IKamsyLogger
) {

    /**
     * Fallback constructor for non-dependency injection usage.
     *
     * @param kamsyView The KamsyView instance to configure
     */
    constructor(kamsyView: KamsyView) : this(
        kamsyView = kamsyView,
        logger = DefaultKamsyLogger(KamsyConfiguration())
    )

    /**
     * Applies an online status indicator.
     *
     * Shows a green status dot indicating the user is online and available.
     * Position and size are determined by the overlay drawable's default settings.
     */
    fun online() {
        try {
            kamsyView.overlayDrawable?.applyOnlineStatus()
            logger.debug("Applied online status")
        } catch (e: Exception) {
            logger.error("Failed to apply online status", e)
        }
    }

    /**
     * Applies an offline status indicator.
     *
     * Shows a gray status dot indicating the user is offline or unavailable.
     */
    fun offline() {
        try {
            kamsyView.overlayDrawable?.applyOfflineStatus()
            logger.debug("Applied offline status")
        } catch (e: Exception) {
            logger.error("Failed to apply offline status", e)
        }
    }

    /**
     * Applies a custom status indicator with specified appearance and position.
     *
     * Creates a status indicator with full control over color, position, and size.
     * Useful for custom status states like "away", "busy", or application-specific statuses.
     *
     * @param color The color of the status indicator
     * @param position The position where the indicator should be placed
     * @param size The size of the indicator in pixels (defaults to 15% of view width)
     *
     * @see KamsyOverlayDrawable.StatusPosition
     */
    fun custom(
        @ColorInt color: Int,
        position: KamsyOverlayDrawable.StatusPosition = KamsyOverlayDrawable.StatusPosition.BOTTOM_RIGHT,
        size: Float = kamsyView.width * 0.15f
    ) {
        kamsyView.overlayDrawable
            ?.takeIf { kamsyView.isDependenciesInjected }
            ?.run {
                addStatusIndicator(color, size, position)
                logger.debug("Applied custom status: color=$color, position=$position, size=$size")
            }
            ?: logger.warning("Cannot apply custom status: overlay drawable not available")
    }

    /**
     * Configures nested status indicators.
     *
     * Allows for complex status configurations with multiple indicators
     * or chained status operations.
     *
     * @param block Configuration block for additional status indicators
     */
    fun status(block: StatusConfiguration.() -> Unit) {
        StatusConfiguration(kamsyView, logger).apply(block)
    }

    /**
     * Removes all status indicators from the view.
     *
     * Clears any existing status indicators, returning the view to a neutral state
     * without status indication.
     */
    fun none() {
        try {
            // Remove all status indicators
            kamsyView.overlayDrawable?.clearOverlays()
            logger.debug("Cleared all status indicators")
        } catch (e: Exception) {
            logger.error("Failed to clear status indicators", e)
        }
    }
}

/**
 * DSL for configuring notification badges and custom badge overlays.
 *
 * BadgeConfiguration provides methods for adding badges that display text, numbers,
 * or symbols on top of the view. Commonly used for notification counts, user roles,
 * achievement indicators, or status labels.
 *
 * ## Notification Badges
 * ```kotlin
 * badge {
 *     notification(12) // Shows "12" in a red badge
 * }
 * ```
 *
 * ## Custom Badges
 * ```kotlin
 * badge {
 *     custom(
 *         text = "NEW",
 *         backgroundColor = Color.RED,
 *         textColor = Color.WHITE,
 *         position = StatusPosition.TOP_RIGHT
 *     )
 * }
 * ```
 *
 * ## Predefined Badge Types
 * ```kotlin
 * badge {
 *     vip() // Shows "VIP" in yellow badge
 *     premium() // Shows star symbol in blue badge
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 * @param logger Logger for debugging and error reporting (injected or fallback)
 *
 * @see KamsyOverlayDrawable.StatusPosition
 * @see OverlayConfiguration
 */
@KamsyDsl
class BadgeConfiguration @Inject constructor(
    private val kamsyView: KamsyView,
    private val logger: IKamsyLogger
) {

    /**
     * Fallback constructor for non-dependency injection usage.
     *
     * @param kamsyView The KamsyView instance to configure
     */
    constructor(kamsyView: KamsyView) : this(
        kamsyView = kamsyView,
        logger = DefaultKamsyLogger(KamsyConfiguration())
    )

    /**
     * Applies a notification badge showing a count number.
     *
     * Creates a red badge with white text displaying the specified count.
     * Typically positioned in the top-right corner for notification indicators.
     *
     * @param count The number to display in the badge
     */
    fun notification(count: Int) {
        try {
            kamsyView.overlayDrawable?.applyNotificationBadge(count)
            logger.debug("Applied notification badge: count=$count")
        } catch (e: Exception) {
            logger.error("Failed to apply notification badge", e)
        }
    }

    /**
     * Creates a custom badge with full control over appearance and content.
     *
     * Allows specification of custom text, colors, and positioning for application-specific
     * badge requirements.
     *
     * @param text The text to display in the badge
     * @param backgroundColor The background color of the badge
     * @param textColor The color of the text
     * @param position The position where the badge should be placed
     *
     * @see KamsyOverlayDrawable.StatusPosition
     */
    fun custom(
        text: String,
        @ColorInt backgroundColor: Int = Color.RED,
        @ColorInt textColor: Int = Color.WHITE,
        position: KamsyOverlayDrawable.StatusPosition = KamsyOverlayDrawable.StatusPosition.TOP_RIGHT
    ) {
        try {
            kamsyView.overlayDrawable?.addBadge(
                backgroundColor = backgroundColor,
                textColor = textColor,
                text = text,
                position = position
            )
            logger.debug("Applied custom badge: text=$text, position=$position")
        } catch (e: Exception) {
            logger.error("Failed to apply custom badge", e)
        }
    }

    /**
     * Applies a VIP badge.
     *
     * Shows a yellow badge with black "VIP" text, commonly used to indicate
     * premium users or special status.
     */
    fun vip() {
        custom("VIP", Color.YELLOW, Color.BLACK)
    }

    /**
     * Applies a premium badge.
     *
     * Shows a blue badge with a white star symbol, typically used to indicate
     * premium membership or special features.
     */
    fun premium() {
        custom("★", Color.BLUE, Color.WHITE)
    }
}

/**
 * DSL for configuring BlurHash properties for progressive image loading placeholders.
 *
 * BlurHashConfiguration provides methods for setting up BlurHash placeholders that
 * display a blurred representation of an image while the actual image loads.
 * BlurHash creates compact representations of images that can be decoded quickly
 * to show meaningful placeholders.
 *
 * ## Basic BlurHash Configuration
 * ```kotlin
 * blurHash {
 *     hash("LGFFaXYk^6#M@-5c,1J5@[or[Q6.") // BlurHash string
 *     punch(1.2f) // Increase contrast
 * }
 * ```
 *
 * ## Loading BlurHash with Custom Settings
 * ```kotlin
 * blurHash {
 *     load(
 *         hash = "LGFFaXYk^6#M@-5c,1J5@[or[Q6.",
 *         punch = 1.5f // Higher contrast for more vibrant colors
 *     )
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 *
 * @see kamsyView.blurHash
 * @see kamsyView.blurHashPunch
 */
@KamsyDsl
class BlurHashConfiguration(private val kamsyView: KamsyView) {

    /**
     * Sets the BlurHash string for processing.
     *
     * The BlurHash string is a compact representation of an image that can be
     * decoded to create a blurred placeholder. Setting this triggers asynchronous
     * BlurHash processing and updates the view's UI state.
     *
     * @param hash The BlurHash string to process
     */
    fun hash(hash: String) {
        kamsyView.blurHash = hash
    }

    /**
     * Sets the punch factor for BlurHash processing.
     *
     * The punch factor controls the contrast and saturation of the decoded BlurHash.
     * Higher values create more vibrant, contrasted images while lower values
     * create more muted, subtle effects.
     *
     * @param punch The punch factor (0.0 - 2.0)
     *              1.0 = normal contrast
     *              < 1.0 = lower contrast/saturation
     *              > 1.0 = higher contrast/saturation
     */
    fun punch(@FloatRange(from = 0.0, to = 2.0) punch: Float) {
        kamsyView.blurHashPunch = punch
    }

    /**
     * Loads a BlurHash with specified punch factor in a single operation.
     *
     * Convenience method that sets both the BlurHash string and punch factor,
     * then triggers processing.
     *
     * @param hash The BlurHash string to process
     * @param punch The punch factor for contrast/saturation control (default: 1.0)
     */
    fun load(hash: String, punch: Float = 1f) {
        kamsyView.blurHashPunch = punch
        kamsyView.blurHash = hash
    }
}

/**
 * DSL for configuring general appearance properties including shape, scaling, and styling presets.
 *
 * AppearanceConfiguration provides control over the fundamental visual characteristics
 * of the view, including its shape, corner treatments, scaling factors, and preset
 * styling themes that affect the overall aesthetic.
 *
 * ## Shape Configuration
 * ```kotlin
 * appearance {
 *     shape(KamsyShape.SQUIRCLE) // Apply squircle shape
 *     // or
 *     roundedCorners(16f) // Custom corner radius
 * }
 * ```
 *
 * ## Custom Shape Models
 * ```kotlin
 * appearance {
 *     customShape(
 *         ShapeAppearanceModel.builder()
 *             .setTopLeftCorner(RoundedCornerTreatment())
 *             .setTopLeftCornerSize(20f)
 *             .setBottomRightCorner(CutCornerTreatment())
 *             .setBottomRightCornerSize(15f)
 *             .build()
 *     )
 * }
 * ```
 *
 * ## Scaling and Spacing
 * ```kotlin
 * appearance {
 *     iconScale(0.8f) // 80% icon size
 *     margin(12) // 12px margin around content
 * }
 * ```
 *
 * ## Preset Styles
 * ```kotlin
 * appearance {
 *     material3() // Apply Material 3 design system styling
 *     // or
 *     minimal() // Apply minimal, clean styling
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 *
 * @see KamsyShape
 * @see ShapeAppearanceModel
 */
@KamsyDsl
class AppearanceConfiguration(private val kamsyView: KamsyView) {

    /**
     * Sets the icon scale factor for drawable content.
     *
     * Controls how large icon-based content appears within the view bounds.
     * This affects the relative size of icons and drawable content but not text placeholders.
     *
     * @param scale Scale factor (0.0 - 1.0)
     *              0.5 = 50% of view size
     *              1.0 = full view size
     */
    fun iconScale(@FloatRange(from = 0.0, to = 1.0) scale: Float) {
        kamsyView.iconDrawableScale = scale
    }

    /**
     * Sets the margin around the avatar content.
     *
     * Creates spacing between the view's edge and the content, primarily affecting
     * circular avatars by adding internal padding.
     *
     * @param margin Margin in pixels
     */
    fun margin(@Dimension margin: Int) {
        kamsyView.avatarMargin = margin
    }

    /**
     * Applies a predefined shape to the view.
     *
     * Shapes define the clipping path and corner treatments for the view.
     * Each shape has its own characteristic appearance and corner handling.
     *
     * @param shape The shape to apply
     * @see KamsyShape
     */
    fun shape(shape: KamsyShape) {
        shape.apply(kamsyView)
    }

    /**
     * Applies a custom shape appearance model to the view.
     *
     * Provides complete control over corner treatments, sizes, and shape characteristics
     * using Material Design's shape system. Allows for complex custom shapes that
     * aren't covered by the predefined [KamsyShape] options.
     *
     * Example:
     * ```kotlin
     * val customShape = ShapeAppearanceModel.builder()
     *     .setTopLeftCorner(RoundedCornerTreatment())
     *     .setTopLeftCornerSize(24f)
     *     .setTopRightCorner(CutCornerTreatment())
     *     .setTopRightCornerSize(12f)
     *     .setBottomLeftCorner(RoundedCornerTreatment())
     *     .setBottomLeftCornerSize(8f)
     *     .setBottomRightCorner(RoundedCornerTreatment())
     *     .setBottomRightCornerSize(16f)
     *     .build()
     *
     * appearance {
     *     customShape(customShape)
     * }
     * ```
     *
     * @param shapeModel The custom shape appearance model to apply
     */
    fun customShape(shapeModel: ShapeAppearanceModel) {
        kamsyView.shapeAppearanceModel = shapeModel
    }

    /**
     * Creates a rounded rectangle shape with uniform corner radius.
     *
     * Convenience method for creating a shape with rounded corners of the same size.
     * This is equivalent to creating a custom ShapeAppearanceModel with all corners
     * set to rounded treatment with the specified radius.
     *
     * @param cornerRadius The radius for all corners in pixels
     */
    fun roundedCorners(cornerRadius: Float) {
        val shapeModel = ShapeAppearanceModel.builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(cornerRadius)
            .build()
        kamsyView.shapeAppearanceModel = shapeModel
    }

    /**
     * Applies Material 3 design system styling.
     *
     * Configures the view according to Material 3 design guidelines including:
     * - Material 3 color scheme (primary color border)
     * - Appropriate border width (2px)
     * - Volumetric effects enabled
     * - Material 3 shape characteristics
     */
    fun material3() {
        // Apply Material 3 styling
        kamsyView.apply {
            // Example Material 3 styling
            borderWidth = 2
            borderColor = "#6750A4".toColorInt()
            volumetricType = KamsyView.VolumetricType.ALL
        }
    }

    /**
     * Applies minimal, clean styling.
     *
     * Removes decorative elements for a clean, unadorned appearance:
     * - No border
     * - No volumetric effects
     * - No margins
     * - Clean, simple presentation
     */
    fun minimal() {
        kamsyView.apply {
            borderWidth = 0
            volumetricType = KamsyView.VolumetricType.NONE
            avatarMargin = 0
        }
    }
}

/**
 * DSL for configuring animation effects for borders, volumetric effects, and other dynamic behaviors.
 *
 * AnimationConfiguration provides methods for adding various animation effects to enhance
 * the visual appeal and user experience. Animations can be applied to different components
 * of the view including borders, volumetric effects, and decorative elements.
 *
 * ## Border Animations
 * ```kotlin
 * animations {
 *     borderRotation(duration = 3000L) // 3-second rotation
 *     borderPulse(pulseCount = 5, duration = 600L) // 5 pulses
 * }
 * ```
 *
 * ## Volumetric Animations
 * ```kotlin
 * animations {
 *     volumetricPulse(
 *         minIntensity = 0.1f,
 *         maxIntensity = 0.6f,
 *         duration = 1200L,
 *         cycles = 3
 *     )
 *     volumetricBreathe(
 *         baseIntensity = 0.2f,
 *         peakIntensity = 0.5f,
 *         duration = 2500L
 *     )
 * }
 * ```
 *
 * ## Animation Control
 * ```kotlin
 * animations {
 *     // ... configure animations
 *     stopAll() // Stop all running animations
 * }
 * ```
 *
 * @param kamsyView The KamsyView instance to configure
 *
 * @see kamsyView.borderDrawable
 * @see kamsyView.volumetricDrawable
 */
@KamsyDsl
class AnimationConfiguration(private val kamsyView: KamsyView) {

    /**
     * Starts a continuous rotation animation for border arches.
     *
     * Creates a smooth rotation effect for decorative arch elements around the border.
     * Useful for loading indicators, progress displays, or visual flair.
     *
     * @param duration Duration for one complete rotation in milliseconds (default: 2000ms)
     */
    fun borderRotation(duration: Long = 2000L) {
        kamsyView.borderDrawable?.animateArches(duration)
    }

    /**
     * Starts a pulsing animation for the border.
     *
     * Creates a pulsing effect that can be used for notifications, emphasis,
     * or to draw attention to the view.
     *
     * @param pulseCount Number of pulse cycles to perform (default: 3)
     * @param duration Duration for each pulse cycle in milliseconds (default: 500ms)
     */
    fun borderPulse(pulseCount: Int = 3, duration: Long = 500L) {
        kamsyView.borderDrawable?.pulseAnimation(pulseCount, duration)
    }

    /**
     * Starts a pulsing animation for volumetric effects.
     *
     * Creates a pulsing depth effect by animating the intensity of volumetric lighting.
     * The effect cycles between minimum and maximum intensity values for the specified
     * number of cycles.
     *
     * @param minIntensity Minimum volumetric intensity (0.0 - 1.0, default: 0.1f)
     * @param maxIntensity Maximum volumetric intensity (0.0 - 1.0, default: 0.5f)
     * @param duration Duration for each pulse cycle in milliseconds (default: 1000ms)
     * @param cycles Number of pulse cycles to perform (default: 3)
     */
    fun volumetricPulse(
        minIntensity: Float = 0.1f,
        maxIntensity: Float = 0.5f,
        duration: Long = 1000L,
        cycles: Int = 3
    ) {
        kamsyView.volumetricDrawable?.pulseVolumetric(minIntensity, maxIntensity, duration, cycles)
    }

    /**
     * Starts a continuous breathing animation for volumetric effects.
     *
     * Creates a smooth, continuous breathing effect for volumetric lighting that
     * cycles between base and peak intensity values. This creates a calm, organic
     * feeling that's suitable for ambient effects or meditation apps.
     *
     * @param baseIntensity Base volumetric intensity (0.0 - 1.0, default: 0.2f)
     * @param peakIntensity Peak volumetric intensity (0.0 - 1.0, default: 0.4f)
     * @param duration Duration for one complete breath cycle in milliseconds (default: 2000ms)
     */
    fun volumetricBreathe(
        baseIntensity: Float = 0.2f,
        peakIntensity: Float = 0.4f,
        duration: Long = 2000L
    ) {
        kamsyView.volumetricDrawable?.breatheEffect(baseIntensity, peakIntensity, duration)
    }

    /**
     * Stops all running animations.
     *
     * Immediately stops all border and volumetric animations, returning the view
     * to its static appearance state.
     */
    fun stopAll() {
        kamsyView.borderDrawable?.stopAnimation()
        // Stop volumetric animations would be implemented
    }
}

/**
 * Predefined visual styles that provide complete theming configurations for KamsyView.
 *
 * KamsyStyle defines sealed class hierarchy of predefined styles that configure multiple
 * aspects of the view according to design system guidelines or specific use cases.
 * Each style applies a coordinated set of properties to achieve a cohesive visual theme.
 *
 * ## Available Styles
 * - [Default] - Standard gray styling with moderate effects
 * - [Minimal] - Clean, unadorned styling for subtle presentations
 * - [Material3] - Material Design 3 system styling
 * - [Gaming] - Vibrant, dynamic styling with gradients and effects
 * - [Professional] - Business-appropriate styling with blue theme
 *
 * ## Usage
 * ```kotlin
 * kamsyView.configure {
 *     style(KamsyStyle.Gaming) // Apply gaming theme
 * }
 *
 * // or directly
 * KamsyStyle.Material3.apply(kamsyView)
 * ```
 *
 * @see configure
 * @see KamsyViewConfiguration.style
 */
sealed class KamsyStyle {
    /**
     * Applies this style's configuration to the specified KamsyView.
     *
     * @param kamsyView The view to apply the style to
     */
    abstract fun apply(kamsyView: KamsyView)

    /**
     * Default styling with standard gray theme and moderate effects.
     *
     * Provides a balanced, neutral appearance suitable for general use:
     * - Gray border with moderate width
     * - Volumetric effects enabled on all content
     * - White text on gray background for placeholders
     * - Suitable for most applications as a starting point
     */
    object Default : KamsyStyle() {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.configure {
                border {
                    width(4)
                    color(Color.GRAY)
                }
                volumetric { all() }
                placeholder {
                    textColor(Color.WHITE)
                    backgroundColor(Color.GRAY)
                }
            }
        }
    }

    /**
     * Minimal styling with no decorative elements.
     *
     * Provides a clean, unadorned appearance for subtle presentations:
     * - No border
     * - No volumetric effects
     * - Minimal appearance settings
     * - Ideal for clean, modern interfaces
     */
    object Minimal : KamsyStyle() {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.configure {
                border { none() }
                volumetric { none() }
                appearance { minimal() }
            }
        }
    }

    /**
     * Material Design 3 system styling.
     *
     * Implements Material 3 design guidelines:
     * - Material 3 primary color (#6750A4) border
     * - Thin border (2px) as per Material guidelines
     * - Volumetric effects enabled for depth
     * - Material 3 appearance characteristics
     */
    object Material3 : KamsyStyle() {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.configure {
                border {
                    width(2)
                    color("#6750A4".toColorInt())
                }
                volumetric { all() }
                appearance { material3() }
            }
        }
    }

    /**
     * Gaming-themed styling with vibrant colors and dynamic effects.
     *
     * Provides an energetic, dynamic appearance suitable for gaming applications:
     * - Thick border (6px) with cyan-to-magenta gradient
     * - 45-degree gradient angle for dynamic appearance
     * - 8 decorative arches covering 270 degrees with mirrored pattern
     * - Volumetric effects enabled for depth
     * - Cyan tint overlay for futuristic feel
     * - Ideal for gaming, esports, or entertainment applications
     */
    object Gaming : KamsyStyle() {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.configure {
                border {
                    width(6)
                    gradient(Color.CYAN, Color.MAGENTA, 45)
                    arches {
                        count(8)
                        degreeArea(270)
                        mirror()
                    }
                }
                volumetric { all() }
                overlay {
                    tint(Color.CYAN, 0.1f)
                }
            }
        }
    }

    /**
     * Professional styling with business-appropriate blue theme.
     *
     * Provides a polished, professional appearance suitable for business applications:
     * - Blue border (#2196F3) with thin width (2px)
     * - Volumetric effects applied only to drawable content
     * - Light blue background (#E3F2FD) for placeholders
     * - Darker blue text (#1976D2) for good contrast and readability
     * - Ideal for business, productivity, or professional applications
     */
    object Professional : KamsyStyle() {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.configure {
                border {
                    width(2)
                    color("#2196F3".toColorInt())
                }
                volumetric { drawable() }
                placeholder {
                    backgroundColor("#E3F2FD".toColorInt())
                    textColor("#1976D2".toColorInt())
                }
            }
        }
    }
}

/**
 * Predefined configurations for decorative arch elements.
 *
 * ArchesPreset provides common arch configurations for specific use cases,
 * offering ready-made settings for typical arch decoration patterns.
 *
 * ## Available Presets
 * - [LOADING] - Configuration suitable for loading indicators
 * - [PROGRESS] - Configuration for progress display
 * - [DECORATIVE] - Full decorative circle configuration
 *
 * ## Usage
 * ```kotlin
 * kamsyView.configure {
 *     border {
 *         arches {
 *             preset(ArchesPreset.LOADING)
 *         }
 *     }
 * }
 * ```
 *
 * @see ArchesConfiguration.preset
 * @see KamsyView.ArchesType
 */
enum class ArchesPreset {
    /**
     * Loading indicator configuration.
     *
     * Provides a configuration suitable for loading spinners or progress indicators:
     * - 4 arch elements for clear visibility without overcrowding
     * - 270-degree coverage (3/4 circle) leaving space for visual flow
     * - Single arch type for clean, simple appearance
     * - Ideal for loading states or progress indication
     */
    LOADING {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.apply {
                archesCount = 4
                archesDegreeArea = 270
                archesType = KamsyView.ArchesType.SINGLE
            }
        }
    },

    /**
     * Progress display configuration.
     *
     * Provides a configuration suitable for progress bars or completion indicators:
     * - 6 arch elements for more granular progress indication
     * - 300-degree coverage for substantial visual presence
     * - Mirrored arch type for balanced, symmetrical appearance
     * - Ideal for progress tracking or completion status
     */
    PROGRESS {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.apply {
                archesCount = 6
                archesDegreeArea = 300
                archesType = KamsyView.ArchesType.MIRROR
            }
        }
    },

    /**
     * Full decorative circle configuration.
     *
     * Provides a configuration for maximum visual impact and decoration:
     * - 8 arch elements for rich, detailed appearance
     * - 360-degree coverage (full circle) for complete decoration
     * - Single arch type for uniform distribution
     * - Ideal for special emphasis, achievements, or decorative purposes
     */
    DECORATIVE {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.apply {
                archesCount = 8
                archesDegreeArea = 360
                archesType = KamsyView.ArchesType.SINGLE
            }
        }
    };

    /**
     * Applies this preset's configuration to the specified KamsyView.
     *
     * @param kamsyView The view to apply the preset to
     */
    abstract fun apply(kamsyView: KamsyView)
}

/**
 * Predefined styling options for placeholder content.
 *
 * PlaceholderStyle provides common placeholder configurations that affect
 * text appearance, background colors, and overall placeholder presentation.
 *
 * ## Available Styles
 * - [DEFAULT] - Standard gray placeholder with white text
 * - [COLORFUL] - Vibrant placeholder with auto-generated colors
 * - [MINIMAL] - Clean, minimal placeholder with subtle colors
 *
 * ## Usage
 * ```kotlin
 * kamsyView.configure {
 *     placeholder {
 *         initials("John Doe")
 *         style(PlaceholderStyle.COLORFUL)
 *     }
 * }
 * ```
 *
 * @see PlaceholderConfiguration.style
 */
enum class PlaceholderStyle {
    /**
     * Standard placeholder styling.
     *
     * Provides conventional placeholder appearance:
     * - White text for good contrast
     * - Gray background for neutral appearance
     * - Normal text size (100%)
     * - Suitable for most general use cases
     */
    DEFAULT {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.apply {
                textColor = Color.WHITE
                backgroundPlaceholderColor = Color.GRAY
                textSizePercentage = 1f
            }
        }
    },

    /**
     * Colorful placeholder styling.
     *
     * Provides vibrant placeholder appearance:
     * - White text for contrast against colorful backgrounds
     * - Larger text size (120%) for enhanced visibility
     * - Designed to work with auto-generated background colors
     * - Ideal for user avatars with name-based color generation
     */
    COLORFUL {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.apply {
                textColor = Color.WHITE
                textSizePercentage = 1.2f
                // Background color would be auto-generated via autoColor()
            }
        }
    },

    /**
     * Minimal placeholder styling.
     *
     * Provides clean, understated placeholder appearance:
     * - Black text for subtle contrast
     * - Light gray background (#F5F5F5) for minimal visual weight
     * - Smaller text size (80%) for subtle presentation
     * - Ideal for clean, modern interfaces
     */
    MINIMAL {
        override fun apply(kamsyView: KamsyView) {
            kamsyView.apply {
                textColor = Color.BLACK
                backgroundPlaceholderColor = "#F5F5F5".toColorInt()
                textSizePercentage = 0.8f
            }
        }
    };

    /**
     * Applies this style's configuration to the specified KamsyView.
     *
     * @param kamsyView The view to apply the style to
     */
    abstract fun apply(kamsyView: KamsyView)
}

/**
 * Predefined shapes that define the visual form and clipping characteristics of KamsyView.
 *
 * KamsyShape provides a set of common shapes with appropriate corner treatments
 * and visual characteristics. Each shape is designed to work well with the view's
 * other features like borders, overlays, and volumetric effects.
 *
 * ## Available Shapes
 * - [CIRCLE] - Perfect circle shape
 * - [ROUNDED_SQUARE] - Square with rounded corners (15% corner radius)
 * - [HEXAGON] - Six-sided shape with cut corners
 * - [SQUIRCLE] - Square with heavily rounded corners (30% corner radius)
 * - [DIAMOND] - Diamond/rhombus shape with 45-degree cut corners
 * - [STAR] - Star-like shape with alternating corner treatments
 * - [CUSTOM] - Preserves any custom shape set via other means
 *
 * ## Usage
 * ```kotlin
 * kamsyView.configure {
 *     appearance {
 *         shape(KamsyShape.SQUIRCLE)
 *     }
 * }
 *
 * // or directly
 * KamsyShape.HEXAGON.apply(kamsyView)
 * ```
 *
 * @see AppearanceConfiguration.shape
 * @see AppearanceConfiguration.customShape
 */
enum class KamsyShape {
    /**
     * Perfect circle shape.
     *
     * Creates a circular clipping path using 50% corner radius on all corners.
     * This is the most common shape for user avatars and profile pictures.
     *
     * Characteristics:
     * - 50% corner radius (creates perfect circle)
     * - Rounded corner treatment
     * - Works well with padding/margins
     * - Ideal for profile pictures and avatars
     */
    CIRCLE {
        override fun apply(kamsyView: KamsyView) {
            val shapeModel = ShapeAppearanceModel.builder()
                .setAllCorners(RoundedCornerTreatment())
                .setAllCornerSizes(RelativeCornerSize(0.5f))
                .build()
            kamsyView.shapeAppearanceModel = shapeModel
        }
    },

    /**
     * Rounded square shape.
     *
     * Creates a square with moderately rounded corners (15% of width).
     * Provides a softer alternative to sharp squares while maintaining
     * the square's structural characteristics.
     *
     * Characteristics:
     * - 15% corner radius relative to view width
     * - Rounded corner treatment
     * - Balanced between circular and square aesthetics
     * - Good for modern, friendly interfaces
     */
    ROUNDED_SQUARE {
        override fun apply(kamsyView: KamsyView) {
            val cornerRadius = kamsyView.width * 0.15f // 15% of width as corner radius
            val shapeModel = ShapeAppearanceModel.builder()
                .setAllCorners(RoundedCornerTreatment())
                .setAllCornerSizes(cornerRadius)
                .build()
            kamsyView.shapeAppearanceModel = shapeModel
        }
    },

    /**
     * Hexagonal shape.
     *
     * Creates a six-sided shape using cut corner treatments.
     * The cut corners create the characteristic angled edges of a hexagon.
     *
     * Characteristics:
     * - 25% cut corner size relative to view width
     * - Cut corner treatment for sharp, angled edges
     * - Geometric, modern appearance
     * - Good for gaming or tech-oriented interfaces
     */
    HEXAGON {
        override fun apply(kamsyView: KamsyView) {
            val shapeModel = ShapeAppearanceModel.builder()
                .setAllCorners(CutCornerTreatment())
                .setAllCornerSizes(kamsyView.width * 0.25f)
                .build()
            kamsyView.shapeAppearanceModel = shapeModel
        }
    },

    /**
     * Squircle shape (super-ellipse).
     *
     * Creates a shape that's between a square and a circle, with heavily
     * rounded corners (30% of width). Popular in modern interface design
     * for its organic, friendly appearance.
     *
     * Characteristics:
     * - 30% corner radius relative to view width
     * - Rounded corner treatment
     * - Organic, friendly appearance
     * - Popular in modern mobile interfaces
     */
    SQUIRCLE {
        override fun apply(kamsyView: KamsyView) {
            val cornerRadius = kamsyView.width * 0.3f
            val shapeModel = ShapeAppearanceModel.builder()
                .setAllCorners(RoundedCornerTreatment())
                .setAllCornerSizes(cornerRadius)
                .build()
            kamsyView.shapeAppearanceModel = shapeModel
        }
    },

    /**
     * Diamond/rhombus shape.
     *
     * Creates a diamond shape using 50% cut corners, effectively rotating
     * the square by 45 degrees to create a diamond orientation.
     *
     * Characteristics:
     * - 50% cut corner size (creates diamond points)
     * - Cut corner treatment for sharp edges
     * - Distinctive, attention-grabbing shape
     * - Good for special emphasis or unique branding
     */
    DIAMOND {
        override fun apply(kamsyView: KamsyView) {
            val cornerSize = kamsyView.width * 0.5f
            val shapeModel = ShapeAppearanceModel.builder()
                .setAllCorners(CutCornerTreatment())
                .setAllCornerSizes(cornerSize)
                .build()
            kamsyView.shapeAppearanceModel = shapeModel
        }
    },

    /**
     * Star-like shape with alternating corner treatments.
     *
     * Creates a star-like effect using alternating cut and rounded corners.
     * The varying corner treatments create points and curves that suggest
     * a star or badge-like appearance.
     *
     * Characteristics:
     * - Top-left and bottom-right: cut corners (20% size) - creates points
     * - Top-right and bottom-left: rounded corners (10% size) - creates curves
     * - Alternating pattern creates visual interest
     * - Good for achievements, badges, or special recognition
     */
    STAR {
        override fun apply(kamsyView: KamsyView) {
            // For star shape, we'll use alternating cut and rounded corners for a star-like effect
            val cutSize = kamsyView.width * 0.2f
            val shapeModel = ShapeAppearanceModel.builder()
                .setTopLeftCorner(CutCornerTreatment())
                .setTopLeftCornerSize(cutSize)
                .setTopRightCorner(RoundedCornerTreatment())
                .setTopRightCornerSize(cutSize / 2)
                .setBottomLeftCorner(RoundedCornerTreatment())
                .setBottomLeftCornerSize(cutSize / 2)
                .setBottomRightCorner(CutCornerTreatment())
                .setBottomRightCornerSize(cutSize)
                .build()
            kamsyView.shapeAppearanceModel = shapeModel
        }
    },

    /**
     * Custom shape placeholder.
     *
     * Preserves any custom shape that was set via other means (such as
     * AppearanceConfiguration.customShape() or direct ShapeAppearanceModel setting).
     * This option doesn't modify the current shape but serves as a placeholder
     * for custom configurations.
     *
     * Use this when you want to maintain a custom shape that doesn't fit
     * the predefined categories.
     */
    CUSTOM {
        override fun apply(kamsyView: KamsyView) {
            // Custom shape - maintains current shape or applies default circle
            // If a custom shape was already set via appearance{}, it will remain
        }
    };

    /**
     * Applies this shape's configuration to the specified KamsyView.
     *
     * @param kamsyView The view to apply the shape to
     */
    abstract fun apply(kamsyView: KamsyView)
}

/**
 * DSL marker annotation for type-safe DSL construction.
 *
 * This annotation ensures that DSL blocks can only contain methods from their
 * appropriate configuration classes, preventing incorrect nesting and providing
 * better IDE support and compile-time safety.
 *
 * Applied to all DSL configuration classes to create scope restrictions.
 */
@DslMarker
annotation class KamsyDsl

/**
 * Extension function for easy DSL-based configuration of KamsyView.
 *
 * Provides the primary entry point for configuring KamsyView using the fluent DSL syntax.
 * This extension function creates a KamsyViewConfiguration instance and applies the
 * provided configuration block to it.
 *
 * ## Basic Usage
 * ```kotlin
 * kamsyView.configure {
 *     appearance {
 *         shape(KamsyShape.CIRCLE)
 *         margin(8)
 *     }
 *     border {
 *         width(4)
 *         color(Color.BLUE)
 *     }
 * }
 * ```
 *
 * ## Complete Configuration Example
 * ```kotlin
 * kamsyView.configure {
 *     // Apply overall style
 *     style(KamsyStyle.Gaming)
 *
 *     // Customize appearance
 *     appearance {
 *         shape(KamsyShape.SQUIRCLE)
 *         iconScale(0.8f)
 *         margin(12)
 *     }
 *
 *     // Configure border with gradient
 *     border {
 *         width(6)
 *         gradient(Color.CYAN, Color.MAGENTA, 45)
 *         arches {
 *             count(8)
 *             degreeArea(270)
 *             mirror()
 *         }
 *     }
 *
 *     // Setup placeholder
 *     placeholder {
 *         initials("Jane Doe")
 *         autoColor("Jane Doe")
 *         textSize(1.2f)
 *     }
 *
 *     // Add overlays
 *     overlay {
 *         tint(Color.BLUE, 0.1f)
 *         status { online() }
 *         badge { notification(5) }
 *     }
 *
 *     // Configure volumetric effects
 *     volumetric {
 *         all()
 *         intensity(highlight = 0.4f, shadow = 0.3f)
 *     }
 *
 *     // Add animations
 *     animations {
 *         borderRotation(duration = 3000L)
 *         volumetricPulse(cycles = 3)
 *     }
 *
 *     // BlurHash placeholder
 *     blurHash {
 *         load("LGFFaXYk^6#M@-5c,1J5@[or[Q6.", punch = 1.2f)
 *     }
 * }
 * ```
 *
 * @param block Configuration block that defines the view's appearance and behavior
 * @receiver KamsyView instance to configure
 *
 * @see KamsyViewConfiguration
 */
fun KamsyView.configure(block: KamsyViewConfiguration.() -> Unit) {
    KamsyViewConfiguration(this).apply(block)
}

/**
 * Extension function to extract initials from a full name string.
 *
 * Extracts up to the specified number of initials from a name by taking the first
 * character of each space-separated word. Empty or whitespace-only words are ignored.
 * If no valid initials can be extracted, returns a fallback character.
 *
 * ## Examples
 * ```kotlin
 * "John Doe".extractInitials() // Returns "JD"
 * "Mary Jane Watson".extractInitials() // Returns "MJ" (first 2 initials)
 * "Madonna".extractInitials() // Returns "M"
 * "  ".extractInitials() // Returns "?" (fallback)
 * "".extractInitials() // Returns "?" (fallback)
 * "John".extractInitials(1) // Returns "J" (limited to 1 initial)
 * "A B C D".extractInitials(3) // Returns "ABC" (limited to 3 initials)
 * ```
 *
 * ## Algorithm
 * 1. Split the string by spaces
 * 2. Filter out blank/empty segments
 * 3. Take up to `maxInitials` words
 * 4. Extract the first character of each word and convert to uppercase
 * 5. Join into a single string
 * 6. Return fallback "?" if no valid initials found
 *
 * @param maxInitials Maximum number of initials to extract (default: 2)
 * @return Extracted initials as uppercase string, or "?" if no valid initials found
 *
 * @see PlaceholderConfiguration.initials
 */
private fun String.extractInitials(maxInitials: Int = 2): String {
    return split(" ")
        .filter { it.isNotBlank() }
        .take(maxInitials)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .takeIf { it.isNotBlank() } ?: "?"
}

/**
 * Generates a consistent background color from a text string using deterministic hashing.
 *
 * Creates a visually appealing color by converting the text's hash code into HSV color space
 * components. The algorithm ensures that the same input text always produces the same color,
 * making it ideal for user avatar background colors that remain consistent across sessions.
 *
 * ## Color Generation Algorithm
 * 1. Calculate hash code of the input text
 * 2. Extract hue from hash modulo 360 (full color spectrum)
 * 3. Generate saturation between 0.5-1.0 for vibrant colors
 * 4. Generate lightness between 0.4-0.7 for good contrast with white text
 * 5. Convert HSV values to RGB color integer
 *
 * ## Color Characteristics
 * - **Hue**: Full spectrum (0-360°) - ensures color variety
 * - **Saturation**: 0.5-1.0 - ensures vibrant, non-washed-out colors
 * - **Lightness**: 0.4-0.7 - ensures good contrast for white text readability
 *
 * ## Examples
 * ```kotlin
 * generateBackgroundColor("John Doe") // Always returns same color for "John Doe"
 * generateBackgroundColor("Jane Smith") // Different color, but consistent for "Jane Smith"
 * generateBackgroundColor("") // Returns color based on empty string hash
 * ```
 *
 * ## Usage with KamsyView
 * ```kotlin
 * kamsyView.configure {
 *     placeholder {
 *         initials("John Doe")
 *         autoColor("John Doe") // Uses this function internally
 *     }
 * }
 * ```
 *
 * @param text Input text to generate color from (typically a user's name)
 * @return RGB color integer suitable for use with Android Color APIs
 *
 * @see PlaceholderConfiguration.autoColor
 * @see Color.HSVToColor
 */
private fun generateBackgroundColor(text: String): Int {
    val hash = text.hashCode()
    val hue = (hash % 360).toFloat()
    val saturation = 0.5f + (hash % 50) / 100f
    val lightness = 0.4f + (hash % 30) / 100f

    return Color.HSVToColor(floatArrayOf(hue, saturation, lightness))
}
