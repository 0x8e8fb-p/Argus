package com.nexusblock.cert

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Handles Luna CA certificate export and installation.
 *
 * On Android 11+ (API 30+), CA certificates can ONLY be installed through
 * Settings → Security → Encryption & credentials → Install a certificate → CA certificate.
 * The KeyChain.createInstallIntent() just shows an informational popup.
 *
 * Our strategy:
 * 1. Write the .crt file to Downloads (so it's visible in "Install from storage")
 * 2. Open the credential install settings page directly
 * 3. Toast guides user to select the file
 */
object LunaCertInstaller {

    private const val TAG = "NexusBlock/LunaCert"
    private const val ASSET_PATH = "luna/ca_cert.pem"
    private const val EXPORT_FILENAME = "nexusblock_luna_ca.crt"

    /**
     * Read the bundled Luna CA certificate PEM bytes from assets.
     */
    fun getPemBytes(context: Context): ByteArray {
        return context.assets.open(ASSET_PATH).use { it.readBytes() }
    }

    /**
     * Get DER-encoded certificate bytes by parsing the PEM through CertificateFactory.
     */
    fun getDerBytes(context: Context): ByteArray {
        val pemBytes = getPemBytes(context)
        val cf = CertificateFactory.getInstance("X.509")
        val cert = cf.generateCertificate(pemBytes.inputStream()) as X509Certificate
        return cert.encoded
    }

    /**
     * Main install method.
     *
     * On Android 11+ (API 30+), CA certs CANNOT be installed via any app intent.
     * On many Android TV boxes, Settings intents resolve to wrong pages (Unknown Sources).
     *
     * Our approach:
     * 1. Export cert to Downloads
     * 2. Show clear instructions (do NOT try to open Settings — it goes to wrong page)
     * 3. The cert install is optional — Luna DNS blocking works without it
     */
    fun install(context: Context) {
        // Export cert to Downloads so user can find it
        val uri = exportCertToDownloads(context)
        val exported = uri != null

        if (exported) {
            Log.i(TAG, "Cert saved to Downloads. User must install manually from Settings.")
        } else {
            // Fallback: export to cache
            exportCertToCache(context)
            Log.w(TAG, "Could not export to Downloads, saved to app cache")
        }

        // Show instructions — don't try to open any Settings page
        // (all security intents go to "Unknown Sources" on generic Android TV)
        val message = if (exported) {
            "Certificate saved to Downloads!\n\n" +
            "To install: Settings → Security → Install from storage → " +
            "CA certificate → select \"nexusblock_luna_ca.crt\""
        } else {
            "Certificate export failed. Luna DNS blocking still works without it."
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Export the CA cert to Downloads as DER-encoded .crt.
     * Uses MediaStore on API 29+ for scoped storage compliance.
     */
    fun exportCertToDownloads(context: Context): Uri? {
        return try {
            val derBytes = getDerBytes(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+ (scoped storage)
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, EXPORT_FILENAME)
                    put(MediaStore.Downloads.MIME_TYPE, "application/x-x509-ca-cert")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { it.write(derBytes) }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Log.i(TAG, "CA cert exported via MediaStore: $uri")
                    uri
                } else null
            } else {
                // Legacy external storage for API < 29
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                downloadsDir.mkdirs()
                val outFile = File(downloadsDir, EXPORT_FILENAME)
                FileOutputStream(outFile).use { it.write(derBytes) }
                Log.i(TAG, "CA cert exported to ${outFile.absolutePath}")
                Uri.fromFile(outFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CA cert to Downloads", e)
            null
        }
    }

    /**
     * Export to app cache + FileProvider as fallback.
     */
    fun exportCertToCache(context: Context): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "certs")
            cacheDir.mkdirs()
            val outFile = File(cacheDir, EXPORT_FILENAME)
            FileOutputStream(outFile).use { fos ->
                fos.write(getDerBytes(context))
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CA cert to cache", e)
            null
        }
    }
}
