package com.muratcan.apps.petvaccinetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class AddVaccineActivity extends AppCompatActivity {
    private TextInputEditText nameInput;
    private TextInputEditText notesInput;
    private EditText dateAdministeredInput;
    private AutoCompleteTextView vaccineTypeInput;
    private AutoCompleteTextView recurringPeriodInput;
    private View recurringOptionsContainer;
    private AppDatabase database;
    private long petId;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vaccine);
        
        // Initialize views
        nameInput = findViewById(R.id.vaccine_name_input);
        notesInput = findViewById(R.id.vaccine_notes_input);
        dateAdministeredInput = findViewById(R.id.date_administered_input);
        vaccineTypeInput = findViewById(R.id.vaccine_type_input);
        recurringPeriodInput = findViewById(R.id.recurring_period_input);
        recurringOptionsContainer = findViewById(R.id.recurring_options_container);
        Button saveButton = findViewById(R.id.save_vaccine_button);
        Button cancelButton = findViewById(R.id.cancel_button);
        
        // Get pet ID and vaccine from intent
        petId = getIntent().getLongExtra("pet_id", -1);
        Vaccine vaccine = getIntent().getParcelableExtra("vaccine");
        
        if (petId == -1) {
            Toast.makeText(this, R.string.error_pet_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize database
        database = AppDatabase.getInstance(this);
        
        // Set up date picker
        dateAdministeredInput.setOnClickListener(v -> showDatePicker(dateAdministeredInput));
        
        // Set up vaccine type dropdown
        String[] vaccineTypes = getResources().getStringArray(R.array.recurring_options);
        ArrayAdapter<String> vaccineTypeAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, vaccineTypes);
        vaccineTypeInput.setAdapter(vaccineTypeAdapter);
        
        // Set up recurring period dropdown
        String[] recurringPeriods = getResources().getStringArray(R.array.months_array);
        ArrayAdapter<String> recurringPeriodAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, recurringPeriods);
        recurringPeriodInput.setAdapter(recurringPeriodAdapter);
        
        // Handle vaccine type selection
        vaccineTypeInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedType = parent.getItemAtPosition(position).toString();
            if (selectedType.equals(getString(R.string.recurring))) {
                recurringOptionsContainer.setVisibility(View.VISIBLE);
                if (recurringPeriodInput.getText().toString().isEmpty()) {
                    recurringPeriodInput.setText(getString(R.string.months_format, 1), false);
                }
            } else {
                recurringOptionsContainer.setVisibility(View.GONE);
            }
        });
        
        // Load vaccine details if editing
        if (vaccine != null) {
            nameInput.setText(vaccine.getName());
            notesInput.setText(vaccine.getNotes());
            
            Date administered = vaccine.getDateAdministered();
            if (administered != null && administered.getTime() > 0) {
                dateAdministeredInput.setText(dateFormat.format(administered));
            }
            
            // Set default type to "One Time Only" if not recurring
            boolean isRecurring = vaccine.getNextDueDate() != null && vaccine.getNextDueDate().getTime() > 0;
            vaccineTypeInput.setText(getString(R.string.one_time_only), false);
            
            if (isRecurring && administered != null && administered.getTime() > 0) {
                vaccineTypeInput.setText(getString(R.string.recurring), false);
                recurringOptionsContainer.setVisibility(View.VISIBLE);
                // Calculate months between administered and next due
                Calendar administeredCal = Calendar.getInstance();
                Calendar nextDueCal = Calendar.getInstance();
                administeredCal.setTime(administered);
                nextDueCal.setTime(vaccine.getNextDueDate());
                
                int months = (nextDueCal.get(Calendar.YEAR) - administeredCal.get(Calendar.YEAR)) * 12 
                    + (nextDueCal.get(Calendar.MONTH) - administeredCal.get(Calendar.MONTH));
                
                if (months > 0 && months <= 12) {
                    recurringPeriodInput.setText(getString(R.string.months_format, months), false);
                }
            }
        } else {
            // For new vaccines, default to "One Time Only"
            vaccineTypeInput.setText(getString(R.string.one_time_only), false);
        }
        
        // Set up save button
        saveButton.setOnClickListener(v -> saveVaccine());
        
        // Set up cancel button
        cancelButton.setOnClickListener(v -> confirmExit());
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(vaccine != null ? R.string.edit_vaccine : R.string.add_vaccine);
        }
        
        // Add back pressed callback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExit();
            }
        });
    }
    
    private void showDatePicker(final EditText dateInput) {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            
            // Validate that selected date is not in the future
            if (calendar.getTime().after(new Date())) {
                Toast.makeText(this, R.string.date_future_error, Toast.LENGTH_SHORT).show();
                return;
            }
            
            dateInput.setText(dateFormat.format(calendar.getTime()));
        };
        
        // Set max date to today
        DatePickerDialog dialog = new DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }
    
    private void saveVaccine() {
        String name = Objects.requireNonNullElse(nameInput.getText(), "").toString().trim();
        String notes = Objects.requireNonNullElse(notesInput.getText(), "").toString().trim();
        String dateAdministered = dateAdministeredInput.getText().toString().trim();
        String vaccineType = vaccineTypeInput.getText().toString().trim();
        
        if (name.isEmpty()) {
            nameInput.setError(getString(R.string.vaccine_name_required));
            return;
        }
        
        try {
            Date administered = dateAdministered.isEmpty() ? null : dateFormat.parse(dateAdministered);
            Date nextDue = null;
            
            if (administered != null && vaccineType.equals(getString(R.string.recurring))) {
                String recurringPeriod = recurringPeriodInput.getText().toString();
                if (recurringPeriod.isEmpty()) {
                    recurringPeriodInput.setError(getString(R.string.recurring_period_required));
                    return;
                }
                
                // Parse the number of months from the recurring period (e.g., "3 months" -> 3)
                int months = Integer.parseInt(recurringPeriod.split(" ")[0]);
                
                // Calculate next due date
                Calendar nextDueCalendar = Calendar.getInstance();
                nextDueCalendar.setTime(administered);
                nextDueCalendar.add(Calendar.MONTH, months);
                nextDue = nextDueCalendar.getTime();
            }
            
            final Vaccine existingVaccine = getIntent().getParcelableExtra("vaccine");
            final Vaccine vaccineToSave = existingVaccine != null ? existingVaccine : new Vaccine();
            
            vaccineToSave.setName(name);
            vaccineToSave.setNotes(notes);
            vaccineToSave.setDateAdministered(administered);
            vaccineToSave.setNextDueDate(nextDue);
            vaccineToSave.setPetId(petId);
            
            new Thread(() -> {
                try {
                    if (vaccineToSave.getId() == 0) {
                        // New vaccine
                        long vaccineId = database.vaccineDao().insert(vaccineToSave);
                        vaccineToSave.setId(vaccineId);
                    } else {
                        // Update existing vaccine
                        database.vaccineDao().update(vaccineToSave);
                    }
                    
                    final boolean isNewVaccine = vaccineToSave.getId() == 0;
                    runOnUiThread(() -> {
                        Toast.makeText(this, isNewVaccine ? R.string.vaccine_added : R.string.vaccine_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.error_saving_vaccine, Toast.LENGTH_SHORT).show());
                }
            }).start();
            
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_invalid_date, Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean hasUnsavedChanges() {
        String name = Objects.requireNonNull(nameInput.getText()).toString().trim();
        String notes = Objects.requireNonNull(notesInput.getText()).toString().trim();
        String dateAdministered = dateAdministeredInput.getText().toString().trim();
        String vaccineType = vaccineTypeInput.getText().toString().trim();
        
        return !name.isEmpty() || !notes.isEmpty() || !dateAdministered.isEmpty() || !vaccineType.isEmpty();
    }
    
    private void confirmExit() {
        if (hasUnsavedChanges()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.discard_changes_title)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.discard, (dialog, which) -> finish())
                .setNegativeButton(R.string.cancel, null)
                .show();
        } else {
            finish();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            confirmExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 