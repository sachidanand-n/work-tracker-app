package com.example.worktracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AdminPanelScreen(viewModel: WorkViewModel) {
    var newEmployee by remember { mutableStateOf("") }
    var newWorkId by remember { mutableStateOf("") }
    var newActivity by remember { mutableStateOf("") }

    val employees by viewModel.masterEmployees.collectAsState()
    val workIds by viewModel.masterWorkIds.collectAsState()
    val activities by viewModel.masterActivities.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Admin Database Management", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // 1. Add Employee
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Register Employee Name", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newEmployee,
                    onValueChange = { newEmployee = it },
                    label = { Text("Employee Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.addMasterEmployee(newEmployee)
                    newEmployee = ""
                }) {
                    Text("Add Employee")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Current: ${employees.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // 2. Add Work ID
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add Work ID", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newWorkId,
                    onValueChange = { newWorkId = it },
                    label = { Text("e.g. WRK-2001") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.addMasterWorkId(newWorkId)
                    newWorkId = ""
                }) {
                    Text("Add Work ID")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Current: ${workIds.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // 3. Add Activity
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add Activity", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newActivity,
                    onValueChange = { newActivity = it },
                    label = { Text("e.g. Quality Inspection") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.addMasterActivity(newActivity)
                    newActivity = ""
                }) {
                    Text("Add Activity")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Current: ${activities.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
