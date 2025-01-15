package com.muratcan.apps.petvaccinetracker

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.muratcan.apps.petvaccinetracker.R
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper
import timber.log.Timber

class LoginActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var firebaseHelper: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate called")
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)
            Timber.d("Content view set successfully")
            
            firebaseHelper = FirebaseHelper()
            
            // Initialize views
            emailEditText = findViewById(R.id.emailEditText)
            passwordEditText = findViewById(R.id.passwordEditText)
            
            setupViews()
        } catch (e: Exception) {
            Timber.e(e, "Error in onCreate")
            throw e
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        if (firebaseHelper.isUserSignedIn()) {
            startMainActivity()
        }
    }
    
    private fun setupViews() {
        try {
            // Setup login button
            findViewById<Button>(R.id.loginButton).setOnClickListener {
                loginUser()
            }

            // Setup register button
            findViewById<Button>(R.id.registerButton).setOnClickListener {
                registerUser()
            }
            
            Timber.d("Views setup completed")
        } catch (e: Exception) {
            Timber.e(e, "Error setting up views")
            throw e
        }
    }
    
    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        firebaseHelper.signIn(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    var errorMessage = "Authentication failed"
                    task.exception?.message?.let { message ->
                        errorMessage += ": $message"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun registerUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        firebaseHelper.signUp(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                    startMainActivity()
                } else {
                    var errorMessage = "Registration failed"
                    task.exception?.message?.let { message ->
                        errorMessage += ": $message"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        // Prevent going back
        moveTaskToBack(true)
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.d("onConfigurationChanged: ${newConfig.orientation}")
        super.onConfigurationChanged(newConfig)
    }
    
    override fun onDestroy() {
        Timber.d("onDestroy called")
        super.onDestroy()
    }
} 