package pt.isec.amov.cityfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Builds [ImpactViewModel] with a Context for the SensorManager, like the other factories. */
class ImpactViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    // Only ever asked for ImpactViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ImpactViewModel(context) as T
}
