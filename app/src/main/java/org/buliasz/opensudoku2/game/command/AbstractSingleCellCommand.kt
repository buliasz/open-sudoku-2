package org.buliasz.opensudoku2.game.command

import org.buliasz.opensudoku2.game.Cell
import java.util.StringTokenizer

abstract class AbstractSingleCellCommand : AbstractCellCommand {
    private var mCellRow = 0
    private var mCellColumn = 0

    constructor(cell: Cell) {
        mCellRow = cell.rowIndex
        mCellColumn = cell.columnIndex
    }

    internal constructor()

    override fun serialize(data: StringBuilder) {
        super.serialize(data)
        data.append(mCellRow).append("|")
        data.append(mCellColumn).append("|")
    }

    override fun deserialize(data: StringTokenizer) {
        super.deserialize(data)
        mCellRow = data.nextToken().toInt()
        mCellColumn = data.nextToken().toInt()
    }

    val cell: Cell
        get() = cells!!.getCell(mCellRow, mCellColumn)
}
