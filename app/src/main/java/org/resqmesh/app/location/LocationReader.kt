package org.resqmesh.app.location
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

object LocationReader {
    /** No network call. A missing or stale fix never prevents SOS creation. */
    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): Location? {
        val manager=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.getProviders(true).mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }
}
