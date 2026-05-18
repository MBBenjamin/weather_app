# Contribuindo para Weather App (Android)

Bem-vindo! Este documento fornece orientações práticas para contribuir com o projeto Weather App, uma aplicação mobile Android que segue princípios de qualidade, testes, UX mobile-first e performance.

## Antes de Começar

Familiarize-se com nossa [Constituição](./.specify/memory/constitution.md), que governa o projeto.

**Requisitos do sistema:**
- Android Studio 2024.1+ (versão atual)
- Android SDK 24+ (API level mínimo)
- JDK 17+
- Emulador Android (ou dispositivo real API 24+)
- Git

### 5 Princípios que Regem o Projeto

1. **Qualidade de Código** - Kotlin, sem avisos de linting
2. **Padronização de Testes** - TDD obrigatório (JUnit, Espresso, instrumentados)
3. **Experiência do Usuário (Mobile-First)** - Android Material Design 3, design para touch
4. **Performance** - APK ≤15MB, startup ≤2s, memória ≤50MB
5. **Padrões Consistentes** - MVVM, Hilt, Coroutines, estrutura padronizada

## Setup do Ambiente

### 1. Configuração Local

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/weather_app.git
cd weather_app

# Configure Android Studio
# 1. Abra Android Studio
# 2. File → Settings → Appearance & Behavior → System Settings → Android SDK
# 3. Instale SDK Platform API 24+ e Build Tools atuais
# 4. Crie um emulador: Tools → Device Manager → Create Device

# Configure no Android Studio
# 1. File → Project Structure → Project
#    - SDK Location: seu Android SDK path
#    - JDK location: JDK 17+
# 2. File → Settings → Plugins → Instale "Detekt" e "Timber"

# Verifique o build
./gradlew tasks  # Lista tasks disponíveis
```

### 2. Comande Android Studio

```bash
# Build debug
./gradlew assembleDebug

# Instale em emulador/device
./gradlew installDebug

# Build release (com ProGuard)
./gradlew assembleRelease

# Execute testes
./gradlew test                 # Unit tests (JVM)
./gradlew connectedAndroidTest # Instrumented tests (device)

# Lint + análise
./gradlew lint
./gradlew detekt

# Coverage
./gradlew jacocoTestReport
```

---

## Fluxo de Trabalho

### 1. Começando uma Feature

```bash
# Crie uma branch a partir de main
git checkout main
git pull origin main
git checkout -b feat/[feature-name]

# Exemplo: git checkout -b feat/weather-forecast-detail
```

### 2. Estrutura de Projeto

Estrutura padrão do projeto:

```
app/
├── src/main/
│   ├── kotlin/com/weather/
│   │   ├── presentation/           # UI Composables ou Activities
│   │   │   └── weather/
│   │   │       ├── WeatherScreen.kt
│   │   │       ├── WeatherViewModel.kt
│   │   │       └── components/
│   │   ├── domain/                 # Use cases, entities
│   │   │   └── weather/
│   │   │       ├── modelo/
│   │   │       │   └── Temperatura.kt
│   │   │       └── casouso/
│   │   │           └── BuscarPrevisaoUsoCase.kt
│   │   ├── data/                   # Data layer
│   │   │   ├── repository/         # Implementations
│   │   │   ├── remote/             # API + DTOs
│   │   │   └── local/              # Room database
│   │   ├── di/                     # Hilt modules
│   │   │   └── AppModule.kt
│   │   └── utils/                  # Extensões, helpers
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml         # Textos em PT-BR
│   │   │   ├── colors.xml
│   │   │   └── dimens.xml          # Espaçamentos, tamanhos
│   │   └── drawable/
│   └── AndroidManifest.xml
├── src/test/                       # Testes unitários (JVM local)
│   └── kotlin/com/weather/
│       ├── viewmodel/
│       │   └── WeatherViewModelTest.kt
│       └── data/
│           └── RepositoryTest.kt
├── src/androidTest/                # Testes instrumentados (device)
│   └── kotlin/com/weather/
│       └── presentation/
│           └── WeatherScreenTest.kt
└── build.gradle.kts
```

### 3. Desenvolvimento

#### Antes de qualquer código:

- [ ] Leia a especificação da feature (se existir em `/specs/`)
- [ ] Entenda os requisitos de UX mobile-first
- [ ] Prepare ambiente Android Studio, emulador ligado
- [ ] Verifique dependencies no `build.gradle.kts`

#### Escreva testes primeiro (TDD - Test-Driven Development):

❌ **Não faça** - Código sem testes:
```kotlin
// WeatherViewModel.kt - SEM TESTES
fun obterTemperatura(cidade: String): String {
  val temp = apiService.buscarTemperatura(cidade)
  return "$temp°C"
}
```

✅ **Faça** - Testes primeiro (JUnit):
```kotlin
// WeatherViewModelTest.kt - TESTES PRIMEIRO
class WeatherViewModelTest {
  private lateinit var viewModel: WeatherViewModel
  private val repositoryMock = mockk<WeatherRepository>()
  
