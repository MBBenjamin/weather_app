# Funcionalidades Recomendadas - Weather App Android
## Primeira Versão (MVP) + Roteiro Futuro

**Data**: 17 de maio de 2026  
**Base**: Análise Open-Meteo API + Padrões de Mercado + Constitution v1.1.0  
**Foco**: Mobile-First, Performance, UX Clara

---

## 📋 Resumo Executivo

### MVP (Mínimo Viável) - v1.0
- **Escopo**: 5 funcionalidades core
- **Story Points**: 40 SP
- **Tempo estimado**: 5-7 semanas
- **Complexidade**: Média
- **Cobertura de testes**: ≥ 70% (conforme Constitution)

### ✅ Clarificações Registradas
- **Q1**: Priorização de features → **A** (Reduzir escopo: remover Cidades Favoritas em v1.0, adicionar v1.1)
- **Q2**: Variáveis meteorológicas → **A** (Essencial: temp, umidade, vento, WMO; adicionar incrementalmente)
- **Q3**: Estratégia de localização → **C** (Híbrido: Network rápido + GPS em background)
- **Q4**: Plataforma de cache → **A** (Room Database: padrão Android, testável, suporta migrations)
- **Q5**: Internacionalização → **A** (PT-BR no MVP, arquitetura i18n pronta para v1.1)

### Roadmap (v1.1 - v2.0)
- Expansão gradual baseada em feedback de usuários
- Novas variáveis meteorológicas conforme demanda

