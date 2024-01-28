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
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceManager
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names.PUZZLE_ID
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.SudokuBoardView.HighlightMode
import org.buliasz.opensudoku2.gui.fragments.SimpleDialog
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanel
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanelStatePersister
import org.buliasz.opensudoku2.gui.inputmethod.IMInsertOnTap
import org.buliasz.opensudoku2.gui.inputmethod.IMPopup
import org.buliasz.opensudoku2.gui.inputmethod.IMSelectOnTap
import org.buliasz.opensudoku2.utils.ThemeUtils


class SudokuPlayActivity : ThemedActivity() {
	private var bellEnabled: Boolean = true
	private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
	private lateinit var mSudokuGame: SudokuGame
	private lateinit var mDatabase: SudokuDatabase
	private lateinit var mRootLayout: ViewGroup
	private lateinit var mSudokuBoard: SudokuBoardView
	private lateinit var mOptionsMenu: Menu
	private lateinit var mIMControlPanel: IMControlPanel
	private lateinit var mIMControlPanelStatePersister: IMControlPanelStatePersister
	private lateinit var mIMPopup: IMPopup
	private lateinit var mIMInsertOnTap: IMInsertOnTap
	private lateinit var mIMSelectOnTap: IMSelectOnTap
	private var mShowTime = true
	private lateinit var mGameTimer: GameTimer
	private val mGameTimeFormatter = GameTimeFormat()
	private var mFillInNotesEnabled = false
	private lateinit var mHintsQueue: HintsQueue

