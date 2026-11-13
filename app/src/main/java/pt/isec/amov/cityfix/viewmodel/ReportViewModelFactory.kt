package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.TicketsRepository

/**
 * Builds [ReportViewModel] with the app's repository, attachment storage and the reporter's id, like
 * EditContactViewModelFactory in Contacts_#3.
 */
class ReportViewModelFactory(
    private val repository: TicketsRepository,
    private val attachmentStorage: AttachmentStorage,
    private val creatorId: String,
) : ViewModelProvider.Factory {
    // Only ever asked for ReportViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReportViewModel(repository, attachmentStorage, creatorId) as T
}
