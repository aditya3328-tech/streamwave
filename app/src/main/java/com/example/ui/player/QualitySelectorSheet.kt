package com.example.ui.player

import androidx.compose.runtime.Composable
import com.example.data.model.PlaybackQuality

/**
 * Quality selector — driven entirely by the already-resolved [PlaybackQuality] list
 * (Auto + every resolution the provider returned). No stream re-resolution happens here.
 */
@Composable
fun QualitySelectorSheet(
    qualities: List<PlaybackQuality>,
    selectedLabel: String,
    onSelect: (PlaybackQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerBottomSheet(title = "Video Quality", onDismiss = onDismiss) {
        qualities.forEach { quality ->
            PlayerOptionRow(
                label = quality.label,
                subLabel = if (quality.label == "Auto") "Recommended" else null,
                selected = quality.label == selectedLabel,
                onClick = { onSelect(quality) },
            )
        }
    }
}
