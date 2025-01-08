package com.muratcan.apps.petvaccinetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
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
    private Spinner petTypeSpinner;
    private TextInputEditText petBreedEditText;
    private TextInputEditText petDobEditText;
    private MaterialCheckBox addRecommendedVaccinesCheckbox;
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

        initializeViews();
        setupSpinner();
        setupDatePicker();
        setupAddButton();
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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this,
            R.array.pet_types,
            android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        petTypeSpinner.setAdapter(adapter);
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
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    petDobEditText.setText(date);
                },
                year, month, day
            );
            datePickerDialog.show();
        });
    }

    private void setupAddButton() {
        addPetButton.setOnClickListener(v -> {
            CharSequence nameSeq = petNameEditText.getText();
            String name = nameSeq != null ? nameSeq.toString().trim() : "";

            Object typeObj = petTypeSpinner.getSelectedItem();
            String type = typeObj != null ? typeObj.toString() : "";

            CharSequence breedSeq = petBreedEditText.getText();
            String breed = breedSeq != null ? breedSeq.toString().trim() : "";

            CharSequence dobSeq = petDobEditText.getText();
            String dob = dobSeq != null ? dobSeq.toString().trim() : "";

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