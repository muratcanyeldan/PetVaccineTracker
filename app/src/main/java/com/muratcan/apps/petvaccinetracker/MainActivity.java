package com.muratcan.apps.petvaccinetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Pets");
        }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        } else {
            emptyView.setVisibility(View.GONE);
            petRecyclerView.setVisibility(View.VISIBLE);
            petAdapter.updatePets(pets);
        }
    }

    private void updateLoadingState(boolean isLoading) {
        swipeRefreshLayout.setRefreshing(isLoading);
        if (isLoading) {
            addPetFab.hide();
        } else {
            addPetFab.show();
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