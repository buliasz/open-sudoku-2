package org.buliasz.opensudoku2.gui.inputmethod

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * This class is responsible for persisting of control panel's state.
 *
 * @author romario, Kotlin version by buliasz
 */
class IMControlPanelStatePersister(context: Context?) {
    private val mPreferences: SharedPreferences

    init {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context!!)
    }

    fun saveState(controlPanel: IMControlPanel) {
        // save state of control panel itself
        val cpState = StateBundle(mPreferences, PREFIX + "", true)
        cpState.putInt("activeMethodIndex", controlPanel.activeMethodIndex)
        cpState.commit()

        // save state of all input methods
        for (im in controlPanel.inputMethods) {
            val outState = StateBundle(mPreferences, PREFIX + "" + im.inputMethodName, true)
            im.onSaveState(outState)
            outState.commit()
        }
    }

    fun restoreState(controlPanel: IMControlPanel) {
        // restore state of control panel itself
        val cpState = StateBundle(mPreferences, PREFIX + "", false)
        val methodId = cpState.getInt("activeMethodIndex", 0)
        if (methodId != -1) {
            controlPanel.activateInputMethod(methodId)
        }

        // restore state of all input methods
        for (im in controlPanel.inputMethods) {
            val savedState = StateBundle(mPreferences, PREFIX + "" + im.inputMethodName, false)
            im.onRestoreState(savedState)
        }
    }

    /**
     * This is basically wrapper around anything which is capable of storing
     * state. Instance of this object will be passed to concrete input method's
     * to store and retrieve their state.
     *
     * @author romario, Kotlin version by buliasz
     */
    class StateBundle(private val mPreferences: SharedPreferences, private val mPrefix: String, private val mEditable: Boolean) {
        private var mPrefEditor: SharedPreferences.Editor? = null

        init {
            mPrefEditor = if (mEditable) {
                mPreferences.edit()
            } else {
                null
            }
        }

        fun getBoolean(key: String, defValue: Boolean): Boolean = mPreferences.getBoolean(mPrefix + key, defValue)

        fun getInt(key: String, defValue: Int): Int = mPreferences.getInt(mPrefix + key, defValue)

        fun getString(key: String, defValue: String?): String? = mPreferences.getString(mPrefix + key, defValue)

        fun putInt(key: String, value: Int) {
            check(mEditable) { "StateBundle is not editable" }
            mPrefEditor!!.putInt(mPrefix + key, value)
        }

        fun commit() {
            check(mEditable) { "StateBundle is not editable" }
            mPrefEditor!!.commit()
        }
    }

    companion object {
        private val PREFIX = IMControlPanel::class.java.name
    }
}
