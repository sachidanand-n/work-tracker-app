package com.example.worktracker

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun AdminPanelScreen(viewModel: WorkViewModel) {
    val context = LocalContext.current

    var newEmployee by remember { mutableStateOf("") }
    var newWorkId by remember { mutableStateOf("") }
    var newActivity by remember { mutableStateOf("") }

    val employees by viewModel.masterEmployees.collectAsState()
    val workIds by viewModel.masterWorkIds.collectAsState()
    val activities by viewModel.masterActivities.collectAsState()
    val allRecords by viewModel.records.collectAsState()

    val isSyncing by viewModel.isSyncing.collectAsState()
    val progress by viewModel.syncProgress.collectAsState()
    val percentage by viewModel.syncPercentage.collectAsState()
    val syncStatus by viewModel.syncStatusMessage.collectAsState()
    val hasError by viewModel.syncHasError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Admin Database Management",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // --- 1. CLOUD SYNCHRONIZATION CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cloud Synchronization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Publish newly added Work IDs, Activities, and Employees to all user devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.syncMasterDataToCloud() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isSyncing,
                    colors = if (hasError) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSyncing) "Synchronizing ($percentage)..." else "Sync to All Devices")
                }

                if (isSyncing || progress > 0f || hasError) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = syncStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = if (hasError) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = percentage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!hasError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // --- 2. CSV EXPORT CARD ---
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Spreadsheet Report Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate and share an Excel-compatible CSV report containing all ${allRecords.size} records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        if (allRecords.isEmpty()) {
                            Toast.makeText(context, "No records to export yet.", Toast.LENGTH_SHORT).show()
                        } else {
                            shareRecordsAsCsv(context, allRecords)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export All Records to CSV")
                }
            }
        }

        // --- 3. ADD EMPLOYEE ---
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

        // --- 4. ADD WORK ID ---
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

        // --- 5. ADD ACTIVITY ---
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

private fun shareRecordsAsCsv(context: Context, records: List<WorkRecord>) {
    val headers = "Work ID,Date,Employee Name,Customer Name,Phone Number,Location,Activity,Visit Count,Total Amount,Advance Amount,Pending Amount\n"

    val rows = records.joinToString("\n") { record ->
        val safeCustomer = record.customerName.replace("\"", "\"\"")
        val safeLocation = record.location.replace("\"", "\"\"")
        val safeActivity = record.activity.replace("\"", "\"\"")
        val safeEmployee = record.employeeName.replace("\"", "\"\"")

        "\"${record.workId}\",\"${record.date}\",\"$safeEmployee\",\"$safeCustomer\",\"${record.phoneNumber}\",\"$safeLocation\",\"$safeActivity\",\"${record.visitCount}\",${record.totalAmount},${record.advanceAmount},${record.pendingAmount}"
    }

    val csvContent = headers + rows
    val file = File(context.cacheDir, "WorkTracker_Report_${System.currentTimeMillis()}.csv")
    file.writeText(csvContent)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Work Tracker Records Export")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(sendIntent, "Share CSV Export"))
}
