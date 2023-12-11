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
