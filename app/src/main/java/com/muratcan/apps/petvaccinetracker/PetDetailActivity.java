package com.muratcan.apps.petvaccinetracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.muratcan.apps.petvaccinetracker.adapter.VaccineAdapter;
import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper;
import com.muratcan.apps.petvaccinetracker.util.Logger;
import com.muratcan.apps.petvaccinetracker.util.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PetDetailActivity extends AppCompatActivity {
    private static final String TAG = "PetDetailActivity";
    private Pet pet;
    private RecyclerView vaccinesRecyclerView;
    private VaccineAdapter vaccineAdapter;
    private List<Vaccine> vaccineList;
    private AppDatabase database;
    private ExecutorService executorService;
    private SimpleDateFormat dateFormat;
    private AlertDialog activeDialog;
    private DatePickerDialog activeDatePickerDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_detail);

        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        dateFormat = createDateFormat();
        vaccineList = new ArrayList<>();

        // Handle getParcelableExtra for different Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pet = getIntent().getParcelableExtra("pet", Pet.class);
        } else {
            pet = getIntent().getParcelableExtra("pet");
        }

        if (pet == null) {
            Toast.makeText(this, "Error: Pet details not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Verify pet ownership
        verifyPetOwnership();
    }

    private void verifyPetOwnership() {
        String userId = new FirebaseHelper().getCurrentUserId();
        if (userId == null) {
            Logger.error(TAG, "User not logged in when attempting to access pet details");
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Store userId to prevent race condition
        final String finalUserId = userId;

        executorService.execute(() -> {
            try {
                // Re-verify user is still logged in
                if (new FirebaseHelper().getCurrentUserId() == null || 
                    !finalUserId.equals(new FirebaseHelper().getCurrentUserId())) {
                    Logger.error(TAG, "User session changed during pet ownership verification");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: Session expired", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                Pet verifiedPet = database.petDao().getPetById(pet.getId(), finalUserId);
                if (verifiedPet == null) {
                    Logger.error(TAG, String.format(Locale.getDefault(),
                        "Security violation: Unauthorized access attempt to pet ID: %d by user: %s", 
                        pet.getId(), finalUserId), new SecurityException("Unauthorized access"));
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: Unauthorized access", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // Update the pet object with verified data
                pet = verifiedPet;

                Logger.info(TAG, String.format(Locale.getDefault(), 
                    "Pet ownership verified for pet ID: %d, name: %s", 
                    pet.getId(), pet.getName()));

                runOnUiThread(() -> {
                    // Set up toolbar
                    Toolbar toolbar = findViewById(R.id.toolbar);
                    setSupportActionBar(toolbar);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        getSupportActionBar().setDisplayShowTitleEnabled(false);
                    }

                    initializeViews();
                    setupRecyclerView();
                    loadVaccines();
                });
            } catch (Exception e) {
                Logger.error(TAG, "Failed to verify pet ownership", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading pet details", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private SimpleDateFormat createDateFormat() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update dateFormat in case locale changed
        dateFormat = createDateFormat();
        // Only load vaccines if we have verified ownership
        if (pet != null && pet.getUserId() != null) {
            loadVaccines();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pet_detail, menu);
        return true;
    }

    private void initializeViews() {
        TextView petNameText = findViewById(R.id.petNameText);
        Chip petTypeChip = findViewById(R.id.petTypeChip);
        TextView petBreedText = findViewById(R.id.petBreedText);
        TextView petDobText = findViewById(R.id.petDobText);
        vaccinesRecyclerView = findViewById(R.id.vaccinesRecyclerView);
        ExtendedFloatingActionButton addVaccineFab = findViewById(R.id.addVaccineFab);

        petNameText.setText(pet.getName());
        petTypeChip.setText(pet.getType());
        petBreedText.setText(pet.getBreed());
        petDobText.setText(pet.getDateOfBirth());

        addVaccineFab.setOnClickListener(v -> showAddVaccineDialog());
    }

    private void setupRecyclerView() {
        vaccineAdapter = new VaccineAdapter(vaccineList, this::showEditVaccineDialog);
        vaccinesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        vaccinesRecyclerView.setAdapter(vaccineAdapter);
    }

    private void showAddVaccineDialog() {
        showVaccineDialog(null);
    }

    private void showEditVaccineDialog(Vaccine vaccine) {
        showVaccineDialog(vaccine);
    }

    private void showVaccineDialog(Vaccine existingVaccine) {
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }

        // Check for exact alarm permission if needed
        if (NotificationHelper.needsExactAlarmPermission(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("To schedule vaccine reminders, this app needs permission to schedule exact alarms. Would you like to grant this permission?")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = NotificationHelper.getExactAlarmSettingsIntent();
                    if (intent != null) {
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_vaccine, null);
        EditText nameInput = dialogView.findViewById(R.id.vaccineNameInput);
        EditText dateInput = dialogView.findViewById(R.id.vaccineDateInput);
        com.google.android.material.switchmaterial.SwitchMaterial recurringCheckbox = dialogView.findViewById(R.id.recurringCheckbox);
        View recurringContainer = dialogView.findViewById(R.id.recurringContainer);
        com.google.android.material.textfield.MaterialAutoCompleteTextView monthsSpinner = dialogView.findViewById(R.id.monthsSpinner);
        EditText notesInput = dialogView.findViewById(R.id.vaccineNotesInput);

        // Set up months spinner
        String[] monthsArray = getResources().getStringArray(R.array.months_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown_menu, monthsArray);
        monthsSpinner.setAdapter(adapter);
        
        // Make it non-editable but clickable
        monthsSpinner.setFocusable(false);
        monthsSpinner.setFocusableInTouchMode(false);
        monthsSpinner.setText(monthsArray[0], false); // Set default value without filtering
        
        // Enable dropdown on click
        monthsSpinner.setOnClickListener(v -> monthsSpinner.showDropDown());
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(existingVaccine != null ? "Edit Vaccine" : "Add Vaccine")
            .setView(dialogView)
            .setPositiveButton(existingVaccine != null ? "Update" : "Add", (dialog, which) -> {
                String name = nameInput.getText().toString().trim();
                String dateStr = dateInput.getText().toString().trim();
                boolean isRecurring = recurringCheckbox.isChecked();
                int months = isRecurring ? Integer.parseInt(monthsSpinner.getText().toString().split(" ")[0]) : 0;
                String notes = notesInput.getText().toString().trim();

                if (name.isEmpty() || dateStr.isEmpty()) {
                    Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    Date administeredDate = dateFormat.parse(dateStr);
                    if (administeredDate == null) {
                        Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Calendar nextDueDate = Calendar.getInstance();
                    nextDueDate.setTime(administeredDate);
                    if (isRecurring) {
                        nextDueDate.add(Calendar.MONTH, months);
                    }

                    String nextDueDateStr = isRecurring ? dateFormat.format(nextDueDate.getTime()) : "";

                    final Vaccine vaccine;
                    if (existingVaccine != null) {
                        vaccine = new Vaccine(name, pet.getId(), dateStr, nextDueDateStr, isRecurring, months, notes);
                        vaccine.setId(existingVaccine.getId());
                    } else {
                        vaccine = new Vaccine(name, pet.getId(), dateStr, nextDueDateStr, isRecurring, months, notes);
                    }
                    
                    executorService.execute(() -> {
                        try {
                            if (existingVaccine != null) {
                                database.vaccineDao().update(vaccine);
                                if (vaccine.isRecurring()) {
                                    NotificationHelper.scheduleNotification(
                                        PetDetailActivity.this,
                                        pet.getName(),
                                        vaccine.getName(),
                                        vaccine.getNextDueDate()
                                    );
                                }
                                runOnUiThread(() -> {
                                    Toast.makeText(PetDetailActivity.this, "Vaccine updated successfully", Toast.LENGTH_SHORT).show();
                                    loadVaccines();
                                });
                            } else {
                                long vaccineId = database.vaccineDao().insert(vaccine);
                                vaccine.setId(vaccineId);
                                
                                if (vaccine.isRecurring()) {
                                    NotificationHelper.scheduleNotification(
                                        PetDetailActivity.this,
                                        pet.getName(),
                                        vaccine.getName(),
                                        vaccine.getNextDueDate()
                                    );
                                }

                                runOnUiThread(() -> {
                                    Toast.makeText(PetDetailActivity.this, "Vaccine added successfully", Toast.LENGTH_SHORT).show();
                                    loadVaccines();
                                });
                            }
                        } catch (Exception e) {
                            String errorMessage = e.getMessage();
                            runOnUiThread(() -> Toast.makeText(PetDetailActivity.this,
                                "Error " + (existingVaccine != null ? "updating" : "adding") + 
                                " vaccine: " + (errorMessage != null ? errorMessage : "Unknown error"),
                                Toast.LENGTH_SHORT).show());
                        }
                    });
                } catch (Exception e) {
                    Toast.makeText(this, "Error processing date format", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null);

        // Add delete button for existing vaccines
        if (existingVaccine != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> showDeleteVaccineConfirmationDialog(existingVaccine));
        }

        // Set up recurring checkbox listener
        recurringCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> 
            recurringContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // If editing, populate the fields
        if (existingVaccine != null) {
            nameInput.setText(existingVaccine.getName());
            dateInput.setText(existingVaccine.getDateAdministered());
            recurringCheckbox.setChecked(existingVaccine.isRecurring());
            recurringContainer.setVisibility(existingVaccine.isRecurring() ? View.VISIBLE : View.GONE);
            if (existingVaccine.isRecurring()) {
                for (String month : monthsArray) {
                    if (month.startsWith(String.valueOf(existingVaccine.getRecurringPeriodMonths()))) {
                        monthsSpinner.setText(month, false);
                        break;
                    }
                }
            }
            notesInput.setText(existingVaccine.getNotes());
        } else {
            recurringContainer.setVisibility(View.GONE);
        }

        dateInput.setOnClickListener(v -> showDatePickerDialog(dateInput, existingVaccine));

        activeDialog = builder.create();
        activeDialog.show();
    }

    private void showDatePickerDialog(EditText dateInput, Vaccine existingVaccine) {
        if (activeDatePickerDialog != null && activeDatePickerDialog.isShowing()) {
            activeDatePickerDialog.dismiss();
        }

        Calendar calendar = Calendar.getInstance();
        
        // If editing an existing vaccine, set the calendar to its date
        if (existingVaccine != null && existingVaccine.getDateAdministered() != null) {
            try {
                Date date = dateFormat.parse(existingVaccine.getDateAdministered());
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception e) {
                Logger.error(TAG, "Error parsing existing vaccine date", e);
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                // Check if selected date is in the future
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(selectedYear, selectedMonth, selectedDay);
                
                if (selectedDate.after(Calendar.getInstance())) {
                    Toast.makeText(this, 
                        "Administration date cannot be in the future", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String date = String.format(Locale.getDefault(), "%02d/%02d/%d", 
                    selectedDay, selectedMonth + 1, selectedYear);
                dateInput.setText(date);
            },
            year, month, day
        );

        // Set the maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        
        activeDatePickerDialog = datePickerDialog;
        datePickerDialog.show();
    }

    private void loadVaccines() {
        executorService.execute(() -> {
            try {
                List<Vaccine> vaccines = database.vaccineDao().getVaccinesForPet(pet.getId());
                
                // Check for upcoming vaccines (due in next 7 days)
                Calendar calendar = Calendar.getInstance();
                Date today = calendar.getTime();
                calendar.add(Calendar.DAY_OF_MONTH, 7);
                Date sevenDaysLater = calendar.getTime();
                
                List<Vaccine> upcomingVaccines = new ArrayList<>();
                for (Vaccine vaccine : vaccines) {
                    try {
                        Date dueDate = dateFormat.parse(vaccine.getNextDueDate());
                        if (dueDate != null && !dueDate.before(today) && !dueDate.after(sevenDaysLater)) {
                            upcomingVaccines.add(vaccine);
                        }
                    } catch (Exception e) {
                        Logger.error(TAG, "Error parsing vaccine due date", e);
                    }
                }
                
                Logger.info(TAG, String.format(Locale.getDefault(),
                    "Loaded %d vaccines for pet: %s, %d upcoming in next 7 days",
                    vaccines.size(), pet.getName(), upcomingVaccines.size()));

                runOnUiThread(() -> {
                    vaccineAdapter.updateVaccines(vaccines);

                    if (vaccines.isEmpty()) {
                        findViewById(R.id.emptyVaccinesView).setVisibility(View.VISIBLE);
                        vaccinesRecyclerView.setVisibility(View.GONE);
                    } else {
                        findViewById(R.id.emptyVaccinesView).setVisibility(View.GONE);
                        vaccinesRecyclerView.setVisibility(View.VISIBLE);
                        
                        // Show alert for upcoming vaccines only if there are any within 7 days
                        if (!upcomingVaccines.isEmpty()) {
                            showUpcomingVaccinesAlert(upcomingVaccines);
                        }
                    }
                });
            } catch (Exception e) {
                Logger.error(TAG, String.format(Locale.getDefault(),
                    "Failed to load vaccines for pet: %s", pet.getName()), e);
                String errorMessage = e.getMessage();
                runOnUiThread(() -> Toast.makeText(PetDetailActivity.this, 
                    "Error loading vaccines: " + (errorMessage != null ? errorMessage : "Unknown error"), 
                    Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showUpcomingVaccinesAlert(List<Vaccine> upcomingVaccines) {
        StringBuilder message = new StringBuilder("Upcoming vaccines due within 7 days:\n\n");
        for (Vaccine vaccine : upcomingVaccines) {
            message.append(String.format(Locale.getDefault(), "• %s (Due: %s)\n", 
                vaccine.getName(), vaccine.getNextDueDate()));
        }

        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }

        activeDialog = new AlertDialog.Builder(this)
            .setTitle("Vaccine Reminders")
            .setMessage(message.toString())
            .setPositiveButton("OK", null)
            .create();
        activeDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Pet")
                .setMessage("Are you sure you want to delete this pet? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePet())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePet() {
        executorService.execute(() -> {
            try {
                // Cancel all notifications for this pet first
                NotificationHelper.cancelNotificationsForPet(this, pet.getName());
                
                // Then delete the pet and its vaccines
                database.petDao().deletePetAndVaccines(pet);
                Logger.info(TAG, String.format(Locale.getDefault(),
                    "Deleted pet and associated vaccines: name=%s, id=%d", pet.getName(), pet.getId()));
                runOnUiThread(() -> {
                    Toast.makeText(this, "Pet deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                Logger.error(TAG, String.format(Locale.getDefault(),
                    "Failed to delete pet: name=%s, id=%d", pet.getName(), pet.getId()), e);
                String errorMessage = e.getMessage();
                runOnUiThread(() -> Toast.makeText(this,
                    "Error deleting pet: " + (errorMessage != null ? errorMessage : "Unknown error"),
                    Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showDeleteVaccineConfirmationDialog(Vaccine vaccine) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Vaccine")
                .setMessage("Are you sure you want to delete this vaccine record? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteVaccine(vaccine))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteVaccine(Vaccine vaccine) {
        executorService.execute(() -> {
            try {
                database.vaccineDao().delete(vaccine);
                Logger.info(TAG, String.format(Locale.getDefault(),
                    "Deleted vaccine: name=%s, id=%d, petId=%d", 
                    vaccine.getName(), vaccine.getId(), pet.getId()));
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Vaccine deleted successfully", Toast.LENGTH_SHORT).show();
                    loadVaccines();
                });
            } catch (Exception e) {
                Logger.error(TAG, String.format(Locale.getDefault(),
                    "Failed to delete vaccine: name=%s, id=%d", 
                    vaccine.getName(), vaccine.getId()), e);
                String errorMessage = e.getMessage();
                runOnUiThread(() -> Toast.makeText(this,
                    "Error deleting vaccine: " + (errorMessage != null ? errorMessage : "Unknown error"),
                    Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }
        if (activeDatePickerDialog != null && activeDatePickerDialog.isShowing()) {
            activeDatePickerDialog.dismiss();
        }
        executorService.shutdown();
    }
} 