package com.weather.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.weather.domain.model.HoraDados
import com.weather.utils.WmoMapper
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Seção de previsão horária com duas abas: gráfico Vico e listagem de cards.
 *
 * @param horas lista de horas do dia atual (tipicamente 24 entradas)
 * @param onHoraSelecionada callback chamado ao tocar em um [HourCard]
 */
@Composable
fun HourlyForecastSection(
    horas: List<HoraDados>,
    onHoraSelecionada: (HoraDados) -> Unit,
    modifier: Modifier = Modifier,
    fusoHorario: String = "UTC"
) {
    var abaAtiva by remember { mutableIntStateOf(0) }
    val abas = listOf(
        stringResource(com.weather.R.string.label_hourly_tab_chart),
        stringResource(com.weather.R.string.label_hourly_tab_list)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = abaAtiva) {
            abas.forEachIndexed { index, titulo ->
                Tab(
                    selected = abaAtiva == index,
                    onClick = { abaAtiva = index },
                    text = { Text(titulo) }
                )
            }
        }

        when (abaAtiva) {
            0 -> GraficoHorario(horas = horas, fusoHorario = fusoHorario, modifier = Modifier.fillMaxWidth())
            1 -> ListagemHoraria(horas = horas, fusoHorario = fusoHorario, onHoraSelecionada = onHoraSelecionada)
        }
    }
}

@Composable
private fun GraficoHorario(horas: List<HoraDados>, fusoHorario: String, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer.build() }

    // Índice da hora atual — usa o fuso da localidade, não o do dispositivo
    val horaAtualIndex = remember(horas, fusoHorario) {
        val fuso = try { java.time.ZoneId.of(fusoHorario) } catch (_: Exception) { java.time.ZoneId.of("UTC") }
        val horaAtual = LocalTime.now(fuso).hour
        horas.indexOfFirst { it.hora.startsWith(horaAtual.toString().padStart(2, '0')) }
            .takeIf { it >= 0 } ?: 0
    }

    LaunchedEffect(horas) {
        if (horas.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries { series(horas.map { it.temperaturaC }) }
                columnSeries { series(horas.map { it.precipitacaoMm.coerceAtLeast(0f) }) }
            }
        }
    }

    if (horas.isNotEmpty()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                rememberColumnCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
            ),
            modelProducer = modelProducer,
            modifier = modifier
                .height(220.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ListagemHoraria(
    horas: List<HoraDados>,
    fusoHorario: String,
    onHoraSelecionada: (HoraDados) -> Unit
) {
    val listState: LazyListState = rememberLazyListState()

    // Scroll automático — usa o fuso da localidade, não o do dispositivo
    val horaAtualIndex = remember(horas, fusoHorario) {
        val fuso = try { java.time.ZoneId.of(fusoHorario) } catch (_: Exception) { java.time.ZoneId.of("UTC") }
        val horaAtual = LocalTime.now(fuso).hour
        horas.indexOfFirst { it.hora.startsWith(horaAtual.toString().padStart(2, '0')) }
            .takeIf { it >= 0 } ?: 0
    }

    LaunchedEffect(horaAtualIndex) {
        if (horas.isNotEmpty()) {
            listState.animateScrollToItem(horaAtualIndex)
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(horas) { _, hora ->
            HourCard(hora = hora, onClick = { onHoraSelecionada(hora) })
        }
    }
}

/**
 * Card individual de previsão horária.
 *
 * Usado na aba "Listagem" do [HourlyForecastSection] e reutilizado no [HourDetailSheet].
 */
@Composable
fun HourCard(
    hora: HoraDados,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val descricaoWmo = stringResource(WmoMapper.descricaoWMO(hora.codigoWMO))
    val desc = "${hora.hora}, $descricaoWmo, ${hora.temperaturaC.roundToInt()}°C, " +
        "precipitação ${hora.precipitacaoMm}mm"

    Card(
        modifier = modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = desc },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = hora.hora,
                style = MaterialTheme.typography.labelSmall
            )
            Image(
                painter = painterResource(WmoMapper.iconeWMO(hora.codigoWMO)),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "${hora.temperaturaC.roundToInt()}°C",
                style = MaterialTheme.typography.bodyLarge
            )
            if (hora.precipitacaoMm > 0f) {
                Text(
                    text = "${hora.precipitacaoMm}mm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
