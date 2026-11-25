package pl.sekula.myvideofonlite

import android.app.Application
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pl.sekula.myvideofonlite.data.local.LocalStore
import pl.sekula.myvideofonlite.data.tuya.TuyaBackend
import pl.sekula.myvideofonlite.domain.model.BackendMode
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
        // Tuya SDK startuje tylko w trybie Tuya (tryb demo działa bez SDK i kluczy).
        get<CoroutineScope>().launch(Dispatchers.Main) {
            get<LocalStore>().settings.map { it.backendMode }.distinctUntilChanged().collect { mode ->
                if (mode == BackendMode.TUYA) get<TuyaBackend>().start()
            }
        }
        get<DoorbellNotifier>().createChannel()
        get<DoorbellEventDispatcher>().start()
        StatusRefreshWorker.schedule(this)
    }
}
