package pt.isec.amov.cityfix.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.isec.amov.cityfix.utils.haversineMeters
import kotlin.math.cos

class TicketsListTest {

    /** Near Pr. Republica, Coimbra. */
    private val anchor = Coordinates(40.2089, -8.4193)

    private val category = "pavement"

    /** Moves [meters] north of [from] (about 111 195 m per degree of latitude). */
    private fun north(from: Coordinates, meters: Double) =
        Coordinates(from.latitude + meters / 111_195.0, from.longitude)

    /** Moves [meters] east of [from]: a degree of longitude shrinks with cos(latitude). */
    private fun east(from: Coordinates, meters: Double) = Coordinates(
        from.latitude,
        from.longitude + meters / (111_195.0 * cos(Math.toRadians(from.latitude))),
    )

    private fun ticket(
        id: String,
        location: Coordinates = anchor,
        categoryId: String = category,
        situation: Situation = Situation.DAMAGE,
        status: TicketStatus = TicketStatus.OPEN,
        scope: Scope = Scope.PUBLIC,
        createdAt: Long = 1_000L,
        statusHistory: List<StatusHistory> = emptyList(),
    ) = Ticket(
        id = id,
        location = location,
        categoryId = categoryId,
        situation = situation,
        currentStatus = status,
        scope = scope,
        createdAt = createdAt,
        statusHistory = statusHistory,
    )

    // findNearby

    @Test
    fun findNearby_finds_ticket_9m_away() {
        val list = TicketsList(listOf(ticket("a", location = north(anchor, 9.0))))
        assertEquals("a", list.findNearby(anchor, category, Situation.DAMAGE)?.id)
    }

    @Test
    fun findNearby_ignores_ticket_11m_away() {
        val list = TicketsList(listOf(ticket("a", location = north(anchor, 11.0))))
        assertNull(list.findNearby(anchor, category, Situation.DAMAGE))
    }

    @Test
    fun findNearby_finds_ticket_9m_east() {
        val list = TicketsList(listOf(ticket("a", location = east(anchor, 9.0))))
        assertEquals("a", list.findNearby(anchor, category, Situation.DAMAGE)?.id)
    }

    @Test
    fun findNearby_ignores_ticket_11m_east() {
        val list = TicketsList(listOf(ticket("a", location = east(anchor, 11.0))))
        assertNull(list.findNearby(anchor, category, Situation.DAMAGE))
    }

    @Test
    fun findNearby_ignores_other_category() {
        val list = TicketsList(listOf(ticket("a", location = north(anchor, 9.0), categoryId = "lighting")))
        assertNull(list.findNearby(anchor, category, Situation.DAMAGE))
    }

    @Test
    fun findNearby_ignores_other_situation() {
        val list = TicketsList(listOf(ticket("a", location = north(anchor, 9.0), situation = Situation.THEFT)))
        assertNull(list.findNearby(anchor, category, Situation.DAMAGE))
    }

    @Test
    fun findNearby_ignores_resolved_ticket() {
        val list = TicketsList(listOf(ticket("a", location = north(anchor, 9.0), status = TicketStatus.RESOLVED)))
        assertNull(list.findNearby(anchor, category, Situation.DAMAGE))
    }

    @Test
    fun findNearby_ignores_closed_ticket() {
        val list = TicketsList(listOf(ticket("a", location = north(anchor, 9.0), status = TicketStatus.CLOSED)))
        assertNull(list.findNearby(anchor, category, Situation.DAMAGE))
    }

    @Test
    fun findNearby_ignores_ticket_at_unset_location() {
        val list = TicketsList(listOf(ticket("a", location = Coordinates())))
        assertNull(list.findNearby(north(Coordinates(), 9.0), category, Situation.DAMAGE))
    }

    @Test
    fun findNearby_returns_null_for_unset_query() {
        val list = TicketsList(listOf(ticket("a", location = north(Coordinates(), 9.0))))
        assertNull(list.findNearby(Coordinates(), category, Situation.DAMAGE))
    }

    @Test
    fun findNearby_returns_null_for_blank_category() {
        val list = TicketsList(listOf(ticket("a", location = north(anchor, 9.0), categoryId = "")))
        assertNull(list.findNearby(anchor, "", Situation.DAMAGE))
    }

