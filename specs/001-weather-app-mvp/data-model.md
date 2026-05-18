# Data Model: Weather App Android MVP v1.0

**Gerado por**: `/speckit-plan` | **Data**: 2026-05-17 | **Plano**: [plan.md](plan.md)

---

## Visão Geral das Entidades

```
┌────────────────────────────────────────────────────────────┐
│                     CAMADA DE DADOS                        │
│                                                            │
│  [API Remote]          [Room Local DB]                     │
│                                                            │
│  PrevisaoDto ──────→  PrevisaoEntity  (cache 1h)          │
│  GeocodingDto          HistoricoBuscaEntity (busca)        │
│                                                            │
│  [Domain Layer]                                            │
│                                                            │
│  Previsao (aggregate)                                      │
│    ├── DadosAtuais                                         │
│    ├── DadosHorarios [List<HoraDados>]                    │
│    └── DadosDiarios [List<DiaDados>]                       │
│                                                            │
│  CidadeSugestao (geocoding result)                         │
│  HistoricoBusca (UI model para histórico)                  │
└────────────────────────────────────────────────────────────┘
```

---

## Entidades Room (Persistência Local)

### PrevisaoEntity

**Tabela**: `previsoes`

| Campo | Tipo SQL | Tipo Kotlin | Nullable | Descrição |
|-------|----------|-------------|----------|-----------|
| `id` | TEXT PRIMARY KEY | String | Não | `"lat_2d,lon_2d"` ex: `"-23.55,-46.63"` |
| `latitude` | REAL | Double | Não | Latitude original (precisão completa) |
| `longitude` | REAL | Double | Não | Longitude original (precisão completa) |
| `nome_localidade` | TEXT | String | Não | Ex: "São Paulo, SP" |
| `temp_atual` | REAL | Float | Não | Temperatura atual em °C |
| `sensacao_termica` | REAL | Float | Não | Sensação térmica em °C |
| `umidade` | INTEGER | Int | Não | 0-100 % |
| `velocidade_vento` | REAL | Float | Não | km/h |
| `direcao_vento` | INTEGER | Int | Não | 0-359 graus |
| `codigo_wmo` | INTEGER | Int | Não | WMO weather code 0-99 |
| `dados_json` | TEXT | String | Não | JSON completo da resposta API |
| `timestamp_atualizado` | INTEGER | Long | Não | Epoch millis da última atualização |
| `criado_em` | INTEGER | Long | Não | Epoch millis da criação do registro |

**Índices**:
- `PRIMARY KEY (id)` — lookup por localização
- `INDEX idx_timestamp (timestamp_atualizado)` — queries de expiração

**Regras de validação**:
- `id` nunca null nem vazio
- `latitude` entre -90.0 e 90.0
- `longitude` entre -180.0 e 180.0
- `timestamp_atualizado` > 0
- `codigo_wmo` entre 0 e 99

**Estado de cache**:
- Válido: `timestamp_atualizado >= now - 3600_000` (1 hora)
- Expirado: `timestamp_atualizado < now - 3600_000` (revalidar)
- Obsoleto: `timestamp_atualizado < now - 604_800_000` (7 dias → deletar)

---

### HistoricoBuscaEntity

**Tabela**: `historico_busca`

| Campo | Tipo SQL | Tipo Kotlin | Nullable | Descrição |
|-------|----------|-------------|----------|-----------|
| `id` | INTEGER PRIMARY KEY AUTOINCREMENT | Long | Não | Auto ID |
| `nome_cidade` | TEXT | String | Não | Ex: "São Paulo" |
| `estado` | TEXT | String | Não | Ex: "São Paulo" (admin1) |
| `pais` | TEXT | String | Não | Ex: "Brasil" |
| `latitude` | REAL | Double | Não | Coordenada da cidade |
| `longitude` | REAL | Double | Não | Coordenada da cidade |
| `buscado_em` | INTEGER | Long | Não | Epoch millis da busca |

