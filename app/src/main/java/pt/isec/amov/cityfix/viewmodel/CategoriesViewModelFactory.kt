package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.CategoriesRepository

class CategoriesViewModelFactory(
    private val repository: CategoriesRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for CategoriesViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CategoriesViewModel(repository) as T
}
