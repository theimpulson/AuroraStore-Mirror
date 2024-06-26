package com.aurora.store.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aurora.Constants
import com.aurora.extensions.isSAndAbove
import com.aurora.store.data.work.CacheWorker
import com.aurora.store.util.CertUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_MIGRATION_VERSION
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import com.aurora.store.util.save
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MigrationReceiver: BroadcastReceiver() {

    private val TAG = MigrationReceiver::class.java.simpleName

    private val prefVersion = 2 // BUMP THIS MANUALLY ON ADDING NEW MIGRATION STEP

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val oldVersion = Preferences.getInteger(context, PREFERENCE_MIGRATION_VERSION)

            // Run required migrations on version upgrade for existing installs
            if (Preferences.getBoolean(context, PREFERENCE_INTRO) && oldVersion != prefVersion) {
                val newVersion = upgradeIfNeeded(context, oldVersion)
                Preferences.putInteger(context, PREFERENCE_MIGRATION_VERSION, newVersion)
            }
        }
    }

    private fun upgradeIfNeeded(context: Context, oldVersion: Int): Int {
        Log.i(TAG, "Upgrading from version $oldVersion to version $prefVersion")
        var currentVersion = oldVersion

        // Add new migrations / defaults below this point
        // Always bump currentVersion at the end of migration for next release

        // 58 -> 59
        if (currentVersion == 0) {
            CacheWorker.scheduleAutomatedCacheCleanup(context) // !1089
            if (isSAndAbove()) context.save(PREFERENCE_THEME_ACCENT, 0)
            context.save(PREFERENCE_DISPENSER_URLS, setOf(Constants.URL_DISPENSER)) // !1117
            if (Preferences.getInteger(context, PREFERENCE_VENDING_VERSION) == -1) {
                context.save(PREFERENCE_VENDING_VERSION, 0) // !1049
            }
            currentVersion++
        }

        // 59 -> 60
        if (currentVersion == 1) {
            if (CertUtil.isAppGalleryApp(context, context.packageName)) {
                val dispensers = Preferences.getStringSet(context, PREFERENCE_DISPENSER_URLS)
                    .toMutableSet()
                    .remove(Constants.URL_DISPENSER)
                context.save(PREFERENCE_DISPENSER_URLS, dispensers)
            }
            currentVersion++
        }

        // Add new migrations / defaults above this point.
        if (currentVersion != prefVersion) {
            Log.e(TAG, "Upgrading to version $prefVersion left it at $currentVersion instead")
        } else {
            Log.i(TAG, "Finished running required migrations!")
        }

        return currentVersion
    }
}
