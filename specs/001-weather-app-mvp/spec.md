# Feature Specification: Weather App Android MVP v1.0

**Documento**: spec.md  
**Data**: 17 de maio de 2026  
**Versão**: 1.0  
**Status**: 🟢 PRONTO PARA IMPLEMENTAÇÃO  
**Story Points**: ~35 SP  
**Timeline**: 5-6 semanas  

---

## 1. VISÃO GERAL

### Declaração da Feature

Implementar primeira versão (MVP) de aplicativo Android nativo para previsão de tempo, oferecendo:
- Condições meteorológicas atuais detalhadas
- Previsão horária para 24 horas
- Previsão diária para 7 dias
- Localização automática (Network + GPS híbrido)
- Sincronização de dados com cache local

**Objetivo Estratégico**: Lançar app funcional, rápido e offline-ready no Android para usuários brasileiros, com foco em Mobile-First UX e performance.

### Contexto

- **Fonte de dados**: Open-Meteo API (gratuita, sem autenticação)
- **Plataforma**: Android 8.0+ (API 24+)
- **Linguagem**: Kotlin 100%
- **Arquitetura**: MVVM + Hilt + Coroutines
- **Persistência**: Room Database SQLite
- **UI**: Jetpack Compose + Material Design 3
- **Conformidade**: Constitution v1.1.0 (Android Mobile-First)

### Escopo Incluso (v1.0 MVP)

| # | Feature | Prioridade | Estimado |
|---|---------|-----------|----------|
| 1 | Previsão Atual Detalhada | CRÍTICA | 3 SP |
| 2 | Previsão Horária (24h) | CRÍTICA | 5 SP |
| 3 | Previsão 7 Dias | CRÍTICA | 3 SP |
| 4 | Localização Híbrida | CRÍTICA | 5 SP |
| 5 | Sincronização + Cache | CRÍTICA | 4 SP |
| 6 | Infraestrutura (Gradle, Hilt, Room) | SUPORTE | 8 SP |
| 7 | Testes (Unitários + Instrumentados) | OBRIGATÓRIO | 10 SP |

**TOTAL**: 38 SP (com buffer) → ~35-36 SP efetivo

### Escopo Excluído (v1.1+)

- Cidades Favoritas/Salvadas
- Alertas de tempestade
- Radar de chuva
- Poluição do ar
- Histórico climático
- Múltiplos idiomas (apenas PT-BR)
- Widgets
- Compartilhamento de previsão
- Notificações push

---

## 2. HISTÓRIAS DE USUÁRIO & CENÁRIOS

### Persona Primária: João (Usuário Regular)

```
Nome: João Silva
Idade: 32 anos
Profissão: Analista de Sistemas
Dispositivo: Motorola G13, Android 12
Necessidade: Verificar rápido a previsão do tempo para São Paulo antes de sair de casa
Frequência: 2-3x por dia
Pain Point: Apps lentos, gastam bateria
Ganho: App leve, rápido, offline
```

### Cenário 1: Visualizar Previsão Atual (Happy Path)

```gherkin
Cenário: Usuário abre app e vê previsão da sua localização

GIVEN: App é aberto pela primeira vez
AND: Permissão de localização foi concedida
AND: Dispositivo tem conectividade

WHEN: App inicializa
THEN: Em até 2 segundos, deve exibir:
  - Localização atual (ex: "São Paulo, SP")
  - Temperatura atual (ex: "24°C")
  - Descrição WMO (ex: "Parcialmente Nublado")
  - Sensação térmica (ex: "22°C")
  - Umidade (ex: "65%")
  - Velocidade do vento (ex: "15 km/h")
  - Timestamp de atualização (ex: "Atualizado há 1m")

AND: Usuário vê ícone WMO mapeado (☀️, 🌤️, ⛅, 🌧️, etc)
AND: Botão de refresh está acessível

GIVEN: Dados são mais antigos que 1 hora
WHEN: Usuário puxa tela para baixo (pull-to-refresh)
THEN: App requisita novos dados da API
AND: Spinner loading é exibido
AND: Dados são atualizados
AND: Timestamp muda para "Atualizado há <1m"
```

### Cenário 2: Visualizar Previsão Horária

```gherkin
Cenário: Usuário consulta temperatura e precipitação hora-a-hora

GIVEN: Usuário está na tela principal
WHEN: Usuário scroll para baixo até "Previsão Horária"
THEN: Deve exibir dois tabs:
  1. "Gráfico" - linha/área com temperatura (vermelho) e precipitação (azul)
  2. "Listagem" - cards horizontais com hora, temp, precipitação

GIVEN: Modo "Gráfico"
THEN: Deve mostrar:
  - Eixo X: horas (00:00, 02:00, 04:00, ..., 22:00)
  - Eixo Y: temperatura (em °C)
  - Linha vermelha: temperatura por hora
  - Área azul: precipitação (mm)
  - Ponto destacado: "Agora"

GIVEN: Modo "Listagem"
THEN: Deve exibir cards horizontais (scroll left/right) com:
  - Hora (ex: "14:00")
  - Ícone WMO
  - Temperatura (ex: "24°C")
  - Precipitação (ex: "0 mm" ou "5 mm")

GIVEN: Usuário tap em um card
WHEN: Tempo decorrido < 1 segundo
THEN: Bottom sheet abre mostrando detalhe daquela hora:
  - Hora
  - Temperatura
  - Umidade
  - Velocidade do vento
  - Direção do vento
  - Descrição WMO completa
```

### Cenário 3: Visualizar Previsão de 7 Dias

