package com.github.kamsyview.impl

import android.content.Context
import com.github.kamsyview.core.KamsyView
import com.github.kamsyview.interfaces.IBlurHashProcessor
import com.github.kamsyview.interfaces.IKamsyDrawableFactory
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.interfaces.IKamsyMetrics
import com.github.kamsyview.interfaces.IKamsyViewFactory
import com.github.kamsyview.models.KamsyConfiguration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * Injectable KamsyView factory
 */
@Singleton
class InjectableKamsyViewFactory @Inject constructor(
    private val blurHashProcessor: IBlurHashProcessor,
    private val drawableFactory: IKamsyDrawableFactory,
    private val configuration: KamsyConfiguration,
    private val logger: IKamsyLogger,
    private val metrics: IKamsyMetrics
) : IKamsyViewFactory {

    override fun create(context: Context): KamsyView {
        logger.debug("Creating new KamsyView")
        metrics.incrementViewCreation()

        return KamsyView(context).apply {
            // Inject dependencies
            this.blurHashProcessor = this@InjectableKamsyViewFactory.blurHashProcessor
            this.drawableFactory = this@InjectableKamsyViewFactory.drawableFactory
            this.configuration = this@InjectableKamsyViewFactory.configuration
            this.logger = this@InjectableKamsyViewFactory.logger
            this.metrics = this@InjectableKamsyViewFactory.metrics
        }
    }
}
