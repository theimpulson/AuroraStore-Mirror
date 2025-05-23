/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.navigation.compose.rememberNavController
import com.aurora.store.compose.navigation.NavGraph
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.theme.AuroraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // TODO: Change startDestination logic to mirror MainActivity
        val startDestination = IntentCompat.getParcelableExtra(
            intent,
            Screen.PARCEL_KEY,
            Screen::class.java
        ) ?: Screen.Blacklist

        setContent {
            AuroraTheme {
                val navController = rememberNavController()
                NavGraph(navHostController = navController, startDestination = startDestination)
            }
        }
    }
}
