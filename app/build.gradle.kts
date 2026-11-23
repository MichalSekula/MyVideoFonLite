import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Klucze aplikacji Tuya SDK trzymamy poza repozytorium (local.properties).
// To NIE jest client_secret Cloud OpenAPI – ten nigdy nie trafia do aplikacji.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Stały klucz podpisu (Tuya wiąże aplikację z SHA-256 certyfikatu). Bez niego – domyślny klucz debug.
val signingStoreFile: String? = localProps.getProperty("signing.storeFile")?.takeIf { it.isNotBlank() }

android {
    namespace = "pl.sekula.myvideofonlite"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.sekula.myvideofonlite"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TUYA_APP_KEY", "\"${localProps.getProperty("tuya.appKey", "")}\"")
        buildConfigField("String", "TUYA_APP_SECRET", "\"${localProps.getProperty("tuya.appSecret", "")}\"")
    }

    signingConfigs {
        if (signingStoreFile != null) {
            create("stable") {
                storeFile = rootProject.file(signingStoreFile)
                storePassword = localProps.getProperty("signing.storePassword")
                keyAlias = localProps.getProperty("signing.keyAlias")
                keyPassword = localProps.getProperty("signing.keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            if (signingStoreFile != null) signingConfig = signingConfigs.getByName("stable")
        }
        release {
            if (signingStoreFile != null) signingConfig = signingConfigs.getByName("stable")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.biometric)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Tuya SmartLife App SDK + IPC SDK – dodać po pozytywnym Etapie 0 (docs/TUYA_INTEGRATION.md)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
