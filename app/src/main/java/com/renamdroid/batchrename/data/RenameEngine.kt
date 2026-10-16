package com.renamdroid.batchrename.data

import com.renamdroid.batchrename.core.MediaNameParser
import com.renamdroid.batchrename.data.model.Cleanup
import com.renamdroid.batchrename.data.model.CleanupMode
import com.renamdroid.batchrename.data.model.DeleteMode
import com.renamdroid.batchrename.data.model.DeleteText
import com.renamdroid.batchrename.data.model.FindReplace
import com.renamdroid.batchrename.data.model.InsertPosition
import com.renamdroid.batchrename.data.model.InsertText
import com.renamdroid.batchrename.data.model.RemoveTags
import com.renamdroid.batchrename.data.model.RenameRule
import com.renamdroid.batchrename.data.model.SerialNumber
import com.renamdroid.batchrename.data.model.SymbolMode
import com.renamdroid.batchrename.data.model.SymbolReplace
import com.renamdroid.batchrename.data.model.Template
import java.io.IOException

/**
 * 重命名引擎：把规则链应用到文件名上，并做各种合法性检查。
 *
 * 重要约定：
 *  - 所有规则只作用于"文件名主体"（不含扩展名），扩展名默认保留；
 *  - 单个文件计算出错（例如非法正则）不会影响其他文件；
 *  - 引擎是纯函数，不碰文件系统，方便测试。
 */
object RenameEngine {

    /** 绝大多数文件系统（ext4/FAT32/exFAT）对单个文件名的上限 */
    const val MAX_NAME_LENGTH = 255

    val ILLEGAL_CHARS = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

    private val TAG_PATTERN = Regex("""\[[^\[\]]*\]|【[^【】]*】|（[^（）]*）|\([^()]*\)""")
    private val CONSECUTIVE_SPACES = Regex(""" {2,}""")
    private val CONSECUTIVE_DOTS = Regex("""\.{2,}""")
    // 注意：Android 的 java.util.regex 底层是 ICU 正则，比桌面 JVM 更严格——
    // 字面花括号必须写成 \{ 和 \}。裸 } 在 ICU 下会抛 PatternSyntaxException，
    // 而这里又是对象初始化，一旦抛异常整个 RenameEngine 类初始化失败，
    // 之后一碰预览就闪退（ExceptionInInitializerError）。改这段正则时千万别去掉转义！
    private val PLACEHOLDER = Regex("""\{([a-zA-Z]+)\}""")

    /**
     * 应用整条规则链。
     * @param baseName 原文件名主体（不含扩展名）
     * @param ordinal 该文件在列表中的序号（0 起始），供 {index} 和序号规则使用
     */
    fun applyRuleChain(baseName: String, rules: List<RenameRule>, ordinal: Int): String {
        var current = baseName
        for (rule in rules) {
            if (!rule.enabled) continue
            current = when (rule) {
                is FindReplace -> applyFindReplace(current, rule)
                is DeleteText -> applyDelete(current, rule)
                is InsertText -> applyInsert(current, rule)
                is SerialNumber -> applySerial(current, rule, ordinal)
                is SymbolReplace -> applySymbol(current, rule.mode)
                is Cleanup -> applyCleanup(current, rule.mode)
                is RemoveTags -> applyRemoveTags(current)
                is Template -> renderTemplate(rule.template, current, ordinal)
            }
        }
        return current
    }

    // ---------- 各规则实现 ----------

