package org.artemchik.newmrim.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import org.artemchik.newmrim.ui.screens.ChatScreen
import org.artemchik.newmrim.ui.screens.ContactListScreen
import org.artemchik.newmrim.ui.screens.LoginScreen
import org.artemchik.newmrim.ui.screens.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
    const val CHAT = "chat/{email}"
    fun chat(email: String) = "chat/$email"
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Contacts : BottomNavItem(Routes.CONTACTS, "Контакты", Icons.Outlined.People, Icons.Filled.People)
    object Settings : BottomNavItem(Routes.SETTINGS, "Настройки", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun MrimNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) { 
            LoginScreen(onLoginSuccess = { 
                navController.navigate(Routes.MAIN) { 
                    popUpTo(Routes.LOGIN) { inclusive = true } 
                } 
            }) 
        }
        composable(Routes.MAIN) { MainScreen(rootNavController = navController) }
        composable(
            route = Routes.CHAT, 
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { 
            ChatScreen(onBack = { navController.popBackStack() }) 
        }
    }
}

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val navController = rememberNavController()
    val items = listOf(BottomNavItem.Contacts, BottomNavItem.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Routes.CONTACTS, 
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.CONTACTS) { 
                ContactListScreen(
                    onContactClick = { email -> 
                        rootNavController.navigate(Routes.chat(email)) 
                    },
                    onLogout = { 
                        rootNavController.navigate(Routes.LOGIN) { 
                            popUpTo(0) { inclusive = true } 
                        } 
                    }
                ) 
            }
            composable(Routes.SETTINGS) { 
                SettingsScreen(
                    onLogout = { 
                        rootNavController.navigate(Routes.LOGIN) { 
                            popUpTo(0) { inclusive = true } 
                        } 
                    }
                ) 
            }
        }
    }
}
