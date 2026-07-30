package com.gios.lightocr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gios.lightocr.ui.theme.LightIcon
import com.gios.lightocr.ui.theme.LightIconSpec
import com.gios.lightocr.ui.theme.LightText
import com.gios.lightocr.ui.theme.LightTextVariant
import com.gios.lightocr.ui.theme.LightThemeTokens
import com.gios.lightocr.ui.theme.gridUnitsAsDp
import com.gios.lightocr.ui.theme.lightClickable
import com.gios.lightocr.ui.theme.verticalGridUnitsAsDp

/**
 * LightOS's bar chrome, approximated faithfully from the real tokens (`LightGrid`, the
 * named type scale, [lightClickable]) rather than pulled in as the SDK's own
 * `LightBottomBar`/`LightTopBar` composables, which live in a Gradle module this plain APK
 * doesn't depend on. Sizes and behaviour mirror `lightphone/light-sdk`'s
 * `sdk/ui/src/main/kotlin/com/thelightphone/sdk/ui/{LightBottomBar,LightTopBar}.kt`: a top
 * bar 3 grid units tall, a bottom bar 4, both padded 1 grid unit horizontally, no ripples.
 */

private const val TOP_BAR_HEIGHT_UNITS = 3f
private const val BOTTOM_BAR_HEIGHT_UNITS = 4f
private const val HORIZONTAL_INSET_UNITS = 1f
private const val BAR_ICON_SIZE_UNITS = 2f

@Composable
fun LightTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val barHeight = TOP_BAR_HEIGHT_UNITS.gridUnitsAsDp()
    val inset = HORIZONTAL_INSET_UNITS.gridUnitsAsDp()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .padding(horizontal = inset),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.height(barHeight), contentAlignment = Alignment.CenterStart) {
                if (onBack != null) {
                    ChromeIcon(icon = com.gios.lightocr.ui.theme.LightIcons.Back, onClick = onBack)
                }
            }
            Box(Modifier.weight(1f))
            Box(Modifier.height(barHeight), contentAlignment = Alignment.CenterEnd) {
                trailing?.invoke()
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LightText(text = title, variant = LightTextVariant.Fine)
        }
    }
}

sealed interface BottomTab {
    val label: String

    data class Item(override val label: String, val selected: Boolean, val onClick: () -> Unit) : BottomTab
}

/** Two text items -- well inside LightOS's own "at most 3 items when any is text" rule. */
@Composable
fun LightBottomBar(items: List<BottomTab.Item>, modifier: Modifier = Modifier) {
    val barHeight = BOTTOM_BAR_HEIGHT_UNITS.gridUnitsAsDp()
    val inset = HORIZONTAL_INSET_UNITS.gridUnitsAsDp() * 2
    val colors = LightThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = HORIZONTAL_INSET_UNITS.verticalGridUnitsAsDp())
            .height(barHeight)
            .padding(horizontal = inset),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            LightText(
                text = item.label.uppercase(),
                variant = LightTextVariant.Button,
                color = if (item.selected) colors.content else colors.contentSecondary,
                modifier = Modifier.lightClickable { item.onClick() },
            )
        }
    }
}

/** An icon that behaves: no ripple, a buzz on finger-down, a generous invisible target. */
@Composable
fun ChromeIcon(
    icon: LightIconSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lighten: Boolean = false,
    size: androidx.compose.ui.unit.Dp? = null,
    enabled: Boolean = true,
) {
    val colors = LightThemeTokens.colors
    val resolvedSize = size ?: BAR_ICON_SIZE_UNITS.gridUnitsAsDp()
    Box(
        modifier = modifier
            .lightClickable(enabled = enabled) { onClick() }
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        LightIcon(
            icon = icon,
            size = resolvedSize,
            tint = if (!enabled) colors.rule else if (lighten) colors.contentSecondary else colors.content,
        )
    }
}

@Composable
fun EmptyState(text: String, detail: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LightText(text, LightTextVariant.Subheading, align = TextAlign.Center)
        if (detail != null) {
            LightText(
                detail,
                LightTextVariant.Paragraph,
                lighten = true,
                align = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