  @Before
  fun setup() {
    viewModel = WeatherViewModel(repositoryMock)
  }
  
  @Test
  fun `deve formatar temperatura corretamente`() {
    // Arrange
    coEvery { repositoryMock.buscarTemperatura("São Paulo") } returns 25.5f
    
    // Act
    val resultado = viewModel.obterTemperatura("São Paulo")
    
    // Assert
    assertEquals("26°C", resultado)
  }
  
  @Test
  fun `deve arredondar temperatura negativa`() {
    coEvery { repositoryMock.buscarTemperatura("Gramado") } returns -5.2f
    assertEquals("-5°C", viewModel.obterTemperatura("Gramado"))
  }
  
  @Test
  fun `deve tratar erro de conexão`() {
    coEvery { repositoryMock.buscarTemperatura("X") } throws IOException()
    viewModel.obterTemperatura("X")
    // Verificar que erro foi tratado
  }
}

// WeatherViewModel.kt - DEPOIS IMPLEMENTAR
class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
  fun obterTemperatura(cidade: String): String {
    return runBlocking {
      try {
        val temp = repository.buscarTemperatura(cidade)
        "${Math.round(temp)}°C"
      } catch (e: Exception) {
        // Tratar erro
        ""
      }
    }
  }
}
```

#### Código limpo e idiomático em Kotlin:

❌ **Não faça** - Variáveis vagas, lógica complexa:
```kotlin
// Ruim: nomes crípticos, lógica acoplada
fun tmp(c: String): Double {
  var r = 0.0
  try {
    r = api.get(c).toDouble()
    if (r > 30) r = 30.0
    if (r < -10) r = -10.0
  } catch (e: Exception) {
    r = 0.0
  }
  return r
}
```

✅ **Faça** - Nomes descritivos, simples, extensível:
```kotlin
// Bom: claro, imutável, com padrão Result
sealed class TemperaturaResult {
  data class Sucesso(val celsius: Float) : TemperaturaResult()
  data class Erro(val mensagem: String) : TemperaturaResult()
}

// Constantes no companion object
companion object {
  private const val TEMPERATURA_MAX_CELSIUS = 30f
  private const val TEMPERATURA_MIN_CELSIUS = -10f
}

