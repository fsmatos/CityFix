package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketsList
import pt.isec.amov.cityfix.model.User

/**
 * Tickets for the UI, like ContactsViewModel in Contacts_#3: owns the flows below and fills them
 * from [repository] in viewModelScope. Survives rotation as a ViewModel. Create it once above the
 * NavHost (D-006), with TicketsViewModelFactory, so every screen shares the same instance.
 * The repository's reads are live, so the flows follow every change without a refresh.
 * Which tickets are listed depends on the user (see [setUser]); [user] null = anonymous.
 * Starts empty and fills after the first snapshot, so the first frame can show the empty state.
 */
class TicketsViewModel(private val repository: TicketsRepository, user: User?) : ViewModel() {

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())

    /** The tickets this user may see, newest first (anonymous: the public projection only). */
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val _stats = MutableStateFlow(DashboardStats())

    /** Dashboard figures over every ticket, private ones included (decision 0005). */
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    /** The running ticket listener, cancelled when the user changes. */
    private var ticketsJob: Job? = null

    init {
        // The dashboard is the same for every user, so it is listened to once.
        viewModelScope.launch {
            repository.stats().collect { _stats.value = it }
        }
        setUser(user)
    }

    /**
     * Restarts the ticket listener for [user]: null = anonymous. D-009 calls this on sign-in and
     * sign-out. The list is cleared first, so after sign-out an anonymous user never sees the
     * previous user's full tickets while the new listener loads.
     */
    fun setUser(user: User?) {
        ticketsJob?.cancel()
        _tickets.value = emptyList()
        ticketsJob = viewModelScope.launch {
            when (user) {
                null -> repository.publicTickets().collect { _tickets.value = it }

                // combine (beyond class) emits again whenever either flow emits. Own tickets go
                // first: TicketsList keeps the first ticket with an id, so the citizen's full
                // ticket wins over its public copy.
                is Citizen -> combine(
                    repository.participantTickets(user.id),
                    repository.publicTickets(),
                ) { own, public ->
                    TicketsList(own + public).getAllSorted()
                }.collect { _tickets.value = it }

                is Operator -> repository.allTickets().collect { _tickets.value = it }
            }
        }
    }

    /** The ticket with this id in the current list, or null. */
    fun getTicketById(id: String): Ticket? = _tickets.value.firstOrNull { it.id == id }
}
