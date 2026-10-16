package com.renamdroid.batchrename.data.model

/** 单个文件重命名失败的原因 */
data class RenameFailure(
    val uri: String,
    val fileName: String,
    val reason: String,
)

/** 一次批量重命名中成功改名的记录（Undo 用） */
data class RenamedItem(
    val uri: String,
    val originalName: String,
    val newName: String,
)

/** 批量重命名结果 */
data class RenameOutcome(
    val successCount: Int,
    val failures: List<RenameFailure>,
    val renamed: List<RenamedItem>,
) {
    val total: Int get() = successCount + failures.size
}

/** 一条操作历史 */
data class HistoryEntry(
    val id: Long,
    val timestamp: Long,
    val ruleSummary: String,
    val fileCount: Int,
    val items: List<RenamedItem>,
    val undone: Boolean = false,
)

/** 预览中的一行 */
data class PreviewRow(
    val file: FileItem,
    val newName: String,
    val newExtension: String,
    val error: String? = null,
) {
    val changed: Boolean get() = newName != file.name
}
