package com.muratcan.apps.petvaccinetracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.util.ImageUtils;
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel;
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import timber.log.Timber;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AddPetActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
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
    private FirebaseHelper firebaseHelper;
    private Uri photoUri;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                handleImageResult(result.getData().getData());
            }
        }
    );

    private final ActivityResultLauncher<Intent> takePhoto = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK) {
                handleImageResult(photoUri);
            }
        }
    );

    private void handleImageResult(Uri sourceUri) {
        if (sourceUri != null) {
            try {
                // Show preview immediately
                selectedImageUri = sourceUri; // Store the original URI for immediate preview
                
                // Reset image view styling for actual image
                petImageView.setBackgroundResource(0);
                petImageView.setPadding(0, 0, 0, 0);
                petImageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                petImageView.setColorFilter(null);
                
                // Load the image immediately for preview
                ImageUtils.loadImage(this, selectedImageUri, petImageView);
                Timber.d("Showing preview of image: %s", selectedImageUri);
                
                // Copy the image to app storage in the background
                new Thread(() -> {
                    try {
                        String storedImagePath = ImageUtils.copyImageToAppStorage(this, sourceUri);
                        if (storedImagePath != null) {
                            Uri storedUri = ImageUtils.getImageUri(this, storedImagePath);
                            if (storedUri != null) {
                                runOnUiThread(() -> {
                                    selectedImageUri = storedUri; // Update the URI to the stored version
                                    Timber.d("Image stored successfully at: %s", storedUri);
                                });
                            } else {
                                Timber.e("Failed to get URI for stored image path: %s", storedImagePath);
                                runOnUiThread(() -> {
                                    Snackbar.make(petImageView, R.string.error_saving_image, Snackbar.LENGTH_LONG).show();
                                });
                            }
                        } else {
                            Timber.e("Failed to copy image to app storage");
                            runOnUiThread(() -> {
                                Snackbar.make(petImageView, R.string.error_saving_image, Snackbar.LENGTH_LONG).show();
                            });
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Error storing image: %s", e.getMessage());
                        runOnUiThread(() -> {
                            Snackbar.make(petImageView, R.string.error_saving_image, Snackbar.LENGTH_LONG).show();
                        });
                    }
                }).start();
                
                isImageChanged = true;
            } catch (Exception e) {
                Timber.e(e, "Error processing selected image: %s", e.getMessage());
                Snackbar.make(petImageView, R.string.error_processing_image, Snackbar.LENGTH_LONG).show();
                resetToPlaceholder();
            }
        }
    }

    private void resetToPlaceholder() {
        petImageView.setBackgroundResource(R.color.md_theme_light_surfaceVariant);
        petImageView.setPadding(32, 32, 32, 32);
        petImageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        petImageView.setImageResource(R.drawable.ic_pet_placeholder);
        petImageView.setColorFilter(getResources().getColor(R.color.md_theme_light_outline, getTheme()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        Timber.d("AddPetActivity onCreate");
        
        firebaseHelper = new FirebaseHelper();
        initViews();
        setupToolbar();
        setupTypeDropdown();
        setupDatePicker();
        setupImagePicker();

        viewModel = new ViewModelProvider(this).get(PetViewModel.class);
        observeViewModel();

        // Check if we're editing an existing pet
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("pet")) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    existingPet = intent.getParcelableExtra("pet", Pet.class);
                } else {
                    existingPet = intent.getParcelableExtra("pet");
                }
                Timber.d("Received pet for editing: %s", existingPet != null ? existingPet.getName() : "null");
                if (existingPet != null) {
                    populateExistingPetData();
                } else {
                    Timber.e("Failed to get pet from intent");
                    showError(getString(R.string.error_pet_not_found));
                    finish();
                }
            } catch (Exception e) {
                Timber.e(e, "Error getting pet from intent");
                showError(getString(R.string.error_pet_not_found));
                finish();
            }
        } else {
            Timber.d("Creating new pet");
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
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

        dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

        addButton.setOnClickListener(v -> savePet());
        
        // Setup cancel button
        MaterialButton cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> finish());
    }

    private void setupTypeDropdown() {
        String[] petTypes = getResources().getStringArray(R.array.pet_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, petTypes);
        typeInput.setAdapter(adapter);
    }

    private void setupDatePicker() {
        birthDateInput.setOnClickListener(v -> showDatePicker());
        birthDateInput.setFocusable(false);
    }

    private void setupImagePicker() {
        petImageView.setOnClickListener(v -> checkAndRequestPermissions(true));
        FloatingActionButton changeImageButton = findViewById(R.id.changeImageButton);
        changeImageButton.setOnClickListener(v -> checkAndRequestPermissions(false));
    }

    private void checkAndRequestPermissions(boolean isGallery) {
        if (isGallery) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
                } else {
                    launchImagePicker();
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                } else {
                    launchImagePicker();
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
            } else {
                launchCamera();
            }
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImage.launch(intent);
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                File storageDir = new File(getFilesDir(), ImageUtils.IMAGES_DIR);
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }
                Timber.d("Storage directory created at: %s", storageDir.getAbsolutePath());
                
                String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
                photoFile = new File(storageDir, "JPEG_" + timeStamp + ".jpg");
                Timber.d("Photo file created at: %s", photoFile.getAbsolutePath());
            } catch (Exception e) {
                Timber.e(e, "Error creating image file");
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    photoUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile
                    );
                    Timber.d("FileProvider URI created: %s", photoUri);
                    
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    // Grant URI permissions
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    takePhoto.launch(intent);
                } catch (Exception e) {
                    Timber.e(e, "Error getting URI for photo file: %s", e.getMessage());
                    Snackbar.make(petImageView, R.string.error_processing_image, Snackbar.LENGTH_LONG).show();
                }
            }
        } else {
            Timber.w("No camera app available");
            Snackbar.make(petImageView, R.string.error_no_camera, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permissions[0].equals(Manifest.permission.CAMERA)) {
                    launchCamera();
                } else {
                    launchImagePicker();
                }
            } else {
                Snackbar.make(petImageView, R.string.permission_denied_message,
                    Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTime(selectedDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.set(year, month, dayOfMonth);
                
                // Check if selected date is in the future
                if (selectedCal.after(Calendar.getInstance())) {
                    Snackbar.make(birthDateInput, R.string.future_date_error, Snackbar.LENGTH_LONG).show();
                    return;
                }
                
                selectedDate = selectedCal.getTime();
                birthDateInput.setText(dateFormat.format(selectedDate));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set the maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        
        datePickerDialog.show();
    }

    private void populateExistingPetData() {
        if (existingPet == null) {
            Timber.e("Existing pet is null in edit mode");
            showError(getString(R.string.error_pet_not_found));
            finish();
            return;
        }

        Timber.d("Populating existing pet data: id=%d, name=%s", existingPet.getId(), existingPet.getName());
        
        nameInput.setText(existingPet.getName());
        typeInput.setText(existingPet.getType());
        breedInput.setText(existingPet.getBreed());
        
        if (existingPet.getBirthDate() != null) {
            selectedDate = existingPet.getBirthDate();
            birthDateInput.setText(dateFormat.format(selectedDate));
        } else {
            Timber.w("Birth date is null for existing pet");
        }
        
        if (existingPet.getImageUri() != null && !existingPet.getImageUri().isEmpty()) {
            try {
                selectedImageUri = android.net.Uri.parse(existingPet.getImageUri());
                // Reset image view styling for actual image
                petImageView.setBackgroundResource(0); // Remove background
                petImageView.setPadding(0, 0, 0, 0);
                petImageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                petImageView.setColorFilter(null);
                
                ImageUtils.loadImage(this, selectedImageUri, petImageView);
                Timber.d("Loaded image URI in edit mode: %s", selectedImageUri);
            } catch (Exception e) {
                Timber.e(e, "Error loading pet image in edit mode: %s", e.getMessage());
                resetToPlaceholder();
            }
        } else {
            Timber.d("No image URI for existing pet");
            resetToPlaceholder();
        }

        addButton.setText(R.string.update_pet);
    }

    private void observeViewModel() {
        // Observe StateFlow values using DefaultLifecycleObserver
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(LifecycleOwner owner) {
                // Observe loading state using LiveData
                viewModel.getIsLoadingLiveData().observe(AddPetActivity.this, AddPetActivity.this::updateLoadingState);
                
                // Observe error state using LiveData
                viewModel.getErrorLiveData().observe(AddPetActivity.this, AddPetActivity.this::showError);
            }
        });
    }

    private void updateLoadingState(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            addButton.setEnabled(false);
            addButton.setText(R.string.saving);
        } else {
            addButton.setEnabled(true);
            addButton.setText(existingPet != null ? R.string.update_pet : R.string.add_pet);
        }
    }

    private void showError(String error) {
        if (error != null && !error.isEmpty()) {
            Snackbar.make(addButton, error, Snackbar.LENGTH_LONG).show();
        }
    }

    private void savePet() {
        if (!validateInputs()) {
            Timber.d("Input validation failed");
            return;
        }

        String name = nameInput.getText().toString().trim();
        String type = typeInput.getText().toString().trim();
        String breed = breedInput.getText().toString().trim();

        if (existingPet != null) {
            // Update existing pet
            Timber.d("Updating existing pet with ID: " + existingPet.getId());
            existingPet.setName(name);
            existingPet.setType(type);
            existingPet.setBreed(breed);
            existingPet.setBirthDate(selectedDate);
            
            if (isImageChanged && selectedImageUri != null) {
                // Delete old image if it exists
                if (existingPet.getImageUri() != null && !existingPet.getImageUri().isEmpty()) {
                    ImageUtils.deleteImage(this, existingPet.getImageUri());
                }
                // Set the new image URI directly
                existingPet.setImageUri(selectedImageUri.toString());
                Timber.d("Setting new image URI: %s", selectedImageUri.toString());
            }
            
            viewModel.updatePet(existingPet);
            finish();
        } else {
            // Create new pet
            Timber.d("Creating new pet with name: " + name);
            Pet newPet = new Pet();
            newPet.setName(name);
            newPet.setType(type);
            newPet.setBreed(breed);
            newPet.setBirthDate(selectedDate);
            
            String userId = firebaseHelper.getCurrentUserId();
            if (userId == null) {
                showError("Error: User not logged in");
                return;
            }
            newPet.setUserId(userId);
            
            if (selectedImageUri != null) {
                newPet.setImageUri(selectedImageUri.toString());
                Timber.d("Setting image URI for new pet: %s", selectedImageUri.toString());
            }
            
            viewModel.addPet(newPet);
            
            // Only finish if there's no error
            viewModel.getErrorLiveData().observe(this, error -> {
                if (error == null) {
                    finish();
                }
            });
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (TextUtils.isEmpty(nameInput.getText())) {
            nameLayout.setError(getString(R.string.required_field));
            Timber.w("Name field is empty");
            isValid = false;
        } else {
            nameLayout.setError(null);
        }

        if (TextUtils.isEmpty(typeInput.getText())) {
            typeLayout.setError(getString(R.string.required_field));
            Timber.w("Type field is empty");
            isValid = false;
        } else {
            typeLayout.setError(null);
        }

        if (TextUtils.isEmpty(breedInput.getText())) {
            breedLayout.setError(getString(R.string.required_field));
            Timber.w("Breed field is empty");
            isValid = false;
        } else {
            breedLayout.setError(null);
        }

        if (selectedDate == null) {
            birthDateLayout.setError(getString(R.string.required_field));
            Timber.w("Birth date is not selected");
            isValid = false;
        } else {
            birthDateLayout.setError(null);
        }

        return isValid;
    }
} 