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

data class BroadcastReminder(
    val id: String = "",
    val targetAssociate: String = "ALL", // "ALL" or specific associate name
    val message: String = "",
    val createdBy: String = "Admin",
    val timestamp: Long = System.currentTimeMillis()
)

data class EntryFormState(
    val employeeName: String = "",
    val workId: String = "",
    val date: String = "",
    val activity: String = "",
    val customerName: String = "",
    val pincode: String = "641601",
    val place: String = "Tirupur Head Post",
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
    val location: String = "",
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
    val activities: List<MasterItem> = emptyList(),
    val pincodes: List<MasterItem> = emptyList()
)