**Regras de negócio**:
- Máximo 5 entradas no histórico (deletar mais antigas se >5)
- Se cidade já existe no histórico: atualizar `buscado_em` (não duplicar)
- Ordenar por `buscado_em DESC` para exibir mais recente primeiro

---

## Modelos de Domínio (Business Logic Layer)

### Previsao (Aggregate Root)

```kotlin
data class Previsao(
    val latitude: Double,
    val longitude: Double,
    val nomeLocalidade: String,
    val fusoHorario: String,           // Ex: "America/Sao_Paulo"
    val atual: DadosAtuais,
    val horario: DadosHorarios,
    val diario: DadosDiarios,
    val timestampAtualizado: Long      // Epoch millis
)
```

---

### DadosAtuais

```kotlin
data class DadosAtuais(
    val temperaturaC: Float,           // °C, 1 decimal
    val sensacaoTermicaC: Float,       // °C, 1 decimal
    val umidadePercent: Int,           // 0-100
    val velocidadeVentoKmh: Float,     // km/h
    val direcaoVentoGraus: Int,        // 0-359
    val codigoWMO: Int,                // 0-99
    val horaAtualizado: String         // ISO8601 "2026-05-17T14:30"
)

// Funções derivadas (extensões, sem estado):
fun DadosAtuais.direcaoCardinal(): String   // "N", "NE", "E", etc.
fun DadosAtuais.descricaoWMO(): String      // via WmoMapper
fun DadosAtuais.iconeWMO(): Int             // @DrawableRes via WmoMapper
```

**Regras de validação**:
- `temperaturaC` entre -90f e 60f (limites terrestres)
- `umidadePercent` entre 0 e 100
- `direcaoVentoGraus` entre 0 e 359

---

### DadosHorarios

```kotlin
data class DadosHorarios(
    val horas: List<HoraDados>         // Exatamente 24 entradas
)

data class HoraDados(
    val hora: String,                  // "HH:00" formatado PT-BR
    val temperaturaC: Float,
    val precipitacaoMm: Float,         // 0.0+ mm
    val codigoWMO: Int
)
```

**Regras de negócio**:
- Filtrar apenas as 24 horas do dia corrente (não dias futuros)
- `hora` deve ser formatado como "14:00", não timestamp ISO completo

---

### DadosDiarios

```kotlin
data class DadosDiarios(
    val dias: List<DiaDados>           // Exatamente 7 entradas
)

data class DiaDados(
    val data: String,                  // "Sex, 17 mai" (formatado PT-BR)
    val dataIso: String,               // "2026-05-17" (para lookup horário)
    val temperaturMaxC: Float,
    val temperaturMinC: Float,
    val probChuvaPercent: Int,         // 0-100
    val velocidadeMaxVentoKmh: Float,
    val codigoWMO: Int,
    val eHoje: Boolean                 // true para primeiro dia
)
```

---

### CidadeSugestao (Geocoding Result)

```kotlin
data class CidadeSugestao(
    val id: Int,                       // ID Open-Meteo
    val nome: String,                  // "São Paulo"
    val estado: String,                // "São Paulo" (admin1)
    val pais: String,                  // "Brasil"
    val latitude: Double,
    val longitude: Double,
    val populacao: Int?                // Para ordenar resultados (maior pop primeiro)
)

// Display format:
fun CidadeSugestao.displayNome(): String = "$nome, $estado, $pais"
```

---

### HistoricoBusca (UI Model)

```kotlin
data class HistoricoBusca(
    val id: Long,
    val displayNome: String,           // "São Paulo, SP, Brasil"
    val latitude: Double,
    val longitude: Double
)
```

---

## Estados de UI (StateFlow)

### HomeUiState

