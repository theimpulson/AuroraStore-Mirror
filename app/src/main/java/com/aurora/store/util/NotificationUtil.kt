package com.aurora.store.util

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.WorkManager
import com.aurora.Constants
import com.aurora.extensions.getStyledAttributeColor
import com.aurora.gplayapi.data.models.App
import com.aurora.store.MainActivity
import com.aurora.store.data.room.download.Download as AuroraDownload
import com.aurora.store.R
import com.aurora.store.data.activity.InstallActivity
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import java.util.UUID

object NotificationUtil {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
            val channels = ArrayList<NotificationChannel>()
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ALERT,
                    context.getString(R.string.notification_channel_alert),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                }
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_GENERAL,
                    context.getString(R.string.notification_channel_general),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_UPDATER_SERVICE,
                    context.getString(R.string.notification_channel_updater_service),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_UPDATES,
                    context.getString(R.string.notification_channel_updates),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setSound(null, null)
                }
            )
            notificationManager.createNotificationChannels(channels)
        }
    }

    fun getDownloadNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setColor(ContextCompat.getColor(context, R.color.colorAccent))
            .setContentTitle(context.getString(R.string.app_updater_service_notif_title))
            .setContentText(context.getString(R.string.app_updater_service_notif_text))
            .setOngoing(true)
            .build()
    }

    fun getDownloadNotification(
        context: Context,
        download: AuroraDownload,
        workID: UUID,
        largeIcon: Bitmap? = null
    ): Notification {
        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_GENERAL)
        builder.setContentTitle(download.displayName)
        builder.color = ContextCompat.getColor(context, R.color.colorAccent)
        builder.setContentIntent(getContentIntentForDownloads(context))
        builder.setLargeIcon(largeIcon)

        when (download.downloadStatus) {
            DownloadStatus.CANCELLED -> {
                builder.setSmallIcon(R.drawable.ic_download_cancel)
                builder.setContentText(context.getString(R.string.download_canceled))
                builder.color = Color.RED
                builder.setCategory(Notification.CATEGORY_ERROR)
            }

            DownloadStatus.FAILED -> {
                builder.setSmallIcon(R.drawable.ic_download_fail)
                builder.setContentText(context.getString(R.string.download_failed))
                builder.color = Color.RED
                builder.setCategory(Notification.CATEGORY_ERROR)
            }

            DownloadStatus.COMPLETED -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setContentText(context.getString(R.string.download_completed))
                builder.setAutoCancel(true)
                builder.setCategory(Notification.CATEGORY_STATUS)
                builder.setContentIntent(getContentIntentForDetails(context, download.packageName))
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_install,
                        context.getString(R.string.action_install),
                        getInstallIntent(context, download)
                    ).build()
                )
            }

            DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                builder.setContentText(
                    if (download.progress == 0) {
                        context.getString(R.string.download_queued)
                    } else {
                        context.getString(
                            R.string.download_progress,
                            download.downloadedFiles,
                            download.totalFiles,
                            CommonUtil.humanReadableByteSpeed(download.speed, true)
                        )
                    }
                )
                builder.setOngoing(true)
                builder.setCategory(Notification.CATEGORY_PROGRESS)
                builder.setProgress(100, download.progress, download.progress == 0)
                builder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_download_cancel,
                        context.getString(R.string.action_cancel),
                        WorkManager.getInstance(context).createCancelPendingIntent(workID)
                    ).build()
                )
            }

            else -> {}
        }
        return builder.build()
    }

    fun getInstallNotification(
        context: Context,
        displayName: String,
        packageName: String
    ): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_install)
            .setLargeIcon(PackageUtil.getIconForPackage(context, packageName))
            .setColor(context.getStyledAttributeColor(R.color.colorAccent))
            .setContentTitle(displayName)
            .setContentText(context.getString(R.string.installer_status_success))
            .setContentIntent(getContentIntentForDetails(context, packageName))
            .build()
    }

    fun getInstallerStatusNotification(
        context: Context,
        packageName: String,
        displayName: String,
        content: String?
    ): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_install)
            .setColor(context.getStyledAttributeColor(R.color.colorAccent))
            .setContentTitle(displayName)
            .setContentText(content)
            .setContentIntent(getContentIntentForDetails(context, packageName))
            .build()
    }

    fun getUpdateNotification(context: Context, updatesList: List<App>): Notification {
        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.updatesFragment)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_updates)
            .setContentTitle(
                if (updatesList.size == 1)
                    context.getString(
                        R.string.notification_updates_available_1,
                        updatesList.size
                    )
                else
                    context.getString(
                        R.string.notification_updates_available,
                        updatesList.size
                    )
            )
            .setContentText(
                when (updatesList.size) {
                    1 -> {
                        context.getString(
                            R.string.notification_updates_available_desc_1,
                            updatesList[0].displayName
                        )
                    }

                    2 -> {
                        context.getString(
                            R.string.notification_updates_available_desc_2,
                            updatesList[0].displayName,
                            updatesList[1].displayName
                        )
                    }

                    3 -> {
                        context.getString(
                            R.string.notification_updates_available_desc_3,
                            updatesList[0].displayName,
                            updatesList[1].displayName,
                            updatesList[2].displayName
                        )
                    }

                    else -> {
                        context.getString(
                            R.string.notification_updates_available_desc_4,
                            updatesList[0].displayName,
                            updatesList[1].displayName,
                            updatesList[2].displayName,
                            updatesList.size - 3
                        )
                    }
                }
            )
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()
    }

    private fun getContentIntentForDetails(context: Context, packageName: String): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.appDetailsFragment)
            .setComponentName(MainActivity::class.java)
            .setArguments(bundleOf("packageName" to packageName))
            .createPendingIntent()
    }

    private fun getContentIntentForDownloads(context: Context): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.downloadFragment)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()
    }

    private fun getInstallIntent(context: Context, download: Download): PendingIntent? {
        val intent = Intent(context, InstallActivity::class.java).apply {
            putExtra(Constants.PARCEL_DOWNLOAD, download)
        }
        return PendingIntentCompat.getActivity(
            context,
            download.packageName.hashCode(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT,
            false
        )
    }
}
