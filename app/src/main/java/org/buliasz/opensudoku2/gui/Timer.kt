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

import android.os.Bundle
import android.os.Handler
import android.os.SystemClock

/**
 * This class implements a simple periodic timer.
 * Construct a periodic timer with a given tick interval.
 * @param mTickInterval The tick interval in ms.
 */
internal abstract class Timer(private var mTickInterval: Long) : Handler() {
	/**
	 * Query whether this Timer is running.
	 *
	 * @return true iff we're running.
	 */
	var isRunning = false
		private set

	// Number of times step() has been called.
	private var mTickCount = 0

	// Time at which to execute the next step.  We schedule each
	// step at this plus x ms; this gives us an even execution rate.
	private var mNextTime: Long = 0

	/**
	 * Get the accumulated time of this Timer.
	 *
	 * @return How long this timer has been running, in ms.
	 */
	// The accumulated time in ms for which this timer has been running.
	// Increments between start() and stop(); start(true) resets it.
	var time: Long = 0
		private set

	// The time at which we last added to accumTime.
	private var mLastLogTime: Long = 0

	// ******************************************************************** //
	// Handlers.
	// ******************************************************************** //
	/**
	 * Handle a step of the animation.
	 */
	private val runner: Runnable = object : Runnable {
		override fun run() {
			if (isRunning) {
				val now = SystemClock.uptimeMillis()

				// Add up the time since the last step.
				time += now - mLastLogTime
				mLastLogTime = now
				if (!step(mTickCount++, time)) {
					// Schedule the next.  If we've got behind, schedule
					// it for a tick after now.  (Otherwise we'd end
					// up with a zillion events queued.)
					mNextTime += mTickInterval
					if (mNextTime <= now) mNextTime += mTickInterval
					postAtTime(this, mNextTime)
				} else {
					isRunning = false
					done()
				}
			}
		}
	}
	// ******************************************************************** //
	// Implementation.
	// ******************************************************************** //
	/**
	 * Start the timer.  step() will be called at regular intervals
	 * until it returns true; then done() will be called.
	 *
	 *
	 * Subclasses may override this to do their own setup; but they
	 * must then call super.start().
	 */
	fun start() {
		if (isRunning) return
		isRunning = true
		val now = SystemClock.uptimeMillis()

		// Start accumulating time again.
		mLastLogTime = now

		// Schedule the first event at once.
		mNextTime = now
		postAtTime(runner, mNextTime)
	}
	// ******************************************************************** //
	// State Save/Restore.
	// ******************************************************************** //
	/**
	 * Stop the timer.  step() will not be called again until it is
	 * restarted.
	 *
	 *
	 * Subclasses may override this to do their own setup; but they
	 * must then call super.stop().
	 */
	fun stop() {
		if (isRunning) {
			isRunning = false
			val now = SystemClock.uptimeMillis()
			time += now - mLastLogTime
			mLastLogTime = now
		}
	}

	/**
	 * Subclasses override this to handle a timer tick.
	 *
	 * @param count The call count; 0 on the first call.
	 * @param time  The total time for which this timer has been
	 * running, in ms.  Reset by reset().
	 * @return true if the timer should stop; this will
	 * trigger a call to done().  false otherwise;
	 * we will continue calling step().
	 */
	protected abstract fun step(count: Int, time: Long): Boolean

	/**
	 * Subclasses may override this to handle completion of a run.
	 */
	protected fun done() {}

	/**
	 * Save game state so that the user does not lose anything
	 * if the game process is killed while we are in the
	 * background.
	 *
	 * @param outState A Bundle in which to place any state
	 * information we wish to save.
	 */
	fun saveState(outState: Bundle) {
		// Accumulate all time up to now, so we know where we're saving.
		if (isRunning) {
			val now = SystemClock.uptimeMillis()
			time += now - mLastLogTime
			mLastLogTime = now
		}
		outState.putLong("tickInterval", mTickInterval)
		outState.putBoolean("isRunning", isRunning)
		outState.putInt("tickCount", mTickCount)
		outState.putLong("accumTime", time)
	}

	/**
	 * Restore our game state from the given Bundle.  If the saved
	 * state was running, we will continue running.
	 *
	 * @param map A Bundle containing the saved state.
	 * @return true if the state was restored OK; false
	 * if the saved state was incompatible with the
	 * current configuration.
	 */
	@JvmOverloads
	fun restoreState(map: Bundle, run: Boolean = true): Boolean {
		mTickInterval = map.getLong("tickInterval")
		isRunning = map.getBoolean("isRunning")
		mTickCount = map.getInt("tickCount")
		time = map.getLong("accumTime")
		mLastLogTime = SystemClock.uptimeMillis()

		// If we were running, restart if requested, else stop.
		if (isRunning) {
			if (run) start() else isRunning = false
		}
		return true
	}
}
