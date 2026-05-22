package com.weather.data.location

import android.content.Context
import android.content.pm.PackageManager
import com.weather.domain.location.ILocationHandler
import com.weather.domain.location.LocationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Implementação de [ILocationHandler] com estratégia híbrida Network + GPS.
 *
 * 1. Verifica permissões — emite [LocationResult.PermissionDenied] se ausentes.
 * 2. Obtém `lastLocation` (tipicamente Network, rápido) → emite [LocationResult.Success]
 *    com `isApproximate=true`.
 * 3. Em background refina com GPS (timeout [GPS_TIMEOUT_MS]). Se delta > 100m emite
 *    [LocationResult.GpsRefinement]. Timeout silencioso — sem erro para o usuário.
 * 4. Se `lastLocation` for null, tenta `getCurrentLocation` com timeout [CURRENT_LOC_TIMEOUT_MS].
 *    Falha → emite [LocationResult.LocationFailed].
 */
class LocationHandlerImpl @Inject constructor(
    internal val locationSource: ILocationSource,
    @ApplicationContext private val context: Context,
) : ILocationHandler {

    internal var gpsTimeoutMs: Long = GPS_TIMEOUT_MS
    internal var currentLocTimeoutMs: Long = CURRENT_LOC_TIMEOUT_MS

    companion object {
        const val GPS_TIMEOUT_MS = 30_000L
        const val CURRENT_LOC_TIMEOUT_MS = 5_000L
    }

    override fun observarLocalizacao(): Flow<LocationResult> = flow {
        if (!hasPermission()) {
            emit(LocationResult.PermissionDenied)
            return@flow
        }

        val lastLoc = locationSource.ultimaLocalizacao()

        if (lastLoc != null) {
            val lat = lastLoc.latitude
            val lon = lastLoc.longitude
            validarCoordenadas(lat, lon)
            emit(LocationResult.Success(lat, lon, isApproximate = true))

            // Refinamento GPS silencioso — timeout não propaga erro
            try {
                withTimeout(gpsTimeoutMs) {
                    val gpsLoc = locationSource.localizacaoAtual()
                    if (gpsLoc != null) {
                        val delta = haversineMetros(lat, lon, gpsLoc.latitude, gpsLoc.longitude)
                        if (delta > 100.0) {
                            emit(LocationResult.GpsRefinement(gpsLoc.latitude, gpsLoc.longitude))
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                // GPS demorou mais que gpsTimeoutMs — ignora silenciosamente
            }
        } else {
            // Sem lastLocation — fallback para getCurrentLocation com timeout curto
            try {
                val loc = withTimeout(currentLocTimeoutMs) {
                    locationSource.localizacaoAtual()
                }
                if (loc != null) {
                    validarCoordenadas(loc.latitude, loc.longitude)
                    emit(LocationResult.Success(loc.latitude, loc.longitude, isApproximate = false))
                } else {
                    emit(LocationResult.LocationFailed)
                }
            } catch (_: TimeoutCancellationException) {
                emit(LocationResult.LocationFailed)
            }
        }
    }

    private fun hasPermission(): Boolean {
        val fine = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun validarCoordenadas(lat: Double, lon: Double) {
        require(lat in -90.0..90.0) { "Latitude inválida: $lat (deve estar em [-90, 90])" }
        require(lon in -180.0..180.0) { "Longitude inválida: $lon (deve estar em [-180, 180])" }
    }

    /** Fórmula de Haversine simplificada para distância aproximada em metros. */
    private fun haversineMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0 // raio da Terra em metros
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = sin(dLat / 2).let { it * it } +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLon / 2).let { it * it }
        return r * 2.0 * atan2(sqrt(a), sqrt(1 - a))
    }
}
