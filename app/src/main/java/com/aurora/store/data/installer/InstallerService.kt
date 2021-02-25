/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.installer

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.aurora.store.R
import com.aurora.store.data.event.SessionEvent
import com.aurora.store.util.Log
import org.greenrobot.eventbus.EventBus

class InstallerService : Service() {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -69)
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        val extra = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> promptUser(intent)
            else -> postStatus(status, packageName, extra)
        }

        stopSelf()
        return START_NOT_STICKY
    }

    private fun promptUser(intent: Intent) {
        val confirmationIntent: Intent? = intent.getParcelableExtra(Intent.EXTRA_INTENT)

        confirmationIntent?.let {
            it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                startActivity(it)
            } catch (e: Exception) {
                Log.e(e.message)
            }
        }
    }

    private fun postStatus(status: Int, packageName: String?, extra: String?) {
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                EventBus.getDefault().post(SessionEvent.Success(packageName, "Success"))
            }
            else -> {
                val errorString = getErrorString(status)
                Log.e("$packageName : $errorString")
                EventBus.getDefault().post(SessionEvent.Failed(packageName, errorString, extra))
            }
        }
    }

    private fun getErrorString(status: Int): String {
        return when (status) {
            PackageInstaller.STATUS_FAILURE_ABORTED -> getString(R.string.installer_status_user_action)
            PackageInstaller.STATUS_FAILURE_BLOCKED -> getString(R.string.installer_status_failure_blocked)
            PackageInstaller.STATUS_FAILURE_CONFLICT -> getString(R.string.installer_status_failure_conflict)
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> getString(R.string.installer_status_failure_incompatible)
            PackageInstaller.STATUS_FAILURE_INVALID -> getString(R.string.installer_status_failure_invalid)
            PackageInstaller.STATUS_FAILURE_STORAGE -> getString(R.string.installer_status_failure_storage)
            else -> getString(R.string.installer_status_failure)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}