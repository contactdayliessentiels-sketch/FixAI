package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DeviceEntity
import com.example.data.local.FixAiRepository
import com.example.data.model.AppLanguage
import com.example.data.model.DiagnosticCategory
import com.example.data.model.DiagnosticResult
import com.example.data.model.HardwareTestResult
import com.example.data.model.InteractiveQuestionNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class ScreenDestination {
    HOME,
    TEXT_DIAGNOSTIC,
    IMAGE_DIAGNOSTIC,
    FIXAI_VISION,
    INTERACTIVE_TREE,
    DIAGNOSTIC_RESULT,
    HISTORY,
    HARDWARE_TESTS,
    ACCOUNT_DEVICES
}

class FixAiViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val repository = FixAiRepository(application)

    // UI state flows
    val allDiagnostics: StateFlow<List<DiagnosticResult>> = repository.allDiagnostics.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allDevices: StateFlow<List<DeviceEntity>> = repository.allDevices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentScreen = MutableStateFlow(ScreenDestination.HOME)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.FRENCH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isTechnicianMode = MutableStateFlow(false)
    val isTechnicianMode: StateFlow<Boolean> = _isTechnicianMode.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _currentDiagnostic = MutableStateFlow<DiagnosticResult?>(null)
    val currentDiagnostic: StateFlow<DiagnosticResult?> = _currentDiagnostic.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("Analyse en cours...")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    private val _hardwareTestResults = MutableStateFlow<List<HardwareTestResult>>(emptyList())
    val hardwareTestResults: StateFlow<List<HardwareTestResult>> = _hardwareTestResults.asStateFlow()

    private val _interactiveCurrentNode = MutableStateFlow<InteractiveQuestionNode?>(null)
    val interactiveCurrentNode: StateFlow<InteractiveQuestionNode?> = _interactiveCurrentNode.asStateFlow()

    // Form states
    var textProblemInput = MutableStateFlow("")
    var selectedCategory = MutableStateFlow(DiagnosticCategory.COMPUTER.labelFr)
    var selectedDeviceName = MutableStateFlow("Mon PC")
    var selectedImageBitmap = MutableStateFlow<Bitmap?>(null)

    // TTS
    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
        // Seed initial mock registered device if empty
        viewModelScope.launch {
            repository.allDevices.collect { list ->
                if (list.isEmpty()) {
                    repository.saveDevice(
                        DeviceEntity(
                            name = "Mon PC Principal",
                            brand = "ASUS / Sur-Mesure",
                            model = "Gaming Tower",
                            os = "Windows 11 64-bit",
                            ram = "16 GB DDR4",
                            storage = "1 TB NVMe M.2 SSD",
                            cpu = "Intel Core i7",
                            gpu = "NVIDIA GeForce RTX 3060"
                        )
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.FRENCH
        }
    }

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        when (lang) {
            AppLanguage.FRENCH -> tts?.language = Locale.FRENCH
            AppLanguage.ENGLISH -> tts?.language = Locale.ENGLISH
            AppLanguage.HEBREW -> tts?.language = Locale("he")
        }
    }

    fun toggleTechnicianMode() {
        _isTechnicianMode.value = !_isTechnicianMode.value
    }

    fun togglePremium() {
        _isPremium.value = !_isPremium.value
    }

    fun submitTextDiagnostic(problem: String, category: String, device: String) {
        if (problem.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "FixAI analyse ton problème informatique..."
            try {
                val result = repository.runTextDiagnostic(problem, category, device)
                _currentDiagnostic.value = result
                _currentScreen.value = ScreenDestination.DIAGNOSTIC_RESULT
            } catch (e: Exception) {
                // Fallback handled
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitImageDiagnostic(bitmap: Bitmap, problem: String, category: String, device: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "FixAI examine la photo et extrait les composants..."
            try {
                val result = repository.runImageDiagnostic(bitmap, problem, category, device)
                _currentDiagnostic.value = result
                _currentScreen.value = ScreenDestination.DIAGNOSTIC_RESULT
            } catch (e: Exception) {
                // Handled
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startInteractiveDiagnostic() {
        val trees = repository.getInteractiveTrees()
        val rootNode = trees.firstOrNull { it.id == "root" }
        _interactiveCurrentNode.value = rootNode
        _currentScreen.value = ScreenDestination.INTERACTIVE_TREE
    }

    fun answerInteractiveQuestion(answerYes: Boolean) {
        val currentNode = _interactiveCurrentNode.value ?: return
        val nextId = if (answerYes) currentNode.optionYesNodeId else currentNode.optionNoNodeId
        
        val trees = repository.getInteractiveTrees()
        val nextNode = trees.firstOrNull { it.id == nextId }

        if (nextNode?.finalDiagnostic != null) {
            val diag = nextNode.finalDiagnostic
            viewModelScope.launch {
                repository.saveDiagnostic(diag)
            }
            _currentDiagnostic.value = diag
            _currentScreen.value = ScreenDestination.DIAGNOSTIC_RESULT
        } else if (nextNode != null) {
            _interactiveCurrentNode.value = nextNode
        }
    }

    fun runHardwareTests() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Analyse des composants matériels..."
            val results = repository.runHardwareDiagnosticSuite()
            _hardwareTestResults.value = results
            _isLoading.value = false
            _currentScreen.value = ScreenDestination.HARDWARE_TESTS
        }
    }

    fun markDiagnosticResolved(id: String, resolved: Boolean) {
        viewModelScope.launch {
            repository.updateDiagnosticStatus(id, resolved)
            val updated = _currentDiagnostic.value
            if (updated?.id == id) {
                _currentDiagnostic.value = updated.copy(statusResolved = resolved)
            }
        }
    }

    fun selectDiagnosticFromHistory(diag: DiagnosticResult) {
        _currentDiagnostic.value = diag
        _currentScreen.value = ScreenDestination.DIAGNOSTIC_RESULT
    }

    fun deleteDiagnosticHistory(id: String) {
        viewModelScope.launch {
            repository.deleteDiagnostic(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun saveUserDevice(device: DeviceEntity) {
        viewModelScope.launch {
            repository.saveDevice(device)
        }
    }

    fun deleteUserDevice(id: Int) {
        viewModelScope.launch {
            repository.deleteDevice(id)
        }
    }

    fun speakText(text: String) {
        if (_isSpeaking.value) {
            tts?.stop()
            _isSpeaking.value = false
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FixAiTTS")
            _isSpeaking.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
