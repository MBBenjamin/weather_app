# Implementation Plan: Weather App Android MVP v1.0

**Branch**: `001-weather-app-mvp` | **Data**: 2026-05-17 | **Spec**: [spec.md](spec.md)

---

## Summary

Implementar app Android nativo de previsão do tempo usando Open-Meteo API (gratuita, sem auth). O app exibe condições atuais, previsão horária (24h) e diária (7 dias), com localização híbrida (Network + GPS), cache offline-first via Room e UX Mobile-First com Material Design 3. Design moderno, limpo e leve com paleta cromática inspirada em condições climáticas.

---

## Technical Context

**Language/Version**: Kotlin 1.9.x (100%) | Android Studio Ladybug 2024.1+

**Primary Dependencies**:
- Jetpack Compose BOM 2024.02.00
- Hilt 2.50 (Dependency Injection)
- Room 2.6.1 (Local DB + DAO)
- Retrofit 2.9.0 + OkHttp 4.12 + Kotlinx Serialization 1.6.3
- Vico 1.13.0 (gráfico horário — Compose-native)
- Coil 2.5.0 (imagens)
- Timber 5.0.1 (logging)
- FusedLocationProvider (Play Services 21.0+)

**Storage**: Room Database SQLite (local cache), SharedPreferences (config simples)

**Testing**: JUnit4 4.13.2 + Mockk 1.13.9 + Espresso 3.5.1 + Paparazzi 1.3.2

**Target Platform**: Android 8.0+ (API 24) → Target API 34

**Project Type**: Mobile app (Android nativo)

**Performance Goals**:
- Cold start ≤ 2s | Warm start ≤ 500ms
- Frame rate ≥ 50fps | Memory ≤ 50MB | APK ≤ 15MB

**Constraints**: Offline-capable, portrait-only, PT-BR, WCAG AA contrast, TalkBack compatible

**Scale/Scope**: 1 usuário/dispositivo, 1 cidade ativa, cache 7 dias

---

## Constitution Check

*GATE: Verificado pré-Phase 0. Re-verificado pós-Phase 1.*

| Princípio | Status | Observação |
|-----------|--------|------------|
| I. Kotlin 100% | ✅ PASSA | Nenhum Java novo |
| I. MVVM + Hilt + Coroutines | ✅ PASSA | Arquitetura definida na spec |
| I. Detekt 0 erros | ✅ PASSA | Configurado no setup |
| II. TDD obrigatório | ✅ PASSA | ≥80% ViewModel, ≥70% Repository |
| II. Testes unitários + instrumentados | ✅ PASSA | JUnit4 + Espresso + Paparazzi |
| III. Mobile-First | ✅ PASSA | Portrait, touch targets ≥48dp, PT-BR |
| III. Material Design 3 | ✅ PASSA | Vico + Compose MD3 components |
| III. TalkBack | ✅ PASSA | contentDescription obrigatória |
| IV. APK ≤ 15MB | ✅ PASSA | Estimado ~12MB com ProGuard/R8 |
| IV. Cold start ≤ 2s | ✅ PASSA | Network location 0-2s garantido |
| IV. Sem memory leaks | ✅ PASSA | LeakCanary em dev |
| V. Estrutura de projeto padrão | ✅ PASSA | Ver seção abaixo |
| V. StateFlow + Navigation Compose | ✅ PASSA | Sem LiveData novo |
| V. Timber (sem Log.d) | ✅ PASSA | Definido na spec |

**Violações**: Nenhuma. ✅ Constitution-compliant.

---

## Project Structure

### Documentation (this feature)

```text
specs/001-weather-app-mvp/
├── plan.md              # Este arquivo
├── research.md          # Decisões técnicas de pesquisa (Phase 0)
├── data-model.md        # Modelo de dados completo (Phase 1)
├── quickstart.md        # Guia de início rápido (Phase 1)
├── contracts/           # Contratos de interface (Phase 1)
│   ├── open-meteo-forecast.md
│   ├── open-meteo-geocoding.md
│   └── ui-contracts.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Gerado por /speckit-tasks (não este comando)
```

### Source Code (repository root)

