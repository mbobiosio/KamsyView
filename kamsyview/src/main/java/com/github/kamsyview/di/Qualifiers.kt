package com.github.kamsyview.di

import javax.inject.Qualifier

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BlurHashScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BlurHashCacheSize

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BlurHashMaxConcurrentJobs
