package utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

object ViewUtils {
    fun applyClickEffect(view: View) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    fun applyPressEffect(view: View) {
        view.animate()
            .translationY(8f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .translationY(0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        return drawable.toBitmap()
    }
}
