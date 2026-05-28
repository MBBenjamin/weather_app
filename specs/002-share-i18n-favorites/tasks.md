# Tasks: Weather App v1.1 — Compartilhamento, i18n e Favoritos

**Branch**: `002-share-i18n-favorites` | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

**Regra de completude**: Toda tarefa que gera código só está PRONTA quando seus testes unitários estão passando. O checkbox da tarefa só pode ser marcado após todos os sub-itens estarem completos.

---

## Formato: `[ID] [P?] [Story?] Descrição com caminho do arquivo`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências incompletas)
- **[US1]**: Gerenciar Cidades Favoritas | **[US2]**: Alternar Localização/Favoritos | **[US3]**: Compartilhar Previsão | **[US4]**: Usar App em Inglês

---

## Phase 1: Foundational — i18n Infrastructure

**Purpose**: Infraestrutura de internacionalização — transversal a todas as features. Deve estar completa antes de qualquer outra fase (descrições WMO e formatos de data afetam Favoritos e Compartilhamento).

**⚠️ CRÍTICO**: Nenhuma outra fase pode começar até que esta esteja completa.

---

- [ ] T001 [US4] Criar `app/src/main/res/values/strings.xml` com todas as strings PT-BR do app
  - [ ] Incluir todas as strings atualmente hardcoded no código (labels, mensagens de erro, badges, placeholders)
  - [ ] Incluir todas as descrições WMO (wmo_0 até wmo_99 — apenas os codes usados pela Open-Meteo)
  - [ ] Incluir strings de formato de data/hora: `fmt_data` = `"EEE, dd 'de' MMM"`, `fmt_hora` = `"HH:mm"`
  - [ ] Incluir strings de compartilhamento: `share_agora`, `share_sensacao`, `share_via`, etc.
  - [ ] Seguir naming convention documentada em data-model.md (prefixos lbl_, msg_, err_, hint_, badge_, wmo_, fmt_, share_)

- [ ] T002 [P] [US4] Criar `app/src/main/res/values-en/strings.xml` com todas as strings EN-US
  - [ ] Traduzir TODAS as strings de T001 para inglês (sem omissões)
  - [ ] WMO descriptions em inglês (ex: wmo_0 = "Clear Sky", wmo_2 = "Partly Cloudy")
  - [ ] Formatos de data/hora EN-US: `fmt_data` = `"EEE, MMM d"`, `fmt_hora` = `"h:mm a"`
  - [ ] Strings de compartilhamento em inglês (ex: `share_agora` = "Now", `share_via` = "Via Weather App")

- [ ] T003 [US4] Atualizar `app/src/main/kotlin/com/weather/utils/WmoMapper.kt` para usar string resources
  - [ ] Substituir strings hardcoded `when(code) { 0 -> "Céu Limpo" ... }` por `context.getString(R.string.wmo_0)`
  - [ ] Adicionar parâmetro `Context` (ou `Resources`) ao método de mapeamento
  - [ ] Manter retorno de fallback para codes não mapeados: `context.getString(R.string.wmo_45)` (nevoeiro)
  - [ ] Todos os pontos de chamada de `WmoMapper` devem ser atualizados para passar o `Context`
  - [ ] **Testes**: `WmoMapperTest` — verificar que código 0 retorna "Céu Limpo" em pt-BR e "Clear Sky" em en-US (mockar `Resources` via `Locale`)

- [ ] T004 [P] [US4] Atualizar `app/src/main/kotlin/com/weather/utils/DateFormatter.kt` para formatação locale-aware
  - [ ] Usar `Locale.getDefault()` em todos os `DateTimeFormatter.ofPattern(...)` existentes
  - [ ] Formatos hora: PT-BR = 24h (`HH:mm`), EN-US = 12h (`h:mm a`)
  - [ ] Formatos data: PT-BR = `"EEE, dd 'de' MMM"`, EN-US = `"EEE, MMM d"`
  - [ ] **Testes**: `DateFormatterTest` — verificar formato PT-BR e EN-US com `Locale("pt", "BR")` e `Locale("en", "US")` explícitos

