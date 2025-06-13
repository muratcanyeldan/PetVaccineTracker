package com.muratcan.apps.petvaccinetracker

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.chrisbanes.photoview.PhotoView
import com.muratcan.apps.petvaccinetracker.util.ImageUtils

class ImagePreviewActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_IMAGE_URI = "image_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.title = ""  // Empty title for cleaner look
        }

        // Get image URI from intent
        val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUri == null) {
            finish()
            return
        }

        // Set up PhotoView
        val photoView = findViewById<PhotoView>(R.id.photoView)
        ImageUtils.loadImage(this, imageUri, photoView)

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                supportFinishAfterTransition()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            supportFinishAfterTransition()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 