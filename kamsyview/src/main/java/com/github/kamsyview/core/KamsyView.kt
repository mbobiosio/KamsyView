package com.github.kamsyview.core

import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.fragment.app.Fragment
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.github.kamsyview.R
import com.github.kamsyview.di.BlurHashScope
import com.github.kamsyview.drawables.KamsyBorderDrawable
import com.github.kamsyview.drawables.KamsyOverlayDrawable
import com.github.kamsyview.drawables.KamsyPlaceholderDrawable
import com.github.kamsyview.drawables.KamsyVolumetricDrawable
import com.github.kamsyview.extensions.colorAttribute
import com.github.kamsyview.extensions.getColorOrNull
import com.github.kamsyview.extensions.getTypefaceOrNull
import com.github.kamsyview.impl.DefaultKamsyLogger
import com.github.kamsyview.impl.DefaultKamsyMetrics
import com.github.kamsyview.impl.InjectableBlurHashProcessor
import com.github.kamsyview.impl.InjectableKamsyDrawableFactory
import com.github.kamsyview.interfaces.IBlurHashProcessor
import com.github.kamsyview.interfaces.IKamsyDrawableFactory
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.interfaces.IKamsyMetrics
import com.github.kamsyview.models.KamsyConfiguration
import com.github.kamsyview.processing.BlurHashResult
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * A highly customizable avatar and image view component that extends [ShapeableImageView].
 *
 * KamsyView provides advanced features for displaying user avatars, images, and placeholders
 * with extensive customization options including:
 * - Multiple predefined shapes (circle, rounded square, hexagon, squircle, diamond, star)
 * - Various styling themes (Default, Minimal, Material3, Gaming, Professional)
 * - BlurHash image placeholders for progressive loading
 * - Status indicators with customizable positions
 * - Volumetric effects and border customizations
 * - Arches decorations for enhanced visual appeal
 *
 * ## Basic Usage
 * ```kotlin
 * val kamsyView = KamsyView(context).apply {
 *     setShape(KamsyShape.CIRCLE)
 *     setStyle(KamsyStyle.Material3)
 *     borderWidth = 8
 *     borderColor = Color.BLUE
 * }
 * ```
 *
 * ## XML Usage
 * ```xml
 * <com.github.kamsyview.core.KamsyView
 *     android:layout_width="80dp"
 *     android:layout_height="80dp"
 *     app:kamsyShape="circle"
 *     app:kamsyStyle="material3"
 *     app:borderWidth="4dp"
 *     app:borderColor="@color/primary"
 *     app:placeholderText="JD"
 *     app:statusIndicator="online"
 *     app:statusPosition="bottom_right" />
 * ```
 *
 * @param context The context for the view
 * @param attrs The attribute set from XML inflation
 * @param defStyleAttr The default style attribute
 *
 * @see ShapeableImageView
 * @see KamsyShape
 * @see KamsyStyle
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
open class KamsyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ShapeableImageView(context, attrs, defStyleAttr) {

    /**
     * The current shape applied to this view.
     *
     * @see KamsyShape
     * @see setShape
     */
    var currentShape: KamsyShape = KamsyShape.CIRCLE
        private set

    /**
     * The current visual style theme applied to this view.
     *
     * @see KamsyStyle
     * @see setStyle
     */
    var currentStyle: KamsyStyle = KamsyStyle.Default
        private set

    companion object {
        /** Default text size as a percentage of the view's width for placeholder text */
        private const val DEFAULT_TEXT_SIZE_PERCENTAGE = 1f

        /** Default scale factor for icon drawables relative to view size */
        private const val DEFAULT_ICON_SCALE = 0.5f

        /** Default size in pixels for BlurHash placeholder generation */
        private const val DEFAULT_BLUR_HASH_SIZE = 20

        /** Pending status indicator type from XML attributes */
        private var pendingStatusIndicator: Int = -1

        /** Pending status indicator color from XML attributes */
        private var pendingStatusColor: Int = Color.GREEN

        /** Pending status indicator position from XML attributes */
        private var pendingStatusPosition: Int = 3 // bottom_right

        /** Pending status indicator size in pixels from XML attributes */
        private var pendingStatusSize: Float = 24f
    }

    /**
     * Internal mutable state flow for managing view's UI state.
     *
     * @see uiState
     * @see KamsyUiState
     */
    private val _uiState = MutableStateFlow<KamsyUiState>(KamsyUiState.Loading)

