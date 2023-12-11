package org.buliasz.opensudoku2.game.command

import org.buliasz.opensudoku2.game.CellCollection

/**
 * Generic command acting on one or more cells.
 *
 * @author romario, Kotlin version by buliasz
 */
abstract class AbstractCellCommand : AbstractCommand() {
    var cells: CellCollection? = null
}
