package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.UsersRepository

class CitizenApprovalViewModelFactory(
    private val citizenId: String,
    private val operatorId: String,
    private val users: UsersRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for CitizenApprovalViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CitizenApprovalViewModel(citizenId, operatorId, users) as T
}
