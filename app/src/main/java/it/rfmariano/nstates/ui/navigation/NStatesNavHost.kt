package it.rfmariano.nstates.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import it.rfmariano.nstates.ui.issues.IssuesScreen
import it.rfmariano.nstates.ui.login.LoginScreen
import it.rfmariano.nstates.ui.nation.NationScreen

@Composable
fun NStatesNavHost(
    navController: NavHostController,
    startDestination: String,
    onLogout: () -> Unit,
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
                    navController.navigate(Routes.NATION) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.NATION) {
            NationScreen(
                onLogout = onLogout
            )
        }

        composable(Routes.ISSUES) {
            IssuesScreen()
        }
    }
}
