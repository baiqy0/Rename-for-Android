package com.renamdroid.batchrename.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.renamdroid.batchrename.data.FolderImportScope
import com.renamdroid.batchrename.data.ThemeColor
import com.renamdroid.batchrename.data.ThemeMode
import com.renamdroid.batchrename.data.model.SortMode
import com.renamdroid.batchrename.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    var sortDialog by remember { mutableStateOf(false) }
    var scopeDialog by remember { mutableStateOf(false) }
    var licenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SettingsSection("外观") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = ThemeMode.entries
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            label = { Text(mode.label) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "主题颜色",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ThemeColor.entries.forEach { color ->
                        val selected = settings.themeColor == color
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { vm.setThemeColor(color) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(color.swatchHex))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                color.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "动态取色跟随壁纸（需 Android 12 及以上，低版本自动用靛蓝）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection("重命名") {
                SwitchSetting(
                    title = "默认保留扩展名",
                    subtitle = "规则只修改文件名主体，不动 .mkv / .mp4 等扩展名",
                    checked = settings.keepExtension,
                    onChange = { vm.setKeepExtension(it) },
                )
                SwitchSetting(
                    title = "执行前显示预览列表",
                    subtitle = "在规则页实时展示每个文件的改名结果",
                    checked = settings.showPreview,
                    onChange = { vm.setShowPreview(it) },
                )
                SwitchSetting(
                    title = "完成后显示撤销入口",
                    subtitle = "批量重命名完成后提供撤销按钮",
                    checked = settings.showUndo,
                    onChange = { vm.setShowUndo(it) },
                )
            }

            SettingsSection("文件") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("默认排序", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "影视文件推荐自然排序（Episode 2 在 Episode 10 前面）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { sortDialog = true }) { Text(settings.defaultSort.label) }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("文件夹导入范围", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            settings.folderImportScope.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { scopeDialog = true }) {
                        Text(settings.folderImportScope.label)
                    }
                }
            }

            SettingsSection("高级") {
                SwitchSetting(
                    title = "正则表达式",
                    subtitle = "在查找替换规则中启用正则模式，适合高级用户",
                    checked = settings.regexEnabled,
                    onChange = { vm.setRegexEnabled(it) },
                )
            }

            SettingsSection("关于") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("版本", style = MaterialTheme.typography.bodyLarge)
                    Text("1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("开源许可", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { licenseDialog = true }) { Text("查看") }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (sortDialog) {
        AlertDialog(
            onDismissRequest = { sortDialog = false },
            title = { Text("默认排序") },
            text = {
                Column {
                    SortMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        ) {
                            RadioButton(
                                selected = settings.defaultSort == mode,
                                onClick = { vm.setDefaultSort(mode); sortDialog = false },
                            )
                            Text(mode.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sortDialog = false }) { Text("完成") }
            },
        )
    }

    if (scopeDialog) {
        AlertDialog(
            onDismissRequest = { scopeDialog = false },
            title = { Text("文件夹导入范围") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    FolderImportScope.entries.forEach { scope ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.setFolderImportScope(scope)
                                    scopeDialog = false
                                }
                                .padding(vertical = 6.dp),
                        ) {
                            RadioButton(
                                selected = settings.folderImportScope == scope,
                                onClick = {
                                    vm.setFolderImportScope(scope)
                                    scopeDialog = false
                                },
                            )
                            Spacer(Modifier.size(4.dp))
                            Column {
                                Text(scope.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    scope.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { scopeDialog = false }) { Text("完成") }
            },
        )
    }

    if (licenseDialog) {
        AlertDialog(
            onDismissRequest = { licenseDialog = false },
            title = { Text("开源许可") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "本项目使用以下开源组件：\n\n" +
                            "· AndroidX / Jetpack Compose — Apache License 2.0\n" +
                            "· Material Components for Android — Apache License 2.0\n" +
                            "· Kotlin / Kotlinx Coroutines — Apache License 2.0\n" +
                            "· Android Open Source Project — Apache License 2.0",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { licenseDialog = false }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
