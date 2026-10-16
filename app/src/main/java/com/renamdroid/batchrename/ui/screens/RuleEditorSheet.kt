package com.renamdroid.batchrename.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.renamdroid.batchrename.data.RenameEngine
import com.renamdroid.batchrename.data.model.CleanupMode
import com.renamdroid.batchrename.data.model.Cleanup
import com.renamdroid.batchrename.data.model.DeleteMode
import com.renamdroid.batchrename.data.model.DeleteText
import com.renamdroid.batchrename.data.model.FindReplace
import com.renamdroid.batchrename.data.model.InsertPosition
import com.renamdroid.batchrename.data.model.InsertText
import com.renamdroid.batchrename.data.model.RenameRule
import com.renamdroid.batchrename.data.model.RemoveTags
import com.renamdroid.batchrename.data.model.SerialNumber
import com.renamdroid.batchrename.data.model.SymbolMode
import com.renamdroid.batchrename.data.model.SymbolReplace
import com.renamdroid.batchrename.data.model.Template
import com.renamdroid.batchrename.data.model.TemplatePlaceholders

/**
 * 规则编辑器（ModalBottomSheet）。
 * 新建规则时先选择规则类型，然后进入对应表单；编辑已有规则直接进表单。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorSheet(
    initial: RenameRule?,
    isNew: Boolean,
    regexEnabled: Boolean,
    exampleFileName: String?,
    onSave: (RenameRule) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 当前编辑的类型：null 表示还在选择类型
    var editingType by remember {
        mutableStateOf(
            when (initial) {
                is FindReplace -> RuleType.FIND_REPLACE
                is com.renamdroid.batchrename.data.model.DeleteText -> RuleType.DELETE
                is InsertText -> RuleType.INSERT
                is SerialNumber -> RuleType.SERIAL
                is SymbolReplace -> RuleType.SYMBOL
                is Cleanup -> RuleType.CLEANUP
                is RemoveTags -> RuleType.TAGS
                is Template -> RuleType.TEMPLATE
                null -> if (isNew) null else RuleType.FIND_REPLACE
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (editingType == null) {
                Text("添加规则", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                RuleType.entries.forEach { type ->
                    Card(
                        onClick = { editingType = type },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(type.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                type.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNew) {
                        TextButton(onClick = { editingType = null }) { Text("‹ 返回") }
                    }
                    Text(
                        text = if (isNew) "添加规则" else "编辑规则",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (!isNew && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除规则")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                val exampleBase = exampleFileName?.substringBeforeLast('.') ?: ""
                val exampleExt = exampleFileName?.substringAfterLast('.', "") ?: ""

                when (editingType!!) {
                    RuleType.FIND_REPLACE -> FindReplaceForm(
                        initial = initial as? FindReplace,
                        regexEnabled = regexEnabled,
                        exampleBase = exampleBase,
                        exampleExt = exampleExt,
                        onSave = { onSave(it); onDismiss() },
                    )
                    RuleType.DELETE -> DeleteForm(
                        initial = initial as? com.renamdroid.batchrename.data.model.DeleteText,
                        exampleBase = exampleBase,
                        exampleExt = exampleExt,
                        onSave = { onSave(it); onDismiss() },
                    )
                    RuleType.INSERT -> InsertForm(
                        initial = initial as? InsertText,
                        exampleBase = exampleBase,
                        exampleExt = exampleExt,
                        onSave = { onSave(it); onDismiss() },
                    )
                    RuleType.SERIAL -> SerialForm(
                        initial = initial as? SerialNumber,
                        exampleBase = exampleBase,
                        exampleExt = exampleExt,
                        onSave = { onSave(it); onDismiss() },
                    )
                    RuleType.SYMBOL -> SymbolForm(
                        initial = initial as? SymbolReplace,
                        exampleBase = exampleBase,
                        exampleExt = exampleExt,
                        onSave = { onSave(it); onDismiss() },
                    )
                    RuleType.CLEANUP -> CleanupForm(
                        initial = initial as? Cleanup,
                        exampleBase = exampleBase,
                        exampleExt = exampleExt,
                        onSave = { onSave(it); onDismiss() },
                    )
                    RuleType.TAGS -> {
                        Text("自动去掉文件名中的 [字幕组] 【字幕组】 (字幕组) 等标签。")
                        Spacer(Modifier.height(16.dp))
                        SaveButton {
                            onSave(
                                (initial as? RemoveTags)
                                    ?: RemoveTags(id = 0, enabled = true)
                            )
                            onDismiss()
                        }
                    }
                    RuleType.TEMPLATE -> TemplateForm(
                        initial = initial as? Template,
                        exampleBase = exampleBase,
                        exampleExt = exampleExt,
                        onSave = { onSave(it); onDismiss() },
                    )
                }
            }
        }
    }
}

enum class RuleType(val label: String, val description: String) {
    FIND_REPLACE("查找替换", "把文件名中的一段文字换成另一段"),
    DELETE("删除文字", "删除指定文字、开头/结尾文字或指定范围"),
    INSERT("插入文字", "在开头、结尾或指定位置插入文字"),
    SERIAL("添加序号", "自动添加 01、02、03 … 编号"),
    SYMBOL("替换符号", "点号 / 下划线 / 空格 互相转换"),
    CLEANUP("清理", "清理连续空格、连续点号、首尾空格"),
    TAGS("去除标签", "去掉 [字幕组] 【字幕组】 (字幕组) 等标签"),
    TEMPLATE("模板命名", "用 {title} {season} {episode} 等占位符生成名字"),
}

@Composable
private fun SaveButton(onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onClick) {
            Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("保存规则", fontWeight = FontWeight.Medium)
        }
    }
}

/** 实时示例展示 */
@Composable
private fun ExampleRow(originalBase: String, newBase: String, ext: String) {
    if (originalBase.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "示例",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "$originalBase.$ext → $newBase.$ext",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(if (value == 0) "" else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter { c -> c.isDigit() }
            onValueChange(text.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

// ---------- 各规则表单 ----------

@Composable
private fun FindReplaceForm(
    initial: FindReplace?,
    regexEnabled: Boolean,
    exampleBase: String,
    exampleExt: String,
    onSave: (FindReplace) -> Unit,
) {
    var find by remember { mutableStateOf(initial?.find ?: "") }
    var replace by remember { mutableStateOf(initial?.replace ?: "") }
    var caseSensitive by remember { mutableStateOf(initial?.caseSensitive ?: false) }
    var useRegex by remember { mutableStateOf(initial?.useRegex ?: false) }

    val rule = FindReplace(0, true, find, replace, caseSensitive, useRegex)
    val error = RenameEngine.ruleError(rule)

    OutlinedTextField(
        value = find,
        onValueChange = { find = it },
        label = { Text(if (useRegex) "正则表达式" else "查找内容") },
        placeholder = { Text(if (useRegex) "\\[.*?\\]" else "1080p") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = replace,
        onValueChange = { replace = it },
        label = { Text("替换为（留空 = 删除）") },
        placeholder = { Text("1080p WEB-DL") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    RowSetting("区分大小写", caseSensitive) { caseSensitive = it }
    if (regexEnabled) {
        RowSetting("使用正则表达式", useRegex) { useRegex = it }
        if (useRegex) {
            Text(
                "正则中 $1 表示第 1 个捕获组",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    ExampleRow(exampleBase, RenameEngine.applyRuleChain(exampleBase, listOf(rule), 0), exampleExt)
    SaveButton(enabled = find.isNotEmpty() && error == null) {
        onSave(rule.copy(id = initial?.id ?: 0L))
    }
}

@Composable
private fun DeleteForm(
    initial: com.renamdroid.batchrename.data.model.DeleteText?,
    exampleBase: String,
    exampleExt: String,
    onSave: (com.renamdroid.batchrename.data.model.DeleteText) -> Unit,
) {
    var mode by remember { mutableStateOf(initial?.mode ?: DeleteMode.SPECIFIED) }
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var caseSensitive by remember { mutableStateOf(initial?.caseSensitive ?: false) }
    var rangeStart by remember { mutableStateOf(initial?.rangeStart ?: 1) }
    var rangeEnd by remember { mutableStateOf(initial?.rangeEnd ?: 3) }

    val rule = com.renamdroid.batchrename.data.model.DeleteText(
        0, true, mode, text, caseSensitive, rangeStart, rangeEnd
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val modes = DeleteMode.entries
        modes.forEachIndexed { index, m ->
            SegmentedButton(
                selected = mode == m,
                onClick = { mode = m },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                label = { Text(m.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    if (mode == DeleteMode.RANGE) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField("从第几个字符", rangeStart, { rangeStart = it }, Modifier.weight(1f))
            NumberField("到第几个字符", rangeEnd, { rangeEnd = it }, Modifier.weight(1f))
        }
    } else {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("要删除的文字") },
            placeholder = { Text("[某某字幕组]") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        RowSetting("区分大小写", caseSensitive) { caseSensitive = it }
    }
    ExampleRow(exampleBase, RenameEngine.applyRuleChain(exampleBase, listOf(rule), 0), exampleExt)
    SaveButton(enabled = mode == DeleteMode.RANGE || text.isNotEmpty()) {
        onSave(rule.copy(id = initial?.id ?: 0L))
    }
}

@Composable
private fun InsertForm(
    initial: InsertText?,
    exampleBase: String,
    exampleExt: String,
    onSave: (InsertText) -> Unit,
) {
    var position by remember { mutableStateOf(initial?.position ?: InsertPosition.PREFIX) }
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var atIndex by remember { mutableStateOf(initial?.atIndex ?: 0) }

    val rule = InsertText(0, true, position, text, atIndex)

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val positions = InsertPosition.entries
        positions.forEachIndexed { index, p ->
            SegmentedButton(
                selected = position == p,
                onClick = { position = p },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = positions.size),
                label = { Text(p.label) },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("要插入的文字") },
        placeholder = { Text("Breaking Bad - ") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (position == InsertPosition.AT_INDEX) {
        Spacer(Modifier.height(8.dp))
        NumberField("在第几个字符之后插入（0 = 最开头）", atIndex, { atIndex = it }, Modifier.fillMaxWidth())
    }
    ExampleRow(exampleBase, RenameEngine.applyRuleChain(exampleBase, listOf(rule), 0), exampleExt)
    SaveButton(enabled = text.isNotEmpty()) { onSave(rule.copy(id = initial?.id ?: 0L)) }
}

@Composable
private fun SerialForm(
    initial: SerialNumber?,
    exampleBase: String,
    exampleExt: String,
    onSave: (SerialNumber) -> Unit,
) {
    var start by remember { mutableStateOf(initial?.start ?: 1) }
    var digits by remember { mutableStateOf(initial?.digits ?: 2) }
    var step by remember { mutableStateOf(initial?.step ?: 1) }
    var prefix by remember { mutableStateOf(initial?.prefix ?: "") }
    var suffix by remember { mutableStateOf(initial?.suffix ?: "") }
    var position by remember { mutableStateOf(initial?.position ?: InsertPosition.PREFIX) }
    var atIndex by remember { mutableStateOf(initial?.atIndex ?: 0) }

    val rule = SerialNumber(0, true, start, digits, step, prefix, suffix, position, atIndex)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("起始编号", start, { start = it }, Modifier.weight(1f))
        NumberField("位数", digits, { digits = it }, Modifier.weight(1f))
        NumberField("步进", step, { step = it }, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = prefix,
            onValueChange = { prefix = it },
            label = { Text("前缀") },
            placeholder = { Text("S01E") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = suffix,
            onValueChange = { suffix = it },
            label = { Text("后缀") },
            placeholder = { Text(" - ") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(12.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val positions = listOf(InsertPosition.PREFIX, InsertPosition.SUFFIX, InsertPosition.AT_INDEX)
        positions.forEachIndexed { index, p ->
            SegmentedButton(
                selected = position == p,
                onClick = { position = p },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = positions.size),
                label = { Text(p.label, maxLines = 1) },
            )
        }
    }
    if (position == InsertPosition.AT_INDEX) {
        Spacer(Modifier.height(8.dp))
        NumberField("在第几个字符之后插入", atIndex, { atIndex = it }, Modifier.fillMaxWidth())
    }
    ExampleRow(exampleBase, RenameEngine.applyRuleChain(exampleBase, listOf(rule), 0), exampleExt)
    SaveButton { onSave(rule.copy(id = initial?.id ?: 0L)) }
}

@Composable
private fun SymbolForm(
    initial: SymbolReplace?,
    exampleBase: String,
    exampleExt: String,
    onSave: (SymbolReplace) -> Unit,
) {
    var mode by remember { mutableStateOf(initial?.mode ?: SymbolMode.DOT_TO_SPACE) }
    val rule = SymbolReplace(0, true, mode)
    SymbolMode.entries.forEach { m ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = mode == m, onClick = { mode = m })
            Text(m.label)
        }
    }
    ExampleRow(exampleBase, RenameEngine.applyRuleChain(exampleBase, listOf(rule), 0), exampleExt)
    SaveButton { onSave(rule.copy(id = initial?.id ?: 0L)) }
}

@Composable
private fun CleanupForm(
    initial: Cleanup?,
    exampleBase: String,
    exampleExt: String,
    onSave: (Cleanup) -> Unit,
) {
    var mode by remember { mutableStateOf(initial?.mode ?: CleanupMode.CONSECUTIVE_SPACES) }
    val rule = Cleanup(0, true, mode)
    CleanupMode.entries.forEach { m ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = mode == m, onClick = { mode = m })
            Text(m.label)
        }
    }
    ExampleRow(exampleBase, RenameEngine.applyRuleChain(exampleBase, listOf(rule), 0), exampleExt)
    SaveButton { onSave(rule.copy(id = initial?.id ?: 0L)) }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TemplateForm(
    initial: Template?,
    exampleBase: String,
    exampleExt: String,
    onSave: (Template) -> Unit,
) {
    var template by remember { mutableStateOf(initial?.template ?: "{title} - S{season}E{episode}") }
    val rule = Template(0, true, template)

    OutlinedTextField(
        value = template,
        onValueChange = { template = it },
        label = { Text("命名模板") },
        placeholder = { Text("{title} - S{season}E{episode} - {resolution}") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text("可用占位符（点击插入）：", style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(4.dp))
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TemplatePlaceholders.ALL.forEach { (placeholder, desc) ->
            AssistChip(
                onClick = { template += placeholder },
                label = { Text(placeholder) },
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        TemplatePlaceholders.ALL.joinToString("  ") { "${it.first}=${it.second}" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ExampleRow(exampleBase, RenameEngine.applyRuleChain(exampleBase, listOf(rule), 0), exampleExt)
    SaveButton(enabled = template.isNotBlank()) { onSave(rule.copy(id = initial?.id ?: 0L)) }
}

@Composable
private fun RowSetting(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SaveButton(enabled: Boolean = true, onClick: () -> Unit) {
    if (enabled) {
        SaveButton(onClick)
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {}, enabled = false) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("保存规则")
            }
        }
    }
}
