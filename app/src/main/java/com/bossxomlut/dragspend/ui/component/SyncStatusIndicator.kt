package com.bossxomlut.dragspend.ui.component

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.data.local.entity.SyncStatus
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme

/**
 * A small indicator dot that shows the sync status of an item.
 * - Green/no indicator: synced
 * - Orange dot: pending sync
 */
@Composable
fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
) {
    if (syncStatus == SyncStatus.SYNCED) {
        // Don't show anything for synced items
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = CircleShape,
                ),
        )
        if (showLabel) {
            Text(
                text = stringResource(R.string.sync_status_pending),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/**
 * Icon-based sync status indicator that shows an upload icon for pending items.
 */
@Composable
fun SyncStatusIcon(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
) {
    if (syncStatus == SyncStatus.SYNCED) {
        return
    }

    Icon(
        imageVector = Icons.Default.CloudUpload,
        contentDescription = stringResource(R.string.sync_status_pending),
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = modifier.size(14.dp),
    )
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun SyncStatusIndicatorPreview() {
    DragSpendTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SyncStatusIndicator(syncStatus = SyncStatus.PENDING_CREATE, showLabel = true)
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light Icon")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark Icon")
@Composable
private fun SyncStatusIconPreview() {
    DragSpendTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Transaction", modifier = Modifier.padding(end = 8.dp))
            SyncStatusIcon(syncStatus = SyncStatus.PENDING_UPDATE)
        }
    }
}