- [ ] T005 [US4] Verificação end-to-end de i18n no emulador
  - [ ] Mudar dispositivo/emulador para "English (United States)"
  - [ ] Verificar que 100% da UI está em inglês (labels, WMO descriptions, mensagens, datas, horas)
  - [ ] Voltar para PT-BR — verificar que 100% da UI está em português
  - [ ] Nenhuma string hardcoded deve aparecer em nenhum dos dois idiomas

**Checkpoint**: i18n completo — mudar locale do dispositivo reflete imediatamente em todo o app.

---

## Phase 2: US1+US2 — Favoritos Data Layer

**Goal (US1)**: Usuário consegue adicionar, listar e remover cidades favoritas com persistência entre sessões.

**Goal (US2)**: App exibe pager horizontal com localização atual + favoritas, navegáveis por swipe.

**Independent Test**: Adicionar "Rio de Janeiro" como favorito → fechar e reabrir o app → cidade persiste e exibe temperatura correta (dados da API ou cache).

---

### Modelos de Domínio

- [ ] T006 [P] [US1] Criar `app/src/main/kotlin/com/weather/domain/model/CidadeFavorita.kt`
  - [ ] Campos: `id: String`, `nomeCidade: String`, `estado: String`, `pais: String`, `latitude: Double`, `longitude: Double`, `adicionadoEm: Long`
  - [ ] Propriedade computada `nomeCompleto: String` = `"$nomeCidade, $estado"`
  - [ ] `data class` imutável (sem lógica de negócio)

- [ ] T007 [P] [US2] Criar `app/src/main/kotlin/com/weather/domain/model/PaginaCidade.kt`
  - [ ] `sealed class PaginaCidade` com subtipos: `object LocalizacaoAtual` e `data class Favorita(val cidade: CidadeFavorita)`
  - [ ] Propriedade `nomeExibicao: String` em cada subtipo
  - [ ] **Testes**: `PaginaCidadeTest` — verificar `nomeExibicao` para cada subtipo

### Camada Room

- [ ] T008 [US1] Criar `app/src/main/kotlin/com/weather/data/local/entity/FavoritaCidadeEntity.kt`
  - [ ] `@Entity(tableName = "favoritos", indices = [Index(value = ["nome_cidade"])])`
  - [ ] Campos conforme `data-model.md`: `id`, `nomeCidade`, `estado`, `pais`, `latitude`, `longitude`, `adicionadoEm`
  - [ ] `@PrimaryKey` em `id`; `@ColumnInfo` nos campos com nome diferente da convenção

- [ ] T009 [US1] Criar `app/src/main/kotlin/com/weather/data/local/dao/FavoritaCidadeDao.kt`
  - [ ] `observarTodos(): Flow<List<FavoritaCidadeEntity>>` — ORDER BY nome_cidade ASC
  - [ ] `listarTodos(): suspend List<FavoritaCidadeEntity>` — ORDER BY nome_cidade ASC
  - [ ] `contarTodos(): suspend Int`
  - [ ] `existe(id: String): suspend Boolean`
  - [ ] `inserir(favorito: FavoritaCidadeEntity): suspend Unit` com `OnConflictStrategy.IGNORE`
  - [ ] `remover(id: String): suspend Unit`
  - [ ] **Testes**: `FavoritaCidadeDaoTest` (instrumented, Room in-memory) — testar inserir, remover, existe, limite de contagem

