package com.sally.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sally.ide.ui.IdeScreen
import com.sally.ide.ui.SallyTheme

class MainActivity : ComponentActivity() {
    private val vm: IdeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            SallyTheme(prefs.themeMode, prefs.dynamicColor) { IdeScreen(vm) }
        }
    }

    override fun onStop() {
        vm.saveAll() // auto-save saat app ke background
        super.onStop()
    }
}
