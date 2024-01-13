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

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceManager
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.game.SudokuGame.OnPuzzleSolvedListener
import org.buliasz.opensudoku2.gui.SudokuBoardView.HighlightMode
import org.buliasz.opensudoku2.gui.fragments.SimpleDialog
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanel
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanelStatePersister
import org.buliasz.opensudoku2.gui.inputmethod.IMNumpad
import org.buliasz.opensudoku2.gui.inputmethod.IMPopup
import org.buliasz.opensudoku2.gui.inputmethod.IMSingleNumber
import org.buliasz.opensudoku2.utils.ThemeUtils

class SudokuPlayActivity : ThemedActivity() {
	private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
	private lateinit var mSudokuGame: SudokuGame
	private lateinit var mDatabase: SudokuDatabase
	private lateinit var mRootLayout: ViewGroup
	private lateinit var mSudokuBoard: SudokuBoardView
	private lateinit var mOptionsMenu: Menu
	private lateinit var mIMControlPanel: IMControlPanel
	private lateinit var mIMControlPanelStatePersister: IMControlPanelStatePersister
	private lateinit var mIMPopup: IMPopup
	private lateinit var mIMSingleNumber: IMSingleNumber
	private lateinit var mIMNumpad: IMNumpad
	private var mShowTime = true
	private lateinit var mGameTimer: GameTimer
	private val mGameTimeFormatter = GameTimeFormat()
	private var mFillInNotesEnabled = false
	private lateinit var mHintsQueue: HintsQueue

	/**
	 * Occurs when puzzle is solved.
	 */
	private val onSolvedListener = object : OnPuzzleSolvedListener {
		override fun onPuzzleSolved() {
			if (mShowTime) {
				mGameTimer.stop()
			}
			mSudokuBoard.isReadOnly = (true)
			if (mSudokuGame.usedSolver()) {
				SimpleDialog(supportFragmentManager).show(R.string.used_solver)

			} else {
				with(SimpleDialog(supportFragmentManager)) {
					iconId = R.drawable.ic_info
					titleId = R.string.well_done
					message = this@SudokuPlayActivity.getString(R.string.congrats, mGameTimeFormatter.format(mSudokuGame.time))
					show()
				}
			}
		}
	}

