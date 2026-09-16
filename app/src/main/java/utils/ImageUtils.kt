package utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.bustrack_app.R

object ImageUtils {

    /**
     * Loads a profile image with face-focused circular crop and Cloudinary face-detection transformation.
     */
    fun loadProfileImage(context: Context, url: String?, imageView: ImageView) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_person)
            return
        }

        val transformedUrl = if (url.contains("res.cloudinary.com")) {
            // Cloudinary face-detection transformation
            // z_0.3: Reduced zoom to ensure the full head/hair is visible with a natural margin above
            // c_thumb, g_face: Intelligently crops around the face while including shoulders
            if (url.contains("/upload/")) {
                url.replace("/upload/", "/upload/c_thumb,g_face,w_400,h_400,z_0.3/")
            } else {
                url
            }
        } else {
            url
        }

        Glide.with(context)
            .load(transformedUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }

    /**
     * Loads a profile image for preview (before upload)
     */
    fun loadPreviewImage(context: Context, uri: android.net.Uri, imageView: ImageView) {
        Glide.with(context)
            .load(uri)
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(imageView)
    }

    /**
     * Shows a compact popup menu to pick a photo or choose 'No Photo'.
     */
    fun showPhotoOptionsDialog(context: Context, anchorView: android.view.View, onGallerySelected: () -> Unit, onNoPhotoSelected: () -> Unit) {
        val themedContext = android.view.ContextThemeWrapper(context, R.style.PopupMenuTheme)
        val popup = android.widget.PopupMenu(themedContext, anchorView)
        
        // Adding items programmatically to keep it simple and avoid needing a menu XML
        popup.menu.add(0, 0, 0, "Choose from Gallery")
        popup.menu.add(0, 1, 1, "No Photo / Keep Empty")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> onGallerySelected()
                1 -> onNoPhotoSelected()
            }
            true
        }
        
        popup.show()
    }
}
