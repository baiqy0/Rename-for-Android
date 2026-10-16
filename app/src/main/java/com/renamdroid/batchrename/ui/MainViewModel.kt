package com.renamdroid.batchrename.ui

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.renamdroid.batchrename.ServiceLocator
import com.renamdroid.batchrename.core.NaturalSort
import com.renamdroid.batchrename.data.FileRepository
import com.renamdroid.batchrename.data.RenameEngine
import com.renamdroid.batchrename.data.model.FileFilter
import com.renamdroid.batchrename.data.model.FileItem
import com.renamdroid.batchrename.data.model.HistoryEntry
import com.renamdroid.batchrename.data.model.PreviewRow
import com.renamdroid.batchrename.data.model.RenameFailure
import com.renamdroid.batchrename.data.model.RenameOutcome
import com.renamdroid.batchrename.data.model.RenameRule
import com.renamdroid.batchrename.data.model.RenamedItem
import com.renamdroid.batchrename.data.model.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------- UI 状态 ----------

data class FilesUiState(
    val loading: Boolean = false,
    val loaded: Int = 0,
    val total: Int = 0,
    val filter: FileFilter = FileFilter.ALL,
    val sortMode: SortMode = SortMode.NATURAL,
    val files: List<FileItem> = emptyList(),
    val selected: Set<String> = emptySet(),
) {
    /** 当前筛选 + 排序后的列表 */
    val visibleFiles: List<FileItem>
        get() {
            val filtered = when (filter) {
                FileFilter.ALL -> files
                FileFilter.VIDEO -> files.filter { it.category.label == FileFilter.VIDEO.label }
                FileFilter.SUBTITLE -> files.filter { it.category.label == FileFilter.SUBTITLE.label }
                FileFilter.IMAGE -> files.filter { it.category.label == FileFilter.IMAGE.label }
                FileFilter.OTHER -> files.filter { it.category.label == FileFilter.OTHER.label }
            }
            return sortFiles(filtered, sortMode)
        }

    /** 被选中且在可见列表中的文件（重命名只处理这些） */
    val selectedVisibleFiles: List<FileItem>
        get() = visibleFiles.filter { it.id in selected }

    companion object {
        fun sortFiles(files: List<FileItem>, mode: SortMode): List<FileItem> = when (mode) {
            SortMode.NATURAL -> files.sortedWith { a, b -> NaturalSort.compare(a.name, b.name) }
            SortMode.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            SortMode.SIZE -> files.sortedByDescending { it.sizeBytes }
            SortMode.MODIFIED -> files.sortedByDescending { it.lastModified }
        }
    }
}

data class RulesUiState(
    val rules: List<RenameRule> = emptyList(),
    val preview: List<PreviewRow> = emptyList(),
    val conflictCount: Int = 0,
    val errorCount: Int = 0,
    val computing: Boolean = false,
    /** 自定义扩展名（null = 保留原扩展名） */
    val extensionOverride: String? = null,
) {
    val changedCount: Int get() = preview.count { it.changed && it.error == null }
    val canExecute: Boolean get() = errorCount == 0 && changedCount > 0 && !computing
}

sealed class ExecState {
    data object Idle : ExecState()
    data class Running(val done: Int, val total: Int) : ExecState()
    data class Finished(val outcome: RenameOutcome) : ExecState()
}

