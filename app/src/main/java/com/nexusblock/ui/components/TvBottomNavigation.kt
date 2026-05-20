package com.nexusblock.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.tv.material3.*
import com.nexusblock.ui.Screen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvBottomNavigation(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        Screen.Dashboard,
        Screen.Blocklists,
        Screen.CustomRules,
        Screen.Firewall,
        Screen.Logs,
        Screen.Settings
    )

    Surface(
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { screen ->
                val selected = currentRoute == screen.route
                Button(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) {
                    Text(
                        text = screen.title,
                        style = if (selected)
                            MaterialTheme.typography.labelLarge
                        else
                            MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