```gherkin
Cenário: Usuário verifica forecast semanal

GIVEN: Usuário na tela principal
WHEN: Scroll para seção "Próximos 7 Dias"
THEN: Exibe 7 cards verticais (um por linha):
  - Dia da semana + data (ex: "Sex 17 de Maio")
  - Ícone WMO (baseado em weather_code do dia)
  - Descrição textual (ex: "Parcialmente Nublado")
  - Temperatura máxima (ex: "27°C") em vermelho
  - Temperatura mínima (ex: "18°C") em azul
  - Probabilidade de chuva (ex: "10%")
  - Velocidade máxima do vento (ex: "12 km/h")

GIVEN: Primeiro card é hoje
THEN: Destaque visual diferente (cor de fundo ou badge "HOJE")

GIVEN: Usuário tap em um card
WHEN: Tempo decorrido < 1 segundo
THEN: Modal abre exibindo:
  - Data + ícone WMO grande
  - Temperatura máxima/mínima
  - Previsão horária para aquele dia (tab)
  - Índices (umidade, vento, etc)
```

### Cenário 4: Localização Automática (Híbrida)

```gherkin
Cenário: App localiza usuário rapidamente com refinement em background

GIVEN: App é aberto
AND: Permissão de localização foi concedida anteriormente
AND: Dispositivo tem conectividade

WHEN: App inicia
THEN: Em até 500ms:
  1. Network Provider (WiFi/LTE) retorna localização aproximada
  2. App requisita previsão Open-Meteo com essas coordenadas
  3. Exibe previsão com badge: "Localização aproximada - refinando..."

AND: Em paralelo (background):
  1. GPS Provider começa a processar (5-30s)
  2. Quando GPS retorna (mais preciso):
     - App requisita previsão atualizada
     - Badge desaparece
     - Previsão atualiza silenciosamente (sem jarring)

GIVEN: Permissão NÃO foi concedida
WHEN: App abre
THEN: Exibe campo de busca com placeholder:
  "Buscar cidade... (ex: São Paulo)"
AND: Botão para solicitar permissão novamente
```

### Cenário 5: Busca de Cidade Manual

```gherkin
Cenário: Usuário digita nome da cidade e seleciona nos resultados

GIVEN: Usuário clica no campo de busca
WHEN: Campo fica em foco
THEN: Exibe:
  - Histórico de 3-5 últimas buscas
  - Placeholder: "Buscar cidade..."

GIVEN: Usuário digita "São Paulo"
WHEN: Aguarda 500ms (debounce)
THEN: Requisita /v1/geocoding?name=São%20Paulo&count=5&language=pt
AND: Exibe 5 primeiros resultados:
  1. "São Paulo, São Paulo, Brasil" (-23.55, -46.63)
  2. "São Paulo, Paraná, Brasil" (-23.13, ...)
  3. Etc

GIVEN: Usuário clica em resultado
WHEN: Requisição API completa
THEN: App muda localização principal
AND: Previsão é atualizada
AND: Histórico registra a busca
```

### Cenário 6: Offline Fallback

```gherkin
Cenário: Usuário abre app sem conectividade

GIVEN: App já foi aberto antes (dados em cache)
AND: Cache tem menos de 24 horas
AND: Dispositivo está OFFLINE

WHEN: Usuário abre app
THEN: Exibe dados do cache
AND: Badge vermelha 🔴 "OFFLINE" no topo
AND: Mensagem: "Usando dados em cache de há X horas"

GIVEN: Usuário tenta pull-to-refresh
WHEN: Sem conectividade
THEN: Exibe toast:
  "Sem conexão. Usando dados em cache."
AND: Badge permanece visível

GIVEN: Usuário reconecta
WHEN: App detecta conectividade
THEN: Requisita dados frescos automaticamente
AND: Badge desaparece quando dados são sincronizados
```

---

## 3. REQUISITOS FUNCIONAIS

### RF-01: Previsão Atual Detalhada

**Descrição**: Exibir condições meteorológicas atuais em card destaque

**Requisitos**:
- [ ] RF-01.1: Exibir temperatura atual em °C (fonte: `current.temperature_2m`)
- [ ] RF-01.2: Exibir ícone WMO mapeado (fonte: `current.weather_code`)
- [ ] RF-01.3: Exibir descrição textual (ex: "Parcialmente Nublado")
- [ ] RF-01.4: Exibir sensação térmica (fonte: `current.apparent_temperature`)
- [ ] RF-01.5: Exibir umidade relativa (fonte: `current.relative_humidity_2m`)
- [ ] RF-01.6: Exibir velocidade do vento (fonte: `current.wind_speed_10m`)
- [ ] RF-01.7: Exibir direção do vento em graus e cardinal (N, NE, E, etc)
- [ ] RF-01.8: Exibir timestamp de atualização em formato relativo ("Atualizado há Xm")
- [ ] RF-01.9: Card deve ocupar ~60% da altura da tela (priority content)
- [ ] RF-01.10: Ícone WMO deve ter tamanho ≥ 80dp

**Critérios de Aceitação**:
- Valores exibidos correspondem exatamente aos retornados pela API
- Atualização reflete mudanças quando pull-to-refresh é acionado
- Timestamp atualiza a cada minuto automaticamente (mesmo sem requerer API)
- Card é acessível via TalkBack (descreve todos os valores)

---

### RF-02: Previsão Horária (24 horas)

**Descrição**: Gráfico + lista de temperatura e precipitação hora-a-hora

