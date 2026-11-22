package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.CategoriesRepository

class OperatorPanelViewModelFactory(
    private val categories: CategoriesRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for OperatorPanelViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        OperatorPanelViewModel(categories) as T
}