### ⚠️ Movido para v1.1
- **Cidades Favoritas** (funcionalidade #5) - será implementada em v1.1, depois do MVP

---

## 🎯 VERSÃO 1.0 (MVP) - Funcionalidades Core (5 principais)

### 1. ✅ Previsão Atual Detalhada (Tela Principal)

**Descrição**: Exibição das condições atuais para localização do usuário

**Dados a exibir**:
```
┌─ HEADER ─────────────────────┐
│ Localização (nome + coordenadas)
│ Hora da atualização: "Atualizado há 1h"
└──────────────────────────────┘

┌─ CARD PRINCIPAL ──────────────┐
│        ☀️  ou 🌧️ (ícone WMO)
│         24°C (temperatura)
│    "Parcialmente Nublado"
│  Sensação: 22°C  Umidade: 65%
│  Vento: 15 km/h  
└──────────────────────────────┘
```

**Dados Open-Meteo utilizado (MVP v1.0 - Essencial)**:
- `current.temperature_2m`
- `current.weather_code` (WMO)
- `current.apparent_temperature`
- `current.relative_humidity_2m`
- `current.wind_speed_10m`
- `current.wind_direction_10m`

**Dados adicionados em versões futuras** (v1.1+):
- UV index, pressão, visibilidade, radiação solar

**UX Mobile-First**:
- ✅ Ícone grande (48dp+) com cores Material Design 3
- ✅ Temperatura em tamanho XXL (headline)
- ✅ Descrição em linguagem natural (sem códigos)
- ✅ Swipe para atualizar (pull-to-refresh)
- ✅ Loading state enquanto sincroniza

**Testes obrigatórios**:
- [ ] Teste unitário: Parsing de resposta JSON
- [ ] Teste instrumentado: Exibição de valores
- [ ] Teste de screenshot: Layout em múltiplas resoluções
- [ ] Teste de acessibilidade: TalkBack compatível

---

### 2. ✅ Previsão Horária Hoje (Gráfico/Lista)

**Descrição**: Temperatura e precipitação hora-a-hora para as próximas 24 horas

**Apresentação (Tabs)**:

#### Tab 1: Gráfico de Linhas (Temperatura + Precipitação)
```
       25°C |
    ╱╲      |
   ╱  ╲╱╲   |
  ╱       ╲ |  Temp: vermelho
 ╱────────╲─┼─ Precip: azul (área)
0h 2h 4h 6h 8h ... 22h 23h
```

#### Tab 2: Lista com Cards Horizontais
```
📋 Listagem Horária
┌──────────┬──────────┬──────────┐
│ 10:00    │ 12:00    │ 14:00    │
│ 24°C ☀️  │ 26°C ☀️  │ 23°C 🌧️  │
│ 0% chuva │ 5% chuva │ 40% chuva│
└──────────┴──────────┴──────────┘
```

**Dados Open-Meteo utilizado**:
- `hourly.time` (timestamps)
- `hourly.temperature_2m` (24 valores)
- `hourly.precipitation` (precipitação mm)
- `hourly.weather_code` (para ícone)

**Dados adicionados em versões futuras** (v1.1+):
- Precipitation probability, wind speed, umidade

**UX Mobile-First**:
- ✅ Scroll horizontal automático para "agora"
- ✅ Highlight da hora atual
- ✅ Ícones WMO mapeados a cores
- ✅ Tap em hora = detalhe de 30 minutos
- ✅ Sem scroll vertical infinito (apenas 24h)

**Performance**:
- Cache de 1 hora (requisição única para daily + hourly)
- Apenas renderizar 24 horas (não todo o mês)

---

### 3. ✅ Previsão de 7 Dias

**Descrição**: Cards com previsão diária para próximos 7 dias

**Layout**:
```
┌─ DIA 1 (Hoje) ────────────────┐
│ Sex 17 de Maio
│ 🌤️  Parcialmente nublado
│ Máx: 27°C  |  Mín: 18°C
│ Chuva: 10% | Vento: 12 km/h
└────────────────────────────────┘

┌─ DIA 2 ────────────────────────┐
│ Sab 18 de Maio
│ 🌧️  Chuva
│ Máx: 24°C  |  Mín: 16°C
│ Chuva: 60% | Vento: 15 km/h
└────────────────────────────────┘

... (mais 5 dias)
```

**Dados Open-Meteo utilizado**:
- `daily.time` (7 datas)
- `daily.temperature_2m_max`
- `daily.temperature_2m_min`
- `daily.weather_code` (WMO)
- `daily.precipitation_probability_max`
- `daily.wind_speed_10m_max`

**Dados adicionados em versões futuras** (v1.1+):
- Sunrise/sunset, UV index, precipitation sum, daylight duration

**UX Mobile-First**:
- ✅ Card grande (full-width, tappable)
- ✅ Scroll vertical suave
- ✅ Tap em dia = expande para ver detalhes horários
- ✅ Ícone WMO com background cor (Material Design 3)
- ✅ "Hoje" destacado visualmente

**Interatividade**:
- Tap em card → abre detalhe da previsão horária daquele dia
- Swipe left/right → próximo/anterior dia

---

### 4. ✅ Localização Automática + Busca Manual

**Descrição**: Geolocalização com fallback para busca manual

**Funcionalidade A: Localização Automática**
```
1. App inicia → solicita permissão de localização
2. Se permitido:
   └─ Busca coordenadas via Network (WiFi/LTE) - RÁPIDO (0-2s)
   └─ Requisita previsão Open-Meteo imediatamente
   └─ Exibe resultados (com "localização aproximada" visual)
   └─ Em background: refina com GPS (5-30s) para maior precisão
3. Se negado:
   └─ Exibe campo de busca com sugestão padrão
```

**Implementação (Kotlin)**:
```kotlin
// Híbrido: Network rápido + GPS em background
val locationProvider = FusedLocationProviderClient(context)

// 1. Tentar Network rápido primeiro
locationProvider.getLastLocation()
    .addOnSuccessListener { location ->
        if (location != null && location.provider == LocationManager.NETWORK_PROVIDER) {
            // Network: 0-2s
            requisitarPrevisao(location.latitude, location.longitude)
            mostrarBadge("Localização aproximada - refinando...")
        }
    }

// 2. Em background: refinar com GPS
val locationRequest = LocationRequest.create().apply {
    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    interval = 5000
}

val locationCallback = object : LocationCallback() {
    override fun onLocationResult(result: LocationResult) {
        result.lastLocation?.let { location ->
            // GPS: 5-30s (mais preciso)
            requisitarPrevisao(location.latitude, location.longitude)
            removerBadge()
        }
    }
}

locationProvider.requestLocationUpdates(locationRequest, locationCallback, looper)
```

**UX Mobile-First**:
- ✅ Resultado rápido em 0-2s (Network)
- ✅ Badge visual "refinando localização..." enquanto GPS processa
- ✅ Atualização silenciosa quando GPS termina (sem jarring)
- ✅ Atende Constitution: startup ≤ 2s

**Funcionalidade B: Busca Manual (Geocoding)**
```
Usar endpoint: /v1/geocoding?name=...

Exemplo:
- Usuário digita: "São Paulo"
- API retorna: [
    {"name": "São Paulo", "admin1": "São Paulo", "latitude": -23.55, "longitude": -46.63, "country": "Brasil"},
    {"name": "São Paulo", "admin1": "Paraná", "latitude": -23.13, ...},
    ...
  ]
- User seleciona resultado
- App atualiza previsão
```

**Dados Open-Meteo utilizado**:
- `POST /v1/geocoding?name=...&count=5&language=pt`
- Latitude/longitude retornados → usar em `/v1/forecast`

**UX Mobile-First**:
- ✅ Campo de busca no topo (sempre acessível)
- ✅ Autocomplete com histórico de buscas (3-5 últimas)
- ✅ Icons: 📍 para localização atual, 🔍 para busca
- ✅ Debounce de 500ms na busca (não bombardear API)
- ✅ Mostrar nome da cidade + país nos resultados

**Permissões Android**:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

### 5. ✅ Atualização Manual + Sincronização Automática

**Descrição**: Controle de atualizações de dados

**Funcionalidade A: Pull-to-Refresh**
```
Usuário puxa para baixo:
1. Ativa refresh (loading spinner)
2. Requisita dados novos da API
3. Atualiza UI com dados frescos
4. Mostra "Atualizado há 1m"
```

**Funcionalidade B: Sincronização Automática**
```
Política de cache:
├── Primeira carga: requisita API
├── Próximas 1h: usa cache local
├── Após 1h: requisita API em background
├── Se offline: mostra cache + badge "offline"
├── Se erro: exibe toast "Erro ao atualizar"
```

**Implementação**:
```kotlin
// ViewModel
fun atualizarAgora(latitude: Double, longitude: Double) {
    viewModelScope.launch {
        _estado.value = EstadoTela.Carregando
        try {
            // Requisitar API (ignora cache)
            val resposta = servico.obterPrevisao(latitude, longitude)
            
            // Salvar em Room
            repositorio.salvarPrevisao(resposta)
            
            // Atualizar timestamp
            ultimoUpdate = System.currentTimeMillis()
            
            _estado.value = EstadoTela.Sucesso(resposta)
        } catch (e: Exception) {
            // Tentar carregar do cache mesmo expirado (Room)
            val cached = repositorio.obterPrevisaoCache()
            if (cached != null) {
                _estado.value = EstadoTela.SuccessoOffline(cached)
            } else {
                _estado.value = EstadoTela.Erro(e.message)
            }
        }
    }
}

// Room Entity
@Entity(tableName = "previsoes")
data class PrevisaoCache(
    @PrimaryKey val id: String,  // "latitude,longitude"
    val latitude: Double,
    val longitude: Double,
    val temperaturaAtual: Float,
    val umidade: Int,
    val codigoWMO: Int,
    val tempoAtualizado: Long,   // timestamp
    val dadosJson: String        // JSON completo para offline
)

// DAO
@Dao
interface PrevisaoDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(previsao: PrevisaoCache)
    
    @Query("SELECT * FROM previsoes WHERE id = :id")
    suspend fun obter(id: String): PrevisaoCache?
    
    @Query("DELETE FROM previsoes WHERE tempoAtualizado < :limiteMs")
    suspend fun limparExpirados(limiteMs: Long)
}
```

**Estratégia de cache**:
- 1 hora: usar dados em cache sem requisitar API
- Após 1h: requisitar API em background
- Se offline: usar cache mesmo expirado
- Limpeza automática: deletar dados com > 7 dias (background)

**UX Mobile-First**:
- ✅ Swipe down = refresh manual
- ✅ Timestamp: "Atualizado há 23 minutos"
- ✅ Badge vermelha 🔴 se offline
- ✅ Notificação silenciosa se dados 2+ horas velhos
- ✅ Sem freezing UI durante sync

---

## 📊 Comparação com Padrões de Mercado

### Funcionalidades que NÃO incluímos no MVP (v1.0)

| Feature | Motivo | Versão Sugerida |
|---------|--------|-----------------|
| **Cidades Favoritas** | **Reduzido para v1.1 por priorização** | **v1.1** |
| Alertas de tempestade | Requer análise complexa | v1.2 |
| Radar de chuva | Requer dados de terceiros | v2.0 |
| Histórico (7-90 dias) | Requer API Commercial plan | v1.5+ |
| Poluição do ar | Endpoints separados | v1.3 |
| Previsão semanal texto | Bom ter, não essencial | v1.1 |
| Compartilhar previsão | Social feature, v1.1 ok | v1.1 |
| Notificações push | Analytics primeiro | v1.2 |
| Modo dark automático | Fácil de adicionar | v1.1 |
| Múltiplos idiomas | Localização, v1.1 | v1.1 |
| Widgets | Post-launch | v1.3+ |

---

## 🎨 Estrutura de Telas (Mobile-First)

### Hierarquia de Informações

```
┌─────────────────────────────────────┐
│ TELA 1: HOME (Principal)             │
│ ┌─────────────────────────────────┐ │
│ │ HEADER: Localização + Menu      │ │
│ ├─────────────────────────────────┤ │
│ │ SEÇÃO 1: Previsão Atual         │ │  ← Scrollable vertical
│ │ (Grande, destaque)              │ │
│ ├─────────────────────────────────┤ │
│ │ SEÇÃO 2: Previsão Horária       │ │
│ │ (Gráfico ou lista scrollável)   │ │
│ ├─────────────────────────────────┤ │
│ │ SEÇÃO 3: Próximos 7 Dias        │ │
│ │ (Cards verticais)               │ │
│ ├─────────────────────────────────┤ │
│ │ SEÇÃO 4: Cidades Favoritas      │ │
│ │ (Scroll horizontal)             │ │
│ ├─────────────────────────────────┤ │
│ │ FOOTER: Crédito Open-Meteo      │ │
│ │ "Dados: open-meteo.com"         │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ TELA 2: DETALHE DIÁRIO (Modal)       │
│ ┌─────────────────────────────────┐ │
│ │ Data + Ícone + Descrição        │ │
│ ├─────────────────────────────────┤ │
│ │ Temp máx/mín + sensação térmica │ │
│ ├─────────────────────────────────┤ │
│ │ Previsão Horária Detalhada      │ │
│ │ (24 horas daquele dia)          │ │
│ ├─────────────────────────────────┤ │
│ │ Índices: Vento, Umidade         │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ TELA 3: BUSCA/ADICIONAR (Sheet)      │
│ ┌─────────────────────────────────┐ │
│ │ Campo de busca                  │ │
│ │ "Buscar cidade..."              │ │
│ ├─────────────────────────────────┤ │
│ │ Histórico (últimas 3 buscas)    │ │
│ ├─────────────────────────────────┤ │
│ │ Resultados de busca             │ │
│ │ (lista de cidades)              │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

## 📱 Padrões Material Design 3 (Android)

### Componentes a Usar

```kotlin
// App Bar
TopAppBar(
    title = { Text("São Paulo") },
    navigationIcon = { /* Menu */ },
    actions = { /* Menu ⋮ */ }
)

// Bottom Navigation (futuro, se múltiplas abas)
NavigationBar {
    NavigationBarItem(label = "Home", icon = Icons.Default.Home)
    NavigationBarItem(label = "Favoritos", icon = Icons.Default.Favorite)
    NavigationBarItem(label = "Sobre", icon = Icons.Default.Info)
}

// Cards
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    elevation = CardDefaults.cardElevation(4.dp)
) {
    // Conteúdo
}

