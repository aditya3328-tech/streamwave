package com.example.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════════
// PREMIUM PLAYER DESIGN SYSTEM
// Netflix / JioCinema / MX Player inspired responsive design tokens.
// Every dimension is derived from screen percentages — ZERO fixed sizes.
// ═══════════════════════════════════════════════════════════════════════════════

// ─── Color Palette ──────────────────────────────────────────────────────────
// Premium dark theme anchored on near-true-black (#0B0B0B).

/** Primary background — near-AMOLED black for the deepest, most cinematic feel. */
val PLAYER_BG = Color(0xFF0B0B0B)

/** Surface for controls, cards, and elevated elements. */
val PLAYER_SURFACE = Color(0xFF141414)

/** Subtle surface for hover / pressed states. */
val PLAYER_SURFACE_VARIANT = Color(0xFF1A1A1A)

/** Glassmorphism: semi-transparent white fill. */
val GLASS_FILL = Color(0x14FFFFFF)          // ~8% white

/** Glassmorphism: slightly brighter fill for interactive elements. */
val GLASS_FILL_HOVER = Color(0x1FFFFFFF)    // ~12% white

/** Glassmorphism: subtle white border for depth. */
val GLASS_STROKE = Color(0x19FFFFFF)        // ~10% white

/** Primary icon / text on controls — white at 85% opacity. */
val PLAYER_ICON_WHITE = Color(0xD9FFFFFF)   // 85%

/** Secondary / muted text. */
val PLAYER_TEXT_MUTED = Color(0x99FFFFFF)   // 60%

/** Scrim: top gradient anchor. */
val SCRIM_TOP = Color(0x99000000)           // 60% black

/** Scrim: bottom gradient anchor (heavier for readability over bright video). */
val SCRIM_BOTTOM = Color(0xCC000000)        // 80% black

/** Progress bar inactive track. */
val TRACK_INACTIVE = Color(0x47FFFFFF)      // 28% white

/** Minimum touch target per Material 3 accessibility guidelines. */
val MIN_TOUCH = 48.dp

/** Standard corner radius for glassmorphism elements. */
val GLASS_CORNER = 24.dp

/** Auto-hide timeout in milliseconds (7 seconds). */
const val AUTO_HIDE_MS = 7000L

/** Fade animation duration in milliseconds. */
const val FADE_ANIM_MS = 300

// ─── Window Size Class ──────────────────────────────────────────────────────
// Mirrors the Material 3 WindowSizeClass concept without pulling in the
// optional library — keeps the dependency footprint minimal.

enum class PlayerWindowSizeClass {
    /** Phones in portrait or landscape (shortest dimension < 600dp). */
    COMPACT,
    /** Small tablets, large phones unfolded, foldables (600–840dp). */
    MEDIUM,
    /** Large tablets, desktop (840dp+). */
    EXPANDED,
}

/**
 * Determines the [PlayerWindowSizeClass] from the current screen width.
 * Uses `LocalConfiguration.screenWidthDp` which updates on rotation and
 * foldable posture changes without needing the Jetpack WindowManager library.
 */
@Composable
fun rememberWindowSizeClass(): PlayerWindowSizeClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp < 600  -> PlayerWindowSizeClass.COMPACT
            widthDp < 840  -> PlayerWindowSizeClass.MEDIUM
            else           -> PlayerWindowSizeClass.EXPANDED
        }
    }
}

// ─── Responsive Dimensions ──────────────────────────────────────────────────
// All values are computed from screen width/height percentages so the player
// scales smoothly from 320dp phones to 1200dp+ tablets without any breakpoint
// jumps. The [PlayerDimens] instance is remembered and recomputed only when
// the configuration (orientation / screen size) changes.

@Immutable
data class PlayerDimens(
    // ── Screen reference ────────────────────────────────────────────────
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isLandscape: Boolean,

    // ── Top bar ─────────────────────────────────────────────────────────
    /** Padding from the top safe area to the first control row. */
    val topPadding: Dp,
    /** Horizontal padding for the top bar and bottom action row. */
    val horizontalPadding: Dp,
    /** Size of circular buttons (back, menu) in the top bar. */
    val topButtonSize: Dp,
    /** Maximum width for the centered title (prevents overlap with edges). */
    val titleMaxWidth: Dp,

    // ── Center transport ────────────────────────────────────────────────
    /** Size of the play/pause button. */
    val playButtonSize: Dp,
    /** Size of the 10s rewind / forward buttons. */
    val seekButtonSize: Dp,
    /** Horizontal gap between seek buttons and the play button. */
    val transportGap: Dp,

    // ── Progress bar ────────────────────────────────────────────────────
    /** Width of the seek bar (percentage of screen). */
    val seekBarWidth: Dp,
    /** Height of the track line. */
    val trackHeight: Dp,
    /** Diameter of the thumb circle. */
    val thumbSize: Dp,
    /** Total touchable height of the slider row. */
    val seekBarTouchHeight: Dp,

    // ── Bottom action row ───────────────────────────────────────────────
    /** Icon size for bottom action buttons (CC, Quality, etc.). */
    val bottomIconSize: Dp,
    /** Touch target size for bottom action buttons. */
    val bottomTouchTarget: Dp,
    /** Bottom padding from navigation bar safe area. */
    val controlsBottomPadding: Dp,

    // ── Spacing ─────────────────────────────────────────────────────────
    /** Gap between the seek bar and the time text row. */
    val seekToTimeGap: Dp,
    /** Gap between the time text row and the bottom icons. */
    val timeToIconsGap: Dp,
    /** Extra bottom offset for the center transport (clears the progress area). */
    val transportBottomOffset: Dp,
)

