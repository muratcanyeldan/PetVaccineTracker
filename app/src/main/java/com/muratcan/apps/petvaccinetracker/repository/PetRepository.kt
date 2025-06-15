package com.muratcan.apps.petvaccinetracker.repository

import android.content.Context
import com.muratcan.apps.petvaccinetracker.database.AppDatabase
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.model.Vaccine
import com.muratcan.apps.petvaccinetracker.model.VaccineHistoryItem
import com.muratcan.apps.petvaccinetracker.model.VaccineWithPetName
import com.muratcan.apps.petvaccinetracker.util.DefaultDispatcherProvider
import com.muratcan.apps.petvaccinetracker.util.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PetRepository(
    context: Context,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    private val database = AppDatabase.getDatabase(context)
    private val petDao = database.petDao()
    private val vaccineDao = database.vaccineDao()

    fun getAllPetsFlow(): Flow<List<Pet>> = petDao.getAllPetsFlow()

    fun getPetsForUser(userId: String): Flow<List<Pet>> = petDao.getPetsForUser(userId)

    suspend fun getPetByIdFlow(petId: Long): Pet? = withContext(dispatchers.io) {
        petDao.getPetById(petId)
    }

    fun getVaccinesForPetFlow(petId: Long): Flow<List<Vaccine>> =
        vaccineDao.getVaccinesForPetFlow(petId)

    suspend fun addPet(pet: Pet): Long = withContext(dispatchers.io) {
        petDao.insert(pet)
    }

    suspend fun updatePet(pet: Pet) = withContext(dispatchers.io) {
        petDao.update(pet)
    }

    suspend fun deletePet(pet: Pet) = withContext(dispatchers.io) {
        petDao.delete(pet)
    }

    suspend fun addVaccine(vaccine: Vaccine): Long = withContext(dispatchers.io) {
        vaccineDao.insert(vaccine)
    }

    suspend fun updateVaccine(vaccine: Vaccine) = withContext(dispatchers.io) {
        vaccineDao.update(vaccine)
    }

    suspend fun deleteVaccine(vaccine: Vaccine) = withContext(dispatchers.io) {
        vaccineDao.delete(vaccine)
    }

    fun getVaccineHistory(): Flow<List<VaccineHistoryItem>> {
        return vaccineDao.getVaccineHistoryRaw()
            .map { vaccineWithPetList: List<VaccineWithPetName> ->
                vaccineWithPetList.map { vaccineWithPet: VaccineWithPetName ->
                    vaccineWithPet.toHistoryItem()
                }
            }
    }

    fun getVaccineHistoryForPet(petId: Long): Flow<List<VaccineHistoryItem>> {
        return vaccineDao.getVaccineHistoryForPetRaw(petId)
            .map { vaccineWithPetList: List<VaccineWithPetName> ->
                vaccineWithPetList.map { vaccineWithPet: VaccineWithPetName ->
                    vaccineWithPet.toHistoryItem()
                }
            }
    }
}