package com.aozijx.passly.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.aozijx.passly.data.maintenance.proto.RevisionHistoryMaintenance
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.revisionHistoryMaintenanceDataStore: DataStore<RevisionHistoryMaintenance> by dataStore(
    fileName = "revision_history_maintenance.pb",
    serializer = RevisionHistoryMaintenanceSerializer,
)

@Singleton
internal class RevisionHistoryMaintenanceStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun completedGeneration(): Int =
        context.revisionHistoryMaintenanceDataStore.data.first().completedGeneration

    suspend fun markCompleted(generation: Int) {
        context.revisionHistoryMaintenanceDataStore.updateData { current ->
            current.toBuilder().setCompletedGeneration(generation).build()
        }
    }
}
