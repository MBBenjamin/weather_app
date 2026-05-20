package com.weather.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.weather.R
import com.weather.domain.model.DadosAtuais
import com.weather.presentation.theme.WeatherColors
import com.weather.utils.WindDirectionMapper
import com.weather.utils.WmoMapper
import kotlin.math.roundToInt

/**
 * Card principal que exibe as condições meteorológicas atuais.
 *
 * Ocupa ~60% da altura da tela com gradiente dinâmico conforme o código WMO.
 * Inclui `contentDescription` completo para TalkBack.
 */
@Composable
fun CurrentWeatherCard(
    nomeLocalidade: String,
    atual: DadosAtuais,
    timestampRelativo: String,
    modifier: Modifier = Modifier
) {
    val gradientColors = WeatherColors.gradientForWmoCode(atual.codigoWMO)
    val descricaoWmo = stringResource(WmoMapper.descricaoWMO(atual.codigoWMO))
    val direcaoCardinal = WindDirectionMapper.paraCardinal(atual.direcaoVentoGraus)

    val contentDesc = "Temperatura: ${atual.temperaturaC.roundToInt()}°C, " +
        "$descricaoWmo, " +
        "Sensação ${atual.sensacaoTermicaC.roundToInt()}°C, " +
        "Umidade ${atual.umidadePercent}%, " +
        "Vento ${atual.velocidadeVentoKmh.roundToInt()} km/h $direcaoCardinal"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .background(Brush.verticalGradient(gradientColors))
            .semantics { contentDescription = contentDesc }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = nomeLocalidade,
                style = MaterialTheme.typography.titleLarge,
                color = WeatherColors.OnSurface
            )
            Text(
                text = timestampRelativo,
                style = MaterialTheme.typography.labelSmall,
                color = WeatherColors.OnSurface.copy(alpha = 0.7f)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(WmoMapper.iconeWMO(atual.codigoWMO)),
                contentDescription = null, // descrito pelo contentDesc do Box
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${atual.temperaturaC.roundToInt()}°C",
                style = MaterialTheme.typography.displayLarge,
                color = WeatherColors.OnSurface
            )
            Text(
                text = descricaoWmo,
                style = MaterialTheme.typography.bodyLarge,
                color = WeatherColors.OnSurface.copy(alpha = 0.85f)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherDataChip(
                label = stringResource(R.string.chip_feels_like),
                value = "${atual.sensacaoTermicaC.roundToInt()}°C"
            )
            WeatherDataChip(
                label = stringResource(R.string.chip_humidity),
                value = "${atual.umidadePercent}%"
            )
            WeatherDataChip(
                label = stringResource(R.string.chip_wind),
                value = "${atual.velocidadeVentoKmh.roundToInt()} km/h $direcaoCardinal"
            )
        }
    }
}

@Composable
private fun WeatherDataChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = WeatherColors.OnSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = WeatherColors.OnSurface
        )
    }
}
