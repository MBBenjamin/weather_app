package com.weather.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.weather.domain.model.DadosAtuais
import com.weather.presentation.home.components.CurrentWeatherCard
import com.weather.presentation.home.components.OfflineBadge
import com.weather.presentation.theme.WeatherTheme
import org.junit.Rule
import org.junit.Test

/**
 * Screenshots de regressão visual dos componentes principais via Paparazzi.
 *
 * Executar: `./gradlew recordPaparazziDebug` (grava baseline)
 *           `./gradlew verifyPaparazziDebug` (verifica regressões)
 */
class HomeScreenScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    @Test
    fun portrait_light_theme() {
        paparazzi.snapshot {
            WeatherTheme {
                CurrentWeatherCard(
                    nomeLocalidade = "São Paulo, SP",
                    atual = fakeDadosAtuais(),
                    timestampRelativo = "Atualizado há 5 min"
                )
            }
        }
    }

    @Test
    fun portrait_offline_state() {
        paparazzi.snapshot {
            WeatherTheme {
                androidx.compose.foundation.layout.Column {
                    OfflineBadge(horasAtraso = 3)
                    CurrentWeatherCard(
                        nomeLocalidade = "São Paulo, SP",
                        atual = fakeDadosAtuais(),
                        timestampRelativo = "Atualizado há 3h"
                    )
                }
            }
        }
    }

    // --- helpers ---

    private fun fakeDadosAtuais() = DadosAtuais(
        temperaturaC = 24f,
        sensacaoTermicaC = 26f,
        umidadePercent = 68,
        velocidadeVentoKmh = 12f,
        direcaoVentoGraus = 180,
        codigoWMO = 1,
        horaAtualizado = "2026-05-19T14:00"
    )
}
