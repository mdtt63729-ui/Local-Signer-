package com.example

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast
import java.io.File

/**
 * Split APK install support — PackageInstaller session diye
 * base + sob split ek sathe ekta install request e pathay.
 * (Single APK install er ACTION_VIEW trick split APK er khetre kaj kore na.)
 */
object SplitInstaller {

    fun installApks(context: Context, apkFiles: List<File>) {
        try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)

            apkFiles.forEach { f ->
                session.openWrite(f.name, 0, f.length()).use { out ->
                    f.inputStream().use { input -> input.copyTo(out) }
                }
            }

            val intent = Intent(context, InstallResultReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pi.intentSender)
            session.close()
        } catch (e: Exception) {
            Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Install session er result receiver — success/failure/user-confirm handle kore.
 * Manifest e register kora ache (exported = false).
 */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // System confirm dialog ta open korte hove
                @Suppress("DEPRECATION")
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                confirmIntent?.let { context.startActivity(it) }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, "App installed successfully", Toast.LENGTH_LONG).show()
            }
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Toast.makeText(context, "Install failed: ${msg ?: "unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
