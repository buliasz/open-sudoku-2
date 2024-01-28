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

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.extractSudokuGameFromCursorRow
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.utils.ThemeUtils
import java.io.Closeable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PuzzleListRecyclerAdapter(
	private val mContext: Context,
	private var puzzlesCursor: Cursor,
	private val onClickListener: (Long) -> Unit
) : RecyclerView.Adapter<PuzzleListRecyclerAdapter.ViewHolder?>(), Closeable {
	private val LocalZoneOffset: ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())
	var selectedGameId: Long = 0
	private val mGameTimeFormatter = GameTimeFormat()
	private val mDateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
	private val mTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.sudoku_list_item, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = puzzlesCursor.count

	@SuppressLint("NotifyDataSetChanged")
	fun updateGameList(newGames: Cursor) {
		puzzlesCursor.close()
		puzzlesCursor = newGames
		notifyDataSetChanged()
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		puzzlesCursor.moveToPosition(position)
		val game: SudokuGame = extractSudokuGameFromCursorRow(puzzlesCursor)
		holder.itemView.setOnClickListener { onClickListener(game.id) }
		holder.itemView.setOnCreateContextMenuListener { menu, _, _ ->
			selectedGameId = game.id
			with(menu ?: return@setOnCreateContextMenuListener) {
				setHeaderTitle("Puzzle")
				add(0, PuzzleListActivity.MenuItems.PLAY.id, 0, R.string.play_puzzle)
				add(0, PuzzleListActivity.MenuItems.EDIT_NOTE.id, 1, R.string.edit_note)
				add(0, PuzzleListActivity.MenuItems.EXPORT_GAME.id, 2, R.string.export)
				add(0, PuzzleListActivity.MenuItems.RESET.id, 3, R.string.reset_puzzle)
				add(0, PuzzleListActivity.MenuItems.EDIT.id, 4, R.string.edit_puzzle)
				add(0, PuzzleListActivity.MenuItems.DELETE.id, 5, R.string.delete_puzzle)
			}
		}

//          R.id.board_view
		holder.boardView.isReadOnly = true
		holder.boardView.isFocusable = false
		holder.boardView.cells = game.cells
		ThemeUtils.applyThemeToSudokuBoardViewFromContext(
			ThemeUtils.getCurrentThemeFromPreferences(mContext),
			holder.boardView,
			mContext
		)

//          R.id.state
		var stateString: String? = null
		when (game.state) {
			SudokuGame.GAME_STATE_COMPLETED -> stateString = mContext.getString(R.string.solved)
			SudokuGame.GAME_STATE_PLAYING -> stateString = mContext.getString(R.string.playing)
		}
		holder.state.visibility = if (stateString == null) View.GONE else View.VISIBLE
		holder.state.text = stateString
		if (game.state == SudokuGame.GAME_STATE_COMPLETED) {
			holder.state.setTextColor(ThemeUtils.getCurrentThemeColor(holder.userNote.context, android.R.attr.colorAccent))
		} else {
			holder.state.setTextColor(ThemeUtils.getCurrentThemeColor(holder.userNote.context, android.R.attr.textColorPrimary))
		}

//            R.id.time
		var timeString: String? = null
		if (game.time != 0L) {
			timeString = mGameTimeFormatter.format(game.time)
		}
		holder.time.visibility = if (timeString == null) View.GONE else View.VISIBLE
		holder.time.text = timeString
		if (game.state == SudokuGame.GAME_STATE_COMPLETED) {
			holder.time.setTextColor(ThemeUtils.getCurrentThemeColor(holder.userNote.context, android.R.attr.colorAccent))
		} else {
			holder.time.setTextColor(ThemeUtils.getCurrentThemeColor(holder.userNote.context, android.R.attr.textColorPrimary))
		}

//            R.id.last_played
		var lastPlayedString: String? = null
		if (game.lastPlayed != 0L) {
			lastPlayedString = mContext.getString(R.string.last_played_at, getDateAndTimeForHumans(game.lastPlayed))
		}
		holder.lastPlayed.visibility = if (lastPlayedString == null) View.GONE else View.VISIBLE
		holder.lastPlayed.text = lastPlayedString

//          R.id.created
		var createdString: String? = null
		if (game.created != 0L) {
			createdString = mContext.getString(R.string.created_at, getDateAndTimeForHumans(game.created))
		}
		holder.created.visibility = if (createdString == null) View.GONE else View.VISIBLE
		holder.created.text = createdString

//          R.id.user_note
		if (game.userNote == "") {
			holder.userNote.visibility = View.GONE
		} else {
			holder.userNote.text = game.userNote
		}
		holder.userNote.visibility = if (game.userNote == "") View.GONE else View.VISIBLE
		holder.userNote.text = game.userNote
	}

	private fun getDateAndTimeForHumans(epochSeconds: Long): String {
		val dateTime = LocalDateTime.ofEpochSecond(epochSeconds, 0, LocalZoneOffset)
		val today = LocalDate.now()
		val yesterday = today.minusDays(1)
		return if (dateTime.isAfter(today.atStartOfDay())) {
			mContext.getString(R.string.at_time, dateTime.format(mTimeFormatter))
		} else if (dateTime.isAfter(yesterday.atStartOfDay())) {
			mContext.getString(R.string.yesterday_at_time, dateTime.format(mTimeFormatter))
		} else {
			mContext.getString(R.string.on_date, dateTime.format(mDateTimeFormatter))
		}
	}

	internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val boardView: SudokuBoardView = itemView.findViewById(R.id.board_view)
		val state: TextView = itemView.findViewById(R.id.state)
		val time: TextView = itemView.findViewById(R.id.time)
		val lastPlayed: TextView = itemView.findViewById(R.id.last_played)
		val created: TextView = itemView.findViewById(R.id.created)
		val userNote: TextView = itemView.findViewById(R.id.user_note)
	}

	override fun close() {
		puzzlesCursor.close()
	}
}
