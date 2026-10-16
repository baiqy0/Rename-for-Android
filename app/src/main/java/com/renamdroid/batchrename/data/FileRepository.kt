package com.renamdroid.batchrename.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.renamdroid.batchrename.data.model.FileCategory
import com.renamdroid.batchrename.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException

/**
 * 文件仓库：所有文件操作都通过 Storage Access Framework 完成。
 * 不申请任何存储权限，不访问绝对路径。
 */
class FileRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    // ---------- 读取 ----------

    /**
     * 通过 SAF Uri 读取文件信息。
     * 单个文件读取失败会被跳过，不会影响其他文件。
     */
    suspend fun loadFiles(
        uris: List<Uri>,
        onProgress: (loaded: Int, total: Int) -> Unit = { _, _ -> },
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val result = ArrayList<FileItem>(uris.size)
        uris.forEachIndexed { index, uri ->
            onProgress(index + 1, uris.size)
            loadFile(uri)?.let { result.add(it) }
        }
        result
    }

    private fun loadFile(uri: Uri): FileItem? {
        return try {
            resolver.query(uri, PROJECTION, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val mtimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                val name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) cursor.getString(nameIndex)
                           else uri.lastPathSegment?.substringAfterLast('/') ?: return@use null
                if (name.isBlank()) return@use null
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                val mtime = if (mtimeIndex >= 0 && !cursor.isNull(mtimeIndex)) cursor.getLong(mtimeIndex) else 0L
                val mime = if (mimeIndex >= 0 && !cursor.isNull(mimeIndex)) cursor.getString(mimeIndex) else null
                FileItem(
                    uri = uri,
                    name = name,
                    sizeBytes = size,
                    lastModified = mtime,
                    mimeType = mime,
                    parentChildrenUri = parentChildrenUri(uri),
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 由文档 Uri 推导其父目录的 children Uri。
     * 无法推导时（例如下载提供者）返回 null，此时跳过"目标已存在"检查。
     */
    private fun parentChildrenUri(uri: Uri): Uri? {
        return try {
            val authority = uri.authority ?: return null
            val docId = DocumentsContract.getDocumentId(uri)
            if (!docId.contains('/')) return null
            val parentId = docId.substringBeforeLast('/')
            DocumentsContract.buildChildDocumentsUri(authority, parentId)
        } catch (e: Exception) {
            null
        }
    }

    /** 查询某个目录下已有的文件名（用于冲突检测） */
    suspend fun childNames(childrenUri: Uri): Set<String> = withContext(Dispatchers.IO) {
        try {
            resolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (nameIndex < 0) return@withContext emptySet()
                    val names = LinkedHashSet<String>()
                    while (cursor.moveToNext()) {
                        if (!cursor.isNull(nameIndex)) names.add(cursor.getString(nameIndex))
                    }
                    names
                } ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /** 列出文件夹中的媒体文件（选择文件夹时使用），类型范围由设置决定 */
    suspend fun listFolderFiles(
        treeUri: Uri,
        scope: FolderImportScope = FolderImportScope.MEDIA,
    ): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
            root.listFiles()
                .filter { it.isFile }
                .mapNotNull { doc ->
                    val name = doc.name ?: return@mapNotNull null
                    val category = FileCategory.of(name.substringAfterLast('.', ""))
                    // 按用户设置的范围过滤类型，避免把 nfo/jpg 之类混进来
                    val allowed = when (scope) {
                        FolderImportScope.MEDIA ->
                            setOf(FileCategory.VIDEO, FileCategory.SUBTITLE, FileCategory.IMAGE)
                        FolderImportScope.VIDEO_SUB ->
                            setOf(FileCategory.VIDEO, FileCategory.SUBTITLE)
                        FolderImportScope.ALL_MEDIA ->
                            setOf(
                                FileCategory.VIDEO,
                                FileCategory.SUBTITLE,
                                FileCategory.IMAGE,
                                FileCategory.AUDIO,
                            )
                        FolderImportScope.ALL -> null // 不过滤
                    }
                    if (allowed != null && category !in allowed) return@mapNotNull null
                    FileItem(
                        uri = doc.uri,
                        name = name,
                        sizeBytes = doc.length(),
                        lastModified = doc.lastModified(),
                        mimeType = doc.type,
                        parentChildrenUri = parentChildrenUri(doc.uri),
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------- 权限 ----------

    /** 持久化 Uri 权限（SD 卡目录授权后长期有效） */
    fun persistPermission(uri: Uri): Boolean {
        return try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 当前持有的持久化权限列表 */
    fun persistedPermissions(): List<Uri> =
        try {
            resolver.persistedUriPermissions
                .filter { it.isReadPermission && it.isWritePermission }
                .map { it.uri }
        } catch (e: Exception) {
            emptyList()
        }

    /** 最近授权位置（用于首页"最近位置"） */
    data class RecentLocation(val uri: Uri, val displayName: String)

    fun recentLocations(): List<RecentLocation> {
        return try {
            resolver.persistedUriPermissions
                .filter { it.isReadPermission && it.isWritePermission }
                .mapNotNull { perm ->
                    val uri = perm.uri
                    // 可能是文件夹（树形 Uri）也可能是单个文件，两种都要能拿到名字
                    val name = runCatching {
                        DocumentFile.fromTreeUri(context, uri)?.name
                    }.getOrNull()
                        ?: runCatching {
                            DocumentFile.fromSingleUri(context, uri)?.name
                        }.getOrNull()
                        ?: uri.lastPathSegment?.substringAfterLast('/')
                    if (name.isNullOrBlank()) null else RecentLocation(uri, name)
                }
                .take(6)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 目录当前是否可读。
     * 用于区分"文件夹里没有影视文件"和"文件夹已被移动/删除、授权已失效"。
     */
    fun canReadTree(treeUri: Uri): Boolean = try {
        DocumentFile.fromTreeUri(context, treeUri)?.canRead() == true
    } catch (e: Exception) {
        false
    }

    /**
     * 忘记某个最近位置：释放持久化授权（之后列表里不再出现它）。
     * 返回 false 表示授权本来就不存在或已被系统回收。
     */
    fun forgetRecentLocation(uri: Uri): Boolean {
        return try {
            resolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    // ---------- 写入 ----------

    /**
     * 重命名单个文件。
     * @return 成功时返回重命名后的新 Uri（旧 Uri 会失效，撤销时必须用这个新 Uri），
     *         失败时返回带中文原因的异常
     */
    suspend fun rename(item: FileItem, newFullName: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val newUri = DocumentsContract.renameDocument(resolver, item.uri, newFullName)
                if (newUri != null) {
                    // 尝试把新 Uri 的权限也持久化，让撤销在应用重启后依然可用
                    runCatching { persistPermission(newUri) }
                    Result.success(newUri)
                } else {
                    Result.failure(IOException("重命名失败"))
                }
            } catch (e: SecurityException) {
                Result.failure(IOException("没有权限"))
            } catch (e: FileNotFoundException) {
                Result.failure(IOException("文件不存在"))
            } catch (e: IllegalArgumentException) {
                Result.failure(IOException("文件名不合法"))
            } catch (e: Exception) {
                Result.failure(IOException(e.message ?: "未知错误"))
            }
        }

    companion object {
        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
    }
}