**Requisitos**:
- [ ] RF-02.1: Exibir gráfico de linha com temperatura (vermelho)
- [ ] RF-02.2: Exibir área de precipitação acumulada (azul com transparência)
- [ ] RF-02.3: Eixo X = horas (00:00, 02:00, 04:00, ..., 23:00)
- [ ] RF-02.4: Eixo Y = temperatura em °C (auto-scale baseado em min/max)
- [ ] RF-02.5: Destacar ponto "Agora" no gráfico (linha tracejada vertical)
- [ ] RF-02.6: Tab alternativo com listagem horizontal de cards
- [ ] RF-02.7: Cada card lista: hora, ícone WMO, temperatura, precipitação
- [ ] RF-02.8: Scroll automático para "Agora" quando aba abre
- [ ] RF-02.9: Tap em hora abre bottom sheet com detalhes completos
- [ ] RF-02.10: Renderizar apenas 24 horas (não mais)

**Fontes de Dados**:
- `hourly.time` (timestamps)
- `hourly.temperature_2m` (24 valores)
- `hourly.precipitation` (precipitação mm)
- `hourly.weather_code` (para ícone)

**Critérios de Aceitação**:
- Gráfico renderiza sem glitches até 24 pontos
- Escala Y acomoda valores válidos (ex: -10°C a 40°C)
- Scroll horizontal suave sem travamento
- Tap em card tem latência ≤ 100ms

---

### RF-03: Previsão de 7 Dias

**Descrição**: Cards com previsão diária (máx/mín, condições, índices)

**Requisitos**:
- [ ] RF-03.1: Exibir 7 cards (um por linha) em scroll vertical
- [ ] RF-03.2: Cada card mostra:
  - Dia da semana + data (ex: "Sex 17 de Maio")
  - Ícone WMO (baseado em `daily.weather_code`)
  - Descrição WMO (ex: "Parcialmente Nublado")
  - Temperatura máxima (vermelho, fonte: `daily.temperature_2m_max`)
  - Temperatura mínima (azul, fonte: `daily.temperature_2m_min`)
  - Probabilidade máxima de chuva (fonte: `daily.precipitation_probability_max`)
  - Velocidade máxima do vento (fonte: `daily.wind_speed_10m_max`)
- [ ] RF-03.3: Primeiro card (hoje) tem destaque visual (badge "HOJE" ou cor especial)
- [ ] RF-03.4: Tap em card abre modal com:
  - Data grande + ícone grande
  - Previsão horária para aquele dia
  - Índices adicionais (umidade, vento, direção)
- [ ] RF-03.5: Swipe left/right na modal navega entre dias
- [ ] RF-03.6: Cards ocupam full-width da tela

**Critérios de Aceitação**:
- 7 linhas renderizam sem scroll lag
- Cores de temperatura máx/mín são distintas e Material Design 3
- Modal abre em ≤ 300ms
- Navegação left/right funciona sem pular dias

---

### RF-04: Localização (Híbrida Network + GPS)

**Descrição**: Inicializar app com localização rápida (Network) refinada por GPS em background

**Requisitos**:
- [ ] RF-04.1: Solicitar permissão `ACCESS_FINE_LOCATION` on first run
- [ ] RF-04.2: Se permitido:
  - Obter coordenadas via Network Provider (0-2s)
  - Requisitar previsão Open-Meteo
  - Exibir resultados com badge: "Localização aproximada - refinando..."
  - Em background: ativar GPS para refinement (5-30s)
  - Quando GPS retorna: atualizar previsão silenciosamente
- [ ] RF-04.3: Se negado:
  - Exibir campo de busca
  - Botão para solicitar permissão novamente
- [ ] RF-04.4: Se Network falha:
  - Mostrar toast: "Erro ao localizar. Use busca manual."
  - Exibir campo de busca

**Requisitos de Performance**:
- [ ] RF-04.5: Startup com dados Network ≤ 2 segundos (Constitution)
- [ ] RF-04.6: GPS refinement não bloqueia UI
- [ ] RF-04.7: Badge desaparece assim que GPS retorna

**Critérios de Aceitação**:
- App abre e exibe dados em < 2s (mesmo com Network lento)
- Badge "refinando..." aparece quando Network rápido
- Badge desaparece quando GPS atualiza (sem refresh user-triggered)
- GPS refinement não causa flicker

---

### RF-05: Busca Manual de Cidades

**Descrição**: Campo de busca com autocomplete via Open-Meteo `/v1/geocoding`

**Requisitos**:
- [ ] RF-05.1: Campo de busca sempre acessível no topo (search bar)
- [ ] RF-05.2: Placeholder: "Buscar cidade..."
- [ ] RF-05.3: Quando campo recebe foco:
  - Exibir últimas 3-5 buscas do histórico
  - Limpar placeholder
- [ ] RF-05.4: Usuário digita, após 500ms (debounce):
  - Requisitar `/v1/geocoding?name={input}&count=5&language=pt`
  - Exibir até 5 resultados
- [ ] RF-05.5: Cada resultado mostra:
  - Nome da cidade
  - Estado/região
  - País
- [ ] RF-05.6: Tap em resultado:
  - Fechar teclado
  - Requisitar previsão para aquelas coordenadas
  - Adicionar ao histórico
  - Atualizar tela principal
- [ ] RF-05.7: Historico persiste entre sessões (local storage)

**Requisitos de Performance**:
- [ ] RF-05.8: Debounce deve ser exatamente 500ms
- [ ] RF-05.9: Requisição geocoding < 2s
- [ ] RF-05.10: Resultados aparecem sem animação (instant)

**Critérios de Aceitação**:
- Histórico salva corretamente entre restarts
- Debounce funciona (não chama API a cada keystroke)
- Geocoding retorna resultados corretos para "São Paulo", "Rio de Janeiro", etc
- Campo de busca é acessível via TalkBack

---

### RF-06: Sincronização + Cache Local (Room)

**Descrição**: Carregar dados da API, cachear em Room, offline fallback

