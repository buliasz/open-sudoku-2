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
package org.buliasz.opensudoku2.gui

import java.util.Formatter

/**
 * Game time formatter.
 *
 * @author romario, Kotlin version by buliasz
 */
class GameTimeFormat {
    private val mTimeText = StringBuilder()
    private val mGameTimeFormatter = Formatter(mTimeText)

    /**
     * Formats time to format of mm:ss, hours are
     * never displayed, only total number of minutes.
     *
     * @param time Time in milliseconds.
     * @return
     */
    fun format(time: Long): String {
        mTimeText.setLength(0)
        if (time > TIME_99_99) {
            mGameTimeFormatter.format("%d:%02d", time / 60000, time / 1000 % 60)
        } else {
            mGameTimeFormatter.format("%02d:%02d", time / 60000, time / 1000 % 60)
        }
        return "$mTimeText"
    }

    companion object {
        private const val TIME_99_99 = 99 * 99 * 1000
    }
}
