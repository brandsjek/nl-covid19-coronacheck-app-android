/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.ctr.holder.persistence.database.usecases

import nl.rijksoverheid.ctr.holder.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.holder.persistence.database.entities.isExpiring

interface GreenCardsUseCase {
    suspend fun needsRefresh(): String?
}

class GreenCardsUseCaseImpl(
    private val holderDatabase: HolderDatabase,
): GreenCardsUseCase {
    override suspend fun needsRefresh() =
        holderDatabase.greenCardDao().getAll().first { greenCard ->
            // TODO before merging
            // according to https://github.com/minvws/nl-covid19-coronacheck-app-coordination/blob/main/architecture/Privacy%20Preserving%20Green%20Card.md#mass-revocation
            // we should also check minimumCredentialVersion from config which is missing now
            greenCard.credentialEntities.maxByOrNull { it.expirationTime }?.isExpiring() ?: true
        }.origins.firstOrNull()?.type.toString()
}
