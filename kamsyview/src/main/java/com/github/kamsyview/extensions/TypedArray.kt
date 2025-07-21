package com.github.kamsyview.extensions

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
@ColorInt
fun TypedArray.getColorOrNull(index: Int): Int? {
    val color = getColor(index, Integer.MAX_VALUE)
    return if (color == Integer.MAX_VALUE) null else color
}

fun TypedArray.getTypefaceOrNull(context: Context, index: Int): Typeface? {
    return runCatching {
        getResourceId(index, 0).takeIf { it != 0 }?.let { resId ->
            ResourcesCompat.getFont(context, resId)
        } ?: getString(index)?.lowercase()?.let { fontFamily ->
            if (fontFamily.startsWith("res/") || fontFamily.endsWith(".xml")) {
                return@let null
            }

            when (fontFamily) {
                "serif" -> Typeface.SERIF
                "sans-serif" -> Typeface.SANS_SERIF
                "monospace" -> Typeface.MONOSPACE
                "casual", "cursive", "fantasy" -> Typeface.create(fontFamily, Typeface.NORMAL)
                else -> Typeface.create(fontFamily, Typeface.NORMAL)
            }
        }
    }.getOrNull()
}