**Requisitos**:
- [ ] RF-06.1: Primeira requisição salva em Room Database
- [ ] RF-06.2: Cache de 1 hora:
  - Se última atualização < 1h: usar cache sem requisitar API
  - Se última atualização > 1h: requisitar API em background
- [ ] RF-06.3: Pull-to-refresh força requisição API (ignora cache)
- [ ] RF-06.4: Se offline:
  - Usar cache mesmo que expirado
  - Exibir badge 🔴 "OFFLINE"
  - Mensagem: "Usando dados de há X horas"
- [ ] RF-06.5: Se erro na API:
  - Tentar carregar cache (mesmo expirado)
  - Se nenhum cache: exibir erro com botão "Tentar novamente"
- [ ] RF-06.6: Limpeza automática de dados > 7 dias (background)
- [ ] RF-06.7: Cache armazena:
  - Latitude, longitude
  - Dados atuais completos (JSON)
  - Dados horários completos (JSON)
  - Dados diários completos (JSON)
  - Timestamp de atualização

**Schema Room**:
```sql
CREATE TABLE previsoes (
  id TEXT PRIMARY KEY,              -- latitude,longitude
  latitude REAL NOT NULL,
  longitude REAL NOT NULL,
  dados_json TEXT NOT NULL,         -- JSON completo da API
  timestamp_atualizado INTEGER,     -- milliseconds since epoch
  criado_em INTEGER
);
```

**Critérios de Aceitação**:
- App funciona offline com dados em cache
- Badge offline desaparece quando reconecta
- Cache expirado é revalidado quando online
- Dados > 7 dias são deletados automaticamente
- Timestamp relativo atualiza a cada minuto

---

### RF-07: Pull-to-Refresh

**Descrição**: Gesto para atualizar dados manualmente

**Requisitos**:
- [ ] RF-07.1: Swipe down from top exibe spinner
- [ ] RF-07.2: Requisita dados frescos (ignora cache 1h)
- [ ] RF-07.3: Spinner exibe enquanto carrega
- [ ] RF-07.4: Após sucesso, anima spinner para fora
- [ ] RF-07.5: Timestamp atualiza para "Atualizado há <1m"
- [ ] RF-07.6: Se erro, exibe toast e spinner sai

**Performance**:
- [ ] RF-07.7: Spin duration ≤ 500ms (mesmo se API lenta, min)
- [ ] RF-07.8: UI não bloqueia durante requisição

**Critérios de Aceitação**:
- Gesto pull-down 100% recognized (90°)
- Spinner visual é Material Design 3
- Requisição ocorre uma única vez por pull
- Tap em spinner para cancelar (opcional, nice-to-have)

---

## 4. REQUISITOS NÃO-FUNCIONAIS

### RNF-01: Performance

- **Startup (cold start)**: ≤ 2 segundos
- **Startup (warm start)**: ≤ 500ms
- **Memória (idle)**: ≤ 50MB
- **APK size**: ≤ 15MB (release build com ProGuard/R8)
- **Frame rate**: ≥ 50fps em scroll
- **Latência de rede (API call)**: ≤ 500ms (P95)

### RNF-02: Confiabilidade

- **Uptime**: Target 99% (best effort, API Open-Meteo gratuita)
- **Crash rate**: < 0.1%
- **ANR (Application Not Responding)**: < 0.1%
- **Sem memory leaks**: LeakCanary clean em dev

### RNF-03: Segurança

- **HTTPS obrigatório**: Todas as requisições Open-Meteo já são HTTPS
- **Sem credenciais hardcoded**: API é pública, sem auth
- **Permissões mínimas**: `ACCESS_FINE_LOCATION` + `INTERNET`
- **Sem dados sensíveis**: Apenas coordenadas e previsão pública

### RNF-04: Usabilidade (Mobile-First)

- **Responsive**: 100% portrait mode
- **Touch targets**: ≥ 48dp × 48dp
- **Acessibilidade**: TalkBack compatible, WCAG AA contrast
- **Orientação**: Portrait primary (landscape not required for v1.0)
- **Dark mode**: Não obrigatório v1.0 (v1.1)

### RNF-05: Conformidade Android

- **Target API**: 34 (Projeto suporta até API 24)
- **Compilação**: Android Studio 2024.1+
- **Kotlin**: 100% (sem Java novo)
- **Lint**: 0 erros, 0 avisos (Detekt + Android Lint)
- **Testes**: ≥ 70% cobertura ViewModels, ≥ 60% UI

### RNF-06: Escalabilidade

- **Múltiplas cidades**: Suporte a 1 cidade principal + cache (v1.1: favoritos)
- **Taxa de atualização**: 1 requisição/hora por localização
- **Open-Meteo limits**: 300k chamadas/mês (gratuita) - OK para v1.0

---

## 5. ESPECIFICAÇÃO TÉCNICA

### 5.1 Modelo de Dados

#### Entity: PrevisaoCache (Room)

```kotlin
@Entity(tableName = "previsoes")
@Serializable
data class PrevisaoCache(
    @PrimaryKey
    val id: String,  // Format: "latitude,longitude"
    
    val latitude: Double,
    val longitude: Double,
    val nomeLocalidade: String,  // Ex: "São Paulo"
    
    // Dados Atuais
    val tempAtual: Float,        // °C
    val sensacaoTermica: Float,  // °C
    val umidade: Int,            // 0-100 %
    val velocidadeVento: Float,  // km/h
    val direcaoVento: Int,       // 0-359 graus
    val codigoWMO: Int,          // 0-99 (WMO codes)
    
    // Timestamps
    val tempoAtualizado: Long,   // millis since epoch
    val criadoEm: Long,          // millis since epoch
    
    // JSON Completo (backup para offline)
    val dadosJsonCompleto: String  // Serialized full API response
)
```

