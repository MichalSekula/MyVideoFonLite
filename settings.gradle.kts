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
        // Tuya SDK (Etap 0): po weryfikacji urządzenia odkomentuj repozytorium Tuya
        // maven { url = uri("https://maven-other.tuya.com/repository/maven-releases/") }
    }
}

rootProject.name = "MyVideoFonLite"
include(":app", ":domain")
