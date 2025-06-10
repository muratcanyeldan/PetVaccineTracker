package com.muratcan.apps.petvaccinetracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.muratcan.apps.petvaccinetracker.adapter.PetAdapter;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper;
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private RecyclerView petRecyclerView;
    private PetAdapter petAdapter;
    private ExtendedFloatingActionButton addPetFab;
    private View emptyView;
    private PetViewModel viewModel;
    private SearchView searchView;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseHelper firebaseHelper;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseHelper = new FirebaseHelper();
        setupPermissions();
        initViews();
        setupRecyclerView();
        setupFab();
        setupSwipeRefresh();

        viewModel = new ViewModelProvider(this).get(PetViewModel.class);
        observeViewModel();
    }

    private void setupPermissions() {
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Timber.d("Notification permission granted");
                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Notifications are disabled. You won't receive vaccine reminders.",
                        Snackbar.LENGTH_LONG
                    ).show();
                }
            }
        );

        // Check and request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void initViews() {
        petRecyclerView = findViewById(R.id.petRecyclerView);
        addPetFab = findViewById(R.id.addPetFab);
        emptyView = findViewById(R.id.emptyView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        // Setup sort button
        ImageButton sortButton = findViewById(R.id.sortButton);
        sortButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, sortButton);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
            
            // Remove the search item from popup menu
            popup.getMenu().removeItem(R.id.action_search);
            
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_sort_name) {
                    Timber.d("Selected sort by name");
                    viewModel.sortByName();
                    return true;
                } else if (id == R.id.action_sort_date) {
                    Timber.d("Selected sort by date");
                    viewModel.sortByDate();
                    return true;
                } else if (id == R.id.action_sort_none) {
                    Timber.d("Selected clear sort");
                    viewModel.clearSort();
                    return true;
                }
                return false;
            });
            popup.show();
        });
        
        // Setup search view
        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.filterPets(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.filterPets(newText);
                return true;
            }
        });
        
        // Setup empty state add button
        View emptyStateButton = findViewById(R.id.emptyStateAddButton);
        if (emptyStateButton != null) {
            emptyStateButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AddPetActivity.class);
                startActivity(intent);
            });
        }

        // Setup logout button
        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            // Show confirmation dialog
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_confirmation)
                .setMessage(R.string.logout_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    try {
                        // Sign out from Firebase
                        firebaseHelper.signOut();
                        Timber.d("User signed out successfully");
                        
                        // Clear all activities and start LoginActivity
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        // Kill the current task to prevent going back
                        finishAndRemoveTask();
                    } catch (Exception e) {
                        Timber.e(e, "Error logging out: %s", e.getMessage());
                        Snackbar.make(v, "Error logging out: " + e.getMessage(), 
                            Snackbar.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshPets();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the pet list when returning to this screen
        viewModel.refreshPets();
    }

    private void setupRecyclerView() {
        petRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        petAdapter = new PetAdapter(new ArrayList<>(), pet -> {
            Intent intent = new Intent(MainActivity.this, PetDetailActivity.class);
            intent.putExtra("pet", pet);
            String transitionName = "pet_card_" + pet.getId();
            View sharedView = petRecyclerView.findViewWithTag(transitionName);
            if (sharedView != null) {
                androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
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

    private void setupFab() {
        addPetFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPetActivity.class);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        // Observe StateFlow values using DefaultLifecycleObserver
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(LifecycleOwner owner) {
                // Observe pets using LiveData
                viewModel.getPetsLiveData().observe(MainActivity.this, MainActivity.this::updatePetList);
                
                // Observe loading state using LiveData
                viewModel.getIsLoadingLiveData().observe(MainActivity.this, MainActivity.this::updateLoadingState);
                
                // Observe error state using LiveData
                viewModel.getErrorLiveData().observe(MainActivity.this, MainActivity.this::showError);
            }
        });
    }

    private void updatePetList(List<Pet> pets) {
        if (pets == null) return;
        
        petAdapter.updatePets(pets);
        updateEmptyView(pets.isEmpty());
    }

    private void updateLoadingState(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            // Show loading indicator
            swipeRefreshLayout.setRefreshing(true);
            if (petAdapter.getItemCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
                petRecyclerView.setVisibility(View.GONE);
                addPetFab.hide();
            }
        } else {
            // Update visibility based on whether we have pets
            swipeRefreshLayout.setRefreshing(false);
            updateEmptyView(petAdapter.getItemCount() == 0);
        }
    }

    private void showError(String error) {
        if (error != null && !error.isEmpty()) {
            Snackbar.make(petRecyclerView, error, Snackbar.LENGTH_LONG).show();
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            petRecyclerView.setVisibility(View.GONE);
            addPetFab.hide();
            
            // Make sure the empty state button is visible
            View emptyStateButton = findViewById(R.id.emptyStateAddButton);
            if (emptyStateButton != null) {
                emptyStateButton.setVisibility(View.VISIBLE);
            }
            
            ((TextView) emptyView.findViewById(R.id.emptyTextView))
                .setText(R.string.no_pets_found);
        } else {
            emptyView.setVisibility(View.GONE);
            petRecyclerView.setVisibility(View.VISIBLE);
            addPetFab.show();
            addPetFab.extend();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
} 