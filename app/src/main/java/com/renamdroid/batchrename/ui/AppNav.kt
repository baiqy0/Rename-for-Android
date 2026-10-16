package com.renamdroid.batchrename.ui

import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.renamdroid.batchrename.ui.screens.AboutScreen
import com.renamdroid.batchrename.ui.screens.FilesScreen
import com.renamdroid.batchrename.ui.screens.HistoryScreen
import com.renamdroid.batchrename.ui.screens.HomeScreen
import com.renamdroid.batchrename.ui.screens.RulesScreen
import com.renamdroid.batchrename.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Files : Screen("files")
    data object Rules : Screen("rules")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object About : Screen("about")
}

/**
 * 应用导航 + 系统文件选择器。
 * 选择器统一用 StartActivityForResult 手工构建 Intent，
 * 这样可以同时使用 EXTRA_ALLOW_MULTIPLE 和 EXTRA_INITIAL_URI。
 */
@Composable
fun AppNav() {
    val navController = rememberNavController()
    val vm: MainViewModel = viewModel()

    val pickFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uris = extractUris(data)
        if (uris.isNotEmpty()) vm.onFilesPicked(uris)
    }

    val pickFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val treeUri = data.data ?: return@rememberLauncherForActivityResult
        vm.onFolderPicked(treeUri)
    }

    fun launchFilePicker(initialUri: android.net.Uri? = null) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "audio/*", "text/*", "image/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            if (initialUri != null) putExtra(DocumentsExtra.INITIAL_URI, initialUri)
        }
        pickFilesLauncher.launch(intent)
    }

    fun launchFolderPicker(initialUri: android.net.Uri? = null) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            if (initialUri != null) putExtra(DocumentsExtra.INITIAL_URI, initialUri)
        }
        pickFolderLauncher.launch(intent)
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                vm = vm,
                onPickFiles = { launchFilePicker(null) },
                onPickFolder = { launchFolderPicker(null) },
                onOpenRecent = { uri ->
                    // 最近位置可能是文件夹（树形 Uri）也可能是单个文件，必须区别对待：
                    // 文件夹直接加载内容，单个文件直接加入列表，都不该再弹系统选择器
                    // （文件夹在系统的"打开文件"选择器里是无法被选中的）。
                    if (DocumentsContract.isTreeUri(uri)) vm.onFolderPicked(uri)
                    else vm.onFilesPicked(listOf(uri))
                    navController.navigate(Screen.Files.route)
                },
                onForgetRecent = { uri -> vm.forgetRecentLocation(uri) },
                onContinue = { navController.navigate(Screen.Files.route) },
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        composable(Screen.Files.route) {
            FilesScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onPickFiles = { launchFilePicker(null) },
                onPickFolder = { launchFolderPicker(null) },
                onNext = { navController.navigate(Screen.Rules.route) },
            )
        }
        composable(Screen.Rules.route) {
            RulesScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

private object DocumentsExtra {
    const val INITIAL_URI = "android.provider.extra.INITIAL_URI"
}

private fun extractUris(data: Intent): List<android.net.Uri> {
    val clip = data.clipData
    if (clip != null && clip.itemCount > 0) {
        return (0 until clip.itemCount).map { clip.getItemAt(it).uri }
    }
    return data.data?.let { listOf(it) } ?: emptyList()
}
