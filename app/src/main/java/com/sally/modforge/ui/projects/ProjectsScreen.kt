package com.sally.modforge.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sally.modforge.model.ProjectConfig

@Composable
fun ProjectsScreen(vm: ProjectsViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    if (ui.creating) {
        NewProjectWizard(vm)
        return
    }

    Box(Modifier.fillMaxSize()) {
        if (ui.projects.isEmpty()) {
            EmptyProjects(onCreate = vm::openWizard)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("Projects", style = MaterialTheme.typography.titleLarge) }
                items(ui.projects, key = { it.id }) { ProjectCard(it) }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }

        FloatingActionButton(
            onClick = vm::openWizard,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Project baru")
        }
    }
}

@Composable
private fun EmptyProjects(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Belum ada project", style = MaterialTheme.typography.titleMedium)
        Text(
            "Project disimpan di HP. Sinkron ke GitHub menyusul di tahap berikutnya.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onCreate, modifier = Modifier.padding(top = 24.dp)) {
            Text("Buat mod pertama")
        }
    }
}

@Composable
private fun ProjectCard(p: ProjectConfig) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium)
            Text(
                p.modId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoChip("MC ${p.minecraft}")
                InfoChip("Java ${p.java}")
                InfoChip("Gradle ${p.gradle}")
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    AssistChip(onClick = {}, label = { Text(text) })
}
