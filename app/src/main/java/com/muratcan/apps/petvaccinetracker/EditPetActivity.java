package com.muratcan.apps.petvaccinetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.util.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class EditPetActivity extends AppCompatActivity {
    private TextInputEditText nameInput;
    private AutoCompleteTextView typeInput;
    private TextInputEditText breedInput;
    private TextInputEditText birthDateInput;
    private AppDatabase database;
    private Pet pet;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pet);

        // Initialize views
        ShapeableImageView petImageView = findViewById(R.id.petImageView);
        nameInput = findViewById(R.id.nameInput);
        typeInput = findViewById(R.id.typeInput);
        breedInput = findViewById(R.id.breedInput);
        birthDateInput = findViewById(R.id.birthDateInput);
        ExtendedFloatingActionButton saveFab = findViewById(R.id.saveFab);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set up pet type dropdown
        String[] petTypes = {"Dog", "Cat"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, petTypes);
        typeInput.setAdapter(adapter);

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Get pet from intent
        pet = getIntent().getParcelableExtra("pet");
        if (pet == null) {
            Toast.makeText(this, "Error: Pet not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load pet details
        nameInput.setText(pet.getName());
        typeInput.setText(pet.getType());
        breedInput.setText(pet.getBreed());
        birthDateInput.setText(dateFormat.format(pet.getBirthDate()));
        
        String imageUriString = pet.getImageUri();
        if (imageUriString != null && !imageUriString.isEmpty()) {
            ImageUtils.loadImage(this, imageUriString, petImageView);
        }

        // Set up date picker
        birthDateInput.setOnClickListener(v -> showDatePicker());

        // Set up save button
        saveFab.setOnClickListener(v -> savePet());
    }

    private void showDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            
            // Validate that selected date is not in the future
            if (calendar.getTime().after(new Date())) {
                Toast.makeText(this, "Birth date cannot be in the future", Toast.LENGTH_SHORT).show();
                return;
            }
            
            birthDateInput.setText(dateFormat.format(calendar.getTime()));
        };

        // Create dialog and set max date to today
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

    private void savePet() {
        String name = Objects.requireNonNull(nameInput.getText()).toString().trim();
        String type = typeInput.getText().toString().trim();
        String breed = Objects.requireNonNull(breedInput.getText()).toString().trim();
        String birthDate = Objects.requireNonNull(birthDateInput.getText()).toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Pet name is required");
            return;
        }

        if (type.isEmpty()) {
            typeInput.setError("Pet type is required");
            return;
        }

        try {
            Date birth = birthDate.isEmpty() ? null : dateFormat.parse(birthDate);
            
            pet.setName(name);
            pet.setType(type);
            pet.setBreed(breed);
            if (birth != null) {
                pet.setBirthDate(birth);
            }

            new Thread(() -> {
                database.petDao().update(pet);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Pet updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();

        } catch (Exception e) {
            Toast.makeText(this, "Error: Invalid date format", Toast.LENGTH_SHORT).show();
        }
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