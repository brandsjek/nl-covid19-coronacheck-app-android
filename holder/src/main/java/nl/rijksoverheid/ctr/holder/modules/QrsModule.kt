package nl.rijksoverheid.ctr.holder.modules

import nl.rijksoverheid.ctr.holder.ui.create_qr.usecases.*
import org.koin.dsl.module

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
val qrsModule = module {
    factory<QrCodeDataUseCase> { QrCodeDataUseCaseImpl(get(), get(), get()) }

    factory<GetMyOverviewItemsUseCase> {
        GetMyOverviewItemsUseCaseImpl(get(), get(), get(), get())
    }

    factory {
        TokenQrUseCase(get())
    }
}
