package com.muratcan.apps.petvaccinetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.model.Vaccine
import com.muratcan.apps.petvaccinetracker.model.VaccineHistoryItem
import com.muratcan.apps.petvaccinetracker.repository.PetRepository
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class PetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PetRepository
    private val firebaseHelper: FirebaseHelper = FirebaseHelper()

    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets.asStateFlow()
    private val _petsLiveData: LiveData<List<Pet>> = pets.asLiveData()

    private val _currentPet = MutableStateFlow<Pet?>(null)
    val currentPet: StateFlow<Pet?> = _currentPet.asStateFlow()
    private val _currentPetLiveData: LiveData<Pet?> = currentPet.asLiveData()

    private val _vaccines = MutableStateFlow<List<Vaccine>>(emptyList())
    val vaccines: StateFlow<List<Vaccine>> = _vaccines.asStateFlow()
    private val _vaccinesLiveData: LiveData<List<Vaccine>> = vaccines.asLiveData()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _isLoadingLiveData: LiveData<Boolean> = isLoading.asLiveData()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _errorLiveData: LiveData<String?> = error.asLiveData()

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow("none")

    init {
        repository = PetRepository(application.applicationContext)
        loadPets()
        observePets()
    }

    private fun observePets() {
        viewModelScope.launch {
            try {
                val currentUserId = firebaseHelper.getCurrentUserId()
                if (currentUserId == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }
                
                repository.getPetsForUser(currentUserId).collect { petsList ->
                    val filteredAndSorted = applyFiltersAndSort(petsList)
                    _pets.value = filteredAndSorted
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun applyFiltersAndSort(pets: List<Pet>): List<Pet> {
        var result = if (_searchQuery.value.isEmpty()) {
            pets
        } else {
            pets.filter { pet ->
                pet.name.contains(_searchQuery.value, ignoreCase = true) ||
                pet.type.contains(_searchQuery.value, ignoreCase = true) ||
                pet.breed.contains(_searchQuery.value, ignoreCase = true)
            }
        }

        when (_sortOrder.value) {
            "name" -> result = result.sortedBy { it.name.lowercase() }
            "date" -> result = result.sortedByDescending { it.birthDate }
            else -> { /* no sorting */ }
        }

        Timber.d("Sorting by: ${_sortOrder.value}, Results: ${result.size} pets")
        return result
    }

    fun loadPets() {
        refreshPets()
    }

    fun refreshPets() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = firebaseHelper.getCurrentUserId()
                if (currentUserId == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }
                
                // Get first emission only
                val petsList = repository.getPetsForUser(currentUserId).first()
                val filteredAndSorted = applyFiltersAndSort(petsList)
                _pets.value = filteredAndSorted
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadPetById(petId: Long) {
        viewModelScope.launch {
            try {
                val pet = repository.getPetByIdFlow(petId)
                _currentPet.value = pet
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadVaccines(petId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getVaccinesForPetFlow(petId).collect { vaccinesList ->
                    _vaccines.value = vaccinesList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    // Alias for Java code
    fun refreshVaccines(petId: Long) = loadVaccines(petId)

    fun addPet(pet: Pet) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Timber.d("Starting to add pet: ${pet.name}")
                
                // Get current user ID
                val currentUserId = firebaseHelper.getCurrentUserId()
                if (currentUserId == null) {
                    throw IllegalArgumentException("User not logged in")
                }
                pet.userId = currentUserId
                
                // Validate all required fields
                if (pet.name.isNullOrBlank()) {
                    throw IllegalArgumentException("Pet name cannot be null or empty")
                }
                if (pet.type.isNullOrBlank()) {
                    throw IllegalArgumentException("Pet type cannot be null or empty")
                }
                if (pet.breed.isNullOrBlank()) {
                    pet.breed = "Unknown" // Set a default value for breed if null
                }
                if (pet.birthDate == null) {
                    throw IllegalArgumentException("Birth date cannot be null")
                }
                
                Timber.d("Validated pet details - Name: ${pet.name}, Type: ${pet.type}, Breed: ${pet.breed}, UserId: ${pet.userId}, BirthDate: ${pet.birthDate}")
                
                // Add pet and get its ID
                val id = repository.addPet(pet)
                
                if (id > 0) {
                    Timber.d("Pet added successfully with ID: $id")
                    pet.id = id
                    
                    // Force a refresh to update the UI
                    val petsList = repository.getPetsForUser(currentUserId).first()
                    val filteredAndSorted = applyFiltersAndSort(petsList)
                    _pets.value = filteredAndSorted
                } else {
                    Timber.e("Failed to add pet - returned ID was $id")
                    _error.value = "Failed to add pet to database"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding pet: ${e.message}")
                _error.value = "Error adding pet: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePet(pet: Pet) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updatePet(pet)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun deletePet(pet: Pet) {
        viewModelScope.launch {
            try {
                repository.deletePet(pet)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun filterPets(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            try {
                val currentUserId = firebaseHelper.getCurrentUserId()
                if (currentUserId == null) {
                    _error.value = "User not logged in"
                    return@launch
                }
                
                val petsList = repository.getPetsForUser(currentUserId).first()
                val filteredAndSorted = applyFiltersAndSort(petsList)
                _pets.value = filteredAndSorted
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun sortByName() {
        Timber.d("Sorting by name")
        _sortOrder.value = "name"
        refreshPets()
    }

    fun sortByDate() {
        Timber.d("Sorting by date")
        _sortOrder.value = "date"
        refreshPets()
    }

    fun clearSort() {
        Timber.d("Clearing sort")
        _sortOrder.value = "none"
        refreshPets()
    }

    fun clearError() {
        _error.value = null
    }

    // For accessing all pets (for the filter spinner)
    fun getAllPets(): LiveData<List<Pet>> = _petsLiveData

    // For vaccination history
    fun getVaccineHistory(): LiveData<List<VaccineHistoryItem>> {
        return repository.getVaccineHistory().asLiveData()
    }

    fun getVaccineHistoryForPet(petId: Long): LiveData<List<VaccineHistoryItem>> {
        return repository.getVaccineHistoryForPet(petId).asLiveData()
    }

    // Getter methods for Java interop
    fun getPetsLiveData(): LiveData<List<Pet>> = _petsLiveData
    fun getCurrentPetLiveData(): LiveData<Pet?> = _currentPetLiveData
    fun getVaccinesLiveData(): LiveData<List<Vaccine>> = _vaccinesLiveData
    fun getIsLoadingLiveData(): LiveData<Boolean> = _isLoadingLiveData
    fun getErrorLiveData(): LiveData<String?> = _errorLiveData
} 