package com.muratcan.apps.petvaccinetracker;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.muratcan.apps.petvaccinetracker.adapter.VaccineAdapter;
import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;
import com.muratcan.apps.petvaccinetracker.util.AnimationUtils;
import com.muratcan.apps.petvaccinetracker.util.ImageUtils;
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class PetDetailActivity extends AppCompatActivity implements VaccineAdapter.OnVaccineClickListener {
    private Pet pet;
    private ImageView petImageView;
    private TextView petNameTextView;
    private com.google.android.material.chip.Chip petTypeChip;
    private com.google.android.material.chip.Chip petBreedChip;
    private TextView petBirthDateTextView;
    private RecyclerView vaccineRecyclerView;
    private VaccineAdapter vaccineAdapter;
    private ExtendedFloatingActionButton addVaccineFab;
    private View emptyView;
    private PetViewModel viewModel;
    private DateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get pet from intent first
        pet = getIntent().getParcelableExtra("pet");
        if (pet == null) {
            finish();
            return;
        }
        
        // Handle back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                supportFinishAfterTransition();
            }
        });
        
        setContentView(R.layout.activity_pet_detail);
        
        // Get the transition name from the extra and set it on root view
        String transitionName = "pet_card_" + pet.getId();
        View rootView = findViewById(R.id.root_view);
        rootView.setTransitionName(transitionName);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PetViewModel.class);

        // Initialize views
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupAddVaccineButton();

        // Load data
        loadPetDetails();
        observeViewModel();
    }

    private void initializeViews() {
        petImageView = findViewById(R.id.petImageView);
        petNameTextView = findViewById(R.id.petNameTextView);
        petTypeChip = findViewById(R.id.petTypeChip);
        petBreedChip = findViewById(R.id.petBreedChip);
        petBirthDateTextView = findViewById(R.id.petBirthDateTextView);
        vaccineRecyclerView = findViewById(R.id.vaccineRecyclerView);
        addVaccineFab = findViewById(R.id.addVaccineFab);
        emptyView = findViewById(R.id.emptyView);
        dateFormat = android.text.format.DateFormat.getDateFormat(this);

        // Show empty view by default and hide recycler view
        vaccineRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setAlpha(1f);
        addVaccineFab.hide();

        // Set up empty state add button
        findViewById(R.id.emptyStateAddVaccineButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddVaccineActivity.class);
            intent.putExtra("pet_id", pet.getId());
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                v,
                "shared_element_container"
            );
            startActivity(intent, options.toBundle());
        });

        // Set up FAB behavior
        vaccineRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    addVaccineFab.shrink();
                } else if (dy < 0) {
                    addVaccineFab.extend();
                }
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(pet.getName());

        findViewById(R.id.backButton).setOnClickListener(v -> 
            getOnBackPressedDispatcher().onBackPressed());

        findViewById(R.id.editButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPetActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });

        findViewById(R.id.deleteButton).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_pet_confirmation)
                .setMessage(R.string.delete_pet_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Delete pet
                    new Thread(() -> {
                        AppDatabase db = AppDatabase.getInstance(this);
                        // First delete all vaccines for this pet
                        db.vaccineDao().deleteAllForPet(pet.getId());
                        // Then delete the pet
                        db.petDao().delete(pet);
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.pet_deleted, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        });
    }

    private void setupRecyclerView() {
        vaccineRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        vaccineAdapter = new VaccineAdapter(new ArrayList<>(), this);
        vaccineRecyclerView.setAdapter(vaccineAdapter);
    }

    private void setupAddVaccineButton() {
        addVaccineFab.setOnClickListener(v -> {
            Intent intent = new Intent(PetDetailActivity.this, AddVaccineActivity.class);
            intent.putExtra("pet_id", pet.getId());
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                addVaccineFab,
                "shared_element_container"
            );
            startActivity(intent, options.toBundle());
        });
    }

    private void loadPetDetails() {
        // Load pet image
        if (pet.getImageUri() != null) {
            ImageUtils.loadImage(this, pet.getImageUri(), petImageView);
        }

        // Set pet details
        petNameTextView.setText(pet.getName());
        petTypeChip.setText(pet.getType());
        petBreedChip.setText(pet.getBreed());

        // Calculate and set age
        com.google.android.material.chip.Chip petAgeChip = findViewById(R.id.petAgeChip);
        if (petAgeChip != null && pet.getBirthDate() != null) {
            // Calculate age
            long ageInMillis = System.currentTimeMillis() - pet.getBirthDate().getTime();
            int ageInYears = (int) (ageInMillis / (1000L * 60 * 60 * 24 * 365));
            int ageInMonths = (int) (ageInMillis / (1000L * 60 * 60 * 24 * 30)) % 12;

            String ageText;
            if (ageInYears > 0) {
                ageText = getResources().getQuantityString(R.plurals.years_old, ageInYears, ageInYears);
                if (ageInMonths > 0) {
                    ageText += " " + getResources().getQuantityString(R.plurals.months_old, ageInMonths, ageInMonths);
                }
            } else {
                ageText = getResources().getQuantityString(R.plurals.months_old, ageInMonths, ageInMonths);
            }
            petAgeChip.setText(ageText);
        }

        // Set birth date
        if (pet.getBirthDate() != null) {
            petBirthDateTextView.setText(getString(R.string.born_on_date, dateFormat.format(pet.getBirthDate())));
        }

        // Start loading vaccines
        viewModel.loadVaccines(pet.getId());
    }

    private void observeViewModel() {
        // Remove duplicate loadVaccines call
        viewModel.getVaccines().observe(this, this::updateVaccineList);
        viewModel.getIsLoading().observe(this, this::updateLoadingState);
        viewModel.getError().observe(this, this::showError);
        viewModel.getCurrentPet().observe(this, this::updatePetDetails);
    }

    private void updateVaccineList(List<Vaccine> vaccines) {
        if (vaccines == null) return;
        
        vaccineAdapter.updateVaccines(vaccines);
        updateEmptyView(vaccines.isEmpty());
    }

    private void updateLoadingState(boolean isLoading) {
        if (isLoading) {
            // Keep empty view visible while loading
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setAlpha(1f);
            vaccineRecyclerView.setVisibility(View.GONE);
            addVaccineFab.hide();
        } else {
            // Get current vaccines and update views
            List<Vaccine> currentVaccines = vaccineAdapter.getVaccines();
            updateEmptyView(currentVaccines == null || currentVaccines.isEmpty());
        }
    }

    private void showError(String error) {
        if (error != null) {
            Snackbar.make(vaccineRecyclerView, error, Snackbar.LENGTH_LONG)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .setAction("Retry", v -> viewModel.loadVaccines(pet.getId()))
                .show();
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            // Show empty view with animation
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setAlpha(1f);
            vaccineRecyclerView.setVisibility(View.GONE);
            addVaccineFab.hide();
        } else {
            // Show recycler view with animation
            vaccineRecyclerView.setAlpha(0f);
            vaccineRecyclerView.setVisibility(View.VISIBLE);
            vaccineRecyclerView.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
            emptyView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> emptyView.setVisibility(View.GONE))
                .start();
            addVaccineFab.show();
            addVaccineFab.extend();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Force a refresh of the vaccines
        viewModel.refreshVaccines(pet.getId());
        viewModel.loadPetById(pet.getId());
    }

    @Override
    public void onVaccineClick(Vaccine vaccine) {
        Intent intent = new Intent(this, AddVaccineActivity.class);
        intent.putExtra("vaccine", vaccine);
        intent.putExtra("pet_id", pet.getId());
        startActivity(intent);
    }

    @Override
    public void onVaccineDelete(Vaccine vaccine) {
        new Thread(() -> {
            AppDatabase.getInstance(this).vaccineDao().delete(vaccine);
            runOnUiThread(() -> {
                Snackbar.make(vaccineRecyclerView, R.string.vaccine_deleted, Snackbar.LENGTH_SHORT).show();
                viewModel.refreshVaccines(pet.getId());
            });
        }).start();
    }

    private void updatePetDetails(Pet updatedPet) {
        if (updatedPet != null) {
            pet = updatedPet;
            // Update UI elements
            if (pet.getImageUri() != null) {
                ImageUtils.loadImage(this, pet.getImageUri(), petImageView);
            }
            petNameTextView.setText(pet.getName());
            petTypeChip.setText(pet.getType());
            petBreedChip.setText(pet.getBreed());
            
            // Update birth date and age
            if (pet.getBirthDate() != null) {
                petBirthDateTextView.setText(getString(R.string.born_on_date, dateFormat.format(pet.getBirthDate())));
                
                // Update age chip
                com.google.android.material.chip.Chip petAgeChip = findViewById(R.id.petAgeChip);
                if (petAgeChip != null) {
                    long ageInMillis = System.currentTimeMillis() - pet.getBirthDate().getTime();
                    int ageInYears = (int) (ageInMillis / (1000L * 60 * 60 * 24 * 365));
                    int ageInMonths = (int) (ageInMillis / (1000L * 60 * 60 * 24 * 30)) % 12;

                    String ageText;
                    if (ageInYears > 0) {
                        ageText = getResources().getQuantityString(R.plurals.years_old, ageInYears, ageInYears);
                        if (ageInMonths > 0) {
                            ageText += " " + getResources().getQuantityString(R.plurals.months_old, ageInMonths, ageInMonths);
                        }
                    } else {
                        ageText = getResources().getQuantityString(R.plurals.months_old, ageInMonths, ageInMonths);
                    }
                    petAgeChip.setText(ageText);
                }
            }
        }
    }
} 