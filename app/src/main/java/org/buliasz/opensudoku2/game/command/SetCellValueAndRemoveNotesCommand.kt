package org.buliasz.opensudoku2.game.command

import org.buliasz.opensudoku2.game.Cell
import java.util.StringTokenizer

class SetCellValueAndRemoveNotesCommand : AbstractMultiNoteCommand {
    private var mCellRow = 0
    private var mCellColumn = 0
    private var mValue = 0
    private var mOldValue = 0

    constructor(cell: Cell, value: Int) {
        mCellRow = cell.rowIndex
        mCellColumn = cell.columnIndex
        mValue = value
    }

    internal constructor()

    val cell: Cell
        get() = cells!!.getCell(mCellRow, mCellColumn)

    override fun serialize(data: StringBuilder) {
        super.serialize(data)
        data.append(mCellRow).append("|")
        data.append(mCellColumn).append("|")
        data.append(mValue).append("|")
        data.append(mOldValue).append("|")
    }

    override fun deserialize(data: StringTokenizer) {
        super.deserialize(data)
        mCellRow = data.nextToken().toInt()
        mCellColumn = data.nextToken().toInt()
        mValue = data.nextToken().toInt()
        mOldValue = data.nextToken().toInt()
    }

    override fun execute() {
        mOldCornerNotes.clear()
        saveOldNotes()
        val cell = cell
        cells!!.removeNotesForChangedCell(cell, mValue)
        mOldValue = cell.value
        cell.value = mValue
    }

    override fun undo() {
        super.undo()
        val cell = cell
        cell.value = mOldValue
    }
}
