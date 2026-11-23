package pl.sekula.myvideofonlite.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

class WidgetUpdater(private val context: Context) {
    suspend fun update() {
        runCatching { DoorbellWidget().updateAll(context) }
    }
}
