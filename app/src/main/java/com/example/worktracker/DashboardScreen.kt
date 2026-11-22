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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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

    // Month Slicer Options dynamically built from records + current month
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

    // Score Card Totals (Total Amount, Pending Amount, % of Pending Amount)
    val totalBilled: Double = visibleRecords.sumOf { it.totalAmount }
    val totalPending: Double = visibleRecords.sumOf { it.pendingAmount }
    val pendingPercentage: Double = if (totalBilled > 0.0) (totalPending / totalBilled) * 100.0 else 0.0

    // Graph Data: Collected (Light Green), Pending (Light Red), % Pending (Black Line + Dots)
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
        }.reversed() // Chronological left-to-right order
    }

    // Distinct associate counts per date for calendar superscripts
    val associateCountByDate: Map<String, Int> = remember(slicerRecords) {
        slicerRecords.groupBy { it.date }
            .mapValues { (_, list) ->
                list.map { it.employeeName.trim().lowercase() }.distinct().size
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

        // Slicer Filters (including Month Slicer)
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

        // --- 3-PART FINANCIAL SCORE CARD (Total Amount, Pending Amount, % of Pending Amount) ---
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
                        // 1. Total Amount
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Amount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "₹%.0f".format(totalBilled),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // 2. Pending Amount
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Pending Amount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "₹%.0f".format(totalPending),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // 3. % of Pending Amount
                        Column(
                            modifier = Modifier.weight(0.85f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "% Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "%.1f%%".format(pendingPercentage),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (pendingPercentage > 50.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // --- CLUSTERED COLUMN + LINE COMBO CHART ---
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Collection vs Pending (%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Chart Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendIndicator(color = Color(0xFF81C784), label = "Collected", isLine = false)
                        LegendIndicator(color = Color(0xFFE57373), label = "Pending", isLine = false)
                        LegendIndicator(color = Color.Black, label = "% Pending", isLine = true)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (monthlyMetrics.isEmpty()) {
                        Text(
                            text = "No collection records available to plot",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ClusteredCollectionComboChart(data = monthlyMetrics)
                    }
                }
            }
        }

        // --- DYNAMIC CALENDAR WITH FORWARD & BACKWARD BUTTONS ---
        item {
            DynamicAttendanceCalendar(
                associateCountByDate = associateCountByDate,
                selectedDate = selectedCalDate,
                onDateClick = { dateStr -> viewModel.toggleCalendarDate(dateStr) },
                onClearDate = { viewModel.selectedCalendarDate.value = null }
            )
        }

        // Records Section Header
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

        // Records List
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
fun LegendIndicator(color: Color, label: String, isLine: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isLine) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color, CircleShape)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ClusteredCollectionComboChart(data: List<MonthlyFinancialMetrics>) {
    val maxAmount = (data.maxOfOrNull { maxOf(it.collectedAmount, it.pendingAmount) } ?: 1.0).coerceAtLeast(1.0)
    val lightGreen = Color(0xFF81C784)
    val lightRed = Color(0xFFE57373)
    val lineColor = Color.Black

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val slotCount = data.size
            val slotWidth = canvasWidth / slotCount
            val barWidth = (slotWidth * 0.30f).coerceAtMost(28.dp.toPx())
            val gapBetweenBars = 3.dp.toPx()
            val topPadding = 16.dp.toPx()
            val drawableHeight = canvasHeight - topPadding

            val linePoints = mutableListOf<Offset>()

            data.forEachIndexed { index, item ->
                val slotCenter = (index * slotWidth) + (slotWidth / 2f)

                // 1. Collected Column (Light Green - Left of Center)
                val collectedHeight = ((item.collectedAmount / maxAmount) * drawableHeight).toFloat()
                val collectedLeft = slotCenter - barWidth - (gapBetweenBars / 2f)
                drawRoundRect(
                    color = lightGreen,
                    topLeft = Offset(collectedLeft, canvasHeight - collectedHeight),
                    size = Size(barWidth, collectedHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // 2. Pending Column (Light Red - Right of Center)
                val pendingHeight = ((item.pendingAmount / maxAmount) * drawableHeight).toFloat()
                val pendingLeft = slotCenter + (gapBetweenBars / 2f)
                drawRoundRect(
                    color = lightRed,
                    topLeft = Offset(pendingLeft, canvasHeight - pendingHeight),
                    size = Size(barWidth, pendingHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // 3. % Pending Coordinate (0% to 100% scale mapped to drawableHeight)
                val clampedPct = item.pendingPercentage.coerceIn(0.0, 100.0)
                val pointY = canvasHeight - ((clampedPct / 100.0) * drawableHeight).toFloat()
                linePoints.add(Offset(slotCenter, pointY))
            }

            // Draw Black Connecting Line for % Pending
            if (linePoints.size > 1) {
                val path = Path().apply {
                    moveTo(linePoints.first().x, linePoints.first().y)
                    for (i in 1 until linePoints.size) {
                        lineTo(linePoints[i].x, linePoints[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Draw Filled Dot Markers on every data point
            linePoints.forEach { pt ->
                drawCircle(
                    color = lineColor,
                    radius = 4.5.dp.toPx(),
                    center = pt
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-Axis Labels & Values per Month
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
                        text = item.monthYear,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%.0f%% pend".format(item.pendingPercentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "₹%.0f / ₹%.0f".format(item.collectedAmount, item.pendingAmount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicAttendanceCalendar(
    associateCountByDate: Map<String, Int>,
    selectedDate: String?,
    onDateClick: (String) -> Unit,
    onClearDate: () -> Unit
) {
    // Track month offset from current month (0 = current month, -1 = previous month, +1 = next month)
    var monthOffset by remember { mutableIntStateOf(0) }

    val calendarData = remember(monthOffset) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, monthOffset)

        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time)
        val dateFormatter = SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)

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
            // Month Header with Backward (<) and Forward (>) Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { monthOffset -= 1 }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month"
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Superscript = active associates entered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthOffset += 1 }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month"
                        )
                    }
                    if (selectedDate != null) {
                        IconButton(onClick = onClearDate) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Date")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
