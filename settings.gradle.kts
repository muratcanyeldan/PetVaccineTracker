pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// Add version catalogs for plugins
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            plugin("firebase-crashlytics", "com.google.firebase.crashlytics").version("2.9.9")
            plugin("gms", "com.google.gms.google-services").version("4.4.2")
            plugin("ksp", "com.google.devtools.ksp").version("1.9.22-1.0.17")
        }
    }
}

rootProject.name = "PetVaccineTracker"
include(":app")
 