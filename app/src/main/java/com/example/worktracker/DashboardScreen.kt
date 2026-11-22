package com.example.worktracker

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DuplicateHighlightStatus {
    NONE,
    SINGLE_ASSOCIATE_DUPLICATE,    // Light Yellow
    MULTIPLE_ASSOCIATES_DUPLICATE  // Light Pink
}

data class MonthlyFinancialMetrics(
    val monthYear: String,
    val totalAmount: Double,
    val collectedAmount: Double,
    val pendingAmount: Double,
    val pendingPercentage: Double
)

sealed class AssociateDailyRow {
    data class Entered(val record: WorkRecord) : AssociateDailyRow()
    data class NotEntered(val associateName: String) : AssociateDailyRow()
}

@Composable
fun DashboardScreen(viewModel: WorkViewModel) {
    val allRecords by viewModel.records.collectAsState()
    val slicerRecords by viewModel.slicerFilteredRecords.collectAsState(initial = emptyList())
    val visibleRecords by viewModel.visibleRecords.collectAsState(initial = emptyList())
    val selectedCalDate by viewModel.selectedCalendarDate.collectAsState()

    val search by viewModel.searchQuery.collectAsState()
    val role by viewModel.currentUserRole.collectAsState()
    val loggedInEmp by viewModel.loggedInEmployee.collectAsState()

    val selectedMonth by viewModel.selectedMonthFilter.collectAsState()
    val selectedDistrict by viewModel.selectedDistrictFilter.collectAsState()
    val selectedAct by viewModel.selectedActivityFilter.collectAsState()
    val selectedWorkId by viewModel.selectedWorkIdFilter.collectAsState()
    val selectedEmp by viewModel.selectedEmployeeFilter.collectAsState()

    val masterActs by viewModel.masterActivities.collectAsState()
    val masterWorks by viewModel.masterWorkIds.collectAsState()
    val masterEmps by viewModel.masterEmployees.collectAsState()

    var showAiSummaryDialog by remember { mutableStateOf(false) }

    val monthOptions = remember(allRecords) {
        val currentMonthStr = SimpleDateFormat("MMM-yy", Locale.ENGLISH).format(Calendar.getInstance().time)
        val distinctMonths = (listOf(currentMonthStr) + allRecords.map { extractMonthYear(it.date) })
            .filter { it.isNotBlank() }
            .distinct()
        listOf("All") + distinctMonths
    }

    val districtOptions = remember {
        listOf("All") + tamilNaduPincodeList.map { it.district }.distinct().sorted()
    }

    val totalBilled: Double = visibleRecords.sumOf { it.totalAmount }
    val totalPending: Double = visibleRecords.sumOf { it.pendingAmount }
    val pendingPercentage: Double = if (totalBilled > 0.0) (totalPending / totalBilled) * 100.0 else 0.0

    val monthlyMetrics: List<MonthlyFinancialMetrics> = remember(slicerRecords) {
        slicerRecords.groupBy { record: WorkRecord ->
            extractMonthYear(record.date)
        }.map { (month: String, recList: List<WorkRecord>) ->
            val billed = recList.sumOf { it.totalAmount }
            val collected = recList.sumOf { it.advanceAmount }
            val pending = recList.sumOf { it.pendingAmount }
            val pct = if (billed > 0.0) (pending / billed) * 100.0 else 0.0
            MonthlyFinancialMetrics(
                monthYear = month,
                totalAmount = billed,
                collectedAmount = collected,
                pendingAmount = pending,
                pendingPercentage = pct
            )
        }.reversed()
    }

    // Distinct associate counts per date for calendar superscripts
    val associateCountByDate: Map<String, Int> = remember(slicerRecords) {
        slicerRecords.groupBy { it.date }
            .mapValues { (_, list) ->
                list.map { it.employeeName.trim().lowercase() }.distinct().size
            }
    }

    // Duplicate detection per date for calendar cell background highlighting
    val duplicateStatusByDate: Map<String, DuplicateHighlightStatus> = remember(slicerRecords) {
        slicerRecords.groupBy { it.date }
            .mapValues { (_, list) ->
                val countsPerAssociate = list.groupingBy { it.employeeName.trim().lowercase() }.eachCount()
                val associatesWithDuplicates = countsPerAssociate.count { it.value > 1 }
                when {
                    associatesWithDuplicates > 1 -> DuplicateHighlightStatus.MULTIPLE_ASSOCIATES_DUPLICATE
                    associatesWithDuplicates == 1 -> DuplicateHighlightStatus.SINGLE_ASSOCIATE_DUPLICATE
                    else -> DuplicateHighlightStatus.NONE
                }
            }
    }

    // Alphabetical associate breakdown when a calendar date is selected
    val dailyAssociateRows: List<AssociateDailyRow> = remember(selectedCalDate, visibleRecords, masterEmps, role, loggedInEmp, selectedEmp) {
        if (selectedCalDate == null) {
            emptyList()
        } else {
            val targetAssociates: List<String> = when {
                role == UserRole.USER -> listOf(loggedInEmp)
                selectedEmp != "All" -> listOf(selectedEmp)
                else -> {
                    val activeNames = masterEmps.filter { it.isActive }.map { it.name }
                    val enteredNames = visibleRecords.map { it.employeeName }
                    (activeNames + enteredNames).distinct()
                }
            }.sortedBy { it.lowercase() }

            val rows = mutableListOf<AssociateDailyRow>()
            for (assoc in targetAssociates) {
                val matchingRecords = visibleRecords
                    .filter { it.employeeName.equals(assoc, ignoreCase = true) }
                    .sortedBy { it.timestamp }

                if (matchingRecords.isEmpty()) {
                    rows.add(AssociateDailyRow.NotEntered(assoc))
                } else {
                    matchingRecords.forEach { rec ->
                        rows.add(AssociateDailyRow.Entered(rec))
                    }
                }
            }
            rows
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- HEADER WITH "AI SUMMARY" BUTTON AT TOP RIGHT ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = { showAiSummaryDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Summary",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Summary",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search customer, phone, pincode, district...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
        }

        // Slicer Filters
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
                        Text("Reset All")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DropdownFilterChip(
                        label = "Month",
                        currentValue = selectedMonth,
                        options = monthOptions,
                        onSelect = { viewModel.selectedMonthFilter.value = it }
                    )

                    if (role == UserRole.ADMIN) {
                        DropdownFilterChip(
                            label = "Employee",
                            currentValue = selectedEmp,
                            options = listOf("All") + masterEmps.map { it.name },
                            onSelect = { viewModel.selectedEmployeeFilter.value = it }
                        )
                    }

                    DropdownFilterChip(
                        label = "District",
                        currentValue = selectedDistrict,
                        options = districtOptions,
                        onSelect = { viewModel.selectedDistrictFilter.value = it }
                    )

                    DropdownFilterChip(
                        label = "Activity",
                        currentValue = selectedAct,
                        options = listOf("All") + masterActs.map { it.name },
                        onSelect = { viewModel.selectedActivityFilter.value = it }
                    )

                    DropdownFilterChip(
                        label = "Work ID",
                        currentValue = selectedWorkId,
                        options = listOf("All") + masterWorks.map { it.name },
                        onSelect = { viewModel.selectedWorkIdFilter.value = it }
                    )
                }
            }
        }

        // 3-Part Financial Score Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (selectedCalDate != null) "FINANCIAL SUMMARY ($selectedCalDate)" else "FINANCIAL SUMMARY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Amount",
                                style = MaterialTheme.typography.label
