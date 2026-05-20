package com.nexusblock.engine

import android.content.Context
import android.util.Log
import com.nexusblock.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CertificateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository
) {
    companion object {
        private const val TAG = "NexusBlock/Cert"
        private const val CA_ALIAS = "nexusblock-ca"
        private const val KEYSTORE_FILE = "nexusblock-keystore.bks"
        private const val KEYSTORE_PASSWORD = "nexusblock123"
        private const val CA_CERT_FILE = "nexusblock-ca.crt"
        private const val CA_DN = "CN=NexusBlock CA, O=NexusBlock, L=Internet"
        private const val CERT_VALIDITY_YEARS = 10L
    }

    private val keystoreDir = context.filesDir
    private val keystoreFile = File(keystoreDir, KEYSTORE_FILE)
    private val caCertFile = File(keystoreDir, CA_CERT_FILE)

    private var caKeyPair: java.security.KeyPair? = null
    private var caCertificate: X509Certificate? = null

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (keystoreFile.exists() && caCertFile.exists()) {
                loadExisting()
                Log.i(TAG, "Loaded existing CA certificate")
                true
            } else {
                generateCaCertificate()
                Log.i(TAG, "Generated new CA certificate")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize certificate manager", e)
            false
        }
    }

    fun isCaInstalled(): Boolean {
        return settingsRepo.caInstalled && caCertFile.exists()
    }

    fun getCaCertificateFile(): File = caCertFile

    fun getCaCertificate(): X509Certificate? = caCertificate

    fun getCaKeyPair(): java.security.KeyPair? = caKeyPair

    suspend fun generateLeafCertificate(hostname: String): Pair<java.security.PrivateKey, X509Certificate>? =
        withContext(Dispatchers.IO) {
            try {
                val caCert = caCertificate ?: return@withContext null
                val caKey = caKeyPair?.private ?: return@withContext null

                val keyPairGen = KeyPairGenerator.getInstance("RSA")
                keyPairGen.initialize(2048)
                val keyPair = keyPairGen.generateKeyPair()

                val serial = BigInteger(UUID.randomUUID().toString().replace("-", ""), 16)
                val notBefore = Date(System.currentTimeMillis() - 86400000)
                val notAfter = Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000)

                val subject = X500Name("CN=$hostname, O=NexusBlock")
                val issuer = X500Name(caCert.subjectX500Principal.name)

                val certBuilder = JcaX509v3CertificateBuilder(
                    issuer,
                    serial,
                    notBefore,
                    notAfter,
                    subject,
                    keyPair.public
                )

                // Subject Alternative Name
                val names = GeneralNames(
                    arrayOf(
                        GeneralName(GeneralName.dNSName, hostname),
                        GeneralName(GeneralName.dNSName, "*.$hostname")
                    )
                )
                certBuilder.addExtension(Extension.subjectAlternativeName, false, names)

                val signer = JcaContentSignerBuilder("SHA256withRSA").build(caKey)
                val certHolder = certBuilder.build(signer)
                val cert = JcaX509CertificateConverter().getCertificate(certHolder)

                Pair(keyPair.private, cert)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate leaf certificate for $hostname", e)
                null
            }
        }

    private fun loadExisting() {
        val keyStore = KeyStore.getInstance("BKS", "BC")
        keyStore.load(keystoreFile.inputStream(), KEYSTORE_PASSWORD.toCharArray())

        val key = keyStore.getKey(CA_ALIAS, KEYSTORE_PASSWORD.toCharArray())
        val cert = keyStore.getCertificate(CA_ALIAS) as X509Certificate

        caKeyPair = java.security.KeyPair(cert.publicKey, key as java.security.PrivateKey)
        caCertificate = cert
    }

    private fun generateCaCertificate() {
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(2048)
        val keyPair = keyPairGen.generateKeyPair()

        val serial = BigInteger(UUID.randomUUID().toString().replace("-", ""), 16)
        val notBefore = Date(System.currentTimeMillis() - 86400000)
        val notAfter = Date(System.currentTimeMillis() + CERT_VALIDITY_YEARS * 365 * 24 * 60 * 60 * 1000)

        val subject = X500Name(CA_DN)

        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        // CA basic constraint
        certBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert = JcaX509CertificateConverter().getCertificate(certHolder)

        // Save to keystore
        val keyStore = KeyStore.getInstance("BKS", "BC")
        keyStore.load(null, null)
        keyStore.setKeyEntry(
            CA_ALIAS,
            keyPair.private,
            KEYSTORE_PASSWORD.toCharArray(),
            arrayOf<Certificate>(cert)
        )
        keyStore.store(FileOutputStream(keystoreFile), KEYSTORE_PASSWORD.toCharArray())

        // Save CA cert to file for user installation
        val certPem = buildString {
            append("-----BEGIN CERTIFICATE-----\n")
            append(android.util.Base64.encodeToString(cert.encoded, android.util.Base64.DEFAULT))
            append("-----END CERTIFICATE-----\n")
        }
        caCertFile.writeText(certPem)

        caKeyPair = keyPair
        caCertificate = cert
        settingsRepo.caInstalled = false // user still needs to install system-wide
    }
}
