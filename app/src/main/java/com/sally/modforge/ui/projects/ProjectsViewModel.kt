package com.sally.modforge.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sally.modforge.ModForgeApp
import com.sally.modforge.model.ProjectConfig
import com.sally.modforge.model.ProjectFactory
import com.sally.modforge.model.ProjectStore
import com.sally.modforge.model.Severity
import com.sally.modforge.model.ValidationIssue
import com.sally.modforge.model.VersionMatrix
import com.sally.modforge.model.validate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ProjectsUiState(
    val projects: List<ProjectConfig> = emptyList(),
    val creating: Boolean = false,
    val step: Int = 0,
    val name: String = "",
    val modId: String = "",
    val modIdEdited: Boolean = false,
    val packageName: String = "",
    val packageEdited: Boolean = false,
    val minecraft: String = "",
    val java: Int = 0,
    val template: String = "empty",
    val issues: List<ValidationIssue> = emptyList(),
)

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ModForgeApp
    private val store = ProjectStore(File(app.filesDir, "projects"))
    private val _ui = MutableStateFlow(ProjectsUiState())
    val ui: StateFlow<ProjectsUiState> = _ui.asStateFlow()

    val matrix: VersionMatrix get() = app.versions.matrix.value

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { store.list() }
            _ui.update { it.copy(projects = list) }
        }
    }

    private fun draft(s: ProjectsUiState = _ui.value): ProjectConfig = ProjectFactory.create(
        id = "draft",
        name = s.name.trim(),
        modId = s.modId.trim(),
        packageName = s.packageName.trim(),
        minecraft = s.minecraft,
        java = s.java,
        matrix = matrix,
    )

    private fun issuesFor(s: ProjectsUiState = _ui.value): List<ValidationIssue> =
        validate(draft(s), matrix)

    fun openWizard() {
        val first = matrix.versions.firstOrNull { it.stability == "stable" } ?: matrix.versions.firstOrNull()
        _ui.update {
            it.copy(
                creating = true,
                step = 0,
                name = "",
                modId = "",
                modIdEdited = false,
                packageName = "",
                packageEdited = false,
                minecraft = first?.minecraft.orEmpty(),
                java = first?.javaRecommended ?: 0,
                template = "empty",
                issues = emptyList(),
            )
        }
    }

    fun closeWizard() {
        _ui.update { it.copy(creating = false, issues = emptyList()) }
    }

    fun back() {
        _ui.update { it.copy(step = (it.step - 1).coerceAtLeast(0), issues = emptyList()) }
    }

    fun onName(value: String) {
        val s = _ui.value
        val newModId = if (s.modIdEdited) s.modId else ProjectFactory.slugModId(value)
        val newPackage = if (s.packageEdited) s.packageName else ProjectFactory.defaultPackage(newModId)
        _ui.update { it.copy(name = value, modId = newModId, packageName = newPackage) }
    }

    fun onModId(value: String) {
        val s = _ui.value
        val newPackage = if (s.packageEdited) s.packageName else ProjectFactory.defaultPackage(value)
        _ui.update { it.copy(modId = value, modIdEdited = true, packageName = newPackage) }
    }

    fun onPackage(value: String) {
        _ui.update { it.copy(packageName = value, packageEdited = true) }
    }

    fun onMinecraft(version: String) {
        val entry = matrix.find(version)
        _ui.update {
            it.copy(minecraft = version, java = entry?.javaRecommended ?: it.java, issues = emptyList())
        }
    }

    fun onJava(java: Int) {
        _ui.update { it.copy(java = java, issues = emptyList()) }
    }

    fun onTemplate(template: String) {
        _ui.update { it.copy(template = template) }
    }

    fun next() {
        val s = _ui.value
        val all = issuesFor(s)
        val blocking = when (s.step) {
            0 -> all.filter { it.severity == Severity.ERROR && it.field in BASIC_FIELDS }
            1 -> all.filter { it.severity == Severity.ERROR }
            else -> emptyList()
        }
        if (blocking.isNotEmpty()) {
            _ui.update { it.copy(issues = blocking) }
            return
        }
        _ui.update { it.copy(step = s.step + 1, issues = emptyList()) }
    }

    fun create() {
        val s = _ui.value
        val errors = issuesFor(s).filter { it.severity == Severity.ERROR }
        if (errors.isNotEmpty()) {
            _ui.update { it.copy(issues = errors) }
            return
        }
        val config = draft(s).copy(id = UUID.randomUUID().toString())
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.save(config) }
            _ui.update { it.copy(creating = false, issues = emptyList()) }
            reload()
        }
    }

    fun applyFix(issue: ValidationIssue) {
        val fix = issue.fixedJava ?: return
        _ui.update { it.copy(java = fix, issues = emptyList()) }
    }

    private companion object {
        val BASIC_FIELDS = setOf("name", "modId", "package")
    }
}
