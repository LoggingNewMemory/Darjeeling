package com.kanagawa.yamada.darjeeling

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build

class CaffeineTileService : TileService() {
    companion object {
        const val ACTION_UPDATE_TILE = "com.kanagawa.yamada.darjeeling.UPDATE_TILE"
    }

    private val updateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            updateTileState()
        }
    }

    override fun onClick() {
        super.onClick()
        
        val now = System.currentTimeMillis()
        if (CaffeineService.currentState == 0) {
            CaffeineService.currentState = 1
        } else {
            if (now - CaffeineService.lastTapTime <= 3000L) {
                CaffeineService.currentState = when (CaffeineService.currentState) {
                    1 -> 2
                    2 -> 3
                    3 -> 4
                    4 -> 5
                    5 -> 0
                    else -> 0
                }
            } else {
                CaffeineService.currentState = 0
            }
        }
        CaffeineService.lastTapTime = now
        
        updateTileState()
        
        val intent = Intent(this, CaffeineService::class.java)
        intent.action = "ACTION_APPLY_CURRENT_STATE"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val filter = android.content.IntentFilter(ACTION_UPDATE_TILE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateReceiver, filter)
        }
        updateTileState()
    }
    
    override fun onStopListening() {
        super.onStopListening()
        try {
            unregisterReceiver(updateReceiver)
        } catch (e: Exception) {}
    }
    
    private fun updateTileState() {
        val tile = qsTile ?: return
        when (CaffeineService.currentState) {
            0 -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "Off"
            }
            1 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "5 minutes"
            }
            2 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "10 minutes"
            }
            3 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "15 minutes"
            }
            4 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "30 minutes"
            }
            5 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "Unlimited"
            }
        }
        tile.updateTile()
    }
}
