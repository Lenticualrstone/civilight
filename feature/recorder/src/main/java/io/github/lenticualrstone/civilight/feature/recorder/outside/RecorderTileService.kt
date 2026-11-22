package io.github.lenticualrstone.civilight.feature.recorder.outside

import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.lenticualrstone.civilight.feature.recorder.R
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface

/**
 * 빠른 설정 타일 (LRC-03): one tap starts a recording (`알림창에서 한 번`) with no pre-filled link.
 *
 * **needsExternal stub (LRC-05):** recording on the lock screen without unlocking needs a
 * show-when-locked recording surface, which is not built. On the lock screen the tile asks for the
 * device unlock first (`unlockAndRun`), then opens the app's recording overlay. Not run on a device yet.
 */
class RecorderTileService : TileService() {
    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.label = RecorderUiCopy.TILE_LABEL
        tile.icon = Icon.createWithResource(this, R.drawable.recorder_widget_mic)
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        if (isLocked) unlockAndRun(::open) else open()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun open() {
        val route = OutsideEntries.route(EntrySurface.QS_TILE)
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                OutsideEntries.pendingIntent(this, route, OutsideEntries.REQUEST_TILE)?.let(::startActivityAndCollapse)
            } else {
                @Suppress("DEPRECATION")
                OutsideEntries.intent(this, route)?.let(::startActivityAndCollapse)
            }
        }
    }
}
