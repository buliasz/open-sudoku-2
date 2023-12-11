package org.buliasz.opensudoku2.gui.exporting

import java.io.OutputStream

class FileExportTaskParams {
    /**
     * Id of folder to export. Set to -1, if you want to export all folders.
     */
    var folderID: Long? = null

    /**
     * Id of sudoku puzzle to export.
     */
    var sudokuID: Long? = null

    /**
     * File where data should be saved.
     */
    var fileOutputStream: OutputStream? = null
    var filename: String? = null
}
