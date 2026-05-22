package com.weather.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.time.LocalTime
import java.time.ZoneId
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
    modifier: Modifier = Modifier,
    fusoHorario: String = "UTC"
) {
    val isNight = remember(fusoHorario) { isNight(fusoHorario) }
    val gradientColors = WeatherColors.gradientForWmoCode(atual.codigoWMO, isNight)
    val textColor = if (isNight) Color.White else WeatherColors.OnSurface
    val descricaoWmo = stringResource(WmoMapper.descricaoWMO(atual.codigoWMO))
    val direcaoCardinal = WindDirectionMapper.paraCardinal(atual.direcaoVentoGraus)

    val contentDesc = "Temperatura: ${atual.temperaturaC.roundToInt()}°C, " +
        "$descricaoWmo, " +
        "Sensação ${atual.sensacaoTermicaC.roundToInt()}°C, " +
        "Umidade ${atual.umidadePercent}%, " +
        "Vento ${atual.velocidadeVentoKmh.roundToInt()} km/h $direcaoCardinal"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .background(Brush.verticalGradient(gradientColors))
            .semantics { contentDescription = contentDesc }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = nomeLocalidade,
                style = MaterialTheme.typography.titleLarge,
                color = textColor
            )
            Text(
                text = timestampRelativo,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(WmoMapper.iconeWMO(atual.codigoWMO)),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${atual.temperaturaC.roundToInt()}°C",
                style = MaterialTheme.typography.displayLarge,
                color = textColor
            )
            Text(
                text = descricaoWmo,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor.copy(alpha = 0.85f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherDataChip(
                label = stringResource(R.string.chip_feels_like),
                value = "${atual.sensacaoTermicaC.roundToInt()}°C",
                textColor = textColor
            )
            WeatherDataChip(
                label = stringResource(R.string.chip_humidity),
                value = "${atual.umidadePercent}%",
                textColor = textColor
            )
            WeatherDataChip(
                label = stringResource(R.string.chip_wind),
                value = "${atual.velocidadeVentoKmh.roundToInt()} km/h $direcaoCardinal",
                textColor = textColor
            )
        }
    }
}

@Composable
private fun WeatherDataChip(label: String, value: String, textColor: Color = WeatherColors.OnSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

private fun isNight(fusoHorario: String): Boolean {
    val hour = try {
        LocalTime.now(ZoneId.of(fusoHorario)).hour
    } catch (_: Exception) {
        LocalTime.now().hour
    }
    return hour !in 6..<18
}
