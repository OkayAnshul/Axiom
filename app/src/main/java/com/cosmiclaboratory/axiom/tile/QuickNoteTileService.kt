package com.cosmiclaboratory.axiom.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.cosmiclaboratory.axiom.MainActivity
import com.cosmiclaboratory.axiom.R

class QuickNoteTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        
        // Create intent to open the app and create a new note
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "create_note")
        }
        
        // Use PendingIntent for all Android versions (modern approach)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
            @Suppress("NewApi")
            startActivityAndCollapse(pendingIntent)
        } else {
            // For older versions, use the traditional method without collapsing
            startActivity(intent)
        }
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)
            tile.label = "Quick Entry"
            tile.contentDescription = "Create a new note quickly"
            tile.state = Tile.STATE_ACTIVE
            tile.updateTile()
        }
    }
}