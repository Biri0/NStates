package it.rfmariano.nstates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Ballot
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import it.rfmariano.nstates.data.local.AuthLocalDataSource
import it.rfmariano.nstates.data.local.SettingsDataSource
import it.rfmariano.nstates.ui.navigation.NStatesNavHost
import it.rfmariano.nstates.ui.navigation.Routes
import it.rfmariano.nstates.ui.theme.NStatesTheme
import javax.inject.Inject

/**
 * Represents a tab in the bottom navigation bar.
 */
private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authLocal: AuthLocalDataSource

    @Inject
    lateinit var settingsDataSource: SettingsDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read the user's preferred initial page (persisted in DataStore).
        val initialPageRoute = settingsDataSource.getInitialPageSync()

        // If user has valid tokens, go to their preferred initial page.
        // If user has a stored nation name but expired tokens, go to login
        // (LoginViewModel will pre-fill the nation name).
        val requestedRoute = intent?.getStringExtra(EXTRA_ROUTE)
            ?.takeIf { it == Routes.NATION || it == Routes.ISSUES || it == Routes.SETTINGS }
        val startDestination = if (authLocal.isLoggedIn) {
            requestedRoute ?: initialPageRoute
        } else {
            Routes.LOGIN
        }

        setContent {
            NStatesTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Only show bottom nav when logged in (not on login screen)
                val isLoggedIn = currentRoute != Routes.LOGIN && currentRoute != Routes.LOGIN_ADD

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isLoggedIn) {
                            NStatesBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NStatesNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        initialPageRoute = initialPageRoute,
                        onAddNation = {
                            navController.navigate(Routes.LOGIN_ADD) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "extra_route"
    }
}

@Composable
private fun NStatesBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(
            label = "Nation",
            icon = Icons.Filled.Flag,
            route = Routes.NATION
        ),
        BottomNavItem(
            label = "Issues",
            icon = Icons.Filled.Ballot,
            route = Routes.ISSUES
        ),
        BottomNavItem(
            label = "Settings",
            icon = Icons.Filled.Settings,
            route = Routes.SETTINGS
        )
    )

    val telegramBlue = Color(0xFF2AABEE)

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = telegramBlue,
                        selectedTextColor = telegramBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
