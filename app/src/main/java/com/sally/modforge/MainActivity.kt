package com.sally.modforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sally.modforge.ui.ModForgeRoot
import com.sally.modforge.ui.theme.ModForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ModForgeTheme {
                ModForgeRoot()
            }
        }
    }
}
