package com.joanita.wheeloflife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.joanita.wheeloflife.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WheelOfLifeTheme {
                val nav = rememberNavController()
                val vm: WheelViewModel = viewModel()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = route == "wheel",
                                onClick = { nav.navigate("wheel") { popUpTo("wheel"); launchSingleTop = true } },
                                icon = { Icon(Icons.Filled.DonutLarge, contentDescription = null) },
                                label = { Text("Wheel") }
                            )
                            NavigationBarItem(
                                selected = route == "areas" || route?.startsWith("tasks") == true,
                                onClick = { nav.navigate("areas") { popUpTo("wheel"); launchSingleTop = true } },
                                icon = { Icon(Icons.Filled.Category, contentDescription = null) },
                                label = { Text("Areas") }
                            )
                        }
                    }
                ) { pad ->
                    NavHost(nav, startDestination = "wheel", Modifier.padding(pad)) {
                        composable("wheel") {
                            WheelScreen(vm) { id -> nav.navigate("tasks/$id") }
                        }
                        composable("areas") {
                            CategoriesScreen(vm) { id -> nav.navigate("tasks/$id") }
                        }
                        composable("tasks/{id}") { entry ->
                            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                            TasksScreen(vm, id) { nav.popBackStack() }
                        }
                    }
                }
            }
        }
    }
}
