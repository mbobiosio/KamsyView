package com.github.kamsyview.interfaces

import android.content.Context
import com.github.kamsyview.core.KamsyView

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * Interface for KamsyView factory
 */
interface IKamsyViewFactory {
    fun create(context: Context): KamsyView
}
