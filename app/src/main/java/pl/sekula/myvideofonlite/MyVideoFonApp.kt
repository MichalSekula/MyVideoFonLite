package pl.sekula.myvideofonlite

import android.app.Application
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pl.sekula.myvideofonlite.di.appModule
import pl.sekula.myvideofonlite.events.DoorbellEventDispatcher
import pl.sekula.myvideofonlite.notifications.DoorbellNotifier
import pl.sekula.myvideofonlite.workers.StatusRefreshWorker

class MyVideoFonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyVideoFonApp)
            modules(appModule)
        }
        // TODO(TUYA): inicjalizacja SDK (ThingHomeSdk.init) po dodaniu zależności – patrz docs/TUYA_INTEGRATION.md.
        get<DoorbellNotifier>().createChannel()
        get<DoorbellEventDispatcher>().start()
        StatusRefreshWorker.schedule(this)
    }
}
