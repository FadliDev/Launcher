package com.sally.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.BufferedReader
import java.io.InputStreamReader

data class CodeSnippet(
    val prefix: String,
    val description: String,
    val body: String
)

class MainActivity : AppCompatActivity() {
    private lateinit var editor: CodeEditor
    private lateinit var filename: TextView
    private lateinit var status: TextView
    private var currentUri: Uri? = null
    private var currentName = "Untitled.txt"
    private var dirty = false
    private var loadingDocument = false
    private var textMateReady = false
    private lateinit var snippetRepository: FriendlySnippetRepository

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) { }
            readDocument(uri)
        }
    }

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            currentUri = uri
            writeDocument(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivitiesIfAvailable(application)
        super.onCreate(savedInstanceState)
        initializeTextMate()
        snippetRepository = FriendlySnippetRepository(this)
        setContentView(createContent())
        setEditorLanguage()
        updateStatus()
    }

    private fun initializeTextMate() {
        try {
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(applicationContext.assets))
            val themeName = "sally-dark"
            val themePath = "textmate/$themeName.json"
            val stream = assets.open(themePath)
            val source = IThemeSource.fromInputStream(stream, themePath, null)
            ThemeRegistry.getInstance().loadTheme(ThemeModel(source, themeName))
            ThemeRegistry.getInstance().setTheme(themeName)
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            textMateReady = true
        } catch (error: Exception) {
            // Keep the editor usable as plain text if a third-party grammar/theme fails to load.
            android.util.Log.e("SallyTextEditor", "TextMate initialization failed", error)
            toast("Syntax pack unavailable; opening plain text mode")
        }
    }

    private fun createContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#111318"))
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Sally Text Editor"
            subtitle = "Code workspace"
            setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
            setNavigationOnClickListener { showFileMenu() }
            setTitleTextColor(android.graphics.Color.WHITE)
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(60)))

        val fileHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        filename = TextView(this).apply {
            text = currentName
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.parseColor("#D0BCFF"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }
        fileHeader.addView(filename, LinearLayout.LayoutParams(0, -2, 1f))
        fileHeader.addView(button("Save") { saveFile() })
        fileHeader.addView(button("Snippets") { showSnippets() })
        root.addView(fileHeader)

        editor = CodeEditor(this).apply {
            setTypefaceText(Typeface.MONOSPACE)
            setTypefaceLineNumber(Typeface.MONOSPACE)
            setTextSize(15f)
            setWordwrap(true)
            setText("// Welcome to Sally Text Editor\n// Syntax highlighting, autocomplete and snippets are powered by Sora Editor + TextMate.\n\n")
            if (textMateReady) {
                setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()))
            }
            setBackgroundColor(android.graphics.Color.parseColor("#111318"))
            subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                if (!loadingDocument) dirty = true
                updateStatus()
            }
        }
        root.addView(editor, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }
        status = TextView(this).apply {
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#CAC4D0"))
            maxLines = 1
        }
        bottom.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        bottom.addView(button("Open") { openDocument.launch(arrayOf("*/*")) })
        bottom.addView(button("New") { newFile() })
        root.addView(bottom)
        return root
    }

    private fun button(label: String, action: () -> Unit) = MaterialButton(
        this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        text = label
        textSize = 12f
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun setEditorLanguage() {
        if (!textMateReady) return
        val requestedName = currentName
        applyEditorLanguage(snippetsFor(requestedName))
        // If this extension has no bundled/cached pack, fetch it automatically in the background.
        // The current file stays editable while the download runs.
        snippetRepository.ensureForFile(requestedName) { snippets ->
            runOnUiThread {
                if (!isFinishing && !isDestroyed && currentName == requestedName && snippets.isNotEmpty()) {
                    applyEditorLanguage(snippets)
                    toast("Snippet pack ready for $requestedName")
                }
            }
        }
    }

    private fun applyEditorLanguage(snippets: List<CodeSnippet>) {
        if (!textMateReady || !::editor.isInitialized) return
        try {
            editor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()))
            editor.setEditorLanguage(SallyTextMateLanguage(snippets))
        } catch (error: Exception) {
            android.util.Log.e("SallyTextEditor", "Could not attach TextMate language", error)
        }
    }

    private fun showFileMenu() {
        val items = arrayOf("New document", "Open file", "Save", "Save as")
        MaterialAlertDialogBuilder(this)
            .setTitle("File")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> newFile()
                    1 -> openDocument.launch(arrayOf("*/*"))
                    2 -> saveFile()
                    3 -> createDocument.launch(currentName)
                }
            }.show()
    }

    private fun newFile() {
        if (dirty) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Unsaved changes")
                .setMessage("Discard the current changes and create a new document?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Discard") { _, _ -> resetDocument() }
                .show()
        } else resetDocument()
    }

    private fun resetDocument() {
        currentUri = null
        currentName = "Untitled.txt"
        loadingDocument = true
        editor.setText("")
        loadingDocument = false
        dirty = false
        filename.text = currentName
        setEditorLanguage()
        updateStatus()
    }

    private fun readDocument(uri: Uri) {
        try {
            val content = contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            } ?: ""
            currentUri = uri
            currentName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Opened file"
            loadingDocument = true
            editor.setText(content)
            loadingDocument = false
            dirty = false
            filename.text = currentName
            setEditorLanguage()
            updateStatus()
            toast("Opened $currentName")
        } catch (error: Exception) {
            loadingDocument = false
            toast("Couldn't open file: ${error.localizedMessage ?: "unknown error"}")
        }
    }

    private fun saveFile() {
        val uri = currentUri
        if (uri == null) createDocument.launch(currentName) else writeDocument(uri)
    }

    private fun writeDocument(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(editor.text.toString())
            } ?: throw IllegalStateException("Unable to open output stream")
            currentUri = uri
            currentName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: currentName
            dirty = false
            filename.text = currentName
            setEditorLanguage()
            updateStatus()
            toast("Saved successfully")
        } catch (error: Exception) {
            toast("Couldn't save file: ${error.localizedMessage ?: "unknown error"}")
        }
    }

    private fun showSnippets() {
        val fileForRequest = currentName
        val items = snippetsFor(fileForRequest)
        if (items.isNotEmpty()) {
            displaySnippets(items)
            return
        }
        toast("Mencari snippet untuk $fileForRequest…")
        snippetRepository.ensureForFile(fileForRequest) { downloaded ->
            runOnUiThread {
                if (isFinishing || isDestroyed || currentName != fileForRequest) return@runOnUiThread
                if (downloaded.isEmpty()) {
                    toast("Snippet untuk bahasa ini tidak ditemukan atau internet tidak tersedia")
                } else {
                    applyEditorLanguage(downloaded)
                    displaySnippets(downloaded)
                }
            }
        }
    }

    private fun displaySnippets(items: List<CodeSnippet>) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Insert snippet · Friendly Snippets")
            .setItems(items.map { "${it.prefix} — ${it.description}" }.toTypedArray()) { _, index ->
                try {
                    editor.insertText(items[index].body, items[index].body.length)
                    editor.requestFocus()
                } catch (error: Exception) {
                    toast("Couldn't insert snippet: ${error.localizedMessage ?: "unknown error"}")
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun updateStatus() {
        if (!::status.isInitialized || !::editor.isInitialized) return
        val content = editor.text.toString()
        val lines = if (content.isEmpty()) 1 else content.count { it == '\n' } + 1
        status.text = "$lines lines • ${content.length} chars${if (dirty) " • Modified" else ""}"
        filename.text = currentName
    }

    override fun onDestroy() {
        if (::editor.isInitialized) editor.release()
        super.onDestroy()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun snippetsFor(fileName: String): List<CodeSnippet> = snippetRepository.forFile(fileName)
}
