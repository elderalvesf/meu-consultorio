package com.meuconsultorio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.meuconsultorio.ui.navigation.AppNavigation
import com.meuconsultorio.ui.theme.MeuConsultorioTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeuConsultorioTheme {
                AppNavigation()
            }
        }
    }
}
