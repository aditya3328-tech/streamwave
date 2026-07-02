package com.example.ui.player

import androidx.compose.runtime.Composable
import com.example.data.model.SubtitleTrack

const val SUBTITLE_OFF = "Off"

/**
 * Subtitle selector — "Off" plus every caption track fetched from the existing caption API.
 * Selection is by label; the player maps it back to a [SubtitleTrack] / disables text tracks.
 */
@Composable
fun SubtitleSelectorSheet(
    subtitles: List<SubtitleTrack>,
    selectedLabel: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerBottomSheet(title = "Subtitles", onDismiss = onDismiss) {
        PlayerOptionRow(
            label = SUBTITLE_OFF,
            selected = selectedLabel == SUBTITLE_OFF,
            onClick = { onSelect(SUBTITLE_OFF) },
        )
        if (subtitles.isEmpty()) {
            PlayerOptionRow(
                label = "No subtitles available",
                selected = false,
                onClick = {},
            )
        } else {
            subtitles.forEach { track ->
                PlayerOptionRow(
                    label = track.label,
                    selected = track.label == selectedLabel,
                    onClick = { onSelect(track.label) },
                )
            }
        }
    }
}
