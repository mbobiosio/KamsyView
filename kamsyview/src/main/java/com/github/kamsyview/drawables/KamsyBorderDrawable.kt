package com.github.kamsyview.drawables

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.graphics.withSave
import com.github.kamsyview.core.KamsyView
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * KamsyBorderDrawable - Handles advanced border rendering with arches, gradients, and animations
 * Supports circular borders, arch patterns, gradient effects, and smooth animations
 */
class KamsyBorderDrawable(
    private val kamsyView: KamsyView
) : Drawable() {

    companion object {
        private val DEFAULT_STROKE_CAP = Paint.Cap.ROUND
        private const val ARCH_SPACING_RATIO = 0.1f
        private const val GRADIENT_EDGE_SOFTNESS = 0.9f
        private const val ANIMATION_STEP = 2f
    }

    // Border configuration
    private var borderWidth: Int = 0
    private var borderColor: Int = Color.BLACK
    private var borderColorSecondary: Int? = null
    private var gradientAngle: Int = 0
    private var archesCount: Int = 0
    private var archesDegreeArea: Int = 0
    private var archesAngle: Int = 0
    private var archesType: KamsyView.ArchesType = KamsyView.ArchesType.SINGLE
    private var avatarMargin: Int = 0

    // Paint objects
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = DEFAULT_STROKE_CAP
        strokeJoin = Paint.Join.ROUND
        isDither = true
    }

    private val archPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = DEFAULT_STROKE_CAP
        strokeJoin = Paint.Join.ROUND
        isDither = true
    }

    // Geometry properties
    private var circleRadius = 0f
    private var circleCenter = PointF()
    private var borderRect = RectF()
    private var archRect = RectF()

    // Animation properties
    private var animationProgress = 0f
    private var isAnimating = false

    // Computed properties
    private val effectiveBorderWidth: Float
        get() = borderWidth.toFloat()

    private val strokeRadius: Float
        get() = circleRadius - (effectiveBorderWidth / 2f)

    private val totalArchesDegreeArea: Float
        get() = archesDegreeArea.toFloat()

    private val individualArchDegreeLength: Float
        get() = calculateIndividualArchLength()

    private val spaceBetweenArches: Float
        get() = calculateSpaceBetweenArches()

    private val currentAnimationArchesArea: Float
        get() = totalArchesDegreeArea * animationProgress

    init {
        update()
    }

    override fun draw(canvas: Canvas) {
        if (borderWidth <= 0) return

        canvas.withSave {
            val shouldDraw = shouldDrawArches()

            when {
                shouldDraw -> {
                    drawArchBorder(this)
                }
                else -> {
                    drawCircleBorder(this)
                }
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        borderPaint.alpha = alpha
        archPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        borderPaint.colorFilter = colorFilter
        archPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("This method is no longer used in graphics optimizations")
    override fun getOpacity(): Int = when (borderPaint.alpha) {
        255 -> PixelFormat.OPAQUE
        0 -> PixelFormat.TRANSPARENT
        else -> PixelFormat.TRANSLUCENT
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        calculateGeometry()
        updatePaints()
    }

    /**
     * Update border properties from KamsyView
     */
    fun update() {
        borderWidth = kamsyView.borderWidth
        borderColor = kamsyView.borderColor
        borderColorSecondary = kamsyView.borderColorSecondary
        gradientAngle = kamsyView.borderGradientAngle
        archesCount = kamsyView.archesCount
        archesDegreeArea = kamsyView.archesDegreeArea
        archesAngle = kamsyView.archesAngle
        archesType = kamsyView.archesType
        avatarMargin = kamsyView.avatarMargin

        if (!isAnimating) {
            animationProgress = 1f
        }

        calculateGeometry()
        updatePaints()
        invalidateSelf()
    }

    /**
     * Start border animation
     */
    fun startAnimation() {
        isAnimating = true
        animateProgress()
    }

    /**
     * Stop border animation
     */
    fun stopAnimation() {
        isAnimating = false
        animationProgress = 0f
        invalidateSelf()
    }

    /**
     * Set animation progress manually
     */
    fun setAnimationProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float) {
        animationProgress = progress.coerceIn(0f, 1f)
        invalidateSelf()
    }

    // Private methods
    private fun calculateGeometry() {
        val bounds = bounds
        if (bounds.isEmpty) return

        val size = minOf(bounds.width(), bounds.height())
        circleRadius = size / 2f
        circleCenter.set(bounds.exactCenterX(), bounds.exactCenterY())

        val strokeOffset = effectiveBorderWidth / 2f
        borderRect.set(
            circleCenter.x - strokeRadius,
            circleCenter.y - strokeRadius,
            circleCenter.x + strokeRadius,
            circleCenter.y + strokeRadius
        )

        archRect.set(borderRect)
        archRect.inset(strokeOffset, strokeOffset)
    }

    private fun updatePaints() {
        val effectiveSecondaryColor = borderColorSecondary ?: borderColor

        borderPaint.apply {
            strokeWidth = effectiveBorderWidth
            shader = createGradientShader(
                borderColor,
                effectiveSecondaryColor,
                gradientAngle,
                borderRect
            )
        }

        archPaint.apply {
            strokeWidth = effectiveBorderWidth
            shader = borderPaint.shader
        }
    }

    private fun shouldDrawArches(): Boolean {
        val condition1 = archesType == KamsyView.ArchesType.SINGLE && archesCount > 1
        val condition2 = archesType == KamsyView.ArchesType.MIRROR && archesCount > 0
        val degreeAreaCheck = totalArchesDegreeArea > 0f

        return (condition1 || condition2) && degreeAreaCheck
    }

    private fun drawCircleBorder(canvas: Canvas) {
        canvas.drawCircle(
            circleCenter.x,
            circleCenter.y,
            strokeRadius,
            borderPaint
        )
    }

    private fun drawArchBorder(canvas: Canvas) {
        val path = Path()
        val baseAngle = 270f + (archesAngle.toFloat() * animationProgress)

        when (archesType) {
            KamsyView.ArchesType.SINGLE -> {
                drawArchesPattern(path, baseAngle)
                drawMainArc(path, baseAngle)
            }
            KamsyView.ArchesType.MIRROR -> {
                drawArchesPattern(path, baseAngle)
                drawArchesPattern(path, baseAngle + 180f)
            }
        }

        canvas.drawPath(path, archPaint)
    }

    private fun drawArchesPattern(path: Path, startAngle: Float) {
        val startSpace = if (totalArchesDegreeArea == 360f ||
            archesType == KamsyView.ArchesType.MIRROR) {
            0f
        } else {
            individualArchDegreeLength
        }

        repeat(archesCount) { index ->
            val arcDegree = (individualArchDegreeLength + spaceBetweenArches) * index
            val angle = startAngle + startSpace + arcDegree

            path.addArc(
                archRect,
                angle,
                individualArchDegreeLength * animationProgress.coerceIn(0.1f, 1f)
            )
        }
    }

    private fun drawMainArc(path: Path, startAngle: Float) {
        if (archesType == KamsyView.ArchesType.SINGLE) {
            val mainArcStart = startAngle + currentAnimationArchesArea
            val mainArcSweep = 360f - currentAnimationArchesArea

            if (mainArcSweep > 0) {
                path.addArc(archRect, mainArcStart, mainArcSweep)
            }
        }
    }

    private fun calculateIndividualArchLength(): Float = when (archesType) {
        KamsyView.ArchesType.SINGLE -> {
            if (archesCount > 0) {
                totalArchesDegreeArea / ((archesCount * 2) + 1)
            } else {
                0f
            }
        }
        KamsyView.ArchesType.MIRROR -> {
            if (archesCount > 0) {
                totalArchesDegreeArea / archesCount
            } else {
                0f
            }
        }
    }

    private fun calculateSpaceBetweenArches(): Float {
        val totalArchLength = archesCount * individualArchDegreeLength
        val remainingSpace = totalArchesDegreeArea - totalArchLength

        return when {
            totalArchesDegreeArea == 360f -> remainingSpace / archesCount
            archesCount > 0 -> remainingSpace / (archesCount + 1)
            else -> 0f
        }
    }

    private fun createGradientShader(
        @ColorInt colorStart: Int,
        @ColorInt colorEnd: Int,
        @IntRange(from = 0, to = 360) angle: Int,
        rect: RectF
    ): LinearGradient {
        val angleInRadians = Math.toRadians(angle.toDouble())
        val size = maxOf(rect.width(), rect.height())
        val radius = size / 2f
        val centerX = rect.centerX()
        val centerY = rect.centerY()

        val startX = (centerX - radius * cos(angleInRadians)).toFloat()
        val startY = (centerY - radius * sin(angleInRadians)).toFloat()
        val endX = (centerX + radius * cos(angleInRadians)).toFloat()
        val endY = (centerY + radius * sin(angleInRadians)).toFloat()

        return LinearGradient(
            startX.coerceIn(rect.left, rect.right),
            startY.coerceIn(rect.top, rect.bottom),
            endX.coerceIn(rect.left, rect.right),
            endY.coerceIn(rect.top, rect.bottom),
            intArrayOf(colorStart, colorEnd),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun animateProgress() {
        if (!isAnimating) return

        animationProgress = (animationProgress + ANIMATION_STEP / 360f) % 1f
        invalidateSelf()

        // Schedule next frame
        kamsyView.post { animateProgress() }
    }

    /**
     * Builder class for creating KamsyBorderDrawable with custom configuration
     */
    class Builder {
        private var borderWidth: Int = 0
        private var borderColor: Int = Color.BLACK
        private var borderColorSecondary: Int? = null
        private var gradientAngle: Int = 0
        private var archesCount: Int = 0
        private var archesDegreeArea: Int = 0
        private var archesAngle: Int = 0
        private var archesType: KamsyView.ArchesType = KamsyView.ArchesType.SINGLE
        private var avatarMargin: Int = 0

        fun borderWidth(width: Int) = apply { this.borderWidth = width }

        fun borderColor(@ColorInt color: Int) = apply { this.borderColor = color }

        fun borderColorSecondary(@ColorInt color: Int?) = apply {
            this.borderColorSecondary = color
        }

        fun gradientAngle(@IntRange(from = 0, to = 360) angle: Int) = apply {
            this.gradientAngle = angle.coerceIn(0, 360)
        }

        fun archesCount(count: Int) = apply { this.archesCount = count.coerceAtLeast(0) }

        fun archesDegreeArea(@IntRange(from = 0, to = 360) area: Int) = apply {
            this.archesDegreeArea = area.coerceIn(0, 360)
        }

        fun archesAngle(@IntRange(from = 0, to = 360) angle: Int) = apply {
            this.archesAngle = angle.coerceIn(0, 360)
        }

        fun archesType(type: KamsyView.ArchesType) = apply { this.archesType = type }

        fun avatarMargin(margin: Int) = apply { this.avatarMargin = margin }

        fun build(kamsyView: KamsyView): KamsyBorderDrawable {
            return KamsyBorderDrawable(kamsyView).apply {
                this.borderWidth = this@Builder.borderWidth
                this.borderColor = this@Builder.borderColor
                this.borderColorSecondary = this@Builder.borderColorSecondary
                this.gradientAngle = this@Builder.gradientAngle
                this.archesCount = this@Builder.archesCount
                this.archesDegreeArea = this@Builder.archesDegreeArea
                this.archesAngle = this@Builder.archesAngle
                this.archesType = this@Builder.archesType
                this.avatarMargin = this@Builder.avatarMargin
                update()
            }
        }
    }
}

/**
 * Extension functions for KamsyBorderDrawable
 */
fun KamsyBorderDrawable.animateArches(
    duration: Long = 2000L,
    onComplete: (() -> Unit)? = null
) {
    startAnimation()

    // Stop animation after duration
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed({
        stopAnimation()
        onComplete?.invoke()
    }, duration)
}

fun KamsyBorderDrawable.pulseAnimation(
    pulseCount: Int = 3,
    pulseDuration: Long = 500L,
    onComplete: (() -> Unit)? = null
) {
    var currentPulse = 0

    fun nextPulse() {
        if (currentPulse < pulseCount) {
            startAnimation()
            Handler(Looper.getMainLooper()).postDelayed({
                stopAnimation()
                currentPulse++
                nextPulse()
            }, pulseDuration)
        } else {
            onComplete?.invoke()
        }
    }

    nextPulse()
}

/**
 * Factory function for creating border drawable
 */
fun createBorderDrawable(
    kamsyView: KamsyView,
    block: KamsyBorderDrawable.Builder.() -> Unit = {}
): KamsyBorderDrawable = KamsyBorderDrawable.Builder()
    .apply(block)
    .build(kamsyView)