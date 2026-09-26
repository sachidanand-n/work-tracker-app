package com.example.worktracker

enum class UserRole { NONE, USER, ADMIN }

data class MasterItem(
    val name: String = "",
    val isActive: Boolean = true
)

data class PincodeInfo(
    val pincode: String,
    val place: String,
    val district: String
)

data class EntryFormState(
    val employeeName: String = "",
    val workId: String = "",
    val date: String = "",
    val activity: String = "",
    val customerName: String = "",
    val pincode: String = "641601",
    val place: String = "Tirupur",
    val district: String = "Tiruppur",
    val visitCount: String = "1",
    val totalAmount: String = "",
    val advanceAmount: String = "",
    val pendingAmount: Double = 0.0,
    val phoneNumber: String = ""
)

data class MonthlyAdvance(
    val monthYear: String,
    val totalAdvance: Double
)

data class WorkRecord(
    val id: String = "",
    val employeeName: String = "",
    val workId: String = "",
    val date: String = "",
    val activity: String = "",
    val customerName: String = "",
    val pincode: String = "",
    val place: String = "",
    val district: String = "",
    val location: String = "", // Legacy fallback string
    val visitCount: String = "1",
    val totalAmount: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val phoneNumber: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class CloudMasterData(
    val employees: List<MasterItem> = emptyList(),
    val workIds: List<MasterItem> = emptyList(),
    val activities: List<MasterItem> = emptyList()
)
