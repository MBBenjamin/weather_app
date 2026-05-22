package com.weather.presentation.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weather.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weather.domain.model.HoraDados
import com.weather.presentation.detail.DayDetailSheet
import com.weather.presentation.detail.HourDetailSheet
import com.weather.presentation.home.components.CurrentWeatherCard
import com.weather.presentation.home.components.ErrorScreen
import com.weather.presentation.home.components.HourlyForecastSection
import com.weather.presentation.home.components.LoadingSkeleton
import com.weather.presentation.home.components.LocationBadge
import com.weather.presentation.home.components.OfflineBadge
import com.weather.presentation.home.components.RateLimitBanner
import com.weather.presentation.home.components.WeeklyForecastList
import com.weather.presentation.search.SearchSheet
import com.weather.presentation.search.SearchViewModel

/**
 * Tela principal do app — ponto de entrada da US1 (Previsão Atual).
 *
 * Gerencia pull-to-refresh com feedback háptico, coleta o [HomeUiState] do
 * [HomeViewModel] e distribui para os composables filhos conforme o estado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val horasDoDia by viewModel.horasDoDia.collectAsStateWithLifecycle()
    var horaSelecionada by remember { mutableStateOf<HoraDados?>(null) }
    var mostrarDayDetail by remember { mutableStateOf(false) }
    var mostrarBusca by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.verificarLocalizacao()
        }
    }

    LaunchedEffect(Unit) {
        searchViewModel.cidadeSelecionadaEvent.collect { cidade ->
            viewModel.selecionarCidade(cidade.latitude, cidade.longitude, cidade.nome)
        }
    }
    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onPullToRefresh()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState !is HomeUiState.Carregando && pullRefreshState.isRefreshing) {
            pullRefreshState.endRefresh()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            when (val state = uiState) {
                is HomeUiState.Carregando -> LoadingSkeleton()

                is HomeUiState.Sucesso -> SucessoContent(
                    state = state,
                    horasDoDia = horasDoDia,
                    onHoraSelecionada = { horaSelecionada = it },
                    onDiaSelecionado = { index ->
                        viewModel.abrirDia(index)
                        mostrarDayDetail = true
                    },
                    onAbrirBusca = { mostrarBusca = true }
                )

                is HomeUiState.Erro -> ErroContent(
                    state = state,
                    onTentarNovamente = { viewModel.tentarNovamente() },
                    onAbrirBusca = { mostrarBusca = true }
                )

                is HomeUiState.SemPermissao -> SemPermissaoContent(
                    definitiva = false,
                    onSolicitarPermissao = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )

                is HomeUiState.SemPermissaoDefinitiva -> SemPermissaoContent(
                    definitiva = true,
                    onSolicitarPermissao = {}
                )
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    horaSelecionada?.let { hora ->
        HourDetailSheet(
            hora = hora,
            onDismiss = { horaSelecionada = null }
        )
    }

    if (mostrarDayDetail) {
        DayDetailSheet(
            viewModel = viewModel,
            onDismiss = { mostrarDayDetail = false }
        )
    }

    if (mostrarBusca) {
        SearchSheet(
            viewModel = searchViewModel,
            onDismiss = { mostrarBusca = false }
        )
    }
}

@Composable
private fun SucessoContent(
    state: HomeUiState.Sucesso,
    horasDoDia: List<HoraDados>,
    onHoraSelecionada: (HoraDados) -> Unit,
    onDiaSelecionado: (Int) -> Unit,
    onAbrirBusca: () -> Unit
) {
    val scrollState = rememberScrollState()
    val searchDesc = stringResource(R.string.a11y_search_button)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onAbrirBusca,
                modifier = Modifier.semantics { contentDescription = searchDesc }
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        }
        if (state.isLocalizacaoAproximada) {
            LocationBadge(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (state.isOffline) {
            OfflineBadge(
                horasAtraso = state.horasAtraso,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        CurrentWeatherCard(
            nomeLocalidade = state.nomeLocalidade,
            atual = state.previsao.atual,
            timestampRelativo = state.timestampRelativo,
            fusoHorario = state.previsao.fusoHorario,
            modifier = Modifier.fillMaxWidth()
        )
        if (horasDoDia.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HourlyForecastSection(
                horas = horasDoDia,
                onHoraSelecionada = onHoraSelecionada,
                fusoHorario = state.previsao.fusoHorario,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.previsao.diario.dias.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            WeeklyForecastList(
                dias = state.previsao.diario.dias,
                onDiaSelecionado = onDiaSelecionado,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.data_credit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun ErroContent(
    state: HomeUiState.Erro,
    onTentarNovamente: () -> Unit,
    onAbrirBusca: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ErrorScreen(
            mensagem = state.mensagem,
            onTentarNovamente = onTentarNovamente,
            onBuscarOutraCidade = onAbrirBusca,
            tentarNovamenteHabilitado = state.rateLimitSecondsRemaining == null
        )
        state.rateLimitSecondsRemaining?.let { segundos ->
            RateLimitBanner(
                secondsRemaining = segundos,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun SemPermissaoContent(definitiva: Boolean, onSolicitarPermissao: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(120.dp))
        Text(
            text = if (definitiva)
                stringResource(R.string.permission_location_denied_permanent_title)
            else
                stringResource(R.string.permission_location_denied_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_location_rationale),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(32.dp))
        if (definitiva) {
            Button(onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }) {
                Text(stringResource(R.string.label_open_settings))
            }
        } else {
            Button(onClick = onSolicitarPermissao) {
                Text(stringResource(R.string.label_grant_permission))
            }
        }
    }
}