	/**
	 * Occurs when puzzle is solved.
	 */
	private val onSolvedListener = {
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
				positiveButtonCallback = ::finish
				show()
			}
		}
	}

	/**
	 * Occurs when puzzle is solved.
	 */
	private val onDigitFinishedListener: ((Int) -> Unit) = { digit ->
		mSudokuBoard.blinkValue(digit)
		val audioService = getSystemService(Context.AUDIO_SERVICE) as AudioManager
		if (bellEnabled && !audioService.isStreamMute(AudioManager.STREAM_MUSIC) && audioService.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
			val volume = 100 * audioService.getStreamVolume(AudioManager.STREAM_MUSIC) / audioService.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
			ToneGenerator(AudioManager.STREAM_MUSIC, volume).startTone(ToneGenerator.TONE_PROP_ACK, 300)
		}
	}

	private val onSelectedNumberChangedListener: (Int) -> Unit = {
		mSudokuBoard.highlightedValue = it
		mSudokuBoard.clearCellSelection()
	}

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.sudoku_play)
		mRootLayout = findViewById(R.id.play_root_layout)
		mSudokuBoard = findViewById(R.id.play_board_view)
		mDatabase = SudokuDatabase(applicationContext, false)
		mHintsQueue = HintsQueue(this)
		mGameTimer = GameTimer(this)

		// create sudoku game instance
		if (savedInstanceState == null) {
			// activity runs for the first time, read game from database
			val mSudokuGameID = intent.getLongExtra(PUZZLE_ID, 0)
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
			mSudokuBoard.isReadOnly = true
		}
		mSudokuBoard.setGame(mSudokuGame)
		mSudokuGame.onPuzzleSolvedListener = onSolvedListener
		mSudokuGame.onDigitFinishedManuallyListener = onDigitFinishedListener
		mSudokuGame.onHasUndoChangedListener =
			{ isUndoStackEmpty -> mOptionsMenu.findItem(MenuItems.UNDO_ACTION.id).setEnabled(!isUndoStackEmpty) }
		mHintsQueue.showOneTimeHint("welcome", R.string.welcome, R.string.first_run_hint)
		mIMControlPanel = findViewById(R.id.input_methods)
		mIMControlPanel.initialize(mSudokuBoard, mSudokuGame, mHintsQueue)
		mIMControlPanelStatePersister = IMControlPanelStatePersister(this)
		mIMPopup = mIMControlPanel.imPopup
		mIMInsertOnTap = mIMControlPanel.imInsertOnTap
		mIMSelectOnTap = mIMControlPanel.imSelectOnTap
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
		bellEnabled = gameSettings.getBoolean("bell_enabled", true)
		val theme = gameSettings.getString("theme", "opensudoku2")
		if (theme == "custom" || theme == "custom_light") {
			ThemeUtils.applyCustomThemeToSudokuBoardViewFromSharedPreferences(this, mSudokuBoard)
		} else {
			mSudokuBoard.setAllColorsFromThemedContext(this)
		}
		mSudokuBoard.highlightDirectlyWrongValues = gameSettings.getBoolean("highlight_directly_wrong_values", true)
		mSudokuBoard.highlightIndirectlyWrongValues = gameSettings.getBoolean("highlight_indirectly_wrong_values", true)
		mSudokuBoard.highlightTouchedCell = gameSettings.getBoolean("highlight_touched_cell", true)
		val highlightSimilarCells = gameSettings.getBoolean("highlight_similar_cells", true)
		val highlightSimilarNotes = gameSettings.getBoolean("highlight_similar_notes", true)
		if (highlightSimilarCells) {
			mSudokuBoard.highlightSimilarCells = if (highlightSimilarNotes) HighlightMode.NUMBERS_AND_NOTES else HighlightMode.NUMBERS
		} else {
			mSudokuBoard.highlightSimilarCells = HighlightMode.NONE
		}
		mSudokuGame.removeNotesOnEntry = gameSettings.getBoolean("remove_notes_on_input", true)
		mShowTime = gameSettings.getBoolean("show_time", true)
		if (mSudokuGame.state == SudokuGame.GAME_STATE_PLAYING) {
			mSudokuGame.resume()
			if (mShowTime) mGameTimer.start()
		}
		val moveCellSelectionOnPress = gameSettings.getBoolean("im_move_right_on_insert_move_right", false)
		mSudokuBoard.moveCellSelectionOnPress = moveCellSelectionOnPress
		mIMSelectOnTap.isMoveCellSelectionOnPress = (moveCellSelectionOnPress)
		mIMPopup.isEnabled = (gameSettings.getBoolean("im_popup", true))
		mIMInsertOnTap.isEnabled = (gameSettings.getBoolean("insert_on_tap", true))
		mIMSelectOnTap.isEnabled = (gameSettings.getBoolean("select_on_tap", true))
		mIMPopup.highlightCompletedValues = gameSettings.getBoolean("highlight_completed_values", true)
		mIMPopup.showDigitCount = gameSettings.getBoolean("show_number_totals", false)
		mIMInsertOnTap.highlightCompletedValues = gameSettings.getBoolean("highlight_completed_values", true)
		mIMInsertOnTap.showDigitCount = gameSettings.getBoolean("show_number_totals", false)
		mIMInsertOnTap.bidirectionalSelection = gameSettings.getBoolean("bidirectional_selection", true)
		mIMInsertOnTap.highlightSimilar = gameSettings.getBoolean("highlight_similar", true)
		mIMInsertOnTap.onSelectedNumberChangedListener = onSelectedNumberChangedListener
		mIMSelectOnTap.highlightCompletedValues = gameSettings.getBoolean("highlight_completed_values", true)
		mIMSelectOnTap.showDigitCount = gameSettings.getBoolean("show_number_totals", false)
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
		menu.add(0, MenuItems.BELL_ACTION.id, 0, "bell")
			.setIcon(if (bellEnabled) R.drawable.notifications_active else R.drawable.notifications_off)
			.setEnabled(true)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
		menu.add(0, MenuItems.UNDO_ACTION.id, 1, R.string.undo)
			.setIcon(R.drawable.ic_undo)
			.setEnabled(mSudokuGame.hasSomethingToUndo())
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
		menu.add(0, MenuItems.SETTINGS_ACTION.id, 2, R.string.settings)
			.setIcon(R.drawable.ic_settings)
			.setEnabled(true)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

		// drop-down menu
		menu.add(0, MenuItems.UNDO.id, 0, R.string.undo)
			.setShortcut('1', 'u')
			.setIcon(R.drawable.ic_undo)
		if (mFillInNotesEnabled) {
			menu.add(0, MenuItems.FILL_IN_NOTES.id, 1, R.string.fill_in_notes)
				.setIcon(R.drawable.ic_edit_grey)
		}
		menu.add(0, MenuItems.FILL_IN_NOTES_WITH_ALL_VALUES.id, 1, R.string.fill_all_notes)
			.setIcon(R.drawable.ic_edit_grey)
		menu.add(0, MenuItems.CLEAR_ALL_NOTES.id, 2, R.string.clear_all_notes)
			.setShortcut('3', 'a')
			.setIcon(R.drawable.ic_delete)
		menu.add(0, MenuItems.SET_CHECKPOINT.id, 3, R.string.set_checkpoint)
		menu.add(0, MenuItems.UNDO_TO_CHECKPOINT.id, 4, R.string.undo_to_checkpoint)
		menu.add(0, MenuItems.UNDO_TO_BEFORE_MISTAKE.id, 4, R.string.undo_to_before_mistake)
		menu.add(0, MenuItems.HINT.id, 5, R.string.hint)
		menu.add(0, MenuItems.SOLVE.id, 6, R.string.solve_puzzle)
		menu.add(0, MenuItems.RESTART.id, 7, R.string.restart)
			.setShortcut('7', 'r')
			.setIcon(R.drawable.ic_restore)
		menu.add(0, MenuItems.SETTINGS.id, 8, R.string.settings)
			.setShortcut('9', 's')
			.setIcon(R.drawable.ic_settings)
		menu.add(0, MenuItems.HELP.id, 9, R.string.help)
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
			menu.findItem(MenuItems.CLEAR_ALL_NOTES.id).setEnabled(true)
			if (mFillInNotesEnabled) {
				menu.findItem(MenuItems.FILL_IN_NOTES.id).setEnabled(true)
			}
			menu.findItem(MenuItems.FILL_IN_NOTES_WITH_ALL_VALUES.id).setEnabled(true)
			menu.findItem(MenuItems.UNDO.id).setEnabled(mSudokuGame.hasSomethingToUndo())
			menu.findItem(MenuItems.UNDO_TO_CHECKPOINT.id).setEnabled(mSudokuGame.hasUndoCheckpoint())
		} else {
			menu.findItem(MenuItems.CLEAR_ALL_NOTES.id).setEnabled(false)
			if (mFillInNotesEnabled) {
				menu.findItem(MenuItems.FILL_IN_NOTES.id).setEnabled(false)
			}
			menu.findItem(MenuItems.FILL_IN_NOTES_WITH_ALL_VALUES.id).setEnabled(false)
			menu.findItem(MenuItems.UNDO.id).setEnabled(false)
			menu.findItem(MenuItems.UNDO_TO_CHECKPOINT.id).setEnabled(false)
			menu.findItem(MenuItems.UNDO_TO_BEFORE_MISTAKE.id).setEnabled(false)
			menu.findItem(MenuItems.SOLVE.id).setEnabled(false)
			menu.findItem(MenuItems.HINT.id).setEnabled(false)
		}
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			MenuItems.RESTART.id -> {
				with(SimpleDialog(supportFragmentManager)) {
					iconId = R.drawable.ic_restore
					titleId = R.string.app_name
					messageId = R.string.restart_confirm
					positiveButtonCallback = ::resetGame
					show()
				}
				return true
			}

			MenuItems.CLEAR_ALL_NOTES.id -> {
				with(SimpleDialog(supportFragmentManager)) {
					iconId = R.drawable.ic_delete
					titleId = R.string.app_name
					messageId = R.string.clear_all_notes_confirm
					positiveButtonCallback = mSudokuGame::clearAllNotesManual
					show()
				}
				return true
			}

			MenuItems.FILL_IN_NOTES.id -> {
				mSudokuGame.fillInNotesManual()
				return true
			}

			MenuItems.FILL_IN_NOTES_WITH_ALL_VALUES.id -> {
				mSudokuGame.fillInNotesWithAllValuesManual()
				return true
			}

			MenuItems.UNDO_ACTION.id, MenuItems.UNDO.id -> {
				val undoneCell = mSudokuGame.undo()
				selectCell(undoneCell)
				return true
			}

			MenuItems.BELL_ACTION.id -> {
				bellEnabled = !bellEnabled
				val gameSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
				val editor = gameSettings.edit()
				editor.putBoolean("bell_enabled", bellEnabled)
				editor.apply()
				item.setIcon(if (bellEnabled) R.drawable.notifications_active else R.drawable.notifications_off)
				return true
			}

			MenuItems.SETTINGS_ACTION.id, MenuItems.SETTINGS.id -> {
				val i = Intent()
				i.setClass(this, GameSettingsActivity::class.java)
				settingsLauncher.launch(i)
				return true
			}

			MenuItems.HELP.id -> {
				mHintsQueue.showHint(R.string.help, R.string.help_text)
				return true
			}

			MenuItems.SET_CHECKPOINT.id -> {
				mSudokuGame.setUndoCheckpoint()
				return true
			}

			MenuItems.UNDO_TO_CHECKPOINT.id -> {
				with(SimpleDialog(supportFragmentManager)) {
					iconId = R.drawable.ic_undo
					titleId = R.string.app_name
					messageId = R.string.undo_to_checkpoint_confirm
					positiveButtonCallback = {
						mSudokuGame.undoToCheckpoint()
						selectLastCommandCell()
					}
					show()
				}
				return true
			}

			MenuItems.UNDO_TO_BEFORE_MISTAKE.id -> {
				with(SimpleDialog(supportFragmentManager)) {
					iconId = R.drawable.ic_undo
					titleId = R.string.app_name
					messageId = R.string.undo_to_before_mistake_confirm
					positiveButtonCallback = {
						mSudokuGame.undoToBeforeMistake()
						selectLastCommandCell()
					}
					show()
				}
				return true
			}

			MenuItems.SOLVE.id -> {
				with(SimpleDialog(supportFragmentManager)) {
					titleId = R.string.app_name
					messageId = R.string.solve_puzzle_confirm
					positiveButtonCallback = {
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

			MenuItems.HINT.id -> {
				with(SimpleDialog(supportFragmentManager)) {
					messageId = R.string.hint_confirm
					positiveButtonCallback = {
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
		mOptionsMenu.findItem(MenuItems.SOLVE.id).setEnabled(true)
		mOptionsMenu.findItem(MenuItems.HINT.id).setEnabled(true)
		mOptionsMenu.findItem(MenuItems.UNDO_ACTION.id).setEnabled(mSudokuGame.hasSomethingToUndo())
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

	companion object {
		enum class MenuItems {
			RESTART,
			CLEAR_ALL_NOTES,
			FILL_IN_NOTES,
			FILL_IN_NOTES_WITH_ALL_VALUES,
			UNDO_ACTION,
			UNDO,
			HELP,
			SETTINGS_ACTION,
			SETTINGS,
			SET_CHECKPOINT,
			UNDO_TO_CHECKPOINT,
			UNDO_TO_BEFORE_MISTAKE,
			SOLVE,
			HINT,
			BELL_ACTION;

			val id = ordinal + Menu.FIRST
		}

		// This class implements the game clock.  All it does is update the status each tick.
		private class GameTimer(private val sudokuPlayActivity: SudokuPlayActivity) : Timer(1000, Looper.getMainLooper()) {
			override fun step(count: Int, time: Long): Boolean {
				sudokuPlayActivity.updateTime()
				return false // Run until explicitly stopped.
			}
		}
	}
}