// FAB (Floating Action Button)
FloatingActionButton(
    onClick = { /* Adicionar cidade */ },
    icon = { Icon(Icons.Default.Add, "Adicionar") },
    containerColor = MaterialTheme.colorScheme.primary
)

// Snackbar
Snackbar(
    modifier = Modifier.padding(16.dp),
    action = { TextButton(onClick = {}) { Text("Desfazer") } }
) {
    Text("Erro ao atualizar previsão")
}

// Loading
CircularProgressIndicator(
    modifier = Modifier.size(48.dp),
    strokeWidth = 4.dp
)
```

---

## 🏗️ Arquitetura Recomendada (MVVM + Hilt)

```
app/
├── src/main/kotlin/com/weather/
│   ├── presentation/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt          # Composable principal
│   │   │   ├── HomeViewModel.kt       # State management
│   │   │   └── components/
│   │   │       ├── CartaoDiarioAtual.kt
│   │   │       ├── GraficoHorario.kt
│   │   │       ├── PrevisaoSemanal.kt
│   │   │       └── CidadesFavoritas.kt
│   │   ├── detailday/
│   │   │   ├── DetailDayScreen.kt
│   │   │   └── DetailDayViewModel.kt
│   │   └── search/
│   │       ├── SearchSheet.kt
│   │       └── SearchViewModel.kt
│   ├── viewmodel/
│   │   ├── PrevisaoViewModel.kt
│   │   ├── FavoritosViewModel.kt
│   │   └── SearchViewModel.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Previsao.kt
│   │   │   ├── DadosAtuais.kt
│   │   │   └── CidadeFavorita.kt
│   │   └── usecase/
│   │       ├── ObterPrevisaoUseCase.kt
│   │       ├── AdicionarFavoritoUseCase.kt
│   │       └── BuscarCidadesUseCase.kt
│   ├── data/
│   │   ├── repository/
│   │   │   ├── PrevisaoRepositoryImpl.kt
│   │   │   └── FavoritosRepositoryImpl.kt
│   │   ├── remote/
│   │   │   ├── ServicoOpenMeteo.kt
│   │   │   └── ServicoGeocoding.kt
│   │   └── local/
│   │       ├── BancoDados.kt
│   │       └── DAOs/
│   ├── di/
│   │   ├── NetworkModule.kt           # Retrofit
│   │   ├── DatabaseModule.kt          # Room
│   │   └── RepositoryModule.kt        # DI
│   ├── utils/
│   │   ├── MapearWMO.kt              # WMO code → ícone
│   │   ├── FormatadorDatas.kt
│   │   └── GerenciadorCache.kt
│   └── MainActivity.kt
└── src/test/
    ├── PrevisaoViewModelTest.kt
    ├── RepositoryTest.kt
    └── FormatadoresTest.kt
