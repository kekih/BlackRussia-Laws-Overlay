package com.blackrussia.laws

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NotesRepository(private val context: Context) {

    private val file: File
        get() = File(context.filesDir, "notes.json")

    fun loadNotes(): MutableList<Note> {
        if (!file.exists()) {
            return createDefaultNotes()
        }
        return try {
            val json = file.readText(Charsets.UTF_8)
            val array = JSONArray(json)
            val list = mutableListOf<Note>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Note(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        order = obj.optInt("order", i)
                    )
                )
            }
            list.sortBy { it.order }
            list
        } catch (e: Exception) {
            createDefaultNotes()
        }
    }

    fun saveNotes(notes: List<Note>) {
        val array = JSONArray()
        notes.forEachIndexed { index, note ->
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("order", index)
            array.put(obj)
        }
        file.writeText(array.toString(2), Charsets.UTF_8)
    }

    private fun createDefaultNotes(): MutableList<Note> {
        val zakony = try {
            context.assets.open("zakony.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Текст законов..."
        }
        val pravila = try {
            context.assets.open("pravila.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Текст правил..."
        }

        fun toHtml(text: String): String {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>")
        }

        val defaults = mutableListOf(
            Note(title = "Законы", content = toHtml(zakony), order = 0),
            Note(title = "Правила", content = toHtml(pravila), order = 1)
        )
        saveNotes(defaults)
        return defaults
    }

    fun addNote(title: String, content: String = ""): Note {
        val notes = loadNotes()
        val note = Note(title = title, content = content, order = notes.size)
        notes.add(note)
        saveNotes(notes)
        return note
    }

    fun updateNote(note: Note) {
        val notes = loadNotes()
        val idx = notes.indexOfFirst { it.id == note.id }
        if (idx >= 0) {
            notes[idx] = note
            saveNotes(notes)
        }
    }

    fun deleteNote(id: String) {
        val notes = loadNotes().filter { it.id != id }
        saveNotes(notes)
    }

    fun getNote(id: String): Note? {
        return loadNotes().find { it.id == id }
    }
}
