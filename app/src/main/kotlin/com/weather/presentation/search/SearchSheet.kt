package com.weather.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weather.domain.model.CidadeSugestao
import com.weather.domain.model.HistoricoBusca

/**
 * Bottom sheet de busca manual de cidades (US5).
 *
 * Exibe histórico ao focar o [SearchBar] e lista de sugestões ao digitar.
 * Tap em qualquer item chama [SearchViewModel.selecionarCidade], aciona haptic
 * feedback, fecha o teclado e fecha o sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    viewModel: SearchViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.carregarHistorico()
    }

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
            SearchBar(
                query = query,
                onQueryChange = { q ->
                    query = q
                    viewModel.onQueryChange(q)
                },
                onSearch = { /* debounce já trata */ },
                active = searchActive,
                onActiveChange = { searchActive = it },
                placeholder = { Text("Buscar cidade...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Campo de busca de cidade" }
            ) {
                SearchContent(
                    uiState = uiState,
                    onCidadeSelecionada = { cidade ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selecionarCidade(cidade)
                        focusManager.clearFocus()
                        onDismiss()
                    },
                    onHistoricoSelecionado = { hist ->
                        val cidade = CidadeSugestao(
                            nome = hist.nomeCidade,
                            estado = hist.estado,
                            pais = hist.pais,
                            latitude = hist.latitude,
                            longitude = hist.longitude,
                            fusoHorario = null
                        )
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selecionarCidade(cidade)
                        focusManager.clearFocus()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchContent(
    uiState: SearchUiState,
    onCidadeSelecionada: (CidadeSugestao) -> Unit,
    onHistoricoSelecionado: (HistoricoBusca) -> Unit
) {
    when (uiState) {
        is SearchUiState.Idle,
        is SearchUiState.Resultados -> {
            val historico = (uiState as? SearchUiState.Resultados)?.historico ?: emptyList()
            val sugestoes = (uiState as? SearchUiState.Resultados)?.sugestoes ?: emptyList()

            if (sugestoes.isNotEmpty()) {
                LazyColumn {
                    items(sugestoes) { cidade ->
                        ResultItem(
                            cidade = cidade,
                            onClick = { onCidadeSelecionada(cidade) }
                        )
                        HorizontalDivider()
                    }
                }
            } else if (historico.isNotEmpty()) {
                LazyColumn {
                    items(historico) { hist ->
                        HistoryItem(
                            historico = hist,
                            onClick = { onHistoricoSelecionado(hist) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        is SearchUiState.Carregando -> {
            Text(
                text = "Buscando...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(16.dp)
            )
        }
        is SearchUiState.Erro -> {
            Text(
                text = uiState.mensagem,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun ResultItem(cidade: CidadeSugestao, onClick: () -> Unit) {
    val subtitulo = listOfNotNull(cidade.estado, cidade.pais).joinToString(", ")
    val desc = "${cidade.nome}, $subtitulo"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = "Selecionar $desc" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = cidade.nome,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitulo.isNotEmpty()) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(historico: HistoricoBusca, onClick: () -> Unit) {
    val subtitulo = listOfNotNull(historico.estado, historico.pais).joinToString(", ")
    val desc = "${historico.nomeCidade}, $subtitulo"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = "Histórico: $desc" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = historico.nomeCidade,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitulo.isNotEmpty()) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
