/*
 * Updated: 2025-01-21 18:07:12
 * Author: William8677
 */

package com.williamfq.xhat.ui.screens.main.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import com.williamfq.xhat.ui.screens.main.components.EmptyScreenPlaceholder

@Composable
fun ChannelsTab() {
    EmptyScreenPlaceholder(
        icon = Icons.Default.Tv,
        text = "Canales"
    )
}