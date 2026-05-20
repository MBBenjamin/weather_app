package com.weather.presentation.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weather.R
import com.weather.domain.model.DiaDados
import com.weather.domain.model.HoraDados
import com.weather.presentation.home.HomeUiState
import com.weather.presentation.home.HomeViewModel
import com.weather.presentation.home.components.HourCard
import com.weather.presentation.theme.WeatherColors
import com.weather.utils.WindDirectionMapper
import com.weather.utils.WmoMapper
import kotlin.math.roundToInt

/**
 * Bottom sheet de detalhe para um dia selecionado na previsão semanal.
 *
 * Contém cabeçalho com data, ícone WMO 80dp e máx/mín; botões de navegação
 * entre os 7 dias; e [TabRow] com duas abas:
 * - "Horário": lista de [HourCard]s do dia
 * - "Índices": umidade máx, vento máx e direção dominante
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val diaSelecionadoIndex by viewModel.diaSelecionadoIndex.collectAsStateWithLifecycle()
    val horasDoDiaSelecionado by viewModel.horasDoDiaSelecionado.collectAsStateWithLifecycle()

    val dias = (uiState as? HomeUiState.Sucesso)?.previsao?.diario?.dias ?: return
    val dia = dias.getOrNull(diaSelecionadoIndex) ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            DayDetailHeader(
                dia = dia,
                diaSelecionadoIndex = diaSelecionadoIndex,
                totalDias = dias.size,
                onNavegar = { delta -> viewModel.navegarDia(delta) }
            )

            Spacer(Modifier.height(16.dp))

            DayDetailTabs(
                dia = dia,
                horas = horasDoDiaSelecionado
            )
        }
    }
}

@Composable
private fun DayDetailHeader(
    dia: DiaDados,
    diaSelecionadoIndex: Int,
    totalDias: Int,
    onNavegar: (Int) -> Unit
) {
    val descricaoWmo = stringResource(WmoMapper.descricaoWMO(dia.codigoWMO))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = { onNavegar(-1) },
            enabled = diaSelecionadoIndex > 0,
            modifier = Modifier.semantics {
                contentDescription = "Dia anterior"
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dia.data,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(WmoMapper.iconeWMO(dia.codigoWMO)),
                contentDescription = descricaoWmo,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(4.dp))
            Row {
                Text(
                    text = "${dia.temperaturaMaxC.roundToInt()}°",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = WeatherColors.TempMax
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${dia.temperaturaMinC.roundToInt()}°",
                    style = MaterialTheme.typography.headlineMedium,
                    color = WeatherColors.TempMin
                )
            }
            Text(
                text = descricaoWmo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        IconButton(
            onClick = { onNavegar(1) },
            enabled = diaSelecionadoIndex < totalDias - 1,
            modifier = Modifier.semantics {
                contentDescription = "Próximo dia"
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun DayDetailTabs(
    dia: DiaDados,
    horas: List<HoraDados>
) {
    var abaAtiva by remember { mutableIntStateOf(0) }
    val abas = listOf(
        stringResource(R.string.label_day_tab_hourly),
        stringResource(R.string.label_day_tab_indices)
    )

    TabRow(selectedTabIndex = abaAtiva) {
        abas.forEachIndexed { index, titulo ->
            Tab(
                selected = abaAtiva == index,
                onClick = { abaAtiva = index },
                text = { Text(titulo) }
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    when (abaAtiva) {
        0 -> AbaHorario(horas = horas)
        1 -> AbaIndices(dia = dia)
    }
}

@Composable
private fun AbaHorario(horas: List<HoraDados>) {
    if (horas.isEmpty()) {
        Text(
            text = stringResource(R.string.day_detail_no_hourly),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // LazyColumn com altura fixa para funcionar dentro do ModalBottomSheet
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(horas) { hora ->
            HourCard(
                hora = hora,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AbaIndices(dia: DiaDados) {
    val cardinal = WindDirectionMapper.paraCardinal(dia.direcaoDominanteVentoGraus)
    val labelUmidade = stringResource(R.string.index_humidity_max)
    val labelVento = stringResource(R.string.index_wind_max)
    val labelDirecao = stringResource(R.string.index_wind_direction)
    val labelChuva = stringResource(R.string.index_rain_prob)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IndiceCard(
            label = labelUmidade,
            value = "${dia.umidadeMaxPercent}%",
            desc = "$labelUmidade: ${dia.umidadeMaxPercent}%"
        )
        IndiceCard(
            label = labelVento,
            value = "${"%.1f".format(dia.velocidadeMaxVentoKmh)} km/h",
            desc = "$labelVento: ${"%.1f".format(dia.velocidadeMaxVentoKmh)} km/h"
        )
        IndiceCard(
            label = labelDirecao,
            value = "$cardinal (${dia.direcaoDominanteVentoGraus}°)",
            desc = "$labelDirecao: $cardinal, ${dia.direcaoDominanteVentoGraus} graus"
        )
        IndiceCard(
            label = labelChuva,
            value = "${dia.probChuvaPercent}%",
            desc = "$labelChuva: ${dia.probChuvaPercent}%"
        )
    }
}

@Composable
private fun IndiceCard(label: String, value: String, desc: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = desc },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
