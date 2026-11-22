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

class TicketsViewModel(private val repository: TicketsRepository, user: User?) : ViewModel() {

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())

    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val _stats = MutableStateFlow(DashboardStats())

    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    private var ticketsJob: Job? = null

    private var appliedUserKey: String? = null

    init {
        // The dashboard is the same for every user, so it is listened to once.
        viewModelScope.launch {
            repository.stats().collect { _stats.value = it }
        }
        setUser(user)
    }

    fun setUser(user: User?) {
        // D-009: CityFixApp calls this again after rotation and on every profile change (for
        // example a new phone). Same role and id = same tickets, so keep the running listener.
        val key = when (user) {
            null -> "anonymous"
            is Citizen -> "citizen:${user.id}"
            is Operator -> "operator:${user.id}"
        }
        if (key == appliedUserKey) return
        appliedUserKey = key
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

    fun getTicketById(id: String): Ticket? = _tickets.value.firstOrNull { it.id == id }
}
