package com.emergency.offgridmesh.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.UUID

object DeviceIdProvider {

    private const val PREFS_NAME = "offgrid_mesh_prefs"
    private const val KEY_DEVICE_ID = "pref_device_id"
    private const val KEY_NODE_ALIAS = "pref_node_alias"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getDeviceId(context: Context): String {
        val prefs = getPrefs(context)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            val shortUuid = UUID.randomUUID().toString().substring(0, 8).uppercase()
            deviceId = "NODE-$shortUuid"
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    fun getNodeAlias(context: Context): String {
        val prefs = getPrefs(context)
        var alias = prefs.getString(KEY_NODE_ALIAS, null)
        if (alias.isNullOrBlank()) {
            val deviceModel = Build.MODEL.replace("\\s+".toRegex(), "_").take(10)
            val id = getDeviceId(context)
            alias = "$deviceModel-$id"
            prefs.edit().putString(KEY_NODE_ALIAS, alias).apply()
        }
        return alias
    }

    fun setNodeAlias(context: Context, newAlias: String) {
        getPrefs(context).edit().putString(KEY_NODE_ALIAS, newAlias.trim()).apply()
    }
}
