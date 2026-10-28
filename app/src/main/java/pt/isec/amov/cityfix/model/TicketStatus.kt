package pt.isec.amov.cityfix.model

/** Lifecycle state of a ticket ("Estado" in the brief). Stored by name: do not rename. */
enum class TicketStatus { OPEN, INFO_REQUESTED, IN_PROGRESS, RESOLVED, CLOSED }