/**
 * Creates and remembers a [PlayerDimens] instance computed from the current
 * screen configuration. All values are derived from width/height percentages:
 *
 *  - Play button = 10% of screen width (min 48dp)
 *  - Seek buttons = 8% of screen width (min 48dp)
 *  - Track height = 0.6% of screen width
 *  - Thumb size = 3.2% of screen width
 *  - Bottom icons = 4.5% of screen width
 *  - Horizontal padding = 5% of screen width
 *  - Top padding = 4% of screen height
 *  - etc.
 */
@Composable
fun rememberPlayerDimens(): PlayerDimens {
    val config = LocalConfiguration.current
    val sw = config.screenWidthDp.dp
    val sh = config.screenHeightDp.dp
    val isLand = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    return remember(sw, sh, isLand) {
        // Small phones (6.1") vs larger phones (6.7"). Tablets take the landscape branch
        // or slightly larger portrait sizes automatically via this flag.
        val compact = sw < 400.dp

        // ── Top bar ──────────────────────────────────────────────────────────
        // Respect the status bar via statusBarsPadding() in the control layer; this is
        // only the extra gap below it. Kept modest so the bar hugs the top safe area.
        val topPad = coerceMin(sh * 0.03f, 12.dp)
        val hPad = if (isLand) 24.dp else 16.dp
        // Slightly smaller than the old flat 48–85dp so the top row feels lighter.
        val topBtn = if (isLand) 46.dp else 42.dp
        val titleMax = sw * 0.5f

        // ── Center transport ─────────────────────────────────────────────────
        // Portrait: 64dp on <400dp phones, 72dp otherwise (72dp is the max).
        // Landscape: 80dp. This replaces the old width-percentage formula that
        // ballooned the play button to ~85dp in landscape.
        val playBtn = if (isLand) 80.dp else if (compact) 64.dp else 72.dp
        // Rewind / forward circles: 56dp portrait, 60dp landscape.
        val seekBtn = if (isLand) 60.dp else 56.dp
        // Equal spacing applied on BOTH sides of the play button (see PlayerControls).
        val tGap = if (isLand) 40.dp else 28.dp

        // ── Progress bar ─────────────────────────────────────────────────────
        // 92% width portrait, 95% landscape.
        val sbWidth = if (isLand) sw * 0.95f else sw * 0.92f
        val trkH = 3.dp
        val thumb = if (isLand) 14.dp else 12.dp
        val sbTouch = 40.dp

        // ── Bottom action row ────────────────────────────────────────────────
        // 28dp icons portrait (26dp landscape to fit the extra Lock/Speed items),
        // inside comfortable touch targets. Row centering is handled by the layout.
        val btmIcon = if (isLand) 26.dp else 28.dp
        val btmTouch = if (isLand) 48.dp else 52.dp
        // Minimum 24dp bottom padding portrait, 16dp landscape (on top of the
        // navigation-bar inset applied in the control layer).
        val ctrlBtmPad = if (isLand) coerceMin(sh * 0.04f, 16.dp) else coerceMin(sh * 0.03f, 24.dp)

        // ── Spacing ──────────────────────────────────────────────────────────
        val seekTimeGap = 6.dp
        val timeIconsGap = if (isLand) 6.dp else 10.dp
        // Reserved vertical span of the bottom cluster (kept for the data-class contract).
        val transportBtm = playBtn + sbTouch + btmTouch + ctrlBtmPad + 48.dp

        PlayerDimens(
            screenWidth = sw,
            screenHeight = sh,
            isLandscape = isLand,
            topPadding = topPad,
            horizontalPadding = hPad,
            topButtonSize = topBtn,
            titleMaxWidth = titleMax,
            playButtonSize = playBtn,
            seekButtonSize = seekBtn,
            transportGap = tGap,
            seekBarWidth = sbWidth,
            trackHeight = trkH,
            thumbSize = thumb,
            seekBarTouchHeight = sbTouch,
            bottomIconSize = btmIcon,
            bottomTouchTarget = btmTouch,
            controlsBottomPadding = ctrlBtmPad,
            seekToTimeGap = seekTimeGap,
            timeToIconsGap = timeIconsGap,
            transportBottomOffset = transportBtm,
        )
    }
}

// ─── Glassmorphism Modifiers ────────────────────────────────────────────────
// Reusable modifiers that apply the frosted-glass look: semi-transparent fill,
// subtle white border, and generously rounded corners.

/**
 * Glassmorphism surface — semi-transparent white fill + border + rounded corners.
 * Use for control backgrounds, overlays, and interactive chips.
 */
fun Modifier.glassSurface(
    cornerRadius: Dp = GLASS_CORNER,
    fillColor: Color = GLASS_FILL,
    borderColor: Color = GLASS_STROKE,
    borderWidth: Dp = 1.dp,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(fillColor)
    .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))

/**
 * Glassmorphism pill — fully rounded (50%) for chips and badges.
 */
fun Modifier.glassPill(
    fillColor: Color = GLASS_FILL,
    borderColor: Color = GLASS_STROKE,
): Modifier = this
    .clip(RoundedCornerShape(50))
    .background(fillColor)
    .border(1.dp, borderColor, RoundedCornerShape(50))

// ─── Utility ────────────────────────────────────────────────────────────────

/** Ensures a dimension never drops below [min]. */
private fun coerceMin(value: Dp, min: Dp): Dp = if (value < min) min else value

/**
 * Formats milliseconds into a human-readable time string.
 * - Under 1 hour: "m:ss" (e.g. "4:23")
 * - 1 hour or more: "h:mm:ss" (e.g. "1:04:23")
 */
fun formatPlayerTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
