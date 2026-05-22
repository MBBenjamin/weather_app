package com.weather.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weather.R
import com.weather.domain.model.DiaDados
import com.weather.presentation.theme.WeatherColors
import com.weather.utils.WmoMapper
import kotlin.math.roundToInt

/**
 * Lista vertical de 7 cards de previsão diária (US3).
 *
 * O primeiro card exibe badge "HOJE" com cor de fundo distinta.
 * Tap em qualquer card chama [onDiaSelecionado] com o índice correspondente.
 */
@Composable
fun WeeklyForecastList(
    dias: List<DiaDados>,
    onDiaSelecionado: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.label_weekly_forecast),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        dias.forEachIndexed { index, dia ->
            DayCard(
                dia = dia,
                onClick = { onDiaSelecionado(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Card de um único dia da previsão semanal.
 *
 * Exibe badge "HOJE" quando [DiaDados.eHoje] é `true`, com cor de fundo
 * primária do tema. Demais campos: WMO ícone, descrição, máx/mín em
 * [WeatherColors.TempMax]/[WeatherColors.TempMin], chuva % e vento km/h.
 */
@Composable
fun DayCard(
    dia: DiaDados,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val descricaoWmo = stringResource(WmoMapper.descricaoWMO(dia.codigoWMO))
    val desc = "${if (dia.eHoje) "Hoje, " else ""}${dia.data}, $descricaoWmo, " +
        "Mín ${dia.temperaturaMinC.roundToInt()}°C, Máx ${dia.temperaturaMaxC.roundToInt()}°C, " +
        "Chuva ${dia.probChuvaPercent}%, Vento ${dia.velocidadeMaxVentoKmh.roundToInt()} km/h"

    val cardColors = if (dia.eHoje) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = desc },
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(WmoMapper.iconeWMO(dia.codigoWMO)),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dia.data,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (dia.eHoje) {
                            Spacer(Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = stringResource(R.string.label_today),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = descricaoWmo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row {
                    Text(
                        text = "${dia.temperaturaMinC.roundToInt()}°",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WeatherColors.TempMin
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${dia.temperaturaMaxC.roundToInt()}°",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = WeatherColors.TempMax
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${dia.probChuvaPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${dia.velocidadeMaxVentoKmh.roundToInt()} km/h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
