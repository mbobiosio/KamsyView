package com.github.kamsyview.di

import com.github.kamsyview.impl.DefaultKamsyLogger
import com.github.kamsyview.impl.DefaultKamsyMetrics
import com.github.kamsyview.impl.InjectableBlurHashProcessor
import com.github.kamsyview.impl.InjectableKamsyDrawableFactory
import com.github.kamsyview.impl.InjectableKamsyViewFactory
import com.github.kamsyview.interfaces.IBlurHashProcessor
import com.github.kamsyview.interfaces.IKamsyDrawableFactory
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.interfaces.IKamsyMetrics
import com.github.kamsyview.interfaces.IKamsyViewFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * Bindings module for interfaces
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class KamsyViewBindingsModule {

    @Binds
    abstract fun bindBlurHashProcessor(
        implementation: InjectableBlurHashProcessor
    ): IBlurHashProcessor

    @Binds
    abstract fun bindKamsyViewFactory(
        implementation: InjectableKamsyViewFactory
    ): IKamsyViewFactory

    @Binds
    abstract fun bindKamsyDrawableFactory(
        implementation: InjectableKamsyDrawableFactory
    ): IKamsyDrawableFactory

    @Binds
    abstract fun bindKamsyMetrics(
        implementation: DefaultKamsyMetrics
    ): IKamsyMetrics

    @Binds
    abstract fun bindKamsyLogger(
        implementation: DefaultKamsyLogger
    ): IKamsyLogger
}
