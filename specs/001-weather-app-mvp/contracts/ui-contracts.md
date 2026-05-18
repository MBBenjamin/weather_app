# Contratos UI: Weather App Android MVP v1.0

**Design**: Moderno, limpo e leve | **Sistema**: Material Design 3 | **Data**: 2026-05-17

---

## 1. Design System

### 1.1 Paleta de Cores

```kotlin
// Color.kt
object WeatherColors {
    // Primárias
    val SkyBlue = Color(0xFF0288D1)          // Primary — ações e destaques
    val SkyBlueLight = Color(0xFF4FC3F7)      // Primary container
    val SkyBlueDark = Color(0xFF01579B)       // Primary variant
    
    // Superfícies
    val Surface = Color(0xFFFAFAFA)           // Cards, sheets
    val Background = Color(0xFFF0F4F8)        // Fundo da tela
    val SurfaceVariant = Color(0xFFE8EDF2)    // Cards secundários
    
    // Texto
    val OnSurface = Color(0xFF1A1A2E)         // Texto primário
    val OnSurfaceVariant = Color(0xFF546E7A)  // Texto secundário
    val OnPrimary = Color(0xFFFFFFFF)
    
    // Clima específicas
    val TempMax = Color(0xFFEF5350)           // Temperatura máxima (vermelho)
    val TempMin = Color(0xFF42A5F5)           // Temperatura mínima (azul)
    val Precipitation = Color(0x6629B6F6)     // Área precipitação (40% alpha)
    val TempLine = Color(0xFFE53935)          // Linha temperatura no gráfico
    
    // Status
    val OfflineRed = Color(0xFFD32F2F)        // Badge offline
    val RefiningBlue = Color(0xFF1565C0)      // Badge refinando localização
    val Success = Color(0xFF2E7D32)
    
    // Gradientes por condição WMO
    val GradientSunny = listOf(Color(0xFFE3F2FD), Color(0xFFFFF9C4))
    val GradientCloudy = listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC))
    val GradientRainy = listOf(Color(0xFFE8EAF6), Color(0xFFBBDEFB))
    val GradientStormy = listOf(Color(0xFF37474F), Color(0xFF263238))
}
```

### 1.2 Tipografia

```kotlin
// Typography.kt
val WeatherTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    ),   // Temperatura atual "24°"
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold
    ),   // Temperatura max/min
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium
    ),   // Nome da cidade
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),   // Descrições WMO
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),   // Labels de cards, badges
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        color = WeatherColors.OnSurfaceVariant
    )    // Timestamps, secundários
)
```

### 1.3 Shape & Spacing

```kotlin
// Shape.kt
val WeatherShapes = Shapes(
    small = RoundedCornerShape(8.dp),         // Chips, badges
    medium = RoundedCornerShape(16.dp),        // Cards padrão
    large = RoundedCornerShape(24.dp),         // Card principal
    extraLarge = RoundedCornerShape(28.dp)     // Bottom sheets
)

// Design tokens de espaçamento (Grid 8dp)
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val screenPadding = 16.dp
    val cardPadding = 16.dp
    val sectionSpacing = 20.dp
}
```

---

## 2. Tela Principal (HomeScreen)

### Layout

```
┌─────────────────────────────────────────────┐  ← GradientBackground(wmoCode)
│ ┌── TopAppBar (MD3) ──────────────────────┐ │
│ │  [📍] São Paulo, SP         [🔍] [⋮]   │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│  [OfflineBadge ou LocationBadge — opcional] │
│                                             │
│ ┌── CurrentWeatherCard ────────────────┐   │
│ │         ☀️  (ícone 80dp+)           │   │
│ │            24°                      │   │  ← ~60% altura visível
│ │    Parcialmente Nublado             │   │
│ │  Sensação: 22°  Umidade: 65%        │   │
│ │  Vento: 15 km/h NO                  │   │
│ │  Atualizado há 5 minutos            │   │
│ └─────────────────────────────────────┘   │
│                                             │  ← ScrollColumn (vertical)
│ ┌── HourlyForecastSection ─────────────┐   │
│ │  [Gráfico] [Listagem]  ← Tabs        │   │
│ │  ┌── Vico Chart ─────────────────┐   │   │
│ │  │  linha vermelha temp          │   │   │
│ │  │  área azul precipitação       │   │   │
│ │  └───────────────────────────────┘   │   │
│ └─────────────────────────────────────┘   │
│                                             │
│ ┌── WeeklyForecastList ─────────────────┐  │
│ │  ┌─ Sex 17 Mai [HOJE] ──────────────┐ │  │
│ │  │ 🌤️ Nublado    27°↑   18°↓  10%🌧 │ │  │
│ │  └─────────────────────────────────┘ │  │
│ │  ┌─ Sab 18 Mai ─────────────────────┐ │  │
│ │  │ 🌧️ Chuva      24°↑   16°↓  60%🌧 │ │  │
│ │  └─────────────────────────────────┘ │  │
│ │  [... + 5 dias ...]                  │  │
│ └─────────────────────────────────────┘  │
│                                             │
│  "Dados meteorológicos: open-meteo.com"    │  ← Footer (labelSmall)
└─────────────────────────────────────────────┘
```

