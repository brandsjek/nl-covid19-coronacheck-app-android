package nl.rijksoverheid.ctr.holder.persistence.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import nl.rijksoverheid.ctr.holder.persistence.database.converters.HolderDatabaseConverter
import nl.rijksoverheid.ctr.holder.persistence.database.dao.*
import nl.rijksoverheid.ctr.holder.persistence.database.entities.*

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
@Database(
    entities = [WalletEntity::class, EventGroupEntity::class, GreenCardEntity::class, CredentialEntity::class, OriginEntity::class],
    version = 1
)
@TypeConverters(HolderDatabaseConverter::class)
abstract class HolderDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun greenCardDao(): GreenCardDao
    abstract fun credentialDao(): CredentialDao
    abstract fun eventGroupDao(): EventGroupDao
    abstract fun originDao(): OriginDao
}
