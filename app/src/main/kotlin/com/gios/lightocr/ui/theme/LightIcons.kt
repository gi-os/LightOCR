package com.gios.lightocr.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.gios.lightocr.R

/**
 * LightOS's own icon set. The vector drawables in `res/drawable/ic_*` are copied from
 * `lightphone/light-sdk` (MIT licence, (c) 2026 The Light Phone -- see LICENSE-light-sdk);
 * a hand-drawn back chevron never quite matches the native look.
 */
class LightIconSpec(val name: String, @DrawableRes val res: Int)

object LightIcons {
    val Camera = LightIconSpec("camera", R.drawable.ic_camera)

    /** The album glyph LightOS uses for "open a picture from the gallery". */
    val Gallery = LightIconSpec("gallery", R.drawable.ic_camera_landscape)
    val Back = LightIconSpec("back", R.drawable.ic_back_white)

    /** Reused as "copy to clipboard" -- the SDK has no dedicated clipboard glyph. */
    val Copy = LightIconSpec("copy", R.drawable.ic_accept_white)
    val Trash = LightIconSpec("delete", R.drawable.ic_trash)
    val Close = LightIconSpec("close", R.drawable.ic_close_white)
}

@Composable
fun LightIcon(
    icon: LightIconSpec,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Icon(
        painter = painterResource(icon.res),
        contentDescription = icon.name,
        modifier = modifier.size(size),
        tint = tint ?: LightThemeTokens.colors.content,
    )
}
