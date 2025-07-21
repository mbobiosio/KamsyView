package com.github.kamsyview.extensions

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import com.github.kamsyview.core.KamsyView
import com.github.kamsyview.core.KamsyViewConfiguration
import com.github.kamsyview.core.configure
import com.github.kamsyview.di.KamsyViewEntryPoint
import com.github.kamsyview.interfaces.IBlurHashProcessor
import com.github.kamsyview.interfaces.IKamsyDrawableFactory
import com.github.kamsyview.interfaces.IKamsyViewFactory
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
// Extension function for Context
fun Context.colorAttribute(@AttrRes colorRes: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(colorRes, typedValue, true)
    return typedValue.data
}

/**
 * Extension function for easy dependency injection in Activities/Fragments
 */
@ActivityContext
inline fun <reified T : Any> Context.injectKamsyView(): T {
    return EntryPointAccessors.fromApplication(
        this,
        KamsyViewEntryPoint::class.java
    ).let { entryPoint ->
        when (T::class) {
            IKamsyViewFactory::class -> entryPoint.kamsyViewFactory() as T
            IBlurHashProcessor::class -> entryPoint.blurHashProcessor() as T
            IKamsyDrawableFactory::class -> entryPoint.drawableFactory() as T
            else -> throw IllegalArgumentException("Unknown type: ${T::class}")
        }
    }
}

/**
 * Hilt-aware KamsyView builder
 */
class HiltKamsyViewBuilder @Inject constructor(
    private val factory: IKamsyViewFactory
) {
    fun build(context: Context, block: KamsyViewConfiguration.() -> Unit = {}): KamsyView {
        return factory.create(context).apply {
            configure(block)
        }
    }
}


/**
 * Calculate text size based on view size
 */
fun KamsyView.calculateTextSize(): Float {
    val baseSize = measuredWidth.takeIf { it > 0 }?.toFloat() ?: 100f
    return baseSize / 3f
}
