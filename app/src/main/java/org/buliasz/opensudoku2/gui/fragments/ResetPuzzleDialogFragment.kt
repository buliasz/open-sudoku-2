package org.buliasz.opensudoku2.gui.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase

class ResetPuzzleDialogFragment(private val mDatabase: SudokuDatabase, val updateList: () -> Unit) : DialogFragment() {
    var puzzleID: Long = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
            .setIcon(R.drawable.ic_restore)
            .setTitle("Puzzle")
            .setMessage(R.string.reset_puzzle_confirm)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                val game = mDatabase.getSudoku(puzzleID) ?: return@setPositiveButton
                game.reset()
                mDatabase.updateSudoku(game)
                updateList()
            }
            .setNegativeButton(android.R.string.cancel, null)

        return builder.create()
    }
}