#### Entity: HistoricoBusca (Room)

```kotlin
@Entity(tableName = "historico_busca")
data class HistoricoBusca(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val nomeCidade: String,
    val estado: String,
    val pais: String,
    val latitude: Double,
    val longitude: Double,
    val buscadoEm: Long  // millis
)
```

#### Data Model: Previsao (Domain)

```kotlin
@Serializable
data class Previsao(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val generationtimeMs: Float,
    val utcOffsetSeconds: Int,
    val timezone: String,
    val timezoneAbbreviation: String,
    
    val current: DadosAtuais,
    val currentUnits: UnidadesAtuais,
    
    val hourly: DadosHorarios,
    val hourlyUnits: UnidadesHorarias,
    
    val daily: DadosDiarios,
    val dailyUnits: UnidadesDiarias
)

@Serializable
data class DadosAtuais(
    val temperature2m: Float,
    val relativeHumidity2m: Int,
    val apparentTemperature: Float,
    val weatherCode: Int,
    val windSpeed10m: Float,
    val windDirection10m: Int,
    val time: String  // ISO8601
)

@Serializable
data class DadosHorarios(
    val time: List<String>,           // ISO8601 timestamps
    val temperature2m: List<Float>,   // 24 valores
    val precipitation: List<Float>,   // mm
    val weatherCode: List<Int>        // WMO codes
)

@Serializable
data class DadosDiarios(
    val time: List<String>,                    // ISO8601 dates
    val temperature2mMax: List<Float>,         // 7 valores
    val temperature2mMin: List<Float>,         // 7 valores
    val weatherCode: List<Int>,                // WMO codes
    val precipitationProbabilityMax: List<Int>, // 0-100 %
    val windSpeed10mMax: List<Float>           // km/h
)
```

---

### 5.2 Contratos de API

#### Endpoint 1: Open-Meteo Forecast

```
GET https://api.open-meteo.com/v1/forecast

Query Parameters:
  latitude (Float, required)          Ex: -23.5505
  longitude (Float, required)         Ex: -46.6333
  current (String, optional)          "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m"
  hourly (String, optional)           "temperature_2m,precipitation,weather_code"
  daily (String, optional)            "temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,wind_speed_10m_max"
  timezone (String, optional)         "America/Sao_Paulo" (auto-detect)
  forecast_days (Integer, optional)   7 (default)

Response (200 OK):
{
  "latitude": -23.5505,
  "longitude": -46.6333,
  "elevation": 760.0,
  "generationtime_ms": 25.3,
  "utc_offset_seconds": -10800,
  "timezone": "America/Sao_Paulo",
  "timezone_abbreviation": "BRT",
  "current": {
    "temperature_2m": 24.0,
    "relative_humidity_2m": 65,
    "apparent_temperature": 22.1,
    "weather_code": 1,
    "wind_speed_10m": 15.2,
    "wind_direction_10m": 270,
    "time": "2026-05-17T14:30"
  },
  "current_units": {
    "temperature_2m": "°C",
    "relative_humidity_2m": "%",
    "weather_code": "wmo code",
    "wind_speed_10m": "km/h",
    "wind_direction_10m": "°"
  },
  "hourly": {
    "time": ["2026-05-17T00:00", "2026-05-17T01:00", ...],
    "temperature_2m": [20.5, 20.2, 19.8, ...],
    "precipitation": [0.0, 0.0, 0.1, ...],
    "weather_code": [0, 0, 1, ...]
  },
  "hourly_units": {
    "temperature_2m": "°C",
    "precipitation": "mm",
    "weather_code": "wmo code"
  },
  "daily": {
    "time": ["2026-05-17", "2026-05-18", ...],
    "temperature_2m_max": [27.5, 26.2, ...],
    "temperature_2m_min": [18.2, 17.8, ...],
    "weather_code": [1, 3, ...],
    "precipitation_probability_max": [10, 20, ...],
    "wind_speed_10m_max": [18.5, 20.2, ...]
  },
  "daily_units": {
    "temperature_2m_max": "°C",
    "temperature_2m_min": "°C",
    "weather_code": "wmo code",
    "precipitation_probability_max": "%",
    "wind_speed_10m_max": "km/h"
  }
}

Error (4xx/5xx):
{
  "reason": "Invalid coordinates",
  "error": true
}
```

#### Endpoint 2: Open-Meteo Geocoding

```
POST https://api.open-meteo.com/v1/geocoding

Query Parameters:
  name (String, required)       "São Paulo"
  count (Integer, optional)     5 (default)
  language (String, optional)   "pt" (for Portuguese)

Response (200 OK):
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
      "id": 3448440,
      "name": "São Paulo",
      "latitude": -23.13,
      "longitude": -46.22,
      "elevation": 600.0,
      "admin1": "Paraná",
      "country": "Brasil"
    }
  ],
  "generationtime_ms": 2.5
}

Error (4xx):
{
  "error": true,
  "reason": "query_too_short"
}
```

---

### 5.3 Fluxo de Sincronização

