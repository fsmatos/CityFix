package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Ticket

/**
 * Phase-1 tickets for the UI, like ContactsViewModel in Contacts_#3: owns the flows below and fills
 * them from [repository] in viewModelScope. Survives rotation as a ViewModel. Create it once above
 * the NavHost (D-006), with TicketsViewModelFactory, so every screen shares the same instance.
 * Phase 1 has no login, so everything exposed here is the anonymous view (the repository projects it).
 * Starts empty and fills after the first load, so the first frame can show the empty state.
 */
class TicketsViewModel(private val repository: TicketsRepository) : ViewModel() {

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())

    /** Public active tickets, newest first, projected with toAnonymousView(). */
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val _stats = MutableStateFlow(DashboardStats())

    /** Dashboard figures over every ticket, private ones included (decision 0005). */
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    init {
        refresh()
    }

    /** Reloads tickets and stats, e.g. after a report was saved. */
    fun refresh() {
        viewModelScope.launch {
            _tickets.value = repository.loadPublicTickets()
            _stats.value = repository.loadStats()
        }
    }

    /** The visible (projected) ticket with this id, or null: unknown, private, resolved or closed. */
    fun getTicketById(id: String): Ticket? = _tickets.value.firstOrNull { it.id == id }
}
