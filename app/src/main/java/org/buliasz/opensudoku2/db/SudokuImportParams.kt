package org.buliasz.opensudoku2.db

import org.buliasz.opensudoku2.game.SudokuGame

class SudokuImportParams {
    @JvmField
    var created: Long = 0

    @JvmField
    var state: Long = 0

    @JvmField
    var time: Long = 0

    @JvmField
    var lastPlayed: Long = 0

    @JvmField
    var cellsData: String = ""

    @JvmField
    var userNote: String = ""

    @JvmField
    var commandStack: String = ""
    fun clear() {
        created = 0
        state = SudokuGame.GAME_STATE_NOT_STARTED.toLong()
        time = 0
        lastPlayed = 0
        cellsData = ""
        userNote = ""
        commandStack = ""
    }
}
