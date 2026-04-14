package com.juliencardoux.openwol.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark mode brand — gold ────────────────────────────────────────────────────
val Gold   = Color(0xFFFFC300)
val OnGold = Color(0xFF1A1200)   // near-black — icon/text on gold

// ── Light mode brand — deep royal blue ───────────────────────────────────────
val Blue          = Color(0xFF0E3D93)
val BlueDim       = Color(0xFF4B6CB7)   // muted blue — secondary
val BlueContainer = Color(0xFFD6E3FF)   // light blue tint — primaryContainer
val OnBlue        = Color(0xFFFFFFFF)   // white on blue
val OnBlueContainer = Color(0xFF001946) // dark navy — text on blueContainer
val BlueInverse   = Color(0xFFA9C7FF)   // light blue — inversePrimary

// ── Dark surfaces — near-neutral dark grey, barely warm ──────────────────────
val DarkBg                = Color(0xFF181818)   // neutral dark, imperceptible warmth
val DarkContainerLow      = Color(0xFF1D1C1C)
val DarkContainer         = Color(0xFF212120)
val DarkContainerHigh     = Color(0xFF272625)   // bottom bar + automation cards
val DarkContainerHighest  = Color(0xFF2E2D2B)   // automate panel + menu — less yellow, darker
val DarkSurfaceVariant    = Color(0xFF3C3B39)
val OnDark                = Color(0xFFE7E2DA)
val OnDarkVariant         = Color(0xFFC9C2AE)   // warm grey — secondary icons/text
val DarkOutline           = Color(0xFF938E84)
val DarkOutlineVariant    = Color(0xFF3C3B39)

// ── Light surfaces — standard M3 light grey with the faintest warm nudge ─────
val LightBg               = Color(0xFFFFFCF8)   // was #FFFBFF, barely warmer
val LightContainerLow     = Color(0xFFF5F2EE)
val LightContainer        = Color(0xFFF0EDE8)
val LightContainerHigh    = Color(0xFFEAE7E1)   // bottom bar + automation cards
val LightContainerHighest = Color(0xFFE3DFD7)   // automate panel + menu
val LightSurfaceVariant   = Color(0xFFE8E4DC)
val OnLight               = Color(0xFF1C1B19)
val OnLightVariant        = Color(0xFF5E5A54)   // warm dark grey — secondary icons/text
val LightOutline          = Color(0xFF8D8980)
val LightOutlineVariant   = Color(0xFFCDC9C1)

// ── Errors ───────────────────────────────────────────────────────────────────
val ErrorDark             = Color(0xFFFF5449)
val OnErrorDark           = Color(0xFF690005)
val ErrorContainerDark    = Color(0xFF93000A)
val OnErrorContainerDark  = Color(0xFFFFBBB6)

val ErrorLight            = Color(0xFFB3261E)
val OnErrorLight          = Color(0xFFFFFFFF)
val ErrorContainerLight   = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
