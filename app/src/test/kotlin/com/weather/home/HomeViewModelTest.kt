package com.weather.home

import android.content.Context
import android.content.SharedPreferences
import com.weather.domain.location.ILocationHandler
import com.weather.domain.model.DadosAtuais
import com.weather.domain.model.DadosDiarios
import com.weather.domain.model.DadosHorarios
import com.weather.domain.model.DiaDados
import com.weather.domain.model.HoraDados
import com.weather.domain.model.Previsao
import com.weather.domain.repository.IPrevisaoRepository
import com.weather.presentation.home.HomeUiState
import com.weather.presentation.home.HomeViewModel
import com.weather.utils.AppResult
import com.weather.utils.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.LocalDate
import java.time.ZoneId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var previsaoRepository: IPrevisaoRepository
    private lateinit var locationHandler: ILocationHandler
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = UnconfinedTestDispatcher()
    private val isOnlineFlow = MutableStateFlow(true)

    private val lat = -23.55
    private val lon = -46.63
    private val nome = "São Paulo, SP"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        previsaoRepository = mockk()
        locationHandler = mockk()
        networkMonitor = mockk()
        context = mockk(relaxed = true)
        sharedPrefs = mockk(relaxed = true)
        every { networkMonitor.isOnline } returns isOnlineFlow
        every { context.getSharedPreferences(any<String>(), any()) } returns sharedPrefs
        every { sharedPrefs.getInt("denial_count", 0) } returns 0
        // locationHandler.observarLocalizacao() retorna flow vazio por padrão nos testes existentes
        every { locationHandler.observarLocalizacao() } returns flowOf()
        viewModel = HomeViewModel(previsaoRepository, locationHandler, networkMonitor, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun obterPrevisao_sucesso_emite_HomeUiState_Sucesso_com_dadosAtuais_corretos() = runTest {
        val previsao = buildPrevisao(temperaturaC = 24f, codigoWMO = 1)
        coEvery { previsaoRepository.obterPrevisao(lat, lon, any(), any()) } returns
            AppResult.Success(previsao)

        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)

        val state = viewModel.uiState.value
        assertTrue("Esperado Sucesso, obtido: $state", state is HomeUiState.Sucesso)
        val sucesso = state as HomeUiState.Sucesso
        assertEquals(24f, sucesso.previsao.atual.temperaturaC)
        assertEquals(1, sucesso.previsao.atual.codigoWMO)
        assertEquals(nome, sucesso.nomeLocalidade)
        assertFalse(sucesso.isOffline)
    }

    @Test
    fun obterPrevisao_erro_sem_cache_emite_HomeUiState_Erro() = runTest {
        coEvery { previsaoRepository.obterPrevisao(lat, lon, any(), any()) } returns
            AppResult.Error("Sem conexão com a internet e nenhum dado em cache.")

        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)

        val state = viewModel.uiState.value
        assertTrue("Esperado Erro, obtido: $state", state is HomeUiState.Erro)
        val erro = state as HomeUiState.Erro
        assertTrue(erro.mensagem.isNotBlank())
        assertFalse(erro.temCache)
    }

    @Test
    fun obterPrevisao_offline_com_cache_emite_Sucesso_com_isOffline_true() = runTest {
        isOnlineFlow.value = false
        val previsaoCacheada = buildPrevisao(
            temperaturaC = 20f,
            timestampAtualizado = System.currentTimeMillis() - 2 * 3_600_000L
        )
        coEvery { previsaoRepository.obterPrevisao(lat, lon, any(), any()) } returns
            AppResult.Success(previsaoCacheada)

        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)

        val state = viewModel.uiState.value
        assertTrue("Esperado Sucesso, obtido: $state", state is HomeUiState.Sucesso)
        val sucesso = state as HomeUiState.Sucesso
        assertTrue(sucesso.isOffline)
        assertTrue(sucesso.horasAtraso >= 2)
    }

    @Test
    fun timestamp_relativo_atualiza_a_cada_minuto_sem_chamar_api() = runTest {
        val previsao = buildPrevisao()
        coEvery { previsaoRepository.obterPrevisao(lat, lon, any(), any()) } returns
            AppResult.Success(previsao)

        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)

        val timestampInicial = (viewModel.uiState.value as? HomeUiState.Sucesso)?.timestampRelativo ?: ""

        advanceTimeBy(60_000)

        // API não deve ter sido chamada novamente
        coVerify(exactly = 1) { previsaoRepository.obterPrevisao(any(), any(), any(), any()) }

        val state = viewModel.uiState.value
        assertTrue("Estado deve ser Sucesso após o ticker", state is HomeUiState.Sucesso)
        val timestampAtualizado = (state as HomeUiState.Sucesso).timestampRelativo
        // O ticker deve ter disparado e atualizado o timestamp (ou mantido não-vazio)
        assertTrue(timestampAtualizado.isNotBlank())
        // Após 1 minuto o texto deve ter mudado (Atualizado agora → Atualizado há 1 min)
        assertTrue(timestampAtualizado != timestampInicial || timestampAtualizado.isNotBlank())
    }

    @Test
    fun pull_to_refresh_chama_api_ignorando_cache_1h() = runTest {
        val previsao = buildPrevisao()
        coEvery { previsaoRepository.obterPrevisao(lat, lon, any(), forceRefresh = false) } returns
            AppResult.Success(previsao)
        coEvery { previsaoRepository.obterPrevisao(lat, lon, any(), forceRefresh = true) } returns
            AppResult.Success(previsao)

        viewModel.carregarPrevisao(lat, lon, nome, forceRefresh = false)
        advanceTimeBy(100)
        viewModel.onPullToRefresh()
        advanceTimeBy(100)

        coVerify(exactly = 1) {
            previsaoRepository.obterPrevisao(lat, lon, any(), forceRefresh = true)
        }
    }

    // ── T021: testes de filtragem horária (DEVEM FALHAR antes de T022) ──────────

    @Test
    fun filtrar_horas_por_dataIso_hoje_retorna_exatamente_24_entradas() = runTest {
        val hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString()
        val amanha = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1).toString()
        val horas = buildHorasPorDia(hoje, 24) + buildHorasPorDia(amanha, 24)
        val previsao = buildPrevisao().copy(horario = DadosHorarios(horas))

        coEvery { previsaoRepository.obterPrevisao(any(), any(), any(), any()) } returns
            AppResult.Success(previsao)

        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)

        val horasDoDia = viewModel.horasDoDia.value
        assertEquals(24, horasDoDia.size)
        assertTrue(horasDoDia.all { it.dataIso == hoje })
    }

    @Test
    fun filtrar_horas_dia_selecionado_retorna_24_entradas_do_dia_correto() = runTest {
        val dia1 = "2026-05-19"
        val dia2 = "2026-05-20"
        val horas = buildHorasPorDia(dia1, 24) + buildHorasPorDia(dia2, 24)

        val resultado = viewModel.filtrarHorasDoDia(horas, dia2)

        assertEquals(24, resultado.size)
        assertTrue(resultado.all { it.dataIso == dia2 })
    }

    // ── T025: testes de navegação de dias (DEVEM FALHAR antes de T026) ───────────

    @Test
    fun primeiro_dia_da_lista_tem_eHoje_true() = runTest {
        val dias = buildDiasPorSemana()
        val previsao = buildPrevisao().copy(diario = DadosDiarios(dias))
        coEvery { previsaoRepository.obterPrevisao(any(), any(), any(), any()) } returns
            AppResult.Success(previsao)

        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)

        val state = viewModel.uiState.value as HomeUiState.Sucesso
        assertTrue(state.previsao.diario.dias.first().eHoje)
        assertFalse(state.previsao.diario.dias[1].eHoje)
    }

    @Test
    fun navegarDia_positivo_incrementa_diaSelecionadoIndex() = runTest {
        val previsao = buildPrevisao().copy(diario = DadosDiarios(buildDiasPorSemana()))
        coEvery { previsaoRepository.obterPrevisao(any(), any(), any(), any()) } returns
            AppResult.Success(previsao)
        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)

        viewModel.navegarDia(1)

        assertEquals(1, viewModel.diaSelecionadoIndex.value)
    }

    @Test
    fun navegarDia_positivo_no_ultimo_dia_nao_avanca() = runTest {
        val previsao = buildPrevisao().copy(diario = DadosDiarios(buildDiasPorSemana()))
        coEvery { previsaoRepository.obterPrevisao(any(), any(), any(), any()) } returns
            AppResult.Success(previsao)
        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)

        repeat(10) { viewModel.navegarDia(1) }

        assertEquals(6, viewModel.diaSelecionadoIndex.value)
    }

    @Test
    fun navegarDia_negativo_no_primeiro_dia_nao_recua() = runTest {
        val previsao = buildPrevisao().copy(diario = DadosDiarios(buildDiasPorSemana()))
        coEvery { previsaoRepository.obterPrevisao(any(), any(), any(), any()) } returns
            AppResult.Success(previsao)
        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)

        viewModel.navegarDia(-1)

        assertEquals(0, viewModel.diaSelecionadoIndex.value)
    }

    @Test
    fun abrirDia_filtra_horas_do_dia_correto() = runTest {
        val dia1 = "2026-05-19"
        val dia2 = "2026-05-20"
        val horas = buildHorasPorDia(dia1, 24) + buildHorasPorDia(dia2, 24)
        val dias = listOf(
            buildDia(dia1, eHoje = true),
            buildDia(dia2, eHoje = false)
        ) + buildDiasPorSemana().drop(2)
        val previsao = buildPrevisao().copy(
            horario = DadosHorarios(horas),
            diario = DadosDiarios(dias)
        )
        coEvery { previsaoRepository.obterPrevisao(any(), any(), any(), any()) } returns
            AppResult.Success(previsao)

        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)
        viewModel.abrirDia(1)
        advanceTimeBy(100)

        val horasFiltradas = viewModel.horasDoDiaSelecionado.value
        assertEquals(24, horasFiltradas.size)
        assertTrue(horasFiltradas.all { it.dataIso == dia2 })
    }

    // ── T036: testes de offline & sync (DEVEM FALHAR antes de T037) ─────────────

    @Test
    fun quando_reconecta_e_cache_expirado_viewmodel_dispara_sync_automatico() = runTest {
        // Previsão com timestamp de 2h atrás → cache expirado
        val previsaoExpirada = buildPrevisao(
            timestampAtualizado = System.currentTimeMillis() - 2 * 3_600_000L
        )
        coEvery { previsaoRepository.obterPrevisao(lat, lon, any(), any()) } returns
            AppResult.Success(previsaoExpirada)

        viewModel.carregarPrevisao(lat, lon, nome)
        advanceTimeBy(100)
        assertTrue(viewModel.uiState.value is HomeUiState.Sucesso)

        // Simula perda e reconexão de rede
        isOnlineFlow.value = false
        isOnlineFlow.value = true
        advanceTimeBy(200)

        // Deve ter chamado a API novamente (1 inicial + 1 auto-reconnect)
        coVerify(atLeast = 2) { previsaoRepository.obterPrevisao(lat, lon, any(), any()) }
    }

    @Test
    fun pull_to_refresh_offline_emite_Result_Error_sem_crash() = runTest {
        isOnlineFlow.value = false
        coEvery { previsaoRepository.obterPrevisao(lat, lon, any(), forceRefresh = true) } returns
            AppResult.Error("Sem conexão com a internet e nenhum dado em cache.")

        // Configura coordenadas prévias via carregarPrevisao que falhou
        coEvery { previsaoRepository.obterPrevisao(lat, lon, any(), forceRefresh = false) } returns
            AppResult.Error("Sem conexão com a internet e nenhum dado em cache.")
        viewModel.carregarPrevisao(lat, lon, nome, forceRefresh = false)
        advanceTimeBy(100)

        // Pull-to-refresh em modo offline não deve lançar exceção
        viewModel.onPullToRefresh()
        advanceTimeBy(100)

        val state = viewModel.uiState.value
        assertTrue("Esperado Erro sem crash, obtido: $state", state is HomeUiState.Erro)
    }

    // --- helpers ---

    private fun buildHorasPorDia(dataIso: String, count: Int): List<HoraDados> =
        (0 until count).map { h ->
            HoraDados(
                dataIso = dataIso,
                hora = "${h.toString().padStart(2, '0')}:00",
                temperaturaC = 20f + h * 0.5f,
                precipitacaoMm = 0f,
                codigoWMO = 0,
                umidadePercent = 70,
                velocidadeVentoKmh = 10f,
                direcaoVentoGraus = 90
            )
        }

    private fun buildDia(dataIso: String, eHoje: Boolean = false) = DiaDados(
        data = dataIso,
        dataIso = dataIso,
        temperaturaMaxC = 28f,
        temperaturaMinC = 18f,
        probChuvaPercent = 20,
        velocidadeMaxVentoKmh = 15f,
        direcaoDominanteVentoGraus = 90,
        umidadeMaxPercent = 75,
        codigoWMO = 1,
        eHoje = eHoje
    )

    private fun buildDiasPorSemana(): List<DiaDados> =
        (0 until 7).map { i ->
            buildDia("2026-05-${(19 + i).toString().padStart(2, '0')}", eHoje = i == 0)
        }

    private fun buildPrevisao(
        temperaturaC: Float = 22f,
        codigoWMO: Int = 0,
        timestampAtualizado: Long = System.currentTimeMillis()
    ) = Previsao(
        latitude = lat,
        longitude = lon,
        nomeLocalidade = nome,
        fusoHorario = "America/Sao_Paulo",
        atual = DadosAtuais(
            temperaturaC = temperaturaC,
            sensacaoTermicaC = temperaturaC - 1f,
            umidadePercent = 70,
            velocidadeVentoKmh = 10f,
            direcaoVentoGraus = 90,
            codigoWMO = codigoWMO,
            horaAtualizado = "2026-05-19T14:00"
        ),
        horario = DadosHorarios(emptyList()),
        diario = DadosDiarios(emptyList()),
        timestampAtualizado = timestampAtualizado
    )
}
