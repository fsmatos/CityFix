package pt.isec.amov.cityfix.data

// DEBUG ONLY. Delete this file and its call in CityFixApplication before delivery (D-015).

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.resolvedAt
import java.util.Date

/**
 * Fills an empty database with [SampleCategories] and [SampleTickets] (F-011 Q5), so the app has
 * categories to report on and a resolved ticket for the repair-time figure before the operator
 * screens exist (D-013). Called from CityFixApplication.onCreate in debug builds only.
 */
object DebugSeed {
    private const val TAG = "DebugSeed"

    /**
     * Seeds once: stats/summary exists after the first seed. Source.SERVER asks the server, not
     * the local cache, so a fresh install on a second device does not seed again. Offline, the
     * read fails and nothing is written.
     */
    fun seedIfEmpty(firestore: FirebaseFirestore) {
        firestore.document(STATS_SUMMARY).get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) writeSeed(firestore)
            }
            .addOnFailureListener { Log.i(TAG, "seed check skipped", it) }
    }

    /** One batch, like the report: categories, tickets with their companions, and the counters. */
    private fun writeSeed(firestore: FirebaseFirestore) {
        val batch = firestore.batch()
        for (category in SampleCategories.all) {
            batch.set(
                firestore.collection(CATEGORIES).document(category.id),
                category.toCategoryDocument(),
            )
        }
        for (ticket in SampleTickets.all) {
            // Real resolvedAt values are server timestamps written by D-013; the samples use the
            // time of their RESOLVED history entry.
            val resolvedAt = ticket.resolvedAt()?.let { mapOf("resolvedAt" to Timestamp(Date(it))) }
                ?: emptyMap()
            batch.set(
                firestore.collection(TICKETS).document(ticket.id),
                ticket.toTicketDocument() + resolvedAt,
            )
            batch.set(
                firestore.collection(TICKET_LOCATIONS).document(ticket.id),
                ticket.toTicketLocationDocument(),
            )
            if (ticket.scope == Scope.PUBLIC) {
                batch.set(
                    firestore.collection(PUBLIC_TICKETS).document(ticket.id),
                    ticket.toPublicTicketDocument() + resolvedAt,
                )
            }
            batch.set(
                firestore.document(STATS_SUMMARY),
                statsIncrements(null, ticket),
                SetOptions.merge(),
            )
        }
        batch.commit()
            .addOnSuccessListener { Log.i(TAG, "sample data written") }
            .addOnFailureListener { Log.w(TAG, "sample data not written", it) }
    }
}
