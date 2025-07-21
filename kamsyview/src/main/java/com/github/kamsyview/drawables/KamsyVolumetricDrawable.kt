package com.github.kamsyview.drawables

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.annotation.FloatRange
import androidx.core.graphics.withSave
import com.github.kamsyview.core.KamsyView
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * KamsyVolumetricDrawable - Handles volumetric gradient effects for depth perception
 * Creates realistic 3D-like effects with configurable gradients, highlights, and shadows
 */
class KamsyVolumetricDrawable(
    private val kamsyView: KamsyView
) : Drawable() {

    companion object {
        private const val DEFAULT_HIGHLIGHT_ALPHA = 0.3f
        private const val DEFAULT_SHADOW_ALPHA = 0.2f
        private const val DEFAULT_AMBIENT_ALPHA = 0.1f
        private const val LIGHT_ANGLE_DEGREES = 315f // Top-left light source
        private const val GRADIENT_RADIUS_RATIO = 0.8f
        private const val HIGHLIGHT_RADIUS_RATIO = 0.6f
        private const val SHADOW_OFFSET_RATIO = 0.1f
    }

    // Volumetric configuration
    private var volumetricType: KamsyView.VolumetricType = KamsyView.VolumetricType.NONE
    private var avatarMargin: Int = 0
    private var hasDrawableContent: Boolean = false
    private var highlightIntensity: Float = DEFAULT_HIGHLIGHT_ALPHA
    private var shadowIntensity: Float = DEFAULT_SHADOW_ALPHA
    private var ambientIntensity: Float = DEFAULT_AMBIENT_ALPHA

    // Paint objects for different effects
    private val volumetricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
        isFilterBitmap = true
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }

    private val ambientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
    }

    // Geometry properties
    private var circleRadius = 0f
    private var circleCenter = PointF()
    private var volumetricRadius = 0f
    private var lightPosition = PointF()
    private var shadowOffset = PointF()

    // Gradient properties
    private var primaryGradient: RadialGradient? = null
    private var highlightGradient: RadialGradient? = null
    private var shadowGradient: RadialGradient? = null
    private var ambientGradient: LinearGradient? = null

    init {
        update()
    }

    override fun draw(canvas: Canvas) {
        if (!shouldDrawVolumetric()) return

        canvas.withSave {

            // Create circular clip path
            val clipPath = Path().apply {
                addCircle(circleCenter.x, circleCenter.y, volumetricRadius, Path.Direction.CW)
            }
            clipPath(clipPath)

            // Draw volumetric effects in order
            drawAmbientEffect(this)
            drawPrimaryVolumetricEffect(this)
            drawHighlightEffect(this)
            drawShadowEffect(this)

        }
    }

    override fun setAlpha(alpha: Int) {
        val normalizedAlpha = alpha / 255f
        volumetricPaint.alpha = (255 * normalizedAlpha).toInt()
        highlightPaint.alpha = (255 * normalizedAlpha * highlightIntensity).toInt()
        shadowPaint.alpha = (255 * normalizedAlpha * shadowIntensity).toInt()
        ambientPaint.alpha = (255 * normalizedAlpha * ambientIntensity).toInt()
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        volumetricPaint.colorFilter = colorFilter
        highlightPaint.colorFilter = colorFilter
        shadowPaint.colorFilter = colorFilter
        ambientPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        if (right - left > 0 && bottom - top > 0) {
            calculateGeometry()
            createGradients()
        }
    }

    /**
     * Update volumetric properties from KamsyView
     */
    fun update() {
        volumetricType = kamsyView.volumetricType
        avatarMargin = kamsyView.avatarMargin.coerceAtLeast(0)
        hasDrawableContent = kamsyView.drawable != null

        if (bounds.width() > 0 && bounds.height() > 0) {
            calculateGeometry()
            createGradients()
        }
        invalidateSelf()
    }

    /**
     * Set custom volumetric intensities
     */
    fun setIntensities(
        @FloatRange(from = 0.0, to = 1.0) highlight: Float = highlightIntensity,
        @FloatRange(from = 0.0, to = 1.0) shadow: Float = shadowIntensity,
        @FloatRange(from = 0.0, to = 1.0) ambient: Float = ambientIntensity
    ) {
        highlightIntensity = highlight.coerceIn(0f, 1f)
        shadowIntensity = shadow.coerceIn(0f, 1f)
        ambientIntensity = ambient.coerceIn(0f, 1f)

        createGradients()
        invalidateSelf()
    }

    /**
     * Set custom light position
     */
    fun setLightPosition(x: Float, y: Float) {
        lightPosition.set(x, y)
        calculateShadowOffset()
        createGradients()
        invalidateSelf()
    }

    /**
     * Animate volumetric effect intensity
     */
    fun animateIntensity(
        targetIntensity: Float,
        duration: Long = 1000L,
        onComplete: (() -> Unit)? = null
    ) {
        val startIntensity = highlightIntensity
        val intensityDelta = targetIntensity - startIntensity
        val startTime = System.currentTimeMillis()

        fun updateIntensity() {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

            if (progress < 1f) {
                val currentIntensity = startIntensity + (intensityDelta * progress)
                setIntensities(highlight = currentIntensity)

                kamsyView.post { updateIntensity() }
            } else {
                setIntensities(highlight = targetIntensity)
                onComplete?.invoke()
            }
        }

        updateIntensity()
    }

    // Private methods
    private fun shouldDrawVolumetric(): Boolean = when (volumetricType) {
        KamsyView.VolumetricType.NONE -> false
        KamsyView.VolumetricType.ALL -> true
        KamsyView.VolumetricType.DRAWABLE -> hasDrawableContent
        KamsyView.VolumetricType.PLACEHOLDER -> !hasDrawableContent
    }

    private fun calculateGeometry() {
        val bounds = bounds
        if (bounds.isEmpty) return

        val size = minOf(bounds.width(), bounds.height())
        circleRadius = (size / 2f).coerceAtLeast(1f) - avatarMargin.coerceAtMost(size / 2 - 1)
        volumetricRadius = (circleRadius * GRADIENT_RADIUS_RATIO).coerceAtLeast(1f)
        circleCenter.set(bounds.exactCenterX(), bounds.exactCenterY())

        calculateLightPosition()
        calculateShadowOffset()
    }

    private fun calculateLightPosition() {
        val lightAngleRadians = Math.toRadians(LIGHT_ANGLE_DEGREES.toDouble())
        val lightDistance = volumetricRadius * 0.7f

        lightPosition.set(
            circleCenter.x + (lightDistance * cos(lightAngleRadians)).toFloat(),
            circleCenter.y + (lightDistance * sin(lightAngleRadians)).toFloat()
        )
    }

    private fun calculateShadowOffset() {
        val shadowDistance = volumetricRadius * SHADOW_OFFSET_RATIO
        val shadowAngle = Math.toRadians((LIGHT_ANGLE_DEGREES + 180f).toDouble())

        shadowOffset.set(
            (shadowDistance * cos(shadowAngle)).toFloat(),
            (shadowDistance * sin(shadowAngle)).toFloat()
        )
    }

    private fun createGradients() {
        createPrimaryGradient()
        createHighlightGradient()
        createShadowGradient()
        createAmbientGradient()
    }

    private fun createPrimaryGradient() {
        // Validate radius is positive
        if (volumetricRadius <= 0) return

        val colors = intArrayOf(
            Color.argb(0, 255, 255, 255),
            Color.argb((255 * 0.4f).toInt(), 255, 255, 255),
            Color.argb(0, 255, 255, 255)
        )

        val positions = floatArrayOf(0f, 0.4f, 1f)

        primaryGradient = RadialGradient(
            circleCenter.x,
            circleCenter.y,
            volumetricRadius.coerceAtLeast(1f), // Ensure minimum radius of 1
            colors,
            positions,
            Shader.TileMode.CLAMP
        )

        volumetricPaint.shader = primaryGradient
    }

    private fun createHighlightGradient() {
        if (volumetricRadius <= 0) return

        val highlightRadius = (volumetricRadius * HIGHLIGHT_RADIUS_RATIO).coerceAtLeast(1f)
        val highlightAlpha = (255 * highlightIntensity).toInt()

        val colors = intArrayOf(
            Color.argb(highlightAlpha, 255, 255, 255),
            Color.argb(highlightAlpha / 2, 255, 255, 255),
            Color.argb(0, 255, 255, 255)
        )

        val positions = floatArrayOf(0f, 0.6f, 1f)

        highlightGradient = RadialGradient(
            lightPosition.x,
            lightPosition.y,
            highlightRadius,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )

        highlightPaint.shader = highlightGradient
    }

    private fun createShadowGradient() {
        val shadowRadius = volumetricRadius * 0.8f
        val shadowAlpha = (255 * shadowIntensity).toInt()
        val shadowCenterX = circleCenter.x + shadowOffset.x
        val shadowCenterY = circleCenter.y + shadowOffset.y

        val colors = intArrayOf(
            Color.argb(0, 0, 0, 0),
            Color.argb(shadowAlpha / 3, 0, 0, 0),
            Color.argb(shadowAlpha, 0, 0, 0)
        )

        val positions = floatArrayOf(0f, 0.5f, 1f)

        shadowGradient = RadialGradient(
            shadowCenterX,
            shadowCenterY,
            shadowRadius,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )

        shadowPaint.shader = shadowGradient
    }

    private fun createAmbientGradient() {
        val ambientAlpha = (255 * ambientIntensity).toInt()

        val colors = intArrayOf(
            Color.argb(0, 255, 255, 255),
            Color.argb(ambientAlpha, 255, 255, 255)
        )

        ambientGradient = LinearGradient(
            circleCenter.x,
            circleCenter.y - volumetricRadius,
            circleCenter.x,
            circleCenter.y + volumetricRadius,
            colors,
            null,
            Shader.TileMode.CLAMP
        )

        ambientPaint.shader = ambientGradient
    }

    private fun drawAmbientEffect(canvas: Canvas) {
        ambientGradient?.let {
            canvas.drawCircle(circleCenter.x, circleCenter.y, volumetricRadius, ambientPaint)
        }
    }

    private fun drawPrimaryVolumetricEffect(canvas: Canvas) {
        primaryGradient?.let {
            canvas.drawCircle(circleCenter.x, circleCenter.y, volumetricRadius, volumetricPaint)
        }
    }

    private fun drawHighlightEffect(canvas: Canvas) {
        highlightGradient?.let {
            val highlightRadius = volumetricRadius * HIGHLIGHT_RADIUS_RATIO
            canvas.drawCircle(lightPosition.x, lightPosition.y, highlightRadius, highlightPaint)
        }
    }

    private fun drawShadowEffect(canvas: Canvas) {
        shadowGradient?.let {
            val shadowRadius = volumetricRadius * 0.8f
            val shadowCenterX = circleCenter.x + shadowOffset.x
            val shadowCenterY = circleCenter.y + shadowOffset.y
            canvas.drawCircle(shadowCenterX, shadowCenterY, shadowRadius, shadowPaint)
        }
    }

    /**
     * Builder class for creating custom volumetric effects
     */
    class Builder {
        private var highlightIntensity: Float = DEFAULT_HIGHLIGHT_ALPHA
        private var shadowIntensity: Float = DEFAULT_SHADOW_ALPHA
        private var ambientIntensity: Float = DEFAULT_AMBIENT_ALPHA
        private var lightAngle: Float = LIGHT_ANGLE_DEGREES
        private var volumetricType: KamsyView.VolumetricType = KamsyView.VolumetricType.ALL

        fun highlightIntensity(@FloatRange(from = 0.0, to = 1.0) intensity: Float) = apply {
            this.highlightIntensity = intensity.coerceIn(0f, 1f)
        }

        fun shadowIntensity(@FloatRange(from = 0.0, to = 1.0) intensity: Float) = apply {
            this.shadowIntensity = intensity.coerceIn(0f, 1f)
        }

        fun ambientIntensity(@FloatRange(from = 0.0, to = 1.0) intensity: Float) = apply {
            this.ambientIntensity = intensity.coerceIn(0f, 1f)
        }

        fun lightAngle(@FloatRange(from = 0.0, to = 360.0) angle: Float) = apply {
            this.lightAngle = angle % 360f
        }

        fun volumetricType(type: KamsyView.VolumetricType) = apply {
            this.volumetricType = type
        }

        fun build(kamsyView: KamsyView): KamsyVolumetricDrawable {
            return KamsyVolumetricDrawable(kamsyView).apply {
                setIntensities(
                    highlight = this@Builder.highlightIntensity,
                    shadow = this@Builder.shadowIntensity,
                    ambient = this@Builder.ambientIntensity
                )

                // Set custom light position based on angle
                val lightAngleRadians = Math.toRadians(this@Builder.lightAngle.toDouble())
                val lightDistance = volumetricRadius * 0.7f
                setLightPosition(
                    circleCenter.x + (lightDistance * cos(lightAngleRadians)).toFloat(),
                    circleCenter.y + (lightDistance * sin(lightAngleRadians)).toFloat()
                )

                update()
            }
        }
    }
}

