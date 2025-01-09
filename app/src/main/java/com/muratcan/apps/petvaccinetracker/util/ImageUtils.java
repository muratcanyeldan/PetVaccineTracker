package com.muratcan.apps.petvaccinetracker.util;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.imageview.ShapeableImageView;
import com.muratcan.apps.petvaccinetracker.R;

public class ImageUtils {
    public static void loadImage(Context context, String imageUri, ImageView imageView) {
        if (imageUri != null && !imageUri.isEmpty()) {
            Glide.with(context)
                .load(imageUri)
                .apply(new RequestOptions()
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .centerCrop())
                .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_pet_placeholder);
        }
    }

    public static void loadImage(Context context, String imageUri, PhotoView photoView) {
        if (imageUri != null && !imageUri.isEmpty()) {
            Glide.with(context)
                .load(imageUri)
                .apply(new RequestOptions()
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .fitCenter())
                .into(photoView);
        } else {
            photoView.setImageResource(R.drawable.ic_pet_placeholder);
        }
    }

    public static void loadImage(Context context, Uri imageUri, ShapeableImageView imageView) {
        if (imageUri != null) {
            Glide.with(context)
                .load(imageUri)
                .apply(new RequestOptions()
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .centerCrop())
                .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_pet_placeholder);
        }
    }
} 