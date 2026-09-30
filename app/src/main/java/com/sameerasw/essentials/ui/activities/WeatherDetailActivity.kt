package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sameerasw.essentials.ui.features.weather.WeatherDetailSheet

class WeatherDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WeatherDetailSheet(onDismissRequest = { finish() })
        }
    }
}
