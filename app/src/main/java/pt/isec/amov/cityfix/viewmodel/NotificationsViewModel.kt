package pt.isec.amov.cityfix.viewmodel

import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import pt.isec.amov.cityfix.CityFixApplication
import pt.isec.amov.cityfix.data.TicketCheckWorker
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.User

class NotificationsViewModel(private val app: CityFixApplication) : ViewModel() {

    fun onUserChanged(user: User?) {
        if (user != null) {
            TicketCheckWorker.schedule(app, user.id, user is Operator)
        } else {
            TicketCheckWorker.cancel(app)
        }
    }

    fun onLoggedOut() {
        NotificationManagerCompat.from(app).cancelAll()
    }

    suspend fun watch(user: User) {
        val isOperator = user is Operator
        app.ticketsRepository.ticketsFromServer(user.id, isOperator).collect { list ->
            app.ticketNotifier.check(user.id, isOperator, list, System.currentTimeMillis())
        }
    }
}
