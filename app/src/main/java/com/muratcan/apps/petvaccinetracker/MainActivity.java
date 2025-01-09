package com.muratcan.apps.petvaccinetracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.muratcan.apps.petvaccinetracker.adapter.PetAdapter;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper;
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView petRecyclerView;
    private PetAdapter petAdapter;
    private View emptyView;
    private ExtendedFloatingActionButton addPetFab;
    private PetViewModel viewModel;
    private FirebaseHelper firebaseHelper;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set up toolbar buttons
        setupToolbarButtons();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PetViewModel.class);

        // Initialize views
        initializeViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Observe ViewModel
        observeViewModel();

        // Load pets
        viewModel.loadPets();
    }

    private void setupToolbarButtons() {
        // Set up search
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.filterPets(newText);
                return true;
            }
        });

        // Set up sort button
        ImageButton sortButton = findViewById(R.id.sortButton);
        sortButton.setOnClickListener(v -> showSortDialog());

        // Set up logout button
        ImageButton logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> logout());
    }

    private void showSortDialog() {
        String[] sortOptions = {"Sort by Name", "Sort by Date"};
        new MaterialAlertDialogBuilder(this)
            .setTitle("Sort Pets")
            .setItems(sortOptions, (dialog, which) -> {
                if (which == 0) {
                    viewModel.sortByName();
                } else {
                    viewModel.sortByDate();
                }
            })
            .show();
    }

    private void logout() {
        firebaseHelper.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initializeViews() {
        petRecyclerView = findViewById(R.id.petRecyclerView);
        addPetFab = findViewById(R.id.addPetFab);
        emptyView = findViewById(R.id.emptyView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Initially hide FAB since we don't know if we have pets yet
        addPetFab.hide();

        // Set up empty state add button
        findViewById(R.id.emptyStateAddButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPetActivity.class);
            startActivity(intent);
        });

        addPetFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPetActivity.class);
            startActivity(intent);
        });

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.loadPets());
    }

    private void setupRecyclerView() {
        petRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        petAdapter = new PetAdapter(new ArrayList<>(), pet -> {
            Intent intent = new Intent(MainActivity.this, PetDetailActivity.class);
            intent.putExtra("pet", pet);
            String transitionName = "pet_card_" + pet.getId();
            View sharedView = petRecyclerView.findViewWithTag(transitionName);
            if (sharedView != null) {
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    MainActivity.this,
                    sharedView,
                    transitionName
                );
                startActivity(intent, options.toBundle());
            } else {
                startActivity(intent);
            }
        });
        petRecyclerView.setAdapter(petAdapter);
    }

    private void observeViewModel() {
        viewModel.getPets().observe(this, this::updatePetList);
        viewModel.getIsLoading().observe(this, this::updateLoadingState);
        viewModel.getError().observe(this, this::showError);
    }

    private void updatePetList(List<Pet> pets) {
        if (pets == null || pets.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            petRecyclerView.setVisibility(View.GONE);
            addPetFab.hide();
        } else {
            emptyView.setVisibility(View.GONE);
            petRecyclerView.setVisibility(View.VISIBLE);
            addPetFab.show();
            addPetFab.extend();
            petAdapter.updatePets(pets);
        }
    }

    private void updateLoadingState(boolean isLoading) {
        swipeRefreshLayout.setRefreshing(isLoading);
        if (isLoading) {
            addPetFab.hide();
        } else {
            // Only show FAB if we're not in empty state
            if (petRecyclerView.getVisibility() == View.VISIBLE) {
                addPetFab.show();
                addPetFab.extend();
            }
        }
    }

    private void showError(String error) {
        if (error != null) {
            Snackbar.make(petRecyclerView, error, Snackbar.LENGTH_LONG)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .setAction("Retry", v -> viewModel.loadPets())
                .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadPets();
    }
} 