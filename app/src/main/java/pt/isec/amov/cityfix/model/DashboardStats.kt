package pt.isec.amov.cityfix.model

data class DashboardStats(
    val totalTickets: Int = 0,
    val activeTickets: Int = 0,
    val resolvedTickets: Int = 0,
    val averageRepairTimeMillis: Long? = null,
) {
    val activeRatio: Double
        get() = if (totalTickets == 0) 0.0 else activeTickets.toDouble() / totalTickets
}
