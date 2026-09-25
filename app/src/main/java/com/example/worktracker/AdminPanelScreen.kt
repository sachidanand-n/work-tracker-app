package com.example.worktracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatusMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Admin Database Management", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // SYNC CARD: Pushes all local master changes to Firestore for all users
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cloud Synchronization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Press 'Sync' to publish all new Work IDs, Activities, and Employees to all field user devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.syncMasterDataToCloud() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync to All Devices")
                    }
                }

                if (syncStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

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
                    if (newEmployee.isNotBlank()) {
                        viewModel.addMasterEmployee(newEmployee)
                        newEmployee = ""
                    }
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
                    if (newWorkId.isNotBlank()) {
                        viewModel.addMasterWorkId(newWorkId)
                        newWorkId = ""
                    }
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
                    if (newActivity.isNotBlank()) {
                        viewModel.addMasterActivity(newActivity)
                        newActivity = ""
                    }
                }) {
                    Text("Add Activity")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Current: ${activities.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
