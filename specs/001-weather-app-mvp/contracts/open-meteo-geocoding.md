# Contrato: Open-Meteo Geocoding API

**Versão**: v1 | **Base URL**: `https://geocoding-api.open-meteo.com`

> **IMPORTANTE**: O geocoding usa um domínio diferente do forecast. URL correta: `geocoding-api.open-meteo.com`, não `api.open-meteo.com`.

---

## Endpoint: GET /v1/search

Busca cidades pelo nome. Retorna até `count` resultados com coordenadas.

### Request

```
GET https://geocoding-api.open-meteo.com/v1/search
```

**Query Parameters**:

| Parâmetro | Tipo | Obrigatório | Exemplo | Restrições |
|-----------|------|-------------|---------|------------|
| `name` | String | Sim | `São Paulo` | Mínimo 2 caracteres |
| `count` | Integer | Não | `5` | Default: 10, Max: 100 |
| `language` | String | Não | `pt` | ISO 639-1 (pt, en, de, fr...) |
| `format` | String | Não | `json` | Default: json |

**URL usada no MVP** (debounce 500ms antes de chamar):
```
GET https://geocoding-api.open-meteo.com/v1/search?name=São%20Paulo&count=5&language=pt&format=json
```

### Response — 200 OK (com resultados)

```json
{
  "results": [
    {
      "id": 3448439,
      "name": "São Paulo",
      "latitude": -23.5505,
      "longitude": -46.6333,
      "elevation": 760.0,
      "feature_code": "PPLA",
      "country_code": "BR",
      "admin1_id": 3469034,
      "admin1": "São Paulo",
      "timezone": "America/Sao_Paulo",
      "population": 12252023,
      "country_id": 3469034,
      "country": "Brasil"
    },
    {
      "id": 6322752,
      "name": "São Paulo",
      "latitude": -23.1325,
      "longitude": -46.2233,
      "elevation": 640.0,
      "feature_code": "PPLA3",
      "country_code": "BR",
      "admin1_id": 3455077,
      "admin1": "Paraná",
      "timezone": "America/Sao_Paulo",
      "population": 45000,
      "country_id": 3469034,
      "country": "Brasil"
    }
  ],
  "generationtime_ms": 2.5
}
```

### Response — 200 OK (sem resultados)

```json
{
  "generationtime_ms": 1.2
}
```

> Nota: campo `results` ausente (não `null`, não `[]`). O app deve tratar `results?.orEmpty()`.

### Response — Erro (4xx)

```json
{
  "error": true,
  "reason": "query_too_short"
}
```

**Códigos de erro**:
| Reason | Causa | Tratamento |
|--------|-------|------------|
| `query_too_short` | Nome com < 2 chars | Não chamar API até ≥ 2 chars |
| HTTP 429 | Rate limit | Toast + cooldown 60s |
| HTTP 500 | Servidor fora | Toast "Busca indisponível. Tente novamente." |

---

## Configuração Retrofit no MVP

```kotlin
// NetworkModule.kt (Hilt) — segundo Retrofit instance
@Provides
@Singleton
@GeocodingClient
fun provideGeocodingApi(client: OkHttpClient): GeocodingApi {
    return Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(client)
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GeocodingApi::class.java)
}
```

```kotlin
// GeocodingApi.kt (Retrofit interface)
interface GeocodingApi {
    @GET("v1/search")
    suspend fun buscarCidades(
        @Query("name") nome: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "pt",
        @Query("format") format: String = "json"
    ): GeocodingResponseDto
}
```

---

## Regras de Negócio para Busca

1. **Debounce 500ms**: Só chamar API 500ms após última keystroke
2. **Mínimo 2 chars**: Não chamar se `input.length < 2`
3. **Cancelar requisição anterior**: Usar `Job` em `SearchViewModel` para cancelar busca em andamento quando novo texto é digitado
4. **Ordenação de resultados**: Exibir na ordem retornada pela API (já ordenada por população)
5. **Máximo 5 resultados**: `count=5` fixo no MVP

```kotlin
// SearchViewModel.kt — debounce + cancel
private var searchJob: Job? = null

fun buscar(query: String) {
    searchJob?.cancel()
    if (query.length < 2) {
        _state.value = SearchUiState.Idle
        return
    }
    searchJob = viewModelScope.launch {
        delay(500) // debounce
        _state.value = SearchUiState.Carregando
        try {
            val resultados = buscarCidadesUseCase(query)
            _state.value = SearchUiState.Resultados(resultados, historico.value)
        } catch (e: Exception) {
            _state.value = SearchUiState.Erro("Erro ao buscar cidades")
        }
    }
}
```
