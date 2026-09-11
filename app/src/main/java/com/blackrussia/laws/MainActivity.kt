package com.blackrussia.laws

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var repository: NotesRepository
    private lateinit var adapter: NotesAdapter
    private var notes = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = NotesRepository(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerNotes)
        adapter = NotesAdapter(
            onEdit = { note -> openEditor(note.id) },
            onDelete = { note -> confirmDelete(note) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<MaterialButton>(R.id.btnPermission).setOnClickListener {
            requestOverlayPermission()
        }
        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, OverlayService::class.java))
                Toast.makeText(this, "Оверлей запущен", Toast.LENGTH_SHORT).show()
                moveTaskToBack(true)
            } else {
                Toast.makeText(this, "Сначала дайте разрешение", Toast.LENGTH_LONG).show()
                requestOverlayPermission()
            }
        }
        findViewById<MaterialButton>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Оверлей остановлен", Toast.LENGTH_SHORT).show()
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            openEditor(null)
        }

        findViewById<ImageButton>(R.id.btnTheme).setOnClickListener {
            showThemeDialog()
        }

        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        notes = repository.loadNotes()
        adapter.submitList(notes.toList())
    }

    private fun openEditor(noteId: String?) {
        val intent = Intent(this, EditorActivity::class.java)
        if (noteId != null) intent.putExtra("note_id", noteId)
        startActivity(intent)
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Удалить?")
            .setMessage("Удалить «${note.title}»?")
            .setPositiveButton("Удалить") { _, _ ->
                repository.deleteNote(note.id)
                loadNotes()
                stopService(Intent(this, OverlayService::class.java))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Разрешение уже есть", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Тёмная", "Светлая", "Оранжевая")
        val current = when (AppPrefs.getTheme(this)) {
            AppPrefs.THEME_LIGHT -> 1
            AppPrefs.THEME_ORANGE -> 2
            else -> 0
        }
        AlertDialog.Builder(this)
            .setTitle("Тема приложения")
            .setSingleChoiceItems(themes, current) { dialog, which ->
                val theme = when (which) {
                    1 -> AppPrefs.THEME_LIGHT
                    2 -> AppPrefs.THEME_ORANGE
                    else -> AppPrefs.THEME_DARK
                }
                AppPrefs.setTheme(this, theme)
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun applyTheme() {
        when (AppPrefs.getTheme(this)) {
            AppPrefs.THEME_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    inner class NotesAdapter(
        private val onEdit: (Note) -> Unit,
        private val onDelete: (Note) -> Unit
    ) : RecyclerView.Adapter<NotesAdapter.VH>() {

        private var items = listOf<Note>()

        fun submitList(list: List<Note>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_note, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val note = items[position]
            holder.title.text = note.title
            holder.btnEdit.setOnClickListener { onEdit(note) }
            holder.btnDelete.setOnClickListener { onDelete(note) }
            holder.itemView.setOnClickListener { onEdit(note) }
        }

        override fun getItemCount() = items.size

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvNoteTitle)
            val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        }
    }
}
