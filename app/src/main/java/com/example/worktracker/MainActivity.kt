package com.example.worktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private val viewModel: WorkViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val currentRole by viewModel.currentUserRole.collectAsState()
                var currentTab by remember { mutableIntStateOf(0) }

                if (currentRole == UserRole.NONE) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(if (currentRole == UserRole.ADMIN) "Admin Portal" else "Field App")
                                },
                                actions = {
                                    IconButton(onClick = { viewModel.logout() }) {
                                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = { Icon(Icons.Default.Add, contentDescription = "New Entry") },
                                    label = { Text("New Entry") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = { Icon(Icons.Default.List, contentDescription = "Dashboard") },
                                    label = { Text("Dashboard") }
                                )
                                if (currentRole == UserRole.ADMIN) {
                                    NavigationBarItem(
                                        selected = currentTab == 2,
                                        onClick = { currentTab = 2 },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Master Data") },
                                        label = { Text("Master Data") }
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Surface(modifier = Modifier.padding(padding)) {
                            when (currentTab) {
                                0 -> DataEntryScreen(
                                    viewModel = viewModel,
                                    onNavigateToDashboard = { currentTab = 1 }
                                )
                                1 -> DashboardScreen(viewModel = viewModel)
                                2 -> if (currentRole == UserRole.ADMIN) AdminPanelScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
