package com.group14.notes

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.group14.notes.data.Note

/**
 * Custom RecyclerView adapter. It extends ListAdapter, which runs DiffUtil on a background thread
 * and tells RecyclerView exactly which rows were added, removed or moved. That is what produces the
 * default item animations (a deleted card slides out, a new one fades in) with no extra code.
 */
class NotesAdapter(
    private val onNoteClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(DIFF) {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvNoteTitle)
        val preview: TextView = view.findViewById(R.id.tvNotePreview)
        val date: TextView = view.findViewById(R.id.tvNoteDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.title.text = note.title
        holder.preview.text = note.preview
        holder.preview.visibility = if (note.preview.isEmpty()) View.GONE else View.VISIBLE
        holder.date.text = DateUtils.getRelativeTimeSpanString(
            note.updatedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
        holder.itemView.setOnClickListener { onNoteClick(note) }
    }

    fun noteAt(position: Int): Note = getItem(position)

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(old: Note, new: Note) = old.id == new.id
            override fun areContentsTheSame(old: Note, new: Note) = old == new
        }
    }
}