    @Test
    fun findNearby_returns_the_nearer_match() {
        val list = TicketsList(
            listOf(
                ticket("far", location = north(anchor, 8.0)),
                ticket("near", location = north(anchor, 3.0)),
            ),
        )
        assertEquals("near", list.findNearby(anchor, category, Situation.DAMAGE)?.id)
    }

    // publicActive and isActive

    @Test
    fun publicActive_keeps_only_public_active_tickets() {
        val list = TicketsList(
            listOf(
                ticket("private-open", scope = Scope.PRIVATE),
                ticket("public-resolved", status = TicketStatus.RESOLVED),
                ticket("public-closed", status = TicketStatus.CLOSED),
                ticket("public-open", status = TicketStatus.OPEN),
                ticket("public-info", status = TicketStatus.INFO_REQUESTED),
                ticket("public-progress", status = TicketStatus.IN_PROGRESS),
            ),
        )
        val ids = list.publicActive().map { it.id }.toSet()
        assertEquals(setOf("public-open", "public-info", "public-progress"), ids)
    }

    @Test
    fun publicActive_is_newest_first_and_a_copy() {
        val list = TicketsList(
            listOf(
                ticket("old", createdAt = 1_000L),
                ticket("new", createdAt = 3_000L),
                ticket("middle", createdAt = 2_000L),
                ticket("private", createdAt = 4_000L, scope = Scope.PRIVATE),
            ),
        )
        val publicList = list.publicActive()
        assertEquals(listOf("new", "middle", "old"), publicList.map { it.id })

        // sortedWith returns a fixed-size list, so write an element instead of clearing it.
        (publicList as MutableList<Ticket>)[0] = ticket("x")
        assertEquals(listOf("new", "middle", "old"), list.publicActive().map { it.id })
    }

    @Test
    fun isActive_only_for_unrepaired_statuses() {
        assertTrue(Ticket(currentStatus = TicketStatus.OPEN).isActive)
        assertTrue(Ticket(currentStatus = TicketStatus.INFO_REQUESTED).isActive)
        assertTrue(Ticket(currentStatus = TicketStatus.IN_PROGRESS).isActive)
        assertFalse(Ticket(currentStatus = TicketStatus.RESOLVED).isActive)
        assertFalse(Ticket(currentStatus = TicketStatus.CLOSED).isActive)
    }

    // stats

    @Test
    fun stats_on_empty_list_is_default() {
        val stats = TicketsList().stats()
        assertEquals(DashboardStats(), stats)
        assertEquals(0.0, stats.activeRatio, 0.0)
        assertFalse(stats.activeRatio.isNaN())
        assertNull(stats.averageRepairTimeMillis)
    }

    @Test
    fun stats_on_mixed_list() {
        val hour = 3_600_000L
        val list = TicketsList(
            listOf(
                // Active, never resolved: counts as active, skipped from the average.
                ticket("open", status = TicketStatus.OPEN),
                // Private and active: still counted (Q9).
                ticket("private", status = TicketStatus.IN_PROGRESS, scope = Scope.PRIVATE),
                // Resolved after 2 h.
                ticket(
                    "resolved",
                    status = TicketStatus.RESOLVED,
                    createdAt = 10 * hour,
                    statusHistory = listOf(
                        StatusHistory(TicketStatus.OPEN, 10 * hour),
                        StatusHistory(TicketStatus.RESOLVED, 12 * hour),
                    ),
                ),
                // Reopened and resolved twice: the last resolution (6 h) is used.
                ticket(
                    "reopened",
                    status = TicketStatus.CLOSED,
                    createdAt = 10 * hour,
                    statusHistory = listOf(
                        StatusHistory(TicketStatus.OPEN, 10 * hour),
                        StatusHistory(TicketStatus.RESOLVED, 11 * hour),
                        StatusHistory(TicketStatus.OPEN, 12 * hour),
                        StatusHistory(TicketStatus.RESOLVED, 16 * hour),
                        StatusHistory(TicketStatus.CLOSED, 17 * hour),
                    ),
                ),
                // Unset createdAt: skipped from the average.
                ticket(
                    "no-created-at",
                    status = TicketStatus.RESOLVED,
                    createdAt = 0L,
                    statusHistory = listOf(StatusHistory(TicketStatus.RESOLVED, 5 * hour)),
                ),
                // Resolved before it was created (bad data): skipped from the average.
                ticket(
                    "negative",
                    status = TicketStatus.RESOLVED,
                    createdAt = 20 * hour,
                    statusHistory = listOf(StatusHistory(TicketStatus.RESOLVED, 19 * hour)),
                ),
            ),
        )

        val stats = list.stats()
        assertEquals(6, stats.totalTickets)
        assertEquals(2, stats.activeTickets)
        assertEquals(2, stats.resolvedTickets)
        assertEquals(2.0 / 6.0, stats.activeRatio, 1e-9)
        assertEquals(4 * hour, stats.averageRepairTimeMillis)
    }

