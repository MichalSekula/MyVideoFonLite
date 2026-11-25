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
        applicationId = "pl.gbc.myvideofonlite" // zgodnie z rejestracją aplikacji w panelu Tuya
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Numer builda z GitHub Actions – widoczny w Ustawieniach, żeby odróżnić wersje APK.
        buildConfigField("String", "BUILD_ID", "\"${System.getenv("GITHUB_RUN_NUMBER") ?: "lokalny"}\"")
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
    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += setOf("lib/*/libc++_shared.so", "lib/*/libyuv.so", "lib/*/libopenh264.so")
        }
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/*.kotlin_module")
        }
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

    // Tuya SmartLife App SDK 7.11 + IPC SDK (pakiet z panelu Tuya)
    implementation(libs.play.services.code.scanner)
    implementation(libs.tuya.thingsmart)
    implementation(libs.tuya.ipcsdk)
    implementation(libs.fastjson.android)
    implementation(libs.okhttp.urlconnection)
    // Plik bezpieczeństwa Tuya (security-algorithm-*.aar) – odszyfrowywany w CI do app/libs, poza repozytorium.
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

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

// Narzędzie: wypakowuje klasy Tuya SDK do build/tuyajars (inspekcja API w CI).
tasks.register("dumpTuyaJars") {
    doLast {
        val out = layout.buildDirectory.dir("tuyajars").get().asFile.apply { deleteRecursively(); mkdirs() }
        val view = configurations.getByName("debugRuntimeClasspath").incoming.artifactView {
            isLenient = true
            attributes { attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar") }
        }
        view.artifacts.forEach { a ->
            val id = a.id.componentIdentifier
            if (id is org.gradle.api.artifacts.component.ModuleComponentIdentifier && id.group.startsWith("com.thingclips")) {
                a.file.copyTo(File(out, "${id.module}-${id.version}.jar"), overwrite = true)
            }
        }
    }
}

// Wymagane przez Tuya SDK (artefakt adnotacji nie jest publikowany).
configurations.all {
    exclude(group = "com.thingclips.smart", module = "thingsmart-modularCampAnno")
}
