package com.example.worktracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Create Work Entry",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        AppDropdown(
            label = "Work ID",
            options = DropdownOptions.workIds,
            selectedOption = form.workId,
            onOptionSelected = { viewModel.updateForm { copy(workId = it) } }
        )

        AppDropdown(
            label = "Date",
            options = DropdownOptions.dates,
            selectedOption = form.date,
            onOptionSelected = { viewModel.updateForm { copy(date = it) } }
        )

        AppDropdown(
            label = "Activity",
            options = DropdownOptions.activities,
            selectedOption = form.activity,
            onOptionSelected = { viewModel.updateForm { copy(activity = it) } }
        )

        OutlinedTextField(
            value = form.customerName,
            onValueChange = { viewModel.updateForm { copy(customerName = it) } },
            label = { Text("Customer Name *") },
            modifier = Modifier.fillMaxWidth()
        )

        AppDropdown(
            label = "Location",
            options = DropdownOptions.locations,
            selectedOption = form.location,
            onOptionSelected = { viewModel.updateForm { copy(location = it) } }
        )

        AppDropdown(
            label = "No. of Visit",
            options = DropdownOptions.visitCounts,
            selectedOption = form.visitCount,
            onOptionSelected = { viewModel.updateForm { copy(visitCount = it) } }
        )

        OutlinedTextField(
            value = form.totalAmount,
            onValueChange = { viewModel.updateForm { copy(totalAmount = it) } },
            label = { Text("Total Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = form.advanceAmount,
            onValueChange = { viewModel.updateForm { copy(advanceAmount = it) } },
            label = { Text("Advance Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = "%.2f".format(form.pendingAmount),
            onValueChange = {},
            readOnly = true,
            label = { Text("Pending Amount (Auto Calculated)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        OutlinedTextField(
            value = form.phoneNumber,
            onValueChange = { viewModel.updateForm { copy(phoneNumber = it) } },
            label = { Text("Phone Number *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.saveRecord()
                onNavigateToDashboard()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = form.customerName.isNotBlank() && form.phoneNumber.isNotBlank()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save & Open Dashboard")
        }
    }
}
