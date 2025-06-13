package com.muratcan.apps.petvaccinetracker.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object AddPet : Screen("add_pet")
    object EditPet : Screen("edit_pet/{petId}") {
        fun createRoute(petId: Long) = "edit_pet/$petId"
    }

    object PetDetail : Screen("pet_detail/{petId}") {
        fun createRoute(petId: Long) = "pet_detail/$petId"
    }

    object AddVaccine : Screen("add_vaccine/{petId}") {
        fun createRoute(petId: Long) = "add_vaccine/$petId"
    }

    object EditVaccine : Screen("edit_vaccine/{petId}/{vaccineId}") {
        fun createRoute(petId: Long, vaccineId: Long) = "edit_vaccine/$petId/$vaccineId"
    }

    object ImagePreview : Screen("image_preview/{imageUri}") {
        fun createRoute(imageUri: String) = "image_preview/${imageUri}"
    }
} 