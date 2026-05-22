package com.weather.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import com.weather.data.location.ILocationSource
import com.weather.data.location.LocationHandlerImpl
import com.weather.domain.location.LocationResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários de [LocationHandlerImpl].
 *
 * Usam [ILocationSource] mockado para isolar a lógica de GPS/Network
 * sem depender do Play Services Task API.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationHandlerTest {

    private lateinit var locationSource: ILocationSource
    private lateinit var context: Context
    private lateinit var locationHandler: LocationHandlerImpl

    private val lat = -23.55
    private val lon = -46.63

    @Before
    fun setup() {
        locationSource = mockk()
        context = mockk()
        // Permissão concedida por padrão
        every {
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        locationHandler = LocationHandlerImpl(
            locationSource = locationSource,
            context = context,
        ).also {
            it.gpsTimeoutMs = 200L
            it.currentLocTimeoutMs = 200L
        }
    }

    @Test
    fun network_provider_emite_localizacao_com_isApproximate_true() = runTest {
        val loc = buildLocation(lat, lon)
        coEvery { locationSource.ultimaLocalizacao() } returns loc
        coEvery { locationSource.localizacaoAtual() } returns loc // GPS sem delta significativo

        val results = locationHandler.observarLocalizacao().toList()

        assertTrue(results.isNotEmpty())
        val primeiro = results.first()
        assertTrue("Esperado Success, obtido: $primeiro", primeiro is LocationResult.Success)
        assertTrue("isApproximate deve ser true para Network location",
            (primeiro as LocationResult.Success).isApproximate)
        assertEquals(lat, primeiro.lat, 0.0001)
        assertEquals(lon, primeiro.lon, 0.0001)
    }

    @Test
    fun gps_refinement_timeout_30s_cancela_sem_emitir_erro_ao_usuario() = runTest {
        val loc = buildLocation(lat, lon)
        coEvery { locationSource.ultimaLocalizacao() } returns loc
        coEvery { locationSource.localizacaoAtual() } coAnswers {
            delay(10_000L) // bem mais longo que o timeout de 200ms
            loc
        }

        val results = locationHandler.observarLocalizacao().toList()

        // Apenas Success inicial — sem LocationFailed após timeout do GPS
        assertEquals(1, results.size)
        assertTrue(results.first() is LocationResult.Success)
        assertFalse("Não deve emitir LocationFailed por timeout de GPS",
            results.any { it is LocationResult.LocationFailed })
    }

    @Test
    fun lastLocation_null_chama_getCurrentLocation_com_timeout_5s() = runTest {
        val loc = buildLocation(lat, lon)
        coEvery { locationSource.ultimaLocalizacao() } returns null
        coEvery { locationSource.localizacaoAtual() } returns loc

        val results = locationHandler.observarLocalizacao().toList()

        assertTrue(results.isNotEmpty())
        val primeiro = results.first()
        assertTrue("Esperado Success via getCurrentLocation, obtido: $primeiro",
            primeiro is LocationResult.Success)
        // Localização obtida via getCurrentLocation não é aproximada
        assertFalse("isApproximate deve ser false quando veio de getCurrentLocation",
            (primeiro as LocationResult.Success).isApproximate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun coordenadas_lat_fora_de_range_lanca_IllegalArgumentException() = runTest {
        val locInvalida = buildLocation(200.0, 0.0) // lat inválida
        coEvery { locationSource.ultimaLocalizacao() } returns locInvalida

        // Deve lançar IllegalArgumentException durante a coleta
        locationHandler.observarLocalizacao().toList()
    }

    @Test
    fun permissao_negada_emite_LocationResult_PermissionDenied() = runTest {
        every {
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        val results = locationHandler.observarLocalizacao().toList()

        assertEquals(1, results.size)
        assertTrue("Esperado PermissionDenied, obtido: ${results.first()}",
            results.first() is LocationResult.PermissionDenied)
    }

    // --- helper ---

    private fun buildLocation(latitude: Double, longitude: Double): Location =
        mockk<Location>().also {
            every { it.latitude } returns latitude
            every { it.longitude } returns longitude
        }
}
