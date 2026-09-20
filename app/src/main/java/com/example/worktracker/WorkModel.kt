package com.example.worktracker

data class WorkRecord(
    val id: Long = System.currentTimeMillis(),
    val employeeName: String,     // Added field
    val workId: String,
    val date: String,             // Format: DD-MMM-YY
    val activity: String,
    val customerName: String,
    val location: String,         // Format: "Pincode - Place"
    val visitCount: String,
    val totalAmount: Double,
    val advanceAmount: Double,
    val pendingAmount: Double,
    val phoneNumber: String       // 10 digits mandatory
)

// Preloaded India Pincodes mapping (PIN to City/Place)
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