```

---

## ✅ Checklist de Implementação (v1.0)

### Phase 1: Setup & Infrastructure (Semana 1-2)
- [ ] Configurar Gradle, Hilt, Room
- [ ] Setup Retrofit + Open-Meteo client
- [ ] Models (Previsao, DadosAtuais)
- [ ] Testes unitários base

### Phase 2: Data Layer (Semana 2-3)
- [ ] Implementar Repositories
- [ ] Cache strategy (1 hora)
- [ ] Offline-first logic
- [ ] Geocoding integration
- [ ] Testes de integração

### Phase 3: Business Logic (Semana 3-4)
- [ ] ViewModels com StateFlow
- [ ] Use Cases
- [ ] Error handling
- [ ] Performance optimization
- [ ] Testes unitários ≥80%

### Phase 4: UI/UX (Semana 4-5)
- [ ] Composables (Home, Detail, Search)
- [ ] Material Design 3 integration
- [ ] Animations & transitions
- [ ] Acessibilidade (TalkBack)
- [ ] Screenshot tests

### Phase 5: Polish & Testing (Semana 5-7)
- [ ] Testes instrumentados
- [ ] Performance testing
- [ ] Device testing (mín 2 devices)
- [ ] Crédito Open-Meteo
- [ ] Documentação
- [ ] Build release otimizado

---

## 📊 Estimativa de Escopo (MVP v1.0)

### Por Funcionalidade (Story Points)

| Feature | Complexity | Estimado |
|---------|-----------|----------|
| Setup inicial | Média | 8 SP |
| Previsão atual | Baixa | 3 SP |
| Previsão horária | Média | 5 SP |
| Previsão 7 dias | Baixa | 3 SP |
| Localização (híbrida) | Média | 5 SP |
| Sincronização + Room | Média | 4 SP |
| Testes | Alta | 10 SP |
| **TOTAL v1.0** | | **~35 SP** |

**Estimativa FINAL**: 5-6 semanas (1 dev), ~1 sprint de 2 semanas + dias

**Nota**: 
- Cidades Favoritas (5 SP) → v1.1
- i18n (2 SP reduzido) → PT-BR MVP, pronto para adicionar idiomas em v1.1
- Variáveis essenciais reduzem payload/startup vs opção "Completo"

---

## 🚀 Roadmap Futuro (v1.1 - v2.0)

### v1.1 (3-4 semanas após v1.0)
- [ ] **Cidades Favoritas** (PRIORIDADE #1 para v1.1)
  - Salvar até 10 localizações
  - Drag-to-reorder
  - Swipe-to-delete
- [ ] Previsão textual por dia ("Amanhã será um dia quente...")
- [ ] Modo dark automático
- [ ] Compartilhar previsão (link/screenshot)
- [ ] Múltiplos idiomas (PT-BR, EN, ES)
- [ ] Histórico de atualizações

### v1.2 (Mês 2)
- [ ] Alertas de tempestade (notificações)
- [ ] Qualidade do ar (Air Quality API)
- [ ] Índice UV detalhado
- [ ] Analytics (Firebase)
- [ ] Crash reporting (Crashlytics)

### v1.3 (Mês 3)
- [ ] Home screen widgets
- [ ] Previsão de sensação térmica em gráfico
- [ ] Comparação: temperatura real vs sensação
- [ ] Modo avião (dados em cache)
- [ ] Cloud sync (Google Drive/iCloud futuro)

### v2.0 (Trimestre 2)
- [ ] Radar de chuva (integração com terceiros)
- [ ] Histórico climático (plano Commercial)
- [ ] Temas customizáveis
- [ ] Modo notturno automático
- [ ] Integração com calendário (para planejamento)

---

## 📝 Notas Importantes

### Conformidade com Constitution v1.1.0

✅ **Qualidade de Código**
- Kotlin 100%
- Detekt + Android Lint (0 erros/avisos)
- KDoc em classes/funções públicas

✅ **Padronização de Testes**
- TDD: testes antes da implementação
- Cobertura ≥ 70% unitários
- Espresso para UI critical paths
- Screenshot testing para layouts

✅ **Mobile-First UX**
- 100% mobile-first design
- Touch targets ≥ 48dp
- Material Design 3
- TalkBack compatible
- Resposta ≤ 500ms

✅ **Performance**
- APK ≤ 15MB
- Startup ≤ 2s (cold)
- Memory ≤ 50MB
- Cache 1 hora
- Lazy loading

✅ **Padrões**
- MVVM + Hilt
- Coroutines (não threads)
- StateFlow (não LiveData)
- Result<T> sealed class
- Timber logging

---

## 🎯 Decisões de Design (Mobile-First)

### Por que não incluímos...

**Radar de chuva**: 
- Requer dados de terceiros (não Open-Meteo)
- Complexidade alta (mapas, tiles)
- APK size impact
- **Solução**: v2.0 com integração RainViewer/Weather.com

**Alertas automáticos**:
- Requer Firebase Cloud Messaging
- Custo de infraestrutura
- Necessário analytics
- **Solução**: v1.2 com notificações básicas

**Histórico 90 dias**:
- Open-Meteo gratuita não suporta
- Requer plano Commercial (€100/mês)
- DB storage (room performance)
- **Solução**: v1.5+ com upgrade plano Commercial

**Múltiplas abas**:
- Bottom navigation não necessária
- Scroll vertical > abas em mobile
- Simplifica UX
- **Solução**: Gaveta lateral para "Sobre"

---

## 📞 Contato Open-Meteo

Para dúvidas sobre limites ou uso:
- Email: info@open-meteo.com
- Status: https://status.open-meteo.com
- GitHub: https://github.com/open-meteo

---

**Próximos Passos**:
1. ✅ Revisar funcionalidades com product owner
2. ✅ Priorizar features (se MVP > 47 SP, considerar cortes)
3. ✅ Setup repository e CI/CD
4. ✅ Começar Phase 1 (Setup)
5. ✅ Sprint planning com estas funcionalidades

---

## 🎯 RESUMO EXECUTIVO - CLARIFICAÇÕES FINALIZADAS

### Decisões Tomadas (Session 17/05/2026)

| # | Decisão | Impacto | Status |
|---|---------|--------|--------|
| Q1 | Remover Favoritos da v1.0 → v1.1 | -7 SP (40 → 35 SP) | ✅ Acelera MVP |
| Q2 | Variáveis essenciais (temp/umidade/vento/WMO) | -2 KB payload, ≤500ms/req | ✅ Performance |
| Q3 | Localização híbrida (Network + GPS bg) | Startup ≤2s garantido | ✅ Constitution-ready |
| Q4 | Room Database (SQLite) | Offline-first, migrations, testável | ✅ Android-native |
| Q5 | PT-BR MVP, i18n infra pronta | -2 SP (37 → 35 SP) | ✅ Simplifica v1.0 |

### Escopo Final v1.0

```
MVP v1.0: 5 funcionalidades core
├── 1. Previsão Atual Detalhada (temp, umidade, vento, WMO)
├── 2. Previsão Horária (gráfico 24h: temp + precipitação)
├── 3. Previsão 7 Dias (cards com máx/mín/condições)
├── 4. Localização (híbrida: Network 0-2s + GPS refinement)
├── 5. Sincronização (pull-to-refresh + cache 1h)