- [ ] T010 [US1] Adicionar `MIGRATION_1_2` e atualizar versão em `app/src/main/kotlin/com/weather/data/local/AppDatabase.kt`
  - [ ] `DATABASE_VERSION` de `1` para `2`
  - [ ] Implementar `MIGRATION_1_2` com SQL de criação da tabela `favoritos` e índice `index_favoritos_nome_cidade` (conforme `data-model.md`)
  - [ ] Adicionar `FavoritaCidadeEntity::class` em `@Database(entities = [...])`
  - [ ] Registrar `FavoritaCidadeDao` como DAO abstrato
  - [ ] Passar `MIGRATION_1_2` em `.addMigrations(MIGRATION_1_2)` no builder
  - [ ] **Testes**: `AppDatabaseMigrationTest` (instrumented) — testar que migration de v1 para v2 preserva dados existentes de `previsoes` e `historico_busca`

### Repository e Use Case

- [ ] T011 [P] [US1] Criar `app/src/main/kotlin/com/weather/domain/repository/IFavoritosRepository.kt`
  - [ ] `observarFavoritos(): Flow<List<CidadeFavorita>>`
  - [ ] `adicionarFavorito(cidade: CidadeFavorita): Result<Unit>`
  - [ ] `removerFavorito(id: String): Result<Unit>`
  - [ ] `ehFavorito(id: String): Boolean`
  - [ ] `contarFavoritos(): Int`

- [ ] T012 [US1] Criar `app/src/main/kotlin/com/weather/data/repository/FavoritosRepositoryImpl.kt`
  - [ ] Implementa `IFavoritosRepository`
  - [ ] `adicionarFavorito`: verificar limite (contarTodos >= 10 → `Result.failure(LimiteFavoritosException)`) antes de inserir
  - [ ] `adicionarFavorito`: verificar duplicata via `ehFavorito(id)` antes de inserir
  - [ ] `observarFavoritos()`: mapear `FavoritaCidadeEntity` → `CidadeFavorita`
  - [ ] **Testes**: `FavoritosRepositoryTest` — testar: adicionar, remover, limite 10, duplicata bloqueada, flow emite corretamente. Cobertura ≥ 70%.

- [ ] T013 [US1] Criar `app/src/main/kotlin/com/weather/domain/usecase/GerenciarFavoritosUseCase.kt`
  - [ ] `adicionarFavorito(cidade: CidadeFavorita): Result<Unit>` — delega ao repository
  - [ ] `removerFavorito(id: String): Result<Unit>` — delega ao repository
  - [ ] `observarFavoritos(): Flow<List<CidadeFavorita>>` — delega ao repository
  - [ ] `ehFavorito(id: String): Boolean` — delega ao repository
  - [ ] **Testes**: `GerenciarFavoritosUseCaseTest` — mockar IFavoritosRepository com Mockk; testar fluxos de sucesso e erro

- [ ] T014 [US1] Criar `app/src/main/kotlin/com/weather/di/FavoritosModule.kt` (Hilt)
  - [ ] `@Provides @Singleton IFavoritosRepository` → `FavoritosRepositoryImpl`
  - [ ] `@Provides FavoritaCidadeDao` via `AppDatabase.favoritaCidadeDao()`
  - [ ] Registrar no grafo de DI do Hilt (`@InstallIn(SingletonComponent::class)`)

**Checkpoint**: Data layer completo — `FavoritosRepositoryTest` e `GerenciarFavoritosUseCaseTest` passando, migration verificada.

---

## Phase 3: US1+US2 — Favoritos UI

**Goal**: Pager horizontal funcional com favoritos, bottom sheet de navegação e ícone de favorito nos resultados de busca.

**Independent Test**: Adicionar 3 favoritos via busca → pager exibe 4 páginas (localização + 3) → swipe entre elas → bottom sheet navega diretamente → remover um favorito → pager atualiza para 3 páginas.

---

### ViewModel

