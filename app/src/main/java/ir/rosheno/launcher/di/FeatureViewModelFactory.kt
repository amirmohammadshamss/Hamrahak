package ir.rosheno.launcher.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.rosheno.launcher.presentation.contacts.ContactsViewModel
import ir.rosheno.launcher.presentation.family.ContactEditorViewModel
import ir.rosheno.launcher.presentation.photos.CameraViewModel
import ir.rosheno.launcher.presentation.photos.PhotoLibraryViewModel

/** Resolve Android dependencies here; feature ViewModels receive only domain ports. */
val FeatureViewModelFactory = viewModelFactory {
    initializer { ContactsViewModel(createSavedStateHandle(), container().phoneContacts) }
    initializer { ContactEditorViewModel(createSavedStateHandle(), container().photos) }
    initializer { PhotoLibraryViewModel(container().captures) }
    initializer { CameraViewModel(container().captures) }
}

private fun CreationExtras.container() = AppContainer(requireNotNull(this[APPLICATION_KEY]))
