package pl.sekula.myvideofonlite

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pl.sekula.myvideofonlite.presentation.MainActivity
import java.io.File

/**
 * Przechodzi przez aplikację w trybie demo i zapisuje zrzuty ekranu
 * (uruchamiane w CI przez .github/workflows/screenshots.yml).
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTour {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = instrumentation.targetContext
    private val prefix = InstrumentationRegistry.getArguments().getString("prefix", "")
    private val outDir = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }

    private fun shot(name: String) {
        device.waitForIdle()
        Thread.sleep(800)
        device.takeScreenshot(File(outDir, "$prefix$name.png"))
    }

    private fun waitFor(selector: BySelector, timeoutMs: Long = 10_000) {
        assertTrue("Nie znaleziono: $selector", device.wait(Until.hasObject(selector), timeoutMs))
    }

    private fun click(selector: BySelector) {
        waitFor(selector)
        device.findObject(selector).click()
    }

    @Test
    fun tour() {
        try {
            runTour()
        } catch (t: Throwable) {
            device.takeScreenshot(File(outDir, "zz_blad.png"))
            device.dumpWindowHierarchy(File(outDir, "zz_blad_hierarchia.xml"))
            throw t
        }
    }

    private fun runTour() {
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        waitFor(By.text("Nie skonfigurowano urządzenia"))
        shot("01_start_nieskonfigurowane")

        click(By.text("Przejdź do ustawień"))
        click(By.textContains("wejście (demo)"))
        waitFor(By.textContains("Wybrano"), 5_000)
        Thread.sleep(5_000) // snackbar znika
        shot("02_ustawienia")

        click(By.text("Symuluj dzwonek"))
        // Powiadomienie heads-up zasłania górę ekranu (w tym strzałkę "Wstecz") – czekamy aż zniknie.
        Thread.sleep(8_000)
        device.pressBack()
        waitFor(By.textContains("Ostatni dzwonek"))
        shot("03_ekran_glowny")

        click(By.text("Podgląd na żywo"))
        waitFor(By.textContains("Połączono"))
        shot("04_podglad_na_zywo")

        click(By.text("Rozpocznij rozmowę"))
        waitFor(By.text("Zakończ rozmowę"))
        shot("05_rozmowa")

        click(By.text("Otwórz furtkę"))
        waitFor(By.text("Otworzyć furtkę?"))
        Thread.sleep(1_200)
        shot("06_potwierdzenie_furtki")
        click(By.text("Anuluj"))

        click(By.text("Pełny ekran"))
        waitFor(By.text("Zamknij pełny ekran"))
        Thread.sleep(1_500)
        shot("07_pelny_ekran")
        click(By.text("Zamknij pełny ekran"))

        click(By.desc("Wstecz"))
        click(By.desc("Historia zdarzeń"))
        waitFor(By.text("Dzwonek"))
        shot("08_historia_zdarzen")

        device.openNotification()
        waitFor(By.textContains("Ktoś dzwoni do drzwi"))
        shot("09_powiadomienie_dzwonek")
        device.pressBack()
    }
}
