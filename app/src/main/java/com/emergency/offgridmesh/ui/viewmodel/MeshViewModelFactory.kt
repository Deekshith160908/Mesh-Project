package com.emergency.offgridmesh.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.emergency.offgridmesh.MeshApplication

class MeshViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeshViewModel::class.java)) {
            val app = context.applicationContext as MeshApplication
            return MeshViewModel(
                context = app,
                repository = app.repository,
                locationProvider = app.locationProvider,
                alertManager = app.alertManager,
                cloudSyncManager = app.cloudSyncManager,
                meshRouter = app.meshRouter
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
