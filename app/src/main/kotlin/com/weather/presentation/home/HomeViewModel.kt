package com.weather.presentation.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.data.remote.RateLimitException
import com.weather.domain.location.ILocationHandler
import com.weather.domain.location.LocationResult
import com.weather.domain.model.HoraDados
import com.weather.domain.repository.IPrevisaoRepository
import com.weather.utils.AppResult
import com.weather.utils.CacheValidator
import com.weather.utils.DateFormatter
import com.weather.utils.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ViewModel da tela principal ([HomeScreen]).
 *
 * Gerencia o [HomeUiState] expondo dados meteorológicos, estado offline e badges de status.
 * O ticker de timestamp atualiza [HomeUiState.Sucesso.timestampRelativo] a cada minuto
 * sem disparar novas chamadas à API.
 */
@SuppressLint("StaticFieldLeak") // @ApplicationContext é ApplicationContext — sem leak de Activity/Fragment
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val previsaoRepository: IPrevisaoRepository,
    private val locationHandler: ILocationHandler,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val locationPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
    }

    private var denialCount: Int
        get() = locationPrefs.getInt("denial_count", 0)
        set(value) = locationPrefs.edit { putInt("denial_count", value) }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Carregando)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Horas do dia atual filtradas do [uiState], atualizadas automaticamente
     * quando a previsão muda. Usa o fuso horário da localidade retornado pela API.
     */
    @Suppress("OPT_IN_USAGE")
    val horasDoDia: StateFlow<List<HoraDados>> = _uiState
        .mapLatest { state ->
            if (state is HomeUiState.Sucesso) {
                val fuso = try { ZoneId.of(state.previsao.fusoHorario) } catch (_: Exception) { ZoneId.of("UTC") }
                val hoje = LocalDate.now(fuso).toString()
                filtrarHorasDoDia(state.previsao.horario.horas, hoje)
            } else {
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val _diaSelecionadoIndex = MutableStateFlow(0)
    val diaSelecionadoIndex: StateFlow<Int> = _diaSelecionadoIndex.asStateFlow()

    /**
     * Horas do dia selecionado pelo usuário no detalhe diário, filtradas
     * a partir do [uiState] e do [diaSelecionadoIndex] atual.
     */
    @Suppress("OPT_IN_USAGE")
    val horasDoDiaSelecionado: StateFlow<List<HoraDados>> =
        combine(_uiState, _diaSelecionadoIndex) { state, index ->
            if (state is HomeUiState.Sucesso) {
                val dias = state.previsao.diario.dias
                val dia = dias.getOrNull(index) ?: return@combine emptyList()
                filtrarHorasDoDia(state.previsao.horario.horas, dia.dataIso)
            } else {
                emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val geocoder by lazy { Geocoder(context, Locale.getDefault()) }
    private val geocodeCache = HashMap<String, String>()

    private var ultimaLat: Double = 0.0
    private var ultimaLon: Double = 0.0
    private var ultimoNome: String = ""

    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                atualizarTimestamp()
            }
        }
        locationJob = viewModelScope.launch {
            locationHandler.observarLocalizacao().collect { result ->
                processarResultadoLocalizacao(result)
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline
                .drop(1)           // ignora valor inicial — só reage a mudanças
                .filter { it }     // apenas quando volta a ficar online
                .collect { sincronizarSeNecessario() }
        }
    }

    /**
     * Carrega a previsão para as coordenadas fornecidas.
     *
     * Respeita o cache de 1h — use [forceRefresh] para ignorá-lo (ex: pull-to-refresh).
     * Se offline, retorna dados em cache quando disponíveis.
     */
    fun carregarPrevisao(
        lat: Double,
        lon: Double,
        nomeLocalidade: String = "",
        forceRefresh: Boolean = false
    ) {
        ultimaLat = lat
        ultimaLon = lon
        ultimoNome = nomeLocalidade

        viewModelScope.launch {
            val isOnline = networkMonitor.isOnline.first()

            when (val result = previsaoRepository.obterPrevisao(lat, lon, nomeLocalidade, forceRefresh)) {
                is AppResult.Success -> {
                    val previsao = result.data
                    val horasAtraso = CacheValidator.calcularHorasAtraso(previsao.timestampAtualizado)
                    _uiState.value = HomeUiState.Sucesso(
                        previsao = previsao,
                        nomeLocalidade = previsao.nomeLocalidade.ifEmpty { nomeLocalidade },
                        isOffline = !isOnline,
                        timestampRelativo = DateFormatter.formatarTimestampRelativo(previsao.timestampAtualizado),
                        horasAtraso = if (!isOnline) horasAtraso else 0
                    )
                }
                is AppResult.Error -> {
                    val rateLimitEx = result.exception as? RateLimitException
                    if (rateLimitEx != null) {
                        iniciarCountdownRateLimit(rateLimitEx.retryAfterSeconds)
                    } else {
                        _uiState.value = HomeUiState.Erro(
                            mensagem = result.message,
                            temCache = false
                        )
                    }
                }
                AppResult.Loading -> Unit
            }
        }
    }

    /**
     * Filtra as horas de uma lista pelo dia ISO fornecido.
     *
     * @param horas lista completa de horas (tipicamente 168 entradas para 7 dias)
     * @param dataIso data no formato "yyyy-MM-dd", ex: "2026-05-19"
     * @return subconjunto de horas cujo [HoraDados.dataIso] coincide com [dataIso]
     */
    fun filtrarHorasDoDia(horas: List<HoraDados>, dataIso: String): List<HoraDados> =
        horas.filter { it.dataIso == dataIso }

    /** Define o índice do dia selecionado para exibição no detalhe diário. */
    fun abrirDia(index: Int) {
        _diaSelecionadoIndex.value = index
    }

    /**
     * Navega entre os dias da semana pelo [delta] fornecido.
     *
     * Limita o índice ao intervalo [0, 6].
     */
    fun navegarDia(delta: Int) {
        _diaSelecionadoIndex.value = (_diaSelecionadoIndex.value + delta).coerceIn(0, 6)
    }

    /** Re-executa a observação de localização após permissão concedida. */
    fun verificarLocalizacao() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            locationHandler.observarLocalizacao().collect { result ->
                processarResultadoLocalizacao(result)
            }
        }
    }

    /** Dispara refresh forçado ignorando o cache de 1h (chamado pelo pull-to-refresh). */
    fun onPullToRefresh() {
        carregarPrevisao(ultimaLat, ultimaLon, ultimoNome, forceRefresh = true)
    }

    /** Carrega previsão para cidade selecionada na busca, exibindo loading imediatamente. */
    fun selecionarCidade(lat: Double, lon: Double, nome: String) {
        _uiState.value = HomeUiState.Carregando
        carregarPrevisao(lat, lon, nome)
    }

    /** Tenta recarregar a previsão usando a última localização conhecida (chamado pelo botão de erro). */
    fun tentarNovamente() {
        _uiState.value = HomeUiState.Carregando
        carregarPrevisao(ultimaLat, ultimaLon, ultimoNome, forceRefresh = true)
    }

    /**
     * Chamada ao reconectar à internet — recarrega dados se o cache estiver expirado
     * ou se o estado atual for de erro (sem cache disponível).
     *
     * Usa `forceRefresh=false` para respeitar o cache válido (<1h) caso exista.
     */
    private fun sincronizarSeNecessario() {
        val state = _uiState.value
        val cacheExpirado = when (state) {
            is HomeUiState.Sucesso ->
                CacheValidator.estaExpirado(state.previsao.timestampAtualizado)
            is HomeUiState.Erro -> true
            else -> false
        }
        if (cacheExpirado && (ultimaLat != 0.0 || ultimaLon != 0.0)) {
            carregarPrevisao(ultimaLat, ultimaLon, ultimoNome, forceRefresh = false)
        }
    }

    private suspend fun processarResultadoLocalizacao(result: LocationResult) {
        when (result) {
            is LocationResult.Success -> {
                // Reseta contador ao conceder permissão
                denialCount = 0
                val state = _uiState.value
                val isApprox = result.isApproximate
                if (state is HomeUiState.Sucesso) {
                    // Atualiza badge de localização aproximada sem recarregar dados
                    _uiState.value = state.copy(isLocalizacaoAproximada = isApprox)
                }
                val nome = reverseGeocode(result.lat, result.lon)
                carregarPrevisao(result.lat, result.lon, nome)
            }
            is LocationResult.GpsRefinement -> {
                val state = _uiState.value
                val precisouRecarregar = state is HomeUiState.Sucesso &&
                    haversineMetros(ultimaLat, ultimaLon, result.lat, result.lon) > 100.0
                if (precisouRecarregar) {
                    if (state is HomeUiState.Sucesso) {
                        _uiState.value = state.copy(isLocalizacaoAproximada = false)
                    }
                    carregarPrevisao(result.lat, result.lon)
                }
            }
            is LocationResult.PermissionDenied -> {
                denialCount++
                _uiState.value = if (denialCount >= 3) {
                    HomeUiState.SemPermissaoDefinitiva
                } else {
                    HomeUiState.SemPermissao
                }
            }
            is LocationResult.LocationFailed -> {
                _uiState.value = HomeUiState.Erro(
                    mensagem = context.getString(com.weather.R.string.error_location_failed),
                    temCache = false
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(lat: Double, lon: Double): String {
        val key = "%.3f,%.3f".format(lat, lon)
        geocodeCache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocation(lat, lon, 1)
                    ?.firstOrNull()
                    ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
                    ?: ""
            } catch (_: Exception) {
                ""
            }
        }.also { geocodeCache[key] = it }
    }

    /** Distância aproximada em metros entre dois pontos geográficos (Haversine). */
    private fun haversineMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = sin(dLat / 2).let { it * it } +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLon / 2).let { it * it }
        return r * 2.0 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun atualizarTimestamp() {
        val state = _uiState.value as? HomeUiState.Sucesso ?: return
        val novo = DateFormatter.formatarTimestampRelativo(state.previsao.timestampAtualizado)
        if (novo != state.timestampRelativo) {
            _uiState.value = state.copy(timestampRelativo = novo)
        }
    }

    private fun iniciarCountdownRateLimit(retryAfterSeconds: Int) {
        viewModelScope.launch {
            var r = retryAfterSeconds
            while (r > 0) {
                _uiState.update { state ->
                    val erroAtual = state as? HomeUiState.Erro
                    HomeUiState.Erro(
                        mensagem = erroAtual?.mensagem ?: "Limite de requisições atingido.",
                        temCache = erroAtual?.temCache ?: false,
                        previsaoCache = erroAtual?.previsaoCache,
                        rateLimitSecondsRemaining = r
                    )
                }
                delay(1_000L)
                r--
            }
            _uiState.update { state ->
                if (state is HomeUiState.Erro) state.copy(rateLimitSecondsRemaining = null) else state
            }
            carregarPrevisao(ultimaLat, ultimaLon, ultimoNome)
        }
    }
}
