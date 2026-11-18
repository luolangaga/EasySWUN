package com.swun.campus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swun.campus.ui.CampusRoot
import com.swun.campus.ui.theme.SWUNCampusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SWUNCampusTheme {
                val vm: AppViewModel = viewModel()
                CampusRoot(vm)
            }
        }
    }
}