```text
app/
├── src/main/
│   ├── kotlin/com/weather/
│   │   ├── presentation/
│   │   │   ├── home/
│   │   │   │   ├── HomeScreen.kt          # Composable root + scaffold
│   │   │   │   ├── HomeViewModel.kt       # StateFlow state management
│   │   │   │   └── components/
│   │   │   │       ├── CurrentWeatherCard.kt    # RF-01 - card principal
│   │   │   │       ├── HourlyForecastSection.kt # RF-02 - tabs gráfico/lista
│   │   │   │       ├── WeeklyForecastList.kt    # RF-03 - 7 cards diários
│   │   │   │       ├── LocationBadge.kt         # RF-04 - badge "refinando"
│   │   │   │       └── OfflineBadge.kt          # RF-06 - badge offline
│   │   │   ├── detail/
│   │   │   │   ├── DayDetailSheet.kt      # Modal dia (tap em card 7 dias)
│   │   │   │   └── HourDetailSheet.kt     # Bottom sheet hora (tap gráfico)
│   │   │   ├── search/
│   │   │   │   ├── SearchSheet.kt         # RF-05 - busca cidades
│   │   │   │   └── SearchViewModel.kt
│   │   │   └── theme/
│   │   │       ├── Theme.kt               # Material You + paleta climática
│   │   │       ├── Color.kt               # Design tokens cromáticos
│   │   │       ├── Typography.kt          # Type scale
│   │   │       └── Shape.kt               # Corner radius tokens
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Previsao.kt
│   │   │   │   ├── DadosAtuais.kt
│   │   │   │   ├── DadosHorarios.kt
│   │   │   │   └── DadosDiarios.kt
│   │   │   ├── repository/
│   │   │   │   ├── PrevisaoRepository.kt  # Interface
│   │   │   │   └── BuscaRepository.kt     # Interface
│   │   │   └── usecase/
│   │   │       ├── ObterPrevisaoUseCase.kt
│   │   │       └── BuscarCidadesUseCase.kt
│   │   ├── data/
│   │   │   ├── repository/
│   │   │   │   ├── PrevisaoRepositoryImpl.kt
│   │   │   │   └── BuscaRepositoryImpl.kt
│   │   │   ├── remote/
│   │   │   │   ├── OpenMeteoApi.kt
│   │   │   │   ├── GeocodingApi.kt
│   │   │   │   └── dto/
│   │   │   └── local/
│   │   │       ├── AppDatabase.kt
│   │   │       ├── entity/
│   │   │       │   ├── PrevisaoEntity.kt
│   │   │       │   └── HistoricoBuscaEntity.kt
│   │   │       └── dao/
│   │   │           ├── PrevisaoDao.kt
│   │   │           └── HistoricoBuscaDao.kt
│   │   ├── di/
│   │   │   ├── NetworkModule.kt
│   │   │   ├── DatabaseModule.kt
│   │   │   └── RepositoryModule.kt
│   │   └── utils/
│   │       ├── WmoMapper.kt
│   │       ├── DateFormatter.kt
│   │       ├── WindDirectionMapper.kt
│   │       ├── CacheValidator.kt
│   │       └── NetworkMonitor.kt
│   └── res/
│       ├── values/strings.xml
│       ├── values/colors.xml
│       ├── values/dimens.xml
│       └── drawable/                      # Ícones WMO (VectorDrawable)
├── src/test/kotlin/com/weather/
└── src/androidTest/kotlin/com/weather/

gradle/
└── libs.versions.toml                    # Version catalog centralizado
build.gradle.kts
```

**Structure Decision**: Mobile app Android (single module). Estrutura por camada conforme Constitution v1.1.0.

---

## Design System — Visual Moderno, Limpo e Leve

> Detalhado em [contracts/ui-contracts.md](contracts/ui-contracts.md). Resumo:

### Paleta Cromática (Material You + Climática)

| Token | Valor | Uso |
|-------|-------|-----|
| `colorPrimary` | `#0288D1` (Sky Blue 700) | Ações, links, destaques |
| `colorSurface` | `#FAFAFA` | Cards, bottom sheets |
| `colorBackground` | `#F0F4F8` | Fundo da tela |
| `colorOnSurface` | `#1A1A2E` | Texto primário |
| `colorTempMax` | `#EF5350` | Temperatura máxima |
| `colorTempMin` | `#42A5F5` | Temperatura mínima |
| `colorPrecip` | `#29B6F6` com 40% alpha | Área de precipitação |

**Gradientes de fundo por condição WMO**:
- Ensolarado (0-1): `#E3F2FD → #FFF9C4`
- Nublado (2-3): `#ECEFF1 → #CFD8DC`
- Chuva (51-67, 80-82): `#E8EAF6 → #BBDEFB`
- Tempestade (95-99): `#37474F → #263238`

### Tipografia

