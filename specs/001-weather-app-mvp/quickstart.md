# Quickstart: Weather App Android MVP v1.0

**Para**: Desenvolvedores que vão implementar o projeto | **Data**: 2026-05-17

---

## Pré-requisitos

| Ferramenta | Versão mínima | Link |
|------------|---------------|------|
| Android Studio | Ladybug 2024.1 | developer.android.com/studio |
| Android SDK | API 34 (Target) + API 24 (Min) | Via SDK Manager |
| JDK | 17+ | Incluído no Android Studio |
| Git | 2.40+ | git-scm.com |
| Kotlin | 1.9.x | Incluído no Android Studio |

---

## Setup do Projeto

### 1. Clonar e abrir

```bash
git clone <repo-url>
cd weather_app
# Abrir no Android Studio: File → Open → selecionar pasta
```

### 2. Estrutura de módulos

O projeto é **single-module** (apenas `app/`). Sem módulos dinâmicos no MVP.

### 3. Version Catalog

Todas as dependências estão em `gradle/libs.versions.toml`:

```toml
[versions]
compose-bom = "2024.02.00"
hilt = "2.50"
room = "2.6.1"
retrofit = "2.9.0"
okhttp = "4.12.0"
kotlinx-serialization = "1.6.3"
vico = "1.13.0"
coil = "2.5.0"
timber = "5.0.1"
play-services-location = "21.0.1"
junit = "4.13.2"
mockk = "1.13.9"
espresso = "3.5.1"
paparazzi = "1.3.2"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
vico-compose = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }
location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "play-services-location" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
paparazzi = { group = "app.cash.paparazzi", name = "paparazzi", version.ref = "paparazzi" }

[plugins]
android-application = { id = "com.android.application", version = "8.2.0" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version = "1.9.22" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version = "1.9.22" }
ksp = { id = "com.google.devtools.ksp", version = "1.9.22-1.0.17" }
paparazzi = { id = "app.cash.paparazzi", version.ref = "paparazzi" }
```

### 4. Build variants

| Variant | Uso |
|---------|-----|
| `debug` | Desenvolvimento (LeakCanary ativado, logs verbosos) |
| `release` | Produção (ProGuard/R8, sem logs) |

```bash
# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Verificar APK size
./gradlew bundleRelease
```

---

## Configuração do Ambiente

### Open-Meteo API

Nenhuma configuração necessária. A API é **completamente gratuita e sem autenticação**.

Base URLs:
- Forecast: `https://api.open-meteo.com/`
- Geocoding: `https://geocoding-api.open-meteo.com/`

### Permissões Android (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Hilt Application

```kotlin
@HiltAndroidApp
class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
```

---

## Executar Testes

```bash
# Testes unitários (JVM)
./gradlew test

# Testes instrumentados (requer emulador ou device)
./gradlew connectedAndroidTest

# Screenshot tests (Paparazzi — sem device necessário)
./gradlew recordPaparazziDebug   # Gravar golden images
./gradlew verifyPaparazziDebug   # Verificar contra golden images

# Coverage report (JaCoCo)
./gradlew jacocoTestReport
# Relatório em: app/build/reports/jacoco/
```

---

## Checklist de Início por Fase

### Phase 1 (Setup)

```
[ ] libs.versions.toml criado com todas dependências
[ ] @HiltAndroidApp na Application class
[ ] AppDatabase.kt com Room + migration strategy
[ ] NetworkModule.kt com Retrofit instances (forecast + geocoding)
[ ] Theme.kt com WeatherColors + WeatherTypography + WeatherShapes
[ ] HomeScreen.kt esqueleto com Scaffold MD3
[ ] Detekt config em detekt.yml
[ ] GitHub Actions: .github/workflows/ci.yml (lint + test + build)
[ ] ./gradlew assembleDebug → BUILD SUCCESSFUL ✅
```

### Phase 2 (Data Layer)

