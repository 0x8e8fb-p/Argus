package com.nexusblock.cert

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Orchestrates CA certificate installation on Android TV using every known
 * method, with detection and a TV-native fallback wizard.
 *
 * Hard truth: Android 11+ (API 30) blocks silent CA installation.
 * This class tries every vector — intents, OEM-specific Settings pages,
 * .p12 export — and falls back to a step-by-step D-pad wizard.
 */
object CertInstallOrchestrator {

    private const val TAG = "NexusBlock/CertInstall"
    private const val LUNA_PEM_ASSET = "luna/ca_cert.pem"
    private const val EXPORT_FILENAME = "nexusblock_luna_ca.crt"
    private const val EXPORT_P12_FILENAME = "nexusblock_luna_ca.p12"
    private const val P12_DUMMY_PASSWORD = "nexusblock"

    /** Known Luna CA SHA-256 fingerprint (computed from luna_ca_cert.pem). */
    private const val LUNA_CA_FINGERPRINT =
        "01:43:B0:31:91:13:92:0D:D6:0A:21:BE:A0:4E:A6:3F:3D:5B:D6:B4:AC:00:C6:DE:6B:22:00:59:B7:1D:CC:C9"

    /** Result of a single install attempt. */
    data class InstallAttempt(
        val methodName: String,
        val available: Boolean,
        val launched: Boolean = false,
        val error: String? = null
    )

    /** High-level result of the orchestration. */
    data class InstallResult(
        val alreadyInstalled: Boolean,
        val attempts: List<InstallAttempt>,
        val bestIntent: Intent? = null,
        val certUri: Uri? = null,
        val error: String? = null
    )