- [ ] T015 [US1] Atualizar `HomeUiState` em `app/src/main/kotlin/com/weather/presentation/home/HomeViewModel.kt` para incluir campos v1.1
  - [ ] Adicionar: `paginas: List<PaginaCidade>`, `paginaAtualIndex: Int`, `cidadeAtualEhFavorita: Boolean`, `mostrarBottomSheetFavoritos: Boolean`, `limiteAtingido: Boolean`
  - [ ] Valor inicial: `paginas = listOf(PaginaCidade.LocalizacaoAtual)`, demais `false`/`0`

- [ ] T016 [US1] Criar `app/src/main/kotlin/com/weather/presentation/home/FavoritaViewModel.kt`
  - [ ] `StateFlow<FavoritasUiState>` com lista de favoritos + estado de loading
  - [ ] Coletar `GerenciarFavoritosUseCase.observarFavoritos()` com `viewModelScope`
  - [ ] `adicionarFavorito(cidade)` — chama use case, emite resultado (sucesso/limite/duplicata)
  - [ ] `removerFavorito(id)` — chama use case, atualiza estado
  - [ ] `ehFavorito(id)` — retorna booleano do use case
  - [ ] **Testes**: `FavoritaViewModelTest` — usando Mockk para `GerenciarFavoritosUseCase`; testar: adicionar com sucesso, limite atingido, remover, estado inicial. Cobertura ≥ 80%.

### Composables — Novos

- [ ] T017 [P] [US2] Criar `app/src/main/kotlin/com/weather/presentation/home/components/PagerIndicator.kt`
  - [ ] Dots animados: ativo = 8dp `colorPrimary`, inativo = 6dp `colorOnSurface` 30% alpha
  - [ ] `animateFloatAsState` para suavizar transição ao mudar página
  - [ ] Exibido apenas quando `totalPaginas > 1`
  - [ ] `contentDescription` = `"Página $paginaAtual de $totalPaginas"`

- [ ] T018 [P] [US2] Criar `app/src/main/kotlin/com/weather/presentation/home/components/FavoritosBottomSheet.kt`
  - [ ] Lista scrollável de cidades (localização atual no topo + favoritas em ordem alfabética)
  - [ ] Item ativo com fundo `colorPrimary` 10% alpha + checkmark à direita
  - [ ] Touch target ≥ 48dp por item (Constitution III)
  - [ ] Max height 60% da tela com scroll interno se necessário
  - [ ] `contentDescription` por item conforme `contracts/ui-contracts.md`
  - [ ] Fechar ao selecionar item ou swipe down

- [ ] T019 [P] [US1] Criar `app/src/main/kotlin/com/weather/presentation/search/components/FavoritoIconButton.kt`
  - [ ] `Icons.Filled.Favorite` (cheio) quando `isFavorito = true`, `Icons.Outlined.FavoriteBorder` quando `false`
  - [ ] `animateColorAsState` 200ms para transição de cor
  - [ ] Quando `enabled = false`: alpha 38%, não clicável
  - [ ] Touch target mínimo 48dp × 48dp
  - [ ] `contentDescription` dinâmico: "Adicionar [cidade] aos favoritos" / "Remover [cidade] dos favoritos"

- [ ] T020 [US2] Criar `app/src/main/kotlin/com/weather/presentation/home/WeatherPager.kt`
  - [ ] `HorizontalPager` com `beyondBoundsPageCount = 1` (pré-carregar página adjacente)
  - [ ] `userScrollEnabled = true`; sem conflito com pull-to-refresh (eixo Y)
  - [ ] Cada página renderiza `WeatherPageContent` com state independente via `previsaoPorPagina(pagina)`
  - [ ] Incluir `PagerIndicator` acima do conteúdo quando `paginas.size > 1`
  - [ ] Callback `onFavoritosMenuClick` para abrir `FavoritosBottomSheet`

### Composables — Modificados