```
[ ] DTOs com @Serializable para forecast e geocoding
[ ] OpenMeteoApi interface Retrofit
[ ] GeocodingApi interface Retrofit
[ ] PrevisaoEntity + HistoricoBuscaEntity (Room @Entity)
[ ] PrevisaoDao + HistoricoBuscaDao (@Dao)
[ ] PrevisaoRepositoryImpl (cache 1h + offline fallback)
[ ] BuscaRepositoryImpl (debounce 500ms + histórico)
[ ] CacheValidator (lógica expiração 1h + 7d)
[ ] NetworkMonitor (StateFlow isOnline)
[ ] Testes unitários ≥70% coverage
[ ] ./gradlew test → BUILD SUCCESSFUL ✅
```

### Phase 3 (Business Logic)

```
[ ] WmoMapper.kt (todos os 20+ códigos principais)
[ ] WindDirectionMapper.kt (0-359° → N/NE/E/SE/S/SO/O/NO)
[ ] DateFormatter.kt (PT-BR: "Sex, 17 mai", "14:00", "há X minutos")
[ ] ObterPrevisaoUseCase (coordenadas → Previsao domain)
[ ] BuscarCidadesUseCase (query → List<CidadeSugestao>)
[ ] HomeViewModel (StateFlow<HomeUiState>)
[ ] SearchViewModel (StateFlow<SearchUiState>)
[ ] Location handler (FusedLocationProvider Network+GPS)
[ ] Result<T> sealed class para error handling
[ ] Testes unitários ViewModels ≥80% coverage
[ ] ./gradlew test → BUILD SUCCESSFUL ✅
```

### Phase 4 (UI/UX)

```
[ ] GradientBackground composable (baseado em WMO code)
[ ] CurrentWeatherCard com skeleton
[ ] HourlyForecastSection (Vico gráfico + LazyRow cards)
[ ] WeeklyForecastList (LazyColumn 7 cards)
[ ] DayDetailSheet (HorizontalPager + swipe)
[ ] HourDetailSheet (BottomSheet)
[ ] SearchSheet (SearchBar + histórico + resultados)
[ ] OfflineBadge + LocationBadge
[ ] Pull-to-refresh (SwipeRefresh)
[ ] contentDescription em todos ícones e elementos interativos
[ ] Teste manual TalkBack em 2 devices
[ ] ./gradlew assembleDebug → app funcional ✅
```

### Phase 5 (Polish)

```
[ ] Testes Espresso (HomeScreenTest, SearchTest)
[ ] Screenshot tests Paparazzi (light theme, offline state)
[ ] Android Profiler: startup ≤2s, scroll ≥50fps, memory ≤50MB
[ ] Test em device entrada de linha (ex: Motorola G13)
[ ] Test em device topo de linha (ex: Pixel 8)
[ ] ProGuard rules: -keep para Retrofit DTOs e Room entities
[ ] ./gradlew bundleRelease → APK ≤15MB verificado ✅
[ ] Crédito "Dados: open-meteo.com" visível no footer
```

---

## Referências Rápidas

| Recurso | Link |
|---------|------|
| Open-Meteo Forecast API | https://open-meteo.com/en/docs |
| Open-Meteo Geocoding API | https://open-meteo.com/en/docs/geocoding-api |
| Vico Charts Docs | https://patrykandpatrick.com/vico/wiki/ |
| Material Design 3 | https://m3.material.io/ |
| Jetpack Compose | https://developer.android.com/jetpack/compose |
| Hilt Guide | https://dagger.dev/hilt/ |
| Room Guide | https://developer.android.com/training/data-storage/room |
| WMO Code Table | https://open-meteo.com/en/docs#weathervariables (seção WMO) |

---

## Estrutura dos Documentos do Plano

```
specs/001-weather-app-mvp/
├── spec.md              ← Feature Specification (requisitos, critérios)
├── plan.md              ← Este plano (arquitetura, fases, design system)
├── research.md          ← Decisões técnicas justificadas
├── data-model.md        ← Entidades, DTOs, estados de UI
├── quickstart.md        ← Este arquivo
├── contracts/
│   ├── open-meteo-forecast.md   ← Contrato API forecast
│   ├── open-meteo-geocoding.md  ← Contrato API geocoding
│   └── ui-contracts.md          ← Telas, componentes, design system
└── checklists/
    └── requirements.md          ← Checklist de qualidade da spec
```
