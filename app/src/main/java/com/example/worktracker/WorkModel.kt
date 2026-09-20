package com.example.worktracker

data class WorkRecord(
    val id: Long = System.currentTimeMillis(),
    val workId: String,
    val date: String,
    val activity: String,
    val customerName: String,
    val location: String,
    val visitCount: String,
    val totalAmount: Double,
    val advanceAmount: Double,
    val pendingAmount: Double,
    val phoneNumber: String
)

object DropdownOptions {
    val workIds = listOf("WRK-1001", "WRK-1002", "WRK-1003", "WRK-1004", "WRK-1005")
    val dates = listOf("2026-09-20", "2026-09-21", "2026-09-22", "2026-09-23", "2026-09-24")
    val activities = listOf("Site Survey", "New Installation", "Maintenance", "Emergency Repair", "Audit")
    val locations = listOf("Downtown Hub", "North Industrial Zone", "East Suburb", "West Park", "South Metro")
    val visitCounts = listOf("1", "2", "3", "4", "5+")
}
