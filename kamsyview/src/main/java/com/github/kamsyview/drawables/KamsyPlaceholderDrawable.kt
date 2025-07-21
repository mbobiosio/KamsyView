package com.github.kamsyview.drawables

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.core.graphics.withSave

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * KamsyPlaceholderDrawable - Handles placeholder text rendering with advanced typography
 * Supports multi-line text, proper alignment, and responsive sizing
 */
class KamsyPlaceholderDrawable(
    private val size: Int,
    @param:ColorInt private val backgroundColor: Int,
    private val text: CharSequence?,
    @param:ColorInt private val textColor: Int,
    @param:Dimension private val textSize: Float,
    private val typeface: Typeface?,
    @param:FloatRange(from = 0.0, to = 2.0) private val textSizePercentage: Float,
    private val avatarMargin: Int = 0,
    private val maxLines: Int = DEFAULT_MAX_LINES,
    private val alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
) : Drawable() {

    companion object {
        private const val DEFAULT_MAX_LINES = 2
        private const val TEXT_PADDING_RATIO = 0.1f
        private const val MIN_TEXT_SIZE = 12f
        private const val DEFAULT_LINE_SPACING = 1.2f
    }

    // Paint objects - initialized lazily to avoid potential issues
    private val backgroundPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
    }

    private val textPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = calculateActualTextSize()

            this@KamsyPlaceholderDrawable.typeface?.let {
                this.typeface = it
            }

            textAlign = Paint.Align.CENTER
            isSubpixelText = true
            isDither = true
        }
    }

    // Layout properties
    private var textLayout: StaticLayout? = null
    private val textBounds = RectF()
    private val backgroundBounds = RectF()
    private var circleRadius = 0f

    // Computed properties with safe defaults
    private val availableSize: Int
        get() = (size - (avatarMargin * 2)).coerceAtLeast(1)

    private val textPadding: Int
        get() = (availableSize * TEXT_PADDING_RATIO).toInt().coerceAtLeast(1)

    private val maxTextWidth: Int
        get() = (availableSize - (textPadding * 2)).coerceAtLeast(1)

    init {
        // Validate inputs
        require(size > 0) { "Size must be positive" }
        require(textSizePercentage > 0) { "Text size percentage must be positive" }
        require(maxLines > 0) { "Max lines must be positive" }

        calculateBounds()

        textPaint.textSize

        createTextLayout()
    }

    override fun draw(canvas: Canvas) {
        canvas.withSave {
            drawBackground(this)
            textLayout?.let { drawText(this, it) }
        }
    }

    override fun setAlpha(alpha: Int) {
        backgroundPaint.alpha = alpha
        textPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backgroundPaint.colorFilter = colorFilter
        textPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = size

    override fun getIntrinsicHeight(): Int = size

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        calculateBounds()
        createTextLayout()
    }

    // Public update methods
    fun updateText(newText: CharSequence?) = takeIf { text != newText }?.run {
        createTextLayout(newText)
        invalidateSelf()
    }

    fun updateTextColor(@ColorInt newColor: Int) = takeIf { textPaint.color != newColor }?.run {
        textPaint.color = newColor
        invalidateSelf()
    }

    fun updateBackgroundColor(@ColorInt newColor: Int) = takeIf { backgroundPaint.color != newColor }?.run {
        backgroundPaint.color = newColor
        invalidateSelf()
    }

    fun updateTextSize(newSize: Float) {
        val actualSize = calculateActualTextSize(newSize)
        takeIf { textPaint.textSize != actualSize }?.run {
            textPaint.textSize = actualSize
            createTextLayout()
            invalidateSelf()
        }
    }

    private fun calculateBounds() {
        val bounds = bounds
        if (bounds.isEmpty) return

        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()

        val drawableRadius = minOf(bounds.width(), bounds.height()) / 2f
        circleRadius = (drawableRadius - avatarMargin).coerceAtLeast(1f)

        backgroundBounds.apply {
            set(
                centerX - circleRadius,
                centerY - circleRadius,
                centerX + circleRadius,
                centerY + circleRadius
            )
        }

        textBounds.apply {
            set(
                centerX - (maxTextWidth / 2f),
                centerY - circleRadius + textPadding,
                centerX + (maxTextWidth / 2f),
                centerY + circleRadius - textPadding
            )
        }
    }

    private fun createTextLayout(textToUse: CharSequence? = text) {
        textLayout = textToUse?.takeIf { it.isNotBlank() }?.let { textContent ->
            val layoutWidth = maxTextWidth

            runCatching {
                StaticLayout.Builder.obtain(textContent, 0, textContent.length, textPaint, layoutWidth)
                    .setAlignment(alignment)
                    .setMaxLines(maxLines)
                    .setLineSpacing(0f, DEFAULT_LINE_SPACING)
                    .setIncludePad(false)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE)
                            setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                        }
                    }
                    .build()
            }.getOrNull()
        }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(bounds, backgroundPaint)
    }

    private fun drawText(canvas: Canvas, layout: StaticLayout) {
        val centerX = backgroundBounds.centerX()
        val centerY = backgroundBounds.centerY()

        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val textY = centerY - (textHeight / 2f) - fontMetrics.top

        canvas.drawText(
            text.toString(),
            centerX,
            textY,
            textPaint
        )
    }

    private fun calculateActualTextSize(baseSize: Float = textSize): Float =
        (baseSize * textSizePercentage).coerceAtLeast(MIN_TEXT_SIZE)

    /**
     * Builder class for creating KamsyPlaceholderDrawable with fluent API
     */
    class Builder {
        private var size: Int = 100
        private var backgroundColor: Int = Color.GRAY
        private var text: CharSequence? = "?"
        private var textColor: Int = Color.WHITE
        private var textSize: Float = 30f
        private var typeface: Typeface? = null
        private var textSizePercentage: Float = 1f
        private var avatarMargin: Int = 0
        private var maxLines: Int = DEFAULT_MAX_LINES
        private var alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER

        fun size(size: Int) = apply { this.size = size }
        fun backgroundColor(@ColorInt color: Int) = apply { this.backgroundColor = color }
        fun text(text: CharSequence?) = apply { this.text = text }
        fun textColor(@ColorInt color: Int) = apply { this.textColor = color }
        fun textSize(@Dimension size: Float) = apply { this.textSize = size }
        fun typeface(typeface: Typeface?) = apply { this.typeface = typeface }
        fun textSizePercentage(@FloatRange(from = 0.0, to = 2.0) percentage: Float) = apply {
            this.textSizePercentage = percentage
        }
        fun avatarMargin(margin: Int) = apply { this.avatarMargin = margin }
        fun maxLines(lines: Int) = apply { this.maxLines = lines }
        fun alignment(alignment: Layout.Alignment) = apply { this.alignment = alignment }

        fun build(): KamsyPlaceholderDrawable = KamsyPlaceholderDrawable(
            size = size,
            backgroundColor = backgroundColor,
            text = text,
            textColor = textColor,
            textSize = textSize,
            typeface = typeface,
            textSizePercentage = textSizePercentage,
            avatarMargin = avatarMargin,
            maxLines = maxLines,
            alignment = alignment
        )
    }
}

