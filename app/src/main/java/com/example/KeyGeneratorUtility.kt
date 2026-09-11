package com.example

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.util.Date

data class KeygenResult(val success: Boolean, val file: File? = null, val error: String? = null)

object KeyGeneratorUtility {

    suspend fun generateKeystore(
        context: Context,
        alias: String,
        password: String,
        dname: String,
        fileName: String
    ): KeygenResult = withContext(Dispatchers.IO) {
        try {
            // Setup BouncyCastle
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.addProvider(BouncyCastleProvider())

            // 1. Generate RSA Key Pair (2048 bits)
            val kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
            kpg.initialize(2048, SecureRandom())
            val keyPair = kpg.generateKeyPair()

            // 2. Build Self-Signed Certificate
            val issuer = X500Name(dname)
            val serial = BigInteger.valueOf(System.currentTimeMillis())
            val notBefore = Date()
            val notAfter = Date(System.currentTimeMillis() + 25L * 365L * 24L * 60L * 60L * 1000L) // Valid for 25 years

            val certBuilder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                issuer,
                keyPair.public
            )

            // Sign the certificate
            val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.private)
            val certHolder = certBuilder.build(signer)
            val cert = JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certHolder)

            // 3. Create JKS Keystore
            val keyStore = KeyStore.getInstance("JKS")
            keyStore.load(null, null)

            // Set the Key Entry with the private key and certificate chain
            val chain = arrayOf<Certificate>(cert)
            keyStore.setKeyEntry(alias, keyPair.private, password.toCharArray(), chain)

            // 4. Save to Download Directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val keystoresDir = File(downloadsDir, "Keystores")
            if (!keystoresDir.exists()) keystoresDir.mkdirs()
            
            val outputFile = File(keystoresDir, fileName)
            FileOutputStream(outputFile).use { fos ->
                keyStore.store(fos, password.toCharArray())
            }

            Log.d("KeyGen", "Keystore generated successfully at ${outputFile.absolutePath}")
            KeygenResult(success = true, file = outputFile)
        } catch (e: Exception) {
            Log.e("KeyGen", "Failed to generate keystore", e)
            KeygenResult(success = false, error = e.localizedMessage)
        }
    }
}
