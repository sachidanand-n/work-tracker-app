package com.example.worktracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val recordsCollection = db.collection("work_records")
    private val masterDoc = db.collection("app_config").document("master_data")
    private val remindersDoc = db.collection("app_config").document("reminders")

    private val _currentUserRole = MutableStateFlow(UserRole.NONE)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _loggedInEmployee = MutableStateFlow("")
    val loggedInEmployee: StateFlow<String> = _loggedInEmployee.asStateFlow()

    val masterEmployees = MutableStateFlow<List<MasterItem>>(
        listOf(
            MasterItem("Ramesh Kumar", true),
            MasterItem("Priya Sharma", true),
            MasterItem("Murugan S", true),
            MasterItem("Anand Raj", true)
        )
    )

    val masterWorkIds = MutableStateFlow<List<MasterItem>>(
        listOf(
            MasterItem("WRK-1001", true),
            MasterItem("WRK-1002", true),
            MasterItem("WRK-1003", true),
            MasterItem("WRK-1004", true)
        )
    )

    val masterActivities = MutableStateFlow<List<MasterItem>>(
        listOf(
            MasterItem("Site Survey", true),
            MasterItem("New Installation", true),
            MasterItem("Maintenance", true),
            MasterItem("Emergency Repair", true),
            MasterItem("Audit", true),
            MasterItem("Valuation", true),
            MasterItem("Stability", true)
        )
    )

    val masterPincodes = MutableStateFlow<List<MasterItem>>(
        tamilNaduPincodeList.map { MasterItem(it.pincode, true) }
    )

    val visitCounts: List<String> = listOf("1", "2", "3", "4", "5+")
    val pastDates: List<String> = generateDatesList()

    val isSyncing = MutableStateFlow(false)
    val syncProgress = MutableStateFlow(0f)
    val syncPercentage = MutableStateFlow("0%")
    val syncStatusMessage = MutableStateFlow("")
    val syncHasError = MutableStateFlow(false)

    // Scrolling Reminder States (Admin Dashboard & Associate Data Entry)
    val adminScrollingReminder = MutableStateFlow("")
    val associateRemindersMap = MutableStateFlow>(emptyMap())

    private val _records = MutableStateFlow<List<WorkRecord>>(emptyList())
    val records: StateFlow<List<WorkRecord>> = _records.asStateFlow()

    private val _formState = MutableStateFlow(
        EntryFormState(
            pincode = tamilNaduPincodeList.firstOrNull()?.pincode ?: "641601",
            place = tamilNaduPincodeList.firstOrNull()?.place ?: "Tirupur Head Post",
            district = tamilNaduPincodeList.firstOrNull()?.district ?: "Tiruppur"
        )
    )
    val formState: StateFlow<EntryFormState> = _formState.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val selectedMonthFilter = MutableStateFlow("All")
    val selectedDistrictFilter = MutableStateFlow("All")
    val selectedActivityFilter = MutableStateFlow("All")
    val selectedWorkIdFilter = MutableStateFlow("All")
    val selectedEmployeeFilter = MutableStateFlow("All")
    val selectedCalendarDate = MutableStateFlow<String?>(null)

    init {
        listenToMasterData()
        listenToWorkRecords()
        listenToReminders()
    }

    // --- REMINDER ACTIONS (CLOUD SYNCED) ---
    private fun listenToReminders() {
        remindersDoc.addSnapshotListener { snapshot, _ ->
            if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
            adminScrollingReminder.value = snapshot.getString("adminReminder") ?: ""
            val rawMap = snapshot.get("associateReminders") as? Map<*, *>
            if (rawMap != null) {
                val parsed = mutableMapOf<String, String>()
                rawMap.forEach { (k, v) ->
                    if (k is String && v is String) parsed[k] = v
                }
                associateRemindersMap.value = parsed
            }
        }
    }

    fun setAdminReminder(message: String) {
        val clean = message.trim()
        adminScrollingReminder.value = clean
        remindersDoc.set(hashMapOf("adminReminder" to clean), SetOptions.merge())
    }

    fun clearAdminReminder() {
        adminScrollingReminder.value = ""
        remindersDoc.set(hashMapOf("adminReminder" to ""), SetOptions.merge())
    }

    fun setAssociateReminder(associateName: String, message: String) {
        val key = normalizeAssociateName(associateName).lowercase(Locale.ENGLISH)
        val clean = message.trim()
        val updated = associateRemindersMap.value.toMutableMap()
        if (clean.isEmpty()) updated.remove(key) else updated[key] = clean
        associateRemindersMap.value = updated
        remindersDoc.set(hashMapOf("associateReminders" to updated), SetOptions.merge())
    }

    fun broadcastReminderToAllAssociates(message: String) {
        val clean = message.trim()
        val updated = associateRemindersMap.value.toMutableMap()
        updated["all"] = clean
        associateRemindersMap.value = updated
        remindersDoc.set(hashMapOf("associateReminders" to updated), SetOptions.merge())
    }

    fun getReminderForAssociate(associateName: String): String {
        val key = normalizeAssociateName(associateName).lowercase(Locale.ENGLISH)
        val personal = associateRemindersMap.value[key].orEmpty()
        val global = associateRemindersMap.value["all"].orEmpty()
        return listOf(personal, global).filter { it.isNotBlank() }.joinToString("   •   ")
    }

    // --- PROPER CASE & STANDARDIZATION HELPERS ---
    fun formatProperCase(input: String): String {
        val cleaned = input.trim().replace(Regex("\\s+"), " ")
        if (cleaned.isEmpty()) return ""
        return cleaned.split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.ENGLISH).replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.ENGLISH) else ch.toString()
            }
        }
    }

    fun normalizeActivityName(raw: String): String {
        val proper = formatProperCase(raw)
        return when {
            proper.equals("Valuation", ignoreCase = true) -> "Valuation"
            proper.equals("Stability", ignoreCase = true) -> "Stability"
            else -> proper
        }
    }

    fun normalizeAssociateName(raw: String): String = formatProperCase(raw)

    fun normalizeCustomerName(raw: String): String = formatProperCase(raw)

    fun lookupPincodeDetails(pin: String): Pair<String, String>? {
        return tamilNaduPincodeMap[pin.trim()]
    }

    private fun parseCloudList(rawList: List<*>?, type: String): List<MasterItem>? {
        if (rawList == null) return null
        val seenNames = mutableSetOf<String>()
        val items = mutableListOf<MasterItem>()
        for (item in rawList) {
            var name = ""
            var active = true
            when (item) {
                is Map<*, *> -> {
                    name = item["name"] as? String ?: ""
                    active = item["isActive"] as? Boolean ?: (item["active"] as? Boolean ?: true)
                }
                is String -> name = item
            }
            val normalizedName = when (type) {
                "activity" -> normalizeActivityName(name)
                "employee" -> normalizeAssociateName(name)
                else -> name.trim()
            }
            val key = normalizedName.lowercase(Locale.ENGLISH)
            if (normalizedName.isNotBlank() && seenNames.add(key)) {
                items.add(MasterItem(normalizedName, active))
            }
        }
        return if (items.isNotEmpty()) items else null
    }

    private fun listenToMasterData() {
        masterDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                syncStatusMessage.value = "Cloud listener error: ${error.message}"
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

            parseCloudList(snapshot.get("employees") as? List<*>, "employee")?.let { masterEmployees.value = it }
            parseCloudList(snapshot.get("workIds") as? List<*>, "workId")?.let { masterWorkIds.value = it }
            parseCloudList(snapshot.get("activities") as? List<*>, "activity")?.let { masterActivities.value = it }
            parseCloudList(snapshot.get("pincodes") as? List<*>, "pincode")?.let { masterPincodes.value = it }
        }
    }

    private fun listenToWorkRecords() {
        recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val cloudList = snapshot.documents.mapNotNull { doc ->
                    val rec = doc.toObject(WorkRecord::class.java)?.copy(id = doc.id)
                    rec?.copy(
                        activity = normalizeActivityName(rec.activity),
                        employeeName = normalizeAssociateName(rec.employeeName),
                        customerName = normalizeCustomerName(rec.customerName)
                    )
                }
                _records.value = cloudList
            }
    }

    fun standardizeAndMergeMasterDataInCloud() {
        viewModelScope.launch {
            isSyncing.value = true
            syncHasError.value = false
            syncProgress.value = 0.25f
            syncPercentage.value = "25%"
            syncStatusMessage.value = "Standardizing Activities, Associates & Customers to Proper Case..."

            val mergedActivities = linkedMapOf<String, MasterItem>()
            for (item in masterActivities.value) {
                val cleanName = normalizeActivityName(item.name)
                val key = cleanName.lowercase(Locale.ENGLISH)
                val existing = mergedActivities[key]
                if (existing == null) {
                    mergedActivities[key] = MasterItem(cleanName, item.isActive)
                } else {
                    mergedActivities[key] = existing.copy(isActive = existing.isActive || item.isActive)
                }
            }
            if (!mergedActivities.containsKey("valuation")) mergedActivities["valuation"] = MasterItem("Valuation", true)
            if (!mergedActivities.containsKey("stability")) mergedActivities["stability"] = MasterItem("Stability", true)
            masterActivities.value = mergedActivities.values.toList()

            val mergedEmployees = linkedMapOf<String, MasterItem>()
            for (item in masterEmployees.value) {
                val cleanEmp = normalizeAssociateName(item.name)
                val key = cleanEmp.lowercase(Locale.ENGLISH)
                val existing = mergedEmployees[key]
                if (existing == null) {
                    mergedEmployees[key] = MasterItem(cleanEmp, item.isActive)
                } else {
                    mergedEmployees[key] = existing.copy(isActive = existing.isActive || item.isActive)
                }
            }
            masterEmployees.value = mergedEmployees.values.toList()

            syncProgress.value = 0.60f
            syncPercentage.value = "60%"
            syncStatusMessage.value = "Standardizing historical records in Cloud..."

            recordsCollection.get()
                .addOnSuccessListener { querySnapshot ->
                    val batch = db.batch()
                    var updatedCount = 0
                    for (doc in querySnapshot.documents) {
                        val rawAct = doc.getString("activity") ?: ""
                        val cleanAct = normalizeActivityName(rawAct)
                        val rawEmp = doc.getString("employeeName") ?: ""
                        val cleanEmp = normalizeAssociateName(rawEmp)
                        val rawCust = doc.getString("customerName") ?: ""
                        val cleanCust = normalizeCustomerName(rawCust)

                        val updates = mutableMapOf<String, Any>()
                        if (rawAct != cleanAct && cleanAct.isNotBlank()) updates["activity"] = cleanAct
                        if (rawEmp != cleanEmp && cleanEmp.isNotBlank()) updates["employeeName"] = cleanEmp
                        if (rawCust != cleanCust && cleanCust.isNotBlank()) updates["customerName"] = cleanCust

                        if (updates.isNotEmpty()) {
                            batch.update(doc.reference, updates)
                            updatedCount++
                        }
                    }
                    if (updatedCount > 0) batch.commit()
                    syncMasterDataToCloud()
                }
                .addOnFailureListener { syncMasterDataToCloud() }
        }
    }

    fun toggleEmployeeStatus(name: String) {
        masterEmployees.value = masterEmployees.value.map {
            if (it.name == name) it.copy(isActive = !it.isActive) else it
        }
        syncStatusMessage.value = "Status changed! Click 'Sync to All Devices' to push."
    }

    fun toggleWorkIdStatus(workId: String) {
        masterWorkIds.value = masterWorkIds.value.map {
            if (it.name == workId) it.copy(isActive = !it.isActive) else it
        }
        syncStatusMessage.value = "Status changed! Click 'Sync to All Devices' to push."
    }

    fun toggleActivityStatus(activity: String) {
        masterActivities.value = masterActivities.value.map {
            if (it.name == activity) it.copy(isActive = !it.isActive) else it
        }
        syncStatusMessage.value = "Status changed! Click 'Sync to All Devices' to push."
    }

    fun togglePincodeStatus(pin: String) {
        masterPincodes.value = masterPincodes.value.map {
            if (it.name == pin) it.copy(isActive = !it.isActive) else it
        }
        syncStatusMessage.value = "Pincode status changed! Click 'Sync to All Devices' to push."
    }

    fun addMasterEmployee(name: String) {
        val properEmp = normalizeAssociateName(name)
        if (properEmp.isNotBlank() && masterEmployees.value.none { it.name.equals(properEmp, ignoreCase = true) }) {
            masterEmployees.value = masterEmployees.value + MasterItem(properEmp, true)
            syncStatusMessage.value = "Added '$properEmp'. Click 'Sync to All Devices'."
            syncHasError.value = false
        }
    }

    fun addMasterWorkId(workId: String) {
        val trimmed = workId.trim()
        if (trimmed.isNotBlank() && masterWorkIds.value.none { it.name.equals(trimmed, ignoreCase = true) }) {
            masterWorkIds.value = masterWorkIds.value + MasterItem(trimmed, true)
            syncStatusMessage.value = "Unsaved changes! Click 'Sync to All Devices'."
            syncHasError.value = false
        }
    }

    fun addMasterActivity(activity: String) {
        val properName = normalizeActivityName(activity)
        if (properName.isNotBlank() && masterActivities.value.none { it.name.equals(properName, ignoreCase = true) }) {
            masterActivities.value = masterActivities.value + MasterItem(properName, true)
            syncStatusMessage.value = "Added '$properName'. Click 'Sync to All Devices'."
            syncHasError.value = false
        }
    }

    fun addMasterPincode(pin: String): Boolean {
        val cleaned = pin.trim()
        if (cleaned.length != 6 || !cleaned.all { it.isDigit() }) {
            syncStatusMessage.value = "Enter a valid 6-digit Pincode."
            return false
        }
        val dbDetails = lookupPincodeDetails(cleaned)
        if (dbDetails == null) {
            syncStatusMessage.value = "Pincode $cleaned not found in Tamil Nadu database."
            return false
        }

        val existingIndex = masterPincodes.value.indexOfFirst { it.name == cleaned }
        if (existingIndex >= 0) {
            val updated = masterPincodes.value.toMutableList()
            updated.removeAt(existingIndex)
            updated.add(0, MasterItem(cleaned, true))
            masterPincodes.value = updated
            syncStatusMessage.value = "Pincode $cleaned (${dbDetails.first},${dbDetails.second}) activated!"
        } else {
            masterPincodes.value = listOf(MasterItem(cleaned, true)) + masterPincodes.value
            syncStatusMessage.value = "Enrolled $cleaned (${dbDetails.first},${dbDetails.second}). Click 'Sync to All Devices'."
        }
        syncHasError.value = false
        return true
    }

    fun syncMasterDataToCloud() {
        viewModelScope.launch {
            isSyncing.value = true
            syncHasError.value = false
            syncProgress.value = 0.15f
            syncPercentage.value = "15%"
            syncStatusMessage.value = "Validating master database..."
            delay(200)

            syncProgress.value = 0.45f
            syncPercentage.value = "45%"
            syncStatusMessage.value = "Packaging active and inactive items..."
            delay(200)

            syncProgress.value = 0.70f
            syncPercentage.value = "70%"
            syncStatusMessage.value = "Uploading to Cloud Firestore..."

            val payload = hashMapOf(
                "employees" to masterEmployees.value.map { mapOf("name" to normalizeAssociateName(it.name), "isActive" to it.isActive) },
                "workIds" to masterWorkIds.value.map { mapOf("name" to it.name, "isActive" to it.isActive) },
                "activities" to masterActivities.value.map { mapOf("name" to normalizeActivityName(it.name), "isActive" to it.isActive) },
                "pincodes" to masterPincodes.value.map { mapOf("name" to it.name.trim(), "isActive" to it.isActive) }
            )

            val completed = withTimeoutOrNull(8000) {
                var finished = false
                masterDoc.set(payload, SetOptions.merge())
                    .addOnSuccessListener {
                        syncProgress.value = 1.0f
                        syncPercentage.value = "100%"
                        syncStatusMessage.value = "100% Synced! Available on all field devices."
                        syncHasError.value = false
                        isSyncing.value = false
                        finished = true
                    }
                    .addOnFailureListener { e ->
                        syncProgress.value = 0f
                        syncPercentage.value = "Failed"
                        syncStatusMessage.value = "Error: ${e.localizedMessage ?: "Permission Denied"}"
                        syncHasError.value = true
                        isSyncing.value = false
                        finished = true
                    }

                while (!finished) delay(100)
                true
            }

            if (completed == null && isSyncing.value) {
                syncProgress.value = 0f
                syncPercentage.value = "Timeout"
                syncStatusMessage.value = "Connection timed out. Check network or Firestore rules."
                syncHasError.value = true
                isSyncing.value = false
            }
        }
    }

    fun onPincodeSelected(pin: String) {
        val found = tamilNaduPincodeMap[pin]
        if (found != null) {
            updateForm { copy(pincode = pin, place = found.first, district = found.second) }
        } else {
            updateForm { copy(pincode = pin) }
        }
    }

    fun toggleCalendarDate(dateStr: String) {
        selectedCalendarDate.value = if (selectedCalendarDate.value == dateStr) null else dateStr
    }

    fun saveRecord(): Boolean {
        val s = _formState.value
        val properCustomer = normalizeCustomerName(s.customerName)
        if (properCustomer.isBlank() || s.phoneNumber.length != 10 || !s.phoneNumber.all { char -> char.isDigit() }) {
            return false
        }

        val rawEmp = if (_currentUserRole.value == UserRole.USER) _loggedInEmployee.value else s.employeeName
        val assignedEmployee = normalizeAssociateName(rawEmp)

        val newRecord = WorkRecord(
            employeeName = assignedEmployee,
            workId = s.workId,
            date = s.date,
            activity = normalizeActivityName(s.activity),
            customerName = properCustomer,
            pincode = s.pincode,
            place = s.place,
            district = s.district,
            location = "${s.pincode} - ${s.place},${s.district}",
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
        val properEmp = normalizeAssociateName(employeeName)
        _loggedInEmployee.value = properEmp
        _currentUserRole.value = UserRole.USER
        updateForm { copy(employeeName = properEmp) }
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
        selectedCalendarDate.value = null
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
        selectedMonthFilter.value = "All"
        selectedDistrictFilter.value = "All"
        selectedActivityFilter.value = "All"
        selectedWorkIdFilter.value = "All"
        selectedEmployeeFilter.value = "All"
        selectedCalendarDate.value = null
    }

    private fun resetFormToDefaults() {
        val defaultPin = masterPincodes.value.firstOrNull { it.isActive }?.name
            ?: tamilNaduPincodeList.firstOrNull()?.pincode
            ?: "641601"
        val pair = tamilNaduPincodeMap[defaultPin]
        _formState.value = EntryFormState(
            employeeName = masterEmployees.value.firstOrNull { it.isActive }?.name ?: "",
            workId = masterWorkIds.value.firstOrNull { it.isActive }?.name ?: "",
            date = pastDates.firstOrNull() ?: "",
            activity = masterActivities.value.firstOrNull { it.isActive }?.name ?: "",
            pincode = defaultPin,
            place = pair?.first ?: "Tirupur Head Post",
            district = pair?.second ?: "Tiruppur",
            visitCount = "1"
        )
    }

    val slicerFilteredRecords = combine(
        _records,
        _currentUserRole,
        _loggedInEmployee,
        searchQuery,
        selectedDistrictFilter
    ) { recs: List<WorkRecord>, role: UserRole, emp: String, query: String, dist: String ->
        FilterIntermediate(recs, role, emp, query, dist)
    }.combine(
        combine(selectedActivityFilter, selectedWorkIdFilter, selectedEmployeeFilter, selectedMonthFilter) { act, work, targetEmp, month ->
            QuadFilter(act, work, targetEmp, month)
        }
    ) { inter: FilterIntermediate, quad: QuadFilter ->
        val (act, work, targetEmp, month) = quad
        val roleFiltered = if (inter.role == UserRole.USER) {
            inter.recs.filter { it.employeeName.equals(inter.emp, ignoreCase = true) }
        } else {
            if (targetEmp == "All") inter.recs else inter.recs.filter { it.employeeName.equals(targetEmp, ignoreCase = true) }
        }

        roleFiltered.filter { record ->
            val recordMonth = extractMonthYear(record.date)
            val matchMonth = month == "All" || recordMonth.equals(month, ignoreCase = true)
            val matchDist = inter.loc == "All" || record.district.equals(inter.loc, ignoreCase = true)
            val matchAct = act == "All" || record.activity.equals(act, ignoreCase = true)
            val matchWork = work == "All" || record.workId == work
            val matchQuery = inter.query.isBlank() ||
                record.customerName.contains(inter.query, ignoreCase = true) ||
                record.phoneNumber.contains(inter.query) ||
                record.workId.contains(inter.query, ignoreCase = true) ||
                record.pincode.contains(inter.query) ||
                record.place.contains(inter.query, ignoreCase = true) ||
                record.district.contains(inter.query, ignoreCase = true) ||
                record.location.contains(inter.query, ignoreCase = true) ||
                record.employeeName.contains(inter.query, ignoreCase = true)

            matchMonth && matchDist && matchAct && matchWork && matchQuery
        }
    }

    val visibleRecords = combine(slicerFilteredRecords, selectedCalendarDate) { recs, calDate ->
        if (calDate == null) recs else recs.filter { it.date.equals(calDate, ignoreCase = true) }
    }
}

data class FilterIntermediate(
    val recs: List<WorkRecord>,
    val role: UserRole,
    val emp: String,
    val query: String,
    val loc: String
)

data class QuadFilter(
    val act: String,
    val work: String,
    val targetEmp: String,
    val month: String
)

fun extractMonthYear(dateStr: String): String {
    val parts = dateStr.split("-")
    return if (parts.size == 3) "${parts[1]}-${parts[2]}" else dateStr
}

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
