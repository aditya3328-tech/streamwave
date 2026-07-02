package com.example.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CRUNCHY_ORANGE
import com.example.ui.components.DEEP_GREY

/**
 * Shared dark Material3 bottom-sheet scaffold + selectable row used by every player selector
 * (quality, subtitles, screen-fit, speed). Orange highlight + check mark on the active option.
 * Upgraded to premium near-black background (#0B0B0B) with 24dp rounded corners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PLAYER_BG, // Updated to premium #0B0B0B
        shape = RoundedCornerShape(topStart = GLASS_CORNER, topEnd = GLASS_CORNER), // 24dp rounded corners
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp, bottom = 12.dp),
            )
            content()
        }
    }
}

@Composable
fun PlayerOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    subLabel: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GLASS_CORNER)) // Updated to 24dp rounded corners
            .clickable(onClick = onClick)
            .background(if (selected) CRUNCHY_ORANGE.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (selected) CRUNCHY_ORANGE else Color.White,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
            if (subLabel != null) {
                Text(text = subLabel, color = Color.Gray, fontSize = 11.sp)
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = CRUNCHY_ORANGE,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Premium Playback Speed Selector bottom sheet.
 * Supports standard range: 0.25x, 0.5x, 0.75x, 1.0x (Normal), 1.25x, 1.5x, 2.0x.
 */
@Composable
fun SpeedSelectorSheet(
    selectedSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(
        0.25f to "0.25x",
        0.5f to "0.5x",
        0.75f to "0.75x",
        1.0f to "Normal (1.0x)",
        1.25f to "1.25x",
        1.5f to "1.5x",
        2.0f to "2.0x"
    )

    PlayerBottomSheet(title = "Playback Speed", onDismiss = onDismiss) {
        speeds.forEach { (value, label) ->
            PlayerOptionRow(
                label = label,
                selected = selectedSpeed == value,
                onClick = { onSelect(value) }
            )
        }
    }
}
