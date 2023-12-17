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

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton

/**
 * A button that displays an icon, may be checkable.
 *
 *
 * The normal Material icon button assumes the button will still have text, and sizes the
 * icon to the size of the text.
 *
 *
 * This button assumes there is no text, and sizes the icon to 2/3rds the height of the
 * button.
 *
 *
 * Do not set android:text, use android:contentDescription for accessibility.
 */
class IconButton(context: Context?, attrs: AttributeSet?) : MaterialButton(context!!, attrs) {
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val height = bottom - top
        iconSize = (height * (2.0 / 3.0)).toInt()
        super.onLayout(changed, left, top, right, bottom)
    }
}
