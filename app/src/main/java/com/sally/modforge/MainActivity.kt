package com.sally.modforge

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.SideEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sally.modforge.data.ModForgeController
import com.sally.modforge.ui.ModForgeApp
import com.sally.modforge.ui.ModForgeTheme

class MainActivity : ComponentActivity() {
    private lateinit var controller: ModForgeController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = ModForgeController(applicationContext)
        setContent {
            ModForgeTheme(darkTheme = controller.isDark) {
                SideEffect {
                    window.statusBarColor = android.graphics.Color.parseColor(if (controller.isDark) "#0F1115" else "#FAFAFA")
                    window.navigationBarColor = android.graphics.Color.parseColor(if (controller.isDark) "#0F1115" else "#FAFAFA")
                    @Suppress("DEPRECATION")
                    run {
                        window.decorView.systemUiVisibility = if (controller.isDark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    }
                }
                ModForgeApp(controller)
            }
        }
    }

    override fun onDestroy() {
        if (::controller.isInitialized) controller.close()
        super.onDestroy()
    }
}
