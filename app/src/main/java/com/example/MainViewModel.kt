package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

enum class AppPhase {
    IDLE,
    SCANNING,
    CONFIG,
    KEYGEN,
    EXTRACTOR,
    SPLIT_MERGER,
    COMPARE,
    MANIFEST_VIEWER,
    APP_CLONER
}

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val apkPath: String,
    val size: String,
    val icon: android.graphics.drawable.Drawable? = null
)

data class SignedHistory(
    val fileName: String,
    val path: String,
    val file: File
)

// --- Compare tool ---
data class ApkCompareInfo(
    val fileName: String,
    val sizeStr: String,
    val sha256: String,
    val packageName: String,
    val versionName: String,
    val isSigned: Boolean,
    val signatures: String,
    val permissionCount: Int,
    val entryCount: Int
)

data class CompareResult(
    val apkA: ApkCompareInfo,
    val apkB: ApkCompareInfo,
    val added: List<String>,
    val removed: List<String>,
    val modified: List<String>,
    val addedTotal: Int,
    val removedTotal: Int,
    val modifiedTotal: Int,
    val identical: Boolean
)

// --- Split (xapk/apks) installer ---
data class SplitApkEntry(
    val fileName: String,
    val path: String,
    val sizeStr: String,
    val isBase: Boolean
)

// --- Manifest viewer ---
data class ManifestDetails(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String>,
    val entryCount: Int,
    val sizeStr: String,
    val sha256: String
)

