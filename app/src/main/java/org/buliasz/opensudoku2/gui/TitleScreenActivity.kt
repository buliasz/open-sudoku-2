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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.preference.PreferenceManager
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names.PUZZLE_ID
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.fragments.AboutDialogFragment

class TitleScreenActivity : ThemedActivity() {
	private lateinit var mResumeButton: Button
	private lateinit var aboutDialog: AboutDialogFragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_title_screen)
		mResumeButton = findViewById(R.id.resume_button)
		val mPuzzleListButton = findViewById<Button>(R.id.puzzle_lists_button)
		val mSettingsButton = findViewById<Button>(R.id.settings_button)
		setupResumeButton()
		mPuzzleListButton.setOnClickListener { startActivity(Intent(this, FolderListActivity::class.java)) }
		mSettingsButton.setOnClickListener { startActivity(Intent(this, GameSettingsActivity::class.java)) }
		val factory = LayoutInflater.from(this)
		aboutDialog = AboutDialogFragment(factory)

		// check the preference to skip the title screen and launch the folder list activity
		// directly
		val gameSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
		val showPuzzleFolderListOnStartup = gameSettings.getBoolean("show_puzzle_lists_on_startup", false)
		if (showPuzzleFolderListOnStartup) {
			startActivity(Intent(this, FolderListActivity::class.java))
		} else {
			// show changelog on first run
			val changelog = Changelog(this)
			changelog.showOnFirstRun()
		}
	}

	private fun canResume(mSudokuGameID: Long): Boolean {
		val mSudokuGame = SudokuDatabase(applicationContext, true).use { db ->
			db.getPuzzle(mSudokuGameID) ?: return@canResume false
		}
		return mSudokuGame.state != SudokuGame.GAME_STATE_COMPLETED
	}

	private fun setupResumeButton() {
		val gameSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
		val puzzleID = gameSettings.getLong("most_recently_played_puzzle_id", 0)
		if (canResume(puzzleID)) {
			mResumeButton.visibility = View.VISIBLE
			mResumeButton.setOnClickListener {
				val intentToPlay = Intent(this@TitleScreenActivity, SudokuPlayActivity::class.java)
				intentToPlay.putExtra(PUZZLE_ID, puzzleID)
				startActivity(intentToPlay)
			}
		} else {
			mResumeButton.visibility = View.GONE
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)
		menu.add(0, MenuItems.SETTINGS.id, 0, R.string.settings)
			.setShortcut('0', 's')
			.setIcon(R.drawable.ic_settings)
		menu.add(0, MenuItems.ABOUT.id, 1, R.string.about)
			.setShortcut('1', 'h')
			.setIcon(R.drawable.ic_info)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			MenuItems.SETTINGS.id -> {
				startActivity(Intent(this, GameSettingsActivity::class.java))
				return true
			}

			MenuItems.ABOUT.id -> {
				aboutDialog.show(supportFragmentManager, "AboutDialog")
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onResume() {
		super.onResume()
		setupResumeButton()
	}

	companion object {
		enum class MenuItems {
			SETTINGS,
			ABOUT;
			val id = ordinal + Menu.FIRST
		}
	}
}