	private val onSelectedNumberChangedListener: OnSelectedNumberChangedListener =
		object : OnSelectedNumberChangedListener {
			override fun onSelectedNumberChanged(number: Int) {
				mSudokuBoard.setHighlightedValue(number)
				mSudokuBoard.postInvalidate()
			}
		}

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.sudoku_play)
		mRootLayout = findViewById(R.id.play_root_layout)
		mSudokuBoard = findViewById(R.id.play_board_view)
		mDatabase = SudokuDatabase(applicationContext)
		mHintsQueue = HintsQueue(this)
		mGameTimer = GameTimer(this)

		// create sudoku game instance
		if (savedInstanceState == null) {
			// activity runs for the first time, read game from database
			val mSudokuGameID = intent.getLongExtra(EXTRA_PUZZLE_ID, 0)
			mSudokuGame = mDatabase.getPuzzle(mSudokuGameID) ?: SudokuGame()
		} else {
			// activity has been running before, restore its state
			mSudokuGame = SudokuGame()
			mSudokuGame.restoreState(savedInstanceState)
			mGameTimer.restoreState(savedInstanceState)
		}

		// save our most recently played puzzle
		val gameSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
		val editor = gameSettings.edit()
		editor.putLong("most_recently_played_puzzle_id", mSudokuGame.id)
		editor.apply()
		if (mSudokuGame.state == SudokuGame.GAME_STATE_NOT_STARTED) {
			mSudokuGame.start()
		} else if (mSudokuGame.state == SudokuGame.GAME_STATE_PLAYING) {
			mSudokuGame.resume()
		}
		if (mSudokuGame.state == SudokuGame.GAME_STATE_COMPLETED) {
			mSudokuBoard.isReadOnly = (true)
		}
		mSudokuBoard.setGame(mSudokuGame)
		mSudokuGame.setOnPuzzleSolvedListener(onSolvedListener)
		mSudokuGame.onHasUndoChangedListener =
			{ isUndoStackEmpty -> mOptionsMenu.findItem(MENU_ITEM_UNDO_ACTION).setEnabled(!isUndoStackEmpty) }
		mHintsQueue.showOneTimeHint("welcome", R.string.welcome, R.string.first_run_hint)
		mIMControlPanel = findViewById(R.id.input_methods)
		mIMControlPanel.initialize(mSudokuBoard, mSudokuGame, mHintsQueue)
		mIMControlPanelStatePersister = IMControlPanelStatePersister(this)
		mIMPopup = mIMControlPanel.imPopup
		mIMSingleNumber = mIMControlPanel.imSingleNumber
		mIMNumpad = mIMControlPanel.imNumpad
		if (!mSudokuBoard.isReadOnly) selectCell(mSudokuGame.lastCommandCell)

		settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { restartActivity() }
	}

	override fun onResume() {
		super.onResume()

		// read game settings
		val gameSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
		val screenPadding = gameSettings.getInt("screen_border_size", 0)
		mRootLayout.setPadding(screenPadding, screenPadding, screenPadding, screenPadding)
		mFillInNotesEnabled = gameSettings.getBoolean("fill_in_notes_enabled", false)
		val theme = gameSettings.getString("theme", "opensudoku2")
		if (theme == "custom" || theme == "custom_light") {
			ThemeUtils.applyCustomThemeToSudokuBoardViewFromSharedPreferences(this, mSudokuBoard)
		} else {
			mSudokuBoard.setAllColorsFromThemedContext(this)
		}
		mSudokuBoard.setHighlightDirectlyWrongValues(gameSettings.getBoolean("highlight_directly_wrong_values", true))
		mSudokuBoard.setHighlightIndirectlyWrongValues(gameSettings.getBoolean("highlight_indirectly_wrong_values", true))
		mSudokuBoard.setHighlightTouchedCell(gameSettings.getBoolean("highlight_touched_cell", true))
		val highlightSimilarCells = gameSettings.getBoolean("highlight_similar_cells", true)
		val highlightSimilarNotes = gameSettings.getBoolean("highlight_similar_notes", true)
		if (highlightSimilarCells) {
			mSudokuBoard.setHighlightSimilarCell(if (highlightSimilarNotes) HighlightMode.NUMBERS_AND_NOTES else HighlightMode.NUMBERS)
		} else {
			mSudokuBoard.setHighlightSimilarCell(HighlightMode.NONE)
		}
		mSudokuGame.setRemoveNotesOnEntry(gameSettings.getBoolean("remove_notes_on_input", false))
		mShowTime = gameSettings.getBoolean("show_time", true)
		if (mSudokuGame.state == SudokuGame.GAME_STATE_PLAYING) {
			mSudokuGame.resume()
			if (mShowTime) {
				mGameTimer.start()
			}
		}
		val moveCellSelectionOnPress = gameSettings.getBoolean("im_numpad_move_right", false)
		mSudokuBoard.setMoveCellSelectionOnPress(moveCellSelectionOnPress)
		mIMNumpad.isMoveCellSelectionOnPress = (moveCellSelectionOnPress)
		mIMPopup.isEnabled = (gameSettings.getBoolean("im_popup", true))
		mIMSingleNumber.isEnabled = (gameSettings.getBoolean("im_single_number", true))
		mIMNumpad.isEnabled = (gameSettings.getBoolean("im_numpad", true))
		mIMPopup.setHighlightCompletedValues(
			gameSettings.getBoolean(
				"highlight_completed_values",
				true
			)
		)
		mIMPopup.setShowNumberTotals(gameSettings.getBoolean("show_number_totals", false))
		mIMSingleNumber.setHighlightCompletedValues(
			gameSettings.getBoolean(
				"highlight_completed_values",
				true
			)
		)
		mIMSingleNumber.setShowNumberTotals(gameSettings.getBoolean("show_number_totals", false))
		mIMSingleNumber.setBidirectionalSelection(
			gameSettings.getBoolean(
				"bidirectional_selection",
				true
			)
		)
		mIMSingleNumber.setHighlightSimilar(gameSettings.getBoolean("highlight_similar", true))
		mIMSingleNumber.setOnSelectedNumberChangedListener(onSelectedNumberChangedListener)
		mIMNumpad.setHighlightCompletedValues(
			gameSettings.getBoolean(
				"highlight_completed_values",
				true
			)
		)
		mIMNumpad.setShowNumberTotals(gameSettings.getBoolean("show_number_totals", false))
		mIMControlPanel.activateFirstInputMethod() // make sure that some input method is activated
		mIMControlPanelStatePersister.restoreState(mIMControlPanel)
		if (!mSudokuBoard.isReadOnly) {
			mSudokuBoard.invokeOnCellSelected()
		}
		updateTime()
	}

	override fun onPause() {
		super.onPause()

		// we will save game to the database as we might not be able to get back
		mDatabase.updatePuzzle(mSudokuGame)
		mGameTimer.stop()
		mIMControlPanel.pause()
		mIMControlPanelStatePersister.saveState(mIMControlPanel)
	}

	override fun onDestroy() {
		super.onDestroy()
		mDatabase.close()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		mGameTimer.stop()
		if (mSudokuGame.state == SudokuGame.GAME_STATE_PLAYING) {
			mSudokuGame.pause()
		}
		mSudokuGame.saveState(outState)
		mGameTimer.saveState(outState)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)
		// action menu
		menu.add(0, MENU_ITEM_UNDO_ACTION, 0, R.string.undo)
			.setIcon(R.drawable.ic_undo)
			.setEnabled(mSudokuGame.hasSomethingToUndo())
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
		menu.add(0, MENU_ITEM_SETTINGS_ACTION, 8, R.string.settings)
			.setIcon(R.drawable.ic_settings)
			.setEnabled(true)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

		// drop-down menu
		menu.add(0, MENU_ITEM_UNDO, 0, R.string.undo)
			.setShortcut('1', 'u')
			.setIcon(R.drawable.ic_undo)
		if (mFillInNotesEnabled) {
			menu.add(0, MENU_ITEM_FILL_IN_NOTES, 1, R.string.fill_in_notes)
				.setIcon(R.drawable.ic_edit_grey)
		}
		menu.add(0, MENU_ITEM_FILL_IN_NOTES_WITH_ALL_VALUES, 1, R.string.fill_all_notes)
			.setIcon(R.drawable.ic_edit_grey)
		menu.add(0, MENU_ITEM_CLEAR_ALL_NOTES, 2, R.string.clear_all_notes)
			.setShortcut('3', 'a')
			.setIcon(R.drawable.ic_delete)
		menu.add(0, MENU_ITEM_SET_CHECKPOINT, 3, R.string.set_checkpoint)
		menu.add(0, MENU_ITEM_UNDO_TO_CHECKPOINT, 4, R.string.undo_to_checkpoint)
		menu.add(0, MENU_ITEM_UNDO_TO_BEFORE_MISTAKE, 4, R.string.undo_to_before_mistake)
		menu.add(0, MENU_ITEM_HINT, 5, R.string.hint)
		menu.add(0, MENU_ITEM_SOLVE, 6, R.string.solve_puzzle)
		menu.add(0, MENU_ITEM_RESTART, 7, R.string.restart)
			.setShortcut('7', 'r')
			.setIcon(R.drawable.ic_restore)
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
		if (mSudokuGame.state == SudokuGame.GAME_STATE_PLAYING) {
			menu.findItem(MENU_ITEM_CLEAR_ALL_NOTES).setEnabled(true)
			if (mFillInNotesEnabled) {
				menu.findItem(MENU_ITEM_FILL_IN_NOTES).setEnabled(true)
			}
			menu.findItem(MENU_ITEM_FILL_IN_NOTES_WITH_ALL_VALUES).setEnabled(true)
			menu.findItem(MENU_ITEM_UNDO).setEnabled(mSudokuGame.hasSomethingToUndo())
			menu.findItem(MENU_ITEM_UNDO_TO_CHECKPOINT).setEnabled(mSudokuGame.hasUndoCheckpoint())
		} else {
			menu.findItem(MENU_ITEM_CLEAR_ALL_NOTES).setEnabled(false)
			if (mFillInNotesEnabled) {
				menu.findItem(MENU_ITEM_FILL_IN_NOTES).setEnabled(false)
			}
			menu.findItem(MENU_ITEM_FILL_IN_NOTES_WITH_ALL_VALUES).setEnabled(false)
			menu.findItem(MENU_ITEM_UNDO).setEnabled(false)
			menu.findItem(MENU_ITEM_UNDO_TO_CHECKPOINT).setEnabled(false)
			menu.findItem(MENU_ITEM_UNDO_TO_BEFORE_MISTAKE).setEnabled(false)
			menu.findItem(MENU_ITEM_SOLVE).setEnabled(false)
			menu.findItem(MENU_ITEM_HINT).setEnabled(false)
		}
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			MENU_ITEM_RESTART -> {
				with(SimpleDialog(supportFragmentManager)) {
					iconId = R.drawable.ic_restore
					titleId = R.string.app_name
					messageId = R.string.restart_confirm
					onOkCallback = ::resetGame
					show()
				}
				return true
			}

			MENU_ITEM_CLEAR_ALL_NOTES -> {
				with(SimpleDialog(supportFragmentManager)) {
					iconId = R.drawable.ic_delete
					titleId = R.string.app_name
					messageId = R.string.clear_all_notes_confirm
					onOkCallback = mSudokuGame::clearAllNotes
					show()
				}
				return true
			}

			MENU_ITEM_FILL_IN_NOTES -> {
				mSudokuGame.fillInNotes()
				return true
			}

			MENU_ITEM_FILL_IN_NOTES_WITH_ALL_VALUES -> {
				mSudokuGame.fillInNotesWithAllValues()
				return true
			}

			MENU_ITEM_UNDO_ACTION, MENU_ITEM_UNDO -> {
				val undoneCell = mSudokuGame.undo()
				selectCell(undoneCell)
				return true
			}

			MENU_ITEM_SETTINGS_ACTION, MENU_ITEM_SETTINGS -> {
				val i = Intent()
				i.setClass(this, GameSettingsActivity::class.java)
				settingsLauncher.launch(i)
				return true
			}

			MENU_ITEM_HELP -> {
				mHintsQueue.showHint(R.string.help, R.string.help_text)
				return true
			}

			MENU_ITEM_SET_CHECKPOINT -> {
				mSudokuGame.setUndoCheckpoint()
				return true
			}

			MENU_ITEM_UNDO_TO_CHECKPOINT -> {
				with(SimpleDialog(supportFragmentManager)) {
					iconId = R.drawable.ic_undo
					titleId = R.string.app_name
					messageId = R.string.undo_to_checkpoint_confirm
					onOkCallback = {
						mSudokuGame.undoToCheckpoint()
						selectLastCommandCell()
					}
					show()
				}
				return true
			}

			MENU_ITEM_UNDO_TO_BEFORE_MISTAKE -> {
				with(SimpleDialog(supportFragmentManager)) {
					iconId = R.drawable.ic_undo
					titleId = R.string.app_name
					messageId = R.string.undo_to_before_mistake_confirm
					onOkCallback = {
						mSudokuGame.undoToBeforeMistake()
						selectLastCommandCell()
					}
					show()
				}
				return true
			}

			MENU_ITEM_SOLVE -> {
				with(SimpleDialog(supportFragmentManager)) {
					titleId = R.string.app_name
					messageId = R.string.solve_puzzle_confirm
					onOkCallback = {
						val numberOfSolutions = mSudokuGame.solve()
						if (numberOfSolutions == 0) {
							SimpleDialog(supportFragmentManager).show(R.string.puzzle_has_no_solution)
						} else if (numberOfSolutions > 1) {
							SimpleDialog(supportFragmentManager).show(R.string.puzzle_has_multiple_solutions)
						}
					}
					show()
				}
				return true
			}

			MENU_ITEM_HINT -> {
				with(SimpleDialog(supportFragmentManager)) {
					messageId = R.string.hint_confirm
					onOkCallback = {
						val cell = mSudokuBoard.selectedCell
						if (cell != null && cell.isEditable) {
							when (mSudokuGame.solutionCount) {
								1 -> {
									mSudokuGame.solveCell(cell)
								}

								0 -> {
									SimpleDialog(supportFragmentManager).show(R.string.puzzle_has_no_solution)
								}

								else -> {
									SimpleDialog(supportFragmentManager).show(R.string.puzzle_has_multiple_solutions)
								}
							}
						} else {
							SimpleDialog(supportFragmentManager).show(R.string.cannot_give_hint)
						}
					}
					show()
				}
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	private fun resetGame() {
		// Restart game
		mSudokuGame.reset()
		mSudokuGame.start()
		mSudokuBoard.isReadOnly = (false)
		if (mShowTime) {
			mGameTimer.start()
		}
		mOptionsMenu.findItem(MENU_ITEM_SOLVE).setEnabled(true)
		mOptionsMenu.findItem(MENU_ITEM_HINT).setEnabled(true)
		mOptionsMenu.findItem(MENU_ITEM_UNDO_ACTION).setEnabled(mSudokuGame.hasSomethingToUndo())
	}

	/**
	 * Restarts whole activity.
	 */
	private fun restartActivity() {
		startActivity(intent)
		finish()
	}

	private fun selectLastCommandCell() {
		selectCell(mSudokuGame.lastCommandCell ?: return)
	}

	private fun selectCell(cell: Cell?) {
		if (cell != null) {
			mSudokuBoard.moveCellSelectionTo(cell.rowIndex, cell.columnIndex)
		}
	}

	/**
	 * Update the time of game-play.
	 */
	fun updateTime() {
		if (mShowTime) {
			title = mGameTimeFormatter.format(mSudokuGame.time)
		} else {
			setTitle(R.string.app_name)
		}
	}

	interface OnSelectedNumberChangedListener {
		fun onSelectedNumberChanged(number: Int)
	}

	companion object {
		const val EXTRA_PUZZLE_ID = "puzzle_id"
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

		// This class implements the game clock.  All it does is update the status each tick.
		private class GameTimer(private val sudokuPlayActivity: SudokuPlayActivity) : Timer(1000) {
			override fun step(count: Int, time: Long): Boolean {
				sudokuPlayActivity.updateTime()
				return false // Run until explicitly stopped.
			}
		}
	}
}
