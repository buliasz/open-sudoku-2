package org.buliasz.opensudoku2.gui.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase

class EditUserNoteDialogFragment(
    private val factory: LayoutInflater,
    private val mDatabase: SudokuDatabase,
    val updateList: () -> Unit
) :
    DialogFragment() {
    var puzzleId: Long = 0
    lateinit var currentValue: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val noteView = factory.inflate(R.layout.sudoku_list_item_note, null)
        val mEditNoteInput = noteView.findViewById<TextView>(R.id.user_note)!!
        mEditNoteInput.text = currentValue

        val builder = AlertDialog.Builder(requireActivity())
            .setIcon(R.drawable.ic_add)
            .setTitle(R.string.edit_note)
            .setView(noteView)
            .setPositiveButton(R.string.save) { _: DialogInterface?, _: Int ->
                val game = mDatabase.getSudoku(puzzleId)!!
                game.userNote = mEditNoteInput.text.toString()
                mDatabase.updateSudoku(game)
                updateList()
            }
            .setNegativeButton(android.R.string.cancel, null)

        return builder.create()
    }
}
