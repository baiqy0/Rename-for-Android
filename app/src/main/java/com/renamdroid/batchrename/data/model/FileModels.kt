package com.renamdroid.batchrename.data.model

import android.net.Uri

/** 文件分类，用于首页筛选和图标显示 */
enum class FileCategory(val label: String, val extensions: Set<String>) {
    VIDEO("视频", setOf("mkv", "mp4", "avi", "mov", "ts", "m2ts", "mts", "m2v", "ogv", "webm", "flv", "wmv", "rmvb", "rm", "m4v", "3gp", "mpg", "mpeg", "vob", "iso", "f4v", "divx")),
    SUBTITLE("字幕", setOf("ass", "srt", "sub", "idx", "sup", "ssa", "vtt", "sbv", "pgs")),
    AUDIO("音频", setOf("mp3", "flac", "aac", "wav", "ogg", "wma", "ape", "dts", "m4a")),
    IMAGE("图片", setOf("jpg", "jpeg", "png", "bmp", "webp", "gif", "tiff", "tif", "heic", "heif", "avif")),
    OTHER("其他", emptySet());

    companion object {
        fun of(extension: String): FileCategory {
            val ext = extension.lowercase()
            return entries.firstOrNull { ext in it.extensions } ?: OTHER
        }
    }
}

/** 文件筛选器 */
enum class FileFilter(val label: String) {
    ALL("全部"),
    VIDEO("视频"),
    SUBTITLE("字幕"),
    IMAGE("图片"),
    OTHER("其他"),
}

/** 排序方式 */
enum class SortMode(val label: String) {
    NATURAL("自然排序"),
    NAME_ASC("名称 A → Z"),
    NAME_DESC("名称 Z → A"),
    SIZE("文件大小"),
    MODIFIED("修改时间"),
}

/**
 * 一个被选中的文件。
 * 所有操作都通过 SAF 的 Uri 完成，不依赖文件绝对路径。
 */
data class FileItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String?,
    /** 父目录的 children Uri（用于重命名前检查目标名是否已存在）；无法解析时为 null */
    val parentChildrenUri: Uri? = null,
) {
    val id: String get() = uri.toString()

    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    /** 不含扩展名的文件名主体（规则都作用在它上面） */
    val baseName: String
        get() = if (name.contains('.')) name.substringBeforeLast('.') else name

    val category: FileCategory
        get() = FileCategory.of(extension)

    /** 重新拼接文件名（保留/替换扩展名） */
    fun withName(newBase: String, newExtension: String? = null): String {
        val ext = newExtension ?: extension
        return if (ext.isEmpty()) newBase else "$newBase.$ext"
    }
}
