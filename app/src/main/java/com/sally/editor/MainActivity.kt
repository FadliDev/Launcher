package com.sally.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.color.DynamicColors
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var editor: EditText
    private lateinit var lineNumbers: LineNumberView
    private lateinit var status: TextView
    private lateinit var filename: TextView
    private var currentUri: Uri? = null
    private var currentName = "Untitled.txt"
    private var dirty = false
    private var suppressDirty = false

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
        setContentView(createContent())
        updateStatus()
    }

    private fun createContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#F7F2FA"))
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Sally Text Editor"
            subtitle = "A clean workspace"
            setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
            setNavigationOnClickListener { showFileMenu() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))

        val fileHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        filename = TextView(this).apply {
            text = currentName
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.parseColor("#6750A4"))
        }
        fileHeader.addView(filename, LinearLayout.LayoutParams(0, -2, 1f))
        val saveButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Save"
            isAllCaps = false
            setOnClickListener { saveFile() }
        }
        fileHeader.addView(saveButton)
        root.addView(fileHeader)

        val editorArea = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.parseColor("#FFFBFE"))
        }
        lineNumbers = LineNumberView(this).apply {
            setPadding(dp(4), dp(12), dp(4), dp(24))
            lineCount = 1
        }
        editor = EditText(this).apply {
            setText("Welcome to Sally Text Editor!\n\nThis is your own native editing workspace.\n\n• Create a document with New\n• Open a text or source file\n• Edit and save your changes\n\nNext milestones: tabs, syntax highlighting, and autocomplete.")
            textSize = 15f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(16), dp(12), dp(16), dp(24))
            setTextIsSelectable(true)
            setHorizontallyScrolling(false)
            setBackgroundColor(android.graphics.Color.parseColor("#FFFBFE"))
            setHint("Start typing…")
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!suppressDirty) dirty = true
                    updateStatus()
                    lineNumbers.lineCount = (s?.count { it == '\n' } ?: 0) + 1
                }
                override fun afterTextChanged(s: Editable?) {
                    if (s != null) SyntaxHighlighter.apply(s)
                }
            })
        }
        editorArea.addView(lineNumbers, LinearLayout.LayoutParams(dp(42), -1))
        editorArea.addView(editor, LinearLayout.LayoutParams(0, -1, 1f))
        editor.setOnScrollChangeListener { _, _, scrollY, _, _ -> lineNumbers.verticalScrollPx = scrollY }
        editor.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> lineNumbers.lineHeightPx = editor.lineHeight.toFloat() }
        root.addView(editorArea, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        status = TextView(this).apply { textSize = 12f }
        bottom.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        val openButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Open"
            isAllCaps = false
            setOnClickListener { openDocument.launch(arrayOf("*/*")) }
        }
        bottom.addView(openButton)
        val newButton = MaterialButton(this).apply {
            text = "New"
            isAllCaps = false
            setOnClickListener { newFile() }
        }
        bottom.addView(newButton)
        root.addView(bottom)
        return root
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
        suppressDirty = true
        editor.setText("")
        suppressDirty = false
        dirty = false
        filename.text = currentName
        updateStatus()
    }

    private fun readDocument(uri: Uri) {
        try {
            val content = contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            } ?: ""
            currentUri = uri
            currentName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Opened file"
            suppressDirty = true
            editor.setText(content)
            suppressDirty = false
            editor.setSelection(editor.text.length)
            dirty = false
            filename.text = currentName
            updateStatus()
            toast("Opened $currentName")
        } catch (e: Exception) {
            toast("Couldn't open file: ${e.localizedMessage ?: "unknown error"}")
        }
    }

    private fun saveFile() {
        val uri = currentUri
        if (uri == null) {
            createDocument.launch(currentName)
        } else {
            writeDocument(uri)
        }
    }

    private fun writeDocument(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(editor.text.toString())
            } ?: throw IllegalStateException("Unable to open output stream")
            currentUri = uri
            currentName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: currentName
            filename.text = currentName
            dirty = false
            updateStatus()
            toast("Saved successfully")
        } catch (e: Exception) {
            toast("Couldn't save file: ${e.localizedMessage ?: "unknown error"}")
        }
    }

    private fun updateStatus() {
        if (!::status.isInitialized || !::editor.isInitialized) return
        val text = editor.text
        if (::lineNumbers.isInitialized) lineNumbers.lineCount = text.count { it == '\n' } + 1
        val cursor = editor.selectionStart.coerceAtLeast(0)
        val line = text.subSequence(0, cursor.coerceAtMost(text.length)).count { it == '\n' } + 1
        val lastNewline = text.toString().lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        val column = cursor - lastNewline
        status.text = "Ln $line, Col $column  •  ${text.length} chars${if (dirty) "  •  Modified" else ""}"
        filename.text = currentName
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
