package com.example.worktracker

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
    val location: String = "",
    val visitCount: String = "1",
    val totalAmount: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val phoneNumber: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class CloudMasterData(
    val employees: List<String> = listOf("Ramesh Kumar", "Priya Sharma", "Murugan S", "Anand Raj"),
    val workIds: List<String> = listOf("WRK-1001", "WRK-1002", "WRK-1003", "WRK-1004"),
    val activities: List<String> = listOf("Site Survey", "New Installation", "Maintenance", "Emergency Repair", "Audit")
)

val indiaPincodeMap = linkedMapOf(
    "641601" to "Tirupur",
    "641602" to "Tirupur North",
    "641001" to "Coimbatore South",
    "641018" to "Coimbatore Central",
    "600001" to "Chennai Central",
    "600028" to "Chennai Mylapore",
    "560001" to "Bangalore G.P.O",
    "500001" to "Hyderabad G.P.O",
    "400001" to "Mumbai G.P.O",
    "110001" to "New Delhi Central"
)
