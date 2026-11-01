package pt.isec.amov.cityfix.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/** The screen the top bar is showing. One value per route in Destinations.kt. */
enum class TopBarScreen { DASHBOARD, LIST, DETAIL, REPORT }

/**
 * App top bar. Stateless: the caller says which screen is showing and receives the clicks.
 * The toggle appears on the list only; the add button everywhere except the report form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    screen: TopBarScreen,
    canNavigateBack: Boolean,
    isExpanded: Boolean,
    onBackClick: () -> Unit,
    onToggleExpandedClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRes = when (screen) {
        TopBarScreen.DASHBOARD -> R.string.top_bar_title_dashboard
        TopBarScreen.LIST -> R.string.top_bar_title_list
        TopBarScreen.DETAIL -> R.string.top_bar_title_detail
        TopBarScreen.REPORT -> R.string.top_bar_title_report
    }

    TopAppBar(
        title = {
            Text(
                text = stringResource(titleRes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_navigate_back),
                    )
                }
            }
        },
        actions = {
            if (screen == TopBarScreen.LIST) {
                IconButton(onClick = onToggleExpandedClick) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Filled.KeyboardArrowUp
                        } else {
                            Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = stringResource(
                            if (isExpanded) R.string.cd_collapse_tickets else R.string.cd_expand_tickets
                        ),
                    )
                }
            }
            if (screen != TopBarScreen.REPORT) {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_report_ticket),
                    )
                }
            }
        },
    )
}

private const val PORTRAIT = "spec:width=411dp,height=891dp"
private const val LANDSCAPE = "spec:width=411dp,height=891dp,orientation=landscape"

@Preview(name = "Portrait", device = PORTRAIT, showBackground = true)
@Preview(name = "Landscape", device = LANDSCAPE, showBackground = true)
private annotation class OrientationPreviews

@Composable
private fun BarPreview(
    screen: TopBarScreen,
    canNavigateBack: Boolean,
    isExpanded: Boolean = false,
) {
    CityFixTheme {
        MainTopAppBar(
            screen = screen,
            canNavigateBack = canNavigateBack,
            isExpanded = isExpanded,
            onBackClick = {},
            onToggleExpandedClick = {},
            onAddClick = {},
        )
    }
}

@OrientationPreviews
@Composable
private fun DashboardBarPreview() {
    BarPreview(TopBarScreen.DASHBOARD, canNavigateBack = false)
}

@OrientationPreviews
@Composable
private fun ListCollapsedBarPreview() {
    BarPreview(TopBarScreen.LIST, canNavigateBack = true)
}

@OrientationPreviews
@Composable
private fun ListExpandedBarPreview() {
    BarPreview(TopBarScreen.LIST, canNavigateBack = true, isExpanded = true)
}

@OrientationPreviews
@Composable
private fun DetailBarPreview() {
    BarPreview(TopBarScreen.DETAIL, canNavigateBack = true)
}

@OrientationPreviews
@Composable
private fun ReportBarPreview() {
    BarPreview(TopBarScreen.REPORT, canNavigateBack = true)
}
