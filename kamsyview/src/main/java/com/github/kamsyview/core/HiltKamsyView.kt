package com.github.kamsyview.core

import android.content.Context
import android.util.AttributeSet
import com.github.kamsyview.interfaces.IBlurHashProcessor
import com.github.kamsyview.interfaces.IKamsyDrawableFactory
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.interfaces.IKamsyMetrics
import com.github.kamsyview.models.KamsyConfiguration
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hilt-enabled version of KamsyView for seamless dependency injection in Android components.
 *
 * HiltKamsyView extends the base [KamsyView] functionality with automatic dependency injection
 * capabilities provided by Dagger Hilt. This version is designed for use in Activities,
 * Fragments, and other Android components that support Hilt injection, eliminating the need
 * for manual dependency setup.
 *
 * ## Key Features
 * - **Automatic Dependency Injection**: All required dependencies are automatically injected by Hilt
 * - **Zero Configuration**: No manual dependency setup required in Hilt-enabled contexts
 * - **Seamless Integration**: Drop-in replacement for KamsyView in Hilt projects
 * - **Lifecycle Aware**: Dependencies are injected when the view is attached to the window
 * - **Fallback Support**: Falls back to manual injection if Hilt dependencies are not available
 *
 * ## Usage in Activities
 * ```kotlin
 * @AndroidEntryPoint
 * class MainActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         val hiltKamsyView = HiltKamsyView(this).apply {
 *             // All dependencies are automatically injected
 *             configure {
 *                 appearance { shape(KamsyShape.CIRCLE) }
 *                 border { width(4); color(Color.BLUE) }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Usage in Fragments
 * ```kotlin
 * @AndroidEntryPoint
 * class ProfileFragment : Fragment() {
 *     override fun onCreateView(
 *         inflater: LayoutInflater,
 *         container: ViewGroup?,
 *         savedInstanceState: Bundle?
 *     ): View {
 *         return HiltKamsyView(requireContext()).apply {
 *             // Dependencies injected automatically
 *             blurHash = "LGFFaXYk^6#M@-5c,1J5@[or[Q6."
 *         }
 *     }
 * }
 * ```
 *
 * ## XML Usage
 * ```xml
 * <com.github.kamsyview.core.KamsyView
 *     android:layout_width="80dp"
 *     android:layout_height="80dp"
 *     app:kamsyShape="circle"
 *     app:borderWidth="4dp"
 *     app:borderColor="@color/primary"
 *     app:placeholderText="JD" />
 * ```
 *
 * ## Dependency Requirements
 * This view requires the following Hilt modules to be properly configured:
 * - BlurHash processing module providing [IBlurHashProcessor]
 * - Drawable factory module providing [IKamsyDrawableFactory]
 * - Configuration module providing [KamsyConfiguration]
 * - Logging module providing [IKamsyLogger]
 * - Metrics module providing [IKamsyMetrics]
 *
 * ## Fallback Behavior
 * If Hilt injection fails or dependencies are not available, the view will:
 * 1. Log a warning about missing dependencies
 * 2. Fall back to the base KamsyView's manual dependency creation
 * 3. Continue functioning with default implementations
 *
 * ## Performance Considerations
 * - Dependency injection occurs only once when the view is attached to window
 * - No performance overhead compared to manual injection
 * - Hilt provides singleton instances where appropriate for memory efficiency
 *
 * @param context The context for the view (should be from a Hilt-enabled component)
 * @param attrs The attribute set from XML inflation
 * @param defStyleAttr The default style attribute
 *
 * @see KamsyView
 * @see AndroidEntryPoint
 * @see Inject
 *
 * @author Mbuodile Obiosio
 * @see <a href="https://linktr.ee/mbobiosio">Developer Profile</a>
 * @since 1.0.0
 */
@AndroidEntryPoint
class HiltKamsyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : KamsyView(context, attrs, defStyleAttr) {

    /**
     * Injected BlurHash processor for handling asynchronous BlurHash decoding operations.
     *
     * This processor is automatically injected by Hilt and provides:
     * - Asynchronous BlurHash string decoding
     * - Bitmap caching for performance optimization
     * - Thread-safe operations with proper lifecycle management
     * - Error handling and recovery mechanisms
     *
     * The processor is configured through Hilt modules and can be customized
     * via dependency injection configuration.
     *
     * @see IBlurHashProcessor
     * @see KamsyView.blurHashProcessor
     */
    @Inject
    lateinit var injectedBlurHashProcessor: IBlurHashProcessor

    /**
     * Injected drawable factory for creating view's drawable components.
     *
     * This factory is automatically injected by Hilt and provides:
     * - Creation of overlay drawables for status indicators and badges
     * - Creation of border drawables for various border styles
     * - Creation of volumetric drawables for 3D effects
     * - Creation of placeholder drawables for text and icon placeholders
     * - Consistent theming and styling across all drawable components
     *
     * The factory can be customized through Hilt modules to provide
     * application-specific drawable implementations.
     *
     * @see IKamsyDrawableFactory
     * @see KamsyView.drawableFactory
     */
    @Inject
    lateinit var injectedDrawableFactory: IKamsyDrawableFactory

    /**
     * Injected global configuration settings for KamsyView behavior.
     *
     * This configuration is automatically injected by Hilt and provides:
     * - Default colors and styling preferences
     * - Performance tuning parameters
     * - Feature toggle settings
     * - Caching and memory management configuration
     * - Application-wide KamsyView behavior settings
     *
     * Configuration can be customized through Hilt modules to match
     * application requirements and design system guidelines.
     *
     * @see KamsyConfiguration
     * @see KamsyView.configuration
     */
    @Inject
    lateinit var injectedConfiguration: KamsyConfiguration

    /**
     * Injected logger for debugging, monitoring, and error reporting.
     *
     * This logger is automatically injected by Hilt and provides:
     * - Structured logging with configurable levels
     * - Debug information for development and testing
     * - Error reporting and diagnostics
     * - Performance monitoring and profiling data
     * - Integration with application logging infrastructure
     *
     * Logger configuration can be customized through Hilt modules
     * to integrate with existing logging systems.
     *
     * @see IKamsyLogger
     * @see KamsyView.logger
     */
    @Inject
    lateinit var injectedLogger: IKamsyLogger

    /**
     * Injected metrics collector for performance monitoring and analytics.
     *
     * This metrics collector is automatically injected by Hilt and provides:
     * - View creation and lifecycle metrics
     * - BlurHash processing performance data
     * - Error rate tracking and analysis
     * - Memory usage and optimization metrics
     * - User interaction analytics
     * - Integration with application analytics platforms
     *
     * Metrics collection can be customized through Hilt modules
     * to integrate with existing analytics and monitoring systems.
     *
     * @see IKamsyMetrics
     * @see KamsyView.metrics
     */
    @Inject
    lateinit var injectedMetrics: IKamsyMetrics

    /**
     * Handles dependency injection and view initialization when attached to window.
     *
     * This method is called automatically when the view is attached to the window hierarchy.
     * It performs the following operations:
     *
     * 1. **Calls parent implementation**: Ensures base KamsyView initialization is completed
     * 2. **Verifies dependency injection**: Checks that Hilt has successfully injected dependencies
     * 3. **Injects dependencies**: Passes injected dependencies to the base view's injection system
     * 4. **Initializes drawables**: Triggers initialization of all drawable components
     * 5. **Starts state observation**: Begins monitoring view state changes
     *
     * ## Dependency Injection Flow
     * ```
     * View Creation → Hilt Injection → onAttachedToWindow → Dependency Verification →
     * Base View Injection → Drawable Initialization → State Observation
     * ```
     *
     * ## Error Handling
     * If Hilt injection fails or dependencies are not properly initialized:
     * - The method safely exits without crashing
     * - Base KamsyView falls back to creating default dependencies
     * - Warning is logged for debugging purposes
     * - View continues to function with reduced capabilities
     *
     * ## Thread Safety
     * This method is called on the main thread as part of the view lifecycle.
     * All dependency injection and initialization operations are main-thread safe.
     *
     * @see KamsyView.injectDependencies
     * @see KamsyView.initializeDrawables
     * @see KamsyView.observeState
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Ensure dependencies are injected
        if (::injectedBlurHashProcessor.isInitialized) {
            injectDependencies(
                injectedBlurHashProcessor,
                injectedDrawableFactory,
                injectedConfiguration,
                injectedLogger,
                injectedMetrics
            )
        }
    }
}
