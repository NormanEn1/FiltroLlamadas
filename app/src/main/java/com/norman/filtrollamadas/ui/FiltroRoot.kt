package com.norman.filtrollamadas.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.norman.filtrollamadas.FiltroApp
import com.norman.filtrollamadas.domain.Decision
import com.norman.filtrollamadas.ui.home.HomeScreen
import com.norman.filtrollamadas.ui.lists.NumbersScreen
import com.norman.filtrollamadas.ui.log.LogScreen
import com.norman.filtrollamadas.ui.settings.SettingsScreen
import com.norman.filtrollamadas.ui.stats.StatsScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    INICIO("inicio", "Inicio", Icons.Rounded.Shield),
    NUMEROS("numeros", "Números", Icons.Rounded.Inbox),
    REGISTRO("registro", "Registro", Icons.Rounded.History),
    AJUSTES("ajustes", "Ajustes", Icons.Rounded.Tune),
}

private const val ROUTE_STATS = "estadisticas"

@Composable
fun FiltroRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    val app = LocalContext.current.applicationContext as FiltroApp
    val pendingFlow = remember { app.container.database.callDao().observePendingCount(Decision.DESVIADA.name) }
    val pending by pendingFlow.collectAsStateWithLifecycle(initialValue = 0)

    fun go(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = current == t.route,
                        onClick = { go(t.route) },
                        icon = {
                            if (t == Tab.NUMEROS && pending > 0) {
                                BadgedBox(badge = { Badge { Text(if (pending > 99) "99+" else pending.toString()) } }) {
                                    Icon(t.icon, contentDescription = null)
                                }
                            } else {
                                Icon(t.icon, contentDescription = null)
                            }
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.INICIO.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Tab.INICIO.route) {
                HomeScreen(
                    pendingCount = pending,
                    onOpenNumbers = { go(Tab.NUMEROS.route) },
                    onOpenStats = { nav.navigate(ROUTE_STATS) },
                    onOpenLog = { go(Tab.REGISTRO.route) },
                    onOpenSettings = { go(Tab.AJUSTES.route) },
                )
            }
            composable(Tab.NUMEROS.route) { NumbersScreen() }
            composable(Tab.REGISTRO.route) { LogScreen() }
            composable(Tab.AJUSTES.route) { SettingsScreen() }
            composable(ROUTE_STATS) { StatsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
