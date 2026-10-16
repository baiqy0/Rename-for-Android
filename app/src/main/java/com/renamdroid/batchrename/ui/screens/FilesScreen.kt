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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.renamdroid.batchrename.data.model.FileFilter
import com.renamdroid.batchrename.data.model.FileItem
import com.renamdroid.batchrename.data.model.SortMode
import com.renamdroid.batchrename.ui.MainViewModel
import com.renamdroid.batchrename.ui.components.categoryIcon
import com.renamdroid.batchrename.ui.components.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onNext: () -> Unit,
) {
    val state by vm.filesState.collectAsState()
    val settings by vm.settings.collectAsState()
    var sortMenu by remember { mutableStateOf(false) }
    var actionMenu by remember { mutableStateOf(false) }

    val visible = state.visibleFiles
    val selectedVisible = state.selectedVisibleFiles

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已选择 ${state.files.size} 个文件") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    // 排序
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序")
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label + if (mode == state.sortMode) " ✓" else "") },
                                    onClick = { vm.setSortMode(mode); sortMenu = false },
                                )
                            }
                        }
                    }
                    // 更多操作
                    Box {
                        IconButton(onClick = { actionMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(expanded = actionMenu, onDismissRequest = { actionMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("全选") },
                                leadingIcon = { Icon(Icons.Filled.FilterList, null) },
                                onClick = { vm.selectAll(visible, true); actionMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("取消全选") },
                                onClick = { vm.selectAll(visible, false); actionMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("反选") },
                                onClick = { vm.invertSelection(visible); actionMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("清空列表") },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { vm.clearFiles(); actionMenu = false },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onPickFiles, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("添加文件")
                    }
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        enabled = state.selected.isNotEmpty(),
                    ) {
                        Text("下一步：设置规则")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // 筛选条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FileFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        FileFilter.ALL -> state.files.size
                        else -> state.files.count { it.category.label == filter.label }
                    }
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { vm.setFilter(filter) },
                        label = { Text("${filter.label} $count") },
                    )
                }
            }

            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.files.isEmpty()) "还没有选择文件" else "当前筛选下没有文件",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp, end = 12.dp, bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(visible, key = { it.id }) { file ->
                        FileRow(
                            file = file,
                            selected = file.id in state.selected,
                            onToggle = { vm.toggleSelect(file.id) },
                            onRemove = { vm.removeFiles(setOf(file.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: FileItem,
    selected: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                             else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Icon(
                categoryIcon(file.category),
                contentDescription = null,
                tint = if (file.category == com.renamdroid.batchrename.data.model.FileCategory.OTHER)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatFileSize(file.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (file.extension.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        file.extension,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "移除",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
