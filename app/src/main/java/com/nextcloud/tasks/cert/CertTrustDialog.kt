package com.nextcloud.tasks.cert

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextcloud.tasks.R
import com.nextcloud.tasks.data.network.PendingCertDecision

/**
 * Shows a native, themed trust prompt whenever a server presents a certificate the system can't
 * verify. Hosted at the app root so it overlays any screen. Dismissing without an explicit choice
 * counts as a rejection.
 */
@Composable
fun CertTrustDialog(viewModel: CertTrustViewModel = hiltViewModel()) {
    val pending by viewModel.pending.collectAsState()
    pending?.let { cert ->
        CertTrustDialogContent(
            cert = cert,
            onAccept = { viewModel.onDecision(cert.fingerprint, true) },
            onReject = { viewModel.onDecision(cert.fingerprint, false) },
        )
    }
}

@Composable
private fun CertTrustDialogContent(
    cert: PendingCertDecision,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    var verified by remember(cert.fingerprint) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onReject,
        title = { Text(stringResource(R.string.cert_trust_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.cert_trust_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                CertField(stringResource(R.string.cert_trust_issued_for), cert.issuedFor)
                CertField(stringResource(R.string.cert_trust_issued_by), cert.issuedBy)
                CertField(
                    label = stringResource(R.string.cert_trust_validity),
                    value = stringResource(R.string.cert_trust_validity_value, cert.validFrom, cert.validUntil),
                )
                CertField(stringResource(R.string.cert_trust_sha256), cert.sha256, monospace = true)
                CertField(stringResource(R.string.cert_trust_sha1), cert.sha1, monospace = true)
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { verified = !verified },
                ) {
                    Checkbox(checked = verified, onCheckedChange = { verified = it })
                    Text(
                        text = stringResource(R.string.cert_trust_confirm_checkbox),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept, enabled = verified) {
                Text(stringResource(R.string.cert_trust_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text(stringResource(R.string.cert_trust_reject))
            }
        },
    )
}

@Composable
private fun CertField(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else null,
        )
    }
}
