# Research: Weather App Android MVP v1.0

**Gerado por**: `/speckit-plan` | **Data**: 2026-05-17 | **Plano**: [plan.md](plan.md)

---

## Decisão 1: Biblioteca de Gráfico para Previsão Horária

**Contexto**: RF-02 requer gráfico de linha (temperatura) + área (precipitação) em Jetpack Compose.

**Decision**: **Vico 1.13.0**

**Rationale**:
- Nativo Compose (não wrapping de View legacy)
- API declarativa limpa, fácil de customizar cores e linhas
- Mantido ativamente (2024)
- Suporte a múltiplas séries (temperatura + precipitação no mesmo chart)
- Sem overhead de conversão View ↔ Compose

**Alternatives considered**:
- **MPAndroidChart**: Mais maduro, mas requer AndroidView wrapper em Compose → overhead + código verboso
- **Custom Canvas**: Total controle, zero dependência, mas alto custo de implementação para escala/eixos/labels (≥3x mais código)
- **Charty**: Leve mas menos features para gráfico de área + linha combinados

---

## Decisão 2: Endpoint Base para Geocoding

**Contexto**: RF-05 requer busca de cidades por nome via Open-Meteo.

**Decision**: `https://geocoding-api.open-meteo.com/v1/search`

**Rationale**:
- URL correta do serviço de geocoding Open-Meteo (separado do forecast)
- A spec.md usava `api.open-meteo.com/v1/geocoding` que é **incorreta** — o geocoding usa um domínio diferente
- Suporte a `language=pt` para nomes em Português

**Alternatives considered**:
- Google Places API: Paga, requer key, overkill para MVP
- Nominatim (OpenStreetMap): Gratuito mas sem language=pt para nomes de cidades, rate limit restritivo

---

## Decisão 3: Estratégia de Mapeamento WMO

**Contexto**: Todos os weather codes WMO (0-99) precisam mapear para ícone + texto PT-BR.

**Decision**: Kotlin `object` com `Map<Int, WmoInfo>` — compilado em tempo de build, zero I/O.

```kotlin
data class WmoInfo(val icone: Int, val descricao: String)

object WmoMapper {
    private val mapa = mapOf(
        0 to WmoInfo(R.drawable.ic_clear_sky, "Céu limpo"),
        1 to WmoInfo(R.drawable.ic_mostly_clear, "Predominantemente limpo"),
        2 to WmoInfo(R.drawable.ic_partly_cloudy, "Parcialmente nublado"),
        3 to WmoInfo(R.drawable.ic_overcast, "Nublado"),
        45 to WmoInfo(R.drawable.ic_fog, "Neblina"),
        48 to WmoInfo(R.drawable.ic_rime_fog, "Neblina com geada"),
        51 to WmoInfo(R.drawable.ic_drizzle_light, "Garoa leve"),
        53 to WmoInfo(R.drawable.ic_drizzle_mod, "Garoa moderada"),
        55 to WmoInfo(R.drawable.ic_drizzle_dense, "Garoa densa"),
        61 to WmoInfo(R.drawable.ic_rain_slight, "Chuva fraca"),
        63 to WmoInfo(R.drawable.ic_rain_mod, "Chuva moderada"),
        65 to WmoInfo(R.drawable.ic_rain_heavy, "Chuva forte"),
        71 to WmoInfo(R.drawable.ic_snow_slight, "Neve fraca"),
        73 to WmoInfo(R.drawable.ic_snow_mod, "Neve moderada"),
        75 to WmoInfo(R.drawable.ic_snow_heavy, "Neve forte"),
        80 to WmoInfo(R.drawable.ic_shower_slight, "Pancadas fracas"),
        81 to WmoInfo(R.drawable.ic_shower_mod, "Pancadas moderadas"),
        82 to WmoInfo(R.drawable.ic_shower_violent, "Pancadas fortes"),
        95 to WmoInfo(R.drawable.ic_thunderstorm, "Trovoada"),
        96 to WmoInfo(R.drawable.ic_thunderstorm_hail, "Trovoada com granizo"),
        99 to WmoInfo(R.drawable.ic_thunderstorm_hail_heavy, "Trovoada com granizo forte")
    )
    
    fun mapear(codigo: Int): WmoInfo = mapa[codigo] ?: WmoInfo(R.drawable.ic_cloudy, "Nublado")
}
```

**Rationale**: Sem I/O, zero dependências, testável unitariamente, compile-safe.

**Alternatives considered**:
- JSON file em assets: Requer parsing em runtime, I/O adicional
- Room table: Overhead desnecessário para dados estáticos

---

## Decisão 4: Estratégia de Localização (FusedLocationProvider)

**Contexto**: RF-04 requer Network rápido (≤2s) + GPS refinement em background.

**Decision**: FusedLocationProviderClient com duas etapas:

