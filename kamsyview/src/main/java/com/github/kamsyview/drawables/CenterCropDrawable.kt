package com.github.kamsyview.drawables

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * CenterCropDrawable - Advanced center crop implementation for KamsyView
 * Supports various drawable types with proper scaling, clipping, and transformation
 */
class CenterCropDrawable(
    private val target: Drawable,
    private val scaleType: ScaleType = ScaleType.CENTER_CROP,
    private val cropGravity: CropGravity = CropGravity.CENTER
) : Drawable(), Drawable.Callback {

    companion object {
        private const val DEFAULT_MATRIX_SCALE = 1.0f
        private const val MIN_SCALE_FACTOR = 0.1f
        private const val MAX_SCALE_FACTOR = 10.0f
    }

    // Transformation properties
    private val transformMatrix = Matrix()
    private val clipPath = Path()
    private val tempRectF = RectF()
    private val targetBounds = RectF()
    private val viewBounds = RectF()

    // Drawing properties
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }

    // State management
    private var isClippingEnabled = true
    private var customScaleFactor = DEFAULT_MATRIX_SCALE
    private var rotationAngle = 0f
    private var translationX = 0f
    private var translationY = 0f
    private var scaleCalculated = false

    // Animation support
    private var animationCallback: AnimationCallback? = null

    init {
        target.callback = this
        calculateTransformation()
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return

        canvas.save()

        try {
            // Apply clipping if enabled
            if (isClippingEnabled) {
                canvas.clipPath(clipPath)
            }

            // Apply transformation matrix
            canvas.concat(transformMatrix)

            // Draw the target drawable
            target.draw(canvas)

            // Handle animation callback
            animationCallback?.onFrame(canvas)

        } catch (e: Exception) {
            // Graceful error handling
            drawFallback(canvas)
        } finally {
            canvas.restore()
        }
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        viewBounds.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        calculateTransformation()
        updateClipPath()
    }

    override fun setBounds(bounds: Rect) {
        setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
        target.alpha = alpha
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        target.colorFilter = colorFilter
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = target.opacity

    override fun getIntrinsicWidth(): Int = target.intrinsicWidth

    override fun getIntrinsicHeight(): Int = target.intrinsicHeight

    override fun isStateful(): Boolean = target.isStateful

    override fun onStateChange(state: IntArray): Boolean = target.setState(state)

    override fun getConstantState(): ConstantState? = target.constantState

    override fun mutate(): Drawable {
        target.mutate()
        return this
    }

    // Drawable.Callback implementation
    override fun invalidateDrawable(who: Drawable) {
        invalidateSelf()
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        scheduleSelf(what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        unscheduleSelf(what)
    }

    /**
     * Set custom scale factor
     */
    fun setScaleFactor(@FloatRange(from = 0.1, to = 10.0) factor: Float) {
        customScaleFactor = factor.coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)
        calculateTransformation()
        invalidateSelf()
    }

    /**
     * Set rotation angle
     */
    fun setRotation(@FloatRange(from = -360.0, to = 360.0) angle: Float) {
        rotationAngle = angle % 360f
        calculateTransformation()
        invalidateSelf()
    }

    /**
     * Set translation offset
     */
    fun setTranslation(x: Float, y: Float) {
        translationX = x
        translationY = y
        calculateTransformation()
        invalidateSelf()
    }

    /**
     * Enable or disable clipping
     */
    fun setClippingEnabled(enabled: Boolean) {
        isClippingEnabled = enabled
        invalidateSelf()
    }

    /**
     * Set scale type
     */
    fun setScaleType(newScaleType: ScaleType) {
        if (scaleType != newScaleType) {
            // Would need to create a new instance with different scale type
            // This is intentionally left as immutable for thread safety
        }
    }

    /**
     * Set animation callback
     */
    fun setAnimationCallback(callback: AnimationCallback?) {
        animationCallback = callback
    }

    /**
     * Get current transformation matrix
     */
    fun getTransformationMatrix(): Matrix = Matrix(transformMatrix)

    /**
     * Get current scale factor
     */
    fun getCurrentScale(): Float {
        val values = FloatArray(9)
        transformMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    /**
     * Check if drawable is animatable
     */
    fun isAnimatable(): Boolean = target is Animatable

    /**
     * Start animation if drawable is animatable
     */
    fun startAnimation() {
        (target as? Animatable)?.start()
    }

    /**
     * Stop animation if drawable is animatable
     */
    fun stopAnimation() {
        (target as? Animatable)?.stop()
    }

    /**
     * Check if animation is running
     */
    fun isAnimationRunning(): Boolean = (target as? Animatable)?.isRunning ?: false

    // Private methods

    private fun calculateTransformation() {
        if (viewBounds.isEmpty || target.intrinsicWidth <= 0 || target.intrinsicHeight <= 0) {
            return
        }

        val intrinsicWidth = target.intrinsicWidth.toFloat()
        val intrinsicHeight = target.intrinsicHeight.toFloat()
        val viewWidth = viewBounds.width()
        val viewHeight = viewBounds.height()

        // Calculate source rectangle
        val sourceRect = RectF(0f, 0f, intrinsicWidth, intrinsicHeight)

        // Calculate destination rectangle based on scale type
        val destRect = when (scaleType) {
            ScaleType.CENTER_CROP -> calculateCenterCropRect(
                sourceRect, viewWidth, viewHeight
            )
            ScaleType.CENTER_INSIDE -> calculateCenterInsideRect(
                sourceRect, viewWidth, viewHeight
            )
            ScaleType.FIT_CENTER -> calculateFitCenterRect(
                sourceRect, viewWidth, viewHeight
            )
            ScaleType.FIT_XY -> RectF(viewBounds)
            ScaleType.MATRIX -> calculateMatrixRect(sourceRect)
        }

        // Apply crop gravity adjustments
        applyCropGravity(destRect, viewWidth, viewHeight)

        // Calculate transformation matrix
        transformMatrix.reset()
        transformMatrix.setRectToRect(sourceRect, destRect, Matrix.ScaleToFit.FILL)

        // Apply custom transformations
        applyCustomTransformations(destRect)

        // Update target bounds
        targetBounds.set(destRect)
        target.setBounds(
            destRect.left.roundToInt(),
            destRect.top.roundToInt(),
            destRect.right.roundToInt(),
            destRect.bottom.roundToInt()
        )

        scaleCalculated = true
    }

    private fun calculateCenterCropRect(
        sourceRect: RectF,
        viewWidth: Float,
        viewHeight: Float
    ): RectF {
        val sourceAspectRatio = sourceRect.width() / sourceRect.height()
        val viewAspectRatio = viewWidth / viewHeight

        val scale = if (sourceAspectRatio > viewAspectRatio) {
            // Source is wider, scale to fit height
            viewHeight / sourceRect.height()
        } else {
            // Source is taller, scale to fit width
            viewWidth / sourceRect.width()
        }

        val scaledWidth = sourceRect.width() * scale
        val scaledHeight = sourceRect.height() * scale

        val left = (viewWidth - scaledWidth) / 2f
        val top = (viewHeight - scaledHeight) / 2f

        return RectF(
            viewBounds.left + left,
            viewBounds.top + top,
            viewBounds.left + left + scaledWidth,
            viewBounds.top + top + scaledHeight
        )
    }

    private fun calculateCenterInsideRect(
        sourceRect: RectF,
        viewWidth: Float,
        viewHeight: Float
    ): RectF {
        val sourceAspectRatio = sourceRect.width() / sourceRect.height()
        val viewAspectRatio = viewWidth / viewHeight

        val scale = min(
            viewWidth / sourceRect.width(),
            viewHeight / sourceRect.height()
        )

        val scaledWidth = sourceRect.width() * scale
        val scaledHeight = sourceRect.height() * scale

        val left = (viewWidth - scaledWidth) / 2f
        val top = (viewHeight - scaledHeight) / 2f

        return RectF(
            viewBounds.left + left,
            viewBounds.top + top,
            viewBounds.left + left + scaledWidth,
            viewBounds.top + top + scaledHeight
        )
    }

    private fun calculateFitCenterRect(
        sourceRect: RectF,
        viewWidth: Float,
        viewHeight: Float
    ): RectF {
        val scale = min(
            viewWidth / sourceRect.width(),
            viewHeight / sourceRect.height()
        )

        val scaledWidth = sourceRect.width() * scale
        val scaledHeight = sourceRect.height() * scale

        val left = (viewWidth - scaledWidth) / 2f
        val top = (viewHeight - scaledHeight) / 2f

        return RectF(
            viewBounds.left + left,
            viewBounds.top + top,
            viewBounds.left + left + scaledWidth,
            viewBounds.top + top + scaledHeight
        )
    }

    private fun calculateMatrixRect(sourceRect: RectF): RectF {
        val destRect = RectF(sourceRect)
        val matrix = Matrix()
        matrix.setScale(customScaleFactor, customScaleFactor)
        matrix.mapRect(destRect)
        return destRect
    }

    private fun applyCropGravity(destRect: RectF, viewWidth: Float, viewHeight: Float) {
        when (cropGravity) {
            CropGravity.TOP -> {
                val offset = destRect.top - viewBounds.top
                destRect.offset(0f, -offset)
            }
            CropGravity.BOTTOM -> {
                val offset = (viewBounds.bottom - destRect.bottom)
                destRect.offset(0f, offset)
            }
            CropGravity.START -> {
                val offset = destRect.left - viewBounds.left
                destRect.offset(-offset, 0f)
            }
            CropGravity.END -> {
                val offset = (viewBounds.right - destRect.right)
                destRect.offset(offset, 0f)
            }
            CropGravity.CENTER -> {
                // Already centered, no adjustment needed
            }
        }
    }

    private fun applyCustomTransformations(destRect: RectF) {
        if (rotationAngle != 0f || translationX != 0f || translationY != 0f || customScaleFactor != DEFAULT_MATRIX_SCALE) {
            val centerX = destRect.centerX()
            val centerY = destRect.centerY()

            // Apply custom scale
            if (customScaleFactor != DEFAULT_MATRIX_SCALE) {
                transformMatrix.postScale(customScaleFactor, customScaleFactor, centerX, centerY)
            }

            // Apply rotation
            if (rotationAngle != 0f) {
                transformMatrix.postRotate(rotationAngle, centerX, centerY)
            }

            // Apply translation
            if (translationX != 0f || translationY != 0f) {
                transformMatrix.postTranslate(translationX, translationY)
            }
        }
    }

    private fun updateClipPath() {
        clipPath.reset()
        clipPath.addRect(viewBounds, Path.Direction.CW)
    }

    private fun drawFallback(canvas: Canvas) {
        // Draw a simple placeholder in case of errors
        paint.color = Color.GRAY
        paint.style = Paint.Style.FILL
        canvas.drawRect(viewBounds, paint)
    }

    /**
     * Scale types supported by CenterCropDrawable
     */
    enum class ScaleType {
        CENTER_CROP,
        CENTER_INSIDE,
        FIT_CENTER,
        FIT_XY,
        MATRIX
    }

    /**
     * Crop gravity options
     */
    enum class CropGravity {
        CENTER,
        TOP,
        BOTTOM,
        START,
        END
    }

    /**
     * Animation callback interface
     */
    interface AnimationCallback {
        fun onFrame(canvas: Canvas)
    }

    /**
     * Builder class for easier configuration
     */
    class Builder(private val target: Drawable) {
        private var scaleType = ScaleType.CENTER_CROP
        private var cropGravity = CropGravity.CENTER
        private var clippingEnabled = true
        private var scaleFactor = DEFAULT_MATRIX_SCALE
        private var rotation = 0f
        private var translationX = 0f
        private var translationY = 0f
        private var animationCallback: AnimationCallback? = null

        fun scaleType(scaleType: ScaleType) = apply { this.scaleType = scaleType }
        fun cropGravity(gravity: CropGravity) = apply { this.cropGravity = gravity }
        fun clippingEnabled(enabled: Boolean) = apply { this.clippingEnabled = enabled }
        fun scaleFactor(factor: Float) = apply { this.scaleFactor = factor }
        fun rotation(angle: Float) = apply { this.rotation = angle }
        fun translation(x: Float, y: Float) = apply {
            this.translationX = x
            this.translationY = y
        }
        fun animationCallback(callback: AnimationCallback?) = apply {
            this.animationCallback = callback
        }

        fun build(): CenterCropDrawable {
            return CenterCropDrawable(target, scaleType, cropGravity).apply {
                setClippingEnabled(clippingEnabled)
                setScaleFactor(scaleFactor)
                setRotation(rotation)
                setTranslation(translationX, translationY)
                setAnimationCallback(animationCallback)
            }
        }
    }
}

/**
 * Extension functions for easier usage
 */
fun Drawable.centerCrop(
    scaleType: CenterCropDrawable.ScaleType = CenterCropDrawable.ScaleType.CENTER_CROP,
    cropGravity: CenterCropDrawable.CropGravity = CenterCropDrawable.CropGravity.CENTER
): CenterCropDrawable = CenterCropDrawable(this, scaleType, cropGravity)

fun Drawable.centerCropBuilder(): CenterCropDrawable.Builder = CenterCropDrawable.Builder(this)

/**
 * Utility functions for common configurations
 */
object CenterCropUtils {

    /**
     * Create center crop drawable for avatars
     */
    fun forAvatar(drawable: Drawable): CenterCropDrawable {
        return CenterCropDrawable.Builder(drawable)
            .scaleType(CenterCropDrawable.ScaleType.CENTER_CROP)
            .cropGravity(CenterCropDrawable.CropGravity.CENTER)
            .clippingEnabled(true)
            .build()
    }

    /**
     * Create center crop drawable for banners
     */
    fun forBanner(drawable: Drawable): CenterCropDrawable {
        return CenterCropDrawable.Builder(drawable)
            .scaleType(CenterCropDrawable.ScaleType.CENTER_CROP)
            .cropGravity(CenterCropDrawable.CropGravity.CENTER)
            .clippingEnabled(true)
            .build()
    }

    /**
     * Create fit center drawable for icons
     */
    fun forIcon(drawable: Drawable): CenterCropDrawable {
        return CenterCropDrawable.Builder(drawable)
            .scaleType(CenterCropDrawable.ScaleType.CENTER_INSIDE)
            .cropGravity(CenterCropDrawable.CropGravity.CENTER)
            .clippingEnabled(false)
            .build()
    }

    /**
     * Create animated center crop drawable
     */
    fun forAnimation(drawable: Drawable): CenterCropDrawable {
        return CenterCropDrawable.Builder(drawable)
            .scaleType(CenterCropDrawable.ScaleType.CENTER_CROP)
            .cropGravity(CenterCropDrawable.CropGravity.CENTER)
            .clippingEnabled(true)
            .build().apply {
                if (isAnimatable()) {
                    startAnimation()
                }
            }
    }
}

/**
 * Factory methods for common use cases
 */
fun createCenterCropDrawable(
    drawable: Drawable,
    block: CenterCropDrawable.Builder.() -> Unit = {}
): CenterCropDrawable = CenterCropDrawable.Builder(drawable).apply(block).build()
