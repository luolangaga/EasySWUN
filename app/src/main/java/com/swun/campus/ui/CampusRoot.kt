package com.swun.campus.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.swun.campus.AppViewModel

@Composable
fun CampusRoot(vm: AppViewModel, initialRoute: String? = null) {
    if (!vm.loggedIn) {
        LoginScreen(vm)
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val message = vm.globalMessage
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            vm.dismissMessage()
        }
    }

    MainScaffold(vm = vm, snackbarHostState = snackbarHostState, initialRoute = initialRoute)
}
