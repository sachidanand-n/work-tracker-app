package com.example.worktracker

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val recordsCollection = db.collection("work_records")
    private val masterDoc = db.collection("app_config").document("master_data")

    // Authentication & Role
    private val _currentUserRole = MutableStateFlow(UserRole.NONE)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _loggedInEmployee = MutableStateFlow("")
    val loggedInEmployee: StateFlow<String> = _loggedInEmployee.asStateFlow()

    // Master Data States
    val masterEmployees = MutableStateFlow<List<String>>(listOf("Ramesh Kumar", "Priya Sharma", "Murugan S", "Anand Raj"))
    val masterWorkIds = MutableStateFlow<List<String>>(listOf("WRK-1001", "WRK-1002", "WRK-1003", "WRK-1004"))
    val masterActivities = MutableStateFlow<List<String>>(listOf("Site Survey", "New Installation", "Maintenance", "Emergency Repair", "Audit"))
    val masterPincodes = MutableStateFlow(indiaPincodeMap.map { "${it.key} - ${it.value}" })
    val visitCounts = listOf("1", "2", "3", "4", "5+")
    val pastDates: List<String> = generateDatesList()

    // Sync Status indicator for Admin UI
    val syncStatusMessage = MutableStateFlow("")
    val isSyncing = MutableStateFlow(false)

    // Live Records across all devices
    private val _records = MutableStateFlow<List<WorkRecord>>(emptyList())
    val records: StateFlow<List<WorkRecord>> = _records.asStateFlow()

    private val _formState = MutableStateFlow(EntryFormState())
    val formState: StateFlow<EntryFormState> = _formState.asStateFlow()

    // Dashboard Filters
    val searchQuery = MutableStateFlow("")
    val selectedLocationFilter = MutableStateFlow("All")
    val selectedActivityFilter = MutableStateFlow("All")
    val selectedWorkIdFilter = MutableStateFlow("All")
    val selectedEmployeeFilter = MutableStateFlow("All")

    init {
        listenToMasterData()
        listenToWorkRecords()
    }

    // --- Real-time Cloud Listener for Master Lists (All Devices) ---
    private fun listenToMasterData() {
        masterDoc.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val cloudEmployees = snapshot.get("employees") as? List<*>
            val cloudWorkIds = snapshot.get("workIds") as? List<*>
            val cloudActivities = snapshot.get("activities") as? List<*>

            cloudEmployees?.filterIsInstance<String>()?.let { if (it.isNotEmpty()) masterEmployees.value = it }
            cloudWorkIds?.filterIsInstance<String>()?.let { if (it.isNotEmpty()) masterWorkIds.value = it }
            cloudActivities?.filterIsInstance<String>()?.let { if (it.isNotEmpty()) masterActivities.value = it }
        }
    }

    // --- Real-time Cloud Listener for Work Records ---
    private fun listenToWorkRecords() {
        recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val cloudList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(WorkRecord::class.java)?.copy(id = doc.id)
                }
                _records.value = cloudList
            }
    }

    // --- SYNC BUTTON ACTION (Admin triggers cloud broadcast) ---
    fun syncMasterDataToCloud() {
        isSyncing.value = true
        syncStatusMessage.value = "Syncing to cloud..."

        val payload = hashMapOf(
            "employees" to masterEmployees.value,
            "workIds" to masterWorkIds.value,
            "activities" to masterActivities.value
        )

        masterDoc.set(payload, SetOptions.merge())
            .addOnSuccessListener {
                isSyncing.value = false
                syncStatusMessage.value = "Synced successfully! Available on all devices."
            }
            .addOnFailureListener { e ->
                isSyncing.value = false
                syncStatusMessage.value = "Sync failed: ${e.localizedMessage}"
            }
    }

    // Local additions before pressing Sync
    fun addMasterEmployee(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !masterEmployees.value.contains(trimmed)) {
            masterEmployees.value = masterEmployees.value + trimmed
            syncStatusMessage.value = "Unsaved changes! Click 'Sync to All Devices'."
        }
    }

    fun addMasterWorkId(workId: String) {
        val trimmed = workId.trim()
        if (trimmed.isNotBlank() && !masterWorkIds.value.contains(trimmed)) {
            masterWorkIds.value = masterWorkIds.value + trimmed
            syncStatusMessage.value = "Unsaved changes! Click 'Sync to All Devices'."
        }
    }

    fun addMasterActivity(activity: String) {
        val trimmed = activity.trim()
        if (trimmed.isNotBlank() && !masterActivities.value.contains(trimmed)) {
            masterActivities.value = masterActivities.value + trimmed
            syncStatusMessage.value = "Unsaved changes! Click 'Sync to All Devices'."
        }
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
            phoneNumber = s.phoneNumber.trim(),
            timestamp = System.currentTimeMillis()
        )

        recordsCollection.add(newRecord)
        resetFormToDefaults()
        return true
    }

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

    fun updateForm(transform: EntryFormState.() -> EntryFormState) {
        val updated = _formState.value.transform()
        val total = updated.totalAmount.toDoubleOrNull() ?: 0.0
        val advance = updated.advanceAmount.toDoubleOrNull() ?: 0.0
        val pending = (total - advance).coerceAtLeast(0.0)
        _formState.value = updated.copy(pendingAmount = pending)
    }

    fun resetFilters() {
        searchQuery.value = ""
        selectedLocationFilter.value = "All"
        selectedActivityFilter.value = "All"
        selectedWorkIdFilter.value = "All"
        selectedEmployeeFilter.value = "All"
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
