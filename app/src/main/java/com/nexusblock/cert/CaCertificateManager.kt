package com.nexusblock.cert

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.security.KeyChain
import android.security.KeyChain.EXTRA_CERTIFICATE
import android.security.KeyChain.EXTRA_NAME
import android.util.Log
import com.nexusblock.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automated CA certificate lifecycle manager.
 *
 * Handles installation into Android's user certificate store via KeyChain API,
 * removal on app reset, and tracks installation state persistently.
 *
 * Two paths:
 *  1. No-root: Launch KeyChain.createInstallIntent() → user taps OK on system dialog.
 *     We can use AccessibilityService to auto-click the OK button on Android TV.
 *  2. Rooted: Magisk module installs into /system/etc/security/cacerts/ (Sprint 4).
 *
 * On Android TV, the KeyChain install dialog is DPAD-navigable. Our onboarding
 * wizard guides the user through it; optionally ArgusAccessibilityService can
 * detect the dialog and auto-confirm.
 */
@Singleton
class CaCertificateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val certEngine: com.nexusblock.engine.CertificateManager
) {
    companion object {
        private const val TAG = "Argus/CaManager"
        private const val CA_CERT_FILE = "argus-ca.pem"
        private const val INSTALL_REQUEST_CODE = 2001
        private const val CA_ALIAS_USER = "Argus Ad Blocker CA"
    }

    private val caFile: File by lazy {
        File(context.filesDir, CA_CERT_FILE)
    }

    /**
     * Check if the Argus CA is installed in the user trust store.
     * This queries the system KeyStore for private credentials.
     */
    suspend fun isCaInstalled(): Boolean = withContext(Dispatchers.IO) {
        if (settingsRepo.isSystemCaInstalled) return@withContext true
        try {
            val aliases = KeyChain.getCertificateChain(context, CA_ALIAS_USER)
            aliases != null && aliases.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Launch the system KeyChain install dialog so the user can install
     * the Argus CA certificate. The result is delivered to the Activity
     * via onActivityResult with requestCode INSTALL_REQUEST_CODE.
     *
     * @param activity The activity that will receive the onActivityResult callback.
     * @return true if the install intent was launched successfully.
     */
    fun launchInstallIntent(activity: Activity): Boolean {
        return try {
            kotlinx.coroutines.runBlocking { certEngine.initialize() }
            val rawCert = certEngine.getCaCertificate()?.encoded ?: return false

            // Write cert to app-private file for sharing via FileProvider
            caFile.writeBytes(rawCert)

            // Create PEM format for KeyChain
            val pem = buildString {
                append("-----BEGIN CERTIFICATE-----\n")
                append(android.util.Base64.encodeToString(rawCert, android.util.Base64.DEFAULT))
                append("-----END CERTIFICATE-----\n")
            }
            caFile.writeText(pem)

            val intent = KeyChain.createInstallIntent()
            intent.putExtra(EXTRA_NAME, CA_ALIAS_USER)
            intent.putExtra(EXTRA_CERTIFICATE, rawCert)
            activity.startActivityForResult(intent, INSTALL_REQUEST_CODE)
            Log.i(TAG, "Launched KeyChain CA install intent")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch CA install intent", e)
            false
        }
    }

    /**
     * Handle the result from the KeyChain install dialog.
     * Call this from the host Activity's onActivityResult.
     */
    fun onInstallResult(requestCode: Int, resultCode: Int) {
        if (requestCode != INSTALL_REQUEST_CODE) return
        if (resultCode == Activity.RESULT_OK) {
            settingsRepo.isSystemCaInstalled = true
            Log.i(TAG, "User installed Argus CA certificate")
        } else {
            settingsRepo.isSystemCaInstalled = false
            Log.w(TAG, "User cancelled CA certificate installation")
        }
    }

    /**
     * Remove the Argus CA from the user trust store.
     *
     * On Android, apps cannot directly remove user-installed CAs programmatically.
     * We guide the user to Settings → Security → Trusted Credentials → User tab
     * and ask them to remove "Argus Ad Blocker CA". On rooted devices, the Magisk
     * module handles this automatically.
     *
     * For TV devices, we can use AccessibilityService to navigate the Settings
     * menu tree and perform the removal automatically.
     */
    suspend fun removeCaCertificate(): Boolean = withContext(Dispatchers.IO) {
        settingsRepo.isSystemCaInstalled = false
        try {
            // Best-effort: delete our stored cert file
            if (caFile.exists()) caFile.delete()

            // On Android 14+, try to use DevicePolicyManager if we have device owner
            // For consumer apps, this typically fails — we rely on guided user action
            Log.i(TAG, "CA removal flags reset. User must manually remove from Settings.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "CA removal error", e)
            false
        }
    }

    /**
     * Get the PEM-encoded CA certificate content for sharing or manual install.
     */
    suspend fun getCaPem(): String = withContext(Dispatchers.IO) {
        certEngine.initialize()
        val rawCert = certEngine.getCaCertificate()?.encoded ?: return@withContext ""
        buildString {
            append("-----BEGIN CERTIFICATE-----\n")
            append(android.util.Base64.encodeToString(rawCert, android.util.Base64.DEFAULT))
            append("-----END CERTIFICATE-----\n")
        }
    }

    /**
     * Export CA to Downloads folder so user can sideload it manually via
     * Settings → Security → Install from storage.
     */
    suspend fun exportCaToDownloads(): File? = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = context.getExternalFilesDir(null)
                ?: return@withContext null
            val exportFile = File(downloadsDir, "argus-ca.crt")
            exportFile.writeText(getCaPem())
            Log.i(TAG, "CA exported to ${exportFile.absolutePath}")
            exportFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CA", e)
            null
        }
    }
}