- [ ] T021 [US2] Modificar `app/src/main/kotlin/com/weather/presentation/home/HomeScreen.kt`
  - [ ] Quando `uiState.paginas.size > 1`: renderizar `WeatherPager` em vez do conteúdo único
  - [ ] Quando `uiState.paginas.size == 1`: comportamento idêntico à v1.0 (zero overhead)
  - [ ] `TopAppBar` actions: adicionar `FavoritoIconButton` (visível para favoritas, oculto para localização atual) e ícone de menu `☰` quando há favoritos
  - [ ] `FavoritosBottomSheet` como `ModalBottomSheet` controlado por `uiState.mostrarBottomSheetFavoritos`

- [ ] T022 [US1] Modificar `app/src/main/kotlin/com/weather/presentation/search/SearchSheet.kt`
  - [ ] Adicionar `FavoritoIconButton` em cada item de resultado de busca
  - [ ] Passar `isFavorito = favoritaViewModel.ehFavorito(resultado.id)`
  - [ ] `onToggle`: chamar `favoritaViewModel.adicionarFavorito` ou `removerFavorito`
  - [ ] Quando `uiState.limiteAtingido` e cidade não é favorita: `enabled = false` no ícone

- [ ] T023 [US1] Testes instrumentados — fluxo de favoritos
  - [ ] Adicionar cidade favorita via SearchSheet → verificar que pager ganha nova página
  - [ ] Navegar por swipe entre páginas → verificar que conteúdo muda corretamente
  - [ ] Remover favorito via ícone no TopAppBar → verificar que pager retorna para localização atual
  - [ ] Testar limite de 10 → botão adicionar desabilitado na 11ª cidade

**Checkpoint**: Favoritos completos — US1 e US2 funcionais e testados de forma independente.

---

## Phase 4: US3 — Compartilhamento

**Goal**: Usuário compartilha previsão atual (qualquer cidade) via share sheet nativo com texto formatado no idioma do app.

**Independent Test**: Tocar em `ShareButton` → share sheet abre em < 500ms → texto contém cidade, temperatura, descrição WMO, sensação, umidade, vento, próximos 3 dias e "Via Weather App" — no idioma atual do app.

---

- [ ] T024 [US3] Criar `app/src/main/kotlin/com/weather/utils/CompartilhamentoBuilder.kt`
  - [ ] Função pura: `fun buildTexto(previsao: Previsao, strings: CompartilhamentoStrings): String`
  - [ ] `CompartilhamentoStrings` é um data class com os textos localizados (injetados pelo caller — não usa Context internamente)
  - [ ] Texto inclui: ícone emoji WMO, cidade, temperatura, descrição WMO, sensação, umidade, vento, resumo dos próximos 3 dias (dia, emoji, máx/mín), "Via Weather App"
  - [ ] Truncar nome da cidade se > 30 chars: `nomeCidade.take(28) + "…"`
  - [ ] **Testes**: `CompartilhamentoBuilderTest` — testar texto gerado em PT-BR, em EN-US (via diferentes instâncias de `CompartilhamentoStrings`), e truncagem de nome longo. Cobertura ≥ 80%.

- [ ] T025 [US3] Criar `app/src/main/kotlin/com/weather/domain/usecase/CompartilharPrevisaoUseCase.kt`
  - [ ] Recebe `Previsao` + `Context` (para `getString`)
  - [ ] Cria `CompartilhamentoStrings` com strings localizadas via `context.getString(R.string.share_xxx)`
  - [ ] Chama `CompartilhamentoBuilder.buildTexto(...)` para montar o texto
  - [ ] Retorna `TextoCompartilhamento(texto, cidade)`
  - [ ] **Testes**: `CompartilharPrevisaoUseCaseTest` — mockar Context e verificar que delega corretamente ao builder

- [ ] T026 [P] [US3] Criar `app/src/main/kotlin/com/weather/presentation/home/components/ShareButton.kt`
  - [ ] `Icons.Default.Share`, touch target ≥ 48dp
  - [ ] `contentDescription = stringResource(R.string.cd_compartilhar_previsao)`
  - [ ] Sempre habilitado (funciona offline com cache)

