package com.renamdroid.batchrename.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.renamdroid.batchrename.data.model.HistoryEntry
import com.renamdroid.batchrename.ui.MainViewModel
import com.renamdroid.batchrename.ui.components.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit) {
    val history by vm.history.collectAsState()
    var detail by remember { mutableStateOf<HistoryEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("操作历史") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { vm.clearHistory() }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "清空历史")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "还没有操作记录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(history, key = { it.id }) { entry ->
                    Card(
                        onClick = { detail = entry },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.size(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${formatTime(entry.timestamp)} · ${entry.fileCount} 个文件",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    entry.ruleSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                                if (entry.undone) {
                                    Text(
                                        "已撤销",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (!entry.undone && entry.items.isNotEmpty()) {
                                TextButton(onClick = { vm.undoEntry(entry) }) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("撤销")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detail?.let { entry ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text("${formatTime(entry.timestamp)} · ${entry.fileCount} 个文件") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        entry.ruleSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    entry.items.take(100).forEach { item ->
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(item.originalName, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "→ ${item.newName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (entry.items.size > 100) {
                        Text("… 共 ${entry.items.size} 个", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (!entry.undone && entry.items.isNotEmpty()) {
                    TextButton(onClick = { vm.undoEntry(entry); detail = null }) { Text("撤销此操作") }
                }
            },
            dismissButton = {
                TextButton(onClick = { detail = null }) { Text("关闭") }
            },
        )
    }
}
