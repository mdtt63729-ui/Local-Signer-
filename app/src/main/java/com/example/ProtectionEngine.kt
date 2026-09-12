package com.example

import android.content.Context
import com.android.tools.smali.baksmali.Baksmali
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.smali.Smali
import com.android.tools.smali.smali.SmaliOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Anti-tamper protection engine.
 *
 * Ki kore kaj kore:
 *  1. APK er binary AndroidManifest.xml parse kore Application class / launcher
 *     activity khuje ber kore (nijei likha mini AXML parser — kono external
 *     dependency chara).
 *  2. Jei dex e entry class ache seta baksmali diye smali te disassemble kore.
 *  3. Entry class er attachBaseContext/onCreate er shurute ekta signature
 *     guard call inject kore + notun ekta TamperGuard class add kore.
 *  4. Puro dex abar smali diye reassemble kore (baksmali/smali round-trip
 *     battle-tested).
 *  5. APK zip rebuild kore — purano signature META-INF baad diye, STORED
 *     entry gulo (resources.arsc / .so) alignment thik rekhe.
 *
 * Runtime e guard ki kore: app install howar por Application/Activity shuru
 * hobar AGEI nijer signing certificate er SHA/Base64 check kore. Keu jodi
 * decompile kore kichu change kore (icon, code, resource — ja-i hok) abar
 * recompile kore nijer key diye re-sign kore, certificate mile na → app
 * immediate crash (Process.killProcess + System.exit). Apnar keystore er
 * key chara keu same certificate banate parbe na — tai protected app
 * tamper korle 100% crash.
 */
object ProtectionEngine {

    data class ProtectResult(val success: Boolean, val outputFile: File?, val message: String? = null)

    private const val GUARD_DESCRIPTOR = "Lcom/localapksigner/TamperGuard;"
    private const val GUARD_INVOKE =
        "    invoke-static {p0}, Lcom/localapksigner/TamperGuard;->guard(Landroid/content/Context;)V"