// Função clara com tipo de retorno explícito
fun obterTemperaturaLimitada(cidadeNome: String): TemperaturaResult {
  return try {
    val temperaturaBruta = apiServico.buscarTemperatura(cidadeNome)
    val temperaturaBound = temperaturaBruta
      .coerceIn(TEMPERATURA_MIN_CELSIUS, TEMPERATURA_MAX_CELSIUS)
    TemperaturaResult.Sucesso(temperaturaBound)
  } catch (e: IOException) {
    TemperaturaResult.Erro("Erro de conexão ao buscar temperatura")
  } catch (e: Exception) {
    TemperaturaResult.Erro("Erro inesperado: ${e.message}")
  }
}
```

### 4. Padrões Android

#### Nomenclatura

- **Arquivos/Pastas**: `kebab-case` (ex: `weather-forecast.ts`, `user-preferences/`)
- **Constantes**: `UPPER_SNAKE_CASE` (ex: `API_BASE_URL`)
- **Funções/Variáveis**: `camelCase` (ex: `fetchWeatherData`)
- **Componentes (React/Vue)**: `PascalCase` (ex: `WeatherCard.tsx`)
- **Private métodos**: prefixo `_` (ex: `_validateInput()`)

#### Nomenclatura Kotlin/Android

- **Packages**: `com.weather.modulo` (minúsculas, sem underscores)
- **Classes**: `PascalCase` (ex: `WeatherViewModel`, `ForecastRepository`)
- **Interfaces**: `IPascalCase` ou `...Contract` (ex: `IWeatherRepository` ou `WeatherRepositoryContract`)
- **Métodos/Variáveis**: `camelCase` (ex: `obterTemperatura()`, `temperaturaAtual`)
- **Constantes**: `UPPER_SNAKE_CASE` em `companion object` (ex: `TEMPO_TIMEOUT_MS`)
- **Layout XML files**: `snake_case_type` (ex: `weather_card.xml`, `forecast_list.xml`)
- **Resource IDs**: `type_descricao` (ex: `btn_obter_localizacao`, `tv_temperatura`)
- **Arquivos Kotlin**: Mesmo nome da class principal (ex: `WeatherViewModel.kt`)

#### Padrões Obrigatórios Android

**ViewModel + StateFlow (nunca LiveData novo):**
```kotlin
class WeatherViewModel(private val useCase: BuscarPrevisaoUseCase) : ViewModel() {
  private val _estado = MutableStateFlow<EstadoTela>(EstadoTela.Carregando)
  val estado: StateFlow<EstadoTela> = _estado.asStateFlow()
  
  fun obterPrevisao(cidade: String) {
    viewModelScope.launch {
      _estado.value = EstadoTela.Carregando
      val resultado = useCase.executar(cidade)
      _estado.value = when (resultado) {
        is Sucesso -> EstadoTela.Sucesso(resultado.dados)
        is Erro -> EstadoTela.Erro(resultado.mensagem)
      }
    }
  }
}
```

**Repository Pattern:**
```kotlin
interface RepositorioPrevisao {
  suspend fun obterPrevisao(cidade: String): Result<Previsao>
}

