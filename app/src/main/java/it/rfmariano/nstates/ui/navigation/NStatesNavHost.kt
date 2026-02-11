package it.rfmariano.nstates.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import it.rfmariano.nstates.ui.issues.IssuesScreen
import it.rfmariano.nstates.ui.login.LoginScreen
import it.rfmariano.nstates.ui.nation.NationScreen
import it.rfmariano.nstates.ui.settings.SettingsScreen

@Composable
fun NStatesNavHost(
    navController: NavHostController,
    startDestination: String,
    initialPageRoute: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(initialPageRoute) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.NATION) {
            NationScreen()
        }

        composable(Routes.ISSUES) {
            IssuesScreen()
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
