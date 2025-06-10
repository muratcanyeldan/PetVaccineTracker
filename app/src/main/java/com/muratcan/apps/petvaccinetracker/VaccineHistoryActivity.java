package com.muratcan.apps.petvaccinetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.muratcan.apps.petvaccinetracker.adapter.VaccineHistoryAdapter;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.VaccineHistoryItem;
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel;

import java.util.ArrayList;
import java.util.List;

public class VaccineHistoryActivity extends AppCompatActivity implements VaccineHistoryAdapter.OnHistoryItemClickListener {
    private RecyclerView historyRecyclerView;
    private VaccineHistoryAdapter historyAdapter;
    private TextView emptyView;
    private Spinner petFilterSpinner;
    private PetViewModel viewModel;
    private List<Pet> allPets;
    private long selectedPetId = -1; // -1 means all pets

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vaccine_history);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSpinner();

        viewModel = new ViewModelProvider(this).get(PetViewModel.class);
        observeData();
    }

    private void initViews() {
        historyRecyclerView = findViewById(R.id.history_recycler_view);
        emptyView = findViewById(R.id.empty_view);
        petFilterSpinner = findViewById(R.id.pet_filter_spinner);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Vaccination History");
        }
    }

    private void setupRecyclerView() {
        historyAdapter = new VaccineHistoryAdapter(this);
        historyRecyclerView.setAdapter(historyAdapter);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSpinner() {
        petFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedPetId = -1; // All pets
                } else {
                    selectedPetId = allPets.get(position - 1).getId();
                }
                loadHistoryData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observeData() {
        // Load pets for filter spinner
        viewModel.getAllPets().observe(this, pets -> {
            allPets = pets;
            setupSpinnerData();
        });

        loadHistoryData();
    }

    private void setupSpinnerData() {
        List<String> spinnerItems = new ArrayList<>();
        spinnerItems.add("All Pets");

        if (allPets != null) {
            for (Pet pet : allPets) {
                spinnerItems.add(pet.getName());
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        petFilterSpinner.setAdapter(spinnerAdapter);
    }

    private void loadHistoryData() {
        if (selectedPetId == -1) {
            // Load all history
            viewModel.getVaccineHistory().observe(this, this::updateHistoryDisplay);
        } else {
            // Load history for specific pet
            viewModel.getVaccineHistoryForPet(selectedPetId).observe(this, this::updateHistoryDisplay);
        }
    }

    private void updateHistoryDisplay(List<VaccineHistoryItem> historyItems) {
        if (historyItems.isEmpty()) {
            historyRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            historyRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            historyAdapter.updateHistory(historyItems);
        }
    }

    @Override
    public void onHistoryItemClick(VaccineHistoryItem item) {
        // Open pet detail page
        Intent intent = new Intent(this, PetDetailActivity.class);
        intent.putExtra("petId", item.getPetId());
        startActivity(intent);
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