package pt.isec.amov.cityfix.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.components.LocationDeniedMessage
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.openAppSettings
import pt.isec.amov.cityfix.viewmodel.GateStatus
import pt.isec.amov.cityfix.viewmodel.LocationGateUiState
import pt.isec.amov.cityfix.viewmodel.LocationGateViewModel

/**
 * Stateful half of the 10 km gate (brief I.1), shown instead of the app while an anonymous user is
 * not known to be inside. [onTryAgainClick] asks for the permission again (MainActivity owns the
 * launcher); its answer reaches [viewModel], which reads the location again. [onLoginClick] opens
 * the login (D-009); when null the button is not shown.
 */
@Composable
fun LocationGateScreen(
    viewModel: LocationGateViewModel,
    onTryAgainClick: () -> Unit,
    onLoginClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LocationGateContent(
        state = state,
        onTryAgainClick = onTryAgainClick,
        onOpenSettingsClick = { openAppSettings(context) },
        onLoginClick = onLoginClick,
        modifier = modifier,
    )
}

/** Stateless half of the gate: one message per [LocationGateUiState.status]; previews target it. */
@Composable
fun LocationGateContent(
    state: LocationGateUiState,
    onTryAgainClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onLoginClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Scrolls so it fits in landscape and with large fonts.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        when (state.status) {
            GateStatus.CHECKING, GateStatus.INSIDE -> {
                Text(stringResource(R.string.gate_checking), textAlign = TextAlign.Center)
                CircularProgressIndicator()
            }
            GateStatus.DENIED -> LocationDeniedMessage(
                message = R.string.location_permission_denied,
                onTryAgainClick = onTryAgainClick,
                onOpenSettingsClick = onOpenSettingsClick,
                modifier = Modifier.fillMaxWidth(),
            )
            GateStatus.NO_FIX -> MessageAndTryAgain(R.string.gate_no_fix, onTryAgainClick)
            GateStatus.OUTSIDE -> MessageAndTryAgain(R.string.gate_outside, onTryAgainClick)
        }
        if (onLoginClick != null) {
            FilledTonalButton(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.gate_log_in))
            }
        }
    }
}

@Composable
private fun MessageAndTryAgain(@StringRes text: Int, onTryAgainClick: () -> Unit) {
    Text(stringResource(text), textAlign = TextAlign.Center)
    Button(onClick = onTryAgainClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.location_try_again))
    }
}

@Composable
private fun GatePreview(status: GateStatus) {
    CityFixTheme {
        LocationGateContent(
            state = LocationGateUiState(status),
            onTryAgainClick = {},
            onOpenSettingsClick = {},
            onLoginClick = null,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun GateCheckingPreview() {
    GatePreview(GateStatus.CHECKING)
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun GateDeniedPreview() {
    GatePreview(GateStatus.DENIED)
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun GateNoFixPreview() {
    GatePreview(GateStatus.NO_FIX)
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun GateOutsidePreview() {
    GatePreview(GateStatus.OUTSIDE)
}