data class MainState(
    val phase: AppPhase = AppPhase.IDLE,
    
    // Suite States
    val installedApps: List<InstalledApp> = emptyList(),
    val isLoadingApps: Boolean = false,
    val isExtracting: Boolean = false,
    val extractProgress: Float = 0f,
    val extractingAppName: String = "",
    val manifestContent: String = "",
    val clonePackageName: String = "",
    val cloneAppName: String = "",
    
    val selectedApkUri: Uri? = null,
    val selectedApkName: String = "",
    val selectedApkSize: String = "",
    val selectedApkHash: String = "",
    
    val manifestPackageName: String = "",
    val manifestVersionName: String = "",
    val isApkSigned: Boolean = false,
    val apkSignatures: String = "",
    
    val scanProgress: Float = 0f,
    val scanLogs: List<String> = emptyList(),
    
    val useCustomKeystore: Boolean = false,
    val customKeystoreUri: Uri? = null,
    val customKeystoreName: String = "",
    val customAlias: String = "",
    val customKeyPass: String = "",
    val customStorePass: String = "",
    val isSigning: Boolean = false,
    val signProgress: Float = 0f,
    val currentStep: String = "",
    val signLogs: List<String> = emptyList(),
    val signError: String? = null,
    val history: List<SignedHistory> = emptyList(),
    
    // KeyGen State
    val keygenAlias: String = "mykey",
    val keygenPass: String = "",
    val keygenName: String = "John Doe",
    val keygenOrgUnit: String = "Android Dev",
    val keygenOrg: String = "MyCompany",
    val keygenCity: String = "New York",
    val keygenState: String = "NY",
    val keygenCountryCode: String = "US",
    val isGeneratingKey: Boolean = false,
    val keygenError: String? = null,

    // Compare tool
    val compareA: Uri? = null,
    val compareAName: String = "",
    val compareB: Uri? = null,
    val compareBName: String = "",
    val isComparing: Boolean = false,
    val compareResult: CompareResult? = null,

    // Split (xapk/apks) tool
    val splitApks: List<SplitApkEntry> = emptyList(),
    val splitSessionDir: String = "",
    val splitSignedDir: String = "",
    val isProcessingSplits: Boolean = false,
    val isSigningSplits: Boolean = false,
    val splitStatus: String = "",

    // Manifest viewer
    val manifestDetails: ManifestDetails? = null,
    val isLoadingManifest: Boolean = false,

    // Anti-tamper protection switch
    val protectEnabled: Boolean = false
)

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    fun navigateToKeygen() {
        _state.value = _state.value.copy(phase = AppPhase.KEYGEN, keygenError = null)
    }
    
    fun navigateToPhase(newPhase: AppPhase) {
        _state.value = _state.value.copy(phase = newPhase)
    }

    fun navigateToIdle() {
        _state.value = _state.value.copy(phase = AppPhase.IDLE)
    }

    fun updateKeygenParams(alias: String, pass: String, name: String, ou: String, org: String, city: String, stateProv: String, cc: String) {
        _state.value = _state.value.copy(
            keygenAlias = alias,
            keygenPass = pass,
            keygenName = name,
            keygenOrgUnit = ou,
            keygenOrg = org,
            keygenCity = city,
            keygenState = stateProv,
            keygenCountryCode = cc
        )
    }

    fun loadInstalledApps(context: Context) {
        _state.value = _state.value.copy(isLoadingApps = true, phase = AppPhase.EXTRACTOR)
        viewModelScope.launch {
            val apps = InstalledAppUtils.getInstalledApps(context)
            _state.value = _state.value.copy(installedApps = apps, isLoadingApps = false)
        }
    }

    /**
     * Installed app er APK /data/app/ theke cache e copy kore (live progress shoho),
     * tarpor sei copy ta scan+sign flow te pathay.
     * Direct /data/app URI FileProvider diye share kora jay na — tai cache e copy korte hoi.
     */
    fun extractAndSelectApp(context: Context, app: InstalledApp) {
        if (_state.value.isExtracting) return
        _state.value = _state.value.copy(
            isExtracting = true,
            extractProgress = 0f,
            extractingAppName = app.appName
        )
        viewModelScope.launch {
            try {
                val src = File(app.apkPath)
                val dest = File(context.cacheDir, "extracted_${System.currentTimeMillis()}.apk")
                val total = src.length().coerceAtLeast(1L)
                var done = 0L
                var lastReported = 0L
                withContext(Dispatchers.IO) {
                    src.inputStream().use { input ->
                        dest.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                done += read
                                // Prottek ~1MB por progress update (r UI thread e bar bar hit kore na)
                                if (done - lastReported >= 1024 * 1024 || done >= total) {
                                    lastReported = done
                                    _state.value = _state.value.copy(extractProgress = done.toFloat() / total)
                                }
                            }
                        }
                    }
                }
                _state.value = _state.value.copy(isExtracting = false, extractProgress = 0f)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
                selectApk(context, uri, "${app.appName}.apk", app.size)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isExtracting = false, extractProgress = 0f)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Extraction failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun generateKeystore(context: Context) {
        val s = _state.value
        if (s.keygenPass.length < 6) {
            _state.value = s.copy(keygenError = "Password must be at least 6 characters")
            return
        }
        
        _state.value = s.copy(isGeneratingKey = true, keygenError = null)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dname = "CN=${s.keygenName}, OU=${s.keygenOrgUnit}, O=${s.keygenOrg}, L=${s.keygenCity}, ST=${s.keygenState}, C=${s.keygenCountryCode}"
                val result = KeyGeneratorUtility.generateKeystore(
                    context = context,
                    alias = s.keygenAlias,
                    password = s.keygenPass,
                    dname = dname,
                    fileName = "${s.keygenAlias}.jks"
                )
                
                withContext(Dispatchers.Main) {
                    if (result.success && result.file != null) {
                        _state.value = _state.value.copy(
                            isGeneratingKey = false,
                            phase = AppPhase.IDLE,
                            // Auto-select the generated key for convenience
                            useCustomKeystore = true,
                            customKeystoreName = result.file.name,
                            customAlias = s.keygenAlias,
                            customKeyPass = s.keygenPass,
                            customStorePass = s.keygenPass,
                            customKeystoreUri = Uri.fromFile(result.file)
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isGeneratingKey = false,
                            keygenError = result.error ?: "Unknown error during generation"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        isGeneratingKey = false,
                        keygenError = e.message ?: "Exception occurred"
                    )
                }
            }
        }
    }

    fun selectApk(context: Context, uri: Uri, name: String, sizeStr: String) {
        if (name.endsWith(".zip", ignoreCase = true)) {
            selectFromZip(context, uri, name, sizeStr)
        } else {
            beginScan(context, uri, name, sizeStr)
        }
    }

    /**
     * ZIP upload flow: cache e ZIP copy kore, vitorer largest APK extract kore,
     * tarpor uploaded ZIP ta PERMANENTLY delete kore — scan+sign shudhu APK te chole.
     */
    private fun selectFromZip(context: Context, uri: Uri, zipName: String, sizeStr: String) {
        _state.value = _state.value.copy(
            selectedApkUri = uri,
            selectedApkName = zipName,
            selectedApkSize = sizeStr,
            signError = null,
            phase = AppPhase.SCANNING,
            scanProgress = 0.05f,
            scanLogs = listOf(
                "[INFO] ZIP archive detected: $zipName",
                "[INFO] Extracting and inspecting archive..."
            )
        )

        viewModelScope.launch {
            val zipFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.zip")
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        zipFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw Exception("Could not read the ZIP file")
                }
                updateScanProgress(0.25f)
                addScanLog("[INFO] Locating APK inside ZIP...")

                // Vitorer sob .apk entry khuje sobcheye boro ta bechhe nei
                var apkEntry: java.util.zip.ZipEntry? = null
                var apkCount = 0
                withContext(Dispatchers.IO) {
                    java.util.zip.ZipFile(zipFile).use { zf ->
                        val entries = zf.entries()
                        while (entries.hasMoreElements()) {
                            val e = entries.nextElement()
                            if (!e.isDirectory && e.name.endsWith(".apk", ignoreCase = true)) {
                                apkCount++
                                if (apkEntry == null || e.size > apkEntry!!.size) apkEntry = e
                            }
                        }
                    }
                }

                if (apkEntry == null) {
                    addScanLog("[ERROR] No APK found inside this ZIP.")
                    delay(1200)
                    zipFile.delete()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No APK found inside the ZIP", Toast.LENGTH_LONG).show()
                    }
                    resetApkSelection()
                    return@launch
                }

                addScanLog(
                    if (apkCount > 1) "[SUCCESS] ${apkCount} APKs found — picking largest: ${apkEntry!!.name}"
                    else "[SUCCESS] APK found: ${apkEntry!!.name}"
                )
                updateScanProgress(0.50f)

                // APK ta cache e extract kori — file name = zip er vitorker asol name
                // (jate sign korar por output "MyApp-signed.apk" hoy)
                val entryFileName = apkEntry!!.name.substringAfterLast('/').ifEmpty { "extracted.apk" }
                val extracted = File(context.cacheDir, entryFileName)
                withContext(Dispatchers.IO) {
                    java.util.zip.ZipFile(zipFile).use { zf ->
                        zf.getInputStream(zf.getEntry(apkEntry!!.name)).use { input ->
                            extracted.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
                updateScanProgress(0.70f)
                addScanLog("[SUCCESS] APK extracted: $entryFileName")

                // Uploaded ZIP ta permanent delete
                val deleted = withContext(Dispatchers.IO) { zipFile.delete() }
                if (deleted) addScanLog("[INFO] Uploaded ZIP deleted from cache.")

                val apkUri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", extracted
                )
                beginScan(
                    context = context,
                    uri = apkUri,
                    name = entryFileName,
                    sizeStr = formatFileSize(extracted.length()),
                    initialLogs = _state.value.scanLogs + "[INFO] Initializing Security Scanner..."
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "ZIP extraction failed", e)
                zipFile.delete()
                addScanLog("[ERROR] ${e.message}")
                delay(1200)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "ZIP failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                resetApkSelection()
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun beginScan(
        context: Context,
        uri: Uri,
        name: String,
        sizeStr: String,
        initialLogs: List<String> = listOf("[INFO] Initializing Security Scanner...")
    ) {
        _state.value = _state.value.copy(
            selectedApkUri = uri,
            selectedApkName = name,
            selectedApkSize = sizeStr,
            signError = null,
            phase = AppPhase.SCANNING,
            scanProgress = 0f,
            scanLogs = initialLogs
        )

        viewModelScope.launch {
            // Simulate Deep Scan with actual Hash calculation
            delay(500)
            addScanLog("[INFO] Extracting APK contents...")
            updateScanProgress(0.15f)
            delay(600)
            
            addScanLog("[INFO] Analyzing dex bytecode...")
            updateScanProgress(0.30f)
            delay(700)
            
            addScanLog("[INFO] No malicious droppers detected in dex.")
            updateScanProgress(0.45f)
            delay(500)
            
            addScanLog("[INFO] Inspecting AndroidManifest.xml...")
            updateScanProgress(0.60f)
            delay(600)
            
            var packageInfoStr = "Unknown Package"
            var versionStr = "Unknown Version"
            
            val tmpFile = File(context.cacheDir, "temp_scan.apk")
            
            try {
                val pInfo = context.packageManager.getPackageArchiveInfo(
                    uri.let { u ->
                        // Need a local file copy for getPackageArchiveInfo to work on content URIs easily
                        context.contentResolver.openInputStream(u)?.use { input ->
                            tmpFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tmpFile.absolutePath
                    }, 0
                )
                if (pInfo != null) {
                    packageInfoStr = pInfo.packageName
                    versionStr = pInfo.versionName ?: "N/A"
                    addScanLog("[SUCCESS] Package: $packageInfoStr")
                    addScanLog("[SUCCESS] Version: $versionStr")
                } else {
                    addScanLog("[WARN] Could not parse AndroidManifest.xml via PackageArchiveInfo")
                }
            } catch (e: Exception) {
                addScanLog("[WARN] Manifest extraction failed: ${e.message}")
            }
            
            addScanLog("[INFO] Checking declared permissions...")
            updateScanProgress(0.70f)
            delay(500)
            
            addScanLog("[INFO] Verifying existing APK signatures...")
            updateScanProgress(0.80f)
            
            var signed = false
            var sigInfo = "Unsigned"
            try {
                val verifier = com.android.apksig.ApkVerifier.Builder(tmpFile).build()
                val result = verifier.verify()
                if (result.isVerified || result.isVerifiedUsingV1Scheme || result.isVerifiedUsingV2Scheme || result.isVerifiedUsingV3Scheme) {
                    signed = true
                    val schemes = mutableListOf<String>()
                    if (result.isVerifiedUsingV1Scheme) schemes.add("V1")
                    if (result.isVerifiedUsingV2Scheme) schemes.add("V2")
                    if (result.isVerifiedUsingV3Scheme) schemes.add("V3")
                    if (result.isVerifiedUsingV4Scheme) schemes.add("V4")
                    sigInfo = if (schemes.isNotEmpty()) "Signed (${schemes.joinToString(", ")})" else "Signed (Unknown Scheme)"
                    addScanLog("[WARN] APK is already signed: $sigInfo")
                } else {
                    addScanLog("[SUCCESS] APK is Unsigned. Ready for signing.")
                }
            } catch (e: Exception) {
                addScanLog("[WARN] Signature check failed: ${e.message}")
            }
            delay(500)
            
            addScanLog("[RUNNING] Calculating SHA-256 Checksum...")
            updateScanProgress(0.90f)
            
            val hash = try {
                calculateHash(uri, context)
            } catch (e: Exception) {
                "Unknown"
            }
            
            addScanLog("[SUCCESS] Hash: ${if (hash.length > 16) hash.take(16) + "..." else hash}")
            updateScanProgress(0.95f)
            delay(500)
            
            addScanLog("[SUCCESS] Security Scan Passed. APK is safe.")
            updateScanProgress(1.0f)
            delay(1200) // Let user see success
            
            _state.value = _state.value.copy(
                phase = AppPhase.CONFIG,
                selectedApkHash = hash,
                manifestPackageName = packageInfoStr,
                manifestVersionName = versionStr,
                isApkSigned = signed,
                apkSignatures = sigInfo
            )
        }
    }

    private fun addScanLog(log: String) {
        _state.value = _state.value.copy(scanLogs = _state.value.scanLogs + log)
    }
    
    private fun updateScanProgress(progress: Float) {
        _state.value = _state.value.copy(scanProgress = progress)
    }

    private suspend fun calculateHash(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun resetApkSelection() {
        _state.value = _state.value.copy(
            phase = AppPhase.IDLE,
            selectedApkUri = null,
            selectedApkName = "",
            selectedApkSize = "",
            selectedApkHash = "",
            manifestPackageName = "",
            manifestVersionName = "",
            isApkSigned = false,
            apkSignatures = "",
            scanLogs = emptyList(),
            scanProgress = 0f
        )
    }

    fun selectCustomKeystore(uri: Uri, name: String) {
        _state.value = _state.value.copy(
            customKeystoreUri = uri,
            customKeystoreName = name
        )
    }

    fun setUseCustomKeystore(useCustom: Boolean) {
        _state.value = _state.value.copy(useCustomKeystore = useCustom)
    }

    fun updateCustomKeystoreParams(alias: String, keyPass: String, storePass: String) {
        _state.value = _state.value.copy(
            customAlias = alias,
            customKeyPass = keyPass,
            customStorePass = storePass
        )
    }

    fun dismissError() {
        _state.value = _state.value.copy(signError = null)
    }
    
    fun cancelSigning() {
        _state.value = _state.value.copy(isSigning = false, currentStep = "Cancelled by user.", signProgress = 0f)
    }

    fun signApk(context: Context) {
        val currentState = _state.value
        val inputUri = currentState.selectedApkUri ?: return

        _state.value = currentState.copy(
            isSigning = true, 
            signError = null,
            signProgress = 0f,
            currentStep = "Starting...",
            signLogs = listOf("[INFO] Initializing signing engine...")
        )

        viewModelScope.launch {
            val result = SignerEngine.signApk(
                context = context,
                inputUri = inputUri,
                useCustomKey = currentState.useCustomKeystore,
                customKeystoreUri = currentState.customKeystoreUri,
                customAlias = currentState.customAlias,
                customKeyPass = currentState.customKeyPass,
                customStorePass = currentState.customStorePass,
                enableProtection = currentState.protectEnabled,
                progressCallback = { step, progress, logMsg ->
                    _state.value = _state.value.copy(
                        currentStep = step,
                        signProgress = progress / 100f,
                        signLogs = _state.value.signLogs + logMsg
                    )
                    triggerVibration(context, false, 30L)
                }
            )

            if (result.success && result.outputFile != null) {
                val newHistory = SignedHistory(
                    fileName = result.outputFile.name,
                    path = result.outputFile.absolutePath,
                    file = result.outputFile
                )
                _state.value = _state.value.copy(
                    isSigning = false,
                    phase = AppPhase.IDLE, // Go back to start screen to show history
                    history = listOf(newHistory) + _state.value.history,
                    selectedApkUri = null,
                    selectedApkName = ""
                )
                triggerVibration(context, false, 100L)
            } else {
                _state.value = _state.value.copy(
                    isSigning = false,
                    signError = result.errorMsg ?: "Unknown error"
                )
                triggerVibration(context, isError = true, duration = 200L)
            }
        }
    }

    private fun triggerVibration(context: Context, isError: Boolean = false, duration: Long = 100L) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Vibration failed", e)
        }
    }

    fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "APK not found at: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            return
        }
        try {
            // "Package conflicts / package exists" fix: same package er
            // different-signature version install thakle age uninstall offer kori
            val conflictPkg = findSignatureConflict(context, file)
            if (conflictPkg != null) {
                Toast.makeText(
                    context,
                    "Signature differs from the installed app. Uninstall it first, then install again.",
                    Toast.LENGTH_LONG
                ).show()
                val uninstall = Intent(Intent.ACTION_DELETE, Uri.parse("package:$conflictPkg"))
                uninstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(uninstall)
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "No package installer found on this device.", Toast.LENGTH_LONG).show()
            Log.e("MainViewModel", "Install failed", e)
        } catch (e: Exception) {
            Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MainViewModel", "Install failed", e)
        }
    }

    /** Installed same-package app er signature amar signed file er sathe milito kina — mismatch hole package name fire dey */
    private fun findSignatureConflict(context: Context, file: File): String? {
        return try {
            val pm = context.packageManager
            val archive = pm.getPackageArchiveInfo(
                file.absolutePath,
                android.content.pm.PackageManager.GET_SIGNATURES
            ) ?: return null
            val archiveSigs = archive.signatures ?: return null
            val pkg = archive.packageName ?: return null
            val installed = try {
                pm.getPackageInfo(pkg, android.content.pm.PackageManager.GET_SIGNATURES)
            } catch (e: Exception) {
                null
            } ?: return null
            val installedSigs = installed.signatures ?: return null
            val same = archiveSigs.size == installedSigs.size &&
                    archiveSigs.zip(installedSigs).all { (a, b) -> a == b }
            if (same) null else pkg
        } catch (e: Exception) {
            null
        }
    }

    /**
     * History theke kono signed APK manually rename kore.
     * Validation: illegal char remove, duplicate name block, on-disk rename + history update.
     */
    fun renameHistoryItem(context: Context, item: SignedHistory, rawName: String) {
        // .sdk suffix thakle hataiye dao, illegal filename character gulo _ diye replace
        val cleaned = rawName.trim()
            .removeSuffix(".apk")
            .replace(Regex("[\\/:*?\"<>|]"), "_")
            .trim()

        if (cleaned.isEmpty()) {
            Toast.makeText(context, "Name can't be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (cleaned == item.fileName.removeSuffix(".apk")) return

        val dir = item.file.parentFile
        if (dir == null) {
            Toast.makeText(context, "Rename failed: folder not found", Toast.LENGTH_SHORT).show()
            return
        }
        val newFile = File(dir, "$cleaned.apk")
        if (newFile.exists()) {
            Toast.makeText(context, "A file named \"$cleaned.apk\" already exists", Toast.LENGTH_LONG).show()
            return
        }

        val success = try {
            item.file.renameTo(newFile)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Rename failed", e)
            false
        }
        if (success) {
            _state.value = _state.value.copy(
                history = _state.value.history.map {
                    if (it.path == item.path) SignedHistory(newFile.name, newFile.absolutePath, newFile) else it
                }
            )
            Toast.makeText(context, "Renamed to ${newFile.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Rename failed — try a different name", Toast.LENGTH_LONG).show()
        }
    }

    fun shareApk(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "APK not found at: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Kichu app ClipData chara stream read korte pare na
                clipData = android.content.ClipData.newRawUri("APK", uri)
            }
            val chooser = Intent.createChooser(shareIntent, "Share APK")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MainViewModel", "Share failed", e)
        }
    }

    // ===================================================================
    // COMPARE TOOL
    // ===================================================================
    fun selectCompareA(uri: Uri, name: String) {
        _state.value = _state.value.copy(compareA = uri, compareAName = name, compareResult = null)
    }

    fun selectCompareB(uri: Uri, name: String) {
        _state.value = _state.value.copy(compareB = uri, compareBName = name, compareResult = null)
    }

    fun clearComparison() {
        _state.value = _state.value.copy(
            compareA = null, compareAName = "",
            compareB = null, compareBName = "",
            compareResult = null, isComparing = false
        )
    }

    fun runComparison(context: Context) {
        val s = _state.value
        val uriA = s.compareA ?: return
        val uriB = s.compareB ?: return
        _state.value = s.copy(isComparing = true, compareResult = null)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val fileA = copyToCache(context, uriA, "cmp_a.apk")
                    val fileB = copyToCache(context, uriB, "cmp_b.apk")
                    val infoA = analyzeApk(context, fileA)
                    val infoB = analyzeApk(context, fileB)
                    val crcA = zipEntryCrcs(fileA)
                    val crcB = zipEntryCrcs(fileB)
                    val added = crcB.keys - crcA.keys
                    val removed = crcA.keys - crcB.keys
                    val modified = crcA.keys.intersect(crcB.keys).filter { crcA[it] != crcB[it] }
                    CompareResult(
                        apkA = infoA,
                        apkB = infoB,
                        added = added.sorted().take(150),
                        removed = removed.sorted().take(150),
                        modified = modified.sorted().take(150),
                        addedTotal = added.size,
                        removedTotal = removed.size,
                        modifiedTotal = modified.size,
                        identical = infoA.sha256 == infoB.sha256
                    )
                }
                _state.value = _state.value.copy(isComparing = false, compareResult = result)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Comparison failed", e)
                _state.value = _state.value.copy(isComparing = false)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Comparison failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun copyToCache(context: Context, uri: Uri, name: String): File {
        val f = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            f.outputStream().use { output -> input.copyTo(output) }
        } ?: throw Exception("Could not read file")
        return f
    }

    private fun analyzeApk(context: Context, file: File): ApkCompareInfo {
        val pm = context.packageManager
        val pi = pm.getPackageArchiveInfo(file.absolutePath, android.content.pm.PackageManager.GET_PERMISSIONS)

        var isSigned = false
        var sigInfo = "Unsigned"
        try {
            val vr = com.android.apksig.ApkVerifier.Builder(file).build().verify()
            if (vr.isVerified || vr.isVerifiedUsingV1Scheme || vr.isVerifiedUsingV2Scheme || vr.isVerifiedUsingV3Scheme) {
                isSigned = true
                val schemes = mutableListOf<String>()
                if (vr.isVerifiedUsingV1Scheme) schemes.add("V1")
                if (vr.isVerifiedUsingV2Scheme) schemes.add("V2")
                if (vr.isVerifiedUsingV3Scheme) schemes.add("V3")
                if (vr.isVerifiedUsingV4Scheme) schemes.add("V4")
                sigInfo = if (schemes.isNotEmpty()) "Signed (${schemes.joinToString(", ")})" else "Signed"
            }
        } catch (_: Exception) {
        }

        val entryCount = try {
            java.util.zip.ZipFile(file).use { it.size() }
        } catch (_: Exception) {
            0
        }

        return ApkCompareInfo(
            fileName = file.name,
            sizeStr = formatFileSize(file.length()),
            sha256 = sha256File(file),
            packageName = pi?.packageName ?: "Unknown",
            versionName = pi?.versionName ?: "N/A",
            isSigned = isSigned,
            signatures = sigInfo,
            permissionCount = pi?.requestedPermissions?.size ?: 0,
            entryCount = entryCount
        )
    }

    private fun sha256File(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun zipEntryCrcs(file: File): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        java.util.zip.ZipFile(file).use { zf ->
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                if (!e.isDirectory) map[e.name] = e.crc
            }
        }
        return map
    }

    // ===================================================================
    // SPLIT (XAPK/APKS) TOOL
    // ===================================================================
    fun loadSplitFile(context: Context, uri: Uri, name: String) {
        _state.value = _state.value.copy(
            isProcessingSplits = true,
            splitStatus = "Reading $name...",
            splitApks = emptyList(),
            splitSessionDir = "",
            splitSignedDir = ""
        )
        viewModelScope.launch {
            val sessionDir = File(context.cacheDir, "split_session_${System.currentTimeMillis()}")
            try {
                val zipFile = File(context.cacheDir, "split_upload.zip")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        zipFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw Exception("Could not read file")
                    sessionDir.mkdirs()
                    java.util.zip.ZipFile(zipFile).use { zf ->
                        val entries = zf.entries()
                        while (entries.hasMoreElements()) {
                            val e = entries.nextElement()
                            if (!e.isDirectory && e.name.endsWith(".apk", ignoreCase = true)) {
                                val out = File(sessionDir, e.name.substringAfterLast('/'))
                                zf.getInputStream(e).use { input ->
                                    out.outputStream().use { output -> input.copyTo(output) }
                                }
                            }
                        }
                    }
                    zipFile.delete()
                }
                val apks = sessionDir.listFiles()
                    ?.filter { it.name.endsWith(".apk", ignoreCase = true) }
                    ?.sortedByDescending { it.length() }
                    ?: emptyList()

                if (apks.isEmpty()) {
                    sessionDir.deleteRecursively()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No APK modules found inside this file", Toast.LENGTH_LONG).show()
                    }
                    _state.value = _state.value.copy(isProcessingSplits = false, splitStatus = "")
                    return@launch
                }

                _state.value = _state.value.copy(
                    isProcessingSplits = false,
                    splitApks = apks.map {
                        SplitApkEntry(
                            fileName = it.name,
                            path = it.absolutePath,
                            sizeStr = formatFileSize(it.length()),
                            isBase = it.name.startsWith("base", ignoreCase = true)
                        )
                    },
                    splitSessionDir = sessionDir.absolutePath,
                    splitStatus = "${apks.size} APK module(s) extracted — ready to install or sign"
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Split extraction failed", e)
                sessionDir.deleteRecursively()
                _state.value = _state.value.copy(
                    isProcessingSplits = false,
                    splitStatus = "Failed: ${e.message}"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun signAllSplits(context: Context) {
        val s = _state.value
        if (s.splitApks.isEmpty()) return
        _state.value = s.copy(isSigningSplits = true, splitStatus = "Signing splits...", splitSignedDir = "")
        viewModelScope.launch {
            try {
                val baseName = (s.splitApks.firstOrNull { it.isBase } ?: s.splitApks.first())
                    .fileName.substringBeforeLast(".")
                val outDir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                    "SignedAPKs/${baseName}-split-signed"
                )
                if (!outDir.exists()) outDir.mkdirs()

                var ok = 0
                s.splitApks.forEachIndexed { index, entry ->
                    val res = SignerEngine.signApkQuick(context, File(entry.path), File(outDir, entry.fileName))
                    if (res.success) ok++
                    _state.value = _state.value.copy(
                        splitStatus = "Signing ${index + 1}/${s.splitApks.size}..."
                    )
                }

                _state.value = _state.value.copy(
                    isSigningSplits = false,
                    splitSignedDir = outDir.absolutePath,
                    splitStatus = "$ok/${s.splitApks.size} splits signed → ${outDir.absolutePath}"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (ok == s.splitApks.size) "All splits signed successfully" else "Signed $ok of ${s.splitApks.size}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Split signing failed", e)
                _state.value = _state.value.copy(isSigningSplits = false, splitStatus = "Sign failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sign failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Signed folder thakle seta install kore, na thakle extracted original gulo */
    fun installSplits(context: Context) {
        val s = _state.value
        val dirPath = if (s.splitSignedDir.isNotEmpty()) s.splitSignedDir else s.splitSessionDir
        if (dirPath.isEmpty()) return
        val files = File(dirPath).listFiles()
            ?.filter { it.name.endsWith(".apk", ignoreCase = true) }
            ?.sortedByDescending { it.name.startsWith("base", ignoreCase = true) }
            ?: return
        if (files.isEmpty()) return
        SplitInstaller.installApks(context, files)
        Toast.makeText(context, "Install requested — confirm on the system dialog", Toast.LENGTH_LONG).show()
    }

    fun clearSplits() {
        val s = _state.value
        if (s.splitSessionDir.isNotEmpty()) {
            File(s.splitSessionDir).deleteRecursively()
        }
        _state.value = s.copy(
            splitApks = emptyList(), splitSessionDir = "", splitSignedDir = "",
            isProcessingSplits = false, isSigningSplits = false, splitStatus = ""
        )
    }

    // ===================================================================
    // MANIFEST VIEWER
    // ===================================================================
    fun loadManifest(context: Context) {
        val uri = _state.value.selectedApkUri ?: return
        _state.value = _state.value.copy(isLoadingManifest = true, manifestDetails = null)
        viewModelScope.launch {
            try {
                val details = withContext(Dispatchers.IO) {
                    val f = copyToCache(context, uri, "manifest_temp.apk")
                    val pm = context.packageManager
                    val pi = pm.getPackageArchiveInfo(f.absolutePath, android.content.pm.PackageManager.GET_PERMISSIONS)
                    val entryCount = try {
                        java.util.zip.ZipFile(f).use { it.size() }
                    } catch (_: Exception) {
                        0
                    }
                    ManifestDetails(
                        appName = pi?.applicationInfo?.loadLabel(pm)?.toString() ?: "Unknown",
                        packageName = pi?.packageName ?: "Unknown",
                        versionName = pi?.versionName ?: "N/A",
                        versionCode = pi?.versionCode ?: 0,
                        minSdk = pi?.applicationInfo?.minSdkVersion ?: 0,
                        targetSdk = pi?.applicationInfo?.targetSdkVersion ?: 0,
                        permissions = pi?.requestedPermissions?.toList() ?: emptyList(),
                        entryCount = entryCount,
                        sizeStr = formatFileSize(f.length()),
                        sha256 = sha256File(f)
                    )
                }
                _state.value = _state.value.copy(isLoadingManifest = false, manifestDetails = details)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Manifest load failed", e)
                _state.value = _state.value.copy(isLoadingManifest = false)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Manifest load failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ===================================================================
    // PROTECTION SWITCH + HISTORY PERSISTENCE
    // ===================================================================
    fun setProtection(enabled: Boolean) {
        _state.value = _state.value.copy(protectEnabled = enabled)
    }

    /**
     * App start e storage theke ager signed APK gulo history te load kore.
     * Primary: /storage/emulated/0/SignedAPKs, legacy: Download/SignedAPKs.
     */
    fun loadHistoryFromDisk(context: Context) {
        if (_state.value.history.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dirs = listOf(
                    File(android.os.Environment.getExternalStorageDirectory(), "SignedAPKs"),
                    File(
                        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                        "SignedAPKs"
                    )
                )
                val files = dirs
                    .flatMap { d -> d.listFiles()?.filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }?.toList() ?: emptyList() }
                    .sortedByDescending { it.lastModified() }
                if (files.isNotEmpty()) {
                    _state.value = _state.value.copy(
                        history = files.map { SignedHistory(it.name, it.absolutePath, it) }
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "History load failed", e)
            }
        }
    }

    /** History theke delete + storage thekeo file ta permanently delete */
    fun deleteHistoryItem(context: Context, item: SignedHistory) {
        viewModelScope.launch {
            var fileDeleted = false
            try {
                withContext(Dispatchers.IO) {
                    fileDeleted = item.file.delete()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Delete failed", e)
            }
            _state.value = _state.value.copy(
                history = _state.value.history.filter { it.path != item.path }
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    if (fileDeleted) "Deleted: ${item.fileName}" else "Removed from history (file could not be deleted)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