    /**
     * Public read-only state flow representing the current UI state.
     *
     * Example usage:
     * ```kotlin
     * viewLifecycleOwner.lifecycleScope.launch {
     *     kamsyView.uiState.collect { state ->
     *         when (state) {
     *             is KamsyUiState.Loading -> showLoadingIndicator()
     *             is KamsyUiState.Success -> hideLoadingIndicator()
     *             is KamsyUiState.Error -> showErrorMessage(state.error)
     *         }
     *     }
     * }
     * ```
     */
    val uiState: StateFlow<KamsyUiState> = _uiState.asStateFlow()

    /**
     * Primary border color for the view's border decoration.
     *
     * @see borderColorSecondary for gradient borders
     * @see borderWidth
     * @see borderGradientAngle
     */
    @ColorInt
    var borderColor: Int = Color.BLACK
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Secondary border color for gradient border effects.
     *
     * When set (non-null), creates a gradient from [borderColor] to [borderColorSecondary].
     *
     * @see borderColor
     * @see borderGradientAngle
     */
    @ColorInt
    var borderColorSecondary: Int? = null
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Color of the placeholder text displayed when no image is available.
     *
     * @see placeholderText
     * @see backgroundPlaceholderColor
     * @see textSizePercentage
     */
    @ColorInt
    var textColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Background color for placeholder states.
     *
     * @see textColor
     * @see placeholderText
     */
    @ColorInt
    var backgroundPlaceholderColor: Int = Color.BLACK
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Text content displayed in placeholder state.
     *
     * Example usage:
     * ```kotlin
     * placeholderText = "AB"  // User initials
     * placeholderText = "?"   // Generic placeholder
     * placeholderText = "👤"  // Emoji placeholder
     * ```
     *
     * @see textColor
     * @see textSizePercentage
     * @see textTypeface
     */
    var placeholderText: CharSequence? = "?"
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Scale factor for icon drawables relative to the view's size.
     *
     * Valid range: 0.0 (invisible) to 1.0 (full size)
     *
     * @see DEFAULT_ICON_SCALE
     */
    @FloatRange(from = 0.0, to = 1.0)
    var iconDrawableScale: Float = DEFAULT_ICON_SCALE
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Width of the border in pixels.
     *
     * @see borderColor
     * @see borderColorSecondary
     * @see borderGradientAngle
     */
    @Dimension(unit = Dimension.PX)
    var borderWidth: Int = 0
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Text size as a percentage of the view's width for placeholder text.
     *
     * Valid range: 0.0 (invisible text) to 2.0 (very large text)
     *
     * @see placeholderText
     * @see textColor
     * @see DEFAULT_TEXT_SIZE_PERCENTAGE
     */
    @FloatRange(from = 0.0, to = 2.0)
    var textSizePercentage: Float = DEFAULT_TEXT_SIZE_PERCENTAGE
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Margin around the avatar content in pixels.
     *
     * @see currentShape
     */
    @Dimension(unit = Dimension.PX)
    var avatarMargin: Int = 0
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Type of volumetric (3D-like) effects to apply.
     *
     * @see VolumetricType
     * @see volumetricDrawable
     */
    var volumetricType: VolumetricType = VolumetricType.NONE
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Angle in degrees for gradient border direction.
     *
     * Valid range: 0-360 degrees
     *
     * @see borderColor
     * @see borderColorSecondary
     */
    @IntRange(from = 0, to = 360)
    var borderGradientAngle: Int = 0
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Total degree coverage for arch decorations around the view.
     *
     * Valid range: 0-360 degrees
     *
     * @see archesCount
     * @see archesAngle
     * @see archesType
     */
    @IntRange(from = 0, to = 360)
    var archesDegreeArea: Int = 0
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Starting angle for arch decorations in degrees.
     *
     * Valid range: 0-360 degrees
     *
     * @see archesDegreeArea
     * @see archesCount
     */
    @IntRange(from = 0, to = 360)
    var archesAngle: Int = 0
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Number of arch decorations to display around the view.
     *
     * @see archesDegreeArea
     * @see archesAngle
     * @see archesType
     */
    var archesCount: Int = 0
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Visual style for arch decorations.
     *
     * @see ArchesType
     * @see archesCount
     */
    var archesType: ArchesType = ArchesType.SINGLE
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * Custom typeface for placeholder text rendering.
     *
     * @see placeholderText
     * @see textColor
     * @see textSizePercentage
     */
    var textTypeface: Typeface? = null
        set(value) {
            field = value
            invalidateDrawables()
        }

