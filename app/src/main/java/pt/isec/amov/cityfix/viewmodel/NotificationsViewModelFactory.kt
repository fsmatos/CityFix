package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.CityFixApplication

/** Builds [NotificationsViewModel] with the app container (Context, PushTokens, TicketNotifier). */
class NotificationsViewModelFactory(
    private val app: CityFixApplication,
) : ViewModelProvider.Factory {
    // Only ever asked for NotificationsViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NotificationsViewModel(app) as T
}
