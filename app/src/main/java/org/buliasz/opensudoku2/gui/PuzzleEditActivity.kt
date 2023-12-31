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

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.fragments.SimpleDialog
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanel

/**
 * Activity for editing content of puzzle.
 */
class PuzzleEditActivity : ThemedActivity() {
	private var mState = 0
	private lateinit var mDatabase: SudokuDatabase
	private lateinit var newPuzzle: SudokuGame
	private lateinit var mRootLayout: ViewGroup
	private lateinit var mClipboard: ClipboardManager
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.sudoku_edit)
		mRootLayout = findViewById(R.id.root_layout)
		val mBoard = findViewById<SudokuBoardView>(R.id.board_view)
		mDatabase = SudokuDatabase(applicationContext)
		val intent = intent
		val action = intent.action
		val mPuzzleID: Long
		if (Intent.ACTION_EDIT == action) {
			// Requested to edit: set that state, and the data being edited.
			mState = STATE_EDIT
			mPuzzleID = if (intent.hasExtra(EXTRA_PUZZLE_ID)) {
				intent.getLongExtra(EXTRA_PUZZLE_ID, 0)
			} else {
				throw IllegalArgumentException("Extra with key '$EXTRA_PUZZLE_ID' is required.")
			}
		} else if (Intent.ACTION_INSERT == action) {
			mState = STATE_INSERT
			mPuzzleID = 0
		} else {
			// Whoops, unknown action!  Bail.
			Log.e(TAG, "Unknown action, exiting.")
			finish()
			return
		}
		if (savedInstanceState != null) {
			newPuzzle = SudokuGame()
			newPuzzle.restoreState(savedInstanceState)
		} else {
			if (mPuzzleID != 0L) {
				// existing puzzle, read it from database
				newPuzzle = mDatabase.getGame(mPuzzleID) ?: SudokuGame.createEmptyGame()
				newPuzzle.cells.markAllCellsAsEditable()
			} else {
				newPuzzle = SudokuGame.createEmptyGame()
			}
		}
		mBoard.setGame(newPuzzle)
		val mInputMethods = findViewById<IMControlPanel>(R.id.input_methods)
		mInputMethods.initialize(mBoard, newPuzzle, null)

		// only Numpad input method will be enabled
		for (im in mInputMethods.inputMethods) {
			im.isEnabled = false
		}
		mInputMethods.imNumpad.isEnabled = true
		mInputMethods.activateInputMethod(IMControlPanel.INPUT_METHOD_NUMPAD)
		mClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
	}

	override fun onPause() {
		super.onPause()
		if (isFinishing && mState != STATE_CANCEL && !newPuzzle.cells.isEmpty) {
			savePuzzle()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		mDatabase.close()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		newPuzzle.saveState(outState)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// This is our one standard application action -- inserting a
		// new note into the list.
		menu.add(0, MENU_ITEM_COPY, 0, android.R.string.copy)
		menu.add(0, MENU_ITEM_PASTE, 1, android.R.string.paste)
		menu.add(0, MENU_ITEM_CHECK_VALIDITY, 2, R.string.check_validity)
		menu.add(0, MENU_ITEM_SAVE, 3, R.string.save)
			.setShortcut('1', 's')
			.setIcon(R.drawable.ic_save)
		menu.add(0, MENU_ITEM_CANCEL, 4, android.R.string.cancel)
			.setShortcut('3', 'c')
			.setIcon(R.drawable.ic_close)

		// Generate any additional actions that can be performed on the
		// overall list.  In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		val intent = Intent(null, intent.data)
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
		menu.addIntentOptions(
			Menu.CATEGORY_ALTERNATIVE, 0, 0,
			ComponentName(this, PuzzleEditActivity::class.java), null, intent, 0, null
		)
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		super.onPrepareOptionsMenu(menu)
		if (!mClipboard.hasPrimaryClip()) {
			// If the clipboard doesn't contain data, disable the paste menu item.
			menu.findItem(MENU_ITEM_PASTE).setEnabled(false)
		} else if (!(mClipboard.primaryClipDescription!!.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
					mClipboard.primaryClipDescription!!.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))
		) {
			// This disables the paste menu item, since the clipboard has data but it is not plain text
			Toast.makeText(applicationContext, mClipboard.primaryClipDescription!!.getMimeType(0), Toast.LENGTH_SHORT).show()
			menu.findItem(MENU_ITEM_PASTE).setEnabled(false)
		} else {
			// This enables the paste menu item, since the clipboard contains plain text.
			menu.findItem(MENU_ITEM_PASTE).setEnabled(true)
		}
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			MENU_ITEM_COPY -> {
				copyToClipboard()
				return true
			}

			MENU_ITEM_PASTE -> {
				pasteFromClipboard()
				return true
			}

			MENU_ITEM_CHECK_VALIDITY -> {
				when (getNumberOfSolutions()) {
					1 -> {
						with(SimpleDialog()) {
							messageId = R.string.puzzle_solvable
							show(supportFragmentManager)
						}
					}

					0 -> {
						with(SimpleDialog()) {
							messageId = R.string.puzzle_has_no_solution
							show(supportFragmentManager)
						}
					}

					else -> {
						with(SimpleDialog()) {
							messageId = R.string.puzzle_has_multiple_solutions
							show(supportFragmentManager)
						}
					}
				}
				return true
			}

			MENU_ITEM_SAVE -> {
				if (isNewPuzzleValidAndUnique()) {
					finish()  // puzzle will be saved automatically in onPause
				}
				return true
			}

			MENU_ITEM_CANCEL -> {
				mState = STATE_CANCEL
				finish()
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	private fun getNumberOfSolutions(): Int {
		newPuzzle.cells.markFilledCellsAsNotEditable()
		val numberOfSolutions = newPuzzle.solutionCount
		newPuzzle.cells.markAllCellsAsEditable()
		return numberOfSolutions
	}

	private fun savePuzzle() {
		newPuzzle.cells.markFilledCellsAsNotEditable()
		when (mState) {
			STATE_EDIT -> {
				mDatabase.updatePuzzle(newPuzzle)
				Toast.makeText(applicationContext, R.string.puzzle_updated, Toast.LENGTH_SHORT).show()
			}

			STATE_INSERT -> {
				newPuzzle.created = System.currentTimeMillis()
				mDatabase.insertPuzzle(newPuzzle)
				Toast.makeText(applicationContext, R.string.puzzle_inserted, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun isNewPuzzleValidAndUnique(): Boolean {
		// check number of solutions
		val numberOfSolutions = getNumberOfSolutions()
		if (numberOfSolutions == 0) {
			with(SimpleDialog()) {
				messageId = R.string.puzzle_has_no_solution
				onOkCallback = ::finish
				show(supportFragmentManager)
			}
			return false
		} else if (numberOfSolutions > 1) {
			with(SimpleDialog()) {
				messageId = R.string.puzzle_has_multiple_solutions
				onOkCallback = ::finish
				show(supportFragmentManager)
			}
			return false
		}

		// check already existing puzzles
		val existingPuzzle = mDatabase.puzzleExists(newPuzzle.cells)
		if (existingPuzzle != null) {
			with(SimpleDialog()) {
				message =
					applicationContext.getString(R.string.puzzle_already_exists, mDatabase.getFolderInfo(existingPuzzle.folderId)?.name)
				onOkCallback = ::finish
				show(supportFragmentManager)
			}
			return false
		}

		return true
	}

	/**
	 * Copies puzzle to primary clipboard in a plain text format (81 character string).
	 *
	 * @see CellCollection.serialize
	 */
	private fun copyToClipboard() {
		val cells = newPuzzle.cells
		val serializedCells: String = cells.serialize(CellCollection.DATA_VERSION_PLAIN)
		val clipData = ClipData.newPlainText("Sudoku Puzzle", serializedCells)
		mClipboard.setPrimaryClip(clipData)
		Toast.makeText(applicationContext, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
	}

	/**
	 * Pastes puzzle from primary clipboard in any of the supported formats.
	 *
	 * @see CellCollection.serialize
	 */
	private fun pasteFromClipboard() {
		if (mClipboard.hasPrimaryClip()) {
			if (mClipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
				mClipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true
			) {
				val clipDataItem = mClipboard.primaryClip!!.getItemAt(0)
				val clipDataText = clipDataItem.text.toString()
				if (CellCollection.isValid(clipDataText)) {
					val cells: CellCollection = CellCollection.deserialize(clipDataText)
					newPuzzle.cells = cells
					(mRootLayout.getChildAt(0) as SudokuBoardView).cells = cells
					Toast.makeText(applicationContext, R.string.pasted_from_clipboard, Toast.LENGTH_SHORT).show()
				} else {
					Toast.makeText(applicationContext, R.string.invalid_puzzle_format, Toast.LENGTH_SHORT).show()
				}
			} else {
				Toast.makeText(applicationContext, R.string.invalid_mime_type, Toast.LENGTH_LONG).show()
			}
		}
	}

	companion object {
		/**
		 * When inserting new data, we need to know folder in which the new puzzle will be stored.
		 */
		const val EXTRA_PUZZLE_ID = "puzzle_id"
		const val MENU_ITEM_CHECK_VALIDITY = Menu.FIRST
		const val MENU_ITEM_SAVE = Menu.FIRST + 1
		const val MENU_ITEM_CANCEL = Menu.FIRST + 2
		const val MENU_ITEM_COPY = Menu.FIRST + 3
		const val MENU_ITEM_PASTE = Menu.FIRST + 4

		// The distinct states that the activity can be run in.
		private const val STATE_EDIT = 0
		private const val STATE_INSERT = 1
		private const val STATE_CANCEL = 2
		private const val TAG = "PuzzleEditActivity"
	}
}
