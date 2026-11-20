package pt.isec.amov.cityfix.data

import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus

/**
 * Sample tickets near Pr. Republica for the previews (`ShowScreenPrivacyPreview`: a raw ticket with
 * author, follower and exact location). Not written to Firestore. Times are fixed UTC epoch ms.
 */
object SampleTickets {

    /** 2026-10-20 09:00 UTC. */
    private const val SAMPLE_1_CREATED = 1_792_486_800_000L

    /** 2026-10-15 18:30 UTC. */
    private const val SAMPLE_2_CREATED = 1_792_089_000_000L

    /** 2026-10-17 10:00 UTC. */
    private const val SAMPLE_2_IN_PROGRESS = 1_792_231_200_000L

    /** 2026-10-01 08:00 UTC. */
    private const val SAMPLE_3_CREATED = 1_790_841_600_000L

    /** 2026-10-02 08:00 UTC. */
    private const val SAMPLE_3_IN_PROGRESS = 1_790_928_000_000L

    /** 2026-10-04 08:00 UTC: 3 days after creation. */
    private const val SAMPLE_3_RESOLVED = 1_791_100_800_000L

    private val sample1 = Ticket(
        id = "sample-1",
        description = "Pothole in the road near Praca da Republica",
        situation = Situation.DAMAGE,
        scope = Scope.PUBLIC,
        location = Coordinates(40.2100, -8.4180),
        currentStatus = TicketStatus.OPEN,
        createdAt = SAMPLE_1_CREATED,
        categoryId = "pavement",
        categoryName = "Pavement",
        creatorId = "citizen-1",
        messages = listOf(
            Message(
                id = "sample-1-msg-1",
                text = "Still there this morning.",
                sentAt = SAMPLE_1_CREATED + 3_600_000L,
                authorId = "citizen-1",
            ),
        ),
        statusHistory = listOf(
            StatusHistory(TicketStatus.OPEN, SAMPLE_1_CREATED, null, "citizen-1"),
        ),
    )

    private val sample2 = Ticket(
        id = "sample-2",
        description = "Street light out on Rua Alexandre Herculano",
        situation = Situation.BREAKDOWN,
        scope = Scope.PUBLIC,
        location = Coordinates(40.2075, -8.4210),
        currentStatus = TicketStatus.IN_PROGRESS,
        createdAt = SAMPLE_2_CREATED,
        categoryId = "lighting",
        categoryName = "Public lighting",
        creatorId = "citizen-2",
        followerIds = listOf("citizen-3"),
        statusHistory = listOf(
            StatusHistory(TicketStatus.OPEN, SAMPLE_2_CREATED, null, "citizen-2"),
            StatusHistory(TicketStatus.IN_PROGRESS, SAMPLE_2_IN_PROGRESS, null, "operator-1"),
        ),
    )

    private val sample3 = Ticket(
        id = "sample-3",
        description = "Broken bench in Jardim da Sereia",
        situation = Situation.DESTRUCTION,
        scope = Scope.PUBLIC,
        location = Coordinates(40.2110, -8.4225),
        currentStatus = TicketStatus.RESOLVED,
        createdAt = SAMPLE_3_CREATED,
        categoryId = "furniture",
        categoryName = "Urban furniture",
        creatorId = "citizen-1",
        attachments = listOf(
            Attachment(id = "sample-3-photo", file = "", uploadedById = "citizen-1"),
        ),
        statusHistory = listOf(
            StatusHistory(TicketStatus.OPEN, SAMPLE_3_CREATED, null, "citizen-1"),
            StatusHistory(TicketStatus.IN_PROGRESS, SAMPLE_3_IN_PROGRESS, null, "operator-1"),
            StatusHistory(TicketStatus.RESOLVED, SAMPLE_3_RESOLVED, null, "operator-1"),
        ),
    )

    val all: List<Ticket> = listOf(sample1, sample2, sample3)
}
