package com.sally.ember

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sally.ember.data.Paths
import com.sally.ember.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Paths.init(this)
        enableEdgeToEdge()
        setContent {
            EmberTheme {
                val vm: AppViewModel = viewModel()
                val ctx = LocalContext.current
                var tab by remember { mutableIntStateOf(0) }
                val tabs = listOf("Home" to Icons.Default.Home, "Versi" to Icons.Default.Layers,
                    "Akun" to Icons.Default.Person, "Setelan" to Icons.Default.Settings)
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            tabs.forEachIndexed { i, (l, ic) ->
                                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(ic, l) }, label = { Text(l) })
                            }
                        }
                    }
                ) { pad ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                        when (tab) {
                            0 -> HomeScreen(vm, ctx)
                            1 -> VersionsScreen(vm)
                            2 -> AccountsScreen(vm)
                            else -> SettingsScreen(vm, ctx)
                        }
                    }
                }
            }
        }
    }
}
