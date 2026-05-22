package com.weather.search

import com.weather.domain.model.CidadeSugestao
import com.weather.domain.model.HistoricoBusca
import com.weather.domain.repository.IBuscaRepository
import com.weather.domain.usecase.BuscarCidadesUseCase
import com.weather.presentation.search.SearchUiState
import com.weather.presentation.search.SearchViewModel
import com.weather.utils.AppResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes TDD do [SearchViewModel].
 *
 * Escritos antes da implementação — DEVEM FALHAR até T034 ser concluída.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private lateinit var buscaRepository: IBuscaRepository
    private lateinit var buscarCidadesUseCase: BuscarCidadesUseCase
    private lateinit var viewModel: SearchViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    private val cidadeSP = CidadeSugestao(
        nome = "São Paulo",
        estado = "São Paulo",
        pais = "Brasil",
        latitude = -23.55,
        longitude = -46.63,
        fusoHorario = "America/Sao_Paulo"
    )
    private val cidadeRJ = CidadeSugestao(
        nome = "Rio de Janeiro",
        estado = "Rio de Janeiro",
        pais = "Brasil",
        latitude = -22.90,
        longitude = -43.17,
        fusoHorario = "America/Sao_Paulo"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        buscaRepository = mockk()
        coEvery { buscaRepository.obterHistorico() } returns emptyList()
        coEvery { buscaRepository.salvarNoBusca(any()) } returns Unit
        buscarCidadesUseCase = BuscarCidadesUseCase(buscaRepository)
        viewModel = SearchViewModel(buscarCidadesUseCase, buscaRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun debounce_500ms_dispara_exatamente_uma_requisicao_ao_digitar_rapido() = runTest {
        coEvery { buscaRepository.buscarCidades(any()) } returns AppResult.Success(listOf(cidadeSP))

        // Digitação rápida — 4 chars em menos de 500ms
        viewModel.onQueryChange("S")
        advanceTimeBy(100)
        viewModel.onQueryChange("Sã")
        advanceTimeBy(100)
        viewModel.onQueryChange("São")
        advanceTimeBy(100)
        viewModel.onQueryChange("São ")
        // Aguarda o debounce disparar
        advanceTimeBy(600)

        // Apenas uma chamada à API, com a query final
        coVerify(exactly = 1) { buscaRepository.buscarCidades("São ") }
    }

    @Test
    fun busca_valida_emite_SearchUiState_Resultados_com_lista() = runTest {
        coEvery { buscaRepository.buscarCidades("São Paulo") } returns
            AppResult.Success(listOf(cidadeSP, cidadeRJ))
        coEvery { buscaRepository.obterHistorico() } returns emptyList()

        viewModel.onQueryChange("São Paulo")
        advanceTimeBy(600)

        val state = viewModel.uiState.value
        assertTrue("Esperado Resultados, obtido: $state", state is SearchUiState.Resultados)
        val resultados = state as SearchUiState.Resultados
        assertEquals(2, resultados.sugestoes.size)
        assertEquals("São Paulo", resultados.sugestoes.first().nome)
    }

    @Test
    fun selecionar_cidade_adiciona_ao_historico_e_emite_SharedFlow() = runTest {
        coEvery { buscaRepository.salvarNoBusca(cidadeSP) } returns Unit
        coEvery { buscaRepository.obterHistorico() } returns emptyList()

        var eventoRecebido: CidadeSugestao? = null
        val job = launch {
            eventoRecebido = viewModel.cidadeSelecionadaEvent.first()
        }

        viewModel.selecionarCidade(cidadeSP)
        advanceTimeBy(100)
        job.cancel()

        coVerify(exactly = 1) { buscaRepository.salvarNoBusca(cidadeSP) }
        assertEquals(cidadeSP, eventoRecebido)
    }

    @Test
    fun historico_exibe_apenas_as_5_mais_recentes() = runTest {
        val historico5 = (1..5).map { i ->
            HistoricoBusca(
                nomeCidade = "Cidade $i",
                estado = null,
                pais = "Brasil",
                latitude = -23.0 + i,
                longitude = -46.0 + i,
                buscadoEm = System.currentTimeMillis() - i * 1000L
            )
        }
        coEvery { buscaRepository.obterHistorico() } returns historico5

        viewModel.carregarHistorico()
        advanceTimeBy(100)

        val state = viewModel.uiState.value
        assertTrue("Esperado Resultados, obtido: $state", state is SearchUiState.Resultados)
        val resultados = state as SearchUiState.Resultados
        assertEquals(5, resultados.historico.size)
    }

    @Test
    fun cidade_duplicada_no_historico_move_para_o_topo() = runTest {
        val historicoAtualizado = listOf(
            HistoricoBusca(
                nomeCidade = cidadeSP.nome,
                estado = cidadeSP.estado,
                pais = cidadeSP.pais,
                latitude = cidadeSP.latitude,
                longitude = cidadeSP.longitude,
                buscadoEm = System.currentTimeMillis()
            ),
            HistoricoBusca(
                nomeCidade = cidadeRJ.nome,
                estado = cidadeRJ.estado,
                pais = cidadeRJ.pais,
                latitude = cidadeRJ.latitude,
                longitude = cidadeRJ.longitude,
                buscadoEm = System.currentTimeMillis() - 10_000L
            )
        )
        coEvery { buscaRepository.salvarNoBusca(cidadeSP) } returns Unit
        coEvery { buscaRepository.obterHistorico() } returns historicoAtualizado

        viewModel.selecionarCidade(cidadeSP)
        viewModel.carregarHistorico()
        advanceTimeBy(100)

        val state = viewModel.uiState.value
        val resultados = state as SearchUiState.Resultados
        assertEquals(cidadeSP.nome, resultados.historico.first().nomeCidade)
    }
}
