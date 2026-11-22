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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class AssociateDailyRow {
    data class Entered(val record: WorkRecord) : AssociateDailyRow()
    data class NotEntered(val associateName: String) : AssociateDailyRow()
}

@Composable
fun DashboardScreen(viewModel: WorkViewModel) {
    val slicerRecords by viewModel.slicerFilteredRecords.collectAsState(initial = emptyList())
    val visibleRecords by viewModel.visibleRecords.collectAsState(initial = emptyList())
    val selectedCalDate by viewModel.selectedCalendarDate.collectAsState()

    val search by viewModel.searchQuery.collectAsState()
    val role by viewModel.currentUserRole.collectAsState()
    val loggedInEmp by viewModel.loggedInEmployee.collectAsState()

    val selectedDistrict by viewModel.selectedDistrictFilter.collectAsState()
    val selectedAct by viewModel.selectedActivityFilter.collectAsState()
    val selectedWorkId by viewModel.selectedWorkIdFilter.collectAsState()
    val selectedEmp by viewModel.selectedEmployeeFilter.collectAsState()

    val masterActs by viewModel.masterActivities.collectAsState()
    val masterWorks by viewModel.masterWorkIds.collectAsState()
    val masterEmps by viewModel.masterEmployees.collectAsState()

    val districtOptions = remember {
        listOf("All") + tamilNaduPincodeList.map { it.district }.distinct().sorted()
    }

    val totalPending: Double = visibleRecords.sumOf { it.pendingAmount }
    val totalAdvance: Double = visibleRecords.sumOf { it.advanceAmount }
    val totalBilled: Double = visibleRecords.sumOf { it.totalAmount }

    val monthlyAdvances: List<MonthlyAdvance> = remember(slicerRecords) {
        slicerRecords.groupBy { record: WorkRecord ->
            val parts = record.date.split("-")
            if (parts.size == 3) "${parts[1]}-${parts[2]}" else record.date
        }.map { (month: String, recList: List<WorkRecord>) ->
            MonthlyAdvance(month, recList.sumOf { it.advanceAmount })
        }
    }

    // Count distinct associates per date ("dd-MMM-yy") for calendar superscripts
    val associateCountByDate: Map<String, Int> = remember(slicerRecords) {
        slicerRecords.groupBy { it.date }
            .mapValues { (_, list) ->
                list.map { it.employeeName.trim().lowercase() }.distinct().size
            }
    }

    // Build alphabetical associate rows when a specific calendar date is selected
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

        // Receivable Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (selectedCalDate != null) "PENDING RECEIVABLE ($selectedCalDate)" else "TOTAL PENDING RECEIVABLE",
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

        // Monthly Advance Graph
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

        // --- CURRENT MONTH ATTENDANCE CALENDAR ---
        item {
            CurrentMonthAttendanceCalendar(
                associateCountByDate = associateCountByDate,
                selectedDate = selectedCalDate,
                onDateClick = { dateStr -> viewModel.toggleCalendarDate(dateStr) },
                onClearDate = { viewModel.selectedCalendarDate.value = null }
            )
        }

        // Records Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCalDate != null) {
                        "Associates Status • $selectedCalDate (A–Z)"
                    } else {
                        "Records (${visibleRecords.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (selectedCalDate != null) {
                    TextButton(onClick = { viewModel.selectedCalendarDate.value = null }) {
                        Text("Show All Dates")
                    }
                }
            }
        }

        // Records List (Alphabetical Associate Breakdown if a day is clicked, otherwise standard list)
        if (selectedCalDate != null) {
            items(
                items = dailyAssociateRows,
                key = { row ->
                    when (row) {
                        is AssociateDailyRow.Entered -> "entered_${row.record.id}_${row.record.timestamp}"
                        is AssociateDailyRow.NotEntered -> "missing_${row.associateName}"
                    }
                }
            ) { row ->
                when (row) {
                    is AssociateDailyRow.Entered -> {
                        RecordItemCard(record = row.record, showEmployee = true)
                    }
                    is AssociateDailyRow.NotEntered -> {
                        NotEnteredAssociateCard(associateName = row.associateName, date = selectedCalDate!!)
                    }
                }
            }
        } else {
            items(
                items = visibleRecords,
                key = { record: WorkRecord -> record.id.ifEmpty { "${record.timestamp}_${record.phoneNumber}" } }
            ) { record: WorkRecord ->
                RecordItemCard(record = record, showEmployee = role == UserRole.ADMIN)
            }
        }
    }
}

@Composable
fun CurrentMonthAttendanceCalendar(
    associateCountByDate: Map<String, Int>,
    selectedDate: String?,
    onDateClick: (String) -> Unit,
    onClearDate: () -> Unit
) {
    val calendarData = remember {
        val cal = Calendar.getInstance()
        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time)
        val dateFormatter = SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeekOffset = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sun, 6 = Sat
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<Pair<Int, String>?>()
        repeat(firstDayOfWeekOffset) { cells.add(null) }
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            cells.add(day to dateFormatter.format(cal.time))
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        Triple(monthTitle, cells.chunked(7), dateFormatter.format(Calendar.getInstance().time))
    }

    val (monthTitle, weeks, todayFormatted) = calendarData
    val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Superscript = active associates entered • Tap a day to inspect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedDate != null) {
                    IconButton(onClick = onClearDate) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Date")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday Header Row
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar Grid Rows
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { cell ->
                            if (cell == null) {
                                Box(modifier = Modifier.weight(1f).height(40.dp))
                            } else {
                                val (dayNumber, dateStr) = cell
                                val count = associateCountByDate[dateStr] ?: 0
                                val isSelected = selectedDate == dateStr
                                val isToday = todayFormatted == dateStr

                                val superscript = if (count > 0) toSuperscript(count) else ""
                                val displayLabel = "$dayNumber$superscript"

                                val containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    count > 0 -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }

                                val textColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    count > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onDateClick(dateStr) },
                                    color = containerColor,
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (isToday && !isSelected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    } else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = displayLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (count > 0 || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = textColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toSuperscript(number: Int): String {
    val superscriptDigits = mapOf(
        '0' to '⁰',
        '1' to '¹',
        '2' to '²',
        '3' to '³',
        '4' to '⁴',
        '5' to '⁵',
        '6' to '⁶',
        '7' to '⁷',
        '8' to '⁸',
        '9' to '⁹'
    )
    return number.toString().map { superscriptDigits[it] ?: it }.joinToString("")
}

@Composable
fun NotEnteredAssociateCard(associateName: String, date: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = associateName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Date: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Not Entered",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun RecordItemCard(record: WorkRecord, showEmployee: Boolean) {
    val context = LocalContext.current
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(record.timestamp) { timeFormatter.format(Date(record.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.workId} • ${record.date}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (showEmployee) {
                Text(
                    text = "Associate: ${record.employeeName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Visits: ${record.visitCount}", style = MaterialTheme.typography.labelSmall) }
                )
            }

            Text(
                text = "${record.activity} | ${record.place}, ${record.district} (${record.pincode})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total: ₹${record.totalAmount}  |  Adv: ₹${record.advanceAmount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Pending: ₹${record.pendingAmount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (record.pendingAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${record.phoneNumber}"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call Customer", tint = MaterialTheme.colorScheme.primary)
                }
            }
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

            data.forEachIndexed { index: Int, item: MonthlyAdvance ->
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { item: MonthlyAdvance ->
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
