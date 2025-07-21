package com.github.kamsyview.impl

import com.github.kamsyview.core.KamsyError
import com.github.kamsyview.core.getUserMessage
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.interfaces.IKamsyMetrics
import com.github.kamsyview.models.KamsyConfiguration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * Default metrics implementation
 */
@Singleton
class DefaultKamsyMetrics @Inject constructor(
    private val logger: IKamsyLogger,
    private val configuration: KamsyConfiguration
) : IKamsyMetrics {

    private var blurHashProcessingCount = 0
    private var totalBlurHashProcessingTime = 0L
    private var cacheHitCount = 0
    private var cacheMissCount = 0
    private var errorCount = 0
    private var viewCreationCount = 0

    override fun recordBlurHashProcessingTime(duration: Long) {
        if (configuration.enableMetrics) {
            blurHashProcessingCount++
            totalBlurHashProcessingTime += duration
            logger.debug("BlurHash processing metrics: count=$blurHashProcessingCount, avgTime=${totalBlurHashProcessingTime/blurHashProcessingCount}ms")
        }
    }

    override fun recordCacheHit() {
        if (configuration.enableMetrics) {
            cacheHitCount++
        }
    }

    override fun recordCacheMiss() {
        if (configuration.enableMetrics) {
            cacheMissCount++
        }
    }

    override fun recordError(error: KamsyError) {
        if (configuration.enableMetrics) {
            errorCount++
            logger.warning("Error recorded: ${error.getUserMessage()}")
        }
    }

    override fun incrementViewCreation() {
        if (configuration.enableMetrics) {
            viewCreationCount++
        }
    }

    fun getMetrics(): Map<String, Any> = mapOf(
        "blurHashProcessingCount" to blurHashProcessingCount,
        "averageBlurHashProcessingTime" to if (blurHashProcessingCount > 0) totalBlurHashProcessingTime / blurHashProcessingCount else 0,
        "cacheHitCount" to cacheHitCount,
        "cacheMissCount" to cacheMissCount,
        "cacheHitRate" to if (cacheHitCount + cacheMissCount > 0) cacheHitCount.toFloat() / (cacheHitCount + cacheMissCount) else 0f,
        "errorCount" to errorCount,
        "viewCreationCount" to viewCreationCount
    )
}
