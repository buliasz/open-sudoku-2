/*
 * This file is part of Open Sudoku 2 - an open-source Sudoku game.
 * Copyright (C) 2009-2023 by original authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.buliasz.opensudoku2.gui

import android.app.Dialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.game.SudokuGame.OnPuzzleSolvedListener
import org.buliasz.opensudoku2.gui.SudokuBoardView.HighlightMode
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanel
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanelStatePersister
import org.buliasz.opensudoku2.gui.inputmethod.IMNumpad
import org.buliasz.opensudoku2.gui.inputmethod.IMPopup
import org.buliasz.opensudoku2.gui.inputmethod.IMSingleNumber
import org.buliasz.opensudoku2.utils.ThemeUtils

class SudokuPlayActivity : ThemedActivity() {
    private var mSudokuGame: SudokuGame? = null
    private var mDatabase: SudokuDatabase? = null
    private var mGuiHandler: Handler? = null
    private var mRootLayout: ViewGroup? = null
    private var mSudokuBoard: SudokuBoardView? = null
    private var mTimeLabel: TextView? = null
    private var mOptionsMenu: Menu? = null
    private lateinit var mIMControlPanel: IMControlPanel
    private var mIMControlPanelStatePersister: IMControlPanelStatePersister? = null
    private var mIMPopup: IMPopup? = null
    private var mIMSingleNumber: IMSingleNumber? = null
    private var mIMNumpad: IMNumpad? = null
    private var mShowTime = true
    private var mGameTimer: GameTimer? = null
    private val mGameTimeFormatter = GameTimeFormat()
    private var mFullScreen = false
    private var mFillInNotesEnabled = false
    private var mHintsQueue: HintsQueue? = null

    /**
     * Occurs when puzzle is solved.
     */
    private val onSolvedListener = object : OnPuzzleSolvedListener {
        override fun onPuzzleSolved() {
            if (mShowTime) {
                mGameTimer!!.stop()
            }
            mSudokuBoard!!.isReadOnly = (true)
            mOptionsMenu!!.findItem(MENU_ITEM_UNDO_ACTION)
                .setEnabled(false)
            if (mSudokuGame!!.usedSolver()) {
                showDialog(DIALOG_USED_SOLVER)
            } else {
                showDialog(DIALOG_WELL_DONE)
            }
        }
    }

    private val onSelectedNumberChangedListener: OnSelectedNumberChangedListener =
        object : OnSelectedNumberChangedListener {
            override fun onSelectedNumberChanged(number: Int) {
                if (number != 0) {
                    val cell = mSudokuGame!!.cells.findFirstCell(number)
                    mSudokuBoard!!.setHighlightedValue(number)
                    if (cell != null) {
                        mSudokuBoard!!.moveCellSelectionTo(cell.rowIndex, cell.columnIndex)
                    } else {
                        mSudokuBoard!!.clearCellSelection()
                    }
                } else {
                    mSudokuBoard!!.clearCellSelection()
                }
            }
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // go fullscreen for devices with QVGA screen (only way I found
        // how to fit UI on the screen)
        val display = windowManager.defaultDisplay
        if ((display.width == 240 || display.width == 320)
            && (display.height == 240 || display.height == 320)
        ) {
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            mFullScreen = true
        }
        setContentView(R.layout.sudoku_play)
        mRootLayout = findViewById(R.id.root_layout)
        mSudokuBoard = findViewById(R.id.cells_data)
        mTimeLabel = findViewById(R.id.time_label)
        mDatabase = SudokuDatabase(applicationContext)
        mHintsQueue = HintsQueue(this)
        mGameTimer = GameTimer()
        mGuiHandler = Handler()

        // create sudoku game instance
        if (savedInstanceState == null) {
            // activity runs for the first time, read game from database
            val mSudokuGameID = intent.getLongExtra(EXTRA_SUDOKU_ID, 0)
            mSudokuGame = mDatabase!!.getSudoku(mSudokuGameID)
        } else {
            // activity has been running before, restore its state
            mSudokuGame = SudokuGame()
            mSudokuGame!!.restoreState(savedInstanceState)
            mGameTimer!!.restoreState(savedInstanceState)
        }

        // save our most recently played sudoku
        val gameSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val editor = gameSettings.edit()
        editor.putLong("most_recently_played_sudoku_id", mSudokuGame!!.id)
        editor.apply()
        if (mSudokuGame!!.state == SudokuGame.GAME_STATE_NOT_STARTED) {
            mSudokuGame!!.start()
        } else if (mSudokuGame!!.state == SudokuGame.GAME_STATE_PLAYING) {
            mSudokuGame!!.resume()
        }
        if (mSudokuGame!!.state == SudokuGame.GAME_STATE_COMPLETED) {
            mSudokuBoard!!.isReadOnly = (true)
        }
        mSudokuBoard!!.setGame(mSudokuGame!!)
        mSudokuGame!!.setOnPuzzleSolvedListener(onSolvedListener)
        mHintsQueue!!.showOneTimeHint("welcome", R.string.welcome, R.string.first_run_hint)
        mIMControlPanel = findViewById(R.id.input_methods)
        mIMControlPanel.initialize(mSudokuBoard, mSudokuGame, mHintsQueue)
        mIMControlPanelStatePersister = IMControlPanelStatePersister(this)
        mIMPopup = mIMControlPanel.imPopup
        mIMSingleNumber = mIMControlPanel.imSingleNumber
        mIMNumpad = mIMControlPanel.imNumpad
        val cell = mSudokuGame!!.lastChangedCell
        if (cell != null && !mSudokuBoard!!.isReadOnly) mSudokuBoard!!.moveCellSelectionTo(
            cell.rowIndex,
            cell.columnIndex
        ) else mSudokuBoard!!.moveCellSelectionTo(0, 0)
    }

    override fun onResume() {
        super.onResume()

        // read game settings
        val gameSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val screenPadding = gameSettings.getInt("screen_border_size", 0)
        mRootLayout!!.setPadding(screenPadding, screenPadding, screenPadding, screenPadding)
        mFillInNotesEnabled = gameSettings.getBoolean("fill_in_notes_enabled", false)
        val theme = gameSettings.getString("theme", "opensudoku2")
        if (theme == "custom" || theme == "custom_light") {
            ThemeUtils.applyCustomThemeToSudokuBoardViewFromSharedPreferences(this, mSudokuBoard!!)
        } else {
            mSudokuBoard!!.setAllColorsFromThemedContext(this)
        }
        mSudokuBoard!!.setHighlightWrongValues(
            gameSettings.getBoolean(
                "highlight_wrong_values",
                true
            )
        )
        mSudokuBoard!!.setHighlightTouchedCell(
            gameSettings.getBoolean(
                "highlight_touched_cell",
                true
            )
        )
        val highlightSimilarCells = gameSettings.getBoolean("highlight_similar_cells", true)
        val highlightSimilarNotes = gameSettings.getBoolean("highlight_similar_notes", true)
        if (highlightSimilarCells) {
            mSudokuBoard!!.setHighlightSimilarCell(if (highlightSimilarNotes) HighlightMode.NUMBERS_AND_NOTES else HighlightMode.NUMBERS)
        } else {
            mSudokuBoard!!.setHighlightSimilarCell(HighlightMode.NONE)
        }
        mSudokuGame!!.setRemoveNotesOnEntry(gameSettings.getBoolean("remove_notes_on_input", false))
        mShowTime = gameSettings.getBoolean("show_time", true)
        if (mSudokuGame!!.state == SudokuGame.GAME_STATE_PLAYING) {
            mSudokuGame!!.resume()
            if (mShowTime) {
                mGameTimer!!.start()
            }
        }
        mTimeLabel!!.visibility = if (mFullScreen && mShowTime) View.VISIBLE else View.GONE
        val moveCellSelectionOnPress = gameSettings.getBoolean("im_numpad_move_right", false)
        mSudokuBoard!!.setMoveCellSelectionOnPress(moveCellSelectionOnPress)
        mIMNumpad!!.isMoveCellSelectionOnPress = (moveCellSelectionOnPress)
        mIMPopup!!.isEnabled = (gameSettings.getBoolean("im_popup", true))
        mIMSingleNumber!!.isEnabled = (gameSettings.getBoolean("im_single_number", true))
        mIMNumpad!!.isEnabled = (gameSettings.getBoolean("im_numpad", true))
        mIMPopup!!.setHighlightCompletedValues(
            gameSettings.getBoolean(
                "highlight_completed_values",
                true
            )
        )
        mIMPopup!!.setShowNumberTotals(gameSettings.getBoolean("show_number_totals", false))
        mIMSingleNumber!!.setHighlightCompletedValues(
            gameSettings.getBoolean(
                "highlight_completed_values",
                true
            )
        )
        mIMSingleNumber!!.setShowNumberTotals(gameSettings.getBoolean("show_number_totals", false))
        mIMSingleNumber!!.setBidirectionalSelection(
            gameSettings.getBoolean(
                "bidirectional_selection",
                true
            )
        )
        mIMSingleNumber!!.setHighlightSimilar(gameSettings.getBoolean("highlight_similar", true))
        mIMSingleNumber!!.setOnSelectedNumberChangedListener(onSelectedNumberChangedListener)
        mIMNumpad!!.setHighlightCompletedValues(
            gameSettings.getBoolean(
                "highlight_completed_values",
                true
            )
        )
        mIMNumpad!!.setShowNumberTotals(gameSettings.getBoolean("show_number_totals", false))
        mIMControlPanel.activateFirstInputMethod() // make sure that some input method is activated
        mIMControlPanelStatePersister!!.restoreState(mIMControlPanel)
        if (!mSudokuBoard!!.isReadOnly) {
            mSudokuBoard!!.invokeOnCellSelected()
        }
        updateTime()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // FIXME: When activity is resumed, title isn't sometimes hidden properly (there is black
            // empty space at the top of the screen). This is desperate workaround.
            if (mFullScreen) {
                mGuiHandler!!.postDelayed({
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
                    mRootLayout!!.requestLayout()
                }, 1000)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // we will save game to the database as we might not be able to get back
        mDatabase!!.updateSudoku(mSudokuGame!!)
        mGameTimer!!.stop()
        mIMControlPanel.pause()
        mIMControlPanelStatePersister!!.saveState(mIMControlPanel)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDatabase!!.close()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mGameTimer!!.stop()
        if (mSudokuGame!!.state == SudokuGame.GAME_STATE_PLAYING) {
            mSudokuGame!!.pause()
        }
        mSudokuGame!!.saveState(outState)
        mGameTimer!!.saveState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, MENU_ITEM_UNDO_ACTION, 0, R.string.undo)
            .setIcon(R.drawable.ic_undo)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, MENU_ITEM_UNDO, 0, R.string.undo)
            .setShortcut('1', 'u')
            .setIcon(R.drawable.ic_undo)
        if (mFillInNotesEnabled) {
            menu.add(
                0,
                MENU_ITEM_FILL_IN_NOTES,
                1,
                R.string.fill_in_notes
            )
                .setIcon(R.drawable.ic_edit_grey)
        }
        menu.add(
            0,
            MENU_ITEM_FILL_IN_NOTES_WITH_ALL_VALUES,
            1,
            R.string.fill_all_notes
        )
            .setIcon(R.drawable.ic_edit_grey)
        menu.add(
            0,
            MENU_ITEM_CLEAR_ALL_NOTES,
            2,
            R.string.clear_all_notes
        )
            .setShortcut('3', 'a')
            .setIcon(R.drawable.ic_delete)
        menu.add(
            0,
            MENU_ITEM_SET_CHECKPOINT,
            3,
            R.string.set_checkpoint
        )
        menu.add(
            0,
            MENU_ITEM_UNDO_TO_CHECKPOINT,
            4,
            R.string.undo_to_checkpoint
        )
        menu.add(
            0,
            MENU_ITEM_UNDO_TO_BEFORE_MISTAKE,
            4,
            getString(R.string.undo_to_before_mistake)
        )
        menu.add(0, MENU_ITEM_HINT, 5, R.string.solver_hint)
        menu.add(0, MENU_ITEM_SOLVE, 6, R.string.solve_puzzle)
        menu.add(0, MENU_ITEM_RESTART, 7, R.string.restart)
            .setShortcut('7', 'r')
            .setIcon(R.drawable.ic_restore)
        menu.add(0, MENU_ITEM_SETTINGS_ACTION, 8, R.string.settings)
            .setIcon(R.drawable.ic_settings)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, MENU_ITEM_SETTINGS, 8, R.string.settings)
            .setShortcut('9', 's')
            .setIcon(R.drawable.ic_settings)
        menu.add(0, MENU_ITEM_HELP, 9, R.string.help)
            .setShortcut('0', 'h')
            .setIcon(R.drawable.ic_help)


        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        val intent = Intent(null, intent.data)
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
        menu.addIntentOptions(
            Menu.CATEGORY_ALTERNATIVE, 0, 0,
            ComponentName(this, SudokuPlayActivity::class.java), null, intent, 0, null
        )
        mOptionsMenu = menu
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (mSudokuGame!!.state == SudokuGame.GAME_STATE_PLAYING) {
            menu.findItem(MENU_ITEM_CLEAR_ALL_NOTES).setEnabled(true)
            if (mFillInNotesEnabled) {
                menu.findItem(MENU_ITEM_FILL_IN_NOTES).setEnabled(true)
            }
            menu.findItem(MENU_ITEM_FILL_IN_NOTES_WITH_ALL_VALUES)
                .setEnabled(true)
            menu.findItem(MENU_ITEM_UNDO)
                .setEnabled(mSudokuGame!!.hasSomethingToUndo())
            menu.findItem(MENU_ITEM_UNDO_TO_CHECKPOINT)
                .setEnabled(mSudokuGame!!.hasUndoCheckpoint())
        } else {
            menu.findItem(MENU_ITEM_CLEAR_ALL_NOTES).setEnabled(false)
            if (mFillInNotesEnabled) {
                menu.findItem(MENU_ITEM_FILL_IN_NOTES)
                    .setEnabled(false)
            }
            menu.findItem(MENU_ITEM_FILL_IN_NOTES_WITH_ALL_VALUES)
                .setEnabled(false)
            menu.findItem(MENU_ITEM_UNDO).setEnabled(false)
            menu.findItem(MENU_ITEM_UNDO_TO_CHECKPOINT)
                .setEnabled(false)
            menu.findItem(MENU_ITEM_UNDO_TO_BEFORE_MISTAKE)
                .setEnabled(false)
            menu.findItem(MENU_ITEM_SOLVE).setEnabled(false)
            menu.findItem(MENU_ITEM_HINT).setEnabled(false)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_ITEM_RESTART -> {
                showDialog(DIALOG_RESTART)
                return true
            }

            MENU_ITEM_CLEAR_ALL_NOTES -> {
                showDialog(DIALOG_CLEAR_NOTES)
                return true
            }

            MENU_ITEM_FILL_IN_NOTES -> {
                mSudokuGame!!.fillInNotes()
                return true
            }

            MENU_ITEM_FILL_IN_NOTES_WITH_ALL_VALUES -> {
                mSudokuGame!!.fillInNotesWithAllValues()
                return true
            }

            MENU_ITEM_UNDO_ACTION -> {
                if (mSudokuGame!!.hasSomethingToUndo()) {
                    mSudokuGame!!.undo()
                    selectLastChangedCell()
                }
                return true
            }

            MENU_ITEM_UNDO -> {
                mSudokuGame!!.undo()
                selectLastChangedCell()
                return true
            }

            MENU_ITEM_SETTINGS_ACTION, MENU_ITEM_SETTINGS -> {
                val i = Intent()
                i.setClass(this, GameSettingsActivity::class.java)
                startActivityForResult(i, REQUEST_SETTINGS)
                return true
            }

            MENU_ITEM_HELP -> {
                mHintsQueue!!.showHint(R.string.help, R.string.help_text)
                return true
            }

            MENU_ITEM_SET_CHECKPOINT -> {
                mSudokuGame!!.setUndoCheckpoint()
                return true
            }

            MENU_ITEM_UNDO_TO_CHECKPOINT -> {
                showDialog(DIALOG_UNDO_TO_CHECKPOINT)
                return true
            }

            MENU_ITEM_UNDO_TO_BEFORE_MISTAKE -> {
                showDialog(DIALOG_UNDO_TO_BEFORE_MISTAKE)
                return true
            }

            MENU_ITEM_SOLVE -> {
                showDialog(DIALOG_SOLVE_PUZZLE)
                return true
            }

            MENU_ITEM_HINT -> {
                showDialog(DIALOG_HINT)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SETTINGS) {
            restartActivity()
        }
    }

    /**
     * Restarts whole activity.
     */
    private fun restartActivity() {
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(id: Int): Dialog {
        when (id) {
            DIALOG_WELL_DONE -> return AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_info)
                .setTitle(R.string.well_done)
                .setMessage(
                    getString(
                        R.string.congrats,
                        mGameTimeFormatter.format(mSudokuGame!!.time)
                    )
                )
                .setPositiveButton(android.R.string.ok, null)
                .create()

            DIALOG_RESTART -> return AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_restore)
                .setTitle(R.string.app_name)
                .setMessage(R.string.restart_confirm)
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    // Restart game
                    mSudokuGame!!.reset()
                    mSudokuGame!!.start()
                    mSudokuBoard!!.isReadOnly = (false)
                    if (mShowTime) {
                        mGameTimer!!.start()
                    }
                    removeDialog(DIALOG_WELL_DONE)
                    val menuItemSolve =
                        mOptionsMenu!!.findItem(MENU_ITEM_SOLVE)
                    menuItemSolve.setEnabled(true)
                    val menuItemHint =
                        mOptionsMenu!!.findItem(MENU_ITEM_HINT)
                    menuItemHint.setEnabled(true)
                    val menuItemUndoAction =
                        mOptionsMenu!!.findItem(MENU_ITEM_UNDO_ACTION)
                    menuItemUndoAction.setEnabled(true)
                }
                .setNegativeButton(android.R.string.no, null)
                .create()

            DIALOG_CLEAR_NOTES -> return AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.app_name)
                .setMessage(R.string.clear_all_notes_confirm)
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int -> mSudokuGame!!.clearAllNotes() }
                .setNegativeButton(android.R.string.no, null)
                .create()

            DIALOG_UNDO_TO_CHECKPOINT -> return AlertDialog.Builder(
                this
            )
                .setIcon(R.drawable.ic_undo)
                .setTitle(R.string.app_name)
                .setMessage(R.string.undo_to_checkpoint_confirm)
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    mSudokuGame!!.undoToCheckpoint()
                    selectLastChangedCell()
                }
                .setNegativeButton(android.R.string.no, null)
                .create()

            DIALOG_UNDO_TO_BEFORE_MISTAKE -> return AlertDialog.Builder(
                this
            )
                .setIcon(R.drawable.ic_undo)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.undo_to_before_mistake_confirm))
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    mSudokuGame!!.undoToBeforeMistake()
                    selectLastChangedCell()
                }
                .setNegativeButton(android.R.string.no, null)
                .create()

            DIALOG_SOLVE_PUZZLE -> return AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.solve_puzzle_confirm)
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    if (mSudokuGame!!.isSolvable) {
                        mSudokuGame!!.solve()
                    } else {
                        showDialog(DIALOG_PUZZLE_NOT_SOLVED)
                    }
                }
                .setNegativeButton(android.R.string.no, null)
                .create()

            DIALOG_USED_SOLVER -> return AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.used_solver)
                .setPositiveButton(android.R.string.ok, null)
                .create()

            DIALOG_PUZZLE_NOT_SOLVED -> return AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.puzzle_not_solved)
                .setPositiveButton(android.R.string.ok, null)
                .create()

            DIALOG_HINT -> return AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.hint_confirm)
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    val cell = mSudokuBoard!!.selectedCell
                    if (cell != null && cell.isEditable) {
                        if (mSudokuGame!!.isSolvable) {
                            mSudokuGame!!.solveCell(cell)
                        } else {
                            showDialog(DIALOG_PUZZLE_NOT_SOLVED)
                        }
                    } else {
                        showDialog(DIALOG_CANNOT_GIVE_HINT)
                    }
                }
                .setNegativeButton(android.R.string.no, null)
                .create()

            DIALOG_CANNOT_GIVE_HINT -> return AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.cannot_give_hint)
                .setPositiveButton(android.R.string.ok, null)
                .create()
        }
        throw Exception("Invalid id $id")
    }

    private fun selectLastChangedCell() {
        val cell = mSudokuGame!!.lastChangedCell
        if (cell != null) mSudokuBoard!!.moveCellSelectionTo(cell.rowIndex, cell.columnIndex)
    }

    /**
     * Update the time of game-play.
     */
    fun updateTime() {
        if (mShowTime) {
            title = mGameTimeFormatter.format(mSudokuGame!!.time)
            mTimeLabel!!.text = mGameTimeFormatter.format(mSudokuGame!!.time)
        } else {
            setTitle(R.string.app_name)
        }
    }

    interface OnSelectedNumberChangedListener {
        fun onSelectedNumberChanged(number: Int)
    }

    // This class implements the game clock.  All it does is update the
    // status each tick.
    private inner class GameTimer : Timer(1000) {
        override fun step(count: Int, time: Long): Boolean {
            updateTime()

            // Run until explicitly stopped.
            return false
        }
    }

    companion object {
        const val EXTRA_SUDOKU_ID = "sudoku_id"
        const val MENU_ITEM_RESTART = Menu.FIRST
        const val MENU_ITEM_CLEAR_ALL_NOTES = Menu.FIRST + 1
        const val MENU_ITEM_FILL_IN_NOTES = Menu.FIRST + 2
        const val MENU_ITEM_FILL_IN_NOTES_WITH_ALL_VALUES = Menu.FIRST + 3
        const val MENU_ITEM_UNDO_ACTION = Menu.FIRST + 4
        const val MENU_ITEM_UNDO = Menu.FIRST + 5
        const val MENU_ITEM_HELP = Menu.FIRST + 6
        const val MENU_ITEM_SETTINGS_ACTION = Menu.FIRST + 7
        const val MENU_ITEM_SETTINGS = Menu.FIRST + 8
        const val MENU_ITEM_SET_CHECKPOINT = Menu.FIRST + 9
        const val MENU_ITEM_UNDO_TO_CHECKPOINT = Menu.FIRST + 10
        const val MENU_ITEM_UNDO_TO_BEFORE_MISTAKE = Menu.FIRST + 11
        const val MENU_ITEM_SOLVE = Menu.FIRST + 12
        const val MENU_ITEM_HINT = Menu.FIRST + 13
        private const val DIALOG_RESTART = 1
        private const val DIALOG_WELL_DONE = 2
        private const val DIALOG_CLEAR_NOTES = 3
        private const val DIALOG_UNDO_TO_CHECKPOINT = 4
        private const val DIALOG_UNDO_TO_BEFORE_MISTAKE = 5
        private const val DIALOG_SOLVE_PUZZLE = 6
        private const val DIALOG_USED_SOLVER = 7
        private const val DIALOG_PUZZLE_NOT_SOLVED = 8
        private const val DIALOG_HINT = 9
        private const val DIALOG_CANNOT_GIVE_HINT = 10
        private const val REQUEST_SETTINGS = 1
    }
}
