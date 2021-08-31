package nl.rijksoverheid.ctr.appconfig.usecases

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.rijksoverheid.ctr.appconfig.api.model.AppConfig
import nl.rijksoverheid.ctr.appconfig.api.model.HolderConfig
import nl.rijksoverheid.ctr.appconfig.api.model.VerifierConfig
import nl.rijksoverheid.ctr.appconfig.models.AppStatus
import nl.rijksoverheid.ctr.appconfig.models.ConfigResult
import nl.rijksoverheid.ctr.appconfig.persistence.AppConfigPersistenceManager
import nl.rijksoverheid.ctr.appconfig.persistence.RecommendedUpdatePersistenceManager
import nl.rijksoverheid.ctr.shared.ext.toObject
import java.time.Clock
import java.time.OffsetDateTime

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
interface AppStatusUseCase {
    suspend fun get(config: ConfigResult, currentVersionCode: Int): AppStatus
}

class AppStatusUseCaseImpl(
    private val clock: Clock,
    private val cachedAppConfigUseCase: CachedAppConfigUseCase,
    private val appConfigPersistenceManager: AppConfigPersistenceManager,
    private val recommendedUpdatePersistenceManager: RecommendedUpdatePersistenceManager,
    private val moshi: Moshi,
    private val isVerifierApp: Boolean,
) :
    AppStatusUseCase {

    companion object {
        private const val SECONDS_IN_HOUR = 3600
        private const val MS_IN_SECONDS = 1000
    }

    override suspend fun get(config: ConfigResult, currentVersionCode: Int): AppStatus =
        withContext(Dispatchers.IO) {
            when (config) {
                is ConfigResult.Success -> {
                    checkIfActionRequired(
                        currentVersionCode = currentVersionCode,
                        appConfig = if (isVerifierApp) {
                            config.appConfig.toObject<VerifierConfig>(moshi)
                        } else {
                            config.appConfig.toObject<HolderConfig>(moshi)
                        }
                    )
                }
                is ConfigResult.Error -> {
                    val cachedAppConfig = cachedAppConfigUseCase.getCachedAppConfig()
                    if (appConfigPersistenceManager.getAppConfigLastFetchedSeconds() + cachedAppConfig.configTtlSeconds
                        >= OffsetDateTime.now(clock).toEpochSecond()
                    ) {
                        checkIfActionRequired(
                            currentVersionCode = currentVersionCode,
                            appConfig = cachedAppConfig
                        )
                    } else {
                        AppStatus.Error
                    }
                }
            }
        }

    private fun checkIfActionRequired(currentVersionCode: Int, appConfig: AppConfig): AppStatus {
        return when {
            appConfig.appDeactivated -> AppStatus.Deactivated(appConfig.informationURL)
            // currentVersionCode < appConfig.minimumVersion -> AppStatus.UpdateRequired
            // currentVersionCode < appConfig.recommendedVersion -> getUpdateRecommendedStatus(appConfig)
            else -> AppStatus.NoActionRequired
        }
    }

    private fun getUpdateRecommendedStatus(appConfig: AppConfig): AppStatus {
        val localTime = clock.instant().toEpochMilli() / MS_IN_SECONDS
        val updateLastShown = recommendedUpdatePersistenceManager.getRecommendedUpdateShownSeconds()
        val updateIntervalSeconds = appConfig.recommendedUpgradeIntervalHours * SECONDS_IN_HOUR

        return if (localTime > updateLastShown + updateIntervalSeconds) {
            recommendedUpdatePersistenceManager.saveRecommendedUpdateShownSeconds(localTime)
            AppStatus.UpdateRecommended
        } else {
            AppStatus.NoActionRequired
        }
    }
}