```
┌─────────────────────────────────────────────┐
│ Usuário abre app / tira do background       │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │ Verificar cache local│
        │ (1 hora válido?)     │
        └──────────┬───────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
   SIM ▼                    NÃO ▼
┌─────────────┐        ┌──────────────┐
│ Exibir cache│        │ Verificar    │
│ com badge   │        │ conectividade│
│ "offline"   │        └──────┬───────┘
└─────────────┘               │
                     ┌─────────┴────────┐
                     │                  │
                  SIM ▼              NÃO ▼
            ┌──────────────────┐  ┌─────────┐
            │ Requisitar API   │  │ Usar    │
            │ (em background)  │  │ cache   │
            │ nova previsão    │  │ expirado│
            └─────┬────────────┘  └─────────┘
                  │
                  ▼
          ┌──────────────┐
          │ Sucesso?     │
          └──────┬───────┘
                 │
        ┌────────┴──────────┐
        │                   │
       SIM ▼             NÃO ▼
    ┌──────────┐    ┌──────────────┐
    │Salvar em │    │Manter cache  │
    │Room      │    │atual, mostrar│
    │Atualizar │    │toast erro    │
    │UI        │    └──────────────┘
    │Remover   │
    │badge     │
    └──────────┘
```

---

## 6. CASOS EXTREMOS & TRATAMENTO DE ERROS

### Caso Extremo 1: Conectividade Intermitente

```
Cenário: Usuário tem WiFi fraco que desconecta durante requisição

WHEN: Requisição Open-Meteo falha (timeout, connection lost)
THEN:
  1. Cancelar requisição (não retentar automaticamente)
  2. Verificar Room para dados antigos
  3. Se cache existe:
     - Exibir dados
     - Toast: "Usando dados em cache (offline)"
  4. Se nenhum cache:
     - Exibir tela de erro
     - Botão "Tentar novamente"
     - Botão "Buscar outra cidade"
```

### Caso Extremo 2: Coordenadas Inválidas

```
Cenário: GPS retorna coordenadas impossíveis

WHEN: Latitude não é -90 a 90 ou Longitude não é -180 a 180
THEN:
  1. Log erro (Timber)
  2. Não requisitar API
  3. Exibir toast: "Erro ao obter localização. Use busca manual."
  4. Mostrar campo de busca
```

### Caso Extremo 3: Taxa de Limite API Atingida

```
Cenário: Múltiplas requisições rápidas (ex: 10 buscas em 1 minuto)

WHEN: Open-Meteo retorna 429 (Too Many Requests)
THEN:
  1. Exibir toast: "Muitas requisições. Tente novamente em 1 minuto."
  2. Desabilitar button de search/refresh por 60s
  3. Mostrar countdown
  4. Usar cache local se disponível
```

### Caso Extremo 4: Permissão de Localização Negada Múltiplas Vezes

```
Cenário: Usuário nega permissão 3x

WHEN: Permissão pedida e negada 3 vezes
THEN:
  1. Deixar de pedir (respeitar preference do usuário)
  2. Exibir search bar como fallback permanente
  3. Adição à settings: "Habilitar localização" (link para app settings)
```

### Caso Extremo 5: Dados Vazios / Valores Null

```
Cenário: API retorna valor nulo para temperatura

WHEN: Valor crítico é null
THEN:
  1. Usar valor padrão ou placeholder
  2. Log warning (Timber)
  3. Não quebrar UI

Exemplos:
  - temperature: null → exibir "—" ou "N/A"
  - weatherCode: null → usar código 45 (fog)
  - humidity: null → esconder card de umidade
```

---

## 7. CRITÉRIOS DE ACEITAÇÃO (CAC)

### CAC-01: Previsão Atual

```
DADO: App aberto com localização São Paulo
QUANDO: Tela principal é renderizada
ENTÃO:
  ✅ Temperatura atual exibida com 1 casa decimal
  ✅ Ícone WMO renderizado corretamente (≥ 80dp)
  ✅ Descrição em PT-BR ("Parcialmente Nublado", não "Partly Cloudy")
  ✅ Sensação térmica é numérica (ex: "22°C", não "22.5°C")
  ✅ Umidade é percentual (ex: "65%")
  ✅ Vento com unidade (ex: "15 km/h")
  ✅ Timestamp relativo atualiza a cada minuto sem requerer API
  ✅ Card ocupa ~60-70% da altura visível
  ✅ Cores seguem Material Design 3 palette
  ✅ Todo texto é legível (contrast ≥ 4.5:1)
```

### CAC-02: Previsão Horária

```
DADO: Gráfico horário é exibido
QUANDO: Usuário visualiza a aba
ENTÃO:
  ✅ Linha vermelha mostra temperatura hora-a-hora
  ✅ Área azul mostra precipitação
  ✅ Escala Y inclui temperatura mín/máx do período
  ✅ Eixo X mostra labels a cada 2-3 horas (não todos)
  ✅ Ponto "Agora" é claramente destacado (ex: linha tracejada)
  ✅ Renderização é smooth (no jank)
  ✅ Tap em um ponto abre modal em < 300ms
  
DADO: Modal de detalhe abre
QUANDO: Usuário vê informações de uma hora específica
ENTÃO:
  ✅ Hora exibida (ex: "14:00")
  ✅ Temperatura exibida
  ✅ Umidade relativa exibida
  ✅ Velocidade do vento exibida
  ✅ Direção do vento exibida (ex: "N 270°")
  ✅ Descrição WMO exibida
  ✅ Modal fecha com swipe down ou tap outside
```

### CAC-03: Previsão 7 Dias

```
DADO: Seção de 7 dias é visível
QUANDO: Usuário scroll para essa seção
ENTÃO:
  ✅ Exibe exatamente 7 cards (7 linhas)
  ✅ Primeiro card tem label "HOJE" ou destaque visual
  ✅ Cada card mostra data, ícone, descrição, máx/mín, chuva %, vento
  ✅ Cores de temperatura: máx (vermelho), mín (azul)
  ✅ Scroll vertical é suave (≥ 50fps)
  ✅ Tap em card abre modal
  
DADO: Modal de dia abre
QUANDO: Usuário toca em um card
ENTÃO:
  ✅ Modal exibe data grande + ícone grande
  ✅ Tab "Horário" mostra 24 horas daquele dia
  ✅ Tab "Índices" mostra umidade, vento, direção
  ✅ Swipe left/right navega para próximo/anterior dia
  ✅ Primeiro dia não permite swipe left (limite)
  ✅ Sétimo dia não permite swipe right (limite)
```

