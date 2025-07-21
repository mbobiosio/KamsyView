package com.github.kamsyview.interfaces

import android.graphics.Typeface
import com.github.kamsyview.core.BadgeConfiguration
import com.github.kamsyview.core.KamsyView
import com.github.kamsyview.core.OverlayConfiguration
import com.github.kamsyview.core.PlaceholderConfiguration
import com.github.kamsyview.core.StatusConfiguration
import com.github.kamsyview.drawables.KamsyBorderDrawable
import com.github.kamsyview.drawables.KamsyOverlayDrawable
import com.github.kamsyview.drawables.KamsyPlaceholderDrawable
import com.github.kamsyview.drawables.KamsyVolumetricDrawable

/**
 * Factory interface for creating drawable components used by KamsyView.
 *
 * IKamsyDrawableFactory provides a centralized creation mechanism for all drawable
 * components that make up the visual appearance of KamsyView. This factory pattern
 * enables dependency injection, customization, and consistent theming across all
 * drawable elements.
 *
 * ## Factory Benefits
 * - **Centralized Creation**: Single point for creating all drawable components
 * - **Dependency Injection**: Can be easily injected and customized via Hilt
 * - **Consistent Theming**: Ensures uniform styling across all components
 * - **Testability**: Enables mocking and testing of drawable creation
 * - **Customization**: Allows applications to provide custom drawable implementations
 *
 * ## Drawable Components Created
 *
 * ### Border Drawables
 * Creates border components that handle:
 * - Solid and gradient borders
 * - Decorative arch elements
 * - Border animations and effects
 *
 * ### Volumetric Drawables
 * Creates 3D effect components that provide:
 * - Shadow and highlight effects
 * - Depth and dimension rendering
 * - Volumetric animations
 *
 * ### Overlay Drawables
 * Creates overlay components that manage:
 * - Status indicators (online/offline/custom)
 * - Notification badges and custom badges
 * - Color tints and scrims
 *
 * ### Placeholder Drawables
 * Creates placeholder components that display:
 * - Text-based placeholders (initials, custom text)
 * - Icon-based placeholders
 * - Background colors and typography
 *
 * ## Implementation Examples
 *
 * ### Basic Implementation
 * ```kotlin
 * class DefaultKamsyDrawableFactory @Inject constructor(
 *     private val logger: IKamsyLogger
 * ) : IKamsyDrawableFactory {
 *
 *     override fun createBorderDrawable(kamsyView: KamsyView): KamsyBorderDrawable {
 *         return KamsyBorderDrawable(kamsyView).apply {
 *             logger.debug("Created border drawable for view: ${kamsyView.id}")
 *         }
 *     }
 *
 *     // ... other methods
 * }
 * ```
 *
 * ### Custom Themed Implementation
 * ```kotlin
 * class ThemedKamsyDrawableFactory @Inject constructor(
 *     private val themeProvider: IThemeProvider,
 *     private val logger: IKamsyLogger
 * ) : IKamsyDrawableFactory {
 *
 *     override fun createBorderDrawable(kamsyView: KamsyView): KamsyBorderDrawable {
 *         return KamsyBorderDrawable(kamsyView).apply {
 *             // Apply theme-specific styling
 *             setDefaultColors(themeProvider.getBorderColors())
 *             setDefaultWidth(themeProvider.getBorderWidth())
 *         }
 *     }
 *
 *     override fun createPlaceholderDrawable(
 *         size: Int,
 *         backgroundColor: Int,
 *         text: CharSequence?,
 *         textColor: Int,
 *         textSize: Float,
 *         typeface: Typeface?,
 *         textSizePercentage: Float,
 *         avatarMargin: Int
 *     ): KamsyPlaceholderDrawable {
 *         return KamsyPlaceholderDrawable(
 *             size = size,
 *             backgroundColor = themeProvider.getPlaceholderBackground(backgroundColor),
 *             text = text,
 *             textColor = themeProvider.getPlaceholderTextColor(textColor),
 *             textSize = textSize,
 *             typeface = typeface ?: themeProvider.getDefaultTypeface(),
 *             textSizePercentage = textSizePercentage,
 *             avatarMargin = avatarMargin
 *         )
 *     }
 * }
 * ```
 *
 * ### Hilt Module Configuration
 * ```kotlin
 * @Module
 * @InstallIn(SingletonComponent::class)
 * abstract class KamsyDrawableModule {
 *
 *     @Binds
 *     abstract fun bindDrawableFactory(
 *         factory: DefaultKamsyDrawableFactory
 *     ): IKamsyDrawableFactory
 * }
 * ```
 *
 * ## Testing Support
 * ```kotlin
 * class MockKamsyDrawableFactory : IKamsyDrawableFactory {
 *     override fun createBorderDrawable(kamsyView: KamsyView) = mockk<KamsyBorderDrawable>()
 *     override fun createVolumetricDrawable(kamsyView: KamsyView) = mockk<KamsyVolumetricDrawable>()
 *     // ... other mocked methods
 * }
 * ```
 *
 * @see KamsyBorderDrawable
 * @see KamsyVolumetricDrawable
 * @see KamsyOverlayDrawable
 * @see KamsyPlaceholderDrawable
 * @see KamsyView.drawableFactory
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
interface IKamsyDrawableFactory {

    /**
     * Creates a border drawable for the specified KamsyView.
     *
     * Border drawables handle the rendering of view borders including:
     * - Solid color borders with configurable width and color
     * - Gradient borders with start/end colors and angle
     * - Decorative arch elements around the border perimeter
     * - Border animations including rotation and pulsing effects
     *
     * The created drawable should be properly configured to work with the
     * view's current border properties and should update automatically
     * when those properties change.
     *
     * ## Implementation Requirements
     * - Must return a non-null KamsyBorderDrawable instance
     * - Should respect the view's current border configuration
     * - Must handle property changes through update() calls
     * - Should support all border styles (solid, gradient, arches)
     *
     * ## Example Implementation
     * ```kotlin
     * override fun createBorderDrawable(kamsyView: KamsyView): KamsyBorderDrawable {
     *     return KamsyBorderDrawable(kamsyView).apply {
     *         // Initialize with current view properties
     *         update()
     *
     *         // Apply any factory-specific customizations
     *         setAnimationEnabled(configuration.enableAnimations)
     *     }
     * }
     * ```
     *
     * @param kamsyView The KamsyView instance that will use this border drawable
     * @return A configured KamsyBorderDrawable ready for use
     *
     * @see KamsyBorderDrawable
     * @see KamsyView.borderDrawable
     * @see KamsyView.borderWidth
     * @see KamsyView.borderColor
     */
    fun createBorderDrawable(kamsyView: KamsyView): KamsyBorderDrawable

    /**
     * Creates a volumetric drawable for adding 3D depth effects to the specified KamsyView.
     *
     * Volumetric drawables provide depth and dimension effects including:
     * - Shadow effects with configurable intensity and direction
     * - Highlight effects for simulating lighting
     * - Ambient occlusion for realistic depth perception
     * - Volumetric animations including breathing and pulsing effects
     *
     * The drawable should respect the view's volumetric type setting and
     * apply effects only to the appropriate content types (all, drawable, placeholder).
     *
     * ## Implementation Requirements
     * - Must return a non-null KamsyVolumetricDrawable instance
     * - Should respect the view's volumetricType setting
     * - Must handle different content types appropriately
     * - Should support volumetric animations when enabled
     *
     * ## Effect Application
     * The drawable should apply effects based on the view's volumetricType:
     * - **VolumetricType.ALL**: Apply to all content
     * - **VolumetricType.DRAWABLE**: Apply only to image/icon content
     * - **VolumetricType.PLACEHOLDER**: Apply only to placeholder content
     * - **VolumetricType.NONE**: No effects applied
     *
     * ## Example Implementation
     * ```kotlin
     * override fun createVolumetricDrawable(kamsyView: KamsyView): KamsyVolumetricDrawable {
     *     return KamsyVolumetricDrawable(kamsyView).apply {
     *         // Configure based on view's volumetric type
     *         setEffectIntensity(when (kamsyView.volumetricType) {
     *             VolumetricType.ALL -> 1.0f
     *             VolumetricType.DRAWABLE -> 0.8f
     *             VolumetricType.PLACEHOLDER -> 0.6f
     *             VolumetricType.NONE -> 0.0f
     *         })
     *
     *         update()
     *     }
     * }
     * ```
     *
     * @param kamsyView The KamsyView instance that will use this volumetric drawable
     * @return A configured KamsyVolumetricDrawable ready for rendering 3D effects
     *
     * @see KamsyVolumetricDrawable
     * @see KamsyView.volumetricDrawable
     * @see KamsyView.volumetricType
     * @see KamsyView.VolumetricType
     */
    fun createVolumetricDrawable(kamsyView: KamsyView): KamsyVolumetricDrawable

    /**
     * Creates an overlay drawable for status indicators, badges, and overlay effects.
     *
     * Overlay drawables manage visual elements that appear on top of the main content:
     * - Status indicators for online/offline/away/busy states
     * - Notification badges with count numbers or custom text
     * - Color tints and darkening scrims
     * - Custom overlay elements and decorations
     *
     * The overlay drawable acts as a container for multiple overlay elements
     * and handles their positioning, layering, and rendering.
     *
     * ## Implementation Requirements
     * - Must return a non-null KamsyOverlayDrawable instance
     * - Should support multiple simultaneous overlay elements
     * - Must handle proper layering and z-ordering of elements
     * - Should provide methods for adding/removing overlay elements
     *
     * ## Overlay Elements Supported
     * - **Status Indicators**: Colored dots for presence/activity status
     * - **Badges**: Text/number displays for notifications or labels
     * - **Tints**: Color overlays for mood or state indication
     * - **Scrims**: Darkening overlays for emphasis or readability
     *
     * ## Example Implementation
     * ```kotlin
     * override fun createOverlayDrawable(kamsyView: KamsyView): KamsyOverlayDrawable {
     *     return KamsyOverlayDrawable(kamsyView).apply {
     *         // Configure default overlay settings
     *         setOverlayEnabled(true)
     *
     *         // Apply any pending status indicators from XML
     *         applyPendingOverlays()
     *
     *         update()
     *     }
     * }
     * ```
     *
     * @param kamsyView The KamsyView instance that will use this overlay drawable
     * @return A configured KamsyOverlayDrawable ready for overlay management
     *
     * @see KamsyOverlayDrawable
     * @see KamsyView.overlayDrawable
     * @see OverlayConfiguration
     * @see StatusConfiguration
     * @see BadgeConfiguration
     */
    fun createOverlayDrawable(kamsyView: KamsyView): KamsyOverlayDrawable

    /**
     * Creates a placeholder drawable for displaying text, initials, or fallback content.
     *
     * Placeholder drawables are used when no image content is available and provide:
     * - Text-based placeholders (initials, custom text, symbols)
     * - Configurable background colors and typography
     * - Proper text sizing and positioning within the view bounds
     * - Support for custom typefaces and text styling
     *
     * This method receives all necessary parameters to create a fully configured
     * placeholder without needing to access the view's properties directly.
     *
     * ## Implementation Requirements
     * - Must return a non-null KamsyPlaceholderDrawable instance
     * - Should properly center text within the specified size
     * - Must handle null or empty text gracefully
     * - Should respect the provided typography settings
     *
     * ## Text Sizing Algorithm
     * The placeholder should calculate text size using:
     * ```
     * actualTextSize = (size * textSizePercentage) / 3
     * ```
     * This ensures text scales appropriately with view size and user preferences.
     *
     * ## Background and Text Colors
     * - Background color fills the entire drawable bounds
     * - Text color should provide sufficient contrast for readability
     * - Consider accessibility guidelines for color contrast ratios
     *
     * ## Example Implementation
     * ```kotlin
     * override fun createPlaceholderDrawable(
     *     size: Int,
     *     backgroundColor: Int,
     *     text: CharSequence?,
     *     textColor: Int,
     *     textSize: Float,
     *     typeface: Typeface?,
     *     textSizePercentage: Float,
     *     avatarMargin: Int
     * ): KamsyPlaceholderDrawable {
     *     return KamsyPlaceholderDrawable(
     *         size = size,
     *         backgroundColor = backgroundColor,
     *         text = text ?: "?", // Fallback for null text
     *         textColor = textColor,
     *         textSize = calculateOptimalTextSize(size, textSizePercentage),
     *         typeface = typeface ?: Typeface.DEFAULT,
     *         textSizePercentage = textSizePercentage,
     *         avatarMargin = avatarMargin
     *     ).apply {
     *         setBounds(0, 0, size, size)
     *
     *         // Apply accessibility enhancements if needed
     *         if (isHighContrastEnabled()) {
     *             enhanceContrast()
     *         }
     *     }
     * }
     * ```
     *
     * @param size The size in pixels for both width and height of the placeholder
     * @param backgroundColor The background color for the placeholder
     * @param text The text content to display (initials, letters, symbols, etc.)
     * @param textColor The color for the text content
     * @param textSize The calculated text size in pixels
     * @param typeface The typeface for text rendering, or null for default
     * @param textSizePercentage The text size as a percentage of view width (for reference)
     * @param avatarMargin The margin around the content in pixels
     * @return A configured KamsyPlaceholderDrawable ready for display
     *
     * @see KamsyPlaceholderDrawable
     * @see PlaceholderConfiguration
     * @see KamsyView.createPlaceholderDrawable
     * @see KamsyView.placeholderText
     * @see KamsyView.textColor
     * @see KamsyView.backgroundPlaceholderColor
     */
    fun createPlaceholderDrawable(
        size: Int,
        backgroundColor: Int,
        text: CharSequence?,
        textColor: Int,
        textSize: Float,
        typeface: Typeface?,
        textSizePercentage: Float,
        avatarMargin: Int = 0
    ): KamsyPlaceholderDrawable
}