    /**
     * BlurHash string for progressive image loading placeholder.
     *
     * @see blurHashPunch
     * @see blurHashProcessor
     * @see processBlurHash
     */
    var blurHash: String? = null
        set(value) {
            field = value
            value?.let { processBlurHash(it) }
        }

    /**
     * Punch factor for BlurHash processing.
     *
     * Valid range: 0.0 (low contrast) to 2.0 (high contrast)
     *
     * @see blurHash
     * @see blurHashProcessor
     */
    @FloatRange(from = 0.0, to = 2.0)
    var blurHashPunch: Float = 1f

    /**
     * BlurHash processor for handling asynchronous BlurHash decoding.
     *
     * @see IBlurHashProcessor
     * @see blurHash
     * @see processBlurHash
     */
    @Inject
    internal lateinit var blurHashProcessor: IBlurHashProcessor

    /**
     * Factory for creating various drawable components used by the view.
     *
     * @see IKamsyDrawableFactory
     * @see overlayDrawable
     * @see borderDrawable
     * @see volumetricDrawable
     */
    @Inject
    internal lateinit var drawableFactory: IKamsyDrawableFactory

    /**
     * Global configuration settings for KamsyView behavior.
     *
     * @see KamsyConfiguration
     */
    @Inject
    internal lateinit var configuration: KamsyConfiguration

    /**
     * Logger instance for debugging and error reporting.
     *
     * @see IKamsyLogger
     */
    @Inject
    internal lateinit var logger: IKamsyLogger

    /**
     * Metrics collector for performance monitoring and analytics.
     *
     * @see IKamsyMetrics
     */
    @Inject
    internal lateinit var metrics: IKamsyMetrics

    /**
     * Coroutine scope specifically for BlurHash processing operations.
     *
     * @see BlurHashScope
     * @see blurHashProcessor
     * @see processBlurHash
     */
    @Inject
    @BlurHashScope
    internal lateinit var blurHashScope: CoroutineScope

    /**
     * Overlay drawable for status indicators and decorative elements.
     *
     * @see KamsyOverlayDrawable
     * @see drawableFactory
     * @see applyStatusFromAttributes
     */
    var overlayDrawable: KamsyOverlayDrawable? = null
        protected set

    /**
     * Border drawable for rendering view borders and outlines.
     *
     * @see KamsyBorderDrawable
     * @see borderWidth
     * @see borderColor
     * @see borderColorSecondary
     * @see borderGradientAngle
     */
    var borderDrawable: KamsyBorderDrawable? = null
        protected set

    /**
     * Volumetric drawable for 3D effects and depth rendering.
     *
     * @see KamsyVolumetricDrawable
     * @see volumetricType
     * @see VolumetricType
     */
    var volumetricDrawable: KamsyVolumetricDrawable? = null
        protected set

    /**
     * Flag indicating whether dependency injection has been completed.
     *
     * @see initializeDependencies
     * @see injectDependencies
     * @see ensureDependenciesAndInitialize
     */
    var isDependenciesInjected = false

    init {
        initializeDependencies()
        attrs?.let { initializeAttributes(it) }
        post {
            ensureDependenciesAndInitialize()
        }
    }

    /**
     * Initializes dependency injection for the view.
     *
     * @see createFallbackDependencies
     * @see ensureDependenciesAndInitialize
     */
    protected open fun initializeDependencies() {
        runCatching {
            when (context) {
                is Activity, is Fragment -> {
                    // Hilt should inject dependencies
                    if (!this::logger.isInitialized) {
                        createFallbackDependencies()
                    }
                }
                else -> createFallbackDependencies()
            }
        }.getOrElse {
            createFallbackDependencies()
        }
    }

    /**
     * Creates fallback dependencies when Hilt dependency injection is not available.
     *
     * @see initializeDependencies
     * @see injectDependencies
     */
    protected open fun createFallbackDependencies() {
        logger = DefaultKamsyLogger(KamsyConfiguration())
        metrics = DefaultKamsyMetrics(logger, KamsyConfiguration())

        // Create a view-specific scope for BlurHash processing
        val viewScope = CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Default +
                    CoroutineExceptionHandler { _, throwable ->
                        logger.error("BlurHash processing error", throwable)
                    }
        )

