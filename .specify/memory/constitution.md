<!--
SYNC IMPACT REPORT
=================
Version Change: 1.0.0 → 1.1.0 (MINOR: Adaptação para Android Mobile-First)
Modified Principles: I (Kotlin/Android specifics), III (Mobile-First UX), IV (Android performance metrics), V (Android project structure)
Added Sections: Seção "Contexto Técnico - Android" com stack específico
Removed Sections: N/A
Templates Updated: ✅ plan-template.md, ✅ spec-template.md, ✅ tasks-template.md
Follow-up TODOs: None

-->

# Constituição Weather App

## Princípios Fundamentais

### I. Qualidade de Código (NON-NEGOTIABLE)

Todo código DEVE estar em conformidade com padrões rígidos de qualidade, legibilidade e manutenibilidade. Código de baixa qualidade não é aceitável em nenhuma circunstância.

**Requisitos obrigatórios:**
- **Linguagem**: Kotlin é obrigatória (100% do código novo). Java legado apenas em manutenção
- Lint automático (Android Lint + Detekt) DEVE estar em 0 erros, 0 avisos
- Complexity ciclomática DEVE estar abaixo de 10 por método
- Documentação clara em cada função/classe pública (propósito, parâmetros, retorno em KDoc)
- Nomes descritivos em Português (Brasil); termos técnicos podem ficar em inglês
- Sem código duplicado (DRY principle) - máximo 3 linhas repetidas aceitável
- Padrões Android obrigatórios:
  - MVVM (Model-View-ViewModel) para arquitetura
  - Dependency Injection com Hilt
  - Coroutines para operações assíncronas (nunca threads diretas)
  - Material Design 3 para UI
- DEVE passar em revisão de código antes de merge

**Rationale:** Código de qualidade em Kotlin reduz bugs, facilita manutenção e garante performance em dispositivos móveis.

---

### II. Padronização de Testes (NON-NEGOTIABLE)

TDD (Test-Driven Development) é obrigatório. Tests DEVEM ser escritos antes da implementação em 100% do código novo.

**Requisitos obrigatórios:**
- Cobertura de testes ≥ 80% de ViewModels e lógica de negócio
- Cobertura de testes ≥ 70% de Repositories e Data layer
- Cobertura de testes ≥ 60% de Composables/UI (visual testing é complementar)
- Nenhuma mudança é aceita sem testes correspondentes
- Todos os testes DEVEM passar em CI/CD antes de merge

**Categorias de testes obrigatórios:**
- **Unitários (JUnit + Mockk)**: ViewModels, Repositories, utilitários
- **Instrumentados (Espresso + UI Automator)**: Fluxos críticos, interações UI
- **Device Testing**: Testes em múltiplos dispositivos/versões Android (min API 24+)
- **Screenshot Testing**: Validação visual de componentes (Paparazzi ou similar)
- **Performance Testing**: Monitoramento de memória, CPU em devices reais

**Estrutura obrigatória:**
```
app/
├── src/
│   ├── main/      # Código principal
│   ├── test/      # Testes unitários (JVM)
│   └── androidTest/ # Testes instrumentados (device)
tests/ (opcional)
├── unit/
├── integration/
└── e2e/
```

**Rationale:** Testes garantem confiabilidade em múltiplos devices, facilitam refatoração e documentam comportamento esperado em contexto móvel.

---

### III. Experiência do Usuário em Primeiro Lugar (Mobile-First)

Cada feature DEVE ser projetada para maximizar satisfação do usuário em primeiro lugar no contexto MOBILE. UX não é um complemento, é central no design.

**Requisitos obrigatórios - Mobile-First Design:**
- **Prototipagem móvel primeiro**: Designs começam 100% mobile (não adaptação de desktop)
- Interface otimizada para toque (hit targets ≥ 48dp)
- Suporte apenas vertical (portrait) por padrão; landscape opcional se aprovado
- Scroll vertical é natural; **evitar horizontal scroll ao máximo**
- Texto legível em displays pequenos (mínimo 12sp, máximo 2 níveis de hierarquia por tela)
- Gestos simples: tap, swipe vertical, long-press (máx 3 gestos por feature)
- Feedback háptico em ações críticas (tap, confirmação)

**Requisitos obrigatórios - Padrões Android:**
- Material Design 3 components obrigatórios (não custom components)
- App bar adaptável (large, medium, small conforme contexto)
- Bottom app bar ou FAB para ações principais
- Snackbars para feedback transitório (≤ 4 segundos)
- Diálogos apenas para decisões críticas (confirmação, avisos)
- Navigation drawer ou bottom navigation conforme arquitetura

