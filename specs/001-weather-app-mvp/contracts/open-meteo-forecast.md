# Contrato: Open-Meteo Forecast API

**Versão**: v1 | **Base URL**: `https://api.open-meteo.com`

---

## Endpoint: GET /v1/forecast

Retorna condições atuais + previsão horária + previsão diária para uma coordenada.

### Request

```
GET https://api.open-meteo.com/v1/forecast
```

**Query Parameters obrigatórios**:

| Parâmetro | Tipo | Obrigatório | Exemplo | Restrições |
|-----------|------|-------------|---------|------------|
| `latitude` | Float | Sim | `-23.5505` | -90.0 a 90.0 |
| `longitude` | Float | Sim | `-46.6333` | -180.0 a 180.0 |

**Query Parameters do MVP (todos juntos em uma única requisição)**:

| Parâmetro | Valor fixo no MVP | Descrição |
|-----------|-------------------|-----------|
| `current` | `temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m` | Campos atuais |
| `hourly` | `temperature_2m,precipitation,weather_code` | Campos horários |
| `daily` | `temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,wind_speed_10m_max` | Campos diários |
| `timezone` | `auto` | Detecta automático pelo lat/lon |
| `forecast_days` | `7` | 7 dias de previsão |
| `wind_speed_unit` | `kmh` | km/h (padrão) |

**URL completa do MVP**:
```
GET https://api.open-meteo.com/v1/forecast?latitude=-23.5505&longitude=-46.6333&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,precipitation,weather_code&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,wind_speed_10m_max&timezone=auto&forecast_days=7
```

### Response — 200 OK

```json
{
  "latitude": -23.5505,
  "longitude": -46.6333,
  "elevation": 760.0,
  "generationtime_ms": 25.3,
  "utc_offset_seconds": -10800,
  "timezone": "America/Sao_Paulo",
  "timezone_abbreviation": "BRT",
  "current": {
    "time": "2026-05-17T14:30",
    "temperature_2m": 24.0,
    "relative_humidity_2m": 65,
    "apparent_temperature": 22.1,
    "weather_code": 1,
    "wind_speed_10m": 15.2,
    "wind_direction_10m": 270
  },
  "current_units": {
    "time": "iso8601",
    "temperature_2m": "°C",
    "relative_humidity_2m": "%",
    "apparent_temperature": "°C",
    "weather_code": "wmo code",
    "wind_speed_10m": "km/h",
    "wind_direction_10m": "°"
  },
  "hourly": {
    "time": ["2026-05-17T00:00", "2026-05-17T01:00", "..."],
    "temperature_2m": [20.5, 20.2, 19.8, "..."],
    "precipitation": [0.0, 0.0, 0.1, "..."],
    "weather_code": [0, 0, 1, "..."]
  },
  "hourly_units": {
    "time": "iso8601",
    "temperature_2m": "°C",
    "precipitation": "mm",
    "weather_code": "wmo code"
  },
  "daily": {
    "time": ["2026-05-17", "2026-05-18", "2026-05-19", "2026-05-20", "2026-05-21", "2026-05-22", "2026-05-23"],
    "temperature_2m_max": [27.5, 26.2, 24.1, 25.8, 28.0, 27.3, 26.9],
    "temperature_2m_min": [18.2, 17.8, 16.5, 17.0, 18.5, 17.9, 17.2],
    "weather_code": [1, 3, 61, 80, 0, 2, 1],
    "precipitation_probability_max": [10, 20, 60, 40, 5, 15, 10],
    "wind_speed_10m_max": [18.5, 20.2, 22.1, 19.8, 16.3, 17.5, 18.0]
  },
  "daily_units": {
    "time": "iso8601",
    "temperature_2m_max": "°C",
    "temperature_2m_min": "°C",
    "weather_code": "wmo code",
    "precipitation_probability_max": "%",
    "wind_speed_10m_max": "km/h"
  }
}
```

### Response — Erro (4xx/5xx)

```json
{
  "error": true,
  "reason": "Cannot initialize WeatherVariable TEMPERATURE_2M: Coordinates 200, 200 are outside the allowed range."
}
```

**Códigos de erro**:
| HTTP Status | Significado | Tratamento no App |
|-------------|-------------|-------------------|
| 400 | Coordenadas inválidas | Toast "Coordenadas inválidas. Use busca manual." |
| 429 | Rate limit atingido | Toast "Muitas requisições. Aguarde 1 minuto." + cooldown |
| 500/503 | Servidor indisponível | Toast "Serviço temporariamente indisponível." + usar cache |
| Timeout | Sem resposta em 10s | Toast "Conexão lenta. Usando dados em cache." |

---

## Configuração Retrofit no MVP

```kotlin
// NetworkModule.kt (Hilt)
@Provides
@Singleton
fun provideOpenMeteoApi(client: OkHttpClient): OpenMeteoApi {
    return Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenMeteoApi::class.java)
}

@Provides
@Singleton
fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
}
```

```kotlin
// OpenMeteoApi.kt (Retrofit interface)
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun obterPrevisao(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh"
    ): PrevisaoResponseDto
    
    companion object {
        const val CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m"
        const val HOURLY_FIELDS = "temperature_2m,precipitation,weather_code"
        const val DAILY_FIELDS = "temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,wind_speed_10m_max"
    }
}
```

---

## Limites de Uso (Open-Meteo Free Tier)

| Métrica | Limite | Estimativa MVP |
|---------|--------|----------------|
| Requisições/mês | 300.000 | ~1.000 usuários × 30 req/dia = 30.000 ✅ |
| Requisições/minuto | Sem limite documentado | Debounce 500ms protege |
| Rate limit HTTP 429 | Não documentado | Implementar cooldown de 60s |
| Autenticação | Nenhuma | Zero configuração necessária |
| SLA | Best effort (sem garantia) | Cache 1h mitiga downtime |