    private fun applyFindReplace(input: String, rule: FindReplace): String {
        if (rule.find.isEmpty()) return input
        return try {
            if (rule.useRegex) {
                val options = if (rule.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                Regex(rule.find, options).replace(input, rule.replace)
            } else {
                if (rule.caseSensitive) {
                    input.replace(rule.find, rule.replace)
                } else {
                    Regex(Regex.escape(rule.find), RegexOption.IGNORE_CASE)
                        .replace(input, Regex.escapeReplacement(rule.replace))
                }
            }
        } catch (e: Exception) {
            // 正则写错等情况：保持原样，错误会在规则编辑器和预览中提示
            input
        }
    }

    private fun applyDelete(input: String, rule: DeleteText): String {
        return when (rule.mode) {
            DeleteMode.SPECIFIED -> {
                if (rule.text.isEmpty()) input
                else if (rule.caseSensitive) input.replace(rule.text, "")
                else Regex(Regex.escape(rule.text), RegexOption.IGNORE_CASE).replace(input, "")
            }
            DeleteMode.PREFIX ->
                if (rule.text.isNotEmpty() && input.startsWith(rule.text, ignoreCase = !rule.caseSensitive))
                    input.substring(rule.text.length) else input
            DeleteMode.SUFFIX ->
                if (rule.text.isNotEmpty() && input.endsWith(rule.text, ignoreCase = !rule.caseSensitive))
                    input.dropLast(rule.text.length) else input
            DeleteMode.RANGE -> {
                val start = rule.rangeStart.coerceAtLeast(1)
                val end = rule.rangeEnd.coerceAtMost(input.length)
                if (start > end) input
                else input.removeRange(start - 1, end)
            }
        }
    }

    private fun applyInsert(input: String, rule: InsertText): String {
        if (rule.text.isEmpty()) return input
        return when (rule.position) {
            InsertPosition.PREFIX -> rule.text + input
            InsertPosition.SUFFIX -> input + rule.text
            InsertPosition.AT_INDEX -> {
                val idx = rule.atIndex.coerceIn(0, input.length)
                input.substring(0, idx) + rule.text + input.substring(idx)
            }
        }
    }

    private fun applySerial(input: String, rule: SerialNumber, ordinal: Int): String {
        val number = rule.start + ordinal * rule.step
        val digits = rule.digits.coerceAtLeast(1)
        val formatted = number.toString().padStart(digits, '0')
        val text = rule.prefix + formatted + rule.suffix
        return applyInsert(input, InsertText(0, true, rule.position, text, rule.atIndex))
    }

    private fun applySymbol(input: String, mode: SymbolMode): String = when (mode) {
        SymbolMode.DOT_TO_SPACE -> input.replace('.', ' ')
        SymbolMode.UNDERSCORE_TO_SPACE -> input.replace('_', ' ')
        SymbolMode.SPACE_TO_DOT -> input.replace(' ', '.')
    }

    private fun applyCleanup(input: String, mode: CleanupMode): String = when (mode) {
        CleanupMode.CONSECUTIVE_SPACES -> CONSECUTIVE_SPACES.replace(input, " ")
        CleanupMode.CONSECUTIVE_DOTS -> CONSECUTIVE_DOTS.replace(input, ".")
        CleanupMode.TRIM_ENDS -> input.trim(' ', '\t')
    }

    private fun applyRemoveTags(input: String): String =
        TAG_PATTERN.replace(input, "")

    private fun renderTemplate(template: String, current: String, ordinal: Int): String {
        if (template.isBlank()) return current
        val info = MediaNameParser.parse(current)
        val values = mapOf(
            "original" to current,
            "title" to info.title,
            "season" to info.season,
            "episode" to info.episode,
            "year" to info.year,
            "resolution" to info.resolution,
            "codec" to info.codec,
            "source" to info.source,
            "audio" to info.audio,
            "index" to (ordinal + 1).toString(),
        )
        return PLACEHOLDER.replace(template) { m ->
            values[m.groupValues[1]] ?: m.value // 未知占位符原样保留，便于用户发现错误
        }
    }

    // ---------- 检查 ----------

    /** 返回名称中的非法字符列表（为空表示合法） */
    fun illegalChars(name: String): List<Char> =
        name.toCharArray().filter { it in ILLEGAL_CHARS || it.code < 0x20 }.distinct()

    /** 完整文件名（含扩展名）是否超长 */
    fun isTooLong(fullName: String): Boolean = fullName.length > MAX_NAME_LENGTH

    /** 规则自身是否有错误（目前只有正则表达式可能出错） */
    fun ruleError(rule: RenameRule): String? {
        if (rule is FindReplace && rule.useRegex && rule.find.isNotEmpty()) {
            return try {
                Regex(rule.find)
                null
            } catch (e: Exception) {
                "正则表达式无效：${e.message?.take(40) ?: "语法错误"}"
            }
        }
        return null
    }

    /**
     * 计算两个字符串的"变化部分"位置，用于预览高亮。
     * 返回 (公共前缀长度, 变化段长度)，调用方可据此给中间部分上色。
     */
    fun diffMiddle(original: String, newName: String): Pair<Int, Int> {
        if (original == newName) return 0 to 0
        var prefix = 0
        val minLen = minOf(original.length, newName.length)
        while (prefix < minLen && original[prefix] == newName[prefix]) prefix++
        var suffix = 0
        while (suffix < minLen - prefix &&
            original[original.length - 1 - suffix] == newName[newName.length - 1 - suffix]
        ) suffix++
        return prefix to (newName.length - prefix - suffix)
    }
}
