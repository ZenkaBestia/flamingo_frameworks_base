/*
 * Copyright (C) 2017-2018 Paranoid Android
 * Copyright (C) 2022 FlamingoOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy

import android.content.Context
import android.util.Log

import com.android.systemui.R
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.phone.PhoneStatusBarView
import com.android.systemui.statusbar.phone.StatusBar
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener

import javax.inject.Inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@SysUISingleton
class BurnInProtectionController @Inject constructor(
    private val context: Context,
    private val configurationController: ConfigurationController,
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val mutex = Mutex()

    private val shiftEnabled = context.resources.getBoolean(R.bool.config_statusBarBurnInProtection)
    private val shiftInterval = context.resources.getInteger(R.integer.config_shift_interval) * 1000L

    private val configurationListener = object: ConfigurationListener {
        override fun onDensityOrFontScaleChanged() {
            logD("onDensityOrFontScaleChanged")
            coroutineScope.launch {
                mutex.withLock {
                    loadResources()
                }
            }
        }
    }

    private var statusBar: StatusBar? = null
    private var phoneStatusBarView: PhoneStatusBarView? = null

    private var shiftJob: Job? = null

    // Shift amount in pixels
    private var horizontalShift = 0
    private var horizontalMaxShift = 0
    private var verticalShift = 0
    private var verticalMaxShift = 0

    // Increment / Decrement (based on sign) for each tick
    private var horizontalShiftStep = 0
    private var verticalShiftStep = 0

    init {
        logD("Init: shiftEnabled = $shiftEnabled, shiftInterval = $shiftInterval")
        loadResources()
    }

    private fun loadResources()  {
        horizontalShift = 0
        verticalShift = 0
        val res = context.resources
        horizontalMaxShift = res.getDimensionPixelSize(R.dimen.horizontal_max_shift)
        horizontalShiftStep = horizontalMaxShift / TOTAL_SHIFTS_IN_ONE_DIRECTION
        verticalMaxShift = res.getDimensionPixelSize(R.dimen.vertical_max_shift)
        verticalShiftStep = verticalMaxShift / TOTAL_SHIFTS_IN_ONE_DIRECTION
        logD(
            "horizontalMaxShift = $horizontalMaxShift, " +
                "horizontalShiftStep = $horizontalShiftStep, " +
                "verticalMaxShift = $verticalMaxShift, " +
                "verticalShiftStep = $verticalShiftStep"
        )
    }

    fun setStatusBar(statusBar: StatusBar?) {
        this.statusBar = statusBar
    }

    fun setPhoneStatusBarView(phoneStatusBarView: PhoneStatusBarView?) {
        this.phoneStatusBarView = phoneStatusBarView
    }

    fun startShiftTimer() {
        if (!shiftEnabled || (shiftJob?.isActive == true)) return
        configurationController.addCallback(configurationListener)
        shiftJob = coroutineScope.launch {
            while (isActive) {
                withContext(Dispatchers.Default) {
                    recalculateShift()
                }
                shiftViews()
                delay(shiftInterval)
            }
        }
        logD("Started shift job")
    }

    private fun recalculateShift() {
        horizontalShift += horizontalShiftStep
        if (horizontalShift >= horizontalMaxShift ||
                horizontalShift <= -horizontalMaxShift) {
            logD("Switching horizontal direction")
            horizontalShiftStep *= -1
        }

        verticalShift += verticalShiftStep
        if (verticalShift >= verticalMaxShift ||
                verticalShift <= -verticalMaxShift) {
            logD("Switching vertical direction")
            verticalShiftStep *= -1
        }

        logD("Recalculated shifts, horizontalShift = $horizontalShift, " +
            "verticalShift = $verticalShift")
    }

    private fun shiftViews() {
        logD("Shifting views")
        phoneStatusBarView?.shiftStatusBarItems(horizontalShift, verticalShift)
        statusBar?.navigationBarView?.shiftNavigationBarItems(horizontalShift, verticalShift)
    }

    fun stopShiftTimer() {
        if (!shiftEnabled || (shiftJob?.isActive != true)) return
        configurationController.removeCallback(configurationListener)
        logD("Cancelling shift job")
        coroutineScope.launch {
            shiftJob?.cancelAndJoin()
            horizontalShift = 0
            verticalShift = 0
            shiftViews()
            logD("Cancelled shift job")
        }
    }

    companion object {
        private const val TAG = "BurnInProtectionController"
        private const val DEBUG = false
        private const val TOTAL_SHIFTS_IN_ONE_DIRECTION = 3

        private fun logD(msg: String?) {
            if (DEBUG) Log.d(TAG, msg)
        }
    }
}