### CAC-04: Localização

```
DADO: App é aberto pela primeira vez
QUANDO: Permissão é solicitada e concedida
ENTÃO:
  ✅ Network Location é obtido em < 500ms
  ✅ Previsão é requisitada com Network coordinates
  ✅ Dados aparecem na tela em < 2 segundos total
  ✅ Badge "Localização aproximada - refinando..." aparece
  ✅ GPS começa em background (silenciosamente)
  
DADO: GPS obtém coordenadas mais precisas
QUANDO: 5-30 segundos depois
ENTÃO:
  ✅ Previsão é atualizada com GPS coords
  ✅ Badge desaparece
  ✅ Atualização é silenciosa (sem flicker)
  ✅ Nenhuma requisição inútil é feita (verifica se GPS coords mudaram > 100m)
```

### CAC-05: Busca Manual

```
DADO: Campo de busca recebe foco
QUANDO: Usuário clica no ícone de pesquisa
ENTÃO:
  ✅ Histórico de 3-5 últimas buscas aparece
  ✅ Placeholder muda para "Buscar cidade..."
  
DADO: Usuário digita "São Paulo"
QUANDO: Aguarda 500ms (debounce)
ENTÃO:
  ✅ Requisição geocoding é feita (UMA única, não 10)
  ✅ Resultados aparecem em < 2 segundos
  ✅ Lista mostra até 5 cidades
  ✅ Cada item mostra nome, estado, país
  
DADO: Usuário seleciona "São Paulo, SP, Brasil"
QUANDO: Tap em resultado
ENTÃO:
  ✅ Previsão é atualizada
  ✅ Teclado fecha
  ✅ Resultado é adicionado ao histórico
  ✅ Se duplicado no histórico, move para topo (não duplica)
```

### CAC-06: Sincronização & Cache

```
DADO: App abre com dados em cache (< 1 hora)
QUANDO: Usuário vê a tela
ENTÃO:
  ✅ Dados de cache aparecem SEM badge "offline"
  ✅ Timestamp mostra "Atualizado há Xm"
  ✅ Nenhuma requisição API é feita automaticamente
  
DADO: Cache tem > 1 hora
QUANDO: App está em foreground
ENTÃO:
  ✅ Background sync inicia
  ✅ UI não é bloqueada
  ✅ Quando novos dados chegam, valores atualizam
  ✅ Timestamp atualiza
  
DADO: Usuário faz pull-to-refresh
QUANDO: Gesture detectado
ENTÃO:
  ✅ Spinner aparece imediatamente
  ✅ Requisição API é feita (ignora cache 1h)
  ✅ Spinner desaparece quando dados retornam
  ✅ Timestamp muda para "Atualizado há <1m"
  
DADO: App está offline
QUANDO: Dados foram cacheados anteriormente
ENTÃO:
  ✅ Cache aparece com badge 🔴 "OFFLINE"
  ✅ Mensagem: "Usando dados de há X horas"
  ✅ Pull-to-refresh mostra toast "Sem conexão"
  ✅ Dados permanecem acessíveis
```

---

## 8. DEPENDÊNCIAS & INTEGRAÇÕES

### Dependências Externas

| Dependência | Versão | Uso |
|-------------|--------|-----|
| Open-Meteo API | v1 | Dados meteorológicos |
| FusedLocationProvider | Play Services 21.0+ | Geolocalização |
| Retrofit | 2.9.0+ | HTTP client |
| Kotlinx Serialization | 1.5.0+ | JSON parsing |
| Room | 2.5.0+ | Local database |
| Hilt | 2.44+ | Dependency injection |
| Jetpack Compose | 1.5.0+ | UI framework |
| Material Design 3 | 1.1.0+ | Design system |
| Timber | 5.0.0+ | Logging |
| JUnit4 | 4.13.2+ | Unit testing |
| Mockk | 1.13.0+ | Mocking framework |
| Espresso | 3.5.0+ | UI testing |

### Dependências Internas (This Project)

- Constitution v1.1.0 (Governance, patterns)
- CONTRIBUTING.md (Development guidelines)

---

## 9. PLANO DE TESTES

### Cobertura de Testes

| Camada | Mínimo | Tipo | Ferramenta |
|--------|--------|------|-----------|
| ViewModel | 80% | Unit | JUnit4 + Mockk |
| Repository | 70% | Unit + Integration | JUnit4 + Mockk |
| UI (Composables) | 60% | Instrumentation | Espresso |
| UI (Screenshots) | 100% | Visual | Paparazzi |
| Utilitários | 75% | Unit | JUnit4 |

### Testes Obrigatórios

```
// Unit Tests
✅ PrevisaoViewModelTest
  - obterPrevisao_sucesso_exibeValoresCorretos()
  - obterPrevisao_erro_mostraTelaErro()
  - pull_to_refresh_ignora_cache_1hora()
  - offline_mostraCacheCom badge()

✅ RepositoryTest
  - salvarem_room_erecuperar_cache()
  - cache_expirado_1hora_requisita_api()
  - limpeza_automatica_7dias()

✅ MapearWMOTest
  - codigo_0_retorna_icone_clear_sky()
  - codigo_95_retorna_icone_tempestade()
  - codigo_invalido_retorna_default()

// UI Tests (Espresso)
✅ HomeScreenTest
  - previsao_atual_exibida_sem_erros()
  - pull_to_refresh_funciona()
  - tap_em_dia_abre_modal()

✅ SearchTest
  - busca_retorna_resultados()
  - tap_em_resultado_atualiza_previsao()
  - debounce_funciona_500ms()

// Screenshot Tests (Paparazzi)
✅ HomeScreenScreenshots
  - portrait_light_theme()
  - portrait_dark_theme() [v1.1]
  - portrait_com_dados_offline()
```

