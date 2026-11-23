package pl.sekula.myvideofonlite.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.sekula.myvideofonlite.data.local.LocalStore
import pl.sekula.myvideofonlite.data.local.WidgetSnapshot
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.presentation.MainActivity
import pl.sekula.myvideofonlite.presentation.common.formatDateTime
import pl.sekula.myvideofonlite.presentation.common.formatTime

/**
 * Widget 2x2 / 4x2: ostatni ZNANY status, ostatni dzwonek i skróty.
 * Nie odtwarza strumienia i nie udaje obrazu na żywo (spec. rozdz. 5).
 */
class DoorbellWidget : GlanceAppWidget(), KoinComponent {

    private val store: LocalStore by inject()

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = store.widgetSnapshot.first()
        provideContent {
            GlanceTheme {
                WidgetContent(snapshot)
            }
        }
    }

    @Composable
    private fun WidgetContent(snapshot: WidgetSnapshot) {
        val context = LocalContext.current
        val wide = LocalSize.current.width >= WIDE.width
        val openApp = actionStartActivity(MainActivity.intent(context, null))
        val openLive = actionStartActivity(MainActivity.intent(context, MainActivity.ACTION_LIVE_VIEW))
        // Furtka z widgetu: tylko przejście do aplikacji z oknem potwierdzenia.
        val openGate = actionStartActivity(MainActivity.intent(context, MainActivity.ACTION_GATE))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(openApp),
        ) {
            Text(
                text = snapshot.deviceName ?: "Wideodomofon",
                maxLines = 1,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurface,
                ),
            )
            if (!snapshot.configured) {
                Text(
                    text = "Nie skonfigurowano – dotknij, aby ustawić",
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "● ",
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(statusColor(snapshot.status))),
                )
                Text(
                    text = statusLabel(snapshot.status) +
                        (snapshot.statusUpdatedAt?.let { " (stan z ${formatTime(it)})" } ?: ""),
                    maxLines = 1,
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                )
            }
            Text(
                text = "Ostatni dzwonek: " + (snapshot.lastRingAt?.let { formatDateTime(it) } ?: "brak"),
                maxLines = 2,
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Button(text = "Podgląd", onClick = openLive)
                if (wide && snapshot.gateAvailable) {
                    Spacer(GlanceModifier.width(8.dp))
                    Button(text = "Furtka", onClick = openGate)
                }
            }
        }
    }

    private fun statusLabel(status: DeviceStatus) = when (status) {
        DeviceStatus.ONLINE -> "Online"
        DeviceStatus.OFFLINE -> "Offline"
        DeviceStatus.UNKNOWN -> "Stan nieznany"
    }

    private fun statusColor(status: DeviceStatus) = when (status) {
        DeviceStatus.ONLINE -> Color(0xFF2E7D32)
        DeviceStatus.OFFLINE -> Color(0xFFC62828)
        DeviceStatus.UNKNOWN -> Color(0xFF9E9E9E)
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
    }
}
