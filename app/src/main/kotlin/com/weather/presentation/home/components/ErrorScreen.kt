package com.weather.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Tela de erro exibida quando não há dados disponíveis (sem cache, sem conexão).
 *
 * Oferece dois caminhos de recuperação:
 * - "Tentar novamente" → dispara nova requisição
 * - "Buscar outra cidade" → abre o [SearchSheet]
 *
 * O botão "Tentar novamente" fica desabilitado enquanto o rate limit countdown está ativo.
 *
 * @param mensagem mensagem de erro PT-BR
 * @param onTentarNovamente callback para retry
 * @param onBuscarOutraCidade callback para abrir busca
 * @param tentarNovamenteHabilitado `false` durante countdown de rate limit
 */
@Composable
fun ErrorScreen(
    mensagem: String,
    onTentarNovamente: () -> Unit,
    onBuscarOutraCidade: () -> Unit,
    modifier: Modifier = Modifier,
    tentarNovamenteHabilitado: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Não foi possível obter a previsão",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onTentarNovamente,
            enabled = tentarNovamenteHabilitado,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (tentarNovamenteHabilitado)
                        "Tentar novamente"
                    else
                        "Tentar novamente desabilitado, aguardando limite de requisições"
                }
        ) {
            Text("Tentar novamente")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBuscarOutraCidade,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Buscar outra cidade" }
        ) {
            Text("Buscar outra cidade")
        }
    }
}