Story Points: ~35 SP
Timeline: 5-6 semanas (1 dev)
Idioma: PT-BR (infraestrutura i18n pronta para v1.1)
Database: Room SQLite
Cache: 1 hora + offline fallback
Startup: ≤2s (Constitution v1.1.0 compliant)
```

### Conformidade com Constitution v1.1.0

✅ **Qualidade de Código**: Kotlin 100%, Detekt 0 erros, MVVM + Hilt + Coroutines  
✅ **Testes**: TDD obrigatório, ≥70% ViewModels (conforme Constitution)  
✅ **Mobile-First**: Network-first 0-2s startup, offline suportado, Portrait-only  
✅ **Performance**: APK estimado ~12MB, startup ≤2s cold, memory ≤40MB  
✅ **Padrões Android**: Room DAO, StateFlow, Timber logging, error handling Result<T>  

### Versões Futuras Priorizadas

- **v1.1** (3-4 sem): Cidades Favoritas (5 SP) + textos descritivos + modo escuro
- **v1.2** (Mês 2): Alertas + qualidade do ar + notificações
- **v1.3** (Mês 3): Widgets + histórico + Ui refinement
- **v2.0** (Q2 2026): Radar de chuva + histórico climático + integrações

### Recomendação Final

**Especificação pronta para `/speckit.specify`** ✅

Todas as ambiguidades críticas foram resolvidas. O documento está pronto para:
1. Transformação em Feature Spec formal
2. Detalhamento de Acceptance Criteria
3. Task decomposition via `/speckit.tasks`
4. Implementation planning via `/speckit.plan`

---

**Documento gerado**: FUNCIONALIDADES_MVP.md  
**Versão**: 1.0 (Final - Clarificações Completas)  
**Sessão**: speckit.clarify | 17/05/2026

