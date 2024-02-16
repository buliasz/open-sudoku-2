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
package org.buliasz.opensudoku2.gui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Looper
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.annotation.ColorInt
import com.google.android.material.color.MaterialColors
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.CellNote
import org.buliasz.opensudoku2.game.SudokuGame
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Sudoku board widget.
 */
class SudokuBoardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
	private var blinkingValue: Int = 0
	private val blinker = Blinker(this)
	var mSelectedCell: Cell? = null
		private set(value) {
			if (field != value) {
				field = value
				onCellSelectedListener(value)
			}
		}

	private var mCellWidth = 0f
	private var mCellHeight = 0f
	private var mTouchedCell: Cell? = null
	internal var highlightedValue = 0
	private var mReadonly = false
	internal var highlightDirectlyWrongValues = true
		set(value) {
			field = value
			postInvalidate()
		}
	internal var highlightIndirectlyWrongValues = true
		set(value) {
			field = value
			postInvalidate()
		}
	internal var highlightTouchedCell = true
	internal var autoHideTouchedCellHint = true
	internal var highlightSimilarCells = HighlightMode.NONE
	private lateinit var mGame: SudokuGame
	private lateinit var mCells: CellCollection

	/**
	 * Registers callback which will be invoked when user taps the cell.
	 */
	internal var onCellTappedListener: (Cell) -> Unit = {}

	/**
	 * Callback invoked when cell is selected. Cell selection can change without user interaction.
	 */
	internal lateinit var onCellSelectedListener: (Cell?) -> Unit
	private val mLinePaint = Paint()
	private val mSectorLinePaint = Paint()
	private val mText = Paint()
	private val mTextReadOnly = Paint()
	private val mTextSecondary = Paint()
	private val mTextTouched = Paint()
	private val mTextFocused = Paint()
	private val mTextHighlighted = Paint()
	private val mTextNote = Paint()
	private val mTextNoteSecondary = Paint()
	private val mTextNoteTouched = Paint()
	private val mTextNoteFocused = Paint()
	private val mTextNoteHighlighted = Paint()

	/**
	 * Stores the background and foreground paints for each cell so they can be drawn
	 * in one pass. First two indices are the cell's row and column, the last one is
	 * paints for the cell's background, text, or note text, respectively.
	 *
	 *
	 * So [1][3][0] is the background ([0]) paint of the cell in the second ([1]) row
	 * in the fourth ([3]) column.
	 */
	private var mPaints: Array<Array<Array<Paint>>>
	private var mNumberLeft = 0
	private var mNumberTop = 0
	private var mNoteTop = 0f
	private var mSectorLineWidth = 0
	private val mBackground = Paint()
	private val mBackgroundSecondary = Paint()
	private val mBackgroundReadOnly = Paint()
	private val mBackgroundTouched = Paint()
	private val mBackgroundFocused = Paint()
	private val mBackgroundHighlighted = Paint()
	private val mTextInvalid = Paint()
	private val mBackgroundInvalid = Paint()
	private val bounds = Rect()
	private val paint = Paint()

	/** Move the cell focus to the right if a number (not note, digit) is entered  */
	internal var moveCellSelectionOnPress = false

	init {
		isFocusable = true
		isFocusableInTouchMode = true
		mText.isAntiAlias = true
		mTextReadOnly.isAntiAlias = true
		mTextInvalid.isAntiAlias = true
		mTextSecondary.isAntiAlias = true
		mTextTouched.isAntiAlias = true
		mTextFocused.isAntiAlias = true
		mTextHighlighted.isAntiAlias = true
		mTextNote.isAntiAlias = true
		mTextNoteSecondary.isAntiAlias = true
		mTextNoteFocused.isAntiAlias = true
		mTextNoteTouched.isAntiAlias = true
		mTextNoteHighlighted.isAntiAlias = true
		mPaints = Array(9) { Array(9) { arrayOf(mBackground, mText, mTextNote) } }
		setAllColorsFromThemedContext(context)
		contentDescription = context.getString(R.string.sudoku_board_widget)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		val width = width - paddingRight
		val height = height - paddingBottom
		val paddingLeft = paddingLeft
		val paddingTop = paddingTop

		// Ensure the whole canvas starts with the background colour, in case any other colours are transparent.
		canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mBackground)

		// draw cells
		var cellLeft: Int
		var cellTop: Int
		val notesPerRow = 5
		val numberAscent = mText.ascent()
		val noteAscent = mTextNote.ascent()
		val noteWidth = mCellWidth / (notesPerRow + 1)
		if (highlightIndirectlyWrongValues) {
			mCells.solutionCount // make sure solution is filled in cells for highlighting
		}
		for (row in 0..8) {
			for (col in 0..8) {
				// Default colours for ordinary cells
				mPaints[row][col][0] = mBackground
				mPaints[row][col][1] = mText
				mPaints[row][col][2] = mTextNote
				val cell = mCells.getCell(row, col)

				// Even boxes
				if (mBackgroundSecondary.color != Color.TRANSPARENT) {
					val boxNumber = row / 3 + col / 3 + 1 // 1-based
					if (boxNumber % 2 == 0) {
						mPaints[row][col][0] = mBackgroundSecondary
						mPaints[row][col][1] = mTextSecondary
						mPaints[row][col][2] = mTextNoteSecondary
					} else {
						mPaints[row][col][0] = mBackground
						mPaints[row][col][1] = mText
						mPaints[row][col][2] = mTextNote
					}
				}

				// Read-only (given digit) cells
				if (!cell.isEditable) {
					mPaints[row][col][0] = mBackgroundReadOnly
					mPaints[row][col][1] = mTextReadOnly
				}

				// Possibly highlight this cell if it contains the same digit (or, optionally,
				// note) as the selected cell.
				val cellIsNotAlreadySelected = mSelectedCell == null || mSelectedCell !== cell
				val highlightedValueIsValid = highlightedValue != 0
				var shouldHighlightCell = false
				when (highlightSimilarCells) {
					HighlightMode.NONE -> {}
					HighlightMode.NUMBERS -> {
						shouldHighlightCell = cellIsNotAlreadySelected && highlightedValueIsValid && highlightedValue == cell.value
					}

					HighlightMode.NUMBERS_AND_NOTES -> {
						shouldHighlightCell = highlightedValueIsValid && (highlightedValue == cell.value)
					}
				}
				if (shouldHighlightCell) {
					mPaints[row][col][0] = mBackgroundHighlighted
					if (cell.isEditable) mPaints[row][col][1] = mTextHighlighted
					mPaints[row][col][2] = mTextNoteHighlighted
				}

				// Seeing that a cell is invalid is more important than it being highlighted. Only mark editable cells as errors, there's no point
				// marking a given cell with an error (partly because the user can't change it, and partly because then the user can't easily see which
				// of the cells containing the error are editable).
				if (highlightDirectlyWrongValues && cell.value != 0 && cell.isEditable && !cell.isValid) {
					mPaints[row][col][0] = mBackgroundInvalid
					mPaints[row][col][1] = mTextInvalid
					mPaints[row][col][2] = mTextNote // Not read, set to avoid risk of NPEs
				}
				if (highlightIndirectlyWrongValues && cell.value != 0 && !cell.matchesSolution) {
					mPaints[row][col][0] = mBackgroundInvalid
					mPaints[row][col][1] = mTextInvalid
					mPaints[row][col][2] = mTextNote // Not read, set to avoid risk of NPEs
				}

				// Highlight this cell if (a) we're highlighting cells in the same row/column as the touched cell, and (b) this cell is in that row or column.
				val touchedCell = mTouchedCell
				if (highlightTouchedCell && touchedCell != null) {
					val touchedRow = touchedCell.rowIndex
					val touchedCol = touchedCell.columnIndex
					if (row == touchedRow || col == touchedCol) {
						mPaints[row][col][0] = mBackgroundTouched
						mPaints[row][col][1] = mTextTouched
						mPaints[row][col][2] = mTextNoteTouched
					}
				}
				cellLeft = (col * mCellWidth + paddingLeft).roundToInt()
				cellTop = (row * mCellHeight + paddingTop).roundToInt()

				// Draw the cell background
				canvas.drawRect(
					cellLeft.toFloat(), cellTop.toFloat(), cellLeft + mCellWidth, cellTop + mCellHeight, mPaints[row][col][0]
				)

				// Draw cell contents
				val value = cell.value
				if (value != 0) {
					var valuePaint = mPaints[row][col][1]
					if (value == blinkingValue) {
						valuePaint = mTextTouched
					}
					canvas.drawText(
						"$value", (cellLeft + mNumberLeft).toFloat(), cellTop + mNumberTop - numberAscent, valuePaint
					)
				} else {
					// To draw notes the cell is divided to 3 rows (0-2) and notesPerRow columns.

					// "corner" notes are drawn in rows 0 and 2, up to 5 in row 0, up to 4 in row 3, left-aligned.
					if (!cell.cornerNote.isEmpty) {
						for ((i, number) in cell.cornerNote.notedNumbers.withIndex()) {
							val noteCol = i % notesPerRow
							// First notesPerRow numbers draw on row 0, remaining draw on row 2.
							val noteRow = if (i < notesPerRow) 0 else 2
							if (number == highlightedValue) {
								canvas.drawCircle(
									cellLeft + (noteCol + 0.5f) * noteWidth + 2,
									cellTop + mNoteTop + (noteRow + 0.5f) * mPaints[row][col][2].textSize,
									mPaints[row][col][2].textSize * 0.6f,
									mBackgroundHighlighted
								)
							}
							canvas.drawText(
								"$number",
								cellLeft + noteCol * noteWidth + 2,
								cellTop + mNoteTop - noteAscent + noteRow * mPaints[row][col][2].textSize - 1,
								mPaints[row][col][2]
							)
						}
					}

					// "center" notes are drawn in row 1, centre-aligned.
					if (!cell.centerNote.isEmpty) {
						paint.set(mPaints[row][col][2])
						paint.textAlign = Paint.Align.CENTER
						val note = cell.centerNote.notedNumbers.joinToString("")

						// Determine the font size (specifically, width) to use. The string
						// may run out of the cell (typically if it's more than 5 digits long).
						// If it will exceed the cell width, shrink the font.
						paint.getTextBounds(note, 0, note.length, bounds)

						// Horizontally align the centre note
						val offsetX = mCellWidth / 2f

						// If the centre note's width exceeds some percentage of the cell's
						// width then scale down the size of the centre note, and recalculate
						// the y offset.
						if (bounds.width() / mCellWidth > 0.97f) {
							paint.textSize = paint.textSize * mCellWidth * 0.97f / bounds.width()
							paint.getTextBounds(note, 0, note.length, bounds)
						}

						// Vertically align the centre note
						val offsetY = mCellHeight / 2f + bounds.height() / 2f
						cell.centerNote.notedNumbers.withIndex()
							.forEach {
								if (it.value == highlightedValue) {
									canvas.drawCircle(
										cellLeft + offsetX - bounds.width() / 2f + bounds.width() / cell.centerNote.notedNumbers.size * (it.index + 0.5f),
										cellTop + offsetY - bounds.height() / 2f,
										mPaints[row][col][2].textSize * 0.6f,
										mBackgroundHighlighted
									)
								}
							}
						canvas.drawText(note, cellLeft + offsetX, cellTop + offsetY, paint)
					}
				}
			}
		}

		// draw vertical lines
		for (c in 0..9) {
			val x = c * mCellWidth + paddingLeft
			canvas.drawLine(x, paddingTop.toFloat(), x, height.toFloat(), mLinePaint)
		}

		// draw horizontal lines
		for (r in 0..9) {
			val y = r * mCellHeight + paddingTop
			canvas.drawLine(paddingLeft.toFloat(), y, width.toFloat(), y, mLinePaint)
		}
		val sectorLineWidth1 = mSectorLineWidth / 2
		val sectorLineWidth2 = sectorLineWidth1 + mSectorLineWidth % 2

		// draw sector (thick) lines
		var c = 0
		while (c <= 9) {
			val x = c * mCellWidth + paddingLeft
			canvas.drawRect(x - sectorLineWidth1, paddingTop.toFloat(), x + sectorLineWidth2, height.toFloat(), mSectorLinePaint)
			c += 3
		}
		var r = 0
		while (r <= 9) {
			val y = r * mCellHeight + paddingTop
			canvas.drawRect(paddingLeft.toFloat(), y - sectorLineWidth1, width.toFloat(), y + sectorLineWidth2, mSectorLinePaint)
			r += 3
		}

		// highlight selected cell
		val selectedCell = mSelectedCell
		if (!mReadonly && selectedCell != null) {
			cellLeft = (selectedCell.columnIndex * mCellWidth).roundToInt() + paddingLeft
			cellTop = (selectedCell.rowIndex * mCellHeight).roundToInt() + paddingTop

			// The stroke is drawn half inside and half outside the given cell. Compensate by adjusting the cell's bounds by half the
			// stroke width to move it entirely inside the cell.
			val halfStrokeWidth = mBackgroundFocused.strokeWidth / 2
			mBackgroundFocused.alpha = 128
			canvas.drawRect(
				cellLeft + halfStrokeWidth,
				cellTop + halfStrokeWidth,
				cellLeft + mCellWidth - halfStrokeWidth,
				cellTop + mCellHeight - halfStrokeWidth,
				mBackgroundFocused
			)
		}
	}

	fun setAllColorsFromThemedContext(context: Context) {
		// Grid lines
		setLineColor(MaterialColors.getColor(context, R.attr.colorLine, Color.BLACK))
		setSectorLineColor(MaterialColors.getColor(context, R.attr.colorSectorLine, Color.BLACK))

		// Normal cell
		setTextColor(MaterialColors.getColor(context, R.attr.colorText, Color.BLACK))
		setTextColorNote(MaterialColors.getColor(context, R.attr.colorNoteText, Color.BLACK))
		mBackground.color = MaterialColors.getColor(context, R.attr.colorBackground, Color.WHITE)

		// Default view behaviour is to highlight a view that has the focus. This highlights the entire board and leads to incorrect colours.
		defaultFocusHighlightEnabled = false

		// Read-only cell
		colorReadOnlyText = MaterialColors.getColor(context, R.attr.colorReadOnlyText, Color.WHITE)
		colorReadOnlyBackground = MaterialColors.getColor(context, R.attr.colorReadOnlyBackground, Color.RED)

		// Even 3x3 boxes
		setTextColorEven(MaterialColors.getColor(context, R.attr.colorEvenText, Color.BLACK))
		setTextColorNoteEven(MaterialColors.getColor(context, R.attr.colorEvenText, Color.BLACK))
		setBackgroundColorEven(MaterialColors.getColor(context, R.attr.colorEvenBackground, Color.TRANSPARENT))

		// Touched
		setTextColorTouched(MaterialColors.getColor(context, R.attr.colorTouchedText, Color.BLACK))
		setTextColorNoteTouched(MaterialColors.getColor(context, R.attr.colorTouchedNoteText, Color.BLACK))
		setBackgroundColorTouched(MaterialColors.getColor(context, R.attr.colorTouchedBackground, Color.WHITE))

		// Selected / focused
		setBackgroundColorSelected(MaterialColors.getColor(context, R.attr.colorSelectedBackground, Color.WHITE))

		// Highlighted cell
		colorHighlightedText = MaterialColors.getColor(context, R.attr.colorHighlightedText, Color.WHITE)
		setTextColorNoteHighlighted(MaterialColors.getColor(context, R.attr.colorHighlightedNoteText, Color.WHITE))
		colorHighlightedBackground = MaterialColors.getColor(context, R.attr.colorHighlightedBackground, Color.BLACK)

		// Invalid values
		setTextColorError(MaterialColors.getColor(context, R.attr.colorInvalidText, Color.WHITE))
		setBackgroundColorError(MaterialColors.getColor(context, R.attr.colorInvalidBackground, Color.BLACK))
	}

	fun setLineColor(@ColorInt color: Int) {
		mLinePaint.color = color
	}

	fun setSectorLineColor(@ColorInt color: Int) {
		mSectorLinePaint.color = color
	}

	fun setTextColor(@ColorInt color: Int) {
		mText.color = color
	}

	fun setTextColorNote(@ColorInt color: Int) {
		mTextNote.color = color
	}

	override fun setBackgroundColor(@ColorInt color: Int) {
		mBackground.color = color
	}

	@get:ColorInt
	var colorReadOnlyText: Int
		get() = mTextReadOnly.color
		set(color) {
			mTextReadOnly.color = color
		}

	fun setBackgroundColorEven(@ColorInt color: Int) {
		mBackgroundSecondary.color = color
	}

	@get:ColorInt
	var colorReadOnlyBackground: Int
		get() = mBackgroundReadOnly.color
		set(color) {
			mBackgroundReadOnly.color = color
		}

	fun setBackgroundColorTouched(@ColorInt color: Int) {
		mBackgroundTouched.color = color
	}

	fun setBackgroundColorSelected(@ColorInt color: Int) {
		mBackgroundFocused.color = color
	}

	@get:ColorInt
	var colorHighlightedBackground: Int
		get() = mBackgroundHighlighted.color
		set(color) {
			mBackgroundHighlighted.color = color
		}

	@get:ColorInt
	var colorHighlightedText: Int
		get() = mTextHighlighted.color
		set(color) {
			mTextHighlighted.color = color
		}

	fun setTextColorNoteHighlighted(@ColorInt color: Int) {
		mTextNoteHighlighted.color = color
	}

	fun setTextColorEven(@ColorInt color: Int) {
		mTextSecondary.color = color
	}

	fun setTextColorNoteEven(@ColorInt color: Int) {
		mTextNoteSecondary.color = color
	}

	fun setTextColorTouched(@ColorInt color: Int) {
		mTextTouched.color = color
	}

	fun setTextColorNoteTouched(@ColorInt color: Int) {
		mTextNoteTouched.color = color
	}

	fun setTextColorError(@ColorInt color: Int) {
		mTextInvalid.color = color
	}

	fun setBackgroundColorError(@ColorInt color: Int) {
		mBackgroundInvalid.color = color
	}

	fun setGame(game: SudokuGame) {
		mGame = game
		cells = game.cells
	}

	var cells: CellCollection
		get() = mCells
		set(cells) {
			mCells = cells
			mCells.ensureOnChangeListener(this::postInvalidate)
			postInvalidate()
		}
	var isReadOnly: Boolean
		get() = mReadonly
		set(readonly) {
			mReadonly = readonly
			postInvalidate()
		}

	fun hideTouchedCellHint() {
		mTouchedCell = null
		postInvalidate()
	}

	fun invokeOnCellSelected() {
		onCellSelectedListener(mSelectedCell)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val widthSize = MeasureSpec.getSize(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)
		val heightSize = MeasureSpec.getSize(heightMeasureSpec)
		var width: Int
		var height: Int
		if (widthMode == MeasureSpec.EXACTLY) {
			width = widthSize
		} else {
			width = DEFAULT_BOARD_SIZE
			if (widthMode == MeasureSpec.AT_MOST && width > widthSize) {
				width = widthSize
			}
		}
		if (heightMode == MeasureSpec.EXACTLY) {
			height = heightSize
		} else {
			height = DEFAULT_BOARD_SIZE
			if (heightMode == MeasureSpec.AT_MOST && height > heightSize) {
				height = heightSize
			}
		}
		if (widthMode != MeasureSpec.EXACTLY) {
			width = height
		}
		if (heightMode != MeasureSpec.EXACTLY) {
			height = width
		}
		if (widthMode == MeasureSpec.AT_MOST && width > widthSize) {
			width = widthSize
		}
		if (heightMode == MeasureSpec.AT_MOST && height > heightSize) {
			height = heightSize
		}

		// Ensure the board is square
		height = min(height, width)
		width = height
		mCellWidth = (width - paddingLeft - paddingRight) / 9.0f
		mCellHeight = (height - paddingTop - paddingBottom) / 9.0f
		setMeasuredDimension(width, height)
		val cellTextSize = mCellHeight * 0.75f
		mText.textSize = cellTextSize
		mTextReadOnly.textSize = cellTextSize
		mTextInvalid.textSize = cellTextSize
		mTextSecondary.textSize = cellTextSize
		mTextTouched.textSize = cellTextSize
		mTextFocused.textSize = cellTextSize
		mTextHighlighted.textSize = cellTextSize

		// compute offsets in each cell to center the rendered number
		mNumberLeft = ((mCellWidth - mText.measureText("9")) / 2).toInt()
		mNumberTop = ((mCellHeight - mText.textSize) / 2).toInt()

		// add some offset because in some resolutions notes are cut-off in the top
		mNoteTop = mCellHeight / 50.0f
		val noteTextSize = (mCellHeight - mNoteTop * 2) / 3.0f
		mTextNote.textSize = noteTextSize
		mTextNoteSecondary.textSize = noteTextSize
		mTextNoteFocused.textSize = noteTextSize
		mTextNoteTouched.textSize = noteTextSize
		mTextNoteHighlighted.textSize = noteTextSize
		mSectorLineWidth = computeSectorLineWidth(width, height)
		mBackgroundFocused.style = Paint.Style.STROKE
		mBackgroundFocused.strokeWidth = mSectorLineWidth.toFloat()
	}

	private fun computeSectorLineWidth(widthInPx: Int, heightInPx: Int): Int {
		val sizeInPx = min(widthInPx, heightInPx)
		val dipScale = context.resources.displayMetrics.density
		val sizeInDip = sizeInPx / dipScale
		var sectorLineWidthInDip = 2.0f
		if (sizeInDip > 150) {
			sectorLineWidthInDip = 3.0f
		}
		return (sectorLineWidthInDip * dipScale).toInt()
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (mReadonly) return false

		val x = event.x.toInt()
		val y = event.y.toInt()
		when (event.action) {
			MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> mTouchedCell = getCellAtPoint(x, y)
			MotionEvent.ACTION_UP -> {
				mSelectedCell = getCellAtPoint(x, y)
				performClick()
			}

			MotionEvent.ACTION_CANCEL -> mTouchedCell = null
		}
		postInvalidate()
		return true
	}

	override fun performClick(): Boolean {
		invalidate() // selected cell has changed, update board as soon as you can
		mSelectedCell?.let(onCellTappedListener)
		if (autoHideTouchedCellHint) {
			mTouchedCell = null
		}
		return super.performClick()
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		if (mReadonly) return false

		val selectedCell = mSelectedCell

		when (keyCode) {
			KeyEvent.KEYCODE_DPAD_UP -> return moveCellSelection(0, -1)
			KeyEvent.KEYCODE_DPAD_RIGHT -> return moveCellSelection(1, 0)
			KeyEvent.KEYCODE_DPAD_DOWN -> return moveCellSelection(0, 1)
			KeyEvent.KEYCODE_DPAD_LEFT -> return moveCellSelection(-1, 0)
			KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DEL -> {
				// clear value in selected cell
				if (selectedCell != null) {
					if (event.isShiftPressed) {
						mGame.setCellCornerNote(selectedCell, CellNote.EMPTY, true)
					} else if (event.isAltPressed) {
						mGame.setCellCenterNote(selectedCell, CellNote.EMPTY, true)
					} else {
						mGame.setCellValue(selectedCell, 0, true)
						moveCellSelectionRight()
					}
				}
				return true
			}

			KeyEvent.KEYCODE_DPAD_CENTER -> {
				selectedCell?.let(onCellTappedListener)
				return true
			}
		}

		if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9 && selectedCell != null) {
			val selNumber = keyCode - KeyEvent.KEYCODE_0
			if (event.isShiftPressed) {    // add or remove number in cell's corner note
				mGame.setCellCornerNote(selectedCell, selectedCell.cornerNote.toggleNumber(selNumber), true)
			} else if (event.isAltPressed) {    // add or remove number in cell's center note
				mGame.setCellCenterNote(selectedCell, selectedCell.cornerNote.toggleNumber(selNumber), true)
			} else {  // enter number in cell
				mGame.setCellValue(selectedCell, selNumber, true)
				if (moveCellSelectionOnPress) {
					moveCellSelectionRight()
				}
			}
			return true
		}

		return false
	}

	/**
	 * Moves selected cell by one cell to the right. If edge is reached, selection
	 * skips on beginning of another line.
	 */
	fun moveCellSelectionRight() {
		val selectedCell = mSelectedCell ?: return

		if (!moveCellSelection(1, 0)) {
			var selRow = selectedCell.rowIndex
			selRow++
			if (!moveCellSelectionTo(selRow, 0)) {
				moveCellSelectionTo(0, 0)
			}
		}
		postInvalidate()
	}

	/**
	 * Moves selected by vx cells right and vy cells down. vx and vy can be negative. Returns true,
	 * if new cell is selected.
	 *
	 * @param vx Horizontal offset, by which move selected cell.
	 * @param vy Vertical offset, by which move selected cell.
	 */
	private fun moveCellSelection(vx: Int, vy: Int): Boolean {
		val selectedCell = mSelectedCell ?: return false
		val newRow = selectedCell.rowIndex + vy
		val newCol = selectedCell.columnIndex + vx
		return moveCellSelectionTo(newRow, newCol)
	}

	/**
	 * Moves selection to the cell given by row and column index.
	 *
	 * @param row Row index of cell which should be selected.
	 * @param col Column index of cell which should be selected.
	 * @return True, if cell was successfully selected.
	 */
	fun moveCellSelectionTo(row: Int, col: Int): Boolean {
		if (col >= 0 && col < CellCollection.SUDOKU_SIZE && row >= 0 && row < CellCollection.SUDOKU_SIZE) {
			mSelectedCell = mCells.getCell(row, col)
			postInvalidate()
			return true
		}
		return false
	}

	fun clearCellSelection() {
		mSelectedCell = null
		postInvalidate()
	}

	/**
	 * Returns cell at given screen coordinates. Returns null if no cell is found.
	 */
	private fun getCellAtPoint(x: Int, y: Int): Cell? {
		// take into account padding
		val lx = x - paddingLeft
		val ly = y - paddingTop
		val row = (ly / mCellHeight).toInt()
		val col = (lx / mCellWidth).toInt()
		return if (col >= 0 && col < CellCollection.SUDOKU_SIZE && row >= 0 && row < CellCollection.SUDOKU_SIZE) {
			mCells.getCell(row, col)
		} else {
			null
		}
	}

	fun blinkValue(digit: Int) {
		val anim: Animation = AlphaAnimation(0.0f, 1.0f)
		anim.duration = 50 //You can manage the blinking time with this parameter
		anim.startOffset = 20
		anim.repeatMode = Animation.REVERSE
		anim.repeatCount = 4
		blinker.blink(digit)
	}

	enum class HighlightMode {
		NONE,
		NUMBERS,
		NUMBERS_AND_NOTES
	}

	companion object {
		val TAG: String = SudokuBoardView::class.java.simpleName
		const val DEFAULT_BOARD_SIZE = 100

		private class Blinker(private val sudokuBoard: SudokuBoardView) : Timer(250, Looper.getMainLooper()) {
			private var blinkCount = 0
			private var blinkValue = 0

			fun blink(newValue: Int) {
				blinkValue = newValue
				blinkCount = 0
				start()
			}

			override fun step(count: Int, time: Long): Boolean {
				if (sudokuBoard.blinkingValue == 0) {
					sudokuBoard.blinkingValue = blinkValue
					blinkCount += 1
				} else {
					sudokuBoard.blinkingValue = 0
					if (blinkCount >= 2) {
						sudokuBoard.postInvalidate()
						return true
					}
				}
				sudokuBoard.postInvalidate()
				return false
			}
		}
	}
}
