package com.aurora.store.data.room.favourite

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.data.models.App
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "favourite")
data class Favourite(
    @PrimaryKey
    val packageName: String,
    val displayName: String,
    val iconURL: String,
    val added: Long,
    val mode: Mode
) : Parcelable {

    companion object {
        fun fromApp(app: App, mode: Mode): Favourite {
            return Favourite(
                packageName = app.packageName,
                displayName = app.displayName,
                iconURL = app.iconArtwork.url,
                added = System.currentTimeMillis(),
                mode = mode
            )
        }
    }

    enum class Mode {
        MANUAL,
        IMPORT
    }
}
