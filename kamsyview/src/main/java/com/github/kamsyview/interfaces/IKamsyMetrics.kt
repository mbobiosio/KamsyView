package com.github.kamsyview.interfaces

import com.github.kamsyview.core.KamsyError

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * Interface for metrics collection
 */
interface IKamsyMetrics {
    fun recordBlurHashProcessingTime(duration: Long)
    fun recordCacheHit()
    fun recordCacheMiss()
    fun recordError(error: KamsyError)
    fun incrementViewCreation()
}