@Singleton
class RepositorioPrevisaoImpl @Inject constructor(
  private val apiServico: ServicoAPI,
  private val bancodados: BD
) : RepositorioPrevisao {
  override suspend fun obterPrevisao(cidade: String): Result<Previsao> {
    return try {
      val dto = apiServico.buscarPrevisao(cidade)
      Result.success(dto.paraEntidade())
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
```

**Error Handling (Result<T> sealed class):**
```kotlin
sealed class Result<out T> {
  data class Sucesso<T>(val dados: T) : Result<T>()
  data class Erro(val excecao: Throwable) : Result<Nothing>()
}

// Uso
val resultado = repositorio.obterTemperatura("São Paulo")
when (resultado) {
  is Sucesso -> mostrardados(resultado.dados)
  is Erro -> mostrarErro(resultado.excecao.message ?: "Erro desconhecido")
}
```

**Logging com Timber (nunca Log.d):**
```kotlin
// Setup em Application
class WeatherApp : Application() {
  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }
}

// Uso
Timber.d("Buscando previsão para: %s", cidade)
Timber.e(excecao, "Erro ao buscar dados")
```

**Hilt for Dependency Injection:**
```kotlin
// Módulo Hilt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
  @Singleton
  @Provides
  fun proverServicoAPI(): ServicoAPI = Retrofit.Builder()
    .baseUrl("https://api.weather.com")
    .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(ServicoAPI::class.java)
}

// Uso - Hilt injeta automaticamente
@HiltViewModel
class WeatherViewModel @Inject constructor(
  private val repositorio: RepositorioPrevisao
) : ViewModel() { }
```

---

## Testando sua Mudança

### Testes Unitários (JVM - rápido, local)

```bash
# Execute todos os testes
./gradlew test

# Execute um teste específico
./gradlew test --tests "com.weather.viewmodel.WeatherViewModelTest"

# Com coverage
./gradlew jacocoTestReport
```

**Exemplo de teste unitário (Mockk + JUnit):**
```kotlin
@RunWith(JUnit4::class)
class RepositorioPrevisaoImplTest {
  private val apiMock = mockk<ServicoAPI>()
  private val repositorio = RepositorioPrevisaoImpl(apiMock)
  
  @Test
  fun `obterPrevisao retorna Sucesso quando API responde`() {
    // Arrange
    coEvery { apiMock.buscarPrevisao("São Paulo") } returns 
      PrevisaoDTO(cidade = "São Paulo", temp = 25f)
    
    // Act
    val resultado = runBlocking { repositorio.obterPrevisao("São Paulo") }
    
    // Assert
    val dados = (resultado as? Result.Sucesso)?.dados
    assertNotNull(dados)
    assertEquals("São Paulo", dados?.cidade)
  }
  
  @Test
  fun `obterPrevisao retorna Erro quando API falha`() {
    coEvery { apiMock.buscarPrevisao(any()) } throws IOException("Sem conexão")
    
    val resultado = runBlocking { repositorio.obterPrevisao("São Paulo") }
    
    assertIs<Result.Erro>(resultado)
  }
}
```

### Testes Instrumentados (Device - UI + integração)

```bash
# Execute testes em device/emulador (ligar emulador primeiro!)
./gradlew connectedAndroidTest

# Teste apenas um teste
./gradlew connectedAndroidTest --tests "com.weather.presentation.WeatherScreenTest"
```

**Exemplo de teste Espresso (UI):**
```kotlin
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WeatherScreenTest {
  @get:Rule
  val hiltRule = HiltAndroidRule(this)
  
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()
  
  @Test
  fun exibeTemperaturaBuscada() {
    composeTestRule.apply {
      // Arrange (setup inicial)
      
      // Act - Toca no botão
      onNodeWithTag("btn_buscar").performClick()
      
      // Assert - Verifica se temperatura aparece
      onNodeWithText("25°C").assertExists()
      onNodeWithText("São Paulo").assertExists()
    }
  }
  
  @Test
  fun mostraErroQandoConexaoFalha() {
    composeTestRule.apply {
      // Simular erro de conexão
      
      onNodeWithTag("btn_buscar").performClick()
      
      // Verifica mensagem de erro
      onNodeWithText(containsString("Erro de conexão")).assertExists()
    }
  }
}
```

### Checklist Antes de Commit

```bash
# ✅ Testes unitários passam
./gradlew test

# ✅ Testes instrumentados passam (emulador/device ligado)
./gradlew connectedAndroidTest

# ✅ Linting limpo (Detekt)
./gradlew detekt

# ✅ Build debug sem warnings
./gradlew assembleDebug

# ✅ Cobertura adequada (verificar coverage report)
./gradlew jacocoTestReport
# Abra: build/reports/jacoco/jacocoTestReport/html/index.html

# ✅ Performance OK (APK size)
./gradlew bundleRelease
# Verifique: build/outputs/bundle/release/app-release.aab (~15MB)
```

---

## Mobile-First UX Guidelines

Todos os designs começam no mobile (portrait). Landscape é opcional e requer aprovação.

### Princípios de Design Mobile

**Touch Targets:**
- Altura/largura mínima: 48dp (Material Design 3)
- Espaçamento entre targets: ≥ 8dp
- FAB (Floating Action Button): 56dp, positioned bottom-right

**Hierarquia Visual:**
- Máximo 2 níveis por tela (app bar + conteúdo)
- Scroll vertical é natural; evitar horizontal
- Navegação via bottom app bar (< 5 ações) ou drawer

**Tipografia:**
- Headline: 24sp, bold (títulos)
- Title: 18sp, medium (seções)
- Body: 14sp, regular (conteúdo)
- Caption: 12sp, regular (dicas)
- Mínimo 12sp para legibilidade

**Cores & Contrast:**
- Contraste ≥ 4.5:1 (texto normal)
- Contraste ≥ 3:1 (texto grande, componentes)
- Teste com Stark Android app (TalkBack)

### Performance Goals (Mobile)

| Métrica | Limite | Por quê |
|---------|--------|---------|
| Startup frio | ≤ 2s | Usuário não desiste |
| Startup quente | ≤ 500ms | Experience suave |
| Scroll FPS | ≥ 50fps | Não trava |
| Tap → Resposta | ≤ 500ms | Feedback rápido |
| APK base | ≤ 15MB | Cabe em conexões lentas |
| Memória idle | ≤ 50MB | Não mata em background |

---

## Commit Semântico (Conventional Commits)

```bash
git commit -m "feat: adiciona previsão de 7 dias"
git commit -m "fix: corrige cálculo de sensação térmica"
git commit -m "refactor: simplifica ViewModel da previsão"
git commit -m "test: adiciona testes para validação de CEP"
git commit -m "docs: atualiza README com setup Android"
git commit -m "perf: otimiza carregamento de imagens com WebP"
git commit -m "style: formata código com Ktlint"
git commit -m "ci: adiciona teste de performance no CI"
```

**Formato**: `<tipo>(<escopo>): <mensagem>`

**Tipos obrigatórios:**
- `feat` - Nova feature
- `fix` - Correção de bug
- `refactor` - Refatoração sem mudança de comportamento
- `test` - Adição/modificação de testes
- `docs` - Documentação
- `perf` - Otimização de performance
- `style` - Formatação, linting (Ktlint)
- `ci` - Configurações CI/CD
- `chore` - Atualizações de dependências

---

## Pull Request

#### Antes de abrir PR:

- [ ] Atualizada com `main`: `git rebase main`
- [ ] Testes locais passando: `./gradlew test connectedAndroidTest`
- [ ] Sem conflitos de merge
- [ ] Sem warnings de Detekt/Lint

#### Template de PR:

```markdown
## Descrição
Breve descrição do que mudou (1-2 linhas).

## Tipo de Mudança
- [x] Nova feature
- [ ] Bug fix
- [ ] Refatoração
- [ ] Documentação

## Testes
- [ ] Testes unitários adicionados
- [ ] Testes instrumentados adicionados (Espresso)
- [ ] Cobertura: __% (unitário), __% (UI)
- [ ] Todos os testes passam localmente

## Performance Checklist
- [ ] APK bundle size verificado (≤15MB)
- [ ] Memory profile verificado (≤50MB idle)
- [ ] Startup time aceitável (≤2s cold, ≤500ms warm)
- [ ] Nenhum memory leak (LeakCanary clean)

## Mobile-First Checklist
- [ ] UX testada no portrait (principal)
- [ ] Touch targets ≥ 48dp
- [ ] Acessibilidade TalkBack testada
- [ ] Scroll vertical (sem horizontal)
- [ ] Responsiva em min API 24+

## General Checklist
- [ ] Código segue Kotlin style guide
- [ ] Auto-review do próprio código
- [ ] Comentários KDoc para classes/métodos públicos
- [ ] Sem código duplicado
- [ ] Nomes descritivos em PT-BR
- [ ] Detekt clean (0 erros, 0 avisos)
- [ ] Testes novos passam
- [ ] Testes existentes continuam passando
```

---

## Dicas de Performance Android

### APK Size Otimização

```kotlin
// ❌ Evitar: Importar tudo
import androidx.compose.*
import androidx.lifecycle.*

// ✅ Usar: Imports específicos
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
```

### Coroutines (nunca Thread direto)

```kotlin
// ❌ Evitar: Thread explícita (bloqueia)
Thread {
  val dados = apiServico.buscarDados() // BLOQUEIA!
}.start()

// ✅ Usar: Coroutines com viewModelScope
viewModelScope.launch(Dispatchers.IO) {
  val dados = apiServico.buscarDados() // Não bloqueia
  withContext(Dispatchers.Main) {
    _estado.value = dados
  }
}
```

### Imagens Otimizadas

```xml
<!-- ❌ Evitar: Imagem grande em drawable -->
<ImageVector src="weather_icon_high_res.png" />

<!-- ✅ Usar: WebP ou VectorDrawable + Coil lazy loading -->
<Image(
  painter = rememberAsyncImagePainter(
    model = "https://api.weather/icon.webp",
    contentScale = ContentScale.Crop,
    modifier = Modifier
      .size(48.dp)
      .clip(CircleShape)
  ),
  contentDescription = "Ícone de previsão"
)
```

### Memory Leaks Prevention

```kotlin
// ❌ Evitar: Listeners que vazam
class WeatherActivity : AppCompatActivity() {
  private val listener = View.OnClickListener { /* */ }
  
  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    findViewById<View>(R.id.botao).setOnClickListener(listener)
    // Memory leak: Activity keeps listener, listener keeps view
  }
}

