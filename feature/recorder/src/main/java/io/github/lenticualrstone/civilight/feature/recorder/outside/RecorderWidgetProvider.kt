package io.github.lenticualrstone.civilight.feature.recorder.outside

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import io.github.lenticualrstone.civilight.feature.recorder.R
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface

/**
 * 홈 화면 위젯 (LRC-02, board `LrEntry`): the mic in a `pri` circle, `떠오른 생각` / `눌러서 바로
 * 녹음`. Static: it never shows a project name, record text or a count (rule 19). The same widget on
 * a launcher cover screen is LRC-04 (`ED`); unfolding keeps the recording going because the capture
 * lives in the process, not in a screen.
 *
 * **needsExternal stub:** "앱을 열지 않고" needs a translucent, task-isolated recording activity;
 * until it exists a tap opens the app's recording overlay through the launcher activity. Not run on
 * a device yet.
 */
class RecorderWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = views(context)
        for (id in appWidgetIds) runCatching { manager.updateAppWidget(id, views) }
    }

    companion object {
        fun views(context: Context): RemoteViews = RemoteViews(context.packageName, R.layout.recorder_widget).apply {
            setTextViewText(R.id.recorder_widget_title, RecorderUiCopy.WIDGET_TITLE)
            setTextViewText(R.id.recorder_widget_sub, RecorderUiCopy.WIDGET_SUB)
            setContentDescription(R.id.recorder_widget_root, RecorderUiCopy.WIDGET_TITLE)
            OutsideEntries.pendingIntent(context, OutsideEntries.route(EntrySurface.HOME_WIDGET), OutsideEntries.REQUEST_WIDGET)
                ?.let { setOnClickPendingIntent(R.id.recorder_widget_root, it) }
        }
    }
}
