package com.weather.presentation.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.weather.R

/**
 * Banner laranja fixo exibido quando a API retorna HTTP 429 (rate limit).
 *
 * Permanece visível enquanto o countdown está ativo, garantindo que o usuário
 * veja o tempo restante sem interação. O botão "Tentar novamente" no [ErrorScreen]
 * fica desabilitado enquanto este banner está visível.
 *
 * @param secondsRemaining segundos até a próxima tentativa automática
 */
@Composable
fun RateLimitBanner(secondsRemaining: Int, modifier: Modifier = Modifier) {
    val descricaoAcessibilidade = stringResource(R.string.label_rate_limit_banner, secondsRemaining)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = descricaoAcessibilidade },
        color = Color(0xFFF57F17).copy(alpha = 0.15f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF57F17),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.label_rate_limit_banner, secondsRemaining),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFF57F17),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
