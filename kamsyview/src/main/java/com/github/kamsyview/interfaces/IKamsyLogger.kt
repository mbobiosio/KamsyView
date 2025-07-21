package com.github.kamsyview.interfaces

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * Interface for logging
 */
interface IKamsyLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warning(message: String)
    fun error(message: String, throwable: Throwable? = null)
}
