package com.example.worktracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: WorkViewModel) {
    var selectedRole by remember { mutableStateOf(UserRole.USER) }
    var adminPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    val masterEmployees by viewModel.masterEmployees.collectAsState()
    
    // Filter to ONLY ACTIVE employee names for user login
    val activeEmployeeNames = remember(masterEmployees) {
        masterEmployees.filter { it.isActive }.map { it.name }
    }
    
    var selectedEmployee by remember { mutableStateOf("") }

    LaunchedEffect(activeEmployeeNames) {
        if (selectedEmployee.isEmpty() || !activeEmployeeNames.contains(selectedEmployee)) {
            selectedEmployee = activeEmployeeNames.firstOrNull() ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
            text = "Field Service Management Portal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Role Tab Switcher (Field User vs Admin)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedRole == UserRole.USER,
                onClick = {
                    selectedRole = UserRole.USER
                    passwordError = false
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Default.Person, contentDescription = null) }
            ) {
                Text("Technician")
            }

            SegmentedButton(
                selected = selectedRole == UserRole.ADMIN,
                onClick = {
                    selectedRole = UserRole.ADMIN
                    passwordError = false
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Default.Lock, contentDescription = null) }
            ) {
                Text("Admin")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedRole == UserRole.USER) {
                    Text("Select Your Name", fontWeight = FontWeight.SemiBold)

                    if (activeEmployeeNames.isEmpty()) {
                        Text(
                            text = "No active technicians registered. Contact your Admin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        AppDropdown(
                            label = "Technician Name",
                            options = activeEmployeeNames,
                            selectedOption = selectedEmployee.ifEmpty { activeEmployeeNames.first() },
                            onOptionSelected = { selectedEmployee = it }
                        )

                        Button(
                            onClick = {
                                val emp = selectedEmployee.ifEmpty { activeEmployeeNames.first() }
                                viewModel.loginAsUser(emp)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Continue to Field App")
                        }
                    }
                } else {
                    Text("Admin Authentication", fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = {
                            adminPassword = it
                            passwordError = false
                        },
                        label = { Text("Password (Default: admin123)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = passwordError,
                        supportingText = {
                            if (passwordError) {
                                Text("Incorrect password", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val success = viewModel.loginAsAdmin(adminPassword)
                            if (!success) passwordError = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Log In as Admin")
                    }
                }
            }
        }
    }
}
