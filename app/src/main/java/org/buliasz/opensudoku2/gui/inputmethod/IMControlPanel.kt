/*
 * This file is part of Open Sudoku 2 - an open-source Sudoku game.
 * Copyright (C) 2009-2024 by original authors.
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
package org.buliasz.opensudoku2.gui.inputmethod

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.HintsQueue
import org.buliasz.opensudoku2.gui.SudokuBoardView
import java.util.Collections

class IMControlPanel : LinearLayout {
	val imPopup: IMPopup = IMPopup(this)
	val imInsertOnTap: IMInsertOnTap = IMInsertOnTap(this)
	val imSelectOnTap: IMSelectOnTap = IMSelectOnTap(this)
	private var mContext: Context
	private lateinit var mBoard: SudokuBoardView
	private lateinit var mGame: SudokuGame
	private var mHintsQueue: HintsQueue? = null
	private val mInputMethods: MutableList<InputMethod> = ArrayList()
	var activeMethodIndex = -1
		private set
	private val mOnCellTapListener = { cell: Cell ->
		if (activeMethodIndex != -1) {
			mInputMethods[activeMethodIndex].onCellTapped(cell)
		}
	}
	private val mOnCellSelected = { cell: Cell? ->
		if (activeMethodIndex != -1) {
			mInputMethods[activeMethodIndex].onCellSelected(cell)
		}
	}
	private val mSwitchModeListener = OnClickListener { _: View? -> activateNextInputMethod() }

	constructor(context: Context) : super(context) {
		mContext = context
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		mContext = context
	}

	fun initialize(board: SudokuBoardView, game: SudokuGame, hintsQueue: HintsQueue?) {
		board.onCellTappedListener = mOnCellTapListener
		board.onCellSelectedListener = mOnCellSelected
		mBoard = board
		mGame = game
		mHintsQueue = hintsQueue
		createInputMethods()
	}

	/**
	 * Activates first enabled input method. If such method does not exists, nothing
	 * happens.
	 */
	fun activateFirstInputMethod() {
		ensureInputMethods()
		if (activeMethodIndex == -1 || !mInputMethods[activeMethodIndex].isEnabled) {
			activateInputMethod(0)
		}
	}

	/**
	 * Activates given input method (see INPUT_METHOD_* constants). If the given method is
	 * not enabled, activates first available method after this method.
	 *
	 * @param methodID ID of method input to activate.
	 */
	fun activateInputMethod(methodID: Int) {
		require(!(methodID < -1 || methodID >= mInputMethods.size)) { "Invalid method id: $methodID." }
		ensureInputMethods()
		if (activeMethodIndex != -1) {
			mInputMethods[activeMethodIndex].deactivate()
		}
		var idFound = false
		var id = methodID
		var numOfCycles = 0
		if (id != -1) {
			while (numOfCycles <= mInputMethods.size) {
				if (mInputMethods[id].isEnabled) {
					ensureControlPanel(id)
					idFound = true
					break
				}
				id++
				if (id == mInputMethods.size) {
					id = 0
				}
				numOfCycles++
			}
		}
		if (!idFound) {
			id = -1
		}
		for (i in mInputMethods.indices) {
			val im = mInputMethods[i]
			if (im.isInputMethodViewCreated) {
				im.inputMethodView.visibility = if (i == id) VISIBLE else GONE
			}
		}
		activeMethodIndex = id
		if (activeMethodIndex != -1) {
			val activeMethod = mInputMethods[activeMethodIndex]
			activeMethod.activate()
			mHintsQueue?.showOneTimeHint(activeMethod.inputMethodName!!, activeMethod.nameResID, activeMethod.helpResID)
		}
	}

	fun activateNextInputMethod() {
		ensureInputMethods()
		var id = activeMethodIndex + 1
		if (id >= mInputMethods.size) {
			mHintsQueue?.showOneTimeHint("thatIsAll", R.string.that_is_all, R.string.im_disable_modes_hint)
			id = 0
		}
		activateInputMethod(id)
	}

	val inputMethods: List<InputMethod>
		get() = Collections.unmodifiableList(mInputMethods)

	/**
	 * This should be called when activity is paused (so Input Methods can do some cleanup,
	 * for example properly dismiss dialogs because of WindowLeaked exception).
	 */
	fun pause() {
		for (im in mInputMethods) {
			im.pause()
		}
	}

	/**
	 * Ensures that all input method objects are created.
	 */
	private fun ensureInputMethods() {
		check(mInputMethods.size != 0) { "Input methods are not created yet. Call initialize() first." }
	}

	private fun createInputMethods() {
		if (mInputMethods.size == 0) {
			addInputMethod(INPUT_METHOD_POPUP, imPopup)
			addInputMethod(INPUT_METHOD_INSERT_ON_TAP, imInsertOnTap)
			addInputMethod(INPUT_METHOD_SELECT_ON_TAP, imSelectOnTap)
		}
	}

	private fun addInputMethod(methodIndex: Int, im: InputMethod) {
		im.initialize(mContext, this, mGame, mBoard, mHintsQueue)
		mInputMethods.add(methodIndex, im)
	}

	/**
	 * Ensures that control panel for given input method is created.
	 */
	private fun ensureControlPanel(methodID: Int) {
		val im = mInputMethods[methodID]
		if (!im.isInputMethodViewCreated) {
			im.createInputMethodView()
			im.switchModeButton.setOnClickListener(mSwitchModeListener)
			this.addView(im.inputMethodView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
		}
	}

	companion object {
		const val INPUT_METHOD_POPUP = 0
		const val INPUT_METHOD_INSERT_ON_TAP = 1
		const val INPUT_METHOD_SELECT_ON_TAP = 2
	}
}
