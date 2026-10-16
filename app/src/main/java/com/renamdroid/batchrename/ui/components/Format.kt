package com.renamdroid.batchrename.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.ui.graphics.vector.ImageVector
import com.renamdroid.batchrename.data.model.FileCategory
import java.util.Locale

/** 文件大小格式化 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) "${bytes} B"
    else String.format(Locale.US, "%.2f %s", value, units[unitIndex])
}

/** 时间格式化：今天显示 HH:mm，本年显示 M月d日，否则 yyyy/M/d */
fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = java.util.Calendar.getInstance()
    val time = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        now.get(java.util.Calendar.YEAR) == time.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == time.get(java.util.Calendar.DAY_OF_YEAR) ->
            String.format(Locale.US, "%02d:%02d", time.get(java.util.Calendar.HOUR_OF_DAY), time.get(java.util.Calendar.MINUTE))
        now.get(java.util.Calendar.YEAR) == time.get(java.util.Calendar.YEAR) ->
            "${time.get(java.util.Calendar.MONTH) + 1}月${time.get(java.util.Calendar.DAY_OF_MONTH)}日"
        else -> "${time.get(java.util.Calendar.YEAR)}/${time.get(java.util.Calendar.MONTH) + 1}/${time.get(java.util.Calendar.DAY_OF_MONTH)}"
    }
}

/** 分类图标 */
fun categoryIcon(category: FileCategory): ImageVector = when (category) {
    FileCategory.VIDEO -> Icons.Filled.Movie
    FileCategory.SUBTITLE -> Icons.Filled.Subtitles
    FileCategory.AUDIO -> Icons.Filled.Album
    FileCategory.IMAGE -> Icons.Filled.Image
    FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
}
