package com.weather.presentation.theme

import androidx.compose.ui.graphics.Color

/** Paleta cromática do design system Weather App. */
object WeatherColors {
    val Primary = Color(0xFF0288D1)
    val Surface = Color(0xFFFAFAFA)
    val Background = Color(0xFFF0F4F8)
    val OnSurface = Color(0xFF1A1A2E)
    val TempMax = Color(0xFFEF5350)
    val TempMin = Color(0xFF42A5F5)

    // Gradientes diurnos por condição climática
    private val GradientSunny = listOf(Color(0xFFFFF9C4), Color(0xFFFFCC02), Color(0xFFFFA726))
    private val GradientPartlyCloudy = listOf(Color(0xFFE3F2FD), Color(0xFF90CAF9), Color(0xFF42A5F5))
    private val GradientCloudy = listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0xFF78909C))
    private val GradientRainy = listOf(Color(0xFFE3F2FD), Color(0xFF29B6F6), Color(0xFF0277BD))
    private val GradientStorm = listOf(Color(0xFF37474F), Color(0xFF455A64), Color(0xFF263238))
    private val GradientFog = listOf(Color(0xFFF5F5F5), Color(0xFFDDDDDD), Color(0xFFBBBBBB))
    private val GradientSnow = listOf(Color(0xFFF8FBFF), Color(0xFFE3F2FD), Color(0xFFBBDEFB))

    // Gradientes noturnos por condição climática
    private val GradientNightClear = listOf(Color(0xFF0D1B3E), Color(0xFF1A2B5E), Color(0xFF243B7A))
    private val GradientNightPartlyCloudy = listOf(Color(0xFF1A1F3D), Color(0xFF252B50), Color(0xFF303864))
    private val GradientNightCloudy = listOf(Color(0xFF1C1C2A), Color(0xFF252530), Color(0xFF2E2E3A))
    private val GradientNightRainy = listOf(Color(0xFF0A1628), Color(0xFF0D2038), Color(0xFF102A4C))
    private val GradientNightStorm = listOf(Color(0xFF0F0F1A), Color(0xFF161625), Color(0xFF1E1E30))
    private val GradientNightFog = listOf(Color(0xFF1E1E28), Color(0xFF272732), Color(0xFF30303C))
    private val GradientNightSnow = listOf(Color(0xFF1A2438), Color(0xFF1E2E4A), Color(0xFF22385C))

    /**
     * Retorna o gradiente apropriado para um código WMO.
     *
     * Grupos:
     * - 0: ensolarado / céu limpo
     * - 1-3: parcialmente nublado / nublado
     * - 45-48: neblina
     * - 51-67: chuvisco / chuva
     * - 71-77: neve
     * - 80-82: pancadas
     * - 85-86: pancadas de neve
     * - 95-99: tempestade
     *
     * Código nulo ou inválido retorna gradiente de céu claro.
     * [isNight] ativa a paleta noturna (azul-marinho escuro).
     */
    fun gradientForWmoCode(code: Int?, isNight: Boolean = false): List<Color> = when (code) {
        0 -> if (isNight) GradientNightClear else GradientSunny
        1 -> if (isNight) GradientNightPartlyCloudy else GradientPartlyCloudy
        2, 3 -> if (isNight) GradientNightCloudy else GradientCloudy
        45, 48 -> if (isNight) GradientNightFog else GradientFog
        in 51..67 -> if (isNight) GradientNightRainy else GradientRainy
        in 71..77 -> if (isNight) GradientNightSnow else GradientSnow
        in 80..82 -> if (isNight) GradientNightRainy else GradientRainy
        85, 86 -> if (isNight) GradientNightSnow else GradientSnow
        in 95..99 -> if (isNight) GradientNightStorm else GradientStorm
        else -> if (isNight) GradientNightClear else GradientSunny
    }
}
