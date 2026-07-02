package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium, fully responsive, glassmorphic player controls.
 * Adapts seamlessly between phone portrait, phone landscape, and tablet modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    isSpeedBoosted: Boolean,
    positionMs: Long,
    durationMs: Long,
    selectedQualityLabel: String,
    currentSpeedLabel: String,
    pipSupported: Boolean,
    isLocked: Boolean,
    hasPrev: Boolean,
    hasNext: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenResize: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenSpeed: () -> Unit,
    onLockToggle: () -> Unit,
    showLanguage: Boolean = false,
    onOpenLanguage: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dimens = rememberPlayerDimens()
    val interactionSource = remember { MutableInteractionSource() }
    val isSliderDragged by interactionSource.collectIsDraggedAsState()

    // Notify parent about dragging changes to pause/resume the auto-hide timer
    LaunchedEffect(isSliderDragged) {
        if (!isSliderDragged) {
            onSeekFinished()
        }
    }

    val qualityChipLabel by remember(selectedQualityLabel) {
        derivedStateOf {
            if (selectedQualityLabel.equals("Auto", ignoreCase = true)) "HD Auto" else selectedQualityLabel
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SCRIM_TOP,
                        Color.Transparent,
                        Color.Transparent,
                        SCRIM_BOTTOM
                    )
                )
            )
    ) {
        if (isLocked) {
            // Locked screen mode: Only render the single lock button on the left center of the screen
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = dimens.horizontalPadding),
                contentAlignment = Alignment.CenterStart
            ) {
                ControlCircleButton(
                    icon = Icons.Default.Lock,
                    contentDescription = "Unlock Controls",
                    size = dimens.topButtonSize,
                    onClick = onLockToggle,
                    isGlass = true
                )
            }
        } else {
            // ─── TOP BAR ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = dimens.topPadding, start = dimens.horizontalPadding, end = dimens.horizontalPadding)
                    .height(dimens.topButtonSize),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ControlCircleButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    size = dimens.topButtonSize,
                    onClick = onBack,
                    isGlass = true
                )

                Text(
                    text = title,
                    color = PLAYER_ICON_WHITE,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quality Selection Pill
                    Box(
                        modifier = Modifier
                            .height(dimens.topButtonSize)
                            .glassPill()
                            .clickable(onClick = onOpenQuality)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = PLAYER_ICON_WHITE,
                                modifier = Modifier.size(dimens.topButtonSize * 0.45f)
                            )
                            Text(
                                text = qualityChipLabel,
                                color = PLAYER_ICON_WHITE,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (pipSupported) {
                        ControlCircleButton(
                            icon = Icons.Default.PictureInPictureAlt,
                            contentDescription = "PiP",
                            size = dimens.topButtonSize,
                            onClick = onEnterPip,
                            isGlass = true
                        )
                    }
                }
            }

            // ─── CENTER SPEED BADGE (DURING 2X) ──────────────────────────────
            if (isSpeedBoosted) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = dimens.topPadding + dimens.topButtonSize + 16.dp)
                        .glassPill(fillColor = Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "2x Speed",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ─── CENTER TRANSPORT CONTROLS ──────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Previous Episode
                if (hasPrev) {
                    ControlCircleButton(
                        icon = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Episode",
                        size = dimens.seekButtonSize * 0.85f,
                        onClick = onPrevEpisode,
                        isGlass = true
                    )
                    Spacer(modifier = Modifier.width(dimens.transportGap * 0.5f))
                }

                // Seek Back 10s
                ControlCircleButton(
                    icon = Icons.Default.Replay10,
                    contentDescription = "Rewind 10s",
                    size = dimens.seekButtonSize,
                    onClick = onSeekBack,
                    isGlass = true
                )

                Spacer(modifier = Modifier.width(dimens.transportGap))

                // Play / Pause
                ControlCircleButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    size = dimens.playButtonSize,
                    onClick = onPlayPause,
                    isGlass = false,
                    // Semi-transparent dark background (was translucent white).
                    accentColor = Color.Black.copy(alpha = 0.35f)
                )

                Spacer(modifier = Modifier.width(dimens.transportGap))

                // Seek Forward 10s
                ControlCircleButton(
                    icon = Icons.Default.Forward10,
                    contentDescription = "Forward 10s",
                    size = dimens.seekButtonSize,
                    onClick = onSeekForward,
                    isGlass = true
                )

                // Next Episode
                if (hasNext) {
                    Spacer(modifier = Modifier.width(dimens.transportGap * 0.5f))
                    ControlCircleButton(
                        icon = Icons.Default.SkipNext,
                        contentDescription = "Next Episode",
                        size = dimens.seekButtonSize * 0.85f,
                        onClick = onNextEpisode,
                        isGlass = true
                    )
                }
            }

            // ─── PROGRESS BAR & BOTTOM CONTROLS ─────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = dimens.controlsBottomPadding)
                    .width(dimens.seekBarWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Slider ──
                val safeDuration = durationMs.coerceAtLeast(1L)
                val thumbScale by animateFloatAsState(
                    targetValue = if (isSliderDragged) 1.4f else 1f,
                    label = "seek_thumb_scale"
                )
                val progressFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

                Slider(
                    value = positionMs.coerceIn(0L, safeDuration).toFloat(),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..safeDuration.toFloat(),
                    interactionSource = interactionSource,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = TRACK_INACTIVE,
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(dimens.thumbSize)
                                .scale(thumbScale)
                                .clip(CircleShape)
                                .background(Color.White),
                        )
                    },
                    track = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimens.trackHeight)
                                .clip(CircleShape)
                                .background(TRACK_INACTIVE),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(dimens.trackHeight)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.seekBarTouchHeight)
                )

                Spacer(modifier = Modifier.height(dimens.seekToTimeGap))

                // ── Time Text ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatPlayerTime(positionMs)} / ${formatPlayerTime(durationMs)}",
                        color = PLAYER_ICON_WHITE,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(dimens.timeToIconsGap))

                // ── Action Buttons ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (dimens.isLandscape) {
                        // Landscape Bottom Controls: Lock | CC | LANG | Quality | Speed | Zoom | Fullscreen
                        ActionIconButton(Icons.Default.LockOpen, "Lock Controls", onLockToggle, dimens)
                        ActionIconButton(Icons.Default.ClosedCaption, "Subtitles", onToggleSubtitles, dimens)
                        if (showLanguage) {
                            ActionIconButton(Icons.Default.Language, "Audio Language", onOpenLanguage, dimens)
                        }
                        ActionIconButton(Icons.Default.HighQuality, "Quality", onOpenQuality, dimens)
                        
                        // Speed selector chip with text label
                        Box(
                            modifier = Modifier
                                .size(dimens.bottomTouchTarget)
                                .clickable(onClick = onOpenSpeed),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Speed",
                                    tint = PLAYER_ICON_WHITE,
                                    modifier = Modifier.size(dimens.bottomIconSize)
                                )
                                Text(
                                    text = currentSpeedLabel,
                                    color = PLAYER_ICON_WHITE,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        ActionIconButton(Icons.Default.ZoomIn, "Zoom/Resize", onOpenResize, dimens)
                        ActionIconButton(
                            icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            label = "Fullscreen",
                            onClick = onToggleFullscreen,
                            dimens = dimens
                        )
                    } else {
                        // Portrait Bottom Controls: CC | LANG | Quality | Resize | Fullscreen
                        ActionIconButton(Icons.Default.ClosedCaption, "Subtitles", onToggleSubtitles, dimens)
                        if (showLanguage) {
                            ActionIconButton(Icons.Default.Language, "Audio Language", onOpenLanguage, dimens)
                        }
                        ActionIconButton(Icons.Default.HighQuality, "Quality", onOpenQuality, dimens)
                        ActionIconButton(Icons.Default.AspectRatio, "Resize", onOpenResize, dimens)
                        ActionIconButton(Icons.Default.ZoomIn, "Zoom", onOpenResize, dimens) // Zoom is alias to resize mode sheet
                        ActionIconButton(
                            icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            label = "Fullscreen",
                            onClick = onToggleFullscreen,
                            dimens = dimens
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlCircleButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    isGlass: Boolean,
    accentColor: Color = Color.Transparent,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1.0f,
        animationSpec = tween(150),
        label = "button_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .then(
                if (isGlass) Modifier.glassSurface(cornerRadius = 50.dp)
                else Modifier.background(accentColor, CircleShape)
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PLAYER_ICON_WHITE,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    dimens: PlayerDimens,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1.0f,
        animationSpec = tween(150),
        label = "action_button_scale"
    )

    Box(
        modifier = Modifier
            .size(dimens.bottomTouchTarget)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PLAYER_ICON_WHITE,
            modifier = Modifier.size(dimens.bottomIconSize)
        )
    }
}