```kotlin
sealed class HomeUiState {
    object Carregando : HomeUiState()
    
    data class Sucesso(
        val previsao: Previsao,
        val nomeLocalidade: String,
        val isLocalizacaoAproximada: Boolean = false,
        val isOffline: Boolean = false,
        val timestampRelativo: String          // "Atualizado há 5 minutos"
    ) : HomeUiState()
    
    data class SucessoOffline(
        val previsao: Previsao,
        val nomeLocalidade: String,
        val timestampRelativo: String,
        val horasAtraso: Int
    ) : HomeUiState()
    
    data class Erro(
        val mensagem: String,
        val temCache: Boolean = false,
        val previsaoCache: Previsao? = null
    ) : HomeUiState()
    
    object SemPermissao : HomeUiState()
}
```

### SearchUiState

```kotlin
sealed class SearchUiState {
    object Idle : SearchUiState()
    object Carregando : SearchUiState()
    
    data class Resultados(
        val sugestoes: List<CidadeSugestao>,
        val historico: List<HistoricoBusca>
    ) : SearchUiState()
    
    data class Erro(val mensagem: String) : SearchUiState()
}
```

---

## DTOs de API (Data Transfer Objects)

### PrevisaoResponseDto (Open-Meteo Forecast)

```kotlin
@Serializable
data class PrevisaoResponseDto(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int,
    val timezone: String,
    val current: DadosAtuaisDto,
    @SerialName("current_units") val currentUnits: UnidadesDto,
    val hourly: DadosHorariosDto,
    @SerialName("hourly_units") val hourlyUnits: UnidadesDto,
    val daily: DadosDiariosDto,
    @SerialName("daily_units") val dailyUnits: UnidadesDto
)

@Serializable
data class DadosAtuaisDto(
    @SerialName("temperature_2m") val temperature2m: Float,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int,
    @SerialName("apparent_temperature") val apparentTemperature: Float,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed10m: Float,
    @SerialName("wind_direction_10m") val windDirection10m: Int,
    val time: String
)

@Serializable
data class DadosHorariosDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature2m: List<Float>,
    val precipitation: List<Float>,
    @SerialName("weather_code") val weatherCode: List<Int>
)

@Serializable
data class DadosDiariosDto(
    val time: List<String>,
    @SerialName("temperature_2m_max") val temperature2mMax: List<Float>,
    @SerialName("temperature_2m_min") val temperature2mMin: List<Float>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>,
    @SerialName("wind_speed_10m_max") val windSpeed10mMax: List<Float>
)
```

### GeocodingResponseDto (Open-Meteo Geocoding)

```kotlin
@Serializable
data class GeocodingResponseDto(
    val results: List<CidadeDto>? = null,
    @SerialName("generationtime_ms") val generationtimeMs: Float
)

@Serializable
data class CidadeDto(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    @SerialName("country_code") val countryCode: String,
    val admin1: String? = null,
    val country: String,
    val population: Int? = null,
    val timezone: String? = null
)
```

---

## Mappers (Domain ↔ DTO ↔ Entity)

```
PrevisaoResponseDto ──mapper──→ Previsao (domain)
Previsao (domain) ──mapper──→ PrevisaoEntity (room)
PrevisaoEntity (room) ──mapper──→ Previsao (domain)

CidadeDto ──mapper──→ CidadeSugestao (domain)
HistoricoBuscaEntity ──mapper──→ HistoricoBusca (domain)
HistoricoBusca (domain) ──mapper──→ HistoricoBuscaEntity (room)
```

**Regra**: Mappers são funções puras de extensão — sem side effects, sem injeção.

---

## Diagrama de Relacionamentos

```
PrevisaoEntity (Room)
    id: "lat,lon" ─────── (1 por localização ativa)
    dados_json: TEXT ──── (snapshot completo para offline)
    
HistoricoBuscaEntity (Room)
    id: autoincrement ─── (até 5 entradas)
    
[SEM FK entre tabelas] — entidades independentes no MVP v1.0
```
