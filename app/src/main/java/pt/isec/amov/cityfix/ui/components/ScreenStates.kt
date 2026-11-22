package pt.isec.amov.cityfix.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.ui.theme.Dimens

// D-016: the loading, empty and error states every screen shows the same way.

@Composable
fun LoadingState(@StringRes descriptionRes: Int, modifier: Modifier = Modifier) {
    val description = stringResource(descriptionRes)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = description },
        )
    }
}

@Composable
fun EmptyState(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
) {
    MessageState(icon = icon, message = stringResource(messageRes), modifier = modifier)
}

@Composable
fun ErrorState(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    MessageState(
        icon = Icons.Outlined.Warning,
        message = stringResource(messageRes),
        isError = true,
        modifier = modifier,
    ) {
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.location_try_again)) }
        }
    }
}

@Composable
private fun MessageState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    action: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.md),
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.thumbnail)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // Decorative: the message says it all.
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    CityFixTheme { EmptyState(R.string.ticket_list_empty) }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorStatePreview() {
    CityFixTheme { ErrorState(R.string.profile_unavailable, onRetry = {}) }
}
