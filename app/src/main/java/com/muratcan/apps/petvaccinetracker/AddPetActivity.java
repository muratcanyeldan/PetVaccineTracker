package com.muratcan.apps.petvaccinetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper;
import com.muratcan.apps.petvaccinetracker.util.Logger;
import com.muratcan.apps.petvaccinetracker.util.NotificationHelper;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddPetActivity extends AppCompatActivity {
    private static final String TAG = "AddPetActivity";
    private TextInputEditText petNameEditText;
    private MaterialAutoCompleteTextView petTypeSpinner;
    private TextInputEditText petBreedEditText;
    private TextInputEditText petDobEditText;
    private SwitchMaterial addRecommendedVaccinesCheckbox;
    private MaterialButton addPetButton;
    private AppDatabase database;
    private ExecutorService executorService;
    private FirebaseHelper firebaseHelper;
    private DateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        firebaseHelper = new FirebaseHelper();
        dateFormat = android.text.format.DateFormat.getDateFormat(this);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide default title
            }
        }

        initializeViews();
        setupSpinner();
        setupDatePicker();
        setupAddButton();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close the activity when back button is pressed
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        petNameEditText = findViewById(R.id.petNameEditText);
        petTypeSpinner = findViewById(R.id.petTypeSpinner);
        petBreedEditText = findViewById(R.id.petBreedEditText);
        petDobEditText = findViewById(R.id.petDobEditText);
        addRecommendedVaccinesCheckbox = findViewById(R.id.addRecommendedVaccinesCheckbox);
        addPetButton = findViewById(R.id.addPetButton);
    }

    private void setupSpinner() {
        String[] petTypes = getResources().getStringArray(R.array.pet_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            R.layout.item_dropdown_menu,
            petTypes
        );
        petTypeSpinner.setAdapter(adapter);
        
        petTypeSpinner.setFocusable(false);
        petTypeSpinner.setFocusableInTouchMode(false);
        
        if (petTypes.length > 0) {
            petTypeSpinner.setText(petTypes[0], false);
        }
        
        petTypeSpinner.setOnClickListener(v -> petTypeSpinner.showDropDown());
    }

    private void setupDatePicker() {
        petDobEditText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddPetActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Check if selected date is in the future
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    
                    if (selectedDate.after(Calendar.getInstance())) {
                        Toast.makeText(AddPetActivity.this, 
                            "Birth date cannot be in the future", 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", 
                        selectedDay, selectedMonth + 1, selectedYear);
                    petDobEditText.setText(date);
                },
                year, month, day
            );

            // Set the maximum date to today
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void setupAddButton() {
        addPetButton.setOnClickListener(v -> {
            String name = petNameEditText.getText() != null ? petNameEditText.getText().toString().trim() : "";
            String type = petTypeSpinner.getText() != null ? petTypeSpinner.getText().toString().trim() : "";
            String breed = petBreedEditText.getText() != null ? petBreedEditText.getText().toString().trim() : "";
            String dob = petDobEditText.getText() != null ? petDobEditText.getText().toString().trim() : "";

            if (name.isEmpty()) {
                petNameEditText.setError("Please enter pet's name");
                return;
            }

            if (type.isEmpty()) {
                Toast.makeText(this, "Please select a pet type", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = firebaseHelper.getCurrentUserId();
            if (userId == null) {
                Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            Pet pet = new Pet(name, type, breed, dob, userId);
            
            executorService.execute(() -> {
                try {
                    long petId = database.petDao().insert(pet);
                    pet.setId(petId);
                    
                    if (addRecommendedVaccinesCheckbox.isChecked()) {
                        addRecommendedVaccines(pet);
                    }
                    
                    runOnUiThread(() -> {
                        String message = addRecommendedVaccinesCheckbox.isChecked() 
                            ? "Pet added successfully with recommended vaccines"
                            : "Pet added successfully";
                        Toast.makeText(AddPetActivity.this, message, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } catch (Exception e) {
                    String errorMessage = e.getMessage();
                    runOnUiThread(() -> Toast.makeText(AddPetActivity.this, 
                        "Error adding pet: " + (errorMessage != null ? errorMessage : "Unknown error"), 
                        Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private void addInitialVaccine(Pet pet, String name, int months, String notes) {
        try {
            Calendar dueDate = Calendar.getInstance();
            // For initial recommended vaccines, we set them as not administered yet
            // and calculate the due date based on pet's age
            Date petBirthDate = dateFormat.parse(pet.getDateOfBirth());
            if (petBirthDate != null) {
                dueDate.setTime(petBirthDate);
                dueDate.add(Calendar.MONTH, months);
            }
            
            String dueDateStr = dateFormat.format(dueDate.getTime());

            // All recommended vaccines are recurring by nature
            Vaccine vaccine = new Vaccine(name, pet.getId(), null, dueDateStr, true, months, notes);
            
            database.vaccineDao().insert(vaccine);

            NotificationHelper.scheduleNotification(
                this,
                pet.getName(),
                name,
                dueDateStr
            );

            Logger.info(TAG, String.format(Locale.getDefault(),
                "Added recommended vaccine: %s, due: %s", name, dueDateStr));
        } catch (Exception e) {
            Logger.error(TAG, "Error adding initial vaccine: " + name, e);
        }
    }

    private void addRecommendedVaccines(Pet pet) {
        if ("Dog".equals(pet.getType())) {
            addInitialVaccine(pet, "Distemper", 2, "Core vaccine for dogs");
            addInitialVaccine(pet, "Parvovirus", 2, "Core vaccine for dogs");
            addInitialVaccine(pet, "Rabies", 3, "Required by law");
            addInitialVaccine(pet, "Bordetella", 6, "Recommended for social dogs");
        } else if ("Cat".equals(pet.getType())) {
            addInitialVaccine(pet, "FVRCP", 2, "Core vaccine for cats");
            addInitialVaccine(pet, "Rabies", 3, "Required by law");
            addInitialVaccine(pet, "FeLV", 6, "Recommended for outdoor cats");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
} 