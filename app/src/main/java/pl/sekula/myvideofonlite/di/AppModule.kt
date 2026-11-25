package pl.sekula.myvideofonlite.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.sekula.myvideofonlite.data.DoorbellRepositoryImpl
import pl.sekula.myvideofonlite.data.demo.DemoBackend
import pl.sekula.myvideofonlite.data.local.LocalStore
import pl.sekula.myvideofonlite.data.tuya.TuyaBackend
import pl.sekula.myvideofonlite.data.tuya.TuyaPairing
import pl.sekula.myvideofonlite.presentation.pairing.PairingViewModel
import pl.sekula.myvideofonlite.domain.repository.DoorbellRepository
import pl.sekula.myvideofonlite.domain.repository.SettingsRepository
import pl.sekula.myvideofonlite.domain.usecase.ConnectToDoorbell
import pl.sekula.myvideofonlite.domain.usecase.HandleDoorbellEvent
import pl.sekula.myvideofonlite.domain.usecase.ObserveHomeState
import pl.sekula.myvideofonlite.domain.usecase.OpenGate
import pl.sekula.myvideofonlite.domain.usecase.StartLiveView
import pl.sekula.myvideofonlite.events.DoorbellEventDispatcher
import pl.sekula.myvideofonlite.notifications.DoorbellNotifier
import pl.sekula.myvideofonlite.presentation.events.EventsViewModel
import pl.sekula.myvideofonlite.presentation.home.HomeViewModel
import pl.sekula.myvideofonlite.presentation.liveview.LiveViewViewModel
import pl.sekula.myvideofonlite.presentation.settings.SettingsViewModel
import pl.sekula.myvideofonlite.widget.WidgetUpdater

val appModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { LocalStore(androidContext()) }
    single<SettingsRepository> { get<LocalStore>() }
    single { DemoBackend() }
    single { TuyaBackend(androidContext()) }
    single { TuyaPairing(androidContext(), get()) }
    single { DoorbellRepositoryImpl(get(), get(), get(), get()) }
    single<DoorbellRepository> { get<DoorbellRepositoryImpl>() }

    factory { ObserveHomeState(get()) }
    factory { ConnectToDoorbell(get()) }
    factory { OpenGate(get()) }
    factory { StartLiveView(get()) }
    factory { HandleDoorbellEvent(get()) }

    single { DoorbellNotifier(androidContext()) }
    single { WidgetUpdater(androidContext()) }
    single { DoorbellEventDispatcher(get(), get(), get(), get(), get()) }

    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { LiveViewViewModel(get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
    viewModel { EventsViewModel(get()) }
    viewModel { PairingViewModel(get(), get(), get()) }
}
