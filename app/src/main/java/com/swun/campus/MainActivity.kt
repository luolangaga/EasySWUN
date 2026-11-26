package com.swun.campus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swun.campus.ui.CampusRoot
import com.swun.campus.ui.theme.SWUNCampusTheme
import com.swun.campus.reminder.ClassReminderScheduler
import com.swun.campus.widget.BusWidgetProvider
import com.swun.campus.widget.FunctionsWidgetProvider
import com.swun.campus.widget.ScheduleWidgetProvider
import com.swun.campus.widget.WidgetNavigation

class MainActivity : ComponentActivity() {

    private var widgetRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetRoute = WidgetNavigation.routeFrom(intent)
        // 覆盖安装后桌面可能暂时回退到 initialLayout；App 启动时主动重建一次，
        // 保证课表/校车/功能卡片拿到最新数据和尺寸，而不是空白占位。
        ScheduleWidgetProvider.updateAll(this)
        BusWidgetProvider.updateAll(this)
        FunctionsWidgetProvider.updateAll(this)
        ClassReminderScheduler.reschedule(this)
        setContent {
            SWUNCampusTheme {
                val vm: AppViewModel = viewModel()
                CampusRoot(vm, initialRoute = widgetRoute)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetRoute = WidgetNavigation.routeFrom(intent)
    }
}
