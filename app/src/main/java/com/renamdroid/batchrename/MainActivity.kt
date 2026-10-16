package com.renamdroid.batchrename

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.renamdroid.batchrename.ui.AppNav
import com.renamdroid.batchrename.ui.MainViewModel
import com.renamdroid.batchrename.ui.theme.RenamdriodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            // 上次闪退留下的日志（只读一次）
            var crashText by remember { mutableStateOf(CrashReporter.latestCrashText(application)) }
            RenamdriodTheme(
                themeMode = settings.themeMode,
                themeColor = settings.themeColor,
            ) {
                AppNav()
                crashText?.let { text ->
                    val clipboard = LocalClipboardManager.current
                    AlertDialog(
                        onDismissRequest = { crashText = null },
                        title = { Text("检测到上次闪退") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 420.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    "应用上次异常退出了，下面是崩溃详情。" +
                                        "点「复制日志」后直接粘贴到聊天窗口发给我，就能定位问题。",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(text))
                                CrashReporter.clearCrashLogs(application)
                                crashText = null
                            }) { Text("复制日志") }
                        },
                        dismissButton = {
                            TextButton(onClick = { crashText = null }) { Text("知道了") }
                        },
                    )
                }
            }
        }
    }
}
