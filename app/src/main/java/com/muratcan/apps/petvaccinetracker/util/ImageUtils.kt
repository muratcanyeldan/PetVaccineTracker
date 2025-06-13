package com.muratcan.apps.petvaccinetracker.util

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.imageview.ShapeableImageView
import com.muratcan.apps.petvaccinetracker.R
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

object ImageUtils {
    const val IMAGES_DIR = "pet_images"

    fun copyImageToAppStorage(context: Context, sourceUri: Uri): String? {
        return try {
            // Create the images directory if it doesn't exist
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            // Generate a unique filename
            val fileName = "${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir, fileName)

            // Copy the image
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            } ?: run {
                Timber.e("Failed to open input stream for URI: %s", sourceUri)
                return null
            }

            // Return the file path relative to the app's files directory
            "$IMAGES_DIR${File.separator}$fileName"
        } catch (e: IOException) {
            Timber.e(e, "Error copying image to app storage")
            null
        }
    }

    fun getImageUri(context: Context, relativePath: String?): Uri? {
        if (relativePath.isNullOrEmpty()) {
            return null
        }
        val imageFile = File(context.filesDir, relativePath)
        return if (imageFile.exists()) Uri.fromFile(imageFile) else null
    }

    fun deleteImage(context: Context, relativePath: String?) {
        if (relativePath.isNullOrEmpty()) {
            return
        }
        val imageFile = File(context.filesDir, relativePath)
        if (imageFile.exists()) {
            imageFile.delete()
        }
    }

    fun loadImage(context: Context, imageUri: String?, imageView: ImageView) {
        try {
            if (!imageUri.isNullOrEmpty()) {
                val uri = getImageUri(context, imageUri)
                if (uri != null) {
                    Glide.with(context)
                        .load(uri)
                        .apply(
                            RequestOptions()
                                .placeholder(R.drawable.ic_pet_placeholder)
                                .error(R.drawable.ic_pet_placeholder)
                                .centerCrop()
                        )
                        .into(imageView)

                    // Reset placeholder styling
                    imageView.setPadding(0, 0, 0, 0)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.colorFilter = null
                } else {
                    setPlaceholder(context, imageView)
                }
            } else {
                setPlaceholder(context, imageView)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading image from URI: %s", imageUri)
            setPlaceholder(context, imageView)
        }
    }

    fun loadImage(context: Context, imageUri: String?, photoView: PhotoView) {
        try {
            if (!imageUri.isNullOrEmpty()) {
                val uri = Uri.parse(imageUri)
                Glide.with(context)
                    .load(uri)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.ic_pet_placeholder)
                            .error(R.drawable.ic_pet_placeholder)
                            .fitCenter()
                    )
                    .into(photoView)
            } else {
                photoView.setImageResource(R.drawable.ic_pet_placeholder)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading image from URI: %s", imageUri)
            photoView.setImageResource(R.drawable.ic_pet_placeholder)
        }
    }

    fun loadImage(context: Context, imageUri: Uri?, imageView: ShapeableImageView) {
        try {
            if (imageUri != null) {
                Glide.with(context)
                    .load(imageUri)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.ic_pet_placeholder)
                            .error(R.drawable.ic_pet_placeholder)
                            .centerCrop()
                    )
                    .into(imageView)

                // Reset placeholder styling
                imageView.setPadding(0, 0, 0, 0)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.colorFilter = null
            } else {
                setPlaceholder(context, imageView)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading image from URI: %s", imageUri)
            setPlaceholder(context, imageView)
        }
    }

    fun loadImage(context: Context, imageUri: Uri?, imageView: ImageView) {
        try {
            if (imageUri != null) {
                Glide.with(context)
                    .load(imageUri)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.ic_pet_placeholder)
                            .error(R.drawable.ic_pet_placeholder)
                            .centerCrop()
                    )
                    .into(imageView)

                // Reset placeholder styling
                imageView.setPadding(0, 0, 0, 0)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.colorFilter = null
            } else {
                setPlaceholder(context, imageView)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading image from URI: %s", imageUri)
            setPlaceholder(context, imageView)
        }
    }

    private fun setPlaceholder(context: Context, imageView: ImageView) {
        imageView.setImageResource(R.drawable.ic_pet_placeholder)
        val padding = context.resources.getDimensionPixelSize(R.dimen.image_placeholder_padding)
        imageView.setPadding(padding, padding, padding, padding)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageView.setColorFilter(
            context.resources.getColor(
                R.color.md_theme_light_outline,
                context.theme
            )
        )
    }
} 