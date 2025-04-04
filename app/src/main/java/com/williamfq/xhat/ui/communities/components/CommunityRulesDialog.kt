/*
 * Updated: 2025-01-26 18:31:15
 * Author: William8677
 */

package com.williamfq.xhat.ui.communities.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*

@Composable
fun CommunityRulesDialog(
    rules: List<String>,
    onRulesUpdated: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reglas de la comunidad") },
        text = {
            // TODO: Implementar editor de reglas
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}