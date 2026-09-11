package com.example

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

object InstalledAppUtils {
    suspend fun getInstalledApps(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<InstalledApp>()
        
        for (app in apps) {
            // Filter out system apps if possible, or show all
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                val appName = pm.getApplicationLabel(app).toString()
                val packageName = app.packageName
                val apkPath = app.sourceDir
                val file = File(apkPath)
                val size = if (file.exists()) {
                    formatFileSize(file.length())
                } else {
                    "Unknown"
                }
                
                // For performance, we can skip loading icons until UI needs them or load them lazily
                // But for now let's just get the icon
                val icon = pm.getApplicationIcon(app)
                
                result.add(InstalledApp(appName, packageName, apkPath, size, icon))
            }
        }
        result.sortedBy { it.appName }
    }
    
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