---

## 10. CRONOGRAMA & MILESTONES

### Phase 1: Setup (Semana 1-2) - 8 SP

- [ ] Gradle setup (Hilt, Room, Retrofit)
- [ ] Open-Meteo client (Retrofit interface)
- [ ] Room database schema
- [ ] Models (Previsao, DadosAtuais, etc)

### Phase 2: Data Layer (Semana 2-3) - 10 SP

- [ ] Repositories (PrevisaoRepository)
- [ ] DAO queries
- [ ] Cache strategy (1 hora)
- [ ] Offline-first logic
- [ ] Geocoding integration

### Phase 3: Business Logic (Semana 3-4) - 7 SP

- [ ] ViewModels (PrevisaoViewModel, SearchViewModel)
- [ ] Use Cases
- [ ] Error handling (Result<T>)
- [ ] Testes unitários ≥80%

### Phase 4: UI/UX (Semana 4-5) - 8 SP

- [ ] HomeScreen composable
- [ ] Previsão atual card
- [ ] Previsão horária (gráfico + lista)
- [ ] Previsão 7 dias
- [ ] SearchSheet
- [ ] Material Design 3 integration

### Phase 5: Polish & Testing (Semana 5-6) - 6 SP

- [ ] Testes instrumentados (Espresso)
- [ ] Screenshot tests (Paparazzi)
- [ ] Performance testing
- [ ] Device testing (≥2 devices)
- [ ] Build release otimizado

---

## 11. DEFINIÇÃO DE PRONTO (Definition of Done)

Uma feature/task é PRONTO quando:

- [ ] Código implementado (Kotlin)
- [ ] Testes unitários passando (≥80% ViewModel)
- [ ] Testes instrumentados passando
- [ ] Testes de screenshot adicionados
- [ ] Lint limpo (Detekt + Android Lint)
- [ ] Documentação atualizada (KDoc)
- [ ] Code review aprovado
- [ ] Performance dentro dos limites (Constitution)
- [ ] Acessibilidade TalkBack testada
- [ ] Sem memory leaks (LeakCanary)
- [ ] Merged to main branch

---

## 12. RISCOS & MITIGAÇÕES

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|--------|-----------|
| Open-Meteo API downtime | Baixa | Alto | Offline cache (7 dias), status monitoring |
| Lentidão em dispositivos antigos | Média | Médio | Lazy loading, images WebP, performance tests |
| Precision GPS falha | Baixa | Baixo | Fallback para Network provider |
| Permissões nunca concedidas | Baixa | Baixo | Search manual como fallback permanente |
| Conforme Constitution MUITO rigoroso | Média | Médio | Comunicação contínua com time tech |

---

## 13. SUCESSO CRITERIA (Métricas)

| Métrica | Target | Medição |
|---------|--------|---------|
| Startup time | ≤ 2s (cold) | Firebase Performance |
| Memory usage | ≤ 50MB | LeakCanary, ProfileR |
| APK size | ≤ 15MB | bundleRelease |
| Frame rate | ≥ 50fps | Android Profiler |
| Test coverage | ≥ 70% | JaCoCo report |
| Crash rate | < 0.1% | Firebase Crashlytics |
| User satisfaction | ≥ 4.5 ⭐ | Play Store reviews (post-launch) |
| API reliability | 99% | Open-Meteo status page |

---

## 14. NOTAS & CONSIDERAÇÕES

### Decisões de Design Documentadas

1. **Variáveis Essenciais (não todas 80+)**: MVP usa temp, umidade, vento, WMO. Outras (UV, pressão, etc) em v1.1+
2. **Localização Híbrida**: Network rápido (0-2s) + GPS refinement em background (Constitution-compliant)
3. **PT-BR Apenas**: MVP não inclui i18n completo (EN/ES em v1.1)
4. **Sem Favoritos v1.0**: Movido para v1.1 por priorização (escopo reduzido)
5. **Room em vez de SharedPreferences**: Escalável, suporta migrations futuras, testável

### Conformidade com Constitution v1.1.0

✅ **Qualidade de Código**: Kotlin 100%, Detekt 0 erros  
✅ **Padrões Android**: MVVM + Hilt + Coroutines (não RxJava)  
✅ **Mobile-First UX**: Network-first 0-2s startup, offline suportado  
✅ **Performance**: APK ~12MB, memory ≤ 40MB, frame rate 50+fps  
✅ **Testes**: TDD obrigatório, ≥70% ViewModel coverage  

---

## 15. HISTORIAL DE VERSÕES

| Versão | Data | Status | Mudanças |
|--------|------|--------|----------|
| 1.0 | 17/05/2026 | 🟢 FINAL | Criação inicial, 5 clarificações resolvidas |

---

**Próximos Passos**:
1. ✅ Revisar spec com product owner
2. ✅ Apresentar ao team técnico
3. ✅ Criar `/speckit.tasks` para decomposição
4. ✅ Iniciar Phase 1 (Setup)
5. ✅ Code review setup

---

**Documento**: [spec.md](spec.md)  
**Versão Oficial**: 1.0  
**Pronto para**: `/speckit.plan` + `/speckit.tasks`  
**Data Criação**: 17 de maio de 2026