- [ ] T027 [US3] Integrar `ShareButton` no `TopAppBar` de `HomeScreen.kt`
  - [ ] Adicionar `ShareButton` como action no `TopAppBar` (ao lado do FavoritoIconButton)
  - [ ] `onShare`: chamar `CompartilharPrevisaoUseCase` + `Intent.ACTION_SEND` via `LocalContext`
  - [ ] Usar `remember { derivedStateOf {...} }` para não recriar o use case em cada recomposição
  - [ ] Testar: tocar em share → chooser abre com texto correto; cancelar → app retorna ao estado original
  - [ ] Testar offline: share funciona com dados em cache

**Checkpoint**: Compartilhamento completo — US3 funcional e independente; texto correto em PT-BR e EN-US.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Testes de screenshot, acessibilidade, performance, e validação final.

---

- [ ] T028 [P] Screenshot test `WeatherPager` com 2 favoritos em `app/src/test/kotlin/com/weather/screenshots/WeatherPagerScreenshotTest.kt` (Paparazzi)
  - [ ] Snapshot: pager na página 0 (localização atual) com dots visíveis
  - [ ] Snapshot: pager na página 1 (favorito) com dados corretos
  - [ ] Ambos em light theme

- [ ] T029 [P] Screenshot test `FavoritosBottomSheet` com 5 favoritos (Paparazzi)
  - [ ] Snapshot: bottom sheet aberto com item ativo destacado
  - [ ] Snapshot: estado com limite atingido (10 favoritos)

- [ ] T030 [P] Screenshot test `SearchSheet` com `FavoritoIconButton` (Paparazzi)
  - [ ] Snapshot: resultado com ícone ♥ vazio (não favoritado)
  - [ ] Snapshot: resultado com ícone ♥ cheio (favoritado)
  - [ ] Snapshot: ícone desabilitado (limite atingido)

- [ ] T031 Validação de acessibilidade TalkBack — todos os novos componentes
  - [ ] `PagerIndicator`: anuncia "Página X de Y" ao mudar de página
  - [ ] `FavoritoIconButton`: anuncia ação correta ao toggling
  - [ ] `ShareButton`: anuncia "Compartilhar previsão de [cidade]"
  - [ ] Items do `FavoritosBottomSheet`: anuncia cidade + ação ao focar
  - [ ] Testar em dispositivo físico ou emulador com TalkBack ativado

- [ ] T032 Verificação de performance com 10 favoritos carregados
  - [ ] Swipe entre páginas ≥ 60fps (Android Profiler — sem jank frames)
  - [ ] Memória total do app ≤ 50MB com 10 favoritos em cache
  - [ ] Cold start ≤ 2s mesmo com 10 favoritos no pager

- [ ] T033 [P] Verificação de APK size
  - [ ] Executar `./gradlew bundleRelease`
  - [ ] Verificar que tamanho do bundle ≤ 15MB
  - [ ] Confirmar que nenhuma dependência nova foi adicionada inadvertidamente

- [ ] T034 Detekt + Android Lint finais
  - [ ] `./gradlew detekt` — 0 erros, 0 avisos
  - [ ] `./gradlew lint` — 0 erros, 0 avisos
  - [ ] Corrigir qualquer violação encontrada antes de considerar a fase completa

---

## Dependencies & Execution Order

### Dependências entre Fases

- **Phase 1 (i18n)**: Começa imediatamente — sem dependências
- **Phase 2 (Favoritos Data)**: Após Phase 1 — `WmoMapper` atualizado é pré-requisito para ViewModel
- **Phase 3 (Favoritos UI)**: Após Phase 2 — ViewModel depende do repository
- **Phase 4 (Compartilhamento)**: Após Phase 1 — depende apenas de strings localizadas; pode ser paralelo à Phase 3
- **Phase 5 (Polish)**: Após Phases 3 e 4 — testa tudo em conjunto

