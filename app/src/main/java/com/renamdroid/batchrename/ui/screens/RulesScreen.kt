package com.renamdroid.batchrename.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.renamdroid.batchrename.data.RenameEngine
import com.renamdroid.batchrename.data.model.Cleanup
import com.renamdroid.batchrename.data.model.CleanupMode
import com.renamdroid.batchrename.data.model.RenameRule
import com.renamdroid.batchrename.data.model.RemoveTags
import com.renamdroid.batchrename.data.model.SymbolMode
import com.renamdroid.batchrename.data.model.SymbolReplace
import com.renamdroid.batchrename.ui.MainViewModel
import com.renamdroid.batchrename.ui.Screen
import kotlinx.coroutines.flow.collectLatest

private data class EditingRule(val rule: RenameRule?, val isNew: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val state by vm.rulesState.collectAsState()
    val filesState by vm.filesState.collectAsState()
    val settings by vm.settings.collectAsState()
    val exec by vm.execState.collectAsState()
    val history by vm.history.collectAsState()

    var editing by remember { mutableStateOf<EditingRule?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    var showExtWarning by remember { mutableStateOf(false) }
    var extText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // 全局提示（撤销结果等）
    LaunchedEffect(Unit) {
        vm.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    val exampleFile = filesState.selectedVisibleFiles.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("重命名规则") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (filesState.selected.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有选择文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onBack) { Text("去选择文件") }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // ---------- 规则链 ----------
            Text(
                "规则按从上到下依次执行",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            state.rules.forEachIndexed { index, rule ->
                RuleCard(
                    rule = rule,
                    index = index + 1,
                    isFirst = index == 0,
                    isLast = index == state.rules.lastIndex,
                    onClick = { editing = EditingRule(rule, false) },
                    onToggle = { vm.toggleRule(rule.id) },
                    onMoveUp = { vm.moveRule(rule.id, -1) },
                    onMoveDown = { vm.moveRule(rule.id, 1) },
                    onDelete = { vm.removeRule(rule.id) },
                )
                Spacer(Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { editing = EditingRule(null, true) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("添加规则")
            }

            // 快捷规则
            Spacer(Modifier.height(12.dp))
            Text(
                "快捷规则",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            QuickRuleChips(vm)

            // ---------- 扩展名 ----------
            Spacer(Modifier.height(20.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("保留扩展名", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "规则只改文件名，不动 .mkv/.mp4 等扩展名",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.keepExtension,
                            onCheckedChange = { vm.setKeepExtension(it) },
                        )
                    }
                    if (!settings.keepExtension) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = extText,
                            onValueChange = {
                                extText = it.removePrefix(".")
                                vm.setExtensionOverride(extText)
                            },
                            label = { Text("新扩展名") },
                            placeholder = { Text("mkv") },
                            singleLine = true,
                            supportingText = { Text("修改扩展名不会转换文件格式") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ---------- 问题提示 ----------
            if (state.errorCount > 0) {
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("发现 ${state.errorCount} 个问题，已阻止执行", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        }
                        Spacer(Modifier.height(6.dp))
                        state.preview.filter { it.error != null }.take(5).forEach { row ->
                            Text(
                                "· ${row.file.name}：${row.error}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (state.errorCount > 5) {
                            Text("…等 ${state.errorCount} 个问题", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ---------- 实时预览 ----------
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("实时预览", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${state.changedCount} 个文件将变化",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.computing) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.height(8.dp))

            if (state.preview.isEmpty()) {
                Text(
                    "选择文件后这里会实时显示改名结果",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.preview.take(50).forEach { row ->
                    PreviewRowItem(row)
                }
                if (state.preview.size > 50) {
                    Text(
                        "… 还有 ${state.preview.size - 50} 个文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            // ---------- 执行 ----------
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (!settings.keepExtension) showExtWarning = true else showConfirm = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = state.canExecute,
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.size(8.dp))
                Text(
                    if (state.changedCount > 0) "开始重命名（${state.changedCount} 个文件）" else "开始重命名",
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("返回文件列表")
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ---------- 规则编辑器 ----------
    editing?.let { e ->
        RuleEditorSheet(
            initial = e.rule,
            isNew = e.isNew,
            regexEnabled = settings.regexEnabled,
            exampleFileName = exampleFile?.name,
            onSave = { rule ->
                val withId = if (e.isNew) rule.withId(vm.newRuleId()) else rule
                if (e.isNew) vm.addRule(withId) else vm.updateRule(withId)
            },
            onDelete = e.rule?.let { r -> { vm.removeRule(r.id) } },
            onDismiss = { editing = null },
        )
    }

    // ---------- 确认对话框 ----------
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("确定重命名？") },
            text = { Text("${state.changedCount} 个文件将发生变化。重命名后可以通过「撤销」恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    vm.startRename()
                }) { Text("开始") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            },
        )
    }

    // ---------- 扩展名警告 ----------
    if (showExtWarning) {
        AlertDialog(
            onDismissRequest = { showExtWarning = false },
            title = { Text("警告") },
            text = { Text("修改文件扩展名不会转换文件格式。\n确定要修改吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showExtWarning = false
                    showConfirm = true
                }) { Text("确定修改") }
            },
            dismissButton = {
                TextButton(onClick = { showExtWarning = false }) { Text("取消") }
            },
        )
    }

    // ---------- 进度对话框 ----------
    if (exec is com.renamdroid.batchrename.ui.ExecState.Running) {
        val running = exec as com.renamdroid.batchrename.ui.ExecState.Running
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在重命名……") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { if (running.total > 0) running.done.toFloat() / running.total else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("${running.done} / ${running.total}")
                }
            },
            confirmButton = {},
        )
    }

    // ---------- 结果对话框 ----------
    if (exec is com.renamdroid.batchrename.ui.ExecState.Finished) {
        val outcome = (exec as com.renamdroid.batchrename.ui.ExecState.Finished).outcome
        val lastEntry = history.firstOrNull()
        AlertDialog(
            onDismissRequest = { vm.dismissExecResult() },
            title = { Text(if (outcome.failures.isEmpty()) "完成" else "完成（有失败）") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("成功：${outcome.successCount}")
                    if (outcome.failures.isNotEmpty()) {
                        Text("失败：${outcome.failures.size}", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        outcome.failures.forEach { failure ->
                            Text(
                                "· ${failure.fileName}\n  原因：${failure.reason}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "提示：可以修正问题后对失败文件重试。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                if (settings.showUndo && outcome.successCount > 0 && lastEntry != null) {
                    TextButton(onClick = {
                        vm.undoEntry(lastEntry)
                        vm.dismissExecResult()
                    }) { Text("撤销") }
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissExecResult() }) { Text("完成") }
            },
        )
    }
}

/** 给规则分配真实 id（id 在 ViewModel 中生成，保证唯一） */
private fun RenameRule.withId(newId: Long): RenameRule = when (this) {
    is com.renamdroid.batchrename.data.model.FindReplace -> copy(id = newId)
    is com.renamdroid.batchrename.data.model.DeleteText -> copy(id = newId)
    is com.renamdroid.batchrename.data.model.InsertText -> copy(id = newId)
    is com.renamdroid.batchrename.data.model.SerialNumber -> copy(id = newId)
    is SymbolReplace -> copy(id = newId)
    is Cleanup -> copy(id = newId)
    is RemoveTags -> copy(id = newId)
    is com.renamdroid.batchrename.data.model.Template -> copy(id = newId)
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun QuickRuleChips(vm: MainViewModel) {
    val quickRules = listOf<Pair<String, () -> RenameRule>>(
        "去除字幕组标签" to { RemoveTags(id = 0) },
        "点号 → 空格" to { SymbolReplace(id = 0, mode = SymbolMode.DOT_TO_SPACE) },
        "下划线 → 空格" to { SymbolReplace(id = 0, mode = SymbolMode.UNDERSCORE_TO_SPACE) },
        "空格 → 点号" to { SymbolReplace(id = 0, mode = SymbolMode.SPACE_TO_DOT) },
        "清理连续空格" to { Cleanup(id = 0, mode = CleanupMode.CONSECUTIVE_SPACES) },
        "清理连续点号" to { Cleanup(id = 0, mode = CleanupMode.CONSECUTIVE_DOTS) },
    )
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        quickRules.forEach { (label, factory) ->
            AssistChip(
                onClick = { vm.addRule(factory().withId(vm.newRuleId())) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun RuleCard(
    rule: RenameRule,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$index",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.typeLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    rule.summary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = "上移",
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(
                    Icons.Filled.ArrowDownward,
                    contentDescription = "下移",
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun PreviewRowItem(row: com.renamdroid.batchrename.data.model.PreviewRow) {
    val newFull = if (row.newExtension.isEmpty()) row.newName else "${row.newName}.${row.newExtension}"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (row.error != null) MaterialTheme.colorScheme.errorContainer
                             else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.file.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough.takeIf { row.changed && row.error == null } ?: TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = row.error ?: newFull,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    row.error != null -> MaterialTheme.colorScheme.onErrorContainer
                    row.changed -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
