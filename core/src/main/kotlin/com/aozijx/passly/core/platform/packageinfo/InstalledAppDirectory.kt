package com.aozijx.passly.core.platform.packageinfo

/** Read-only catalog of applications installed and launchable on this device. */
interface InstalledAppDirectory {
    suspend fun metadataFor(packageName: String): InstalledAppMetadata?
    suspend fun launchableApps(): List<InstalledAppMetadata>
}