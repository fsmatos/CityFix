package pt.isec.amov.cityfix.model

/** Anonymous dashboard figures (brief I.1.A). Default instance = no tickets. */
data class DashboardStats(
    val totalTickets: Int = 0,
    /** Tickets not yet repaired: OPEN, INFO_REQUESTED or IN_PROGRESS (see [isActive]). */
    val activeTickets: Int = 0,
    /** Tickets that entered the average: not active, with a RESOLVED history entry. */
    val resolvedTickets: Int = 0,
    /** Mean of (last RESOLVED dateTime - createdAt), in milliseconds. Null when no ticket was resolved. */
    val averageRepairTimeMillis: Long? = null,
) {
    /** Share of active tickets, 0.0..1.0 (format with NumberFormat.getPercentInstance()). Never NaN. */
    val activeRatio: Double
        get() = if (totalTickets == 0) 0.0 else activeTickets.toDouble() / totalTickets
}
