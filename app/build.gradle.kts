plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.muratcan.apps.petvaccinetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.muratcan.apps.petvaccinetracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    
    // Firebase Authentication
    implementation(platform(libs.firebase.bom))
    implementation(libs.com.google.firebase.firebase.auth)
    
    // WorkManager
    implementation(libs.work.runtime)
    implementation(libs.guava)
    
    // Room for local database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    
    // Add desugaring for older Android versions
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}