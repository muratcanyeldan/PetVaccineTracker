package com.muratcan.apps.petvaccinetracker;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.muratcan.apps.petvaccinetracker.util.NotificationSettingsHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private NotificationSettingsHelper settingsHelper;
    private LinearLayout reminderDaysContainer;
    private TextView notificationTimeText;
    private MaterialButton saveButton;
    private List<MaterialCheckBox> reminderCheckBoxes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsHelper = new NotificationSettingsHelper(this);
        reminderCheckBoxes = new ArrayList<>();

        initViews();
        setupToolbar();
        loadCurrentSettings();
        setupClickListeners();
    }

    private void initViews() {
        reminderDaysContainer = findViewById(R.id.reminder_days_container);
        notificationTimeText = findViewById(R.id.notification_time_text);
        saveButton = findViewById(R.id.save_settings_button);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notification Settings");
        }
    }

    private void loadCurrentSettings() {
        // Load reminder days
        setupReminderDaysCheckboxes();

        // Load notification time
        updateNotificationTimeDisplay();
    }

    private void setupReminderDaysCheckboxes() {
        reminderDaysContainer.removeAllViews();
        reminderCheckBoxes.clear();

        int[] availableDays = NotificationSettingsHelper.getAllAvailableReminderDays();
        int[] enabledDays = settingsHelper.getReminderDays();

        for (int day : availableDays) {
            MaterialCheckBox checkBox = new MaterialCheckBox(this);
            checkBox.setText(NotificationSettingsHelper.getReminderDayDisplayText(this, day));
            checkBox.setTag(day);

            // Check if this day is currently enabled
            boolean isEnabled = false;
            for (int enabledDay : enabledDays) {
                if (enabledDay == day) {
                    isEnabled = true;
                    break;
                }
            }
            checkBox.setChecked(isEnabled);

            reminderCheckBoxes.add(checkBox);
            reminderDaysContainer.addView(checkBox);
        }
    }

    private void updateNotificationTimeDisplay() {
        int hour = settingsHelper.getNotificationHour();
        int minute = settingsHelper.getNotificationMinute();

        String timeText = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        notificationTimeText.setText("Remind me at: " + timeText);
    }

    private void setupClickListeners() {
        // Time picker
        findViewById(R.id.notification_time_container).setOnClickListener(v -> showTimePicker());

        // Save button
        saveButton.setOnClickListener(v -> saveSettings());
    }

    private void showTimePicker() {
        int currentHour = settingsHelper.getNotificationHour();
        int currentMinute = settingsHelper.getNotificationMinute();

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    settingsHelper.setNotificationTime(hourOfDay, minute);
                    updateNotificationTimeDisplay();
                },
                currentHour,
                currentMinute,
                true // 24-hour format
        );

        timePickerDialog.show();
    }

    private void saveSettings() {
        // Get selected reminder days
        List<Integer> selectedDays = new ArrayList<>();
        for (MaterialCheckBox checkBox : reminderCheckBoxes) {
            if (checkBox.isChecked()) {
                selectedDays.add((Integer) checkBox.getTag());
            }
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one reminder option", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert to array and save
        int[] reminderDays = selectedDays.stream().mapToInt(Integer::intValue).toArray();
        settingsHelper.setReminderDays(reminderDays);

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}