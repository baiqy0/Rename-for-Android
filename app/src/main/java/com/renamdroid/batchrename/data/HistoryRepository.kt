package com.renamdroid.batchrename.data

import android.content.Context
import com.renamdroid.batchrename.data.model.HistoryEntry
import com.renamdroid.batchrename.data.model.RenamedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 操作历史与撤销数据仓库。
 *
 * - 只保存在本机 SharedPreferences，不上传任何内容；
 * - 保存最近 20 次操作，每次操作记录 原URI / 原文件名 / 新文件名，
 *   撤销时不依赖路径，直接对 Uri 反向重命名；
 * - 批量执行过程中会增量落盘（每 25 个文件），
 *   即使应用被杀死，已成功的部分仍然可以撤销。
 */
class HistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("rename_history", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(load())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    // ---------- 读取 ----------

    private fun load(): List<HistoryEntry> {
        val raw = prefs.getString("entries", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val itemsArray = obj.getJSONArray("items")
                val items = (0 until itemsArray.length()).map { j ->
                    val it = itemsArray.getJSONObject(j)
                    RenamedItem(
                        uri = it.getString("uri"),
                        originalName = it.getString("o"),
                        newName = it.getString("n"),
                    )
                }
                HistoryEntry(
                    id = obj.getLong("id"),
                    timestamp = obj.getLong("ts"),
                    ruleSummary = obj.getString("summary"),
                    fileCount = obj.getInt("count"),
                    items = items,
                    undone = obj.optBoolean("undone", false),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val itemsArray = JSONArray()
            entry.items.forEach { item ->
                itemsArray.put(
                    JSONObject().apply {
                        put("uri", item.uri)
                        put("o", item.originalName)
                        put("n", item.newName)
                    }
                )
            }
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("ts", entry.timestamp)
                    put("summary", entry.ruleSummary)
                    put("count", entry.fileCount)
                    put("undone", entry.undone)
                    put("items", itemsArray)
                }
            )
        }
        prefs.edit().putString("entries", array.toString()).apply()
        _history.value = entries
    }

    // ---------- 写入 ----------

    /** 开始一次批量操作会话，返回会话 id */
    fun beginSession(ruleSummary: String): Long {
        val id = System.currentTimeMillis()
        sessionId = id
        sessionSummary = ruleSummary
        sessionItems = mutableListOf()
        return id
    }

    private var sessionId: Long = 0L
    private var sessionSummary: String = ""
    private var sessionItems: MutableList<RenamedItem> = mutableListOf()
    private var unsavedCount = 0

    /** 记录一个成功改名的文件（会周期性落盘） */
    fun record(item: RenamedItem) {
        if (sessionId == 0L) return
        sessionItems.add(item)
        unsavedCount++
        if (unsavedCount >= 25) {
            flushSession()
        }
    }

    private fun flushSession() {
        unsavedCount = 0
        if (sessionId == 0L || sessionItems.isEmpty()) return
        val entry = HistoryEntry(
            id = sessionId,
            timestamp = sessionId,
            ruleSummary = sessionSummary,
            fileCount = sessionItems.size,
            items = sessionItems.toList(),
        )
        val merged = _history.value.filter { it.id != entry.id } + entry
        persist(merged.sortedByDescending { it.timestamp }.take(MAX_ENTRIES))
    }

    /** 结束会话（最终落盘） */
    fun endSession() {
        flushSession()
        sessionId = 0L
        sessionItems = mutableListOf()
        unsavedCount = 0
    }

    fun markUndone(id: Long) {
        val updated = _history.value.map {
            if (it.id == id) it.copy(undone = true) else it
        }
        persist(updated)
    }

    fun clear() {
        persist(emptyList())
    }

    companion object {
        const val MAX_ENTRIES = 20
    }
}
