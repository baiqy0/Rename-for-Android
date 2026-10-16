package com.renamdroid.batchrename

import android.app.Application
import com.renamdroid.batchrename.data.FileRepository
import com.renamdroid.batchrename.data.HistoryRepository
import com.renamdroid.batchrename.data.SettingsRepository

/** 简单的服务定位器，整个 App 共享仓库实例 */
object ServiceLocator {
    lateinit var application: Application
        private set

    fun init(app: Application) {
        application = app
    }

    val fileRepository: FileRepository by lazy { FileRepository(application) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(application) }
}

class RenameApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 尽早安装崩溃捕获，保证任何阶段的闪退都能留下堆栈
        CrashReporter.install(this)
        ServiceLocator.init(this)
    }
}