### Estados da Tela

| Estado | UI |
|--------|----|
| `Carregando` | Skeleton shimmer em todos os cards |
| `Sucesso` | Dados completos, sem badges |
| `Sucesso + isLocalizacaoAproximada` | LocationBadge azul visível |
| `SucessoOffline` | OfflineBadge vermelho + mensagem |
| `Erro` | Tela de erro com botão "Tentar novamente" |
| `SemPermissao` | SearchSheet aberto automaticamente |

---

## 3. Componente: CurrentWeatherCard

```
Props:
  - dadosAtuais: DadosAtuais
  - nomeLocalidade: String
  
Constraints:
  - Ocupa ~60% da altura visível da tela (fillMaxHeight(0.6f))
  - Ícone WMO: tamanho 80dp mínimo (contentDescription obrigatório)
  - Temperatura: displayLarge bold
  - Sensação + Umidade: linha única com divisor "•"
  - Touch target para pull-to-refresh: toda a área do card
  - cornerRadius: 24dp
  
Accessibility:
  - semantics { contentDescription = "Temperatura atual: 24 graus. Parcialmente Nublado. Sensação 22 graus. Umidade 65%." }
```

---

## 4. Componente: HourlyForecastSection

```
Props:
  - dadosHorarios: DadosHorarios
  - tabInicial: Int (0=Gráfico, 1=Listagem)
  
Tabs (PrimaryScrollableTabRow MD3):
  - "Gráfico" — Vico CartesianChartHost
  - "Listagem" — LazyRow com HourCard

Gráfico Vico:
  - LineLayer: temperatura (vermelho #E53935, espessura 2dp)
  - LineLayer: precipitação com ColumnLayer fill (azul 40% alpha)
  - Marcador "Agora": linha vertical tracejada
  - Scroll para "Agora" ao abrir (animateScrollToItem)
  - Eixo X: labels a cada 3h ("00h", "03h", "06h", ...)
  - Eixo Y: auto-scale ±5°C além de min/max

HourCard (LazyRow):
  - Largura: 72dp
  - Destaque hora atual: surface com borda primary
  - Tap → HourDetailSheet

Accessibility:
  - Tab "Gráfico": semantics description = "Gráfico de temperatura e precipitação nas próximas 24 horas"
  - Cada HourCard: semantics "14 horas: 24 graus, sem chuva"
```

---

## 5. Componente: WeeklyForecastList

```
Props:
  - dadosDiarios: DadosDiarios

Layout: LazyColumn (não Column — para scroll eficiente)
Cada DayCard:
  - fullWidth, height: 72dp
  - cornerRadius: 16dp
  - Primeiro card: background colorPrimaryContainer (destaque "HOJE")
  - Tap → DayDetailSheet
  - Conteúdo: [ícone WMO 32dp] [dia + data] [descrição] [↑ max em vermelho] [↓ min em azul] [%🌧]
  
Accessibility:
  - Cada card: "Sexta-feira, 17 de maio. Parcialmente nublado. Máxima 27 graus, mínima 18 graus. 10% de chance de chuva."
```

---

## 6. Bottom Sheet: HourDetailSheet

```
Props:
  - hora: HoraDados
  - onDismiss: () -> Unit
  
Layout: ModalBottomSheet (MD3)
  cornerRadius: 28dp (top only)
  dragHandle: true
  
Conteúdo:
  - Header: "14:00" (titleLarge)
  - Grid 2x3 com items:
    * Temperatura: "24°C"
    * Umidade: "65%"
    * Precipitação: "0 mm"
    * Vento: "15 km/h"
    * Direção: "Noroeste (270°)"
    * Condição: "Parcialmente Nublado" + ícone WMO

Fechamento: swipe down, tap fora, botão ×
```

---

## 7. Bottom Sheet: DayDetailSheet

```
Props:
  - diaSelecionado: Int (0-6)
  - dadosDiarios: DadosDiarios
  - dadosHorariosDoDia: DadosHorarios
  - onDismiss: () -> Unit
  
Layout: ModalBottomSheet fullscreen
  
Swipe navigation left/right entre dias:
  - HorizontalPager (Compose Foundation)
  - pageCount: 7
  - initialPage: diaSelecionado
  
Cada página:
  - Header: data + ícone grande (48dp) + descrição
  - Temp max/min grandes
  - Tab "Horário" → HourlyForecastSection (desse dia)
  - Tab "Índices" → Grid de umidade, vento, direção
  
Limites de swipe:
  - Página 0: sem swipe para trás (UserScrollEnabled via PagerState)
  - Página 6: sem swipe para frente
```

