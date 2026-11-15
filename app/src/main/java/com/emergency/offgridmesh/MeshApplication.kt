package com.emergency.offgridmesh

import android.app.Application
import com.emergency.offgridmesh.alert.EmergencyAlertManager
import com.emergency.offgridmesh.cloud.CloudUplinkSyncManager
import com.emergency.offgridmesh.data.local.AppDatabase
import com.emergency.offgridmesh.data.repository.MeshRepository
import com.emergency.offgridmesh.location.LocationProvider
import com.emergency.offgridmesh.mesh.MultiHopRouter

class MeshApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: MeshRepository
        private set

    lateinit var locationProvider: LocationProvider
        private set

    lateinit var alertManager: EmergencyAlertManager
        private set

    lateinit var cloudSyncManager: CloudUplinkSyncManager
        private set

    lateinit var meshRouter: MultiHopRouter
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        repository = MeshRepository(database)
        locationProvider = LocationProvider(this)
        alertManager = EmergencyAlertManager(this)
        cloudSyncManager = CloudUplinkSyncManager(this, database)
        meshRouter = MultiHopRouter(this, repository, locationProvider, alertManager)
    }
}
