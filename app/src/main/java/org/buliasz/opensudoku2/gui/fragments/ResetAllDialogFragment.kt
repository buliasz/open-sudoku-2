package org.buliasz.opensudoku2.gui.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.gui.SudokuListSorter

class ResetAllDialogFragment(
    private val mDatabase: SudokuDatabase,
    private val mFolderID: Long,
    private val mListSorter: SudokuListSorter,
    val updateList: () -> Unit
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
            .setIcon(R.drawable.ic_restore)
            .setTitle(R.string.reset_all_puzzles_confirm)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                val sudokuGames = mDatabase.getAllSudokuByFolder(mFolderID, mListSorter)
                for (sudokuGame in sudokuGames) {
                    sudokuGame.reset()
                    mDatabase.updateSudoku(sudokuGame)
                }
                updateList()
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }

        return builder.create()
    }
}
