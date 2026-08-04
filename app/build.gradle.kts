import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

/*
 * Release signing, read from a keystore.properties that is never committed
 * (see .gitignore) or from the environment for CI. Absent credentials leave
 * the release build unsigned rather than failing, so anyone can still run
 * assembleRelease to check size and R8 output without holding the key.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { stream -> load(stream) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "AXIOM_KEYSTORE_FILE")
val hasReleaseSigning = releaseStoreFile != null && rootProject.file(releaseStoreFile).exists()

android {
    namespace = "com.cosmiclaboratory.axiom"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cosmiclaboratory.axiom"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("storePassword", "AXIOM_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "AXIOM_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "AXIOM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

/*
 * Where schemas would be written once exportSchema can be switched on. It is
 * off today because Room's schema bundles cannot be deserialized by the
 * kotlinx-serialization compiler plugin in Kotlin 2.0.21 — see AxiomDatabase.
 * The location is declared now so turning export on later is a one-line change.
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    
    // Compose BOM and UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Architecture & DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // Preferences
    implementation(libs.datastore.preferences)

    // Background Work
    implementation(libs.work.manager)

    // Networking — Ktor for Groq AI
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    
    // Widgets
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    
    // Security — encrypted Groq API key storage
    implementation(libs.androidx.security.crypto)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.ktor.client.mock)

    // Android Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.work.testing)
    
    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}