package com.weather

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.weather.data.worker.LimpezaCacheWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Ponto de entrada da aplicação.
 *
 * Responsabilidades:
 * - Inicializar Timber (logging) apenas em builds de debug
 * - Configurar Firebase Crashlytics (ativo apenas em release)
 * - Prover configuração do WorkManager para integração com Hilt
 *
 * LeakCanary é habilitado automaticamente via `debugImplementation` — sem código adicional.
 */
@HiltAndroidApp
class WeatherApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        initLogging()
        agendarLimpezaCache()
    }

    /** Habilita logs detalhados apenas em debug — nunca em release. */
    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    /**
     * Agenda limpeza semanal de entradas de cache com mais de 7 dias via WorkManager.
     *
     * [ExistingPeriodicWorkPolicy.KEEP] garante que um agendamento existente não seja substituído
     * em cada restart do processo, evitando reset acidental do intervalo.
     */
    private fun agendarLimpezaCache() {
        val request = PeriodicWorkRequestBuilder<LimpezaCacheWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cache_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Configuração do WorkManager com suporte a Hilt Workers (@HiltWorker).
     * Necessário para que LimpezaCacheWorker (T037) receba injeção de dependência.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
