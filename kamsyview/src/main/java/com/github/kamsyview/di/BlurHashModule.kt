package com.github.kamsyview.di

import com.github.kamsyview.interfaces.IKamsyLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
@Module
@InstallIn(SingletonComponent::class)
object BlurHashModule {

    /*@Provides
    @Singleton
    fun provideBlurHashProcessor(
        @ApplicationContext context: Context,
        @BlurHashScope processingScope: CoroutineScope,
        @BlurHashCacheSize cacheSize: Int,
        @BlurHashMaxConcurrentJobs maxConcurrentJobs: Int,
        logger: IKamsyLogger,
        metrics: IKamsyMetrics
    ): IBlurHashProcessor = InjectableBlurHashProcessor(
        context = context,
        cacheSize = cacheSize,
        maxConcurrentJobs = maxConcurrentJobs,
        processingScope = processingScope,
        logger = logger,
        metrics = metrics
    )*/

    @Provides
    @BlurHashScope
    @Singleton
    fun provideBlurHashScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
        logger: IKamsyLogger // Assuming you have this
    ): CoroutineScope {
        return CoroutineScope(
            SupervisorJob() +
                    dispatcher +
                    CoroutineExceptionHandler { _, throwable ->
                        logger.error("BlurHash processing error", throwable)
                    }
        )
    }
}
