package com.github.kamsyview.drawables

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.withSave
import com.github.kamsyview.core.KamsyView
import kotlin.math.min

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * KamsyOverlayDrawable - Coordinates and manages all overlay effects for KamsyView
 * Acts as a composite drawable that manages borders, volumetric effects, and additional overlays
 */
class KamsyOverlayDrawable(
    private val kamsyView: KamsyView
) : Drawable() {

    companion object {
        private const val MAX_OVERLAY_LAYERS = 8
        private const val OVERLAY_ALPHA_BLEND = 0.8f
        private const val SCRIM_DEFAULT_ALPHA = 0.3f
        private const val TINT_DEFAULT_ALPHA = 0.2f
    }

    // Layer management
    private val overlayLayers = mutableListOf<OverlayLayer>()
    private var borderDrawable: KamsyBorderDrawable? = null
    private var volumetricDrawable: KamsyVolumetricDrawable? = null
    private var customOverlays = mutableListOf<Drawable>()

    // Configuration
    private var overlayTint: Int? = null
    private var overlayTintAlpha: Float = TINT_DEFAULT_ALPHA
    private var scrimColor: Int? = null
    private var scrimAlpha: Float = SCRIM_DEFAULT_ALPHA
    private var overlayBlendMode: BlendMode = BlendMode.SRC_ATOP
    private var isOverlayEnabled: Boolean = true

    // Paint objects
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
    }

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
    }

    private val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
    }

    // Geometry
    private var circleRadius = 0f
    private var circleCenter = PointF()
    private var overlayBounds = RectF()

    init {
        initializeOverlays()
        update()
    }

    override fun draw(canvas: Canvas) {
        android.util.Log.d("KamsyOverlay", "Drawing overlay, enabled: $isOverlayEnabled, layers: ${overlayLayers.size}")

        if (!isOverlayEnabled) return

        canvas.withSave {

            // Remove the circular clip for now to test
            // val clipPath = Path().apply {
            //     addCircle(circleCenter.x, circleCenter.y, circleRadius, Path.Direction.CW)
            // }
            // canvas.clipPath(clipPath)

            // Only draw overlay layers (status indicators)
            drawOverlayLayers(this)

        }
    }

    override fun setAlpha(alpha: Int) {
        val normalizedAlpha = alpha / 255f

        tintPaint.alpha = (255 * normalizedAlpha * overlayTintAlpha).toInt()
        scrimPaint.alpha = (255 * normalizedAlpha * scrimAlpha).toInt()
        blendPaint.alpha = (255 * normalizedAlpha * OVERLAY_ALPHA_BLEND).toInt()

        borderDrawable?.alpha = alpha
        volumetricDrawable?.alpha = alpha
        customOverlays.forEach { it.alpha = alpha }

        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        tintPaint.colorFilter = colorFilter
        scrimPaint.colorFilter = colorFilter
        blendPaint.colorFilter = colorFilter

        borderDrawable?.colorFilter = colorFilter
        volumetricDrawable?.colorFilter = colorFilter
        customOverlays.forEach { it.colorFilter = colorFilter }

        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)

        calculateGeometry()
        updateOverlayBounds()

        borderDrawable?.setBounds(left, top, right, bottom)
        volumetricDrawable?.setBounds(left, top, right, bottom)
        customOverlays.forEach { it.setBounds(left, top, right, bottom) }
    }

    /**
     * Update overlay from KamsyView state
     */
    fun update() {
        calculateGeometry()
        updateOverlayBounds()

        borderDrawable?.update()
        volumetricDrawable?.update()

        invalidateSelf()
    }

    /**
     * Set overlay tint color
     */
    fun setTint(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float = overlayTintAlpha) {
        overlayTint = color
        overlayTintAlpha = alpha.coerceIn(0f, 1f)

        tintPaint.color = color
        tintPaint.alpha = (255 * overlayTintAlpha).toInt()

        invalidateSelf()
    }

    /**
     * Set scrim overlay
     */
    fun setScrim(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float = scrimAlpha) {
        scrimColor = color
        scrimAlpha = alpha.coerceIn(0f, 1f)

        scrimPaint.color = color
        scrimPaint.alpha = (255 * scrimAlpha).toInt()

        invalidateSelf()
    }

    /**
     * Clear tint overlay
     */
    fun clearTint() {
        overlayTint = null
        invalidateSelf()
    }

    /**
     * Clear scrim overlay
     */
    fun clearScrim() {
        scrimColor = null
        invalidateSelf()
    }

    /**
     * Add custom overlay drawable
     */
    fun addOverlay(drawable: Drawable, zIndex: Int = overlayLayers.size) {
        if (customOverlays.size >= MAX_OVERLAY_LAYERS) {
            customOverlays.removeAt(0) // Remove oldest
        }

        drawable.setBounds(bounds)
        customOverlays.add(drawable)
        invalidateSelf()
    }

    /**
     * Remove custom overlay
     */
    fun removeOverlay(drawable: Drawable) {
        customOverlays.remove(drawable)
        invalidateSelf()
    }

    /**
     * Clear all custom overlays
     */
    fun clearOverlays() {
        customOverlays.clear()
        invalidateSelf()
    }

    /**
     * Enable/disable overlay rendering
     */
    fun setOverlayEnabled(enabled: Boolean) {
        isOverlayEnabled = enabled
        invalidateSelf()
    }

    /**
     * Set blend mode for overlays
     */
    fun setBlendMode(mode: BlendMode) {
        overlayBlendMode = mode
        updateBlendMode()
        invalidateSelf()
    }

    /**
     * Add animated overlay layer
     */
    fun addAnimatedOverlay(
        layer: OverlayLayer,
        duration: Long = 1000L,
        onComplete: (() -> Unit)? = null
    ) {
        overlayLayers.add(layer)

        // Animate layer appearance
        layer.animateAlpha(0f, 1f, duration) {
            onComplete?.invoke()
        }

        invalidateSelf()
    }

    /**
     * Remove animated overlay layer
     */
    fun removeAnimatedOverlay(
        layer: OverlayLayer,
        duration: Long = 500L,
        onComplete: (() -> Unit)? = null
    ) {
        layer.animateAlpha(layer.alpha, 0f, duration) {
            overlayLayers.remove(layer)
            onComplete?.invoke()
            invalidateSelf()
        }
    }

    /**
     * Create status indicator overlay
     */
    fun addStatusIndicator(
        @ColorInt color: Int,
        size: Float = circleRadius * 0.3f,
        position: StatusPosition = StatusPosition.BOTTOM_RIGHT
    ) {
        val indicator = StatusIndicatorLayer(
            color = color,
            size = size,
            position = position,
            center = circleCenter,
            radius = circleRadius
        )

        overlayLayers.add(indicator)
        invalidateSelf()
    }

    /**
     * Create badge overlay
     */
    fun addBadge(
        @ColorInt backgroundColor: Int,
        @ColorInt textColor: Int,
        text: String,
        size: Float = circleRadius * 0.4f,
        position: StatusPosition = StatusPosition.TOP_RIGHT
    ) {
        val badge = BadgeLayer(
            backgroundColor = backgroundColor,
            textColor = textColor,
            text = text,
            size = size,
            position = position,
            center = circleCenter,
            radius = circleRadius
        )

        overlayLayers.add(badge)
        invalidateSelf()
    }

    // Private methods
    private fun initializeOverlays() {
        borderDrawable = KamsyBorderDrawable(kamsyView)
        volumetricDrawable = KamsyVolumetricDrawable(kamsyView)
    }

    private fun calculateGeometry() {
        val bounds = bounds
        if (bounds.isEmpty) return

        val size = min(bounds.width(), bounds.height())
        circleRadius = (size / 2f) - kamsyView.avatarMargin
        circleCenter.set(bounds.exactCenterX(), bounds.exactCenterY())

        overlayBounds.set(
            circleCenter.x - circleRadius,
            circleCenter.y - circleRadius,
            circleCenter.x + circleRadius,
            circleCenter.y + circleRadius
        )
    }

    private fun updateOverlayBounds() {
        overlayLayers.forEach { layer ->
            layer.updateBounds(overlayBounds, circleCenter, circleRadius)
        }
    }

    private fun updateBlendMode() {
        val xfermode = when (overlayBlendMode) {
            BlendMode.SRC_ATOP -> PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            BlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            BlendMode.DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
            BlendMode.LIGHTEN -> PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
            else -> null
        }

        blendPaint.xfermode = xfermode
    }

    private fun drawScrim(canvas: Canvas) {
        scrimColor?.let {
            canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, scrimPaint)
        }
    }

    private fun drawVolumetricEffect(canvas: Canvas) {
        volumetricDrawable?.draw(canvas)
    }

    private fun drawCustomOverlays(canvas: Canvas) {
        customOverlays.forEach { overlay ->
            overlay.draw(canvas)
        }
    }

    private fun drawBorder(canvas: Canvas) {
        borderDrawable?.draw(canvas)
    }

    private fun drawTint(canvas: Canvas) {
        overlayTint?.let {
            canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, tintPaint)
        }
    }

    private fun drawOverlayLayers(canvas: Canvas) {
        overlayLayers.forEach { layer ->
            layer.draw(canvas)
        }
    }

    /**
     * Enum for overlay blend modes
     */
    enum class BlendMode {
        SRC_ATOP,
        MULTIPLY,
        SCREEN,
        OVERLAY,
        DARKEN,
        LIGHTEN,
        ADD,
        SUBTRACT
    }

    /**
     * Enum for status indicator positions
     */
    enum class StatusPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    fun getLayerCount(): Int = overlayLayers.size
}

