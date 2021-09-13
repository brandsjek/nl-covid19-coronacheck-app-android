package nl.rijksoverheid.ctr.holder.ui.myoverview.models

import androidx.annotation.StringRes
import nl.rijksoverheid.ctr.holder.ui.create_qr.usecases.MyOverviewItem

data class DashboardTabItem(
    @StringRes val title: Int,
    val items: List<MyOverviewItem>
)