package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.data.TicketsRepository

class ReportViewModelFactory(
    private val repository: TicketsRepository,
    private val attachmentStorage: AttachmentStorage,
    private val locationRepository: LocationRepository,
    private val creatorId: String,
    private val fromImpact: Boolean,
) : ViewModelProvider.Factory {
    // Only ever asked for ReportViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReportViewModel(repository, attachmentStorage, locationRepository, creatorId, fromImpact) as T
}
