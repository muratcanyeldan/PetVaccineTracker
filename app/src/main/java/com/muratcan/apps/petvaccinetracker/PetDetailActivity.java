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
    private TextView petTypeTextView;
    private TextView petBreedTextView;
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
        petTypeTextView = findViewById(R.id.petTypeTextView);
        petBreedTextView = findViewById(R.id.petBreedTextView);
        petBirthDateTextView = findViewById(R.id.petBirthDateTextView);
        vaccineRecyclerView = findViewById(R.id.vaccineRecyclerView);
        addVaccineFab = findViewById(R.id.addVaccineFab);
        emptyView = findViewById(R.id.emptyView);
        dateFormat = android.text.format.DateFormat.getDateFormat(this);

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
        petTypeTextView.setText(pet.getType());
        petBreedTextView.setText(pet.getBreed());
        petBirthDateTextView.setText(dateFormat.format(pet.getBirthDate()));

        // Load vaccines
        viewModel.loadVaccines(pet.getId());
    }

    private void observeViewModel() {
        viewModel.loadVaccines(pet.getId());
        viewModel.getVaccines().observe(this, this::updateVaccineList);
        viewModel.getIsLoading().observe(this, this::updateLoadingState);
        viewModel.getError().observe(this, this::showError);
        viewModel.getCurrentPet().observe(this, this::updatePetDetails);
    }

    private void updateVaccineList(List<Vaccine> vaccines) {
        vaccineAdapter.updateVaccines(vaccines);
        updateEmptyView(vaccines.isEmpty());

        // Show FAB with animation if list is not empty
        if (!vaccines.isEmpty()) {
            addVaccineFab.extend();
        }
    }

    private void updateLoadingState(boolean isLoading) {
        if (isLoading) {
            addVaccineFab.hide();
        } else {
            addVaccineFab.show();
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
            AnimationUtils.crossFadeViews(emptyView, vaccineRecyclerView);
            addVaccineFab.extend();
        } else {
            AnimationUtils.crossFadeViews(vaccineRecyclerView, emptyView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh pet data from database
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
            loadPetDetails();
        }
    }
} 