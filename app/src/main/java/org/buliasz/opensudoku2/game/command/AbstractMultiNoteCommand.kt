package org.buliasz.opensudoku2.game.command

import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.CellNote
import java.util.StringTokenizer

abstract class AbstractMultiNoteCommand : AbstractCellCommand() {
    protected var mOldCornerNotes: MutableList<NoteEntry> = ArrayList()
    protected var mOldCenterNotes: MutableList<NoteEntry> = ArrayList()
    override fun serialize(data: StringBuilder) {
        super.serialize(data)
        data.append(mOldCornerNotes.size).append("|")
        for (ne in mOldCornerNotes) {
            data.append(ne.rowIndex).append("|")
            data.append(ne.colIndex).append("|")
            ne.note.serialize(data)
        }
        data.append(mOldCenterNotes.size).append("|")
        for (ne in mOldCenterNotes) {
            data.append(ne.rowIndex).append("|")
            data.append(ne.colIndex).append("|")
            ne.note.serialize(data)
        }
    }

    override fun deserialize(data: StringTokenizer) {
        super.deserialize(data)
        var notesSize = data.nextToken().toInt()
        for (i in 0..<notesSize) {
            val row = data.nextToken().toInt()
            val col = data.nextToken().toInt()
            mOldCornerNotes.add(NoteEntry(row, col, CellNote.deserialize(data.nextToken())))
        }

        // Might be no more tokens if deserializing data from before center notes existed.
        if (!data.hasMoreTokens()) {
            return
        }
        notesSize = data.nextToken().toInt()
        for (i in 0..<notesSize) {
            val row = data.nextToken().toInt()
            val col = data.nextToken().toInt()
            mOldCenterNotes.add(NoteEntry(row, col, CellNote.deserialize(data.nextToken())))
        }
    }

    override fun undo() {
        val cells = cells
        for (ne in mOldCornerNotes) {
            cells!!.getCell(ne.rowIndex, ne.colIndex).cornerNote = ne.note
        }
        for (ne in mOldCenterNotes) {
            cells!!.getCell(ne.rowIndex, ne.colIndex).centerNote = ne.note
        }
    }

    protected fun saveOldNotes() {
        val cells = cells
        for (r in 0..<CellCollection.SUDOKU_SIZE) {
            for (c in 0..<CellCollection.SUDOKU_SIZE) {
                mOldCornerNotes.add(NoteEntry(r, c, cells!!.getCell(r, c).cornerNote))
                mOldCenterNotes.add(NoteEntry(r, c, cells.getCell(r, c).centerNote))
            }
        }
    }

    protected class NoteEntry(var rowIndex: Int, var colIndex: Int, var note: CellNote)
}
