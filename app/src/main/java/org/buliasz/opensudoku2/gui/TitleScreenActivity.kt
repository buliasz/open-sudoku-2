package org.buliasz.opensudoku2.gui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.utils.AndroidUtils

class TitleScreenActivity : ThemedActivity() {
    private val menuItemSettings = 0
    private val menuItemAbout = 1
    private val dialogAbout = 0
    private var mResumeButton: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_title_screen)
        mResumeButton = findViewById(R.id.resume_button)
        val mSudokuListButton = findViewById<Button>(R.id.sudoku_lists_button)
        val mSettingsButton = findViewById<Button>(R.id.settings_button)
        setupResumeButton()
        mSudokuListButton.setOnClickListener { startActivity(Intent(this, FolderListActivity::class.java)) }
        mSettingsButton.setOnClickListener { startActivity(Intent(this, GameSettingsActivity::class.java)) }

        // check the preference to skip the title screen and launch the folder list activity
        // directly
        val gameSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val showSudokuFolderListOnStartup = gameSettings.getBoolean("show_sudoku_lists_on_startup", false)
        if (showSudokuFolderListOnStartup) {
            startActivity(Intent(this, FolderListActivity::class.java))
        } else {
            // show changelog on first run
            val changelog = Changelog(this)
            changelog.showOnFirstRun()
        }
    }

    private fun canResume(mSudokuGameID: Long): Boolean {
        val mDatabase = SudokuDatabase(applicationContext)
        val mSudokuGame = mDatabase.getSudoku(mSudokuGameID)
        return if (mSudokuGame != null) {
            mSudokuGame.state != SudokuGame.GAME_STATE_COMPLETED
        } else false
    }

    private fun setupResumeButton() {
        val gameSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val mSudokuGameID = gameSettings.getLong("most_recently_played_sudoku_id", 0)
        if (canResume(mSudokuGameID)) {
            mResumeButton!!.visibility = View.VISIBLE
            mResumeButton!!.setOnClickListener {
                val intentToPlay = Intent(this@TitleScreenActivity, SudokuPlayActivity::class.java)
                intentToPlay.putExtra(SudokuPlayActivity.EXTRA_SUDOKU_ID, mSudokuGameID)
                startActivity(intentToPlay)
            }
        } else {
            mResumeButton!!.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, menuItemSettings, 0, R.string.settings)
            .setShortcut('0', 's')
            .setIcon(R.drawable.ic_settings)
        menu.add(0, menuItemAbout, 1, R.string.about)
            .setShortcut('1', 'h')
            .setIcon(R.drawable.ic_info)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            menuItemSettings -> {
                startActivity(Intent(this, GameSettingsActivity::class.java))
                return true
            }

            menuItemAbout -> {
                showDialog(dialogAbout)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(id: Int): Dialog {
        val factory = LayoutInflater.from(this)
        if (id == dialogAbout) {
            val aboutView = factory.inflate(R.layout.about, null)
            val versionLabel = aboutView.findViewById<TextView>(R.id.version_label)
            val versionName = AndroidUtils.getAppVersionName(applicationContext)
            versionLabel.text = getString(R.string.version, versionName)
            return AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setView(aboutView)
                .setPositiveButton("OK", null)
                .create()
        }
        throw Exception("Invalid id $id")
    }

    override fun onResume() {
        super.onResume()
        setupResumeButton()
    }
}
