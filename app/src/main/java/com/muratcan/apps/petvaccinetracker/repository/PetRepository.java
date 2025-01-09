package com.muratcan.apps.petvaccinetracker.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PetRepository {
    private final AppDatabase database;
    private final ExecutorService executorService;

    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnErrorListener {
        void onError(String error);
    }

    public interface OnVaccinesLoadedListener {
        void onVaccinesLoaded(List<Vaccine> vaccines);
    }

    public interface OnPetLoadedListener {
        void onPetLoaded(Pet pet);
    }

    public PetRepository(Application application) {
        database = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Vaccine>> getVaccinesForPetLive(long petId) {
        return database.vaccineDao().getVaccinesForPet(petId);
    }

    public void getVaccinesForPet(long petId, OnVaccinesLoadedListener successListener, OnErrorListener errorListener) {
        executorService.execute(() -> {
            try {
                List<Vaccine> vaccines = database.vaccineDao().getVaccinesForPetSync(petId);
                successListener.onVaccinesLoaded(vaccines);
            } catch (Exception e) {
                errorListener.onError("Error loading vaccines: " + e.getMessage());
            }
        });
    }

    public LiveData<List<Pet>> getAllPets() {
        return database.petDao().getAllPets();
    }

    public void addPet(Pet pet, OnSuccessListener successListener, OnErrorListener errorListener) {
        executorService.execute(() -> {
            try {
                long id = database.petDao().insert(pet);
                pet.setId(id);
                successListener.onSuccess();
            } catch (Exception e) {
                errorListener.onError("Error adding pet: " + e.getMessage());
            }
        });
    }

    public void getPetById(long petId, OnPetLoadedListener successListener, OnErrorListener errorListener) {
        executorService.execute(() -> {
            try {
                Pet pet = database.petDao().getPetById(petId);
                if (pet != null) {
                    successListener.onPetLoaded(pet);
                } else {
                    errorListener.onError("Pet not found");
                }
            } catch (Exception e) {
                errorListener.onError("Error loading pet: " + e.getMessage());
            }
        });
    }
} 