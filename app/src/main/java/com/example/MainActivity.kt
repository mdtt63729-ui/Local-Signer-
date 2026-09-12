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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerminalGreen
import kotlinx.coroutines.delay
import java.io.File
import kotlin.random.Random
import java.text.DecimalFormat

// ---------------------------------------------------------------------------
// Premium motion constants — Material 3 emphasized easing curves
// ---------------------------------------------------------------------------
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/**
 * Press korle button/card e subtle springy scale — premium tactile feedback.
 * Je kono modifier er sathe use kora jay: Modifier.pressScale()
 */
fun Modifier.pressScale(pressedScale: Float = 0.97f): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/** List e dhokar somoy staggered (ekta ekta kore) smooth entrance */
@Composable
fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(420, easing = EmphasizedDecelerate)) +
                slideInVertically(tween(460, easing = EmphasizedDecelerate)) { it / 5 }
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Activity
// ---------------------------------------------------------------------------
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

// ---------------------------------------------------------------------------
// Premium scanning radar — dual staggered rings + pulsing core
// ---------------------------------------------------------------------------
@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "ring1Scale"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "ring1Alpha"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1000)
        ),
        label = "ring2Scale"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1000)
        ),
        label = "ring2Alpha"
    )
    val coreScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreScale"
    )

    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .scale(ring1Scale)
                .alpha(ring1Alpha)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(84.dp)
                .scale(ring2Scale)
                .alpha(ring2Alpha)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), CircleShape)
        )
        Surface(
            modifier = Modifier
                .size(84.dp)
                .scale(coreScale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main screen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // App start e storage theke ager signed APK gulo history te load hoy
    LaunchedEffect(Unit) {
        viewModel.loadHistoryFromDisk(context)
    }

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = state.phase,
                        transitionSpec = {
                            (fadeIn(tween(250, easing = EmphasizedDecelerate)) +
                                    slideInVertically(tween(300, easing = EmphasizedDecelerate)) { it / 3 }) togetherWith
                                    fadeOut(tween(150))
                        },
                        label = "TopBarTitle"
                    ) { phase ->
                        val title = when (phase) {
                            AppPhase.IDLE -> "APK Signer Suite"
                            AppPhase.SCANNING -> "Security Inspection"
                            AppPhase.KEYGEN -> "Create Keystore"
                            AppPhase.EXTRACTOR -> "Extract Installed App"
                            AppPhase.APP_CLONER -> "App Cloner & Renamer"
                            AppPhase.SPLIT_MERGER -> "Split APK Merger"
                            AppPhase.COMPARE -> "APK Compare Tool"
                            AppPhase.MANIFEST_VIEWER -> "Manifest & Res Viewer"
                            AppPhase.CONFIG -> if (state.isSigning) "Signing APK..." else "Configuration"
                        }
                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    if (state.phase != AppPhase.IDLE && state.phase != AppPhase.SCANNING) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (state.phase == AppPhase.MANIFEST_VIEWER || state.phase == AppPhase.APP_CLONER) {
                                viewModel.navigateToPhase(AppPhase.CONFIG)
                            } else if (state.phase == AppPhase.CONFIG && state.selectedApkUri != null) {
                                viewModel.resetApkSelection()
                            } else {
                                viewModel.navigateToIdle()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.phase,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                if (forward) {
                    (slideInHorizontally(tween(420, easing = EmphasizedDecelerate)) { it / 5 } +
                            fadeIn(tween(320, delayMillis = 40))) togetherWith
                            (slideOutHorizontally(tween(220, easing = EmphasizedAccelerate)) { -it / 6 } +
                                    fadeOut(tween(160)))
                } else {
                    (slideInHorizontally(tween(420, easing = EmphasizedDecelerate)) { -it / 5 } +
                            fadeIn(tween(320, delayMillis = 40))) togetherWith
                            (slideOutHorizontally(tween(220, easing = EmphasizedAccelerate)) { it / 6 } +
                                    fadeOut(tween(160)))
                }.using(SizeTransform(clip = false))
            },
            label = "PhaseTransition"
        ) { phase ->
            when (phase) {
                AppPhase.IDLE -> {
                    IdleScreen(
                        state = state,
                        haptic = haptic,
                        onSelectApk = {
                            apkPickerLauncher.launch(
                                arrayOf(
                                    "application/vnd.android.package-archive",
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/octet-stream"
                                )
                            )
                        },
                        onNavigatePhase = { viewModel.navigateToPhase(it) },
                        onInstall = { viewModel.installApk(context, it) },
                        onShare = { viewModel.shareApk(context, it) },
                        onRename = { item, name -> viewModel.renameHistoryItem(context, item, name) },
                        onDelete = { item -> viewModel.deleteHistoryItem(context, item) }
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
                AppPhase.SPLIT_MERGER -> {
                    SplitMergerScreen(state, viewModel, haptic, context)
                }
                AppPhase.COMPARE -> {
                    CompareScreen(state, viewModel, haptic, context)
                }
                AppPhase.MANIFEST_VIEWER -> {
                    ManifestViewerScreen(state, viewModel, context)
                }
                AppPhase.APP_CLONER -> {
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

// ---------------------------------------------------------------------------
// Keygen screen
// ---------------------------------------------------------------------------
@Composable
fun KeygenScreen(
    state: MainState,
    viewModel: MainViewModel,
    context: Context,
    haptic: HapticFeedback
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                "Generate a new local offline .jks keystore. This key will be used to securely sign your Android applications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        item {
            OutlinedTextField(
                value = state.keygenAlias,
                onValueChange = { viewModel.updateKeygenParams(it, state.keygenPass, state.keygenName, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, state.keygenState, state.keygenCountryCode) },
                label = { Text("Key Alias") },
                leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )
        }

        item {
            OutlinedTextField(
                value = state.keygenPass,
                onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, it, state.keygenName, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, state.keygenState, state.keygenCountryCode) },
                label = { Text("Password (Min 6 chars)") },
                leadingIcon = { Icon(Icons.Filled.Security, contentDescription = null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )
        }

        item {
            Text(
                "Certificate Identity (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            OutlinedTextField(
                value = state.keygenName,
                onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, it, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, state.keygenState, state.keygenCountryCode) },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
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
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = state.keygenOrg,
                    onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, state.keygenName, state.keygenOrgUnit, it, state.keygenCity, state.keygenState, state.keygenCountryCode) },
                    label = { Text("Organization") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
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
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = state.keygenState,
                    onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, state.keygenName, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, it, state.keygenCountryCode) },
                    label = { Text("State/Prov") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = state.keygenCountryCode,
                    onValueChange = { viewModel.updateKeygenParams(state.keygenAlias, state.keygenPass, state.keygenName, state.keygenOrgUnit, state.keygenOrg, state.keygenCity, state.keygenState, it) },
                    label = { Text("C (US)") },
                    singleLine = true,
                    modifier = Modifier.weight(0.5f),
                    shape = MaterialTheme.shapes.small
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            if (state.isGeneratingKey) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.generateKeystore(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .pressScale(0.98f),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 1.dp
                    ),
                    enabled = state.keygenAlias.isNotEmpty() && state.keygenPass.isNotEmpty()
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("GENERATE KEYSTORE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            AnimatedVisibility(visible = state.keygenError != null) {
                Text(
                    text = state.keygenError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Idle screen — hero header + tool cards + history
// ---------------------------------------------------------------------------
@Composable
fun IdleScreen(
    state: MainState,
    haptic: HapticFeedback,
    onSelectApk: () -> Unit,
    onNavigatePhase: (AppPhase) -> Unit,
    onInstall: (File) -> Unit,
    onShare: (File) -> Unit,
    onRename: (SignedHistory, String) -> Unit,
    onDelete: (SignedHistory) -> Unit
) {
    val context = LocalContext.current
    // Manual rename er jonno jei history item select kora ache
    var renamingItem by remember { mutableStateOf<SignedHistory?>(null) }
    // Delete confirm er jonno
    var deletingItem by remember { mutableStateOf<SignedHistory?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero header — subtle brand gradient
        item {
            StaggerIn(index = 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color.Transparent,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Sign APKs Offline",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Private • Fast • On-device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Primary CTA
        item {
            StaggerIn(index = 1) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectApk()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .pressScale(0.98f),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 1.dp
                    )
                ) {
                    Icon(Icons.Filled.FilePresent, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Select APK or ZIP File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            StaggerIn(index = 2) {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigatePhase(AppPhase.EXTRACTOR)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .pressScale(0.98f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Filled.Android, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Extract Installed App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tool cards
        item {
            StaggerIn(index = 3) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(0.99f),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    ToolRow(
                        icon = Icons.Filled.Layers,
                        title = "Merge Split APKs (.xapk)",
                        subtitle = "Combine split installs into one APK"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigatePhase(AppPhase.SPLIT_MERGER)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ToolRow(
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        title = "Compare APKs (Diff Tool)",
                        subtitle = "Spot differences between two builds"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigatePhase(AppPhase.COMPARE)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ToolRow(
                        icon = Icons.Filled.Key,
                        title = "Create New Keystore",
                        subtitle = "Generate your own signing key"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigatePhase(AppPhase.KEYGEN)
                    }
                }
            }
        }

        // History
        if (state.history.isNotEmpty()) {
            item {
                StaggerIn(index = 4) {
                    Text(
                        "Recent Signed APKs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(state.history, key = { it.path }) { historyItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(0.98f)
                        .animateItem(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    historyItem.fileName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "/SignedAPKs/",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onInstall(historyItem.file)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShare(historyItem.file)
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as android.content.ClipboardManager
                                clipboard.setPrimaryClip(
                                    android.content.ClipData.newPlainText("path", historyItem.path)
                                )
                                Toast.makeText(context, "Path copied: ${historyItem.path}", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = "Locate", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                renamingItem = historyItem
                            }) {
                                Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = "Rename", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                deletingItem = historyItem
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
    } // Box close

    // Rename dialog — manually history item rename korar jonno
    renamingItem?.let { item ->
        RenameApkDialog(
            initialName = item.fileName,
            onConfirm = { newName ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRename(item, newName)
                renamingItem = null
            },
            onDismiss = { renamingItem = null }
        )
    }

    // Delete dialog — history + storage dui jaygatei permanently delete
    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("Delete APK?") },
            text = {
                Text(
                    "\"${item.fileName}\" will be permanently deleted from your storage.\n\nPath:\n${item.path}"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete(item)
                    deletingItem = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RenameApkDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(initialName.removeSuffix(".apk")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename APK") },
        text = {
            Column {
                Text(
                    "Enter a new name for this signed APK.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("File name") },
                    suffix = { Text(".apk") },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ToolRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
                .size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

// Scanning screen — matrix terminal + Material 3
// ---------------------------------------------------------------------------
// Scanning screen — matrix terminal + Material 3
// ---------------------------------------------------------------------------
private const val RAIN_CHARS = "アイウエオカキクケコサシスセソ0123456789ABCDEF"

private class RainColumnData(val chars: String, val speed: Float, val phase: Float)

/** Matrix digital-rain background — canvas e falling green glyphs (smooth, loop-safe) */
@Composable
fun MatrixRain(modifier: Modifier = Modifier, rainColor: Color = TerminalGreen) {
    val textMeasurer = rememberTextMeasurer()
    val columns = remember {
        List(26) {
            val count = (8..14).random()
            RainColumnData(
                chars = (0 until count).map { RAIN_CHARS.random() }.joinToString(""),
                // integer speed → animation wrap e kono jump hoy na
                speed = if ((0..1).random() == 0) 1f else 2f,
                phase = Random.nextFloat()
            )
        }
    }
    val t by rememberInfiniteTransition(label = "matrixRain").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "matrixT"
    )
    Canvas(modifier = modifier) {
        val charH = 20.dp.toPx()
        val colW = charH * 0.95f
        val nCols = (size.width / colW).toInt().coerceIn(1, columns.size)
        for (i in 0 until nCols) {
            val col = columns[i]
            val travel = size.height + col.chars.length * charH
            val headY = ((t * col.speed + col.phase) % 1f) * travel - charH
            col.chars.forEachIndexed { idx, c ->
                val y = headY - idx * charH
                if (y > -charH && y < size.height) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = c.toString(),
                        topLeft = Offset(i * colW, y),
                        style = TextStyle(
                            color = rainColor.copy(alpha = (1f - idx.toFloat() / col.chars.length) * 0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

/** Terminal log er color — ERROR red, WARN amber, baki green */
private fun matrixLogColor(log: String): Color = when {
    log.contains("[ERROR]") -> Color(0xFFFF5252)
    log.contains("[WARN]") -> Color(0xFFFFC400)
    else -> TerminalGreen
}

@Composable
fun ScanningScreen(state: MainState) {
    // iOS-smooth: no-bounce critically damped spring
    val animatedProgress by animateFloatAsState(
        targetValue = state.scanProgress,
        animationSpec = spring(dampingRatio = 1f, stiffness = 380f),
        label = "scanProgress"
    )
    // Blinking cursor
    val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    val logListState = rememberLazyListState()
    LaunchedEffect(state.scanLogs.size) {
        if (state.scanLogs.isNotEmpty()) {
            logListState.animateScrollToItem(state.scanLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header — file + size chip
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SECURITY SCAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Text(
                    state.selectedApkName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    state.selectedApkSize,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Matrix terminal — puro screen jure
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.large,
            color = Color(0xFF030A05),
            border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.25f))
        ) {
            Box {
                // Digital rain background
                MatrixRain(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.20f)
                )
                Column {
                    // Terminal title bar — macOS style traffic lights
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .background(Color(0xFFFF5F57), CircleShape)
                            )
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .background(Color(0xFFFEBC2E), CircleShape)
                            )
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .background(Color(0xFF28C840), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "root@signer:~# security-scan",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TerminalGreen.copy(alpha = 0.65f),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalGreen
                        )
                    }

                    // Logs
                    LazyColumn(
                        state = logListState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(state.scanLogs) { log ->
                            Text(
                                log,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = matrixLogColor(log)
                            )
                        }
                        // Input line — blinking cursor
                        item {
                            Row {
                                Text(
                                    "> ",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TerminalGreen
                                )
                                Text(
                                    "▊",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TerminalGreen.copy(alpha = cursorAlpha)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress — M3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Deep Scanning Dex & Permissions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}


// ---------------------------------------------------------------------------
// Config screen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ConfigScreen(
    state: MainState,
    viewModel: MainViewModel,
    keystorePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    haptic: HapticFeedback,
    context: Context
) {
    // Compact layout: content upore (scan hobe khub kom), SIGN button
    // bottom e FIXED — kono scroll chara'i puro screen dekha jay.
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedContent(
                targetState = state.isSigning,
                transitionSpec = {
                    (fadeIn(tween(350, easing = EmphasizedDecelerate)) +
                            slideInVertically(tween(400, easing = EmphasizedDecelerate)) { it / 8 }) togetherWith
                            (fadeOut(tween(180)) + slideOutVertically(tween(220, easing = EmphasizedAccelerate)) { -it / 10 })
                },
                label = "KeystoreOrProgress"
            ) { signing ->
                if (!signing) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ApkDetailsCard(state, viewModel, haptic)
                        UtilitiesRow(state, viewModel, haptic)
                        KeystoreCard(state, viewModel, keystorePickerLauncher, haptic)
                    }
                } else {
                    SigningProgressCard(state)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ------------------------------------------------------------------
        // Fixed action bar — screen er bottom e ALWAYS visible
        // ------------------------------------------------------------------
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
        ) {
            // Anti-tamper protection switch — sign button er thik upore
            if (!state.isSigning) {
                ProtectionSwitchCard(
                    checked = state.protectEnabled,
                    onChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setProtection(it)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (state.isSigning) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.cancelSigning()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .pressScale(0.98f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.signApk(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .pressScale(0.98f),
                    shape = RoundedCornerShape(20.dp),
                    enabled = (!state.useCustomKeystore || (state.customKeystoreUri != null && state.customAlias.isNotEmpty())),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SIGN APK NOW", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = state.signError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Error: ${state.signError ?: ""}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Config: anti-tamper protection switch — smooth animated Material 3 card
// ---------------------------------------------------------------------------
@Composable
private fun ProtectionSwitchCard(checked: Boolean, onChange: (Boolean) -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "protectionCard"
    )
    val iconTint by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "protectionIcon"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Security,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Anti-Tamper Protection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Decompiled & modified rebuilds crash on launch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Config: compact APK details card
// ---------------------------------------------------------------------------
@Composable
private fun ApkDetailsCard(
    state: MainState,
    viewModel: MainViewModel,
    haptic: HapticFeedback
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.selectedApkName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (state.manifestPackageName.isNotEmpty())
                            "${state.manifestPackageName} • v${state.manifestVersionName}"
                        else "Size: ${state.selectedApkSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Status chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (state.isApkSigned)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        if (state.isApkSigned) "Signed" else "Ready",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isApkSigned)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Size: ${state.selectedApkSize}" + if (state.isApkSigned) " • ${state.apkSignatures}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.selectedApkHash.isNotEmpty()) {
                Text(
                    "SHA-256: ${state.selectedApkHash}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!state.isSigning) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.resetApkSelection()
                        }
                    ) {
                        Icon(Icons.Filled.FilePresent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change APK File")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Config: compact utilities row
// ---------------------------------------------------------------------------
@Composable
private fun UtilitiesRow(
    state: MainState,
    viewModel: MainViewModel,
    haptic: HapticFeedback
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Utilities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.navigateToPhase(AppPhase.MANIFEST_VIEWER)
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Manifest", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.navigateToPhase(AppPhase.APP_CLONER)
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Cloner", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Config: keystore selection card
// ---------------------------------------------------------------------------
@Composable
private fun KeystoreCard(
    state: MainState,
    viewModel: MainViewModel,
    keystorePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    haptic: HapticFeedback
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Keystore Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(0.99f)
                    .clickable { viewModel.setUseCustomKeystore(false) }
            ) {
                RadioButton(
                    selected = !state.useCustomKeystore,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setUseCustomKeystore(false)
                    }
                )
                Text("Embedded Default Keystore", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(0.99f)
                    .clickable { viewModel.setUseCustomKeystore(true) }
            ) {
                RadioButton(
                    selected = state.useCustomKeystore,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setUseCustomKeystore(true)
                    }
                )
                Text("Custom Key (.jks / .keystore)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
            }

            AnimatedVisibility(
                visible = state.useCustomKeystore,
                enter = fadeIn(tween(300, easing = EmphasizedDecelerate)) +
                        expandVertically(tween(350, easing = EmphasizedDecelerate)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(220, easing = EmphasizedAccelerate))
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            keystorePickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(
                            if (state.customKeystoreUri == null) "Select Keystore File" else state.customKeystoreName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.customAlias,
                        onValueChange = { viewModel.updateCustomKeystoreParams(it, state.customKeyPass, state.customStorePass) },
                        label = { Text("Key Alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.customStorePass,
                        onValueChange = { viewModel.updateCustomKeystoreParams(state.customAlias, state.customKeyPass, it) },
                        label = { Text("Store Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.customKeyPass,
                        onValueChange = { viewModel.updateCustomKeystoreParams(state.customAlias, it, state.customStorePass) },
                        label = { Text("Key Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Config: signing progress card — Material 3 + iOS-smooth motion
// ---------------------------------------------------------------------------
@Composable
private fun SigningProgressCard(state: MainState) {
    // iOS-smooth: critically damped spring (kono bounce nei)
    val animatedSignProgress by animateFloatAsState(
        targetValue = state.signProgress,
        animationSpec = spring(dampingRatio = 1f, stiffness = 380f),
        label = "signProgress"
    )
    // Gentle pulsing shield icon
    val iconScale by rememberInfiniteTransition(label = "signPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "iconScale"
    )
    val logListState = rememberLazyListState()
    LaunchedEffect(state.signLogs.size) {
        if (state.signLogs.isNotEmpty()) {
            logListState.animateScrollToItem(state.signLogs.size - 1)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header — pulsing shield + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .scale(iconScale)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Signing APK",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Applying V1 + V2 + V3 signatures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Big percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "Progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${(animatedSignProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    state.selectedApkName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(start = 16.dp, bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedSignProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current step — smooth iOS-feel transition
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        (fadeIn(tween(250, easing = EmphasizedDecelerate)) +
                                slideInVertically(tween(300, easing = EmphasizedDecelerate)) { it / 6 }) togetherWith
                                fadeOut(tween(150))
                    },
                    label = "StepAnimation"
                ) { step ->
                    Text(
                        step,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Matrix mini-terminal — live log
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = MaterialTheme.shapes.medium,
                color = Color(0xFF030A05),
                border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.25f))
            ) {
                Box {
                    MatrixRain(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.15f)
                    )
                    LazyColumn(
                        state = logListState,
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(state.signLogs) { log ->
                            Text(
                                log,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = matrixLogColor(log)
                            )
                        }
                    }
                }
            }
        }
    }
}



// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
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
