package com.example.worktracker

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(viewModel: WorkViewModel) {
    val records by viewModel.visibleRecords.collectAsState(initial = emptyList())
    val search by viewModel.searchQuery.collectAsState()
    val role by viewModel.currentUserRole.collectAsState()
    val loggedInEmp by viewModel.loggedInEmployee.collectAsState()

    // Filters
    val selectedLoc by viewModel.selectedLocationFilter.collectAsState()
    val selectedAct by viewModel.selectedActivityFilter.collectAsState()
    val selectedWorkId by viewModel.selectedWorkIdFilter.collectAsState()
    val selectedEmp by viewModel.selectedEmployeeFilter.collectAsState()

    val masterLocs by viewModel.masterPincodes.collectAsState()
    val masterActs by viewModel.masterActivities.collectAsState()
    val masterWorks by viewModel.masterWorkIds.collectAsState()
    val masterEmps by viewModel.masterEmployees.collectAsState()

    val totalPending = records.sumOf { it.pendingAmount }
    val totalAdvance = records.sumOf { it.advanceAmount }
    val totalBilled = records.sumOf { it.totalAmount }

    // Aggregate monthly advance amounts from filtered records
    val monthlyAdvances = remember(records) {
        records.groupBy { record ->
            // Extracts "Mmm-YY" from "DD-Mmm-YY" (e.g. "Sep-26")
            val parts = record.date.split("-")
            if (parts.size == 3) "${parts[1]}-${parts[2]}" else record.date
        }.map { (month, recList) ->
            MonthlyAdvance(month, recList.sumOf { it.advanceAmount })
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = if (role == UserRole.ADMIN) "Admin Central Dashboard" else "Dashboard ($loggedInEmp)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (role == UserRole.ADMIN) "All company data & advanced cross-filters" else "Reviewing your field operations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Omni-Search
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search customer, phone, location, work ID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
        }

        // Horizontal Filters Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filters", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { viewModel.resetFilters() }) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (role == UserRole.ADMIN) {
                        DropdownFilterChip(
                            label = "Employee",
                            currentValue = selectedEmp,
                            options = listOf("All") + masterEmps,
                            onSelect = { viewModel.selectedEmployeeFilter.value = it }
                        )
                    }

                    DropdownFilterChip(
                        label = "Location",
                        currentValue = selectedLoc,
                        options = listOf("All") + masterLocs,
                        onSelect = { viewModel.selectedLocationFilter.value = it }
                    )

                    DropdownFilterChip(
                        label = "Activity",
                        currentValue = selectedAct,
                        options = listOf("All") + masterActs,
                        onSelect = { viewModel.selectedActivityFilter.value = it }
                    )

                    DropdownFilterChip(
                        label = "Work ID",
                        currentValue = selectedWorkId,
                        options = listOf("All") + masterWorks,
                        onSelect = { viewModel.selectedWorkIdFilter.value = it }
                    )
                }
            }
        }

        // Dedicated Score Card: Total Pending Amount
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TOTAL PENDING RECEIVABLE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "₹ %.2f".format(totalPending),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Billed: ₹%.0f".format(totalBilled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Collected: ₹%.0f".format(totalAdvance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Monthly Advance Amount Bar Chart
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Advance Collection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aggregated advance payments grouped by month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (monthlyAdvances.isEmpty()) {
                        Text(
                            text = "No collection records available to plot",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        MonthlyAdvanceBarChart(data = monthlyAdvances)
                    }
                }
            }
        }

        // Records Heading
        item {
            Text(
                text = "Records (${records.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Filtered Records List
        items(records, key = { it.id }) { record ->
            RecordItemCard(record = record, showEmployee = role == UserRole.ADMIN)
        }
    }
}

@Composable
fun DropdownFilterChip(
    label: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = currentValue != "All",
            onClick = { expanded = true },
            label = {
                Text(if (currentValue == "All") "$label: All" else "$label: $currentValue")
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MonthlyAdvanceBarChart(data: List<MonthlyAdvance>) {
    val maxVal = (data.maxOfOrNull { it.totalAdvance } ?: 1.0).coerceAtLeast(1.0)
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barCount = data.size
            val slotWidth = canvasWidth / barCount
            val barWidth = slotWidth * 0.5f

            data.forEachIndexed { index, item ->
                val barHeight = ((item.totalAdvance / maxVal) * (canvasHeight * 0.85f)).toFloat()
                val left = (index * slotWidth) + ((slotWidth - barWidth) / 2f)
                val top = canvasHeight - barHeight

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Month labels and values under bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { item ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "₹%.0f".format(item.totalAdvance),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.monthYear,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
