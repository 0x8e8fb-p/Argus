package com.nexusblock.cert

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.security.KeyChain
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Handles Luna CA certificate export and installation.
 * The CA cert enables Luna's server to perform MITM for ad-blocking
 * on HTTPS traffic (manifest cleaning for Prime Video SSAI, etc.).
 */
object LunaCertInstaller {

    private const val TAG = "NexusBlock/LunaCert"
    private const val ASSET_PATH = "luna/ca_cert.pem"
    private const val EXPORT_FILENAME = "nexusblock_luna_ca.crt"

    /**
     * Read the bundled Luna CA certificate bytes from assets.
     */
    fun getCertBytes(context: Context): ByteArray {
        return context.assets.open(ASSET_PATH).use { it.readBytes() }
    }

    /**
     * Export the CA cert to the device's Downloads directory so the user
     * can install it via Settings → Security → Install from storage.
     * Returns the exported file path, or null on failure.
     */
    fun exportCertToDownloads(context: Context): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            downloadsDir.mkdirs()
            val outFile = File(downloadsDir, EXPORT_FILENAME)
            FileOutputStream(outFile).use { fos ->
                fos.write(getCertBytes(context))
            }
            Log.i(TAG, "CA cert exported to ${outFile.absolutePath}")
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CA cert", e)
            null
        }
    }

    /**
     * Launch the Android certificate installation intent.
     * On Fire TV / Android TV, the user will be prompted to name and install the cert.
     */
    fun createInstallIntent(context: Context): Intent {
        val certBytes = getCertBytes(context)
        return KeyChain.createInstallIntent().apply {
            putExtra(KeyChain.EXTRA_CERTIFICATE, certBytes)
            putExtra(KeyChain.EXTRA_NAME, "NexusBlock Luna CA")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
