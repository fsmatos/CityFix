package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/**
 * The main action of a screen (D-016): solid indigo with white text in both themes, like the
 * report button (`primaryContainer`). The theme's dark `primary` is a light indigo, kept for
 * accent text and selected controls that must stay readable on dark backgrounds; a filled button
 * must not use it. Secondary actions use FilledTonalButton (neutral) or TextButton.
 */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        content = content,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonPreview() {
    CityFixTheme {
        PrimaryButton(onClick = {}) { Text("Submit") }
    }
}
