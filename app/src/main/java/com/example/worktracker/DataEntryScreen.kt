package com.example.worktracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun DataEntryScreen(
    viewModel: WorkViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val form by viewModel.formState.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()
    val loggedInEmp by viewModel.loggedInEmployee.collectAsState()
    val allRecords by viewModel.records.collectAsState()

    val allEmployees by viewModel.masterEmployees.collectAsState()
    val allWorkIds by viewModel.masterWorkIds.collectAsState()
    val allActivities by viewModel.masterActivities.collectAsState()
    val allPincodes by viewModel.masterPincodes.collectAsState()

    val activeEmployees = remember(allEmployees) { allEmployees.filter { it.isActive }.map { it.name } }
    val activeWorkIds = remember(allWorkIds) { allWorkIds.filter { it.isActive }.map { it.name } }
    val activeActivities = remember(allActivities) { allActivities.filter { it.isActive }.map { it.name } }
    val activePincodeOptions = remember(allPincodes) {
        val activeList = allPincodes.filter { it.isActive }.map { it.name }
        activeList.ifEmpty { tamilNaduPincodeList.map { it.pincode } }
    }

    val isPhoneValid = form.phoneNumber.length == 10 && form.phoneNumber.all { char -> char.isDigit() }
    var submitError by remember { mutableStateOf(false) }
    var showAiSummaryDialog by remember { mutableStateOf(false) }

    val targetAssociate = remember(userRole, loggedInEmp, form.employeeName, activeEmployees) {
        if (userRole == UserRole.USER) {
            loggedInEmp
        } else {
            form.employeeName.ifEmpty { activeEmployees.firstOrNull() ?: "Admin" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with AI Summary in Top Right Corner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Create Work Record",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

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

        if (userRole == UserRole.USER) {
            OutlinedTextField(
                value = loggedInEmp,
                onValueChange = {},
                readOnly = true,
                label = { Text("Employee Name") },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AppDropdown(
                label = "Employee Name (Admin selector)",
                options = activeEmployees,
                selectedOption = form.employeeName.ifEmpty { activeEmployees.firstOrNull() ?: "" },
                onOptionSelected = { selected -> viewModel.updateForm { copy(employeeName = selected) } }
            )
        }

        AppDropdown(
            label = "Work ID",
            options = activeWorkIds,
            selectedOption = form.workId.ifEmpty { activeWorkIds.firstOrNull() ?: "" },
            onOptionSelected = { selected -> viewModel.updateForm { copy(workId = selected) } }
        )

        AppDropdown(
            label = "Date (DD-MMM-YY)",
            options = viewModel.pastDates,
            selectedOption = form.date.ifEmpty { viewModel.pastDates.firstOrNull() ?: "" },
            onOptionSelected = { selected -> viewModel.updateForm { copy(date = selected) } }
        )

        AppDropdown(
            label = "Activity",
            options = activeActivities,
            selectedOption = form.activity.ifEmpty { activeActivities.firstOrNull() ?: "" },
            onOptionSelected = { selected -> viewModel.updateForm { copy(activity = selected) } }
        )

        // Customer Name enforced in Proper Case
        OutlinedTextField(
            value = form.customerName,
            onValueChange = { input -> viewModel.updateForm { copy(customerName = input) } },
            label = { Text("Customer Name (Proper Case) *") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            supportingText = {
                if (form.customerName.isNotBlank()) {
                    Text("Will be saved as: ${viewModel.formatProperCase(form.customerName)}")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Location Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        AppDropdown(
            label = "Pincode (Select to auto-retrieve Place & District) *",
            options = activePincodeOptions,
            selectedOption = form.pincode.ifEmpty { activePincodeOptions.firstOrNull() ?: "" },
            onOptionSelected = { selectedPin -> viewModel.onPincodeSelected(selectedPin) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = form.place,
                onValueChange = {},
                readOnly = true,
                label = { Text("Place (Auto)") },
                modifier = Modifier.weight(1.2f)
            )

            OutlinedTextField(
                value = form.district,
                onValueChange = {},
                readOnly = true,
                label = { Text("District (Auto)") },
                modifier = Modifier.weight(1f)
            )
        }

        AppDropdown(
            label = "No. of Visit",
            options = viewModel.visitCounts,
            selectedOption = form.visitCount,
            onOptionSelected = { selected -> viewModel.updateForm { copy(visitCount = selected) } }
        )

        OutlinedTextField(
            value = form.totalAmount,
            onValueChange = { input -> viewModel.updateForm { copy(totalAmount = input) } },
            label = { Text("Total Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = form.advanceAmount,
            onValueChange = { input -> viewModel.updateForm { copy(advanceAmount = input) } },
            label = { Text("Advance Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = "₹ %.2f".format(form.pendingAmount),
            onValueChange = {},
            readOnly = true,
            label = { Text("Pending Amount (Auto Calculated)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = form.phoneNumber,
            onValueChange = { input ->
                if (input.length <= 10 && input.all { char -> char.isDigit() }) {
                    viewModel.updateForm { copy(phoneNumber = input) }
                }
            },
            label = { Text("Phone Number (10 digits mandatory) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = form.phoneNumber.isNotEmpty() && !isPhoneValid,
            supportingText = {
                if (form.phoneNumber.isNotEmpty() && !isPhoneValid) {
                    Text("Must be exactly 10 digits (${form.phoneNumber.length}/10)")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (submitError) {
            Text("Please complete all mandatory fields correctly", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val success = viewModel.saveRecord()
                if (success) {
                    submitError = false
                    onNavigateToDashboard()
                } else {
                    submitError = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = form.customerName.isNotBlank() && isPhoneValid
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Entry")
        }
    }

    if (showAiSummaryDialog) {
        val personalRecords = remember(allRecords, targetAssociate) {
            allRecords.filter { it.employeeName.equals(targetAssociate, ignoreCase = true) }
        }
        val insights = remember(personalRecords, targetAssociate) {
            generateAiSummaryInsights(
                records = personalRecords,
                scopeDescription = "Associate: $targetAssociate"
            )
        }

        AlertDialog(
            onDismissRequest = { showAiSummaryDialog = false },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("AI Summary ($targetAssociate)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    insights.forEach { bullet ->
                        Text(
                            text = "• $bullet",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAiSummaryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