        blurHashProcessor = InjectableBlurHashProcessor(
            context = context,
            cacheSize = 50,
            maxConcurrentJobs = 3,
            processingScope = viewScope,
            logger = logger,
            metrics = metrics
        )
        drawableFactory = InjectableKamsyDrawableFactory(logger)
        configuration = KamsyConfiguration()
        isDependenciesInjected = true
    }

    /**
     * Ensures dependencies are available and completes view initialization.
     *
     * @see initializeDependencies
     * @see initializeDrawables
     * @see observeState
     */
    protected open fun ensureDependenciesAndInitialize() {
        if (!isDependenciesInjected) {
            initializeDependencies()
        }
        initializeDrawables()
        observeState()
    }

    /**
     * Processes XML attributes and applies them to view properties.
     *
     * @param attrs The AttributeSet containing XML attribute values
     * @see configureFromAttributes
     */
    private fun initializeAttributes(attrs: AttributeSet) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.KamsyView,
            0,
            0
        ).also { typedArray ->
            configureFromAttributes(typedArray)
        }.recycle()
    }

    /**
     * Configures view properties from TypedArray values.
     *
     * @param typedArray The TypedArray containing processed attribute values
     * @see initializeAttributes
     * @see applyStatusFromAttributes
     */
    private fun configureFromAttributes(typedArray: TypedArray) {
        val defaultAccent = context.colorAttribute(android.R.attr.colorAccent)

        val shapeValue = typedArray.getInt(R.styleable.KamsyView_kamsyShape, 0)
        logger.debug("Shape value: $shapeValue")
        currentShape = when (shapeValue) {
            0 -> KamsyShape.CIRCLE
            1 -> KamsyShape.ROUNDED_SQUARE
            2 -> KamsyShape.HEXAGON
            3 -> KamsyShape.SQUIRCLE
            4 -> KamsyShape.DIAMOND
            5 -> KamsyShape.STAR
            6 -> KamsyShape.CUSTOM
            else -> KamsyShape.CIRCLE
        }

        val styleValue = typedArray.getInt(R.styleable.KamsyView_kamsyStyle, 0)
        currentStyle = when (styleValue) {
            0 -> KamsyStyle.Default
            1 -> KamsyStyle.Minimal
            2 -> KamsyStyle.Material3
            3 -> KamsyStyle.Gaming
            4 -> KamsyStyle.Professional
            else -> KamsyStyle.Default
        }

        val cornerRadius = typedArray.getDimension(R.styleable.KamsyView_cornerRadius, 0f)
        logger.debug("Corner radius $cornerRadius")

        post {
            if (cornerRadius > 0f && (
                        currentShape == KamsyShape.ROUNDED_SQUARE ||
                                currentShape == KamsyShape.SQUIRCLE)
            ) {
                currentShape.apply(this@KamsyView)

                val currentModel = shapeAppearanceModel
                val newModel = currentModel.toBuilder()
                    .setAllCornerSizes(cornerRadius)
                    .build()
                shapeAppearanceModel = newModel
            } else {
                // For diamond, hexagon, star - ignore custom corner radius and use shape's built-in corners
                currentShape.apply(this@KamsyView)
            }
        }

        backgroundPlaceholderColor = typedArray.getColor(
            R.styleable.KamsyView_backgroundColor,
            defaultAccent
        )

        borderColor = typedArray.getColor(
            R.styleable.KamsyView_borderColor,
            defaultAccent
        )

        borderColorSecondary = typedArray.getColorOrNull(
            R.styleable.KamsyView_borderColorSecondary
        )

        borderWidth = typedArray.getDimensionPixelSize(
            R.styleable.KamsyView_borderWidth,
            borderWidth
        )
        logger.debug("Border width: $borderWidth")

        borderGradientAngle = typedArray.getInt(
            R.styleable.KamsyView_borderGradientAngle,
            borderGradientAngle
        ).coerceIn(0, 360)

        textSizePercentage = typedArray.getFloat(
            R.styleable.KamsyView_textSizePercentage,
            DEFAULT_TEXT_SIZE_PERCENTAGE
        )

        volumetricType = VolumetricType.fromValue(
            typedArray.getInt(R.styleable.KamsyView_kamsyVolumetricType, -1)
        )

        placeholderText = typedArray.getText(R.styleable.KamsyView_placeholderText)

        iconDrawableScale = typedArray.getFloat(
            R.styleable.KamsyView_iconDrawableScale,
            iconDrawableScale
        )

        avatarMargin = typedArray.getDimensionPixelSize(
            R.styleable.KamsyView_avatarMargin,
            avatarMargin
        )

        archesCount = typedArray.getInt(
            R.styleable.KamsyView_archesCount,
            archesCount
        ).coerceAtLeast(0)
        logger.debug("Arches count: $archesCount")

        archesDegreeArea = typedArray.getInt(
            R.styleable.KamsyView_archesDegreeArea,
            archesDegreeArea
        ).coerceIn(0, 360)
        logger.debug("Arches degree area: $archesDegreeArea")

        archesAngle = typedArray.getInt(
            R.styleable.KamsyView_archesAngle,
            archesAngle
        ).coerceIn(0, 360)

        archesType = ArchesType.fromValue(
            typedArray.getInt(R.styleable.KamsyView_archesType, 0)
        )

        textTypeface = typedArray.getTypefaceOrNull(
            context,
            R.styleable.KamsyView_android_fontFamily
        )

        blurHash = typedArray.getString(R.styleable.KamsyView_blurHash)

        blurHashPunch = typedArray.getFloat(
            R.styleable.KamsyView_blurHashPunch,
            blurHashPunch
        )

        pendingStatusIndicator = typedArray.getInt(R.styleable.KamsyView_statusIndicator, -1)
        pendingStatusColor = typedArray.getColor(R.styleable.KamsyView_statusColor, Color.GREEN)
        pendingStatusPosition =
            typedArray.getInt(R.styleable.KamsyView_statusPosition, 3) // bottom_right
        pendingStatusSize = typedArray.getDimension(R.styleable.KamsyView_statusSize, 24f)

        // Handle drawable
        typedArray.getDrawable(R.styleable.KamsyView_android_src)?.let {
            setImageDrawable(it)
        }
    }

    /**
     * Applies status indicator configuration from XML attributes.
     *
     * @see overlayDrawable
     * @see getStatusPosition
     * @see configureFromAttributes
     */
    private fun applyStatusFromAttributes() {
        overlayDrawable?.let { overlay ->
            logger.debug("Applying status: indicator=$pendingStatusIndicator, position=$pendingStatusPosition")

            when (pendingStatusIndicator) {
                0 -> overlay.addStatusIndicator(
                    Color.GREEN,
                    pendingStatusSize,
                    getStatusPosition(pendingStatusPosition)
                )

                1 -> overlay.addStatusIndicator(
                    Color.GRAY,
                    pendingStatusSize,
                    getStatusPosition(pendingStatusPosition)
                )

                2 -> overlay.addStatusIndicator(
                    Color.YELLOW,
                    pendingStatusSize,
                    getStatusPosition(pendingStatusPosition)
                )

                3 -> overlay.addStatusIndicator(
                    Color.RED,
                    pendingStatusSize,
                    getStatusPosition(pendingStatusPosition)
                )

                4 -> overlay.addStatusIndicator(
                    pendingStatusColor,
                    pendingStatusSize,
                    getStatusPosition(pendingStatusPosition)
                )
            }

            logger.debug("Status applied with position: ${getStatusPosition(pendingStatusPosition)}")
        }
    }

    /**
     * Initializes all drawable components used by the view.
     *
     * @see drawableFactory
     * @see overlayDrawable
     * @see borderDrawable
     * @see volumetricDrawable
     * @see applyStatusFromAttributes
     */
    protected open fun initializeDrawables() {
        if (!isDependenciesInjected) return

        runCatching {
            overlayDrawable = drawableFactory.createOverlayDrawable(this)
            borderDrawable = drawableFactory.createBorderDrawable(this)
            volumetricDrawable = drawableFactory.createVolumetricDrawable(this)

            logger.debug("KamsyView drawables initialized successfully")

            // NOW apply status indicator
            applyStatusFromAttributes()

            metrics.incrementViewCreation()
        }.getOrElse { e ->
            logger.error("Failed to initialize KamsyView drawables", e)
            // Create fallback drawables
            overlayDrawable = KamsyOverlayDrawable(this)
            borderDrawable = KamsyBorderDrawable(this)
            volumetricDrawable = KamsyVolumetricDrawable(this)

            // Apply status for fallback too
            applyStatusFromAttributes()
        }
    }

    /**
     * Converts numeric status position value to StatusPosition enum.
     *
     * @param value Numeric position value from XML attributes
     * @return Corresponding StatusPosition enum value
     * @see KamsyOverlayDrawable.StatusPosition
     * @see applyStatusFromAttributes
     */
    private fun getStatusPosition(value: Int): KamsyOverlayDrawable.StatusPosition {
        return when (value) {
            0 -> KamsyOverlayDrawable.StatusPosition.TOP_LEFT
            1 -> KamsyOverlayDrawable.StatusPosition.TOP_RIGHT
            2 -> KamsyOverlayDrawable.StatusPosition.BOTTOM_LEFT
            3 -> KamsyOverlayDrawable.StatusPosition.BOTTOM_RIGHT
            4 -> KamsyOverlayDrawable.StatusPosition.CENTER
            else -> KamsyOverlayDrawable.StatusPosition.BOTTOM_RIGHT
        }
    }

    /**
     * Observes UI state changes and updates view accordingly.
     *
     * @see uiState
     * @see KamsyUiState
     */
    protected open fun observeState() {
        findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
            uiState.collect { state ->
                when (state) {
                    is KamsyUiState.Loading -> handleLoadingState()
                    is KamsyUiState.Success -> handleSuccessState(state)
                    is KamsyUiState.Error -> handleErrorState(state)
                    is KamsyUiState.BlurHashLoaded -> handleBlurHashLoaded(state)
                    is KamsyUiState.Placeholder -> handlePlaceholderState(state)
                }
            }
        }
    }

    /**
     * Processes a BlurHash string asynchronously and updates view state.
     *
     * @param hash The BlurHash string to process
     * @see blurHash
     * @see blurHashProcessor
     * @see blurHashPunch
     * @see BlurHashResult
     */
    private fun processBlurHash(hash: String) {
        takeIf { isDependenciesInjected }?.let {
            _uiState.value = KamsyUiState.Loading

            findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                runCatching {
                    blurHashProcessor.processBlurHash(
                        hash,
                        DEFAULT_BLUR_HASH_SIZE,
                        DEFAULT_BLUR_HASH_SIZE,
                        blurHashPunch
                    ) { result ->
                        post {
                            _uiState.value = when (result) {
                                is BlurHashResult.Success -> KamsyUiState.BlurHashLoaded(
                                    result.drawable,
                                    hash
                                )

                                is BlurHashResult.Error -> KamsyUiState.Error(result.error)
                            }
                        }
                    }
                }.getOrElse { e ->
                    logger.error("BlurHash processing failed", e)
                    post {
                        _uiState.value = KamsyUiState.Error(e.toKamsyError())
                    }
                }
            }
        } ?: logger.warning("Dependencies not injected, skipping BlurHash processing")
    }

    /**
     * Handles the loading UI state by showing a placeholder.
     *
     * @see showPlaceholder
     * @see KamsyUiState.Loading
     */
    private fun handleLoadingState() {
        showPlaceholder()
    }

    /**
     * Handles successful content loading by displaying the loaded drawable.
     *
     * @param state Success state containing the loaded drawable
     * @see KamsyUiState.Success
     * @see updateOverlays
     */
    private fun handleSuccessState(state: KamsyUiState.Success) {
        setImageDrawable(state.drawable)
        updateOverlays()
    }

    /**
     * Handles error states by showing a placeholder.
     *
     * @param state Error state containing error information
     * @see showPlaceholder
     * @see KamsyUiState.Error
     */
    private fun handleErrorState(state: KamsyUiState.Error) {
        showPlaceholder()
    }

    /**
     * Handles BlurHash loaded state by displaying the generated placeholder.
     *
     * @param state BlurHash loaded state containing the generated drawable
     * @see KamsyUiState.BlurHashLoaded
     * @see processBlurHash
     * @see updateOverlays
     */
    private fun handleBlurHashLoaded(state: KamsyUiState.BlurHashLoaded) {
        setImageDrawable(state.drawable)
        updateOverlays()
    }

    /**
     * Handles placeholder state by displaying the provided placeholder drawable.
     *
     * @param state Placeholder state containing the placeholder drawable
     * @see KamsyUiState.Placeholder
     * @see createPlaceholderDrawable
     * @see updateOverlays
     */
    private fun handlePlaceholderState(state: KamsyUiState.Placeholder) {
        setImageDrawable(state.drawable)
        updateOverlays()
    }

    /**
     * Shows a placeholder by creating and setting an appropriate placeholder drawable.
     *
     * @see createPlaceholderDrawable
     * @see updateOverlays
     */
    private fun showPlaceholder() {
        val placeholderDrawable = createPlaceholderDrawable()
        setImageDrawable(placeholderDrawable)
        updateOverlays()
    }

    /**
     * Sets the image drawable with proper bounds and padding configuration.
     *
     * @param drawable The drawable to set as the view's image, or null to clear
     * @see currentShape
     * @see avatarMargin
     */
    override fun setImageDrawable(drawable: Drawable?) {
        drawable?.let { d ->
            if (d.bounds.isEmpty) {
                val size = measuredWidth.takeIf { it > 0 } ?: width.takeIf { it > 0 } ?: 100
                d.setBounds(0, 0, size, size)
            }
        }

        // Only apply padding for circular shapes
        if (currentShape == KamsyShape.CIRCLE) {
            setPadding(avatarMargin, avatarMargin, avatarMargin, avatarMargin)
        } else {
            setPadding(0, 0, 0, 0) // Reset padding for non-circular shapes
        }

        scaleType = when (currentShape) {
            KamsyShape.CIRCLE, KamsyShape.SQUIRCLE -> ScaleType.CENTER_CROP
            KamsyShape.ROUNDED_SQUARE -> ScaleType.CENTER_CROP
            else -> ScaleType.CENTER_CROP
        }

        super.setImageDrawable(drawable)
    }

    /**
     * Creates a placeholder drawable based on current view configuration.
     *
     * @return A drawable suitable for placeholder display
     * @see drawableFactory
     * @see KamsyPlaceholderDrawable
     * @see calculateTextSize
     */
    private fun createPlaceholderDrawable(): Drawable {
        val size = measuredWidth.takeIf { it > 0 } ?: 100

        logger.debug("Creating placeholder with typeface: ${textTypeface?.toString() ?: "null"}")

        return takeIf { isDependenciesInjected }?.let {
            drawableFactory.createPlaceholderDrawable(
                size = size,
                backgroundColor = backgroundPlaceholderColor,
                text = placeholderText,
                textColor = textColor,
                textSize = calculateTextSize(),
                typeface = textTypeface,
                textSizePercentage = textSizePercentage,
                avatarMargin = avatarMargin
            )
        } ?: KamsyPlaceholderDrawable(
            size = size,
            backgroundColor = backgroundPlaceholderColor,
            text = placeholderText,
            textColor = textColor,
            textSize = calculateTextSize(),
            typeface = textTypeface,
            textSizePercentage = textSizePercentage,
            avatarMargin = avatarMargin
        ).apply {
            setBounds(0, 0, size, size)
        }
    }

    /**
     * Calculates appropriate text size for placeholder content.
     *
     * @return Text size in pixels
     * @see textSizePercentage
     * @see placeholderText
     */
    private fun calculateTextSize(): Float =
        measuredWidth.takeIf { it > 0 }?.let { it / 3f } ?: 30f

    /**
     * Updates all overlay drawables and applies them as view foreground.
     *
     * @see borderDrawable
     * @see volumetricDrawable
     * @see overlayDrawable
     */
    private fun updateOverlays() {
        val drawables = mutableListOf<Drawable>()

        // Add border drawable
        borderDrawable?.let { border ->
            border.update()
            border.setBounds(0, 0, width, height)
            drawables.add(border)
        }

        volumetricDrawable?.update()

        // Add overlay drawable
        overlayDrawable?.apply {
            update()
            setBounds(0, 0, width, height)
            drawables.add(this)
        }

        // Combine all drawables into layers
        if (drawables.isNotEmpty()) {
            val layerDrawable = LayerDrawable(drawables.toTypedArray())
            layerDrawable.setBounds(0, 0, width, height)
            foreground = layerDrawable
        }
    }

    /**
     * Invalidates all drawable components and triggers overlay updates.
     *
     * @see updateOverlays
     */
    private fun invalidateDrawables() {
        listOfNotNull(overlayDrawable, borderDrawable, volumetricDrawable)
            .forEach { it.invalidateSelf() }
        updateOverlays()
    }

    /**
     * Handles view size changes by updating shape application and overlays.
     *
     * @param w Current width of the view
     * @param h Current height of the view
     * @param oldw Previous width of the view
     * @param oldh Previous height of the view
     * @see currentShape
     * @see updateOverlays
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Re-apply shape with new dimensions (important for width-based calculations)
        if (w > 0 && h > 0) {
            currentShape.apply(this)
        }

        updateOverlays()
    }

    /**
     * Sets the view's shape and applies it immediately.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.setShape(KamsyShape.HEXAGON)
     * kamsyView.setShape(KamsyShape.SQUIRCLE)
     * ```
     *
     * @param shape The new shape to apply to the view
     * @see KamsyShape
     * @see currentShape
     * @see setStyle
     */
    fun setShape(shape: KamsyShape) {
        currentShape = shape
        shape.apply(this)
    }

    /**
     * Sets the view's style theme and applies it immediately.
     *
     * Example usage:
     * ```kotlin
     * kamsyView.setStyle(KamsyStyle.Material3)
     * kamsyView.setStyle(KamsyStyle.Gaming)
     * ```
     *
     * @param style The new style theme to apply to the view
     * @see KamsyStyle
     * @see currentStyle
     * @see setShape
     */
    fun setStyle(style: KamsyStyle) {
        currentStyle = style
        style.apply(this)
    }

    /**
     * Cleans up resources when the view is detached from the window.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        takeIf { isDependenciesInjected }?.let {
            runCatching {
                blurHashProcessor.cleanup()
                logger.debug("KamsyView cleanup completed")
            }.onFailure { e ->
                logger.error("Error during KamsyView cleanup", e)
            }
        }
    }

    /**
     * Manually injects dependencies when Hilt is not available or desired.
     *
     * Example usage:
     * ```kotlin
     * val kamsyView = KamsyView(context)
     * kamsyView.injectDependencies(
     *     blurHashProcessor = MyBlurHashProcessor(),
     *     drawableFactory = MyDrawableFactory(),
     *     configuration = MyConfiguration(),
     *     logger = MyLogger(),
     *     metrics = MyMetrics(),
     *     blurHashScope = myCoroutineScope
     * )
     * ```
     *
     * @param blurHashProcessor Processor for BlurHash operations
     * @param drawableFactory Factory for creating drawable components
     * @param configuration Global configuration settings
     * @param logger Logger for debugging and error reporting
     * @param metrics Metrics collector for performance monitoring
     * @param blurHashScope Optional coroutine scope for BlurHash processing
     * @return This view instance for method chaining
     * @see initializeDependencies
     * @see createFallbackDependencies
     */
    fun injectDependencies(
        blurHashProcessor: IBlurHashProcessor,
        drawableFactory: IKamsyDrawableFactory,
        configuration: KamsyConfiguration,
        logger: IKamsyLogger,
        metrics: IKamsyMetrics,
        blurHashScope: CoroutineScope? = null
    ) = apply {
        this.blurHashProcessor = blurHashProcessor
        this.drawableFactory = drawableFactory
        this.configuration = configuration
        this.logger = logger
        this.metrics = metrics
        this.blurHashScope = blurHashScope ?: CoroutineScope(
            SupervisorJob() + Dispatchers.Default
        )
        this.isDependenciesInjected = true

        initializeDrawables()
        observeState()
    }

    /**
     * Defines the types of volumetric (3D-like) effects that can be applied to the view.
     *
     * @property value The integer value used for XML attribute mapping
     * @see volumetricType
     * @see volumetricDrawable
     */
    enum class VolumetricType(val value: Int) {
        /** No volumetric effects applied */
        NONE(-1),

        /** Apply volumetric effects to all content (drawables and placeholders) */
        ALL(0),

        /** Apply volumetric effects only to drawable content (images, icons) */
        DRAWABLE(1),

        /** Apply volumetric effects only to placeholder content (text, BlurHash) */
        PLACEHOLDER(2);

        companion object {
            /**
             * Converts an integer value to the corresponding VolumetricType.
             *
             * @param value The integer value to convert
             * @return The corresponding VolumetricType, or [NONE] if no match found
             */
            fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: NONE
        }
    }

    /**
     * Defines the visual styles for arch decorations around the view.
     *
     * @property value The integer value used for XML attribute mapping
     * @see archesType
     * @see archesCount
     * @see archesDegreeArea
     * @see archesAngle
     */
    enum class ArchesType(val value: Int) {
        /** Individual, independent arch elements */
        SINGLE(0),

        /** Mirrored or symmetrical arch patterns that create balanced compositions */
        MIRROR(1);

        companion object {
            /**
             * Converts an integer value to the corresponding ArchesType.
             *
             * @param value The integer value to convert
             * @return The corresponding ArchesType, or [SINGLE] if no match found
             */
            fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: SINGLE
        }
    }
}
