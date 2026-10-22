package com.sally.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sally.ide.ui.CrashScreen
import com.sally.ide.ui.IdeScreen
import com.sally.ide.ui.SallyTheme

class MainActivity : ComponentActivity() {
    private val vm: IdeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            var crash by remember { mutableStateOf(CrashLog.read(this)) }
            SallyTheme(prefs.themeMode, prefs.dynamicColor) {
                val c = crash
                if (c != null) {
                    CrashScreen(
                        c,
                        onSafe = { CrashLog.setSafe(this, true); CrashLog.clear(this); crash = null },
                        onRetry = { CrashLog.setSafe(this, false); CrashLog.clear(this); crash = null },
                    )
                } else IdeScreen(vm)
            }
        }
    }

    override fun onStop() {
        vm.saveAll() // auto-save saat app ke background
        super.onStop()
    }
}
