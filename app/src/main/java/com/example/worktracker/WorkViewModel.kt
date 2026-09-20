package com.example.worktracker

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class UserRole { NONE, USER, ADMIN }

data class EntryFormState(
    val employeeName: String = "",
    val workId: String = "",
    val date: String = "",
    val activity: String = "",
    val customerName: String = "",
    val location: String = "",
    val visitCount: String = "1",
    val totalAmount: String = "",
    val advanceAmount: String = "",
    val pendingAmount: Double = 0.0,
    val phoneNumber: String = ""
)

data class MonthlyAdvance(
    val monthYear: String, // e.g., "Sep-26"
    val totalAdvance: Double
)

class WorkViewModel : ViewModel() {

    private val _currentUserRole = MutableStateFlow(UserRole.NONE)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _loggedInEmployee = MutableStateFlow("")
    val loggedInEmployee: StateFlow<String> = _loggedInEmployee.asStateFlow()

    val masterEmployees = MutableStateFlow(listOf("Ramesh Kumar", "Priya Sharma", "Murugan S", "Anand Raj"))
    val masterWorkIds = MutableStateFlow(listOf("WRK-1001", "WRK-1002", "WRK-1003", "WRK-1004"))
    val masterActivities = MutableStateFlow(listOf("Site Survey", "New Installation", "Maintenance", "Emergency Repair", "Audit"))
    val masterPincodes = MutableStateFlow(indiaPincodeMap.map { "${it.key} - ${it.value}" })
    val visitCounts = listOf("1", "2", "3", "4", "5+")

    val pastDates: List<String> = generateDatesList()

    private val _records = MutableStateFlow<List<WorkRecord>>(sampleInitialData())
    val records: StateFlow<List<WorkRecord>> = _records.asStateFlow()

    private val _formState = MutableStateFlow(EntryFormState())
    val formState: StateFlow<EntryFormState> = _formState.asStateFlow()

    // Dashboard Search & Filters
    val searchQuery = MutableStateFlow("")
    val selectedLocationFilter = MutableStateFlow("All")
    val selectedActivityFilter = MutableStateFlow("All")
    val selectedWorkIdFilter = MutableStateFlow("All")
    val selectedEmployeeFilter = MutableStateFlow("All") // Admin-only filter

    init {
        resetFormToDefaults()
    }

    // Role and Multi-Filter Reactive Flow
    val visibleRecords = combine(
        _records,
        _currentUserRole,
        _loggedInEmployee,
        searchQuery,
        selectedLocationFilter
    ) { recs, role, emp, query, loc ->
        FilterStateIntermediate(recs, role, emp, query, loc)
    }.combine(
        combine(selectedActivityFilter, selectedWorkIdFilter, selectedEmployeeFilter) { act, work, targetEmp ->
            Triple(act, work, targetEmp)
        }
    ) { inter, (act, work, targetEmp) ->
        val roleFiltered = if (inter.role == UserRole.USER) {
            inter.recs.filter { it.employeeName.equals(inter.emp, ignoreCase = true) }
        } else {
            if (targetEmp == "All") inter.recs else inter.recs.filter { it.employeeName == targetEmp }
        }

        roleFiltered.filter { record ->
            val matchLoc = inter.loc == "All" || record.location == inter.loc
            val matchAct = act == "All" || record.activity == act
            val matchWork = work == "All" || record.workId == work
            val matchQuery = inter.query.isBlank() ||
                record.customerName.contains(inter.query, ignoreCase = true) ||
                record.phoneNumber.contains(inter.query) ||
                record.workId.contains(inter.query, ignoreCase = true) ||
                record.location.contains(inter.query, ignoreCase = true) ||
                record.employeeName.contains(inter.query, ignoreCase = true)

            matchLoc && matchAct && matchWork && matchQuery
        }
    }

    // Auth actions
    fun loginAsUser(employeeName: String) {
        _loggedInEmployee.value = employeeName
        _currentUserRole.value = UserRole.USER
        updateForm { copy(employeeName = employeeName) }
    }

    fun loginAsAdmin(password: String): Boolean {
        return if (password == "admin123") {
            _currentUserRole.value = UserRole.ADMIN
            _loggedInEmployee.value = "Admin"
            true
        } else false
    }