**Requisitos obrigatórios - Acessibilidade Android:**
- TalkBack compatible (100% das ações devem ser acessíveis)
- Contraste WCAG AA minimum (≥ 4.5:1 para texto)
- Descrições content para ícones (contentDescription obrigatória)
- Tamanhos mínimos respeitados (48dp x 48dp)
- Teste com TalkBack em 2 devices min antes de merge

**Requisitos obrigatórios - UX:**
- Tempo de resposta ≤ 500ms para ações do usuário
- Feedback visual/auditivo imediato (loading, sucesso, erro)
- Mensagens de erro claras, amigáveis, acionáveis em PT-BR
- Sem jargão técnico em textos de usuário
- Nenhuma feature é completa sem validação mobile/acessibilidade

**Rationale:** Boa UX móvel é a diferença entre sucesso e desinstalação. Mobile-First garante que a experiência é nativa ao Android, não um porting.

---

### IV. Performance e Leveza Extrema (Android-Focused)

Aplicação DEVE ser rápida e leve em qualquer dispositivo Android. Performance não é otimização tardia, é requisito fundamental.

**Requisitos obrigatórios - APK/Bundle:**
- APK base ≤ 15MB (bundle dinâmico para modules opcionais)
- Sem bibliotecas desnecessárias (verificar com bundleRelease)
- ProGuard/R8 ativado (release builds)
- Gradle build ≤ 30s para clean build (debug)

**Requisitos obrigatórios - Runtime:**
- Tempo de startup (cold start) ≤ 2s
- Tempo de startup (warm start) ≤ 500ms
- Uso de memória (heap) ≤ 50MB em repouso
- Sem memory leaks (monitorar com LeakCanary em dev)
- Frame rate ≥ 50fps em scrolls (target 60fps)
- Battery impact: app idle ≤ 1% battery/hora

**Requisitos obrigatórios - Otimizações:**
- Lazy loading de dados (pagination, infinite scroll)
- Lazy Composition (Compose) ou View recycling (RecyclerView)
- Imagens: WebP com fallback PNG, cache estratégico (Coil/Glide)
- Coroutines para I/O (nunca blocking threads)
- Database queries otimizadas (Room + indexes)
- Network: HTTP/2, request batching, offline-first quando aplicável

**Requisitos obrigatórios - Monitoramento:**
- Firebase Performance Monitoring integrado
- ANR (Application Not Responding) < 0.1%
- Crash rate < 0.1%
- Testes de performance em 2 devices min (entrada e topo de linha)

**Rationale:** Aplicações lentas perdem usuários no Android. Performance é um recurso competitivo essencial.

---

### V. Padronização e Consistência (Android)

Toda a base de código DEVE seguir padrões consistentes. Inconsistência cria fricção e bugs.

**Requisitos obrigatórios - Estrutura de Projeto:**
```
app/
├── src/main/
│   ├── kotlin/com/weather/
│   │   ├── presentation/     # UI (Composables ou Activities)
│   │   ├── viewmodel/        # ViewModels
│   │   ├── domain/           # Use cases, entities
│   │   ├── data/
│   │   │   ├── repository/   # Repository implementations
│   │   │   ├── remote/       # API clients, DTOs
│   │   │   └── local/        # Database, SharedPrefs
│   │   ├── di/               # Hilt modules
│   │   └── utils/            # Extensões, helpers
│   ├── res/
│   │   ├── values/           # strings.xml, colors.xml, dimens.xml
│   │   ├── drawable/
│   │   ├── layout/
│   │   └── menu/
│   └── AndroidManifest.xml
├── src/test/              # Testes unitários (JVM)
├── src/androidTest/       # Testes instrumentados
and build.gradle.kts      # Unified build config
```

**Requisitos obrigatórios - Nomenclatura:**
- Packages: `com.weather.[modulo]` (minúsculas)
- Clases: `PascalCase` (ex: `WeatherViewModel`, `ForecastRepository`)
- Interfaces: `IPascalCase` ou `Contract` suffix (ex: `IWeatherRepository` ou `WeatherRepositoryContract`)
- Métodos/propriedades: `camelCase` (ex: `fetchWeatherData()`, `currentTemperature`)
- Constantes: `UPPER_SNAKE_CASE` em `companion object` ou `object`
- Layout files: `snake_case_type` (ex: `weather_card.xml`, `forecast_list.xml`)
- Resource IDs: `type_function` (ex: `btn_get_location`, `tv_temperature`)
- Arquivos Kotlin: PascalCase matching principal class (ex: `WeatherViewModel.kt`)

