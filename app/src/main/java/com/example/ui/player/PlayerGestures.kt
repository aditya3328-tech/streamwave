package com.example.ui.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Which half of the player a vertical swipe started on. Left = brightness, Right = volume. */
enum class GestureSide { LEFT, RIGHT }

/**
 * Reusable OTT gesture layer:
 *  - single tap         → toggle controls
 *  - double-tap left    → seek −10s ; double-tap right → seek +10s
 *  - long-press         → 2x speed boost until release
 *  - vertical swipe L/R  → brightness / volume (reported as a +/- fraction of height)
 *
 * The composable owns the brightness/volume state and overlay; this modifier only reports intent.
 */
fun Modifier.playerGestures(
    onSingleTap: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSpeedBoostStart: () -> Unit,
    onSpeedBoostEnd: () -> Unit,
    onDragStart: (GestureSide) -> Unit,
    onVerticalDrag: (GestureSide, Float) -> Unit,
    onDragEnd: () -> Unit,
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = { onSingleTap() },
            onDoubleTap = { offset ->
                if (offset.x < size.width / 2f) onSeekBack() else onSeekForward()
            },
            onLongPress = { onSpeedBoostStart() },
            onPress = {
                // Completes on finger-up/cancel; pairs with onLongPress to end the 2x boost.
                tryAwaitRelease()
                onSpeedBoostEnd()
            },
        )
    }
    .pointerInput(Unit) {
        var side = GestureSide.RIGHT
        detectVerticalDragGestures(
            onDragStart = { offset ->
                side = if (offset.x < size.width / 2f) GestureSide.LEFT else GestureSide.RIGHT
                onDragStart(side)
            },
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragEnd() },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                // dragAmount > 0 is downward; negate so swiping up increases the value.
                val heightPx = size.height.takeIf { it > 0 } ?: 1
                onVerticalDrag(side, -dragAmount / heightPx)
            },
        )
    }
