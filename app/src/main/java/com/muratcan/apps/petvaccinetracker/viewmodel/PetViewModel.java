package com.muratcan.apps.petvaccinetracker.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;
import com.muratcan.apps.petvaccinetracker.repository.PetRepository;
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

public class PetViewModel extends AndroidViewModel {
    private final PetRepository repository;
    private final FirebaseHelper firebaseHelper;
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();
    private final MutableLiveData<List<Vaccine>> vaccines = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Pet> currentPet = new MutableLiveData<>();
    private LiveData<List<Vaccine>> vaccinesLiveData;
    private final LiveData<List<Pet>> allPetsLiveData;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> sortOrder = new MutableLiveData<>("none");
    private final LiveData<List<Pet>> filteredAndSortedPets;

    public PetViewModel(Application application) {
        super(application);
        repository = new PetRepository(application);
        firebaseHelper = new FirebaseHelper();
        allPetsLiveData = repository.getAllPets();
        
        // Transform allPetsLiveData based on search query and sort order
        filteredAndSortedPets = Transformations.switchMap(searchQuery, query -> 
            Transformations.switchMap(sortOrder, order ->
                Transformations.map(allPetsLiveData, pets -> {
                    List<Pet> filteredList;
                    if (query == null || query.isEmpty()) {
                        filteredList = new ArrayList<>(pets);
                    } else {
                        filteredList = pets.stream()
                            .filter(pet -> pet.getName().toLowerCase().contains(query.toLowerCase()) ||
                                    (pet.getBreed() != null && pet.getBreed().toLowerCase().contains(query.toLowerCase())) ||
                                    pet.getType().toLowerCase().contains(query.toLowerCase()))
                            .collect(Collectors.toList());
                    }

                    // Apply sorting
                    switch (order) {
                        case "name":
                            Collections.sort(filteredList, (p1, p2) -> 
                                p1.getName().compareToIgnoreCase(p2.getName()));
                            break;
                        case "date":
                            Collections.sort(filteredList, (p1, p2) -> {
                                if (p1.getBirthDate() == null) return 1;
                                if (p2.getBirthDate() == null) return -1;
                                return p2.getBirthDate().compareTo(p1.getBirthDate());
                            });
                            break;
                    }
                    return filteredList;
                })
            )
        );
    }

    public LiveData<Boolean> getSuccess() {
        return success;
    }

    public LiveData<List<Vaccine>> getVaccines() {
        return vaccinesLiveData != null ? vaccinesLiveData : vaccines;
    }

    public LiveData<List<Pet>> getPets() {
        return filteredAndSortedPets;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadPets() {
        isLoading.setValue(true);
        // The pets are automatically loaded through LiveData from Room
        isLoading.setValue(false);
    }

    public void sortByName() {
        sortOrder.setValue("name");
    }

    public void sortByDate() {
        sortOrder.setValue("date");
    }

    public void clearSort() {
        sortOrder.setValue("none");
    }

    public void filterPets(String query) {
        searchQuery.setValue(query);
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

    private void setLoading(boolean loading) {
        mainHandler.post(() -> isLoading.setValue(loading));
    }

    private void setError(String errorMessage) {
        mainHandler.post(() -> error.setValue(errorMessage));
    }

    private void clearError() {
        mainHandler.post(() -> error.setValue(null));
    }
} 