    // ===================================================================
    // MAIN PIPELINE
    // ===================================================================
    suspend fun protect(
        context: Context,
        inputApk: File,
        expectedCertBase64: String,
        progress: (String) -> Unit
    ): ProtectResult = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "protect_${System.currentTimeMillis()}")
        try {
            workDir.mkdirs()

            // 1. Manifest theke entry point ber kori
            progress("Analyzing manifest...")
            val manifestBytes = ZipFile(inputApk).use { zf ->
                zf.getEntry("AndroidManifest.xml")?.let { zf.getInputStream(it).readBytes() }
            } ?: throw Exception("AndroidManifest.xml not found")
            val manifest = parseManifest(manifestBytes)

            val entryRaw = manifest.applicationClass ?: manifest.launcherActivity
                ?: throw Exception("No application class or launcher activity found")
            val entryDescriptor = "L" + entryRaw.replace('.', '/') + ";"

            // 2. Entry class kotay ache seta ber kori (kon dex e)
            progress("Locating entry class...")
            val dexEntries = listDexEntries(inputApk)
            if (dexEntries.isEmpty()) throw Exception("No dex file found in APK")

            var targetDexName: String? = null
            var targetDex: DexBackedDexFile? = null
            for ((name, bytes) in dexEntries) {
                val dex = try {
                    DexBackedDexFile(Opcodes.getDefault(), bytes)
                } catch (e: Exception) {
                    continue
                }
                val found = dex.classes.any { it.type == entryDescriptor }
                if (found) {
                    targetDexName = name
                    targetDex = dex
                    break
                }
            }
            if (targetDex == null || targetDexName == null) {
                throw Exception("Entry class not found in any dex file")
            }

            // 3. Seta disassemble kori
            progress("Disassembling dex ($targetDexName)...")
            val smaliDir = File(workDir, "smali")
            smaliDir.mkdirs()
            val jobs = maxOf(1, minOf(4, Runtime.getRuntime().availableProcessors() - 1))
            val allClassTypes = targetDex.classes.map { it.type }.toList()
            val disassembled = try {
                Baksmali.disassembleDexFile(
                    /* dexFile = */ targetDex,
                    /* outputDir = */ smaliDir,
                    /* jobs = */ jobs,
                    /* options = */ BaksmaliOptions().apply { localsDirective = true },
                    /* classes = */ allClassTypes
                )
            } catch (e: Exception) {
                throw Exception("Disassembly failed: ${e.message}")
            }
            if (!disassembled) throw Exception("Disassembly failed")

            // 4. Entry class patch + guard class add
            progress("Injecting tamper guard...")
            val entrySmali = File(smaliDir, descriptorToPath(entryDescriptor))
            if (!entrySmali.exists()) throw Exception("Entry class smali not generated")
            val isApplication = manifest.applicationClass != null
            val patched = patchEntryClass(entrySmali, isApplication)
            if (!patched) throw Exception("No onCreate/attachBaseContext found to patch")
            writeGuardClass(smaliDir, expectedCertBase64)

            // 5. Dex abar assemble kori
            progress("Reassembling dex...")
            val newDexFile = File(workDir, "new_classes.dex")
            val (assembled, assembleLog) = captureOutput {
                Smali.assemble(
                    SmaliOptions().apply {
                        this.jobs = jobs
                        this.outputDexFile = newDexFile.absolutePath
                    },
                    listOf(smaliDir.absolutePath)
                )
            }
            if (!assembled) throw Exception("Smali assembly failed${if (assembleLog.isNotBlank()) ": $assembleLog" else ""}")

            // 6. APK rebuild — patched dex + alignment soho
            progress("Rebuilding APK...")
            val outApk = File(workDir, "protected-unsigned.apk")
            rebuildApk(inputApk, outApk, targetDexName, newDexFile.readBytes())

            // smali temp clean
            smaliDir.deleteRecursively()

            ProtectResult(true, outApk)
        } catch (e: OutOfMemoryError) {
            ProtectResult(false, null, "APK is too large to protect (out of memory)")
        } catch (e: Exception) {
            ProtectResult(false, null, e.message ?: "Unknown protection error")
        }
    }

    // ===================================================================
    // ENTRY CLASS PATCHING
    // ===================================================================

    /** descriptor "Lcom/foo/Bar;" → relative path "com/foo/Bar.smali" */
    private fun descriptorToPath(descriptor: String): String =
        descriptor.trimStart('L').trimEnd(';') + ".smali"

    /**
     * Entry class er attachBaseContext (application er khetre) ba onCreate
     * (activity er khetre) er shuru te guard call inject kore.
     */
    private fun patchEntryClass(smaliFile: File, isApplication: Boolean): Boolean {
        val lines = smaliFile.readLines().toMutableList()

        // Application hole attachBaseContext age try kori (sobcheye age chole),
        // tarpor onCreate. Activity hole direct onCreate.
        val patterns = if (isApplication) {
            listOf(
                Regex("^\\.method (?:public|protected) attachBaseContext\\(Landroid/content/Context;\\)V$"),
                Regex("^\\.method (?:public|protected) onCreate\\(Landroid/os/Bundle;\\)V$")
            )
        } else {
            listOf(
                Regex("^\\.method (?:public|protected) onCreate\\(Landroid/os/Bundle;\\)V$")
            )
        }

        for (pattern in patterns) {
            val methodIdx = lines.indexOfFirst { pattern.matches(it.trim()) }
            if (methodIdx == -1) continue

            // .locals line khuje ber kori — invoke ta oi khane insert korbo
            var localsIdx = -1
            var i = methodIdx + 1
            while (i < lines.size && !lines[i].startsWith(".end method")) {
                if (lines[i].startsWith(".locals ")) {
                    localsIdx = i
                    break
                }
                i++
            }
            if (localsIdx == -1) continue

            lines.add(localsIdx + 1, GUARD_INVOKE)
            smaliFile.writeText(lines.joinToString("\n") + "\n")
            return true
        }
        return false
    }

    /**
     * TamperGuard.smali — runtime signature verification.
     * Expected certificate (Base64) embed kora thake; mismatch hole app kill.
     * Exception holeo (fail-closed) kill — tamper detection bypass korte
     * parbe na.
     */
    private fun writeGuardClass(smaliDir: File, expectedCertBase64: String) {
        val guardSmali = """
.class public Lcom/localapksigner/TamperGuard;
.super Ljava/lang/Object


.method public static guard(Landroid/content/Context;)V
    .locals 3

    :try_start_0
    invoke-virtual {p0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;
    move-result-object v0

    invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
    move-result-object v1

    const/16 v2, 0x40

    invoke-virtual {v0, v1, v2}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;
    move-result-object v0

    iget-object v0, v0, Landroid/content/pm/PackageInfo;->signatures:[Landroid/content/pm/Signature;

    const/4 v1, 0x0

    aget-object v0, v0, v1

    invoke-virtual {v0}, Landroid/content/pm/Signature;->toByteArray()[B
    move-result-object v0

    const/4 v1, 0x2

    invoke-static {v0, v1}, Landroid/util/Base64;->encodeToString([BI)Ljava/lang/String;
    move-result-object v0

    const-string v1, "__EXPECTED_CERT__"

    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v0

    if-eqz v0, :kill

    return-void

    :kill
    invoke-static {}, Landroid/os/Process;->myPid()I
    move-result v0

    invoke-static {v0}, Landroid/os/Process;->killProcess(I)V

    const/4 v0, 0x0

    invoke-static {v0}, Ljava/lang/System;->exit(I)V

    return-void

    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    move-exception v0

    invoke-static {}, Landroid/os/Process;->myPid()I
    move-result v1

    invoke-static {v1}, Landroid/os/Process;->killProcess(I)V

    return-void
.end method
""".trimIndent().replace("__EXPECTED_CERT__", expectedCertBase64)

        val guardFile = File(smaliDir, "com/localapksigner/TamperGuard.smali")
        guardFile.parentFile?.mkdirs()
        guardFile.writeText(guardSmali)
    }

    // ===================================================================
    // APK REBUILD (alignment-safe zip writer)
    // ===================================================================

    private class CountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var count = 0L
        override fun write(b: Int) {
            out.write(b)
            count++
        }
        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            count += len
        }
    }

    /**
     * APK zip rebuild: patched dex replace, purano META-INF signature delete,
     * ar STORED entry (resources.arsc/.so) gulo er alignment thik rakhha —
     * na korle Android 11+ e install fail korte pare.
     */
    private fun rebuildApk(inputApk: File, outputApk: File, dexToReplace: String, newDexBytes: ByteArray) {
        ZipFile(inputApk).use { zf ->
            val counting = CountingOutputStream(FileOutputStream(outputApk))
            ZipOutputStream(counting).use { zos ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    // Purano signature baad — notun sign hobe
                    if (e.name.startsWith("META-INF/")) continue

                    val zipEntry = ZipEntry(e)
                    if (e.method == ZipEntry.STORED) {
                        // Alignment: .so → 16384 (16KB page), baki STORED → 4 byte
                        val align = if (e.name.endsWith(".so")) 16384 else 4
                        val nameLen = e.name.toByteArray(StandardCharsets.UTF_8).size
                        val headerEnd = counting.count + 30L + nameLen
                        val pad = ((align - (headerEnd % align)) % align).toInt()
                        if (pad > 0) zipEntry.extra = ByteArray(pad)
                    }
                    zos.putNextEntry(zipEntry)
                    if (e.name == dexToReplace) {
                        zos.write(newDexBytes)
                    } else {
                        zf.getInputStream(e).copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    // ===================================================================
    // HELPERS
    // ===================================================================

    private fun listDexEntries(apk: File): List<Pair<String, ByteArray>> {
        val result = mutableListOf<Pair<String, ByteArray>>()
        ZipFile(apk).use { zf ->
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                if (!e.isDirectory && e.name.endsWith(".dex")) {
                    result.add(e.name to zf.getInputStream(e).readBytes())
                }
            }
        }
        // classes.dex always age rakhi (entry class shadharonoto shetay thake)
        return result.sortedByDescending { it.first == "classes.dex" }
    }

    /** stdout/stderr capture — smali assembly er error message pete lage */
    private fun <T> captureOutput(block: () -> T): Pair<T, String> {
        val originalOut = System.out
        val originalErr = System.err
        val captured = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(captured))
            System.setErr(PrintStream(captured))
            val result = block()
            return result to captured.toString("UTF-8").trim()
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }

    // ===================================================================
    // MINI AXML PARSER — binary AndroidManifest.xml er jonno
    // ===================================================================

    private data class ManifestInfo(val applicationClass: String?, val launcherActivity: String?)

    private fun parseManifest(data: ByteArray): ManifestInfo {
        try {
            // File header (8 bytes) skip kore string pool read kori
            val strings = readStringPool(data) ?: return ManifestInfo(null, null)

            val androidNs = strings.indexOf("http://schemas.android.com/apk/res/android")

            var pkg: String? = null
            var appClassRaw: String? = null
            var launcherRaw: String? = null

            var currentActivity: String? = null
            var inIntentFilter = false
            var hasMainAction = false
            var hasLauncherCategory = false

            var offset = 8
            val end = data.size
            while (offset + 8 <= end) {
                val type = readU16(data, offset)
                val size = readU32(data, offset + 4).toInt()
                if (size <= 0) break

                when (type) {
                    0x0102 -> { // START_ELEMENT
                        if (offset + 36 <= end) {
                            val nameIdx = readU32(data, offset + 20).toInt()
                            val attrStart = readU16(data, offset + 24)
                            val attrSize = readU16(data, offset + 26)
                            val attrCount = readU16(data, offset + 28)
                            val attrsAbs = offset + 16 + attrStart

                            fun androidAttr(attrName: String): String? {
                                for (a in 0 until attrCount) {
                                    val base = attrsAbs + a * attrSize
                                    val aNs = readU32(data, base).toInt()
                                    val aName = readU32(data, base + 4).toInt()
                                    if (aNs == androidNs && aName in strings.indices && strings[aName] == attrName) {
                                        val raw = readU32(data, base + 8).toInt()
                                        if (raw != -1 && raw in strings.indices) return strings[raw]
                                        val dataType = data[base + 15].toInt() and 0xFF
                                        val dataVal = readU32(data, base + 16)
                                        if (dataType == 0x03 && dataVal < strings.size) return strings[dataVal.toInt()]
                                        return dataVal.toString()
                                    }
                                }
                                return null
                            }

                            val elementName = if (nameIdx in strings.indices) strings[nameIdx] else ""
                            when (elementName) {
                                "manifest" -> {
                                    // package attribute (no namespace)
                                    for (a in 0 until attrCount) {
                                        val base = attrsAbs + a * attrSize
                                        val aNs = readU32(data, base).toInt()
                                        val aName = readU32(data, base + 4).toInt()
                                        if (aNs == -1 && aName in strings.indices && strings[aName] == "package") {
                                            val raw = readU32(data, base + 8).toInt()
                                            if (raw != -1 && raw in strings.indices) pkg = strings[raw]
                                        }
                                    }
                                }
                                "application" -> appClassRaw = androidAttr("name")
                                "activity", "activity-alias" -> currentActivity = androidAttr("name")
                                "intent-filter" -> {
                                    inIntentFilter = true
                                    hasMainAction = false
                                    hasLauncherCategory = false
                                }
                                "action" -> if (inIntentFilter && androidAttr("name") == "android.intent.action.MAIN") {
                                    hasMainAction = true
                                }
                                "category" -> if (inIntentFilter && androidAttr("name") == "android.intent.category.LAUNCHER") {
                                    hasLauncherCategory = true
                                }
                            }
                        }
                    }
                    0x0103 -> { // END_ELEMENT
                        val nameIdx = readU32(data, offset + 20).toInt()
                        val elementName = if (nameIdx in strings.indices) strings[nameIdx] else ""
                        when (elementName) {
                            "intent-filter" -> {
                                if (hasMainAction && hasLauncherCategory && currentActivity != null && launcherRaw == null) {
                                    launcherRaw = currentActivity
                                }
                                inIntentFilter = false
                            }
                            "activity", "activity-alias" -> currentActivity = null
                        }
                    }
                }
                offset += size
            }

            val resolvedApp = appClassRaw?.let { resolveClassName(it, pkg) }
            val resolvedLauncher = launcherRaw?.let { resolveClassName(it, pkg) }
            return ManifestInfo(resolvedApp, resolvedLauncher)
        } catch (e: Exception) {
            return ManifestInfo(null, null)
        }
    }

    /** ".MainActivity" / "MainActivity" / "com.foo.Main" → full class name */
    private fun resolveClassName(raw: String, pkg: String?): String? {
        if (raw.isEmpty()) return null
        return when {
            raw.startsWith(".") -> pkg?.let { it + raw } ?: raw
            raw.contains('.') -> raw
            else -> pkg?.let { "$it.$raw" } ?: raw
        }
    }

    private fun readStringPool(data: ByteArray): List<String>? {
        if (data.size < 36) return null
        var offset = 8
        // Prothom chunk ta string pool e hote hobe
        if (readU16(data, offset) != 0x0001) return null
        val headerSize = readU16(data, offset + 2)
        val stringCount = readU32(data, offset + 8).toInt()
        val flags = readU32(data, offset + 16).toInt()
        val stringsStart = readU32(data, offset + 20).toInt()
        val isUtf8 = (flags and 0x100) != 0
        if (stringCount <= 0 || stringCount > 200000) return null

        val result = ArrayList<String>(stringCount)
        for (i in 0 until stringCount) {
            val off = readU32(data, offset + headerSize + i * 4).toInt()
            var pos = offset + stringsStart + off
            if (pos < 0 || pos >= data.size) {
                result.add("")
                continue
            }
            result.add(
                if (isUtf8) readUtf8String(data, pos)
                else readUtf16String(data, pos)
            )
        }
        return result
    }

    private fun readUtf8String(data: ByteArray, pos: Int): String {
        var p = pos
        val (charCount, p1) = readU8Len(data, p)
        p = p1
        val (byteLen, p2) = readU8Len(data, p)
        p = p2
        val len = byteLen.coerceIn(0, data.size - p)
        val bytes = data.copyOfRange(p, p + len)
        // MUTF-8 — ASCII er jonno plain UTF-8 e thik
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun readU8Len(data: ByteArray, pos: Int): Pair<Int, Int> {
        if (pos >= data.size) return 0 to pos
        val b0 = data[pos].toInt() and 0xFF
        return if (b0 and 0x80 != 0) {
            val b1 = if (pos + 1 < data.size) data[pos + 1].toInt() and 0x7F else 0
            ((b0 and 0x7F) shl 7) or b1 to pos + 2
        } else {
            b0 to pos + 1
        }
    }

    private fun readUtf16String(data: ByteArray, pos: Int): String {
        var p = pos
        if (p + 2 > data.size) return ""
        var len = readU16(data, p)
        p += 2
        if (len and 0x8000 != 0) {
            len = ((len and 0x7FFF) shl 16) or readU16(data, p)
            p += 2
        }
        val byteLen = len * 2
        if (byteLen <= 0 || p + byteLen > data.size) return ""
        return String(data, p, byteLen, StandardCharsets.UTF_16LE)
    }

    private fun readU16(data: ByteArray, pos: Int): Int {
        if (pos + 2 > data.size) return 0
        return (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
    }

    private fun readU32(data: ByteArray, pos: Int): Long {
        if (pos + 4 > data.size) return 0
        return (data[pos].toInt() and 0xFF).toLong() or
                ((data[pos + 1].toInt() and 0xFF).toLong() shl 8) or
                ((data[pos + 2].toInt() and 0xFF).toLong() shl 16) or
                ((data[pos + 3].toInt() and 0xFF).toLong() shl 24)
    }
}
