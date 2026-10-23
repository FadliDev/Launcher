package com.sally.freshlauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Beranda", Icons.Filled.Home),
    VERSIONS("Versi", Icons.Filled.Layers),
    ACCOUNT("Akun", Icons.Filled.Person),
    SETTINGS("Pengaturan", Icons.Filled.Settings)
}

@Composable
fun FreshApp(vm: LauncherViewModel = viewModel()) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val current = Tab.entries[index]

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = i == index,
                        onClick = { index = i },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (current) {
                Tab.HOME -> HomeScreen(vm) { index = it }
                Tab.VERSIONS -> VersionsScreen(vm)
                Tab.ACCOUNT -> AccountScreen(vm)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}