| Estilo | Tamanho | Uso |
|--------|---------|-----|
| `displayLarge` | 57sp Bold | Temperatura atual |
| `headlineMedium` | 28sp SemiBold | Temperatura max/min |
| `titleLarge` | 22sp Medium | Nome da cidade |
| `bodyLarge` | 16sp Regular | Descrições WMO |
| `labelSmall` | 11sp Regular | Timestamps |

### Shape & Spacing

- Raio de canto: 16dp (cards), 24dp (card principal), 12dp (chips)
- Elevação: 0dp surface (flat), 2dp cards
- Grid de 8dp, padding de tela 16dp

---

## Complexity Tracking

Nenhuma violação de Constitution detectada. Sem justificativas necessárias.

---

## Fases de Implementação

### Phase 1: Setup & Infrastructure — Semana 1-2 (8 SP)

1. Configurar `libs.versions.toml` com todas as dependências
2. Setup Hilt (`@HiltAndroidApp`, módulos DI)
3. Criar AppDatabase (Room) + migrations strategy
4. Configurar Retrofit + OkHttp (base URLs, serialization)
5. Setup Detekt + Android Lint
6. Criar `Theme.kt` com paleta cromática e tipografia
7. Criar skeleton da HomeScreen com Scaffold MD3
8. CI básico (GitHub Actions: lint + build)

**DoD**: App compila, abre tela em branco com tema correto, CI verde

### Phase 2: Data Layer — Semana 2-3 (10 SP)

1. DTOs (PrevisaoResponseDto, GeocodingResponseDto)
2. `OpenMeteoApi` + `GeocodingApi` Retrofit interfaces
3. `PrevisaoRepositoryImpl` com lógica de cache (1h)
4. `PrevisaoDao` + `HistoricoBuscaDao`
5. `CacheValidator` utility
6. Limpeza automática cache >7 dias (WorkManager)
7. `NetworkMonitor` (offline detection)
8. Testes unitários Repository ≥70%

**DoD**: Repository retorna dados corretos com/sem cache; testes passando

### Phase 3: Business Logic — Semana 3-4 (7 SP)

1. `ObterPrevisaoUseCase`
2. `BuscarCidadesUseCase` (debounce 500ms)
3. `HomeViewModel` com `StateFlow<HomeUiState>`
4. `SearchViewModel`
5. Location handler (FusedLocationProvider Network+GPS)
6. `WmoMapper.kt` — todos os WMO codes mapeados
7. `WindDirectionMapper.kt`
8. Error handling: `Result<T>` sealed class
9. Testes unitários ViewModels ≥80%

**DoD**: ViewModels expõem estados corretos; todos casos de erro tratados

### Phase 4: UI/UX — Semana 4-5 (8 SP)

1. `CurrentWeatherCard` com gradiente dinâmico WMO
2. `HourlyForecastSection` tabs Gráfico (Vico) + Listagem
3. `WeeklyForecastList` — 7 cards
4. `DayDetailSheet` + swipe left/right
5. `HourDetailSheet`
6. `SearchSheet` com histórico
7. `LocationBadge` + `OfflineBadge`
8. Skeleton loading (shimmer)
9. Pull-to-refresh
10. TalkBack: contentDescription em todos elementos

**DoD**: Todas as telas renderizam corretamente; TalkBack testado

### Phase 5: Polish & Testing — Semana 5-6 (6 SP)

1. Testes instrumentados Espresso
2. Screenshot tests Paparazzi
3. Performance profiling
4. Device testing em 2 devices
5. ProGuard/R8 rules
6. APK release < 15MB verificado
7. Crédito "Dados: Open-Meteo.com" no footer

**DoD**: CI verde; APK validado; crash rate 0

---

## Artefatos Gerados

| Artefato | Arquivo | Status |
|---------|---------|--------|
| Este plano | [plan.md](plan.md) | ✅ |
| Pesquisa técnica | [research.md](research.md) | ✅ |
| Modelo de dados | [data-model.md](data-model.md) | ✅ |
| Contrato API Forecast | [contracts/open-meteo-forecast.md](contracts/open-meteo-forecast.md) | ✅ |
| Contrato API Geocoding | [contracts/open-meteo-geocoding.md](contracts/open-meteo-geocoding.md) | ✅ |
| Contratos UI | [contracts/ui-contracts.md](contracts/ui-contracts.md) | ✅ |
| Quickstart dev | [quickstart.md](quickstart.md) | ✅ |
| Tasks breakdown | tasks.md | ⏳ `/speckit-tasks` |
