package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import pt.isec.amov.cityfix.model.Coordinates

/** Far zoom for the approximate area (brief I.1.C: "mapa com zoom afastado"). */
private const val COARSE_ZOOM = 14f

/** The class zoom (Contacts_#3 ShowScreen) for an exact location. */
private const val EXACT_ZOOM = 15f

/** Radius of the circle drawn around a coarse location (F-012 Q9). */
private const val COARSE_RADIUS_METERS = 750.0

/**
 * A Google map of [location], as in Contacts_#3 (GoogleMap + rememberCameraPositionState).
 * Coarse ([exact] = false, anonymous users): far zoom, a circle, no marker, and every gesture off
 * (MapUiSettings), so the map cannot be moved or zoomed in. Exact (operators, D-013): a Marker at
 * zoom 15 and the normal gestures. Previews cannot draw a Google map, so they show a placeholder.
 * Without a valid Maps key in the manifest the map shows grey tiles.
 */
@Composable
fun TicketMap(location: Coordinates, exact: Boolean, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val point = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(point, if (exact) EXACT_ZOOM else COARSE_ZOOM)
    }
    val uiSettings = if (exact) {
        MapUiSettings()
    } else {
        MapUiSettings(
            zoomControlsEnabled = false,
            zoomGesturesEnabled = false,
            scrollGesturesEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            mapToolbarEnabled = false,
        )
    }
    val circleColor = MaterialTheme.colorScheme.primary

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
    ) {
        if (exact) {
            val markerState = remember(point) { MarkerState(position = point) }
            Marker(state = markerState)
        } else {
            Circle(
                center = point,
                radius = COARSE_RADIUS_METERS,
                fillColor = circleColor.copy(alpha = 0.25f),
                strokeColor = circleColor,
                strokeWidth = 4f,
            )
        }
    }
}
