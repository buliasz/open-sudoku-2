/*
 * Copyright (C) 2009 Roman Masek, Kotlin version 2023 Bart Uliasz
 *
 * This file is part of OpenSudoku2.
 *
 * OpenSudoku2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenSudoku2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenSudoku2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.buliasz.opensudoku2.game

import java.util.StringTokenizer

/**
 * Note attached to cell. This object is immutable by design.
 *
 * @author romario, Kotlin version by buliasz
 */
class CellNote {
    private val mNotedNumbers: Short

    constructor() {
        mNotedNumbers = 0
    }

    private constructor(notedNumbers: Short) {
        mNotedNumbers = notedNumbers
    }

    /**
     * Appends string representation of this object to the given `StringBuilder`.
     * You can later recreate object from this string by calling [.deserialize].
     */
    fun serialize(target: StringBuilder) {
        target.append(mNotedNumbers.toInt())
        target.append("|")
    }

    fun serialize(): String {
        val sb = StringBuilder()
        serialize(sb)
        return "$sb"
    }

    val notedNumbers: MutableList<Int>
        /**
         * Returns numbers currently noted in cell.
         */
        get() {
            val result: MutableList<Int> = ArrayList()
            var c = 1
            for (i in 0..8) {
                if (mNotedNumbers.toInt() and c.toShort().toInt() != 0) {
                    result.add(i + 1)
                }
                c = c shl 1
            }
            return result
        }

    /**
     * Toggles noted number: if number is already noted, it will be removed otherwise it will be added.
     *
     * @param number Number to toggle.
     * @return New CellNote instance with changes.
     */
    fun toggleNumber(number: Int): CellNote {
        require(!(number < 1 || number > 9)) { "Number must be between 1-9." }
        return CellNote((mNotedNumbers.toInt() xor (1 shl number - 1)).toShort())
    }

    /**
     * Adds number to the cell's note (if not present already).
     */
    fun addNumber(number: Int): CellNote {
        require(!(number < 1 || number > 9)) { "Number must be between 1-9." }
        return CellNote((mNotedNumbers.toInt() or (1 shl number - 1)).toShort())
    }

    /**
     * Removes number from the cell's note.
     */
    fun removeNumber(number: Int): CellNote {
        require(!(number < 1 || number > 9)) { "Number must be between 1-9." }
        return CellNote((mNotedNumbers.toInt() and (1 shl number - 1).inv()).toShort())
    }

    fun hasNumber(number: Int): Boolean {
        return if (number < 1 || number > 9) {
            false
        } else mNotedNumbers.toInt() and (1 shl number - 1) != 0
    }

    fun clear(): CellNote = CellNote()

    val isEmpty: Boolean
        /**
         * Returns true, if note is empty.
         *
         * @return True if note is empty.
         */
        get() = mNotedNumbers.toInt() == 0

    companion object {
        val EMPTY = CellNote()

        /**
         * Creates instance from given string (string which has been
         * created by [.serialize] or [.serialize] method).
         * earlier.
         */
        @JvmOverloads
        fun deserialize(note: String, version: Int = CellCollection.DATA_VERSION): CellNote {
            var noteValue = 0
            if (note != "" && note != "-") {
                if (version == CellCollection.DATA_VERSION_1) {
                    val tokenizer = StringTokenizer(note, ",")
                    while (tokenizer.hasMoreTokens()) {
                        val value = tokenizer.nextToken()
                        if (value != "-") {
                            val number = value.toInt()
                            noteValue = noteValue or (1 shl number - 1)
                        }
                    }
                } else {
                    noteValue = note.toInt()
                }
            }
            return CellNote(noteValue.toShort())
        }

        /**
         * Creates note instance from given `int` array.
         *
         * @param notedNumbersArray Array of integers, which should be part of note.
         * @return New note instance.
         */
        fun fromIntArray(notedNumbersArray: Array<Int>): CellNote {
            var notedNumbers = 0
            for (n in notedNumbersArray) {
                notedNumbers = (notedNumbers or (1 shl n - 1)).toShort().toInt()
            }
            return CellNote(notedNumbers.toShort())
        }
    }
}