**Requisitos obrigatórios - Padrões Android:**
- Padrões de commit semânticos (conventional commits)
- Configurações centralizadas: Detekt, Gradle em buildSrc ou Version Catalog
- Padrões para erro handling: Result<T> sealed class ou Try/Catch com logging
- Logging via Timber (nunca Log.d direto)
- State management: StateFlow + ViewModel (nunca LiveData novo)
- Navigation: Jetpack Navigation (Fragment) ou Compose Navigation
- Design tokens centralizados: Theme.kt (cores, tipografia, dimensões)

**Requisitos obrigatórios - Documentação:**
- Guia de estilo em `docs/STYLE_GUIDE.md` (inherited de CONTRIBUTING.md)
- README com setup Android (SDK, emulator, build variants)
- CONTRIBUTING.md atualizado com Android-specific guidance
- KDoc para classes/funções públicas

**Rationale:** Padrões Android consistentes reduzem cognitivo load e aceleram onboarding de novos membros.

---

## Contexto Técnico - Stack Android

**Plataforma alvo**: Android 8.0+ (API 24+)

**Tecnologias obrigatórias:**
- **Linguagem**: Kotlin 100% (IDE: Android Studio 2024.1+)
- **UI**: Jetpack Compose (preferido) ou XML layouts com Material Design 3
- **Arquitetura**: MVVM com LiveData/StateFlow
- **DI**: Hilt para dependency injection
- **Database**: Room para dados locais
- **Networking**: Retrofit + OkHttp + Kotlinx Serialization
- **Async**: Coroutines (Flow, LiveData)
- **Testing**: JUnit4, Mockk, Espresso, Paparazzi
- **Build**: Gradle Kotlin DSL (build.gradle.kts)
- **VCS**: Git com conventional commits
- **CI/CD**: GitHub Actions (lint, test, build)

**Dependências core permitidas:**
- Jetpack: Compose, Hilt, Room, Navigation, DataStore
- Networking: Retrofit, OkHttp, Moshi/Kotlinx Serialization
- Image: Coil ou Glide
- Logging: Timber
- Testing: JUnit, Mockk, Espresso
- Analytics: Firebase Analytics (opcional)
- Crash Reporting: Firebase Crashlytics (opcional)

**Dependências proibidas (sem justificativa):**
- Butterknife, Dagger 2 (use Hilt)
- RxJava (use Coroutines)
- MVP/MVC arquitetura (use MVVM)
- Custom networking (use Retrofit)
- Múltiplas bibliotecas de logging (use Timber + Logcat)

---

## Diretrizes de Desenvolvimento

### Code Review Obrigatório
- Todo código DEVE passar por revisão antes de merge
- Revisor DEVE verificar: qualidade, testes, performance, UX, conformidade com princípios
- Aprovação de pelo menos 1 revisor é obrigatória

### Cadência de Releases
- Releases semânticas (MAJOR.MINOR.PATCH)
- MAJOR: mudanças quebra de compatibilidade
- MINOR: novas features compatíveis
- PATCH: correções de bugs
- CHANGELOG atualizado em cada release

### CI/CD Gates
- Linting ✅
- Testes ✅ (com coverage report)
- Build ✅
- Análise de performance ✅
- Deploy automático apenas após todos os gates passarem

---

## Critérios de Aceitação para Features

Uma feature é considerada COMPLETA apenas quando atender a TODOS os critérios:

1. ✅ Testes escritos e passando (unitários + integração + E2E conforme aplicável)
2. ✅ Código revisado e aprovado
3. ✅ Cobertura de testes ≥ minimums definidos
4. ✅ Linting limpo (0 erros, 0 avisos)
5. ✅ Performance dentro de limites (bundle, memória, carregamento)
6. ✅ UX testada ou validada com usuário
7. ✅ Documentação atualizada (código, README, CHANGELOG)
8. ✅ Sem débito técnico ou marked como accepted tech debt

---

## Governança

### Vigência
Esta Constituição é o documento supremo que rege todas as decisões de design e implementação no projeto Weather App. Todas as práticas, padrões e processos DEVEM estar em conformidade com este documento.

### Emendas e Alterações
- Emendas requerem consenso do time técnico
- Documentação de mudança obrigatória
- Nova versão deve ser publicada após emenda
- Versioning semântico aplicado: quebra de princípio = MAJOR

### Revisão Contínua
- Constitution revisar a cada quarter (a cada 3 meses)
- Métricas de conformidade coletadas: % testes, % linting, % performance compliance
- Princípios não devem ser ignorados por prazos; prazos ajustam-se ao padrão

### Guia de Desenvolvimento Runtime
Para decisões do dia-a-dia e exemplos práticos, consulte `docs/CONTRIBUTING.md`

---

**Versão**: 1.1.0 | **Ratificada**: 2026-05-12 | **Última Emenda**: 2026-05-12
