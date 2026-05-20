package com.weather.data.location

import android.location.Location

/**
 * Abstração sobre [com.google.android.gms.location.FusedLocationProviderClient]
 * que permite testes unitários sem depender de Tasks do Play Services.
 */
interface ILocationSource {
    /** Retorna a última localização conhecida (Network/Passive), ou null se indisponível. */
    suspend fun ultimaLocalizacao(): Location?

    /** Solicita uma localização fresca de alta precisão (GPS). */
    suspend fun localizacaoAtual(): Location?
}