### Dependências Internas (Phase 2)

```
T006 (CidadeFavorita) ──────────────────────────────→ T012 (FavoritosRepositoryImpl)
T007 (PaginaCidade)                                  → T016 (FavoritaViewModel)
T008 (FavoritaCidadeEntity) → T009 (DAO) → T010 (DB) → T012 → T013 (UseCase) → T016
T011 (IFavoritosRepository) ─────────────────────────→ T012
T014 (Hilt Module) depende de T012 + T009
```

### Dependências Internas (Phase 3)

```
T015 (HomeUiState v1.1) → T016 (FavoritaViewModel) → T021 (HomeScreen modificado)
T017 (PagerIndicator) → T020 (WeatherPager)
T018 (FavoritosBottomSheet) → T021 (HomeScreen)
T019 (FavoritoIconButton) → T022 (SearchSheet) + T021 (HomeScreen)
T020 (WeatherPager) → T021 (HomeScreen)
T023 (Testes instrumentados) depende de T021 + T022 completos
```

### Dependências Internas (Phase 4)

```
T024 (CompartilhamentoBuilder) → T025 (UseCase)
T026 (ShareButton) → T027 (HomeScreen integração)
T025 + T027 podem ser paralelos a T026
```

---

## Parallel Opportunities

### Phase 1
```
Paralelo: T001 + T002 (strings PT-BR e EN-US em arquivos separados)
Paralelo: T003 + T004 (WmoMapper e DateFormatter em arquivos separados)
```

### Phase 2
```
Paralelo: T006 + T007 + T008 + T011 (modelos independentes)
Sequencial: T008 → T009 → T010 (entity → DAO → DB)
Sequencial: T011 → T012 → T013 (interface → impl → usecase)
```

### Phase 3
```
Paralelo: T017 + T018 + T019 (composables independentes)
Sequencial: T015 → T016 (state → viewmodel)
Sequencial: T017 + T018 + T019 → T020 → T021 (componentes → pager → homescreen)
```

### Phase 4
```
Paralelo: T024 + T026 (builder e botão em arquivos separados)
Sequencial: T024 → T025 → T027
```

### Phase 5
```
Paralelo: T028 + T029 + T030 + T033 (screenshots e APK em arquivos/processos separados)
```

---

## Implementation Strategy

### MVP (só US1 — Favoritos básico)

1. Completar Phase 1 (i18n — fundação)
2. Completar Phase 2 (data layer)
3. Completar T015, T016, T019, T022 da Phase 3 (ViewModel + FavoritoIconButton + SearchSheet)
4. **PARAR E VALIDAR**: Favoritar/desfavoritar funciona, persiste entre sessões
5. Avançar para o resto da Phase 3 (pager) e Phase 4 (sharing)

### Entrega Incremental

1. Phase 1 → i18n funcionando (US4 completo)
2. Phase 2 + Phase 3 → Favoritos completo (US1 + US2)
3. Phase 4 → Compartilhamento (US3)
4. Phase 5 → Qualidade garantida

---

## Summary

| Fase | Tarefas | Story | SP |
|------|---------|-------|----|
| Phase 1: i18n Foundation | T001–T005 | US4 | 3 |
| Phase 2: Favoritos Data | T006–T014 | US1 | 4 |
| Phase 3: Favoritos UI | T015–T023 | US1+US2 | 5 |
| Phase 4: Compartilhamento | T024–T027 | US3 | 2 |
| Phase 5: Polish | T028–T034 | — | 2 |
| **Total** | **34 tarefas** | | **~16 SP** |

**Oportunidades de paralelismo**: 14 tarefas marcadas com [P]

**Critério de conclusão**: Toda tarefa que gera código requer testes unitários passando. Tasks sem código (T005, T023, T031, T032, T033, T034) requerem verificação manual documentada.
