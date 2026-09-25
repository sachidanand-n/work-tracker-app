package com.example.worktracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    val employees by viewModel.masterEmployees.collectAsState()
    val workIds by viewModel.masterWorkIds.collectAsState()
    val activities by viewModel.masterActivities.collectAsState()
    val pincodes by viewModel.masterPincodes.collectAsState()

    val isPhoneValid = form.phoneNumber.length == 10 && form.phoneNumber.all { char -> char.isDigit() }
    var submitError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create Work Record", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

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
                options = employees,
                selectedOption = form.employeeName.ifEmpty { employees.firstOrNull() ?: "" },
                onOptionSelected = { selected -> viewModel.updateForm { copy(employeeName = selected) } }
            )
        }

        AppDropdown(
            label = "Work ID",
            options = workIds,
            selectedOption = form.workId.ifEmpty { workIds.firstOrNull() ?: "" },
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
            options = activities,
            selectedOption = form.activity.ifEmpty { activities.firstOrNull() ?: "" },
            onOptionSelected = { selected -> viewModel.updateForm { copy(activity = selected) } }
        )

        OutlinedTextField(
            value = form.customerName,
            onValueChange = { input -> viewModel.updateForm { copy(customerName = input) } },
            label = { Text("Customer Name *") },
            modifier = Modifier.fillMaxWidth()
        )

        AppDropdown(
            label = "Location (Pincode - Place)",
            options = pincodes,
            selectedOption = form.location.ifEmpty { pincodes.firstOrNull() ?: "" },
            onOptionSelected = { selected -> viewModel.updateForm { copy(location = selected) } }
        )

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
}
