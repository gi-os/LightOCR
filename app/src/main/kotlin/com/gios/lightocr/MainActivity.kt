package com.gios.lightocr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gios.lightocr.ui.LightOcrNav
import com.gios.lightocr.ui.theme.LightOcrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LightOcrTheme {
                LightOcrNav()
            }
        }
    }
}
