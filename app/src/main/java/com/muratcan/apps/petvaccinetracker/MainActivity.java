package com.muratcan.apps.petvaccinetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.muratcan.apps.petvaccinetracker.adapter.PetAdapter;
import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PetAdapter petAdapter;
    private FloatingActionButton addPetFab;
    private AppDatabase database;
    private ExecutorService executorService;
    private List<Pet> petList;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        petList = new ArrayList<>();
        firebaseHelper = new FirebaseHelper();

        initializeViews();
        setupRecyclerView();
        setupAddPetButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
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
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        firebaseHelper.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.petsRecyclerView);
        addPetFab = findViewById(R.id.addPetFab);
    }

    private void setupRecyclerView() {
        petAdapter = new PetAdapter(petList, pet -> {
            Intent intent = new Intent(MainActivity.this, PetDetailActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(petAdapter);
    }

    private void setupAddPetButton() {
        addPetFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPetActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPets();
    }

    private void loadPets() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        executorService.execute(() -> {
            try {
                List<Pet> pets = database.petDao().getAllPets(userId);
                runOnUiThread(() -> {
                    petAdapter.updatePets(pets);

                    if (pets.isEmpty()) {
                        findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        findViewById(R.id.emptyView).setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(MainActivity.this, 
                        getString(R.string.error_loading_pets, e.getMessage()), 
                        Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
} 