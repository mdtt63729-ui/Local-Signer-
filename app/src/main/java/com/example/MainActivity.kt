package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .alpha(alpha)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape = CircleShape)
        )
        Surface(
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val (name, size) = getFileInfo(context, it)
            viewModel.selectApk(context, it, name, size)
        }
    }

    val keystorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val (name, _) = getFileInfo(context, it)
            viewModel.selectCustomKeystore(it, name)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedContent(targetState = state.phase, label = "TopBarTitle") { phase ->
                        when (phase) {
                            AppPhase.IDLE -> Text("Local Offline APK Suite", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            AppPhase.SCANNING -> Text("Security Inspection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            AppPhase.KEYGEN -> Text("Create Keystore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            AppPhase.EXTRACTOR -> Text("Extract Installed App", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            AppPhase.APP_CLONER -> Text("App Cloner & Renamer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            AppPhase.SPLIT_MERGER -> Text("Split APK Merger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            AppPhase.COMPARE -> Text("APK Compare Tool", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            AppPhase.MANIFEST_VIEWER -> Text("Manifest & Res Viewer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            AppPhase.CONFIG -> {
                                if (state.isSigning) {
                                    Text("Signing APK...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                } else {
                                    Text("Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (state.phase != AppPhase.IDLE && state.phase != AppPhase.SCANNING) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (state.phase == AppPhase.MANIFEST_VIEWER || state.phase == AppPhase.APP_CLONER) {
                                viewModel.navigateToPhase(AppPhase.CONFIG)
                            } else if (state.phase == AppPhase.CONFIG && state.selectedApkUri != null) {
                                viewModel.resetApkSelection()
                            } else {
                                viewModel.navigateToIdle()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.phase,
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) + slideInHorizontally(initialOffsetX = { 200 }) with
                        fadeOut(animationSpec = tween(400)) + slideOutHorizontally(targetOffsetX = { -200 })
            },
            label = "PhaseTransition"
        ) { phase ->
            when (phase) {
                AppPhase.IDLE -> {
                    IdleScreen(
                        state = state,
                        haptic = haptic,
                        onSelectApk = { apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive")) },
                        onNavigatePhase = { viewModel.navigateToPhase(it) },
                        onInstall = { viewModel.installApk(context, it) },
                        onShare = { viewModel.shareApk(context, it) }
                    )
                }
                AppPhase.SCANNING -> {
                    ScanningScreen(state = state)
                }
                AppPhase.KEYGEN -> {
                    KeygenScreen(state = state, viewModel = viewModel, context = context, haptic = haptic)
                }
                AppPhase.EXTRACTOR -> {
                    ExtractorScreen(state, viewModel, haptic, context)
                }
                AppPhase.APP_CLONER, AppPhase.SPLIT_MERGER, AppPhase.COMPARE, AppPhase.MANIFEST_VIEWER -> {
                    PlaceholderScreen(state, viewModel, haptic)
                }
                AppPhase.CONFIG -> {
                    ConfigScreen(
                        state = state,
                        viewModel = viewModel,
                        keystorePickerLauncher = keystorePickerLauncher,
                        haptic = haptic,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun KeygenScreen(
    state: MainState,
    viewModel: MainViewModel,
    context: Context,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        item {
            Text("Generate a new local offline .jks Keystore. This key will be used to securely sign your Android applications.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }

        item {
            OutlinedTextField(
                value = state.keygenAlias,
                onValueChange = { viewModel.updateKeygenParams(it, state.keygenPass, state.keygenName, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, state.keygenState, state.keygenCountryCode) },
                label = { Text("Key Alias") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        item {
            OutlinedTextField(
                value = state.keygenPass,
                onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, it, state.keygenName, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, state.keygenState, state.keygenCountryCode) },
                label = { Text("Password (Min 6 chars)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item { Text("Certificate Identity (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }

        item {
            OutlinedTextField(
                value = state.keygenName,
                onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, it, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, state.keygenState, state.keygenCountryCode) },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.keygenOrgUnit,
                    onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, state.keygenName, it, state.keygenOrg, state.keygenCity, state.keygenState, state.keygenCountryCode) },
                    label = { Text("Org Unit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.keygenOrg,
                    onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, state.keygenName, state.keygenOrgUnit, it, state.keygenCity, state.keygenState, state.keygenCountryCode) },
                    label = { Text("Organization") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.keygenCity,
                    onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, state.keygenName, state.keygenOrgUnit, state.keygenOrg, it, state.keygenState, state.keygenCountryCode) },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.keygenState,
                    onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, state.keygenName, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, it, state.keygenCountryCode) },
                    label = { Text("State/Prov") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.keygenCountryCode,
                    onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, state.keygenName, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, state.keygenState, it) },
                    label = { Text("C (US)") },
                    singleLine = true,
                    modifier = Modifier.weight(0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            if (state.isGeneratingKey) {
                Box(modifier = Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.generateKeystore(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(22.dp),
                    enabled = state.keygenAlias.isNotEmpty() && state.keygenPass.isNotEmpty()
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("GENERATE KEYSTORE", fontWeight = FontWeight.Bold)
                }
            }
            
            if (state.keygenError != null) {
                Text(
                    text = state.keygenError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

@Composable
fun IdleScreen(
    state: MainState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onSelectApk: () -> Unit,
    onNavigatePhase: (AppPhase) -> Unit,
    onInstall: (java.io.File) -> Unit,
    onShare: (java.io.File) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onSelectApk()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.FilePresent, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Select Unsigned APK File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        
        item {
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onNavigatePhase(AppPhase.EXTRACTOR)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Extract Installed App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        
        item {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onNavigatePhase(AppPhase.SPLIT_MERGER)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Merge Split APKs (.xapk)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        
        item {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onNavigatePhase(AppPhase.COMPARE)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Compare APKs (Diff Tool)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        
        item {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onNavigatePhase(AppPhase.KEYGEN)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Create New Keystore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        if (state.history.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Recent Signed APKs",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(state.history) { historyItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(historyItem.fileName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Path: /Downloads/SignedAPKs/",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FilledTonalButton(onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onInstall(historyItem.file)
                            }) {
                                Text("Install")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onShare(historyItem.file)
                            }) {
                                Text("Share")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                Toast.makeText(context, "Saved at: ${historyItem.path}", Toast.LENGTH_LONG).show()
                            }) {
                                Text("Locate")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

@Composable
fun ScanningScreen(state: MainState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Scanning: ${state.selectedApkName}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Size: ${state.selectedApkSize}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        RadarAnimation()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Deep Scanning Dex Files & Permissions...", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Progress", fontWeight = FontWeight.Bold)
            Text("${(state.scanProgress * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = state.scanProgress,
            modifier = Modifier.fillMaxWidth().height(12.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Terminal Logs
        val logListState = rememberLazyListState()
        LaunchedEffect(state.scanLogs.size) {
            if (state.scanLogs.isNotEmpty()) {
                logListState.animateScrollToItem(state.scanLogs.size - 1)
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color(0xFF121212),
            contentColor = Color(0xFF00FF00)
        ) {
            LazyColumn(
                state = logListState,
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { Text("> Security Scanner Output...", fontFamily = FontFamily.Monospace, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp), color = Color.Gray) }
                items(state.scanLogs) { log ->
                    Text(
                        log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ConfigScreen(
    state: MainState,
    viewModel: MainViewModel,
    keystorePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. APK Details (Manifest Info)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Target Details:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                state.selectedApkName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.isApkSigned) {
                        Text("Status: ${state.apkSignatures}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Status: Unsigned (Ready)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (state.manifestPackageName.isNotEmpty()) {
                        Text("Package: ${state.manifestPackageName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("Version: ${state.manifestVersionName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Text("Size: ${state.selectedApkSize}", style = MaterialTheme.typography.bodySmall)
                    if (state.selectedApkHash.isNotEmpty()) {
                        Text("SHA-256: ${state.selectedApkHash}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    
                    if (!state.isSigning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.resetApkSelection()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Change APK File")
                        }
                    }
                }
            }
        }

        // 1.5 Utility Suite Options
        item {
            if (!state.isSigning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Modification & Utilities",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    viewModel.navigateToPhase(AppPhase.MANIFEST_VIEWER)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Manifest", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    viewModel.navigateToPhase(AppPhase.APP_CLONER)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cloner", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // 2. Keystore Config or Signing Progress
        item {
            AnimatedContent(
                targetState = state.isSigning,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 50 }) with
                            fadeOut(animationSpec = tween(400)) + slideOutVertically(targetOffsetY = { -50 })
                },
                label = "KeystoreOrProgress"
            ) { signing ->
                if (!signing) {
                    // Keystore Config
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Keystore Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = !state.useCustomKeystore,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        viewModel.setUseCustomKeystore(false)
                                    }
                                )
                                Text("Use Embedded Default Keystore", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = state.useCustomKeystore,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        viewModel.setUseCustomKeystore(true)
                                    }
                                )
                                Text("Use Custom Key (.jks / .keystore)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                            }

                            AnimatedVisibility(visible = state.useCustomKeystore) {
                                Column(modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp)) {
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            keystorePickerLauncher.launch(arrayOf("*/*"))
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                    ) {
                                        Text(if (state.customKeystoreUri == null) "Select Keystore File" else "Selected: ${state.customKeystoreName}")
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = state.customAlias,
                                        onValueChange = { viewModel.updateCustomKeystoreParams(it, state.customKeyPass, state.customStorePass) },
                                        label = { Text("Key Alias") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = state.customStorePass,
                                        onValueChange = { viewModel.updateCustomKeystoreParams(state.customAlias, state.customKeyPass, it) },
                                        label = { Text("Store Password") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = state.customKeyPass,
                                        onValueChange = { viewModel.updateCustomKeystoreParams(state.customAlias, it, state.customStorePass) },
                                        label = { Text("Key Password") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Signing Progress UI
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Signing Progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("${(state.signProgress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = state.signProgress,
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text("Current Step:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            AnimatedContent(
                                targetState = state.currentStep,
                                transitionSpec = {
                                    fadeIn() + slideInHorizontally { 100 } with fadeOut() + slideOutHorizontally { -100 }
                                },
                                label = "StepAnimation"
                            ) { step ->
                                Text(
                                    "✨ $step",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Terminal Logs
                            val logListState = rememberLazyListState()
                            LaunchedEffect(state.signLogs.size) {
                                if (state.signLogs.isNotEmpty()) {
                                    logListState.animateScrollToItem(state.signLogs.size - 1)
                                }
                            }
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E1E1E),
                                contentColor = Color(0xFF00FF00)
                            ) {
                                LazyColumn(
                                    state = logListState,
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    item { Text("> Live Log Output...", fontFamily = FontFamily.Monospace, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp), color = Color.Gray) }
                                    items(state.signLogs) { log ->
                                        Text(
                                            log,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Action Button
        item {
            if (state.isSigning) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.cancelSigning()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.signApk(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(22.dp),
                    enabled = (!state.useCustomKeystore || (state.customKeystoreUri != null && state.customAlias.isNotEmpty())),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("SIGN APK NOW", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            }
            
            if (state.signError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Error: ${state.signError}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

private fun getFileInfo(context: Context, uri: Uri): Pair<String, String> {
    var name = "Unknown"
    var sizeStr = "0 B"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex != -1) name = cursor.getString(nameIndex)
            if (sizeIndex != -1) {
                val sizeBytes = cursor.getLong(sizeIndex)
                sizeStr = formatSize(sizeBytes)
            }
        }
    }
    return Pair(name, sizeStr)
}

private fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