/**
 * Base class for overlay layers
 */
abstract class OverlayLayer {
    var alpha: Float = 1f
        protected set

    abstract fun draw(canvas: Canvas)
    abstract fun updateBounds(bounds: RectF, center: PointF, radius: Float)

    fun animateAlpha(
        from: Float,
        to: Float,
        duration: Long,
        onComplete: (() -> Unit)? = null
    ) {
        val startTime = System.currentTimeMillis()
        val alphaDelta = to - from

        fun updateAlpha() {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

            alpha = from + (alphaDelta * progress)

            if (progress < 1f) {
                Handler(Looper.getMainLooper()).post { updateAlpha() }
            } else {
                alpha = to
                onComplete?.invoke()
            }
        }

        updateAlpha()
    }
}

/**
 * Status indicator overlay layer
 */
class StatusIndicatorLayer(
    @param:ColorInt private val color: Int,
    private val size: Float,
    private val position: KamsyOverlayDrawable.StatusPosition,
    private var center: PointF,
    private var radius: Float
) : OverlayLayer() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = this@StatusIndicatorLayer.color
        style = Paint.Style.FILL
    }

    private var indicatorCenter = PointF()

    init {
        calculatePosition()
    }

    override fun draw(canvas: Canvas) {
        android.util.Log.d("StatusIndicator", "Drawing status at: (${indicatorCenter.x}, ${indicatorCenter.y}), size: ${size/2f}, alpha: $alpha")
        paint.alpha = (255 * alpha).toInt()
        canvas.drawCircle(indicatorCenter.x, indicatorCenter.y, size / 2f, paint)
    }

    override fun updateBounds(bounds: RectF, center: PointF, radius: Float) {
        this.center = center
        this.radius = radius
        calculatePosition()
    }

    private fun calculatePosition() {
        val offset = radius * 0.7f

        indicatorCenter.set(
            when (position) {
                KamsyOverlayDrawable.StatusPosition.TOP_LEFT -> center.x - offset
                KamsyOverlayDrawable.StatusPosition.TOP_RIGHT -> center.x + offset
                KamsyOverlayDrawable.StatusPosition.BOTTOM_LEFT -> center.x - offset
                KamsyOverlayDrawable.StatusPosition.BOTTOM_RIGHT -> center.x + offset
                KamsyOverlayDrawable.StatusPosition.CENTER -> center.x
            },
            when (position) {
                KamsyOverlayDrawable.StatusPosition.TOP_LEFT -> center.y - offset
                KamsyOverlayDrawable.StatusPosition.TOP_RIGHT -> center.y - offset
                KamsyOverlayDrawable.StatusPosition.BOTTOM_LEFT -> center.y + offset
                KamsyOverlayDrawable.StatusPosition.BOTTOM_RIGHT -> center.y + offset
                KamsyOverlayDrawable.StatusPosition.CENTER -> center.y
            }
        )
    }
}

