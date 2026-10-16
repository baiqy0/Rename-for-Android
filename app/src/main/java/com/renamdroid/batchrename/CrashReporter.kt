package com.renamdroid.batchrename

import android.app.Application
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃捕获器。
 *
 * 作用：应用闪退时（包括我们自己都没预料到的异常），把完整堆栈 + 设备信息
 * 写入 filesDir/crash-logs/，下次启动时由 MainActivity 弹窗展示，
 * 用户点「复制日志」粘贴给开发者即可定位问题。
 *
 * 注意：保存日志后仍然链回系统默认的异常处理器，
 * 保持"应用已停止"的原有行为，不会把进程留在假死状态。
 */
class CrashReporter(private val app: Application) : Thread.UncaughtExceptionHandler {

    /** 安装我们之前系统默认的处理器（RuntimeInit 的 KillApplicationHandler） */
    private val systemHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, error: Throwable) {
        // 保存日志本身不能再抛异常，否则会丢掉真正的堆栈
        runCatching { save(thread, error) }
        Log.e(TAG, "未捕获异常，线程：${thread.name}", error)

        // 链回系统默认处理：弹"应用已停止"并结束进程
        val handler = systemHandler
        if (handler != null && handler !== this) {
            handler.uncaughtException(thread, error)
        } else {
            runCatching { Process.killProcess(Process.myPid()) }
            runCatching { Runtime.getRuntime().exit(2) }
        }
    }

    private fun save(thread: Thread, error: Throwable) {
        val dir = File(app.filesDir, DIR_NAME).apply { mkdirs() }
        val now = Date()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(now)
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        val text = buildString {
            appendLine("=== 批量重命名 崩溃报告 ===")
            appendLine("包名：com.renamdroid.batchrename")
            appendLine("时间：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(now)}")
            appendLine("线程：${thread.name}")
            appendLine("机型：${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("系统：Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("---------- 堆栈 ----------")
            append(sw.toString())
        }
        File(dir, "crash-$stamp.txt").writeText(text)
        // 只保留最近 5 份，避免堆积
        dir.listFiles()
            ?.sortedBy { it.name }
            ?.dropLast(MAX_KEEP)
            ?.forEach { runCatching { it.delete() } }
    }

    companion object {
        private const val TAG = "CrashReporter"
        private const val DIR_NAME = "crash-logs"
        private const val MAX_KEEP = 5

        /** 在 Application.onCreate 中尽早调用 */
        fun install(app: Application) {
            Thread.setDefaultUncaughtExceptionHandler(CrashReporter(app))
        }

        /** 最近一次崩溃的报告文本（没有则返回 null） */
        fun latestCrashText(app: Application): String? {
            val latest = File(app.filesDir, DIR_NAME).listFiles()
                ?.maxByOrNull { it.lastModified() }
                ?: return null
            return runCatching { latest.readText() }.getOrNull()
        }

        /** 用户已取走日志，清空报告文件 */
        fun clearCrashLogs(app: Application) {
            runCatching {
                File(app.filesDir, DIR_NAME).listFiles()?.forEach { it.delete() }
            }
        }
    }
}
