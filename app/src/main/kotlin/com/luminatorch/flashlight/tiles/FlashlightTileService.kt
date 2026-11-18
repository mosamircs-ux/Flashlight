package com.luminatorch.flashlight.tiles

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.luminatorch.flashlight.hardware.TorchController
import com.luminatorch.flashlight.hardware.TorchMode

class FlashlightTileService : TileService() {

    override fun onClick() {
        TorchController.getInstance(this).toggleSteadyTorch()
        updateTileState()
    }

    override fun onStartListening() {
        updateTileState()
    }

    override fun onTileAdded() {
        updateTileState()
    }

    override fun onTileRemoved() {
        TorchController.getInstance(this).stopAll()
    }

    private fun updateTileState() {
        val active = TorchController.getInstance(this).mode.value != TorchMode.OFF
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
