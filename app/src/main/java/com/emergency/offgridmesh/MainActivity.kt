package com.emergency.offgridmesh

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.emergency.offgridmesh.service.MeshForegroundService
import com.emergency.offgridmesh.ui.screens.MainScreen
import com.emergency.offgridmesh.ui.theme.OffGridMeshTheme
import com.emergency.offgridmesh.ui.viewmodel.MeshViewModel
import com.emergency.offgridmesh.ui.viewmodel.MeshViewModelFactory
import com.emergency.offgridmesh.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MeshViewModel by viewModels {
        MeshViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OffGridMeshTheme {
                var hasPermissions by remember {
                    mutableStateOf(PermissionHelper.hasAllPermissions(this@MainActivity))
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissionsMap ->
                    val allGranted = permissionsMap.values.all { it }
                    hasPermissions = allGranted
                    if (allGranted) {
                        Toast.makeText(
                            this@MainActivity,
                            "All mesh network permissions granted.",
                            Toast.LENGTH_SHORT
                        ).show()
                        MeshForegroundService.startService(this@MainActivity)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Nearby and Location permissions are required for off-grid mesh functionality.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (hasPermissions) {
                        MeshForegroundService.startService(this@MainActivity)
                    } else {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        hasPermissions = hasPermissions,
                        onRequestPermissions = {
                            permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                        }
                    )
                }
            }
        }
    }
}
