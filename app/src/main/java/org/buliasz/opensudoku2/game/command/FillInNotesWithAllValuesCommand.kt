package org.buliasz.opensudoku2.game.command

class FillInNotesWithAllValuesCommand : AbstractMultiNoteCommand() {
    override fun execute() {
        val cells = cells
        mOldCornerNotes.clear()
        saveOldNotes()
        cells!!.fillInCenterNotesWithAllValues()
    }
}
