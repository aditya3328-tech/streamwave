package com.example.ui.player

import androidx.compose.runtime.Composable

/**
 * Player screen-fit modes. The player maps each to a Media3 AspectRatioFrameLayout resize
 * constant inside its (already @UnstableApi-opted-in) PlayerView block.
 *  - FIT  → letterbox, preserve aspect (no crop)
 *  - FILL → stretch to fill (may distort)
 *  - ZOOM → crop to fill (preserve aspect, fills screen — removes black bars)
 */
enum class PlayerResizeMode(val label: String, val description: String) {
    FIT("Fit", "Preserve aspect ratio"),
    FILL("Fill", "Stretch to fill screen"),
    ZOOM("Zoom", "Crop to fill screen"),
}

@Composable
fun ResizeModeSheet(
    current: PlayerResizeMode,
    onSelect: (PlayerResizeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerBottomSheet(title = "Screen Fit", onDismiss = onDismiss) {
        PlayerResizeMode.entries.forEach { mode ->
            PlayerOptionRow(
                label = mode.label,
                subLabel = mode.description,
                selected = mode == current,
                onClick = { onSelect(mode) },
            )
        }
    }
}