/**
 * Extension functions for KamsyVolumetricDrawable
 */
fun KamsyVolumetricDrawable.pulseVolumetric(
    minIntensity: Float = 0.1f,
    maxIntensity: Float = 0.5f,
    duration: Long = 1000L,
    cycles: Int = 3,
    onComplete: (() -> Unit)? = null
) {
    var currentCycle = 0

    fun nextCycle() {
        if (currentCycle < cycles) {
            animateIntensity(maxIntensity, duration / 2) {
                animateIntensity(minIntensity, duration / 2) {
                    currentCycle++
                    nextCycle()
                }
            }
        } else {
            onComplete?.invoke()
        }
    }

    nextCycle()
}

fun KamsyVolumetricDrawable.breatheEffect(
    baseIntensity: Float = 0.2f,
    peakIntensity: Float = 0.4f,
    duration: Long = 2000L,
    onComplete: (() -> Unit)? = null
) {
    animateIntensity(peakIntensity, duration / 2) {
        animateIntensity(baseIntensity, duration / 2) {
            onComplete?.invoke()
        }
    }
}

/**
 * Factory function for creating volumetric drawable
 */
fun createVolumetricDrawable(
    kamsyView: KamsyView,
    block: KamsyVolumetricDrawable.Builder.() -> Unit = {}
): KamsyVolumetricDrawable = KamsyVolumetricDrawable.Builder()
    .apply(block)
    .build(kamsyView)

/**
 * Predefined volumetric styles
 */
object VolumetricStyles {
    fun subtle(kamsyView: KamsyView) = createVolumetricDrawable(kamsyView) {
        highlightIntensity(0.15f)
        shadowIntensity(0.1f)
        ambientIntensity(0.05f)
    }

    fun dramatic(kamsyView: KamsyView) = createVolumetricDrawable(kamsyView) {
        highlightIntensity(0.6f)
        shadowIntensity(0.4f)
        ambientIntensity(0.2f)
    }

    fun soft(kamsyView: KamsyView) = createVolumetricDrawable(kamsyView) {
        highlightIntensity(0.25f)
        shadowIntensity(0.15f)
        ambientIntensity(0.1f)
        lightAngle(45f)
    }

    fun backlit(kamsyView: KamsyView) = createVolumetricDrawable(kamsyView) {
        highlightIntensity(0.4f)
        shadowIntensity(0.2f)
        ambientIntensity(0.3f)
        lightAngle(180f)
    }
}
