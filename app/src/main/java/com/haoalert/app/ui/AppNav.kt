package com.haoalert.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.haoalert.app.ui.screens.HistoryScreen
import com.haoalert.app.ui.screens.HomeScreen
import com.haoalert.app.ui.screens.SettingsScreen
import com.haoalert.app.ui.screens.TaskDetailScreen
import com.haoalert.app.ui.screens.TaskFormScreen

private const val HOME = "home"
private const val FORM = "form?id={id}&restore={restore}"
private const val DETAIL = "detail/{id}"
private const val HISTORY = "history"
private const val SETTINGS = "settings"

@Composable
fun AppNav(
    vm: HaoViewModel,
    openTaskRequest: State<Long?>,
    onOpenTaskHandled: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val nav = rememberNavController()

    // Toque em uma notificação: abre direto os detalhes daquela tarefa.
    LaunchedEffect(openTaskRequest.value) {
        openTaskRequest.value?.let { id ->
            nav.navigate("detail/$id") { launchSingleTop = true }
            onOpenTaskHandled()
        }
    }

    NavHost(
        navController = nav,
        startDestination = HOME,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { it / 10 } },
    ) {
        composable(HOME) {
            HomeScreen(
                vm = vm,
                onNewTask = { nav.navigate("form") },
                onOpenTask = { nav.navigate("detail/$it") },
                onHistory = { nav.navigate(HISTORY) },
                onSettings = { nav.navigate(SETTINGS) },
                onRequestNotifications = onRequestNotifications,
            )
        }
        composable(
            FORM,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("restore") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: -1L
            val restore = entry.arguments?.getBoolean("restore") ?: false
            TaskFormScreen(
                vm = vm,
                taskId = id,
                restore = restore,
                onDone = {
                    // Ao restaurar, volta para a lista principal; ao editar/criar, volta uma tela.
                    if (restore) nav.popBackStack(HOME, inclusive = false) else nav.popBackStack()
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: -1L
            TaskDetailScreen(
                vm = vm,
                taskId = id,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("form?id=$it&restore=false") },
                onRestore = { nav.navigate("form?id=$it&restore=true") },
            )
        }
        composable(HISTORY) {
            HistoryScreen(vm = vm, onBack = { nav.popBackStack() }, onOpenTask = { nav.navigate("detail/$it") })
        }
        composable(SETTINGS) {
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() }, onRequestNotifications = onRequestNotifications)
        }
    }
}
