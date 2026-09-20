package com.example.worktracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: WorkViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val employees by viewModel.masterEmployees.collectAsState()
    var selectedEmployee by remember { mutableStateOf(employees.firstOrNull() ?: "") }
    
    var adminPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Work Tracker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Select your portal to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("User Login") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Admin Portal") })
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedTab == 0) {
            // User login: Pick registered name, no password needed
            Text(text = "Select Employee Name", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (employees.isEmpty()) {
                Text("No employees added by Admin yet.", color = MaterialTheme.colorScheme.error)
            } else {
                AppDropdown(
                    label = "Your Name",
                    options = employees,
                    selectedOption = selectedEmployee.ifEmpty { employees.first() },
                    onOptionSelected = { selectedEmployee = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val emp = selectedEmployee.ifEmpty { employees.first() }
                        viewModel.loginAsUser(emp)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Enter as User")
                }
            }
        } else {
            // Admin login: Password protected
            Text(text = "Admin Access", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = adminPassword,
                onValueChange = {
                    adminPassword = it
                    passwordError = false
                },
                label = { Text("Enter Admin Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordError,
                supportingText = { if (passwordError) Text("Incorrect password! (Default: admin123)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val success = viewModel.loginAsAdmin(adminPassword)
                    if (!success) passwordError = true
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Unlock Admin Panel")
            }
        }
    }
}
