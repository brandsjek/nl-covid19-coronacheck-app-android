package nl.rijksoverheid.ctr.holder.persistence.database.usecases

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.ctr.appconfig.CachedAppConfigUseCase
import nl.rijksoverheid.ctr.appconfig.api.model.AppConfig
import nl.rijksoverheid.ctr.holder.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.holder.persistence.database.dao.GreenCardDao
import nl.rijksoverheid.ctr.holder.persistence.database.entities.CredentialEntity
import nl.rijksoverheid.ctr.holder.persistence.database.entities.OriginEntity
import nl.rijksoverheid.ctr.holder.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.holder.persistence.database.models.GreenCard
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.GreenCardUtil
import nl.rijksoverheid.ctr.holder.persistence.database.usecases.GreenCard.Expiring as ExpiringGreenCard
import nl.rijksoverheid.ctr.holder.persistence.database.usecases.GreenCard.Valid as ValidGreenCard
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class GreenCardsUseCaseImplTest {
    
    private val greenCardDao = mockk<GreenCardDao>(relaxed = true)
    private val holderDatabase = mockk< HolderDatabase>(relaxed = true).apply { 
        coEvery { greenCardDao() } returns greenCardDao
    }
    private val appConfig = mockk<AppConfig>(relaxed = true)
    private val cachedAppConfigUseCase = mockk<CachedAppConfigUseCase>(relaxed = true).apply { 
        coEvery { getCachedAppConfig() } returns appConfig
    }

    private val greenCardUtil: GreenCardUtil = mockk(relaxed = true)

    private val firstJanuaryClock = Clock.fixed(Instant.parse("2021-01-01T00:00:00.00Z"), ZoneId.of("UTC"))
    private val greenCardUseCase = GreenCardsUseCaseImpl(holderDatabase, cachedAppConfigUseCase, greenCardUtil, firstJanuaryClock)
    
    private fun greenCard(
        originEntities: List<OriginEntity>? = null,
        credentials: List<CredentialEntity>? = null) = mockk<GreenCard>(relaxed = true).apply {
        if (originEntities != null) {
            coEvery { origins } returns originEntities
        }

        if (credentials != null) {
            coEvery { credentialEntities } returns credentials
        }
    }

    private fun credentialEntity(credentialVersion: Int = 2, expireDateTime: String = "2021-01-07T07:00:00.00Z") = CredentialEntity(1L, 1L, "".toByteArray(), credentialVersion, OffsetDateTime.now(), OffsetDateTime.ofInstant(
        Instant.parse(expireDateTime),
        ZoneId.of("UTC")
    ))

    private fun validOriginEntity() = OriginEntity(1, 1L, OriginType.Test, OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now())

    private fun validGreenCard(expireDateTime1: String = "2021-01-07T07:00:00.00Z",
                               expireDateTime2: String = "2021-01-07T07:00:00.00Z") = greenCard(
        originEntities = listOf(validOriginEntity()),
        credentials = listOf(credentialEntity(expireDateTime = expireDateTime1),
            credentialEntity(expireDateTime = expireDateTime2))
    )

    private fun expiringGreenCard(expireDateTime1: String = "2021-01-03T07:00:00.00Z",
                                  expireDateTime2: String = "2021-01-03T07:00:00.00Z") = greenCard(
        originEntities = listOf(validOriginEntity()),
        credentials = listOf(credentialEntity(expireDateTime = expireDateTime1),
            credentialEntity(expireDateTime = expireDateTime2))
    )

    private fun unsupportedGreenCard() = greenCard(
        originEntities = listOf(validOriginEntity()),
        credentials = listOf(credentialEntity(credentialVersion = 1), credentialEntity())
    )

    @Test
    fun `given two green cards with some credentials, when both green cards do not expire, then return null (no refresh)`() = runBlocking {
        coEvery { appConfig.minimumCredentialVersion } returns 2
        coEvery { greenCardDao.getAll() } returns listOf(validGreenCard(), validGreenCard())

        val expiring = greenCardUseCase.expiring()

        assertFalse(expiring)
    }

    @Test
    fun `given two green cards with some credentials, when a green card expires and the other green card does not expire, then return an expiring origin type to refresh`() = runBlocking {
        coEvery { appConfig.minimumCredentialVersion } returns 2
        coEvery { greenCardDao.getAll() } returns listOf(validGreenCard(), expiringGreenCard())

        val expiring = greenCardUseCase.expiring()

        assertTrue(expiring)
    }

    @Test
    fun `given a green card with some credentials, when all credentials expire, then return the expiring origin type to refresh`() = runBlocking {
        coEvery { appConfig.minimumCredentialVersion } returns 2
        coEvery { greenCardDao.getAll() } returns listOf(expiringGreenCard(), expiringGreenCard())

        val expiring = greenCardUseCase.expiring()

        assertTrue(expiring)
    }

    fun `given a green card with some credentials, when the last credential does not expire, then return null (no refresh)`() = runBlocking {

    }

    fun `given a green card with no credentials, then return an origin type to refresh`() = runBlocking {
        
    }

    @Test
    fun `given two green cards with some credentials, when a credential version is not supported anymore, then return an origin type to refresh`() = runBlocking {
        coEvery { appConfig.minimumCredentialVersion } returns 2
        coEvery { greenCardDao.getAll() } returns listOf(validGreenCard(), unsupportedGreenCard())

        val expiring = greenCardUseCase.expiring()

        assertTrue(expiring)
    }

    fun `given a green card with some credentials, when all credential versions are invalid, then return an origin type to refresh`() = runBlocking {

    }

    @Test
    fun `given a green card with some credentials, when all credential versions are still supported, then return null (no refresh)`() = runBlocking {
        coEvery { appConfig.minimumCredentialVersion } returns 2
        coEvery { greenCardDao.getAll() } returns listOf(validGreenCard())

        val expiring = greenCardUseCase.expiring()

        assertFalse(expiring)
    }

    @Test
    fun `given a green with credential expiring in 7 days after now, when checking the last expiring one, then it doesn't return any credentials needing refresh`() = runBlocking {
        coEvery { greenCardDao.getAll() } returns listOf(validGreenCard("2021-01-08T07:00:00.00Z"))
        coEvery { appConfig.credentialRenewalDays } returns 5

        val lastExpiringCard = greenCardUseCase.lastExpiringCard()

        assertTrue(lastExpiringCard is ValidGreenCard)
    }

    @Test
    fun `given a green with credential expiring 6 days after now, when checking the last expiring one, then it returns it with correct refreshInDays`() = runBlocking {

        coEvery { greenCardDao.getAll() } returns listOf(validGreenCard())
        coEvery { appConfig.credentialRenewalDays } returns 5

        val lastExpiringCard = greenCardUseCase.lastExpiringCard()

        assertEquals(1, (lastExpiringCard as ExpiringGreenCard).refreshInDays)
    }

    @Test
    fun `given a green with credential expiring today, when checking the last expiring one, then it returns it with refreshInDays set to 1`() = runBlocking {
        coEvery { greenCardDao.getAll() } returns listOf(expiringGreenCard())
        coEvery { appConfig.credentialRenewalDays } returns 5

        val lastExpiringCard = greenCardUseCase.lastExpiringCard()

        assertEquals(1, (lastExpiringCard as ExpiringGreenCard).refreshInDays)
    }
}