---

## 8. Bottom Sheet: SearchSheet

```
Props:
  - historico: List<HistoricoBusca>
  - searchState: SearchUiState
  - onCidadeSelecionada: (CidadeSugestao) -> Unit
  
Layout: ModalBottomSheet
  
SearchBar (MD3):
  - placeholder: "Buscar cidade..."
  - leadingIcon: Search
  - trailingIcon: × quando texto não vazio
  - onValueChange → SearchViewModel.buscar(query)

Estados do conteúdo:
  - Idle (sem texto): lista de historico (até 5 itens)
  - Carregando: CircularProgressIndicator (24dp)
  - Resultados: LazyColumn com até 5 CidadeSugestaoItem
  - Erro: Text com mensagem de erro

CidadeSugestaoItem:
  - leadingIcon: 📍
  - primaryText: "São Paulo"
  - supportingText: "São Paulo, Brasil"
  - Tap → fecha sheet + atualiza previsão
  
Accessibility:
  - SearchBar: hint "Digite o nome de uma cidade"
  - Resultados: cada item anunciado como "São Paulo, São Paulo, Brasil"
```

---

## 9. Badges

### OfflineBadge

```
Condição: HomeUiState.isOffline == true
Posição: Abaixo do TopAppBar (animateContentSize)

Aparência:
  - Cor de fundo: OfflineRed (40% alpha)
  - Ícone: WifiOff (16dp)
  - Texto: "🔴 OFFLINE — Usando dados de há X horas"
  - Estilo: labelLarge

Animação: slideInVertically + fadeIn ao aparecer
```

### LocationBadge

```
Condição: isLocalizacaoAproximada == true
Posição: Abaixo do TopAppBar

Aparência:
  - Cor de fundo: RefiningBlue (20% alpha)
  - Ícone: LocationSearching (16dp)
  - Texto: "Localização aproximada — refinando..."
  - Animação de elipsis pulsante

Desaparece: quando GPS retorna (homeViewModel atualiza isLocalizacaoAproximada = false)
```

---

## 10. Estados de Loading (Skeleton Shimmer)

Todos os cards exibem skeleton enquanto `HomeUiState.Carregando`:

```kotlin
// Exemplo CurrentWeatherCard skeleton
@Composable
fun CurrentWeatherCardSkeleton() {
    Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f)) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            // Box cinza animada simulando ícone
            ShimmerBox(80.dp, 80.dp, shape = CircleShape)
            Spacer(Modifier.height(Spacing.md))
            // Box simulando temperatura
            ShimmerBox(120.dp, 48.dp)
            // etc.
        }
    }
}
```

Animação shimmer: gradiente linear que varre da esquerda para direita (1000ms loop).

---

## 11. Animações e Micro-interações

| Interação | Animação | Duração |
|-----------|----------|---------|
| Abrir bottom sheet | Slide up + fade in | 300ms |
| Fechar bottom sheet | Slide down + fade out | 250ms |
| Tap em card (ripple) | MD3 ripple effect | Nativa |
| Badge aparece | slideInVertically + fadeIn | 300ms |
| Badge desaparece | slideOutVertically + fadeOut | 300ms |
| Gradiente de fundo muda | AnimatedContent crossfade | 500ms |
| Pull-to-refresh spinner | MD3 SwipeRefreshIndicator | Nativa |
| Swipe entre dias | HorizontalPager spring | Spring(0.8f) |
| Skeleton → conteúdo | AnimatedContent crossfade | 400ms |

---

## 12. Acessibilidade (TalkBack)

**Regras obrigatórias**:

1. Todos os `Icon()` têm `contentDescription` não null
2. Cards têm `semantics { contentDescription = "..." }` descrevendo o conteúdo completo
3. Botões têm `contentDescription` clara (não apenas ícone)
4. `SwipeRefreshLayout` tem `Modifier.semantics { onClick(label = "Atualizar previsão") }`
5. Tabs têm `contentDescription = "Aba [nome], [N] de [total]"`
6. Gráfico Vico: `semantics { contentDescription = "Gráfico de temperatura e precipitação. Máxima [X]°C às [hora]" }`
7. `HorizontalPager` dias: `semantics { pagerState.currentPage }` anunciado a cada swipe

**Contraste mínimo**: WCAG AA (4.5:1 para texto regular, 3:1 para texto grande)
- Background `#F0F4F8` com texto `#1A1A2E`: ratio 12.5:1 ✅
- Badge offline fundo `#FFCDD2` com texto `#B71C1C`: ratio 4.8:1 ✅
