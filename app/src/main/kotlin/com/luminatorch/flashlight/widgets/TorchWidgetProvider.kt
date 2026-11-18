package com.luminatorch.flashlight.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.luminatorch.flashlight.R
import com.luminatorch.flashlight.hardware.TorchController
import com.luminatorch.flashlight.hardware.TorchMode

class TorchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val active = TorchController.getInstance(context).mode.value != TorchMode.OFF
        updateAllWidgets(context, appWidgetManager, appWidgetIds, active)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_TORCH -> {
                TorchController.getInstance(context).toggleSteadyTorch()
            }
            ACTION_UPDATE_WIDGET_UI -> {
                val active = intent.getBooleanExtra(EXTRA_IS_ACTIVE, false)
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, TorchWidgetProvider::class.java))
                updateAllWidgets(context, manager, ids, active)
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun updateAllWidgets(
        context: Context,
        manager: AppWidgetManager,
        widgetIds: IntArray,
        isActive: Boolean
    ) {
        val toggleIntent = Intent(context, TorchWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_TORCH
        }
        val tintColor = if (isActive) Color.parseColor("#FFB300") else Color.parseColor("#E6EDF7")

        widgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_torch)
            val pending = PendingIntent.getBroadcast(
                context,
                id,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_torch_root, pending)
            views.setOnClickPendingIntent(R.id.flashlight_btn, pending)
            views.setInt(R.id.flashlight_btn, "setColorFilter", tintColor)
            manager.updateAppWidget(id, views)
        }
    }

    companion object {
        const val ACTION_TOGGLE_TORCH = "com.luminatorch.flashlight.ACTION_TOGGLE_TORCH"
        const val ACTION_UPDATE_WIDGET_UI = "com.luminatorch.flashlight.ACTION_UPDATE_WIDGET_UI"
        const val EXTRA_IS_ACTIVE = "extra_is_active"
    }
}
