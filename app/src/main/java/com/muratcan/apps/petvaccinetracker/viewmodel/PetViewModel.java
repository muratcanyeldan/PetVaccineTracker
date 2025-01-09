package com.muratcan.apps.petvaccinetracker.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;
import com.muratcan.apps.petvaccinetracker.repository.PetRepository;

import java.util.List;

public class PetViewModel extends AndroidViewModel {
    private final PetRepository repository;
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();
    private final MutableLiveData<List<Vaccine>> vaccines = new MutableLiveData<>();
    private final LiveData<List<Pet>> pets;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Pet> currentPet = new MutableLiveData<>();
    private LiveData<List<Vaccine>> vaccinesLiveData;

    public PetViewModel(Application application) {
        super(application);
        repository = new PetRepository(application);
        pets = repository.getAllPets();
    }

    public LiveData<Boolean> getSuccess() {
        return success;
    }

    public LiveData<List<Vaccine>> getVaccines() {
        return vaccinesLiveData != null ? vaccinesLiveData : vaccines;
    }

    public LiveData<List<Pet>> getPets() {
        return pets;
    }

    public LiveData<Boolean> getIsLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadPets() {
        // No need to load explicitly since we're using LiveData from Room
        clearError(); // Clear any previous errors
        setLoading(true);
        setLoading(false);
    }

    public void loadVaccines(long petId) {
        clearError();
        setLoading(true);
        // Use LiveData for continuous updates
        vaccinesLiveData = repository.getVaccinesForPetLive(petId);
        setLoading(false);
    }

    public void refreshVaccines(long petId) {
        clearError();
        setLoading(true);
        // Use callback version for immediate refresh
        repository.getVaccinesForPet(petId,
            vaccineList -> mainHandler.post(() -> {
                vaccines.setValue(vaccineList);
                setLoading(false);
                clearError();
            }),
            errorMessage -> mainHandler.post(() -> {
                setError(errorMessage);
                setLoading(false);
            })
        );
    }

    public void addPet(Pet pet) {
        clearError(); // Clear any previous errors
        setLoading(true);
        repository.addPet(pet,
            () -> mainHandler.post(() -> {
                success.setValue(true);
                setLoading(false);
                clearError(); // Clear any previous errors on success
            }),
            errorMessage -> mainHandler.post(() -> {
                setError(errorMessage);
                setLoading(false);
            })
        );
    }

    public LiveData<Pet> getCurrentPet() {
        return currentPet;
    }

    public void loadPetById(long petId) {
        clearError();
        setLoading(true);
        repository.getPetById(petId,
            pet -> mainHandler.post(() -> {
                currentPet.setValue(pet);
                setLoading(false);
                clearError();
            }),
            errorMessage -> mainHandler.post(() -> {
                setError(errorMessage);
                setLoading(false);
            })
        );
    }

    private void setLoading(boolean isLoading) {
        mainHandler.post(() -> loading.setValue(isLoading));
    }

    private void setError(String errorMessage) {
        mainHandler.post(() -> error.setValue(errorMessage));
    }

    private void clearError() {
        mainHandler.post(() -> error.setValue(null));
    }
} 