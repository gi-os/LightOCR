package com.gios.lightocr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val ROUTE_SCAN = "scan"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_DETAIL = "detail/{id}"

/**
 * Two destinations, Scan and History, behind a bottom bar -- comfortably inside LightOS's
 * own bottom-bar rule of at most 5 icon items, or 3 when any item is text (here: 2, both
 * text). Opening a past scan is a third screen, but it is reached from History, not from
 * the bar, the same way LightPass's ticket detail isn't a fourth tab.
 */
@Composable
fun LightOcrNav() {
    val nav = rememberNavController()
    val vm: ScanViewModel = viewModel()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            NavHost(nav, startDestination = ROUTE_SCAN) {
                composable(ROUTE_SCAN) { ScanScreen(vm) }
                composable(ROUTE_HISTORY) {
                    HistoryScreen(vm, onOpen = { scan -> nav.navigate("detail/${scan.id}") })
                }
                composable(
                    ROUTE_DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments!!.getString("id")!!
                    DetailScreen(vm, id, onBack = { nav.popBackStack() })
                }
            }
        }
        if (currentRoute == ROUTE_SCAN || currentRoute == ROUTE_HISTORY) {
            LightBottomBar(
                items = listOf(
                    BottomTab.Item(
                        label = "Scan",
                        selected = currentRoute == ROUTE_SCAN,
                        onClick = { nav.navigate(ROUTE_SCAN) { launchSingleTop = true } },
                    ),
                    BottomTab.Item(
                        label = "History",
                        selected = currentRoute == ROUTE_HISTORY,
                        onClick = { nav.navigate(ROUTE_HISTORY) { launchSingleTop = true } },
                    ),
                ),
            )
        }
    }
}
