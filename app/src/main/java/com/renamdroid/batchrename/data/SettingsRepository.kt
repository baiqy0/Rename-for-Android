package com.renamdroid.batchrename.data

import android.content.Context
import android.content.SharedPreferences
import com.renamdroid.batchrename.data.model.SortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

/** 主题配色。swatchHex 是设置页色板用的展示色（ARGB 长整型）。 */
enum class ThemeColor(val label: String, val swatchHex: Long) {
    /** 跟随壁纸取色（Material You），仅 Android 12+ 有效，低版本回退靛蓝 */
    DYNAMIC("动态取色", 0xFF6750A4),
    INDIGO("靛蓝", 0xFF4A5FE0),
    GREEN("青绿", 0xFF006B5D),
    PURPLE("绛紫", 0xFF7E5CD6),
    ORANGE("暖橙", 0xFFEF7B30),
    ROSE("玫红", 0xFFE05571),
}

/** 从文件夹批量导入时纳入的文件类型范围 */
enum class FolderImportScope(val label: String, val description: String) {
    MEDIA("视频 + 字幕 + 图片", "影视三件套一起导入，nfo/txt 等说明文件忽略"),
    VIDEO_SUB("仅视频 + 字幕", "只要正片和字幕文件"),
    ALL_MEDIA("全部媒体（含音频）", "把 mp3/flac 等音频文件也加入列表"),
    ALL("所有文件", "文件夹内全部文件，不做类型过滤"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: ThemeColor = ThemeColor.DYNAMIC,
    /** 重命名时保留原扩展名 */
    val keepExtension: Boolean = true,
    /** 执行前显示预览列表 */
    val showPreview: Boolean = true,
    /** 完成后显示撤销入口 */
    val showUndo: Boolean = true,
    /** 默认排序方式 */
    val defaultSort: SortMode = SortMode.NATURAL,
    /** 高级：启用正则表达式编辑器 */
    val regexEnabled: Boolean = false,
    /** 文件夹导入时的文件类型范围 */
    val folderImportScope: FolderImportScope = FolderImportScope.MEDIA,
)

/** 设置仓库（SharedPreferences + StateFlow） */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs.getString("themeMode", null) ?: defaults.themeMode.name)
            }.getOrDefault(defaults.themeMode),
            themeColor = runCatching {
                ThemeColor.valueOf(prefs.getString("themeColor", null) ?: defaults.themeColor.name)
            }.getOrDefault(defaults.themeColor),
            keepExtension = prefs.getBoolean("keepExtension", defaults.keepExtension),
            showPreview = prefs.getBoolean("showPreview", defaults.showPreview),
            showUndo = prefs.getBoolean("showUndo", defaults.showUndo),
            defaultSort = runCatching {
                SortMode.valueOf(prefs.getString("defaultSort", null) ?: defaults.defaultSort.name)
            }.getOrDefault(defaults.defaultSort),
            regexEnabled = prefs.getBoolean("regexEnabled", defaults.regexEnabled),
            folderImportScope = runCatching {
                FolderImportScope.valueOf(
                    prefs.getString("folderImportScope", null) ?: defaults.folderImportScope.name
                )
            }.getOrDefault(defaults.folderImportScope),
        )
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        val new = transform(_state.value)
        prefs.edit()
            .putString("themeMode", new.themeMode.name)
            .putString("themeColor", new.themeColor.name)
            .putBoolean("keepExtension", new.keepExtension)
            .putBoolean("showPreview", new.showPreview)
            .putBoolean("showUndo", new.showUndo)
            .putString("defaultSort", new.defaultSort.name)
            .putBoolean("regexEnabled", new.regexEnabled)
            .putString("folderImportScope", new.folderImportScope.name)
            .apply()
        _state.value = new
    }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setThemeColor(color: ThemeColor) = update { it.copy(themeColor = color) }
    fun setKeepExtension(value: Boolean) = update { it.copy(keepExtension = value) }
    fun setShowPreview(value: Boolean) = update { it.copy(showPreview = value) }
    fun setShowUndo(value: Boolean) = update { it.copy(showUndo = value) }
    fun setDefaultSort(mode: SortMode) = update { it.copy(defaultSort = mode) }
    fun setRegexEnabled(value: Boolean) = update { it.copy(regexEnabled = value) }
    fun setFolderImportScope(scope: FolderImportScope) = update { it.copy(folderImportScope = scope) }
}
