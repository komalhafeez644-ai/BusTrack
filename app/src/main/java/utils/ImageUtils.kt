package utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.bustrack_app.R

object ImageUtils {

    /**
     * Loads a profile image with circular crop and Cloudinary face-detection transformation.
     */
    fun loadProfileImage(context: Context, url: String?, imageView: ImageView) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_person)
            return
        }

        val transformedUrl = if (url.contains("res.cloudinary.com")) {
            // Cloudinary face-detection transformation
            // c_thumb: crops to the face
            // g_face: gravity to face
            // r_max: makes it circular on the server side (optional, but good for bandwidth)
            if (url.contains("/upload/")) {
                url.replace("/upload/", "/upload/c_thumb,g_face,w_300,h_300,z_0.7/")
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
}
