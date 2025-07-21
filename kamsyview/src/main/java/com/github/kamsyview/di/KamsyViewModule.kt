package com.github.kamsyview.di

import com.github.kamsyview.interfaces.IBlurHashProcessor
import com.github.kamsyview.interfaces.IKamsyDrawableFactory
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.interfaces.IKamsyMetrics
import com.github.kamsyview.interfaces.IKamsyViewFactory
import com.github.kamsyview.models.KamsyConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * Main KamsyView Hilt module
 */
@Module
@InstallIn(SingletonComponent::class)
object KamsyViewModule {

    @Provides
    @Singleton
    fun provideKamsyConfiguration(): KamsyConfiguration {
        return KamsyConfiguration(
            blurHashCacheSize = 50,
            blurHashMaxConcurrentJobs = 3,
            enableLogging = true,
            enableMetrics = false, //Todo Disable in production
            defaultAnimationDuration = 1000L,
            enableHardwareAcceleration = true
        )
    }

    @Provides
    @BlurHashCacheSize
    fun provideBlurHashCacheSize(configuration: KamsyConfiguration): Int {
        return configuration.blurHashCacheSize
    }

    @Provides
    @BlurHashMaxConcurrentJobs
    fun provideBlurHashMaxConcurrentJobs(configuration: KamsyConfiguration): Int {
        return configuration.blurHashMaxConcurrentJobs
    }

    /*@Provides
    @BlurHashScope
    @Singleton
    fun provideBlurHashScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope {
        return CoroutineScope(SupervisorJob() + dispatcher)
    }*/

    @Provides
    @ApplicationScope
    @Singleton
    fun provideApplicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope {
        return CoroutineScope(SupervisorJob() + dispatcher)
    }
}

/**
 * Entry point for accessing KamsyView dependencies
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface KamsyViewEntryPoint {
    fun kamsyViewFactory(): IKamsyViewFactory
    fun blurHashProcessor(): IBlurHashProcessor
    fun drawableFactory(): IKamsyDrawableFactory
    fun metrics(): IKamsyMetrics
    fun logger(): IKamsyLogger
}