    @Test
    fun stats_ignores_repair_time_of_reopened_active_ticket() {
        val hour = 3_600_000L
        val resolved = ticket(
            "resolved",
            status = TicketStatus.RESOLVED,
            createdAt = 10 * hour,
            statusHistory = listOf(
                StatusHistory(TicketStatus.OPEN, 10 * hour),
                StatusHistory(TicketStatus.RESOLVED, 12 * hour),
            ),
        )
        // Reopened after a resolve: not yet repaired, so no repair time.
        val reopened = ticket(
            "reopened",
            status = TicketStatus.OPEN,
            createdAt = 10 * hour,
            statusHistory = listOf(
                StatusHistory(TicketStatus.OPEN, 10 * hour),
                StatusHistory(TicketStatus.RESOLVED, 11 * hour),
                StatusHistory(TicketStatus.OPEN, 12 * hour),
            ),
        )

        val stats = TicketsList(listOf(resolved, reopened)).stats()
        assertEquals(2, stats.totalTickets)
        assertEquals(1, stats.activeTickets)
        assertEquals(1, stats.resolvedTickets)
        assertEquals(2 * hour, stats.averageRepairTimeMillis)
    }

    // add and copies

    @Test
    fun add_rejects_blank_and_duplicate_ids() {
        val list = TicketsList()
        assertFalse(list.add(ticket("")))
        assertTrue(list.add(ticket("a")))
        assertFalse(list.add(ticket("a")))
        assertEquals(1, list.rawList.size)
    }

    @Test
    fun getAllSorted_is_newest_first_then_id_and_a_copy() {
        val list = TicketsList(
            listOf(
                ticket("b", createdAt = 1_000L),
                ticket("a", createdAt = 1_000L),
                ticket("c", createdAt = 2_000L),
            ),
        )
        val sorted = list.getAllSorted()
        assertEquals(listOf("c", "a", "b"), sorted.map { it.id })

        // sortedWith returns a fixed-size list, so write an element instead of clearing it.
        (sorted as MutableList<Ticket>)[0] = ticket("x")
        assertEquals(listOf("c", "a", "b"), list.getAllSorted().map { it.id })
    }

    @Test
    fun rawList_mutation_does_not_change_the_collection() {
        val list = TicketsList(listOf(ticket("a"), ticket("b")))
        (list.rawList as MutableList<Ticket>).clear()
        assertEquals(2, list.rawList.size)
    }

    @Test
    fun rawList_snapshot_does_not_change_after_add() {
        val list = TicketsList(listOf(ticket("a")))
        val before = list.rawList
        list.add(ticket("b"))
        assertEquals(1, before.size)
        assertEquals(2, list.rawList.size)
        assertNotNull(list.findById("b"))
    }

    // haversineMeters

    @Test
    fun one_degree_of_latitude_is_about_111195m() {
        assertEquals(111_195.0, haversineMeters(40.0, -8.0, 41.0, -8.0), 1.0)
    }

    @Test
    fun one_degree_of_longitude_at_60N_is_about_55597m() {
        // cos 60 = 0.5, so half a degree of latitude: exercises the longitude term.
        assertEquals(55_596.9, haversineMeters(60.0, 0.0, 60.0, 1.0), 1.0)
    }
}
