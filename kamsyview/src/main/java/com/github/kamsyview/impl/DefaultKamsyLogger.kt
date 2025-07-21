package com.github.kamsyview.impl

import android.util.Log
import com.github.kamsyview.interfaces.IKamsyLogger
import com.github.kamsyview.models.KamsyConfiguration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * Default logger implementation
 */
@Singleton
class DefaultKamsyLogger @Inject constructor(
    private val configuration: KamsyConfiguration
) : IKamsyLogger {

    companion object {
        private const val TAG = "KamsyView"
    }

    override fun debug(message: String) {
        if (configuration.enableLogging) {
            Log.d(TAG, message)
        }
    }

    override fun info(message: String) {
        if (configuration.enableLogging) {
            Log.i(TAG, message)
        }
    }

    override fun warning(message: String) {
        if (configuration.enableLogging) {
            Log.w(TAG, message)
        }
    }

    override fun error(message: String, throwable: Throwable?) {
        if (configuration.enableLogging) {
            Log.e(TAG, message, throwable)
        }
    }
}
