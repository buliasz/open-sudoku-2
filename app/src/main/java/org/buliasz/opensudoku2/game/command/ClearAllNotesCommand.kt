package org.buliasz.opensudoku2.game.command

import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.CellNote

class ClearAllNotesCommand : AbstractMultiNoteCommand() {
    override fun execute() {
        val cells = cells
        mOldCornerNotes.clear()
        for (r in 0..<CellCollection.SUDOKU_SIZE) {
            for (c in 0..<CellCollection.SUDOKU_SIZE) {
                val cell = cells!!.getCell(r, c)
                val cornerNote = cell.cornerNote
                val centerNote = cell.centerNote
                if (!cornerNote.isEmpty) {
                    mOldCornerNotes.add(NoteEntry(r, c, cornerNote))
                    mOldCenterNotes.add(NoteEntry(r, c, centerNote))
                    cell.cornerNote = CellNote()
                    cell.centerNote = CellNote()
                }
            }
        }
    }
}
