package org.buliasz.opensudoku2.gui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import org.buliasz.opensudoku2.R

/**
 * A button that displays a number the user can enter in to the grid.
 *
 *
 * The display of the number on the button varies depending on the current edit mode.
 *
 *
 * Exposes a state, app:all_numbers_placed, that is true if all 9 copies of this number
 * have been entered in to the grid. This can be used in a ColorStateList to adjust
 * the button's background/foreground colors if all 9 copies of a digit are entered.
 */
class NumberButton(context: Context?, attrs: AttributeSet?) : MaterialButton(context!!, attrs) {
    /** Paint when entering main numbers  */
    private val mEnterNumberPaint: Paint = Paint()

    /** Paint when entering center notes  */
    private val mCenterNotePaint: Paint = Paint()

    /** Paint when entering corner notes  */
    private val mCornerNotePaint: Paint = Paint()

    /** Paint for "numbers placed" count  */
    private val mNumbersPlacedPaint: Paint = Paint()

    /** Mode used to display numbers  */
    private var mMode: Int = MODE_EDIT_VALUE

    /** True if the count of times the number is placed should be shown on the button  */
    private var mShowNumbersPlaced = false

    /** Count of the number of times this number is placed in the puzzle  */
    private var mNumbersPlaced = 0

    /** True if the all_numbers_placed attribute is enabled  */
    private var mEnableAllNumbersPlaced = false

    /** Bounds of the text to display  */
    private val mTextBounds = Rect()

    init {
        mEnterNumberPaint.isAntiAlias = true
        mCenterNotePaint.isAntiAlias = true
        mCornerNotePaint.isAntiAlias = true
        mNumbersPlacedPaint.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(w, h, oldWidth, oldHeight)

        // Adjust key text sizes
        val third = h / 3f
        mEnterNumberPaint.textSize = third * 2
        mCornerNotePaint.textSize = third * 1.5f
        mCenterNotePaint.textSize = third * 1.5f
        mNumbersPlacedPaint.textSize = third / 1.5f
    }

    override fun onDraw(canvas: Canvas) {
        val left = paddingLeft
        val top = paddingTop
        val right = width - paddingRight
        val bottom = height - paddingBottom
        val midX = ((right - left) / 2.0 + left).toFloat()
        val midY = ((bottom + top) / 2.0).toFloat()
        var textHeight: Float
        var textWidth: Float
        var number = "$tag"
        when (mMode) {
            MODE_EDIT_VALUE -> {
                // Large numbers, vertically/horizontally centered, with optional small number at
                // the right showing the placed count.
                mEnterNumberPaint.color = currentTextColor
                mEnterNumberPaint.getTextBounds(number, 0, 1, mTextBounds)
                textHeight = mTextBounds.height().toFloat()
                textWidth = mEnterNumberPaint.measureText(number, 0, 1)
                canvas.drawText(number, 0, 1, midX - textWidth / 2, midY + textHeight / 2, mEnterNumberPaint)
                if (mShowNumbersPlaced) {
                    // Initial offset is immediately to the right of the large number
                    val initialXOffset = midX + textWidth / 2

                    // It's possible to enter more than 9 copies of a number in to the grid. Rather
                    // than try and scale a 2 digit string, set it to "X", to both indicate an
                    // error, and because "X" is the Roman numeral for 10.
                    number = if (mNumbersPlaced <= 9) {
                        "$mNumbersPlaced"
                    } else {
                        "X"
                    }
                    mNumbersPlacedPaint.color = currentTextColor
                    if (isEnabled) {
                        mNumbersPlacedPaint.alpha = (255 * 0.68).toInt()
                    }
                    mNumbersPlacedPaint.getTextBounds(number, 0, 1, mTextBounds)
                    textHeight = mTextBounds.height().toFloat()
                    textWidth = mNumbersPlacedPaint.measureText(number, 0, 1)

                    // Draw the smaller number 1/4 of its width to the right of the large number
                    canvas.drawText(number, initialXOffset + textWidth / 4, midY + textHeight / 2, mNumbersPlacedPaint)
                }
            }

            MODE_EDIT_CORNER_NOTE -> {
                mCornerNotePaint.color = currentTextColor
                // Small numbers, vertically/horizontally centered, then offset based on col/row
                mCornerNotePaint.getTextBounds(number, 0, 1, mTextBounds)
                textHeight = mTextBounds.height().toFloat()
                textWidth = mCornerNotePaint.measureText(number, 0, 1)

                // Move each number's location along the X/Y axis based on the col/row it is in.
                val tag = tag as Int
                val col = (tag - 1) % 3
                val row = (tag - 1) / 3

                // How far to move each number from the middle of the cell (as a percentage).
                val offsetPct = 0.25f

                // Compute the offset. Results in offsets of 1 - offsetPct for the first column
                // and row, an offset of 1 for the center cell, and offsets of 1 + offsetPct for
                // the last column and row.
                val offsetX = (col - 1) * offsetPct + 1
                val offsetY = (row - 1) * offsetPct + 1
                canvas.drawText(
                    number, 0, 1, midX * offsetX - textWidth / 2, midY * offsetY + textHeight / 2, mCornerNotePaint
                )
            }

            MODE_EDIT_CENTER_NOTE -> {
                mCenterNotePaint.color = currentTextColor
                // Small numbers, vertically/horizontally centered.
                mCenterNotePaint.getTextBounds(number, 0, 1, mTextBounds)
                textHeight = mTextBounds.height().toFloat()
                textWidth = mCenterNotePaint.measureText(number, 0, 1)
                canvas.drawText(number, 0, 1, midX - textWidth / 2, midY + textHeight / 2, mCenterNotePaint)
            }
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        if (mNumbersPlaced == 9 && mEnableAllNumbersPlaced) {
            mergeDrawableStates(state, ALL_NUMBERS_PLACED_STATE)
        }
        return state
    }

    /** Sets the mode used for displaying numbers  */
    fun setMode(mode: Int) {
        if (mMode == mode) return
        mMode = mode
        invalidate()
    }

    /** Sets whether the count of numbers placed should be shown  */
    fun setShowNumbersPlaced(showNumbersPlaced: Boolean) {
        if (mShowNumbersPlaced != showNumbersPlaced) {
            mShowNumbersPlaced = showNumbersPlaced
            invalidate()
        }
    }

    /** Sets the value to use for the count of placed numbers  */
    fun setNumbersPlaced(numbersPlaced: Int) {
        if (mNumbersPlaced != numbersPlaced) {
            mNumbersPlaced = numbersPlaced
            if (mEnableAllNumbersPlaced) {
                refreshDrawableState()
            }
            invalidate()
        }
    }

    fun setEnableAllNumbersPlaced(enableAllNumbersPlaced: Boolean) {
        mEnableAllNumbersPlaced = enableAllNumbersPlaced
    }

    override fun setTag(tag: Any) {
        super.setTag(tag)
        invalidate()
    }

    companion object {
        // TODO: Repeats definitions in
        const val MODE_EDIT_VALUE = 0
        const val MODE_EDIT_CORNER_NOTE = 1
        const val MODE_EDIT_CENTER_NOTE = 2

        /** Attribute that corresponds to setting app:all_numbers_placed  */
        private val ALL_NUMBERS_PLACED_STATE = intArrayOf(R.attr.all_numbers_placed)
    }
}
