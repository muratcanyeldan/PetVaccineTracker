package com.muratcan.apps.petvaccinetracker.repository

import android.content.Context
import com.muratcan.apps.petvaccinetracker.database.AppDatabase
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.model.Vaccine
import com.muratcan.apps.petvaccinetracker.model.VaccineHistoryItem
import com.muratcan.apps.petvaccinetracker.model.VaccineWithPetName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PetRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val petDao = database.petDao()
    private val ktPetDao = database.ktPetDao()
    private val vaccineDao = database.vaccineDao()

    fun getAllPetsFlow(): Flow<List<Pet>> = ktPetDao.getAllPetsFlow()

    fun getPetsForUser(userId: String): Flow<List<Pet>> = ktPetDao.getPetsForUser(userId)

    suspend fun getPetByIdFlow(petId: Long): Pet = withContext(Dispatchers.IO) {
        ktPetDao.getPetById(petId)
    }

    fun getVaccinesForPetFlow(petId: Long): Flow<List<Vaccine>> =
        vaccineDao.getVaccinesForPetFlow(petId)

    suspend fun addPet(pet: Pet) = withContext(Dispatchers.IO) {
        ktPetDao.insert(pet)
    }

    suspend fun updatePet(pet: Pet) = withContext(Dispatchers.IO) {
        ktPetDao.update(pet)
    }

    suspend fun deletePet(pet: Pet) = withContext(Dispatchers.IO) {
        ktPetDao.delete(pet)
    }

    suspend fun addVaccine(vaccine: Vaccine) = withContext(Dispatchers.IO) {
        vaccineDao.insert(vaccine)
    }

    suspend fun updateVaccine(vaccine: Vaccine) = withContext(Dispatchers.IO) {
        vaccineDao.update(vaccine)
    }

    suspend fun deleteVaccine(vaccine: Vaccine) = withContext(Dispatchers.IO) {
        vaccineDao.delete(vaccine)
    }

    fun getVaccineHistory(): Flow<List<VaccineHistoryItem>> {
        return vaccineDao.getVaccineHistoryRaw().map { vaccineWithPetList: List<VaccineWithPetName> ->
            vaccineWithPetList.map { vaccineWithPet: VaccineWithPetName ->
                vaccineWithPet.toHistoryItem()
            }
        }
    }

    fun getVaccineHistoryForPet(petId: Long): Flow<List<VaccineHistoryItem>> {
        return vaccineDao.getVaccineHistoryForPetRaw(petId).map { vaccineWithPetList: List<VaccineWithPetName> ->
            vaccineWithPetList.map { vaccineWithPet: VaccineWithPetName ->
                vaccineWithPet.toHistoryItem()
            }
        }
    }
} 