/**
 * Badge overlay layer
 */
class BadgeLayer(
    @param:ColorInt private val backgroundColor: Int,
    @param:ColorInt private val textColor: Int,
    private val text: String,
    private val size: Float,
    private val position: KamsyOverlayDrawable.StatusPosition,
    private var center: PointF,
    private var radius: Float
) : OverlayLayer() {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        textSize = size * 0.6f
        typeface = Typeface.DEFAULT_BOLD
    }

    private var badgeCenter = PointF()

    init {
        calculatePosition()
    }

    override fun draw(canvas: Canvas) {
        backgroundPaint.alpha = (255 * alpha).toInt()
        textPaint.alpha = (255 * alpha).toInt()

        // Draw background circle
        canvas.drawCircle(badgeCenter.x, badgeCenter.y, size / 2f, backgroundPaint)

        // Draw text
        val textY = badgeCenter.y + (textPaint.textSize / 3f)
        canvas.drawText(text, badgeCenter.x, textY, textPaint)
    }

    override fun updateBounds(bounds: RectF, center: PointF, radius: Float) {
        this.center = center
        this.radius = radius
        calculatePosition()
    }

    private fun calculatePosition() {
        val offset = radius * 0.8f

        badgeCenter.set(
            when (position) {
                KamsyOverlayDrawable.StatusPosition.TOP_LEFT -> center.x - offset
                KamsyOverlayDrawable.StatusPosition.TOP_RIGHT -> center.x + offset
                KamsyOverlayDrawable.StatusPosition.BOTTOM_LEFT -> center.x - offset
                KamsyOverlayDrawable.StatusPosition.BOTTOM_RIGHT -> center.x + offset
                KamsyOverlayDrawable.StatusPosition.CENTER -> center.x
            },
            when (position) {
                KamsyOverlayDrawable.StatusPosition.TOP_LEFT -> center.y - offset
                KamsyOverlayDrawable.StatusPosition.TOP_RIGHT -> center.y - offset
                KamsyOverlayDrawable.StatusPosition.BOTTOM_LEFT -> center.y + offset
                KamsyOverlayDrawable.StatusPosition.BOTTOM_RIGHT -> center.y + offset
                KamsyOverlayDrawable.StatusPosition.CENTER -> center.y
            }
        )
    }
}

/**
 * Extension functions for KamsyOverlayDrawable
 */
fun KamsyOverlayDrawable.applyOnlineStatus() {
    addStatusIndicator(
        color = Color.GREEN,
        position = KamsyOverlayDrawable.StatusPosition.BOTTOM_RIGHT
    )
}

fun KamsyOverlayDrawable.applyOfflineStatus() {
    addStatusIndicator(
        color = Color.GRAY,
        position = KamsyOverlayDrawable.StatusPosition.BOTTOM_RIGHT
    )
}

fun KamsyOverlayDrawable.applyNotificationBadge(count: Int) {
    val text = if (count > 99) "99+" else count.toString()
    addBadge(
        backgroundColor = Color.RED,
        textColor = Color.WHITE,
        text = text,
        position = KamsyOverlayDrawable.StatusPosition.TOP_RIGHT
    )
}

/**
 * Factory function for creating overlay drawable
 */
fun createOverlayDrawable(kamsyView: KamsyView): KamsyOverlayDrawable {
    return KamsyOverlayDrawable(kamsyView)
}
