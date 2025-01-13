package com.muratcan.apps.petvaccinetracker.util;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.UUID;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.imageview.ShapeableImageView;
import com.muratcan.apps.petvaccinetracker.R;

import timber.log.Timber;

public class ImageUtils {
    public static final String IMAGES_DIR = "pet_images";

    public static String copyImageToAppStorage(Context context, Uri sourceUri) {
        try {
            // Create the images directory if it doesn't exist
            File imagesDir = new File(context.getFilesDir(), IMAGES_DIR);
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }

            // Generate a unique filename
            String fileName = UUID.randomUUID().toString() + ".jpg";
            File destFile = new File(imagesDir, fileName);

            // Copy the image
            try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                
                if (in == null) {
                    Timber.e("Failed to open input stream for URI: %s", sourceUri);
                    return null;
                }

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();

                // Return the file path relative to the app's files directory
                return IMAGES_DIR + File.separator + fileName;
            }
        } catch (IOException e) {
            Timber.e(e, "Error copying image to app storage");
            return null;
        }
    }

    public static Uri getImageUri(Context context, String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        File imageFile = new File(context.getFilesDir(), relativePath);
        return imageFile.exists() ? Uri.fromFile(imageFile) : null;
    }

    public static void deleteImage(Context context, String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return;
        }
        File imageFile = new File(context.getFilesDir(), relativePath);
        if (imageFile.exists()) {
            imageFile.delete();
        }
    }

    public static void loadImage(Context context, String imageUri, ImageView imageView) {
        try {
            if (imageUri != null && !imageUri.isEmpty()) {
                Uri uri = getImageUri(context, imageUri);
                if (uri != null) {
                    Glide.with(context)
                        .load(uri)
                        .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_pet_placeholder)
                            .error(R.drawable.ic_pet_placeholder)
                            .centerCrop())
                        .into(imageView);
                    
                    // Reset placeholder styling
                    imageView.setPadding(0, 0, 0, 0);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setColorFilter(null);
                } else {
                    setPlaceholder(context, imageView);
                }
            } else {
                setPlaceholder(context, imageView);
            }
        } catch (Exception e) {
            Timber.e(e, "Error loading image from URI: %s", imageUri);
            setPlaceholder(context, imageView);
        }
    }

    public static void loadImage(Context context, String imageUri, PhotoView photoView) {
        try {
            if (imageUri != null && !imageUri.isEmpty()) {
                Uri uri = Uri.parse(imageUri);
                Glide.with(context)
                    .load(uri)
                    .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_pet_placeholder)
                        .error(R.drawable.ic_pet_placeholder)
                        .fitCenter())
                    .into(photoView);
            } else {
                photoView.setImageResource(R.drawable.ic_pet_placeholder);
            }
        } catch (Exception e) {
            Timber.e(e, "Error loading image from URI: %s", imageUri);
            photoView.setImageResource(R.drawable.ic_pet_placeholder);
        }
    }

    public static void loadImage(Context context, Uri imageUri, ShapeableImageView imageView) {
        try {
            if (imageUri != null) {
                Glide.with(context)
                    .load(imageUri)
                    .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_pet_placeholder)
                        .error(R.drawable.ic_pet_placeholder)
                        .centerCrop())
                    .into(imageView);
                
                // Reset placeholder styling
                imageView.setPadding(0, 0, 0, 0);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setColorFilter(null);
            } else {
                setPlaceholder(context, imageView);
            }
        } catch (Exception e) {
            Timber.e(e, "Error loading image from URI: %s", imageUri);
            setPlaceholder(context, imageView);
        }
    }

    public static void loadImage(Context context, Uri imageUri, ImageView imageView) {
        try {
            if (imageUri != null) {
                Glide.with(context)
                    .load(imageUri)
                    .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_pet_placeholder)
                        .error(R.drawable.ic_pet_placeholder)
                        .centerCrop())
                    .into(imageView);
                
                // Reset placeholder styling
                imageView.setPadding(0, 0, 0, 0);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setColorFilter(null);
            } else {
                setPlaceholder(context, imageView);
            }
        } catch (Exception e) {
            Timber.e(e, "Error loading image from URI: %s", imageUri);
            setPlaceholder(context, imageView);
        }
    }

    private static void setPlaceholder(Context context, ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_pet_placeholder);
        imageView.setPadding(
            context.getResources().getDimensionPixelSize(R.dimen.image_placeholder_padding),
            context.getResources().getDimensionPixelSize(R.dimen.image_placeholder_padding),
            context.getResources().getDimensionPixelSize(R.dimen.image_placeholder_padding),
            context.getResources().getDimensionPixelSize(R.dimen.image_placeholder_padding)
        );
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setColorFilter(context.getResources().getColor(R.color.md_theme_light_outline, context.getTheme()));
    }
} 