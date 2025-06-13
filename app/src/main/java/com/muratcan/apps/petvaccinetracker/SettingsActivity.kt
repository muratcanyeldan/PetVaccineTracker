package com.muratcan.apps.petvaccinetracker

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.muratcan.apps.petvaccinetracker.util.NotificationHelper
import com.muratcan.apps.petvaccinetracker.util.NotificationSettingsHelper
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var settingsHelper: NotificationSettingsHelper
    private lateinit var reminderDaysContainer: LinearLayout
    private lateinit var notificationTimeText: TextView
    private lateinit var saveButton: MaterialButton
    private val reminderCheckBoxes = mutableListOf<MaterialCheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsHelper = NotificationSettingsHelper(this)

        initViews()
        setupToolbar()
        loadCurrentSettings()
        setupClickListeners()
    }

    private fun initViews() {
        reminderDaysContainer = findViewById(R.id.reminder_days_container)
        notificationTimeText = findViewById(R.id.notification_time_text)
        saveButton = findViewById(R.id.save_settings_button)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Notification Settings"
        }
    }

    private fun loadCurrentSettings() {
        // Load reminder days
        setupReminderDaysCheckboxes()

        // Load notification time
        updateNotificationTimeDisplay()
    }

    private fun setupReminderDaysCheckboxes() {
        reminderDaysContainer.removeAllViews()
        reminderCheckBoxes.clear()

        val availableDays = NotificationSettingsHelper.getAllAvailableReminderDays()
        val enabledDays = settingsHelper.getReminderDays()

        for (day in availableDays) {
            val checkBox = MaterialCheckBox(this).apply {
                text =
                    NotificationSettingsHelper.getReminderDayDisplayText(this@SettingsActivity, day)
                tag = day
                isChecked = day in enabledDays
            }

            reminderCheckBoxes.add(checkBox)
            reminderDaysContainer.addView(checkBox)
        }
    }

    private fun updateNotificationTimeDisplay() {
        val hour = settingsHelper.getNotificationHour()
        val minute = settingsHelper.getNotificationMinute()

        val timeText = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        notificationTimeText.text = "Remind me at: $timeText"
    }

    private fun setupClickListeners() {
        // Time picker
        findViewById<View>(R.id.notification_time_container).setOnClickListener { showTimePicker() }

        // Save button
        saveButton.setOnClickListener { saveSettings() }
    }

    private fun showTimePicker() {
        val currentHour = settingsHelper.getNotificationHour()
        val currentMinute = settingsHelper.getNotificationMinute()

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                settingsHelper.setNotificationTime(hourOfDay, minute)
                updateNotificationTimeDisplay()

                // Reschedule all existing notifications with new time
                NotificationHelper.rescheduleAllExistingNotifications(this)

                Toast.makeText(
                    this,
                    "Notification time updated for all existing reminders",
                    Toast.LENGTH_SHORT
                ).show()
            },
            currentHour,
            currentMinute,
            true // 24-hour format
        )

        timePickerDialog.show()
    }

    private fun saveSettings() {
        // Get selected reminder days
        val selectedDays = reminderCheckBoxes
            .filter { it.isChecked }
            .map { it.tag as Int }

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one reminder option", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Convert to array and save
        val reminderDays = selectedDays.toIntArray()
        settingsHelper.setReminderDays(reminderDays)

        // Reschedule all existing notifications with new settings
        NotificationHelper.rescheduleAllExistingNotifications(this)

        Toast.makeText(this, "Settings saved and notifications updated", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
} 