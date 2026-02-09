package it.rfmariano.nstates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import it.rfmariano.nstates.data.local.AuthLocalDataSource
import it.rfmariano.nstates.ui.navigation.NStatesNavHost
import it.rfmariano.nstates.ui.navigation.Routes
import it.rfmariano.nstates.ui.theme.NStatesTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authLocal: AuthLocalDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // If user has valid tokens, go straight to nation screen.
        // If user has a stored nation name but expired tokens, go to login
        // (LoginViewModel will pre-fill the nation name).
        val startDestination = if (authLocal.isLoggedIn) {
            Routes.NATION
        } else {
            Routes.LOGIN
        }

        setContent {
            NStatesTheme {
                val navController = rememberNavController()
                NStatesNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
