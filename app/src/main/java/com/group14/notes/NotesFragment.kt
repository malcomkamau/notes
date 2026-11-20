package com.group14.notes

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.group14.notes.auth.SessionManager
import com.group14.notes.data.DatabaseHelper
import com.group14.notes.data.Note
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Notes list: tap a card to edit it, swipe it away to delete (with Undo). */
class NotesFragment : Fragment(R.layout.fragment_notes) {

    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private lateinit var adapter: NotesAdapter
    private lateinit var emptyView: TextView

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
// Daily inspirational quote
        val quoteText = view.findViewById<TextView>(R.id.txtQuote)
        val authorText = view.findViewById<TextView>(R.id.txtAuthor)

        QuoteManager.fetchQuote(object : QuoteManager.QuoteCallback {

            override fun onSuccess(quote: String, author: String) {
                if (!isAdded) return

                quoteText.text = quote
                authorText.text = "- $author"
            }

            override fun onError(error: String) {
                if (!isAdded) return

                quoteText.text = "Your notes are always important!"
                authorText.text = "Daily inspiration"
            }
        })
        db = DatabaseHelper.getInstance(requireContext())
        session = SessionManager(requireContext())
        emptyView = view.findViewById(R.id.tvEmpty)

        adapter = NotesAdapter { note ->
            // Explicit intent: notes list -> note editor (edit mode)
            startActivity(NoteEditorActivity.newIntent(requireContext(), note.id))
        }

        val recycler = view.findViewById<RecyclerView>(R.id.rvNotes)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
        ItemTouchHelper(swipeToDeleteCallback()).attachToRecyclerView(recycler)
    }

    // Reload whenever we come back (e.g. from the editor)
    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        val userId = session.userId
        executor.execute {
            val notes = try {
                db.getNotes(userId)
            } catch (e: Exception) {
                emptyList()
            }
            activity?.runOnUiThread {
                if (isAdded) showNotes(notes)
            }
        }
    }

    private fun showNotes(notes: List<Note>) {
        adapter.submitList(notes)
        emptyView.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteWithUndo(note: Note) {
        val userId = session.userId

        // Remove from the screen straight away so the slide-out animation plays
        showNotes(adapter.currentList.filter { it.id != note.id })
        executor.execute { db.deleteNote(userId, note.id) }

        val host = requireActivity()
        Snackbar.make(host.findViewById(R.id.main), R.string.msg_note_deleted, Snackbar.LENGTH_LONG)
            .setAnchorView(host.findViewById<FloatingActionButton>(R.id.fabAddNote))
            .setAction(R.string.action_undo) {
                // Undo: put the note back with its original timestamps
                executor.execute {
                    db.insertNote(userId, note.title, note.content, note.createdAt, note.updatedAt)
                    loadNotes()
                }
            }
            .show()
    }

    private fun swipeToDeleteCallback(): ItemTouchHelper.SimpleCallback {
        val context = requireContext()
        val radius = resources.getDimension(R.dimen.corner_card)
        val iconMargin = resources.getDimensionPixelSize(R.dimen.space_lg)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.notes_accent)
        }
        val deleteIcon = AppCompatResources.getDrawable(context, R.drawable.ic_delete)!!.mutate().also {
            DrawableCompat.setTint(it, ContextCompat.getColor(context, R.color.notes_on_accent))
        }

        return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                deleteWithUndo(adapter.noteAt(position))
            }

            // Draw a red rounded panel with a bin icon behind the card as it slides away
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val item = viewHolder.itemView
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0f) {
                    val rect = if (dX > 0) {
                        RectF(item.left.toFloat(), item.top.toFloat(), item.left + dX, item.bottom.toFloat())
                    } else {
                        RectF(item.right + dX, item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat())
                    }
                    c.drawRoundRect(rect, radius, radius, background)

                    val size = deleteIcon.intrinsicHeight
                    val top = item.top + (item.height - size) / 2
                    val needed = size + iconMargin * 2
                    if (dX > needed) {
                        deleteIcon.setBounds(item.left + iconMargin, top, item.left + iconMargin + size, top + size)
                        deleteIcon.draw(c)
                    } else if (-dX > needed) {
                        deleteIcon.setBounds(item.right - iconMargin - size, top, item.right - iconMargin, top + size)
                        deleteIcon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
