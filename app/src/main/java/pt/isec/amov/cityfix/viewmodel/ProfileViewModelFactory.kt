package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.UsersRepository

class ProfileViewModelFactory(
    private val uid: String,
    private val users: UsersRepository,
    private val auth: AuthRepository,
    private val attachmentStorage: AttachmentStorage,
) : ViewModelProvider.Factory {
    // Only ever asked for ProfileViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        ProfileViewModel(uid, users, auth, attachmentStorage, extras.createSavedStateHandle()) as T
}
