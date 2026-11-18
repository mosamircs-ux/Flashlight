package com.luminatorch.flashlight.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.luminatorch.flashlight.R
import com.luminatorch.flashlight.ui.BrightDisplayActivity

class BrightDisplayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val launchIntent = Intent(context, BrightDisplayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_bright_display)
            val pending = PendingIntent.getActivity(
                context,
                id,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_bright_root, pending)
            views.setOnClickPendingIntent(R.id.bright_display_btn, pending)
            views.setInt(R.id.bright_display_btn, "setColorFilter", Color.parseColor("#FFB300"))
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