/**
 * 整个 App 的核心 ViewModel（Activity 作用域，跨页面共享）。
 *
 * 职责：文件选择/筛选/排序、规则链管理、实时预览、冲突检测、
 *       批量重命名执行、撤销、历史记录。
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val fileRepo = ServiceLocator.fileRepository
    private val settingsRepo = ServiceLocator.settingsRepository
    private val historyRepo = ServiceLocator.historyRepository

    val settings = settingsRepo.state
    val history = historyRepo.history

    private val _filesState = MutableStateFlow(FilesUiState(sortMode = settings.value.defaultSort))
    val filesState: StateFlow<FilesUiState> = _filesState.asStateFlow()

    private val _rulesState = MutableStateFlow(RulesUiState())
    val rulesState: StateFlow<RulesUiState> = _rulesState.asStateFlow()

    private val _execState = MutableStateFlow<ExecState>(ExecState.Idle)
    val execState: StateFlow<ExecState> = _execState.asStateFlow()

    private val _recentLocations = MutableStateFlow<List<FileRepository.RecentLocation>>(emptyList())
    val recentLocations: StateFlow<List<FileRepository.RecentLocation>> = _recentLocations.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** 父目录名缓存（parentChildrenUri → 已有文件名集合），用于冲突检测 */
    private val parentNameCache = java.util.concurrent.ConcurrentHashMap<Uri, Set<String>>()

    private var nextRuleId = System.currentTimeMillis()

    init {
        refreshRecentLocations()
        viewModelScope.launch {
            // 设置里的默认排序变化时同步列表
            settingsRepo.state.collect { s ->
                if (_filesState.value.sortMode != s.defaultSort && _filesState.value.files.isEmpty()) {
                    _filesState.value = _filesState.value.copy(sortMode = s.defaultSort)
                }
            }
        }
    }

    // ---------- 文件选择 ----------

    fun onFilesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _filesState.value = _filesState.value.copy(loading = true, loaded = 0, total = uris.size)
            try {
                uris.forEach { fileRepo.persistPermission(it) }
                val items = fileRepo.loadFiles(uris) { loaded, total ->
                    _filesState.value = _filesState.value.copy(loaded = loaded, total = total)
                }.distinctBy { it.id }
                val existingIds = _filesState.value.files.map { it.id }.toSet()
                val merged = _filesState.value.files + items.filter { it.id !in existingIds }
                val selected = _filesState.value.selected + items.map { it.id }
                _filesState.value = _filesState.value.copy(
                    files = merged,
                    selected = selected,
                )
                refreshRecentLocations()
            } catch (e: Throwable) {
                // 任何意外都只提示、不闪退（Throwable：连类初始化失败这类 Error 也接住）
                _messages.emit("读取文件失败：${e.message ?: e::class.simpleName ?: "未知错误"}")
            } finally {
                _filesState.value = _filesState.value.copy(loading = false)
            }
            refreshPreview()
        }
    }

    fun onFolderPicked(treeUri: Uri) {
        viewModelScope.launch {
            _filesState.value = _filesState.value.copy(loading = true, loaded = 0, total = 0)
            try {
                fileRepo.persistPermission(treeUri)
                // 按设置里的导入范围决定纳入哪些类型（默认 视频+字幕+图片）
                val items = fileRepo.listFolderFiles(treeUri, settings.value.folderImportScope)
                    .distinctBy { it.id }
                val existingIds = _filesState.value.files.map { it.id }.toSet()
                val merged = _filesState.value.files + items.filter { it.id !in existingIds }
                val selected = _filesState.value.selected + items.map { it.id }
                _filesState.value = _filesState.value.copy(
                    files = merged,
                    selected = selected,
                )
                refreshRecentLocations()
                _messages.emit(
                    when {
                        items.isNotEmpty() -> "已添加 ${items.size} 个文件"
                        !fileRepo.canReadTree(treeUri) -> "无法访问该文件夹（可能已被移动或删除，请重新选择）"
                        else -> "该文件夹中没有影视/字幕文件"
                    }
                )
            } catch (e: Throwable) {
                _messages.emit("读取文件夹失败：${e.message ?: e::class.simpleName ?: "未知错误"}")
            } finally {
                _filesState.value = _filesState.value.copy(loading = false)
            }
            refreshPreview()
        }
    }

    fun refreshRecentLocations() {
        _recentLocations.value = fileRepo.recentLocations()
    }

    /**
     * 从"最近位置"里删除某一项：
     * 1. 释放该目录/文件的持久化授权（不释放的话刷新列表它还会回来）
     * 2. 把当前文件列表里属于该位置的文件一并移除（授权没了，留着也没法重命名）
     */
    fun forgetRecentLocation(uri: Uri) {
        val current = _filesState.value.files
        val kept = current.filterNot { belongsTo(it.uri, uri) }
        if (kept.size != current.size) {
            val removedIds = current.map { it.id }.toSet() - kept.map { it.id }.toSet()
            _filesState.value = _filesState.value.copy(
                files = kept,
                selected = _filesState.value.selected - removedIds,
            )
        }
        fileRepo.forgetRecentLocation(uri)
        refreshRecentLocations()
        refreshPreview()
    }

    /** 文件 Uri 是否隶属于某个已授权位置（树形目录或单个文件） */
    private fun belongsTo(fileUri: Uri, locationUri: Uri): Boolean {
        if (fileUri == locationUri) return true
        if (!DocumentsContract.isTreeUri(locationUri)) return false
        if (fileUri.authority != locationUri.authority) return false
        val treeId = runCatching { DocumentsContract.getTreeDocumentId(locationUri) }
            .getOrNull() ?: return false
        val fileId = runCatching { DocumentsContract.getDocumentId(fileUri) }
            .getOrNull() ?: return false
        return fileId == treeId || fileId.startsWith("$treeId/")
    }

    // ---------- 列表操作 ----------

    fun setFilter(filter: FileFilter) {
        _filesState.value = _filesState.value.copy(filter = filter)
        refreshPreview()
    }

    fun setSortMode(mode: SortMode) {
        _filesState.value = _filesState.value.copy(sortMode = mode)
        refreshPreview()
    }

    fun toggleSelect(id: String) {
        val current = _filesState.value.selected
        _filesState.value = _filesState.value.copy(
            selected = if (id in current) current - id else current + id
        )
        refreshPreview()
    }

    fun selectAll(visible: List<FileItem>, select: Boolean) {
        val ids = visible.map { it.id }.toSet()
        _filesState.value = _filesState.value.copy(
            selected = if (select) _filesState.value.selected + ids
                       else _filesState.value.selected - ids
        )
        refreshPreview()
    }

    fun invertSelection(visible: List<FileItem>) {
        val visibleIds = visible.map { it.id }.toSet()
        val others = _filesState.value.selected - visibleIds
        val inverted = (visibleIds - _filesState.value.selected)
        _filesState.value = _filesState.value.copy(selected = others + inverted)
        refreshPreview()
    }

    fun removeFiles(ids: Set<String>) {
        _filesState.value = _filesState.value.copy(
            files = _filesState.value.files.filter { it.id !in ids },
            selected = _filesState.value.selected - ids,
        )
        refreshPreview()
    }

    fun clearFiles() {
        _filesState.value = _filesState.value.copy(files = emptyList(), selected = emptySet())
        refreshPreview()
    }

    // ---------- 规则链 ----------

    fun addRule(rule: RenameRule) {
        _rulesState.value = _rulesState.value.copy(rules = _rulesState.value.rules + rule)
        refreshPreview()
    }

    fun updateRule(rule: RenameRule) {
        _rulesState.value = _rulesState.value.copy(
            rules = _rulesState.value.rules.map { if (it.id == rule.id) rule else it }
        )
        refreshPreview()
    }

    fun removeRule(id: Long) {
        _rulesState.value = _rulesState.value.copy(
            rules = _rulesState.value.rules.filter { it.id != id }
        )
        refreshPreview()
    }

    fun toggleRule(id: Long) {
        _rulesState.value = _rulesState.value.copy(
            rules = _rulesState.value.rules.map {
                if (it.id == id) it.withEnabled(!it.enabled) else it
            }
        )
        refreshPreview()
    }

    fun moveRule(id: Long, delta: Int) {
        val list = _rulesState.value.rules.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = index + delta
        if (target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        _rulesState.value = _rulesState.value.copy(rules = list)
        refreshPreview()
    }

    fun newRuleId(): Long = ++nextRuleId

    fun setExtensionOverride(ext: String?) {
        _rulesState.value = _rulesState.value.copy(extensionOverride = ext?.removePrefix("."))
        refreshPreview()
    }

    // ---------- 预览 ----------

    /**
     * 重新计算预览。文件操作（目录名查询）在后台线程，
     * 纯字符串计算量很小，几百上千个文件也不会卡 UI。
     */
    fun refreshPreview() {
        viewModelScope.launch {
            _rulesState.value = _rulesState.value.copy(computing = true)
            val state = try {
                withContext(Dispatchers.Default) { computePreview() }
            } catch (e: Throwable) {
                // 预览只是辅助功能：任何异常（含 Error，例如类初始化失败）都只提示、不闪退。
                // ExceptionInInitializerError / NoClassDefFoundError 都是 Error，
                // 只 catch Exception 是接不住的，这里必须用 Throwable。
                _messages.emit("预览计算失败：${e.message ?: e::class.simpleName ?: "未知错误"}")
                _rulesState.value.copy(computing = false)
            }
            _rulesState.value = state
        }
    }

    private suspend fun computePreview(): RulesUiState {
        val files = _filesState.value.selectedVisibleFiles
        val rules = _rulesState.value.rules
        val extOverride = _rulesState.value.extensionOverride
        val keepExtension = settings.value.keepExtension

        // 1. 先把每个文件的新名字算出来（纯字符串）
        val staged = files.mapIndexed { index, file ->
            val newBase = RenameEngine.applyRuleChain(file.baseName, rules, index)
            val newExt = if (keepExtension) file.extension else (extOverride ?: file.extension)
            Triple(file, newBase, newExt)
        }

        // 2. 选中的文件之间是否重名
        val nameCount = staged.groupingBy { (file, newBase, newExt) ->
            "${file.parentChildrenUri ?: file.id}|${file.withName(newBase, newExt)}"
        }.eachCount()

        // 3. 目标是否已存在于磁盘（按父目录查询，带缓存）
        val parentUris = staged.mapNotNull { (file, _, _) -> file.parentChildrenUri }
            .distinct()
            .filter { !parentNameCache.containsKey(it) }
        if (parentUris.isNotEmpty()) {
            parentUris.forEach { parent ->
                parentNameCache[parent] = fileRepo.childNames(parent)
            }
        }

        val rows = staged.map { (file, newBase, newExt) ->
            val newFullName = file.withName(newBase, newExt)
            val key = "${file.parentChildrenUri ?: file.id}|$newFullName"
            val error = when {
                newFullName.isBlank() || newBase.isBlank() -> "文件名为空"
                RenameEngine.illegalChars(newFullName).isNotEmpty() ->
                    "包含非法字符：${RenameEngine.illegalChars(newFullName).joinToString(" ")}"
                RenameEngine.isTooLong(newFullName) -> "文件名过长（超过 ${RenameEngine.MAX_NAME_LENGTH} 个字符）"
                (nameCount[key] ?: 0) > 1 -> "与选中的其他文件重名"
                else -> {
                    val existing = file.parentChildrenUri?.let { parentNameCache[it] }
                    // 目标名已存在，且不是文件自己 → 冲突
                    if (existing != null && newFullName in existing && newFullName != file.name)
                        "目标文件已存在"
                    else null
                }
            }
            PreviewRow(
                file = file,
                newName = newBase,
                newExtension = newExt,
                error = error,
            )
        }

        return RulesUiState(
            rules = rules,
            preview = rows,
            conflictCount = rows.count { it.error != null },
            errorCount = rows.count { it.error != null },
            computing = false,
            extensionOverride = extOverride,
        )
    }

    // ---------- 执行重命名 ----------

    fun startRename() {
        if (_execState.value is ExecState.Running) return
        val rows = _rulesState.value.preview.filter { it.changed && it.error == null }
        if (rows.isEmpty()) return
        viewModelScope.launch {
            _execState.value = ExecState.Running(0, rows.size)
            val summary = _rulesState.value.rules
                .filter { it.enabled }
                .joinToString("；") { it.summary() }
                .ifEmpty { "重命名" }
            historyRepo.beginSession(summary)

            val failures = mutableListOf<RenameFailure>()
            val renamed = mutableListOf<RenamedItem>()
            rows.forEachIndexed { index, row ->
                val item = row.file
                val newFullName = row.file.withName(row.newName, row.newExtension)
                val result = fileRepo.rename(item, newFullName)
                if (result.isSuccess) {
                    val newUri = result.getOrThrow()
                    renamed.add(RenamedItem(newUri.toString(), item.name, newFullName))
                    historyRepo.record(RenamedItem(newUri.toString(), item.name, newFullName))
                    // 本地同步：旧 Uri 已失效，换成新 Uri 和新文件名
                    val updated = _filesState.value.files.map {
                        if (it.id == item.id) it.copy(uri = newUri, name = newFullName) else it
                    }
                    _filesState.value = _filesState.value.copy(files = updated)
                } else {
                    failures.add(
                        RenameFailure(
                            uri = item.id,
                            fileName = item.name,
                            reason = result.exceptionOrNull()?.message ?: "未知错误",
                        )
                    )
                }
                _execState.value = ExecState.Running(index + 1, rows.size)
            }
            historyRepo.endSession()
            parentNameCache.clear()
            refreshPreview()
            _execState.value = ExecState.Finished(
                RenameOutcome(
                    successCount = renamed.size,
                    failures = failures,
                    renamed = renamed,
                )
            )
        }
    }

    // ---------- 撤销 ----------

    fun undoEntry(entry: HistoryEntry) {
        if (entry.items.isEmpty()) return
        viewModelScope.launch {
            _execState.value = ExecState.Running(0, entry.items.size)
            var success = 0
            val failures = mutableListOf<RenameFailure>()
            entry.items.forEachIndexed { index, item ->
                val uri = runCatching { Uri.parse(item.uri) }.getOrNull()
                if (uri == null) {
                    failures.add(RenameFailure(item.uri, item.newName, "无法解析文件地址"))
                } else {
                    // item.uri 是重命名后的新 Uri，用它把文件名改回去
                    val fileItem = FileItem(uri, item.newName, 0L, 0L, null, null)
                    val result = fileRepo.rename(fileItem, item.originalName)
                    if (result.isSuccess) {
                        val newUri = result.getOrThrow()
                        // 本地同步：列表里的 Uri 换回撤销后的 Uri
                        val updated = _filesState.value.files.map {
                            if (it.id == item.uri) it.copy(uri = newUri, name = item.originalName) else it
                        }
                        _filesState.value = _filesState.value.copy(files = updated)
                        success++
                    } else {
                        failures.add(
                            RenameFailure(item.uri, item.newName, result.exceptionOrNull()?.message ?: "未知错误")
                        )
                    }
                }
                _execState.value = ExecState.Running(index + 1, entry.items.size)
            }
            historyRepo.markUndone(entry.id)
            refreshPreview()
            _execState.value = ExecState.Idle
            _messages.emit(
                if (failures.isEmpty()) "已撤销 $success 个文件"
                else "撤销完成：成功 $success，失败 ${failures.size}"
            )
        }
    }

    fun dismissExecResult() {
        _execState.value = ExecState.Idle
    }

    // ---------- 设置 ----------

    fun setThemeMode(mode: com.renamdroid.batchrename.data.ThemeMode) = settingsRepo.setThemeMode(mode)
    fun setThemeColor(color: com.renamdroid.batchrename.data.ThemeColor) = settingsRepo.setThemeColor(color)
    fun setFolderImportScope(scope: com.renamdroid.batchrename.data.FolderImportScope) =
        settingsRepo.setFolderImportScope(scope)
    fun setKeepExtension(value: Boolean) {
        settingsRepo.setKeepExtension(value)
        refreshPreview()
    }
    fun setShowPreview(value: Boolean) = settingsRepo.setShowPreview(value)
    fun setShowUndo(value: Boolean) = settingsRepo.setShowUndo(value)
    fun setDefaultSort(mode: SortMode) {
        settingsRepo.setDefaultSort(mode)
        _filesState.value = _filesState.value.copy(sortMode = mode)
        refreshPreview()
    }
    fun setRegexEnabled(value: Boolean) = settingsRepo.setRegexEnabled(value)

    fun clearHistory() {
        historyRepo.clear()
    }
}
