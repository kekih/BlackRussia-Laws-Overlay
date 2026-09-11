package com.blackrussia.laws

import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EditorActivity : AppCompatActivity() {

    private lateinit var repository: NotesRepository
    private var noteId: String? = null
    private lateinit var etTitle: TextInputEditText
    private lateinit var etContent: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        repository = NotesRepository(this)
        noteId = intent.getStringExtra("note_id")

        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)

        if (noteId != null) {
            val note = repository.getNote(noteId!!)
            if (note != null) {
                etTitle.setText(note.title)
                etContent.setText(Html.fromHtml(note.content, Html.FROM_HTML_MODE_COMPACT))
            }
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { save() }

        findViewById<MaterialButton>(R.id.btnBold).setOnClickListener { applyStyle(StyleSpan(android.graphics.Typeface.BOLD)) }
        findViewById<MaterialButton>(R.id.btnItalic).setOnClickListener { applyStyle(StyleSpan(android.graphics.Typeface.ITALIC)) }
        findViewById<MaterialButton>(R.id.btnColor).setOnClickListener { showColorPicker() }
        findViewById<MaterialButton>(R.id.btnSize).setOnClickListener { showSizePicker() }
        findViewById<MaterialButton>(R.id.btnHeading).setOnClickListener { applyHeading() }
        findViewById<MaterialButton>(R.id.btnClearFormat).setOnClickListener { clearFormat() }
    }

    private fun applyStyle(span: Any) {
        val start = etContent.selectionStart
        val end = etContent.selectionEnd
        if (start < 0 || end <= start) {
            Toast.makeText(this, "Выделите текст", Toast.LENGTH_SHORT).show()
            return
        }
        val text = etContent.text as Spannable
        text.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun applyHeading() {
        val start = etContent.selectionStart
        val end = etContent.selectionEnd
        if (start < 0 || end <= start) {
            Toast.makeText(this, "Выделите текст", Toast.LENGTH_SHORT).show()
            return
        }
        val text = etContent.text as Spannable
        text.setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(RelativeSizeSpan(1.3f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(ForegroundColorSpan(Color.parseColor("#FF9800")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun showColorPicker() {
        val colors = arrayOf(
            "Оранжевый" to "#FF9800",
            "Красный" to "#EF5350",
            "Зелёный" to "#66BB6A",
            "Синий" to "#42A5F5",
            "Белый" to "#FFFFFF",
            "Жёлтый" to "#FFEE58",
            "Фиолетовый" to "#AB47BC"
        )
        val names = colors.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Цвет текста")
            .setItems(names) { _, which ->
                val color = Color.parseColor(colors[which].second)
                applyStyle(ForegroundColorSpan(color))
            }
            .show()
    }

    private fun showSizePicker() {
        val sizes = arrayOf("Маленький", "Обычный", "Большой", "Очень большой")
        val scales = floatArrayOf(0.85f, 1.0f, 1.25f, 1.5f)
        AlertDialog.Builder(this)
            .setTitle("Размер текста")
            .setItems(sizes) { _, which ->
                applyStyle(RelativeSizeSpan(scales[which]))
            }
            .show()
    }

    private fun clearFormat() {
        val start = etContent.selectionStart
        val end = etContent.selectionEnd
        if (start < 0 || end <= start) {
            Toast.makeText(this, "Выделите текст", Toast.LENGTH_SHORT).show()
            return
        }
        val text = etContent.text as Spannable
        val spans = text.getSpans(start, end, Any::class.java)
        for (span in spans) {
            if (span is StyleSpan || span is ForegroundColorSpan || span is RelativeSizeSpan) {
                text.removeSpan(span)
            }
        }
    }

    private fun save() {
        val title = etTitle.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
            return
        }

        val html = Html.toHtml(etContent.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

        if (noteId != null) {
            val note = repository.getNote(noteId!!)
            if (note != null) {
                note.title = title
                note.content = html
                repository.updateNote(note)
            }
        } else {
            repository.addNote(title, html)
        }
        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun applyTheme() {
        when (AppPrefs.getTheme(this)) {
            AppPrefs.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}