// ✅ Usar: Hilt + ViewModel + composição automática
@Composable
fun WeatherScreen(viewModel: WeatherViewModel = hiltViewModel()) {
  Button(onClick = { viewModel.obterDados() }) {
    Text("Obter Dados")
  }
  // Composable é descartada automaticamente
}
```

### Database Queries

```kotlin
// ❌ Evitar: N+1 queries
val cidades = db.cidades().getAll() // Query 1
cidades.forEach { cidade ->
  val previsoes = db.previsoes().getByCidade(cidade.id) // N queries!
}

// ✅ Usar: Join queries com Room
@Query("""
  SELECT c.nome, p.temperatura 
  FROM cidades c 
  LEFT JOIN previsoes p ON c.id = p.cidade_id
""")
suspend fun obterCidadesComPrevisoes(): List<CidadeComPrevisao>
```

---

## Code Review Checklist

Ao revisar código, verifique:

- ✅ Testes presentes e passando
- ✅ Cobertura ≥ 80% (ViewModels/logic), ≥ 70% (repositories)
- ✅ Nomes descritivos em PT-BR (termos técnicos em EN)
- ✅ Sem código duplicado (max 3 linhas aceitas)
- ✅ Complexidade ciclomática < 10
- ✅ Detekt lint clean
- ✅ Performance dentro de limites (APK, memória, startup)
- ✅ Acessibilidade TalkBack testada
- ✅ Padrões MVVM + Hilt + Coroutines respeitados
- ✅ Documentação atualizada
- ✅ Sem warnings de build

---

## Recursos

- **[Constituição do Projeto](./.specify/memory/constitution.md)** - Princípios e governança
- **[Android Studio Documentation](https://developer.android.com/studio/intro)** - Setup e tools
- **[Kotlin Conventions](https://kotlinlang.org/docs/coding-conventions.html)** - Style guide oficial
- **[Material Design 3](https://m3.material.io/)** - Design guidelines
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)** - Modern UI toolkit
- **[Testing on Android](https://developer.android.com/training/testing)** - Test guides
- **[Hilt Documentation](https://dagger.dev/hilt)** - Dependency injection
- **[Coroutines Guide](https://kotlinlang.org/docs/coroutines-overview.html)** - Async programming

## Comandos Úteis

```bash
# Setup completo
./gradlew clean build

# Desenvolvimento local
./gradlew installDebug          # Instala no emulador
./gradlew connectedAndroidTest  # Testa
./gradlew detekt                # Lint

# Build release (obfuscado)
./gradlew bundleRelease

# Análise
./gradlew jacocoTestReport      # Coverage report
./gradlew analyzeReleaseBundle  # APK analysis

# Limpeza
./gradlew clean                 # Remove build/
./gradlew cleanBuildCache       # Remove cache
```

---

## Dúvidas?

- 📚 Verifique `docs/` no repositório
- 💬 Abra uma issue com a tag `question`
- 👥 Pergunte no time ou em code review

**Obrigado por contribuir! 🎉 Juntos fazemos o Weather App melhor a cada feature.**