    /**
     * Check whether the Luna CA certificate is already present in the Android
     * trust store (system or user). This queries the "AndroidCAStore" keystore
     * and compares SHA-256 fingerprints.
     */
    suspend fun isLunaCaInstalled(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val keyStore = KeyStore.getInstance("AndroidCAStore").apply { load(null, null) }
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val cert = keyStore.getCertificate(aliases.nextElement()) as? X509Certificate ?: continue
                val fp = sha256Fingerprint(cert).uppercase()
                if (fp == LUNA_CA_FINGERPRINT.uppercase()) {
                    Log.i(TAG, "Luna CA found in trust store")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "Trust store inspection failed", e)
            false
        }
    }

    /**
     * Run the full orchestration:
     * 1. Check if already installed
     * 2. Export cert to accessible URI
     * 3. Try every known install method
     * 4. Return results + best intent for the UI to launch
     */
    suspend fun orchestrate(context: Context): InstallResult = withContext(Dispatchers.IO) {
        val attempts = mutableListOf<InstallAttempt>()

        // 1. Already installed?
        if (isLunaCaInstalled(context)) {
            return@withContext InstallResult(alreadyInstalled = true, attempts = attempts)
        }

        // 2. Export cert to an accessible URI
        val certUri = exportCertToCache(context)
        if (certUri == null) {
            Log.e(TAG, "Failed to export cert to cache")
            return@withContext InstallResult(
                alreadyInstalled = false,
                attempts = attempts,
                error = "Failed to export certificate"
            )
        }

        // 3. Try .crt via MIME intent
        val crtIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(certUri, "application/x-x509-ca-cert")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val crtResolved = resolveIntent(context, crtIntent)
        attempts += InstallAttempt(
            methodName = "crt_mime_intent",
            available = crtResolved != null,
            launched = false
        )

        // 4. Try .p12 via MIME intent (some OEMs handle this better)
        val p12Uri = exportP12ToCache(context)
        val p12Intent = p12Uri?.let { uri ->
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/x-pkcs12")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        }
        val p12Resolved = p12Intent?.let { resolveIntent(context, it) }
        attempts += InstallAttempt(
            methodName = "p12_mime_intent",
            available = p12Resolved != null,
            launched = false
        )

        // 5. OEM-specific Settings intents
        val settingsIntents = buildSettingsIntentList()
        var bestSettingsIntent: Intent? = null
        for (intent in settingsIntents) {
            val resolved = resolveIntent(context, intent)
            val available = resolved != null
            attempts += InstallAttempt(
                methodName = intent.action ?: intent.component?.className ?: "unknown_settings",
                available = available,
                launched = false
            )
            if (available && bestSettingsIntent == null) {
                bestSettingsIntent = intent
            }
        }

        // 6. Pick the best intent to suggest to the user
        val bestIntent = crtResolved ?: p12Resolved ?: bestSettingsIntent

        Log.i(TAG, "Orchestration complete. Best intent: ${bestIntent?.component?.className}")

        InstallResult(
            alreadyInstalled = false,
            attempts = attempts.filter { it.available },
            bestIntent = bestIntent,
            certUri = certUri
        )
    }

    /**
     * Launch the best available install intent.
     * Returns true if an intent was launched.
     */
    fun launchBestIntent(context: Context, result: InstallResult): Boolean {
        val intent = result.bestIntent ?: return false
        try {
            // Grant URI permission if we have a cert URI
            result.certUri?.let { uri ->
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.grantUriPermission(
                    intent.component?.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch install intent", e)
            return false
        }
    }

    /**
     * Open the generic Android Settings security page as a last resort.
     */
    fun openSecuritySettings(context: Context): Boolean {
        return try {
            context.startActivity(
                Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "Security settings intent failed", e)
            false
        }
    }

    // ----------------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------------

    private fun resolveIntent(context: Context, intent: Intent): Intent? {
        val pm = context.packageManager
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, 0)
        }
        return resolveInfo?.let { Intent(intent).setClassName(it.activityInfo.packageName, it.activityInfo.name) }
    }

    private fun buildSettingsIntentList(): List<Intent> {
        return listOf(
            // Stock Android / Google TV
            Intent(Settings.ACTION_SECURITY_SETTINGS),
            // Trusted credentials (some OEMs expose this)
            Intent("com.android.settings.TRUSTED_CREDENTIALS"),
            // Sony Bravia-specific
            Intent().setClassName("com.android.settings", "com.android.settings.Settings\$TrustedCredentialsSettingsActivity"),
            // NVIDIA Shield
            Intent().setClassName("com.android.settings", "com.android.settings.Settings\$SecuritySettingsActivity"),
            // Xiaomi / Mi TV
            Intent().setClassName("com.android.settings", "com.android.settings.security.SecuritySettings"),
            // Samsung (rare on TV but possible)
            Intent().setClassName("com.android.settings", "com.android.settings.security.CredentialStorage"),
            // OnePlus / Oppo TV
            Intent().setClassName("com.android.settings", "com.android.settings.security.TrustedCredentialsSettings"),
            // Generic cert installer activity
            Intent("com.android.credentials.INSTALL"),
        )
    }

    private fun exportCertToCache(context: Context): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "certs").apply { mkdirs() }
            val outFile = File(cacheDir, EXPORT_FILENAME)
            context.assets.open(LUNA_PEM_ASSET).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        } catch (e: Exception) {
            Log.e(TAG, "Export cert failed", e)
            null
        }
    }

    /**
     * Export the cert as a PKCS#12 bundle. Some OEM cert installers prefer .p12.
     * We create a minimal P12 containing only the CA cert with a dummy password.
     */
    private fun exportP12ToCache(context: Context): Uri? {
        return try {
            val pemBytes = context.assets.open(LUNA_PEM_ASSET).use { it.readBytes() }
            val cf = CertificateFactory.getInstance("X.509")
            val cert = cf.generateCertificate(pemBytes.inputStream()) as X509Certificate

            val keyStore = java.security.KeyStore.getInstance("PKCS12")
            keyStore.load(null, null)
            keyStore.setCertificateEntry("luna_ca", cert)

            val cacheDir = File(context.cacheDir, "certs").apply { mkdirs() }
            val outFile = File(cacheDir, EXPORT_P12_FILENAME)
            FileOutputStream(outFile).use { fos ->
                keyStore.store(fos, P12_DUMMY_PASSWORD.toCharArray())
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        } catch (e: Exception) {
            Log.e(TAG, "Export P12 failed", e)
            null
        }
    }

    private fun sha256Fingerprint(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
