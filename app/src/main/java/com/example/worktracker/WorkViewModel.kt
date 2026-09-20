package com.example.worktracker

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

data class EntryFormState(
    val workId: String = DropdownOptions.workIds.first(),
    val date: String = DropdownOptions.dates.first(),
    val activity: String = DropdownOptions.activities.first(),
    val customerName: String = "",
    val location: String = DropdownOptions.locations.first(),
    val visitCount: String = DropdownOptions.visitCounts.first(),
    val totalAmount: String = "",
    val advanceAmount: String = "",
    val pendingAmount: Double = 0.0,
    val phoneNumber: String = ""
)

class WorkViewModel : ViewModel() {

    private val _records = MutableStateFlow<List<WorkRecord>>(sampleRecords)
    val records: StateFlow<List<WorkRecord>> = _records.asStateFlow()

    private val _formState = MutableStateFlow(EntryFormState())
    val formState: StateFlow<EntryFormState> = _formState.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val filterLocation = MutableStateFlow("All")
    val filterActivity = MutableStateFlow("All")

    val filteredRecords = combine(
        _records,
        searchQuery,
        filterLocation,
        filterActivity
    ) { list, query, loc, act ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.customerName.contains(query, ignoreCase = true) ||
                item.phoneNumber.contains(query) ||
                item.workId.contains(query, ignoreCase = true)
            val matchesLoc = loc == "All" || item.location == loc
            val matchesAct = act == "All" || item.activity == act
            matchesQuery && matchesLoc && matchesAct
        }
    }

    fun updateForm(transform: EntryFormState.() -> EntryFormState) {
        val updated = _formState.value.transform()
        val total = updated.totalAmount.toDoubleOrNull() ?: 0.0
        val advance = updated.advanceAmount.toDoubleOrNull() ?: 0.0
        val pending = (total - advance).coerceAtLeast(0.0)
        _formState.value = updated.copy(pendingAmount = pending)
    }

    fun saveRecord() {
        val state = _formState.value
        if (state.customerName.isBlank() || state.phoneNumber.isBlank()) return

        val newRecord = WorkRecord(
            workId = state.workId,
            date = state.date,
            activity = state.activity,
            customerName = state.customerName.trim(),
            location = state.location,
            visitCount = state.visitCount,
            totalAmount = state.totalAmount.toDoubleOrNull() ?: 0.0,
            advanceAmount = state.advanceAmount.toDoubleOrNull() ?: 0.0,
            pendingAmount = state.pendingAmount,
            phoneNumber = state.phoneNumber.trim()
        )
        _records.value = listOf(newRecord) + _records.value
        _formState.value = EntryFormState()
    }
}

private val sampleRecords = listOf(
    WorkRecord(1, "WRK-1001", "2026-09-20", "New Installation", "Metro Retailers", "Downtown Hub", "2", 15000.0, 5000.0, 10000.0, "9876543210"),
    WorkRecord(2, "WRK-1002", "2026-09-21", "Maintenance", "Apex Logistics", "North Industrial Zone", "1", 4500.0, 4500.0, 0.0, "9123456780"),
    WorkRecord(3, "WRK-1003", "2026-09-21", "Audit", "Starlight Cafe", "East Suburb", "3", 8000.0, 2000.0, 6000.0, "9988776655")
)
