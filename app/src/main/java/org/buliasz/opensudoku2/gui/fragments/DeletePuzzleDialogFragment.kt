package org.buliasz.opensudoku2.gui.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase

class DeletePuzzleDialogFragment(val mDatabase: SudokuDatabase, val settings: SharedPreferences, val updateList: () -> Unit) :
    DialogFragment() {
    var puzzleID: Long = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
            .setIcon(R.drawable.ic_delete)
            .setTitle("Puzzle")
            .setMessage(R.string.delete_puzzle_confirm)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                val mostRecentId = settings.getLong("most_recently_played_sudoku_id", 0)
                if (puzzleID == mostRecentId) {
                    settings.edit().remove("most_recently_played_sudoku_id").apply()
                }
                mDatabase.deleteSudoku(puzzleID)
                updateList()
            }
            .setNegativeButton(android.R.string.cancel, null)

        return builder.create()
    }
}
