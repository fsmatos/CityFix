package pt.isec.amov.cityfix.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.ui.components.CityCard
import pt.isec.amov.cityfix.ui.navigation.LocalBottomBarPadding
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.ui.theme.Dimens
import java.text.NumberFormat

private const val HOUR_MILLIS = 3_600_000L
private const val DAY_MILLIS = 86_400_000L

/**
 * Anonymous dashboard (brief I.1.A): average repair time, total issues and % active issues.
 * Formats the given DashboardStats only; the figures are computed upstream (TicketsList.stats())
 * and their contract (activeRatio never NaN, null average when nothing is resolved) is trusted.
 */
@Composable
fun DashboardScreen(
    stats: DashboardStats,
    onSeeTicketsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val integerFormat = NumberFormat.getIntegerInstance(locale)

    val total = integerFormat.format(stats.totalTickets)
    val percent = NumberFormat.getPercentInstance(locale).format(stats.activeRatio)
    val average = repairTimeText(stats.averageRepairTimeMillis)
    val basedOn = if (stats.averageRepairTimeMillis != null) {
        pluralStringResource(
            R.plurals.dashboard_based_on_repairs,
            stats.resolvedTickets,
            stats.resolvedTickets,
        )
    } else {
        null
    }
    val activeOfTotal = if (stats.totalTickets > 0) {
        pluralStringResource(
            R.plurals.dashboard_active_of_total,
            stats.totalTickets,
            integerFormat.format(stats.activeTickets),
            integerFormat.format(stats.totalTickets),
        )
    } else {
        null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            // Room to scroll above the floating island and the report button (D-016).
            .padding(bottom = LocalBottomBarPadding.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isLandscape) {
            // One row of equal-width cards; IntrinsicSize.Min + fillMaxHeight gives them equal heights.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Dimens.md),
            ) {
                StatCard(
                    label = stringResource(R.string.dashboard_repair_time_label),
                    value = average,
                    supporting = basedOn,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                StatCard(
                    label = stringResource(R.string.dashboard_total_label),
                    value = total,
                    supporting = null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                StatCard(
                    label = stringResource(R.string.dashboard_active_label),
                    value = percent,
                    supporting = activeOfTotal,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        } else {
            StatCard(
                label = stringResource(R.string.dashboard_repair_time_label),
                value = average,
                supporting = basedOn,
                modifier = Modifier.fillMaxWidth(),
            )
            StatCard(
                label = stringResource(R.string.dashboard_total_label),
                value = total,
                supporting = null,
                modifier = Modifier.fillMaxWidth(),
            )
            StatCard(
                label = stringResource(R.string.dashboard_active_label),
                value = percent,
                supporting = activeOfTotal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Neutral: on this screen the accent is the report button (D-016).
        FilledTonalButton(
            onClick = onSeeTicketsClick,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.dashboard_see_tickets))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    supporting: String?,
    modifier: Modifier = Modifier,
) {
    CityCard(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Column(
            modifier = Modifier.padding(Dimens.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
            )
            supporting?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Average repair time as whole days and hours (both truncated), or "No repairs yet" when null. */
@Composable
private fun repairTimeText(millis: Long?): String {
    if (millis == null) return stringResource(R.string.dashboard_repair_time_none)

    val days = (millis / DAY_MILLIS).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val hours = ((millis % DAY_MILLIS) / HOUR_MILLIS).toInt()

    return when {
        days == 0 && hours == 0 -> stringResource(R.string.dashboard_duration_less_than_hour)
        hours == 0 -> pluralStringResource(R.plurals.dashboard_days, days, days)
        days == 0 -> pluralStringResource(R.plurals.dashboard_hours, hours, hours)
        else -> stringResource(
            R.string.dashboard_duration_days_hours,
            pluralStringResource(R.plurals.dashboard_days, days, days),
            pluralStringResource(R.plurals.dashboard_hours, hours, hours),
        )
    }
}

private val previewStats = DashboardStats(
    totalTickets = 30,
    activeTickets = 12,
    resolvedTickets = 8,
    averageRepairTimeMillis = 3 * DAY_MILLIS + 4 * HOUR_MILLIS,
)

private val sampleFigureStats = DashboardStats(
    totalTickets = 3,
    activeTickets = 2,
    resolvedTickets = 1,
    averageRepairTimeMillis = 3 * DAY_MILLIS,
)

private val singleTicketStats = DashboardStats(
    totalTickets = 1,
    activeTickets = 0,
    resolvedTickets = 1,
    averageRepairTimeMillis = HOUR_MILLIS / 2,
)

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun DashboardScreenPreview() {
    CityFixTheme {
        DashboardScreen(previewStats, onSeeTicketsClick = {})
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun DashboardScreenEmptyPreview() {
    CityFixTheme {
        DashboardScreen(DashboardStats(), onSeeTicketsClick = {})
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun DashboardScreenSampleFiguresPreview() {
    CityFixTheme {
        DashboardScreen(sampleFigureStats, onSeeTicketsClick = {})
    }
}

/** Singular plurals and the shortest duration: "Less than 1 hour", "Based on 1 repair", "0 of 1 issue". */
@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun DashboardScreenSingleTicketPreview() {
    CityFixTheme {
        DashboardScreen(singleTicketStats, onSeeTicketsClick = {})
    }
}
