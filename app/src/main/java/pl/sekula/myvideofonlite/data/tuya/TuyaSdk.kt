package pl.sekula.myvideofonlite.data.tuya

import android.app.Application
import android.util.Log
import com.thingclips.smart.home.sdk.ThingHomeSdk
import pl.sekula.myvideofonlite.BuildConfig

/**
 * Leniwa inicjalizacja Tuya SDK.
 *
 * SDK startuje dopiero wtedy, gdy użytkownik wybierze tryb Tuya (albo aplikacja
 * wstaje z zapisanym trybem Tuya), i tylko gdy build ma klucze aplikacji.
 * Dzięki temu tryb demo działa bez kluczy i bez bibliotek natywnych Tuya.
 */
object TuyaSdk {
    private const val TAG = "TuyaSdk"

    @Volatile
    private var initialized = false

    /** Opis ostatniego błędu inicjalizacji (klasa i komunikat wyjątku, bez kluczy). */
    @Volatile
    var lastError: String? = null
        private set

    val hasKeys: Boolean
        get() = BuildConfig.TUYA_APP_KEY.isNotBlank() && BuildConfig.TUYA_APP_SECRET.isNotBlank()

    /** @return true, gdy SDK jest gotowe do użycia. */
    @Synchronized
    fun ensureInit(app: Application): Boolean {
        if (initialized) return true
        if (!hasKeys) return false
        return try {
            ThingHomeSdk.init(app, BuildConfig.TUYA_APP_KEY, BuildConfig.TUYA_APP_SECRET)
            ThingHomeSdk.setDebugMode(BuildConfig.DEBUG)
            initialized = true
            lastError = null
            true
        } catch (t: Throwable) {
            // Np. brak bibliotek natywnych dla architektury urządzenia. Nie logujemy kluczy.
            val root = generateSequence(t) { it.cause }.last()
            lastError = "${root.javaClass.simpleName}: ${root.message ?: "-"}".take(300)
            Log.e(TAG, "Nie udało się zainicjalizować Tuya SDK", t)
            false
        }
    }
}
