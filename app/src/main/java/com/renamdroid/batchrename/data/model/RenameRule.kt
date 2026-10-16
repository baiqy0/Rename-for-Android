package com.renamdroid.batchrename.data.model

/**
 * 重命名规则。所有规则构成一条"规则链"，按列表顺序依次应用。
 * 每条规则都可以启用/禁用，且只作用于文件名主体（不含扩展名）。
 */
sealed class RenameRule {

    abstract val id: Long
    abstract val enabled: Boolean
    abstract fun withEnabled(enabled: Boolean): RenameRule

    /** 规则类型名称，用于卡片标题 */
    abstract val typeLabel: String

    /** 规则内容摘要，用于卡片副标题和历史记录 */
    abstract fun summary(): String
}

/** 查找与替换 */
data class FindReplace(
    override val id: Long,
    override val enabled: Boolean = true,
    val find: String,
    val replace: String,
    val caseSensitive: Boolean = false,
    val useRegex: Boolean = false,
) : RenameRule() {
    override val typeLabel: String get() = "查找替换"
    override fun withEnabled(enabled: Boolean) = copy(enabled = enabled)
    override fun summary(): String = buildString {
        append("查找「$find」")
        if (useRegex) append("（正则）")
        append(" → 替换为「$replace」")
    }
}

/** 删除文字 */
data class DeleteText(
    override val id: Long,
    override val enabled: Boolean = true,
    val mode: DeleteMode,
    val text: String,
    val caseSensitive: Boolean = false,
    /** 删除范围模式用（1 起始，闭区间） */
    val rangeStart: Int = 1,
    val rangeEnd: Int = 1,
) : RenameRule() {
    override val typeLabel: String get() = "删除文字"
    override fun withEnabled(enabled: Boolean) = copy(enabled = enabled)
    override fun summary(): String = when (mode) {
        DeleteMode.SPECIFIED -> "删除「$text」"
        DeleteMode.PREFIX -> "删除开头「$text」"
        DeleteMode.SUFFIX -> "删除结尾「$text」"
        DeleteMode.RANGE -> "删除第 $rangeStart ~ $rangeEnd 个字符"
    }
}

/** 插入文字 */
data class InsertText(
    override val id: Long,
    override val enabled: Boolean = true,
    val position: InsertPosition,
    val text: String,
    /** 指定位置插入用：在第 N 个字符之后插入（0 = 最开头） */
    val atIndex: Int = 0,
) : RenameRule() {
    override val typeLabel: String get() = "插入文字"
    override fun withEnabled(enabled: Boolean) = copy(enabled = enabled)
    override fun summary(): String = when (position) {
        InsertPosition.PREFIX -> "开头插入「$text」"
        InsertPosition.SUFFIX -> "结尾插入「$text」"
        InsertPosition.AT_INDEX -> "第 $atIndex 个字符后插入「$text」"
    }
}

/** 添加序号 */
data class SerialNumber(
    override val id: Long,
    override val enabled: Boolean = true,
    val start: Int = 1,
    val digits: Int = 2,
    val step: Int = 1,
    val prefix: String = "",
    val suffix: String = "",
    val position: InsertPosition = InsertPosition.PREFIX,
    val atIndex: Int = 0,
) : RenameRule() {
    override val typeLabel: String get() = "添加序号"
    override fun withEnabled(enabled: Boolean) = copy(enabled = enabled)
    override fun summary(): String = buildString {
        append("序号 $start")
        if (step != 1) append(" 步进 $step")
        if (digits > 1) append(" $digits 位")
        if (prefix.isNotEmpty()) append(" 前缀「$prefix」")
        if (suffix.isNotEmpty()) append(" 后缀「$suffix」")
        append(when (position) {
            InsertPosition.PREFIX -> "（开头）"
            InsertPosition.SUFFIX -> "（结尾）"
            InsertPosition.AT_INDEX -> "（第 $atIndex 字符后）"
        })
    }
}

/** 符号替换（快捷操作） */
data class SymbolReplace(
    override val id: Long,
    override val enabled: Boolean = true,
    val mode: SymbolMode,
) : RenameRule() {
    override val typeLabel: String get() = "替换符号"
    override fun withEnabled(enabled: Boolean) = copy(enabled = enabled)
    override fun summary(): String = mode.label
}

/** 清理（快捷操作） */
data class Cleanup(
    override val id: Long,
    override val enabled: Boolean = true,
    val mode: CleanupMode,
) : RenameRule() {
    override val typeLabel: String get() = "清理"
    override fun withEnabled(enabled: Boolean) = copy(enabled = enabled)
    override fun summary(): String = mode.label
}

/** 去除字幕组标签 [XX] 【XX】 (XX) */
data class RemoveTags(
    override val id: Long,
    override val enabled: Boolean = true,
) : RenameRule() {
    override val typeLabel: String get() = "去除标签"
    override fun withEnabled(enabled: Boolean) = copy(enabled = enabled)
    override fun summary(): String = "去除字幕组标签 [ ] 【 】 ( )"
}

/** 自定义模板 */
data class Template(
    override val id: Long,
    override val enabled: Boolean = true,
    val template: String,
) : RenameRule() {
    override val typeLabel: String get() = "模板命名"
    override fun withEnabled(enabled: Boolean) = copy(enabled = enabled)
    override fun summary(): String = "模板「$template」"
}

enum class DeleteMode(val label: String) {
    SPECIFIED("指定文字"),
    PREFIX("开头文字"),
    SUFFIX("结尾文字"),
    RANGE("指定范围"),
}

enum class InsertPosition(val label: String) {
    PREFIX("开头"),
    SUFFIX("结尾"),
    AT_INDEX("指定位置"),
}

enum class SymbolMode(val label: String) {
    DOT_TO_SPACE("点号 → 空格"),
    UNDERSCORE_TO_SPACE("下划线 → 空格"),
    SPACE_TO_DOT("空格 → 点号"),
}

enum class CleanupMode(val label: String) {
    CONSECUTIVE_SPACES("清理连续空格"),
    CONSECUTIVE_DOTS("清理连续点号"),
    TRIM_ENDS("去掉首尾空格"),
}

/** 模板支持的占位符 */
object TemplatePlaceholders {
    val ALL = listOf(
        "{original}" to "原始文件名",
        "{title}" to "剧名/片名",
        "{season}" to "季数",
        "{episode}" to "集数",
        "{year}" to "年份",
        "{resolution}" to "分辨率",
        "{codec}" to "编码",
        "{source}" to "来源",
        "{audio}" to "音频",
        "{index}" to "序号（从 1 开始）",
    )
}
