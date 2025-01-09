package com.muratcan.apps.petvaccinetracker;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.chrisbanes.photoview.PhotoView;
import com.muratcan.apps.petvaccinetracker.util.ImageUtils;

public class ImagePreviewActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URI = "image_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");  // Empty title for cleaner look
        }

        // Get image URI from intent
        String imageUri = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (imageUri == null) {
            finish();
            return;
        }

        // Set up PhotoView
        PhotoView photoView = findViewById(R.id.photoView);
        ImageUtils.loadImage(this, imageUri, photoView);

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                supportFinishAfterTransition();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            supportFinishAfterTransition();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 