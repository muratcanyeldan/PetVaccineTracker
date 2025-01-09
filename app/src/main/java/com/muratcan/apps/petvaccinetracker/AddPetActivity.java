package com.muratcan.apps.petvaccinetracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;
import com.muratcan.apps.petvaccinetracker.util.AnimationUtils;
import com.muratcan.apps.petvaccinetracker.util.ImageUtils;
import com.muratcan.apps.petvaccinetracker.util.RecommendedVaccines;
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AddPetActivity extends AppCompatActivity {
    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private TextInputLayout typeLayout;
    private AutoCompleteTextView typeInput;
    private TextInputLayout breedLayout;
    private TextInputEditText breedInput;
    private TextInputLayout birthDateLayout;
    private TextInputEditText birthDateInput;
    private ShapeableImageView petImageView;
    private MaterialButton addButton;
    private PetViewModel viewModel;
    private DateFormat dateFormat;
    private Uri selectedImageUri;
    private boolean isImageChanged = false;
    private Date selectedDate;
    private Pet existingPet;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri sourceUri = result.getData().getData();
                if (sourceUri != null) {
                    File destinationFile = new File(getCacheDir(), "CroppedImage_" + UUID.randomUUID() + ".jpg");
                    Uri destinationUri = Uri.fromFile(destinationFile);

                    UCrop.Options options = new UCrop.Options();
                    options.setToolbarColor(getColor(R.color.md_theme_light_primary));
                    options.setStatusBarColor(getColor(R.color.md_theme_light_primaryContainer));
                    options.setToolbarWidgetColor(getColor(R.color.md_theme_light_onPrimary));
                    options.setCircleDimmedLayer(true);

                    UCrop.of(sourceUri, destinationUri)
                        .withAspectRatio(1, 1)
                        .withMaxResultSize(1024, 1024)
                        .withOptions(options)
                        .start(this);
                }
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up transitions
        getWindow().setSharedElementEnterTransition(buildContainerTransform(true));
        getWindow().setSharedElementReturnTransition(buildContainerTransform(false));
        findViewById(android.R.id.content).setTransitionName("shared_element_container");
        setEnterSharedElementCallback(new MaterialContainerTransformSharedElementCallback());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        // Get existing pet if editing
        existingPet = getIntent().getParcelableExtra("pet");

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PetViewModel.class);

        // Initialize views and setup UI
        initializeViews();
        setupToolbar();
        setupTypeDropdown();
        setupDatePicker();
        setupImagePicker();
        setupAddButton();
        observeViewModel();

        // Load existing pet data if editing
        if (existingPet != null) {
            loadExistingPetData();
        }
    }

    private MaterialContainerTransform buildContainerTransform(boolean entering) {
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setDrawingViewId(android.R.id.content);
        transform.setDuration(entering ? 300L : 250L);
        transform.setAllContainerColors(getWindow().getStatusBarColor());
        transform.setFadeMode(MaterialContainerTransform.FADE_MODE_THROUGH);
        transform.setPathMotion(null);
        transform.setScrimColor(getResources().getColor(android.R.color.transparent, getTheme()));
        transform.setFitMode(MaterialContainerTransform.FIT_MODE_AUTO);
        return transform;
    }

    private void initializeViews() {
        nameLayout = findViewById(R.id.nameLayout);
        nameInput = findViewById(R.id.nameInput);
        typeLayout = findViewById(R.id.typeLayout);
        typeInput = findViewById(R.id.typeInput);
        breedLayout = findViewById(R.id.breedLayout);
        breedInput = findViewById(R.id.breedInput);
        birthDateLayout = findViewById(R.id.birthDateLayout);
        birthDateInput = findViewById(R.id.birthDateInput);
        petImageView = findViewById(R.id.petImageView);
        addButton = findViewById(R.id.addButton);
        dateFormat = android.text.format.DateFormat.getDateFormat(this);

        // Set the appropriate button text and icon based on whether we're editing or adding
        if (existingPet != null) {
            addButton.setText(R.string.button_update);
            addButton.setIconResource(R.drawable.ic_save);
        } else {
            addButton.setText(R.string.button_add);
            addButton.setIconResource(R.drawable.ic_add);
        }

        // Set up cancel button
        findViewById(R.id.cancelButton).setOnClickListener(v -> confirmExit());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(existingPet != null ? R.string.edit_pet : R.string.add_pet);
        }
    }

    private void setupTypeDropdown() {
        String[] petTypes = getResources().getStringArray(R.array.pet_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown_menu, petTypes);
        typeInput.setAdapter(adapter);
    }

    private void setupDatePicker() {
        birthDateInput.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    now.set(Calendar.YEAR, year);
                    now.set(Calendar.MONTH, month);
                    now.set(Calendar.DAY_OF_MONTH, day);
                    
                    // Validate that selected date is not in the future
                    if (now.getTime().after(new Date())) {
                        birthDateLayout.setError("Birth date cannot be in the future");
                        return;
                    }
                    
                    selectedDate = now.getTime();
                    birthDateInput.setText(dateFormat.format(selectedDate));
                    birthDateLayout.setError(null);
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            );
            
            // Set max date to today
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    private void setupImagePicker() {
        petImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImage.launch(intent);
        });
    }

    private void setupAddButton() {
        addButton.setOnClickListener(v -> validateAndAddPet());
    }

    private void validateAndAddPet() {
        // Get and validate user input with null checks
        String name = Objects.requireNonNullElse(nameInput.getText(), "").toString().trim();
        String type = Objects.requireNonNullElse(typeInput.getText(), "").toString().trim();
        String breed = Objects.requireNonNullElse(breedInput.getText(), "").toString().trim();
        
        // Reset errors
        nameLayout.setError(null);
        typeLayout.setError(null);
        breedLayout.setError(null);
        birthDateLayout.setError(null);

        // Validate inputs
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_enter_pet_name));
            return;
        }
        if (name.length() < 2) {
            nameLayout.setError(getString(R.string.error_name_min_length));
            return;
        }
        if (type.isEmpty()) {
            typeLayout.setError(getString(R.string.error_select_pet_type));
            return;
        }
        if (breed.isEmpty()) {
            breedLayout.setError(getString(R.string.error_enter_pet_breed));
            return;
        }
        if (selectedDate == null) {
            birthDateLayout.setError(getString(R.string.error_select_birth_date));
            return;
        }

        // Get current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Pet petToSave;
        if (existingPet != null) {
            // Update existing pet
            petToSave = existingPet;
            petToSave.setName(name);
            petToSave.setType(type);
            petToSave.setBreed(breed);
            petToSave.setBirthDate(selectedDate);
            if (isImageChanged) {
                petToSave.setImageUri(selectedImageUri != null ? selectedImageUri.toString() : null);
            }
        } else {
            // Create new pet
            petToSave = new Pet(
                name,
                type,
                breed,
                selectedDate,
                isImageChanged ? selectedImageUri : null,
                auth.getCurrentUser().getUid()
            );
        }

        // Save pet
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                if (existingPet != null) {
                    // Update existing pet
                    db.petDao().update(petToSave);
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.pet_updated_successfully, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    // Add new pet
                    long id = db.petDao().insert(petToSave);
                    petToSave.setId(id);
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.pet_added_successfully, Toast.LENGTH_SHORT).show();
                        showRecommendedVaccinesDialog();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    String errorMessage = existingPet != null ? 
                        getString(R.string.error_updating_pet) : 
                        getString(R.string.error_adding_pet);
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    addButton.setEnabled(true);
                });
            }
        }).start();
        
        addButton.setEnabled(false);
        AnimationUtils.pulseAnimation(addButton);
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::updateLoadingState);
        viewModel.getError().observe(this, this::showError);
        viewModel.getSuccess().observe(this, success -> {
            if (success) {
                showRecommendedVaccinesDialog();
            }
        });
    }

    private void updateLoadingState(boolean isLoading) {
        addButton.setEnabled(!isLoading);
        if (isLoading) {
            addButton.setText(existingPet != null ? R.string.button_updating : R.string.button_adding);
        } else {
            addButton.setText(existingPet != null ? R.string.button_update : R.string.button_add);
        }
    }

    private void showError(String error) {
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            addButton.setEnabled(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK && data != null) {
            selectedImageUri = UCrop.getOutput(data);
            if (selectedImageUri != null) {
                ImageUtils.loadImage(this, selectedImageUri, petImageView);
                isImageChanged = true;
                AnimationUtils.pulseAnimation(petImageView);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            confirmExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRecommendedVaccinesDialog() {
        String type = Objects.requireNonNullElse(typeInput.getText(), "").toString().trim();
        new MaterialAlertDialogBuilder(this)
            .setTitle("Add Recommended Vaccines?")
            .setMessage("Would you like to add recommended vaccines for your " + type.toLowerCase() + "?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // Get the newly added pet's ID
                new Thread(() -> {
                    Pet lastAddedPet = AppDatabase.getInstance(this).petDao().getLastAddedPet();
                    if (lastAddedPet != null) {
                        List<Vaccine> recommendedVaccines = RecommendedVaccines.getRecommendedVaccines(type, lastAddedPet.getId());
                        // Add all recommended vaccines
                        AppDatabase.getInstance(this).vaccineDao().insertAll(recommendedVaccines);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Recommended vaccines added", Toast.LENGTH_SHORT).show();
                            supportFinishAfterTransition();
                        });
                    }
                }).start();
            })
            .setNegativeButton("No", (dialog, which) -> {
                Toast.makeText(this, R.string.pet_added_successfully, Toast.LENGTH_SHORT).show();
                supportFinishAfterTransition();
            })
            .setCancelable(false)
            .show();
    }

    private void loadExistingPetData() {
        nameInput.setText(existingPet.getName());
        typeInput.setText(existingPet.getType(), false);
        breedInput.setText(existingPet.getBreed());
        selectedDate = existingPet.getBirthDate();
        birthDateInput.setText(dateFormat.format(selectedDate));
        if (existingPet.getImageUri() != null) {
            selectedImageUri = Uri.parse(existingPet.getImageUri());
            ImageUtils.loadImage(this, selectedImageUri, petImageView);
        }
        // Update both button text and icon for edit mode
        addButton.setText(R.string.button_update);
        addButton.setIconResource(R.drawable.ic_save);
    }

    private boolean hasUnsavedChanges() {
        String name = Objects.requireNonNullElse(nameInput.getText(), "").toString().trim();
        String type = Objects.requireNonNullElse(typeInput.getText(), "").toString().trim();
        String breed = Objects.requireNonNullElse(breedInput.getText(), "").toString().trim();
        if (existingPet == null) {
            // For new pet, check if any field is filled

            return !name.isEmpty() || !type.isEmpty() || !breed.isEmpty() ||
                   selectedDate != null || isImageChanged;
        } else {
            // For existing pet, check if any field is different

            return !existingPet.getName().equals(name) ||
                   !existingPet.getType().equals(type) ||
                   !existingPet.getBreed().equals(breed) ||
                   !existingPet.getBirthDate().equals(selectedDate) ||
                   isImageChanged;
        }
    }

    private void confirmExit() {
        if (hasUnsavedChanges()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.discard_changes_title)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.discard, (dialog, which) -> supportFinishAfterTransition())
                .setNegativeButton(R.string.cancel, null)
                .show();
        } else {
            supportFinishAfterTransition();
        }
    }
} 