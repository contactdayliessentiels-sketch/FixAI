package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.FixAiViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.FixAiHeader
import com.example.ui.screens.DiagnosticResultScreen
import com.example.ui.screens.FixAiVisionScreen
import com.example.ui.screens.HardwareTestScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageAnalysisScreen
import com.example.ui.screens.InteractiveDiagnosticScreen
import com.example.ui.screens.TextDiagnosticScreen
import com.example.ui.screens.UserAccountScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.FixAiTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FixAiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val isPremium by viewModel.isPremium.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val isTechnicianMode by viewModel.isTechnicianMode.collectAsState()
            val isSpeaking by viewModel.isSpeaking.collectAsState()

            val textProblemInput by viewModel.textProblemInput.collectAsState()
            val selectedCategory by viewModel.selectedCategory.collectAsState()
            val selectedDeviceName by viewModel.selectedDeviceName.collectAsState()
            val selectedImageBitmap by viewModel.selectedImageBitmap.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val loadingMessage by viewModel.loadingMessage.collectAsState()

            val currentDiagnostic by viewModel.currentDiagnostic.collectAsState()
            val allDiagnostics by viewModel.allDiagnostics.collectAsState()
            val allDevices by viewModel.allDevices.collectAsState()
            val hardwareTestResults by viewModel.hardwareTestResults.collectAsState()
            val interactiveNode by viewModel.interactiveCurrentNode.collectAsState()

            FixAiTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            FixAiHeader(
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { viewModel.toggleTheme() },
                                currentLanguage = currentLanguage,
                                onSelectLanguage = { viewModel.setLanguage(it) },
                                isPremium = isPremium,
                                onTogglePremium = { viewModel.togglePremium() },
                                onHeaderClick = { viewModel.navigateTo(ScreenDestination.HOME) }
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == ScreenDestination.HOME,
                                    onClick = { viewModel.navigateTo(ScreenDestination.HOME) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Accueil") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyanNeon,
                                        selectedTextColor = CyanNeon
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == ScreenDestination.TEXT_DIAGNOSTIC ||
                                            currentScreen == ScreenDestination.IMAGE_DIAGNOSTIC ||
                                            currentScreen == ScreenDestination.FIXAI_VISION ||
                                            currentScreen == ScreenDestination.INTERACTIVE_TREE,
                                    onClick = { viewModel.navigateTo(ScreenDestination.TEXT_DIAGNOSTIC) },
                                    icon = { Icon(Icons.Default.Build, contentDescription = "Diagnostic") },
                                    label = { Text("Diagnostic") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyanNeon,
                                        selectedTextColor = CyanNeon
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == ScreenDestination.HISTORY,
                                    onClick = { viewModel.navigateTo(ScreenDestination.HISTORY) },
                                    icon = { Icon(Icons.Default.History, contentDescription = "Historique") },
                                    label = { Text("Historique") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyanNeon,
                                        selectedTextColor = CyanNeon
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == ScreenDestination.HARDWARE_TESTS,
                                    onClick = { viewModel.runHardwareTests() },
                                    icon = { Icon(Icons.Default.Speed, contentDescription = "Tests") },
                                    label = { Text("Tests System") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyanNeon,
                                        selectedTextColor = CyanNeon
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == ScreenDestination.ACCOUNT_DEVICES,
                                    onClick = { viewModel.navigateTo(ScreenDestination.ACCOUNT_DEVICES) },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Compte") },
                                    label = { Text("Compte") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyanNeon,
                                        selectedTextColor = CyanNeon
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                ScreenDestination.HOME -> HomeScreen(
                                    onNavigate = { dest ->
                                        if (dest == ScreenDestination.INTERACTIVE_TREE) {
                                            viewModel.startInteractiveDiagnostic()
                                        } else {
                                            viewModel.navigateTo(dest)
                                        }
                                    },
                                    onSelectCategory = { cat ->
                                        viewModel.selectedCategory.value = cat
                                    }
                                )

                                ScreenDestination.TEXT_DIAGNOSTIC -> TextDiagnosticScreen(
                                    problemInput = textProblemInput,
                                    onProblemInputChange = { viewModel.textProblemInput.value = it },
                                    selectedCategory = selectedCategory,
                                    onCategoryChange = { viewModel.selectedCategory.value = it },
                                    selectedDeviceName = selectedDeviceName,
                                    onDeviceNameChange = { viewModel.selectedDeviceName.value = it },
                                    registeredDevices = allDevices,
                                    isLoading = isLoading,
                                    loadingMessage = loadingMessage,
                                    onSubmit = {
                                        viewModel.submitTextDiagnostic(
                                            textProblemInput,
                                            selectedCategory,
                                            selectedDeviceName
                                        )
                                    },
                                    onBack = { viewModel.navigateTo(ScreenDestination.HOME) }
                                )

                                ScreenDestination.IMAGE_DIAGNOSTIC -> ImageAnalysisScreen(
                                    selectedBitmap = selectedImageBitmap,
                                    onBitmapSelected = { viewModel.selectedImageBitmap.value = it },
                                    problemText = textProblemInput,
                                    onProblemTextChange = { viewModel.textProblemInput.value = it },
                                    category = selectedCategory,
                                    deviceName = selectedDeviceName,
                                    isLoading = isLoading,
                                    loadingMessage = loadingMessage,
                                    onSubmitImage = {
                                        val bmp = selectedImageBitmap
                                        if (bmp != null) {
                                            viewModel.submitImageDiagnostic(
                                                bmp,
                                                textProblemInput,
                                                selectedCategory,
                                                selectedDeviceName
                                            )
                                        }
                                    },
                                    onBack = { viewModel.navigateTo(ScreenDestination.HOME) }
                                )

                                ScreenDestination.FIXAI_VISION -> FixAiVisionScreen(
                                    onAnalyzeSnapshot = { bmp, prompt ->
                                        viewModel.submitImageDiagnostic(
                                            bmp,
                                            prompt,
                                            "Matériel Visuel",
                                            selectedDeviceName
                                        )
                                    },
                                    onBack = { viewModel.navigateTo(ScreenDestination.HOME) }
                                )

                                ScreenDestination.INTERACTIVE_TREE -> InteractiveDiagnosticScreen(
                                    currentNode = interactiveNode,
                                    currentLanguage = currentLanguage,
                                    onAnswer = { answer ->
                                        viewModel.answerInteractiveQuestion(answer)
                                    },
                                    onBack = { viewModel.navigateTo(ScreenDestination.HOME) }
                                )

                                ScreenDestination.DIAGNOSTIC_RESULT -> DiagnosticResultScreen(
                                    diagnostic = currentDiagnostic,
                                    isTechnicianMode = isTechnicianMode,
                                    onToggleTechnicianMode = { viewModel.toggleTechnicianMode() },
                                    isSpeaking = isSpeaking,
                                    onSpeakText = { viewModel.speakText(it) },
                                    onMarkStatus = { id, res ->
                                        viewModel.markDiagnosticResolved(id, res)
                                    },
                                    onResumeDiagnostic = {
                                        viewModel.startInteractiveDiagnostic()
                                    },
                                    onBack = { viewModel.navigateTo(ScreenDestination.HOME) }
                                )

                                ScreenDestination.HISTORY -> HistoryScreen(
                                    diagnostics = allDiagnostics,
                                    onSelectDiagnostic = { diag ->
                                        viewModel.selectDiagnosticFromHistory(diag)
                                    },
                                    onDeleteDiagnostic = { id ->
                                        viewModel.deleteDiagnosticHistory(id)
                                    },
                                    onClearAll = { viewModel.clearAllHistory() },
                                    onBack = { viewModel.navigateTo(ScreenDestination.HOME) }
                                )

                                ScreenDestination.HARDWARE_TESTS -> HardwareTestScreen(
                                    results = hardwareTestResults,
                                    onRunTests = { viewModel.runHardwareTests() },
                                    onBack = { viewModel.navigateTo(ScreenDestination.HOME) }
                                )

                                ScreenDestination.ACCOUNT_DEVICES -> UserAccountScreen(
                                    devices = allDevices,
                                    isPremium = isPremium,
                                    onTogglePremium = { viewModel.togglePremium() },
                                    onAddDevice = { dev -> viewModel.saveUserDevice(dev) },
                                    onDeleteDevice = { id -> viewModel.deleteUserDevice(id) },
                                    onBack = { viewModel.navigateTo(ScreenDestination.HOME) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
