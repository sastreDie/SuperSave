package com.diegodavid.supersave.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// Android talks to widgets through these receivers (registered in AndroidManifest.xml).
// Each one just says which Glance widget it draws.

class SuperSaveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SuperSaveWidget()
}

class QuickExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickExpenseWidget()
}
