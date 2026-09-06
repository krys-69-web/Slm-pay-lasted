package com.example.slmplay.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.slmplay.data.db.TrackEntity
import com.example.slmplay.service.MusicPlaybackService

class SLMPlayAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId, null, false)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            MusicPlaybackService.ACTION_TOGGLE,
            MusicPlaybackService.ACTION_NEXT,
            MusicPlaybackService.ACTION_PREV -> {
                val serviceIntent = Intent(context, MusicPlaybackService::class.java).apply {
                    action = intent.action
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

    companion object {
        private var lastTrack: TrackEntity? = null
        private var lastIsPlaying: Boolean = false

        fun updateAllWidgets(context: Context, track: TrackEntity?, isPlaying: Boolean) {
            lastTrack = track
            lastIsPlaying = isPlaying

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SLMPlayAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            for (appWidgetId in appWidgetIds) {
                updateWidgetView(context, appWidgetManager, appWidgetId, track, isPlaying)
            }
        }

        private fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            track: TrackEntity?,
            isPlaying: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_slm_play)

            // Intent to open Main Activity
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_info_container, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_cover, openAppPendingIntent)

            // Playback Intent Actions
            val prevPendingIntent = createActionPendingIntent(context, MusicPlaybackService.ACTION_PREV, 101)
            val togglePendingIntent = createActionPendingIntent(context, MusicPlaybackService.ACTION_TOGGLE, 102)
            val nextPendingIntent = createActionPendingIntent(context, MusicPlaybackService.ACTION_NEXT, 103)

            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, togglePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            // Track metadata & artwork
            if (track != null) {
                views.setTextViewText(R.id.widget_title, track.title)
                views.setTextViewText(R.id.widget_artist, track.artist)
                views.setTextViewText(
                    R.id.widget_app_badge,
                    if (isPlaying) "SLM PLAY • EN LECTURE" else "SLM PLAY • EN PAUSE"
                )

                // Cover
                val coverRes = when (track.coverResName) {
                    "cover_neon" -> R.drawable.cover_neon
                    "cover_ambient" -> R.drawable.cover_ambient
                    else -> R.drawable.slm_logo
                }
                views.setImageViewResource(R.id.widget_cover, coverRes)
            } else {
                views.setTextViewText(R.id.widget_title, "SLM Play")
                views.setTextViewText(R.id.widget_artist, "Appuyez pour écouter")
                views.setTextViewText(R.id.widget_app_badge, "SLM PLAY • AUDIO HD")
                views.setImageViewResource(R.id.widget_cover, R.drawable.slm_logo)
            }

            // Play / Pause Icon
            val playPauseIcon = if (isPlaying) {
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, SLMPlayAppWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
