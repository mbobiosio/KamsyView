package com.github.kamsyview.impl

import android.graphics.Typeface
import com.github.kamsyview.core.KamsyView
import com.github.kamsyview.drawables.KamsyBorderDrawable
import com.github.kamsyview.drawables.KamsyOverlayDrawable
import com.github.kamsyview.drawables.KamsyPlaceholderDrawable
import com.github.kamsyview.drawables.KamsyVolumetricDrawable
import com.github.kamsyview.interfaces.IKamsyDrawableFactory
import com.github.kamsyview.interfaces.IKamsyLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
/**
 * Injectable drawable factory
 */
@Singleton
class InjectableKamsyDrawableFactory @Inject constructor(
    private val logger: IKamsyLogger
) : IKamsyDrawableFactory {

    override fun createBorderDrawable(kamsyView: KamsyView): KamsyBorderDrawable {
        logger.debug("Creating border drawable")
        return KamsyBorderDrawable(kamsyView)
    }

    override fun createVolumetricDrawable(kamsyView: KamsyView): KamsyVolumetricDrawable {
        logger.debug("Creating volumetric drawable")
        return KamsyVolumetricDrawable(kamsyView)
    }

    override fun createOverlayDrawable(kamsyView: KamsyView): KamsyOverlayDrawable {
        logger.debug("Creating overlay drawable")
        return KamsyOverlayDrawable(kamsyView)
    }

    override fun createPlaceholderDrawable(
        size: Int,
        backgroundColor: Int,
        text: CharSequence?,
        textColor: Int,
        textSize: Float,
        typeface: Typeface?,
        textSizePercentage: Float,
        avatarMargin: Int
    ): KamsyPlaceholderDrawable {
        logger.debug("Creating placeholder drawable")
        return KamsyPlaceholderDrawable(
            size = size,
            backgroundColor = backgroundColor,
            text = text,
            textColor = textColor,
            textSize = textSize,
            typeface = typeface,
            textSizePercentage = textSizePercentage,
            avatarMargin = avatarMargin
        )
    }
}