    fun logout() {
        _currentUserRole.value = UserRole.NONE
        _loggedInEmployee.value = ""
        resetFormToDefaults()
    }

    fun resetFilters() {
        searchQuery.value = ""
        selectedLocationFilter.value = "All"
        selectedActivityFilter.value = "All"
        selectedWorkIdFilter.value = "All"
        selectedEmployeeFilter.value = "All"
    }

    fun addMasterEmployee(name: String) {
        if (name.isNotBlank() && !masterEmployees.value.contains(name.trim())) {
            masterEmployees.value = masterEmployees.value + name.trim()
        }
    }

    fun addMasterWorkId(workId: String) {
        if (workId.isNotBlank() && !masterWorkIds.value.contains(workId.trim())) {
            masterWorkIds.value = masterWorkIds.value + workId.trim()
        }
    }

    fun addMasterActivity(activity: String) {
        if (activity.isNotBlank() && !masterActivities.value.contains(activity.trim())) {
            masterActivities.value = masterActivities.value + activity.trim()
        }
    }

    fun updateForm(transform: EntryFormState.() -> EntryFormState) {
        val updated = _formState.value.transform()
        val total = updated.totalAmount.toDoubleOrNull() ?: 0.0
        val advance = updated.advanceAmount.toDoubleOrNull() ?: 0.0
        val pending = (total - advance).coerceAtLeast(0.0)
        _formState.value = updated.copy(pendingAmount = pending)
    }

    fun saveRecord(): Boolean {
        val s = _formState.value
        if (s.customerName.isBlank() || s.phoneNumber.length != 10 || !s.phoneNumber.all { it.isDigit() }) {
            return false
        }

        val assignedEmployee = if (_currentUserRole.value == UserRole.USER) _loggedInEmployee.value else s.employeeName

        val newRecord = WorkRecord(
            employeeName = assignedEmployee,
            workId = s.workId,
            date = s.date,
            activity = s.activity,
            customerName = s.customerName.trim(),
            location = s.location,
            visitCount = s.visitCount,
            totalAmount = s.totalAmount.toDoubleOrNull() ?: 0.0,
            advanceAmount = s.advanceAmount.toDoubleOrNull() ?: 0.0,
            pendingAmount = s.pendingAmount,
            phoneNumber = s.phoneNumber.trim()
        )
        _records.value = listOf(newRecord) + _records.value
        resetFormToDefaults()
        return true
    }

    private fun resetFormToDefaults() {
        _formState.value = EntryFormState(
            employeeName = masterEmployees.value.firstOrNull() ?: "",
            workId = masterWorkIds.value.firstOrNull() ?: "",
            date = pastDates.firstOrNull() ?: "",
            activity = masterActivities.value.firstOrNull() ?: "",
            location = masterPincodes.value.firstOrNull() ?: "",
            visitCount = "1"
        )
    }
}

private data class FilterStateIntermediate(
    val recs: List<WorkRecord>,
    val role: UserRole,
    val emp: String,
    val query: String,
    val loc: String
)

private fun generateDatesList(): List<String> {
    val dates = mutableListOf<String>()
    val formatter = SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)
    val calendar = Calendar.getInstance()
    for (i in 0..30) {
        dates.add(formatter.format(calendar.time))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    return dates
}

private fun sampleInitialData() = listOf(
    WorkRecord(1, "Ramesh Kumar", "WRK-1001", "20-Sep-26", "New Installation", "Tex Styles Ltd", "641601 - Tirupur", "2", 15000.0, 7000.0, 8000.0, "9876543210"),
    WorkRecord(2, "Ramesh Kumar", "WRK-1002", "14-Aug-26", "Maintenance", "Knitwear Hub", "641602 - Tirupur North", "1", 9000.0, 4500.0, 4500.0, "9876543210"),
    WorkRecord(3, "Priya Sharma", "WRK-1003", "18-Sep-26", "Audit", "Apex Knits", "641001 - Coimbatore South", "1", 8500.0, 5000.0, 3500.0, "9123456780"),
    WorkRecord(4, "Priya Sharma", "WRK-1004", "05-Jul-26", "Emergency Repair", "Velan Motors", "641018 - Coimbatore Central", "3", 12000.0, 6000.0, 6000.0, "9123456780")
)