// Extension functions for easy placeholder creation
fun createPlaceholderDrawable(
    size: Int,
    backgroundColor: Int,
    text: CharSequence? = "?",
    textColor: Int = Color.WHITE,
    textSize: Float = size / 3f,
    typeface: Typeface? = null,
    textSizePercentage: Float = 1f,
    avatarMargin: Int = 0
): KamsyPlaceholderDrawable = KamsyPlaceholderDrawable.Builder()
    .size(size)
    .backgroundColor(backgroundColor)
    .text(text)
    .textColor(textColor)
    .textSize(textSize)
    .typeface(typeface)
    .textSizePercentage(textSizePercentage)
    .avatarMargin(avatarMargin)
    .build()

fun createInitialsPlaceholder(
    name: String,
    size: Int,
    backgroundColor: Int,
    textColor: Int = Color.WHITE,
    textSize: Float = size / 3f,
    typeface: Typeface? = null,
    textSizePercentage: Float = 1f,
    avatarMargin: Int = 0
): KamsyPlaceholderDrawable {
    val initials = name.extractInitials()
    return createPlaceholderDrawable(
        size = size,
        backgroundColor = backgroundColor,
        text = initials,
        textColor = textColor,
        textSize = textSize,
        typeface = typeface,
        textSizePercentage = textSizePercentage,
        avatarMargin = avatarMargin
    )
}

private fun String.extractInitials(maxInitials: Int = 2): String =
    split(" ")
        .filter { it.isNotBlank() }
        .take(maxInitials)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .takeIf { it.isNotBlank() } ?: "?"

fun generateBackgroundColor(text: String): Int {
    val hash = text.hashCode()
    val hue = (hash % 360).toFloat()
    val saturation = 0.5f + (hash % 50) / 100f
    val lightness = 0.4f + (hash % 30) / 100f

    return Color.HSVToColor(floatArrayOf(hue, saturation, lightness))
}

fun createAutoStyledPlaceholder(
    name: String,
    size: Int,
    textColor: Int = Color.WHITE,
    textSize: Float = size / 3f,
    typeface: Typeface? = null,
    textSizePercentage: Float = 1f,
    avatarMargin: Int = 0
): KamsyPlaceholderDrawable {
    val backgroundColor = generateBackgroundColor(name)
    return createInitialsPlaceholder(
        name = name,
        size = size,
        backgroundColor = backgroundColor,
        textColor = textColor,
        textSize = textSize,
        typeface = typeface,
        textSizePercentage = textSizePercentage,
        avatarMargin = avatarMargin
    )
}
