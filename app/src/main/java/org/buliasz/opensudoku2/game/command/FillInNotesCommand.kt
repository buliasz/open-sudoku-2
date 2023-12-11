package org.buliasz.opensudoku2.game.command

class FillInNotesCommand : AbstractMultiNoteCommand() {
    override fun execute() {
        val cells = cells
        mOldCornerNotes.clear()
        saveOldNotes()
        cells!!.fillInCenterNotes()
    }
}
