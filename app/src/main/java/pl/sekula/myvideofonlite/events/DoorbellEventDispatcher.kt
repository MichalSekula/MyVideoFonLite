package pl.sekula.myvideofonlite.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.sekula.myvideofonlite.data.DoorbellRepositoryImpl
import pl.sekula.myvideofonlite.domain.usecase.HandleDoorbellEvent
import pl.sekula.myvideofonlite.notifications.DoorbellNotifier
import pl.sekula.myvideofonlite.widget.WidgetUpdater

/**
 * Odbiera zdarzenia z aktywnego źródła (SDK/symulator), zapisuje je,
 * pokazuje powiadomienie i odświeża widget.
 *
 * Uwaga: dostarczanie zdarzeń w tle zależy od urządzenia, SDK (push Tuya)
 * i ograniczeń energii Androida – do przetestowania na docelowym telefonie.
 */
class DoorbellEventDispatcher(
    private val scope: CoroutineScope,
    private val repository: DoorbellRepositoryImpl,
    private val handleEvent: HandleDoorbellEvent,
    private val notifier: DoorbellNotifier,
    private val widgetUpdater: WidgetUpdater,
) {
    fun start() {
        scope.launch {
            repository.incomingEvents.collect { event ->
                handleEvent(event)?.let(notifier::showRing)
                widgetUpdater.update()
            }
        }
        scope.launch {
            repository.device.collect { widgetUpdater.update() }
        }
    }
}