**Etapa 1 — Network rápido (Main thread OK)**:
```kotlin
fusedLocationClient.lastLocation.addOnSuccessListener { location ->
    if (location != null) {
        // Pode ser Network ou GPS (último disponível)
        viewModel.carregarPrevisao(location.latitude, location.longitude, aproximado = true)
    }
}
```

**Etapa 2 — GPS refinement (Background)**:
```kotlin
val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
    .setMinUpdateDistanceMeters(100f) // Só atualizar se mudou >100m
    .setMaxUpdates(1) // Apenas 1 atualização GPS
    .build()
    
fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
```

**Rationale**:
- `lastLocation` retorna em <500ms (cache do sistema)
- `requestLocationUpdates` com `PRIORITY_HIGH_ACCURACY` ativa GPS em background
- `setMinUpdateDistanceMeters(100f)` evita requisições desnecessárias (RF-04.7: "GPS coords mudaram >100m")
- `setMaxUpdates(1)` libera o listener automaticamente após refinement

**Alternatives considered**:
- `getCurrentLocation()`: Blocking, não suporta Network + GPS em paralelo
- `requestSingleUpdate()` (deprecated): API antiga, menos confiável

---

## Decisão 5: Detecção de Conectividade Offline

**Contexto**: RF-06 requer detecção de offline para badge e fallback cache.

**Decision**: `ConnectivityManager.NetworkCallback` + StateFlow

```kotlin
class NetworkMonitor @Inject constructor(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    
    val isOnline: StateFlow<Boolean> = callbackFlow {
        val callback = object : NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), isCurrentlyOnline())
    
    private fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NET_CAPABILITY_INTERNET)
    }
}
```

**Rationale**:
- Reativo (StateFlow) — UI observa mudanças automaticamente
- `callbackFlow` integra perfeitamente com Coroutines
- Sem polling periódico (battery friendly)

**Alternatives considered**:
- `BroadcastReceiver` com `CONNECTIVITY_ACTION` (deprecated API 28)
- Polling manual: Desperdiça bateria, não reativo

---

## Decisão 6: Estratégia de Ícones WMO

**Contexto**: 20+ condições WMO com ícones em tamanho ≥80dp.

**Decision**: SVG vetoriais Android (VectorDrawable) — sem dependência externa de ícones.

**Fontes de ícones recomendadas**:
- [Meteocons](https://github.com/basmilius/weather-icons) — licença MIT, 300+ ícones meteorológicos, exportação SVG
- Converter para VectorDrawable via Android Studio

**Rationale**:
- VectorDrawable escala sem perda para qualquer densidade de tela
- Zero impacto no APK size (vs PNG em múltiplas densidades)
- Suporte a animação via AnimatedVectorDrawable (futuro)
- Licença MIT compatível com app comercial

**Alternatives considered**:
- Emoji (☀️🌧️): Aparência inconsistente entre versões Android
- Lottie animations: Overhead de ~500KB de lib + arquivos JSON; recomendado para v1.1

---

## Decisão 7: Estratégia de Cache Room

**Contexto**: RF-06 — cache 1 hora, limpeza automática 7 dias, offline-first.

**Decision**: Chave primária `"lat,lon"` arredondado a 2 casas decimais + JSON blob completo.

**Rationale**:
- Arredondar lat/lon a 2 casas (precisão ~1km) agrupa buscas próximas → menos duplicatas
- JSON blob completo como coluna `dadosJson TEXT` → flexível para futuras versões sem migrations
- Campos escalares adicionais (`tempAtual`, `codigoWMO`) para queries rápidas sem deserialização

**Schema final**:
```sql
CREATE TABLE previsoes (
    id TEXT PRIMARY KEY,                 -- "lat_2decimais,lon_2decimais"
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    nome_localidade TEXT NOT NULL,
    temp_atual REAL NOT NULL,
    codigo_wmo INTEGER NOT NULL,
    dados_json TEXT NOT NULL,            -- JSON completo para offline
    timestamp_atualizado INTEGER NOT NULL, -- epoch millis
    criado_em INTEGER NOT NULL           -- epoch millis
);

CREATE INDEX idx_timestamp ON previsoes(timestamp_atualizado);
```

**Limpeza automática** via `WorkManager` (periódica, não bloqueia UI):
```kotlin
@Query("DELETE FROM previsoes WHERE timestamp_atualizado < :limite")
suspend fun limparExpirados(limite: Long)
```

---

## Resumo de Decisões

| # | Área | Decisão |
|---|------|---------|
| 1 | Gráfico horário | Vico 1.13.0 (Compose-native) |
| 2 | Geocoding URL | geocoding-api.open-meteo.com/v1/search |
| 3 | WMO mapping | Kotlin object Map compilado |
| 4 | Localização | FusedLocationProvider lastLocation + requestUpdates(max=1) |
| 5 | Offline detection | ConnectivityManager.NetworkCallback + StateFlow |
| 6 | Ícones WMO | VectorDrawable (Meteocons MIT) |
| 7 | Cache schema | lat/lon 2 casas decimais + JSON blob |
