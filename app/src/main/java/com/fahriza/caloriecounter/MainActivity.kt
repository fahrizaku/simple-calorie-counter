package com.fahriza.caloriecounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fahriza.caloriecounter.ui.screens.HistoryScreen
import com.fahriza.caloriecounter.ui.screens.HomeScreen
import com.fahriza.caloriecounter.ui.theme.CalorieCounterTheme
import com.fahriza.caloriecounter.viewmodel.CalorieViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalorieCounterTheme {
                CalorieCounterApp()
            }
        }
    }
}

@Composable
fun CalorieCounterApp() {
    val navController = rememberNavController()
    val vm: CalorieViewModel = viewModel()
    val currentRoute by navController.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute?.destination?.route == "home",
                    onClick = { navController.navigate("home") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Hari Ini") }
                )
                NavigationBarItem(
                    selected = currentRoute?.destination?.route == "history",
                    onClick = { navController.navigate("history") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "History") },
                    label = { Text("Riwayat") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(vm) }
            composable("history") { HistoryScreen(vm) }
        }
    }
}
