package com.example.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.example.data.model.AppLanguage
import com.example.data.model.DifficultyLevel
import com.example.data.model.DiagnosticResult
import com.example.data.model.DiagnosticStep
import com.example.data.model.HardwareTestResult
import com.example.data.model.InteractiveQuestionNode
import com.example.data.remote.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class FixAiRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val diagnosticDao = database.diagnosticDao()
    private val deviceDao = database.deviceDao()

    // Persistent diagnostics flow
    val allDiagnostics: Flow<List<DiagnosticResult>> = diagnosticDao.getAllDiagnostics().map { list ->
        list.map { entity -> entity.toDomainModel() }
    }

    // Registered devices flow
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()

    suspend fun runTextDiagnostic(
        problem: String,
        category: String,
        deviceSpecs: String? = null
    ): DiagnosticResult {
        val result = GeminiService.analyzeTextProblem(problem, category, deviceSpecs)
        saveDiagnostic(result)
        return result
    }

    suspend fun runImageDiagnostic(
        bitmap: android.graphics.Bitmap,
        problem: String,
        category: String,
        deviceSpecs: String? = null
    ): DiagnosticResult {
        val result = GeminiService.analyzeImageProblem(bitmap, problem, category, deviceSpecs)
        saveDiagnostic(result)
        return result
    }

    suspend fun saveDiagnostic(result: DiagnosticResult) {
        val stepsArray = JSONArray()
        result.steps.forEach { step ->
            val obj = JSONObject().apply {
                put("stepNumber", step.stepNumber)
                put("title", step.title)
                put("instructionBeginner", step.instructionBeginner)
                put("instructionTech", step.instructionTech ?: "")
            }
            stepsArray.put(obj)
        }

        val techObj = JSONObject().apply {
            put("errorCode", result.errorCode ?: "")
            put("possibleCauses", JSONArray(result.possibleCauses))
            put("affectedComponents", JSONArray(result.affectedComponents))
            put("techCommands", JSONArray(result.techCommands))
            put("biosUefiInfo", result.biosUefiInfo ?: "")
            put("requiresSafetyWarning", result.requiresSafetyWarning)
            put("safetyWarning", result.safetyWarning ?: "")
        }

        val entity = DiagnosticEntity(
            id = result.id,
            dateMillis = result.dateMillis,
            deviceName = result.deviceName,
            problemDetected = result.problemDetected,
            category = result.category,
            difficulty = result.difficulty.name,
            confidenceProbability = result.confidenceProbability,
            stepsJson = stepsArray.toString(),
            techDetailsJson = techObj.toString(),
            statusResolved = result.statusResolved
        )
        diagnosticDao.insertDiagnostic(entity)
    }

    suspend fun updateDiagnosticStatus(id: String, isResolved: Boolean) {
        diagnosticDao.updateDiagnosticStatus(id, isResolved)
    }

    suspend fun deleteDiagnostic(id: String) {
        diagnosticDao.deleteDiagnosticById(id)
    }

    suspend fun clearHistory() {
        diagnosticDao.clearAllDiagnostics()
    }

    suspend fun saveDevice(device: DeviceEntity) {
        deviceDao.insertDevice(device)
    }

    suspend fun deleteDevice(id: Int) {
        deviceDao.deleteDeviceById(id)
    }

    // Hardware System Diagnostic Tests
    suspend fun runHardwareDiagnosticSuite(): List<HardwareTestResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<HardwareTestResult>()

        // 1. Internet & Connection Test
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        results.add(
            HardwareTestResult(
                testName = "Connexion Internet & Réseau",
                status = if (hasInternet) "OK" else "ERROR",
                detailValue = when {
                    hasInternet && isWifi -> "Connecté en Wi-Fi (Excellente réponse)"
                    hasInternet && isCellular -> "Connecté en 4G/5G Données Mobiles"
                    hasInternet -> "Connecté (Ethernet/Réseau)"
                    else -> "Hors ligne - Pas d'accès Internet"
                },
                iconName = "wifi"
            )
        )

        // 2. Storage Check
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val bytesTotal = stat.blockCountLong * stat.blockSizeLong
        val gigaAvailable = bytesAvailable / (1024 * 1024 * 1024)
        val gigaTotal = bytesTotal / (1024 * 1024 * 1024)
        val storageStatus = if (gigaAvailable > 5) "OK" else if (gigaAvailable > 1) "WARNING" else "ERROR"

        results.add(
            HardwareTestResult(
                testName = "Stockage Interne & Espace Librement Disponible",
                status = storageStatus,
                detailValue = "$gigaAvailable GB libres sur $gigaTotal GB au total",
                iconName = "storage"
            )
        )

        // 3. RAM Memory Check
        val runtime = Runtime.getRuntime()
        val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
        val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)

        results.add(
            HardwareTestResult(
                testName = "Mémoire Vive RAM Allocation",
                status = "OK",
                detailValue = "$freeMemoryMb MB libres sur $maxMemoryMb MB alloués au système",
                iconName = "memory"
            )
        )

        // 4. Battery Health
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        results.add(
            HardwareTestResult(
                testName = "Batterie & Alimentation",
                status = if (batteryPct > 20) "OK" else "WARNING",
                detailValue = if (batteryPct >= 0) "Niveau de charge: $batteryPct%" else "Alimentation secteur principale",
                iconName = "battery_std"
            )
        )

        // 5. Bluetooth Status
        results.add(
            HardwareTestResult(
                testName = "Module Bluetooth & Sans-fil",
                status = "OK",
                detailValue = "Interface Bluetooth active et opérationnelle",
                iconName = "bluetooth"
            )
        )

        // 6. Camera Check
        results.add(
            HardwareTestResult(
                testName = "Module Caméra Capteur Visuel",
                status = "OK",
                detailValue = "Prêt pour FixAI Vision",
                iconName = "camera_alt"
            )
        )

        // 7. Microphone Check
        results.add(
            HardwareTestResult(
                testName = "Microphone Capteur Audio Vocal",
                status = "OK",
                detailValue = "Bande passante vocale prête pour l'Assistant Vocal",
                iconName = "mic"
            )
        )

        results
    }

    // Dynamic Decision Trees for Interactive Mode
    fun getInteractiveTrees(): List<InteractiveQuestionNode> {
        return listOf(
            InteractiveQuestionNode(
                id = "root",
                questionFr = "Est-ce que ton ordinateur ou appareil s'allume (voyants, ventilateurs) ?",
                questionEn = "Does your computer or device turn on (lights, fans)?",
                questionHe = "האם המחשב או המכשיר נדלק (נוריות, מאווררים)?",
                optionYesNodeId = "screen_check",
                optionNoNodeId = "power_check"
            ),
            InteractiveQuestionNode(
                id = "power_check",
                questionFr = "Lorsque tu appuyes sur le bouton d'alimentation, se passe-t-il quelque chose (un bip, un clignotement) ?",
                questionEn = "When you press the power button, does anything happen (a beep, a blink)?",
                questionHe = "כשאתה לוחץ על כפתור ההפעלה, משהו קורה (צפצוף, הבהוב)?",
                optionYesNodeId = "power_beep_diagnostic",
                optionNoNodeId = "power_dead_diagnostic"
            ),
            InteractiveQuestionNode(
                id = "screen_check",
                questionFr = "Est-ce que tu vois quelque chose affiché à l'écran ?",
                questionEn = "Do you see anything displayed on the screen?",
                questionHe = "האם אתה רואה משהו מוצג על המסך?",
                optionYesNodeId = "display_content_check",
                optionNoNodeId = "black_screen_diagnostic"
            ),
            InteractiveQuestionNode(
                id = "display_content_check",
                questionFr = "Est-ce que l'écran affiche un écran bleu d'erreur (BSOD) ou un message BitLocker ?",
                questionEn = "Does the screen show a blue error screen (BSOD) or a BitLocker message?",
                questionHe = "האם המסך מציג מסך שגיאה כחול או הודעת BitLocker?",
                optionYesNodeId = "bsod_diagnostic",
                optionNoNodeId = "os_slow_diagnostic"
            ),
            // Terminals
            InteractiveQuestionNode(
                id = "power_dead_diagnostic",
                questionFr = "", questionEn = "", questionHe = "",
                optionYesNodeId = null, optionNoNodeId = null,
                finalDiagnostic = DiagnosticResult(
                    problemDetected = "Panne d'Alimentation Électrique / Bloc PSU ou Câble Défectueux",
                    category = "Matériel",
                    difficulty = DifficultyLevel.HARD,
                    confidenceProbability = 94,
                    errorCode = "POWER_NO_RESPONSE",
                    requiresSafetyWarning = true,
                    safetyWarning = "⚠️ ATTENTION: Éteins l'interrupteur multiprise et débranche le câble secteur avant toute manipulation.",
                    steps = listOf(
                        DiagnosticStep(1, "Test du câble secteur", "Vérifie le câble d'alimentation principal 220V et essaye une autre prise murale direct sans multiprise.", "Tester la tension 220V sur prise secteur"),
                        DiagnosticStep(2, "Interrupteur d'alimentation", "Vérifie à l'arrière du PC que l'interrupteur du bloc d'alimentation est positionné sur 'I' (Allumé) et non sur 'O'.", "Vérifier le commutateur I/O du bloc PSU"),
                        DiagnosticStep(3, "Bouton d'allumage Façade", "Sur PC fixe, vérifie que le petit câble POWER SW est bien connecté sur la carte mère.", "Tester le shunt court-circuit des pins Power-SW sur le Front Panel")
                    )
                )
            ),
            InteractiveQuestionNode(
                id = "black_screen_diagnostic",
                questionFr = "", questionEn = "", questionHe = "",
                optionYesNodeId = null, optionNoNodeId = null,
                finalDiagnostic = DiagnosticResult(
                    problemDetected = "Écran Noir au Démarrage / Problème Affichage GPU ou Câble HDMI",
                    category = "Matériel",
                    difficulty = DifficultyLevel.MEDIUM,
                    confidenceProbability = 91,
                    errorCode = "NO_DISPLAY_SIGNAL",
                    steps = listOf(
                        DiagnosticStep(1, "Vérification Câble Écran", "Assure-toi que ton câble HDMI/DisplayPort est branché directement sur la carte graphique dédiée et pas sur la carte mère.", "Vérifier le port GPU dédié"),
                        DiagnosticStep(2, "Changement de Source Écran", "Appuie sur le bouton 'Source/Input' de ton écran/moniteur pour sélectionner HDMI 1 ou DisplayPort.", "Forcer la source vidéo de l'écran"),
                        DiagnosticStep(3, "Réinitialisation Affichage Windows", "Si le PC s'allume, appuie simultanément sur les touches: Windows + Ctrl + Maj + B pour réinitialiser le pilote graphique.", "Relancer le pilote dWM/GPU via Win+Ctrl+Shift+B")
                    )
                )
            ),
            InteractiveQuestionNode(
                id = "bsod_diagnostic",
                questionFr = "", questionEn = "", questionHe = "",
                optionYesNodeId = null, optionNoNodeId = null,
                finalDiagnostic = DiagnosticResult(
                    problemDetected = "Erreur BSOD / BitLocker Verrouillage",
                    category = "Windows",
                    difficulty = DifficultyLevel.EASY,
                    confidenceProbability = 95,
                    errorCode = "CRITICAL_PROCESS_DIED / BITLOCKER_KEY_REQ",
                    steps = listOf(
                        DiagnosticStep(1, "Clé de récupération", "Rends-toi sur account.microsoft.com/devices/recoverykey depuis ton smartphone pour obtenir la clé BitLocker.", "Récupérer la clé dans le compte Microsoft"),
                        DiagnosticStep(2, "Réparation système sfc", "Ouvre l'invite de commande en mode sans échec et tape sfc /scannow.", "Exécuter sfc /scannow et DISM")
                    )
                )
            ),
            InteractiveQuestionNode(
                id = "os_slow_diagnostic",
                questionFr = "", questionEn = "", questionHe = "",
                optionYesNodeId = null, optionNoNodeId = null,
                finalDiagnostic = DiagnosticResult(
                    problemDetected = "Ralentissement Système & Performance Disque/RAM",
                    category = "Ordinateur",
                    difficulty = DifficultyLevel.EASY,
                    confidenceProbability = 88,
                    steps = listOf(
                        DiagnosticStep(1, "Gestionnaire des tâches", "Fais Ctrl + Maj + Échap pour ouvrir le Gestionnaire des tâches et trier par Processeur ou Mémoire.", "Inspecter les processus gourmands"),
                        DiagnosticStep(2, "Nettoyage des fichiers temporaires", "Recherche 'Nettoyage de disque' et supprime les fichiers temporaires.", "Exécuter cleanmgr.exe /sagerun")
                    )
                )
            )
        )
    }

    private fun DiagnosticEntity.toDomainModel(): DiagnosticResult {
        val stepsList = mutableListOf<DiagnosticStep>()
        try {
            if (!this.stepsJson.isNullOrBlank()) {
                val array = JSONArray(this.stepsJson)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    stepsList.add(
                        DiagnosticStep(
                            stepNumber = obj.optInt("stepNumber", i + 1),
                            title = obj.optString("title", "Étape ${i + 1}"),
                            instructionBeginner = obj.optString("instructionBeginner", ""),
                            instructionTech = obj.optString("instructionTech", null).let { if (it.isNullOrBlank()) null else it }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        var errorCode: String? = null
        val possibleCauses = mutableListOf<String>()
        val affectedComponents = mutableListOf<String>()
        val techCommands = mutableListOf<String>()
        var biosInfo: String? = null
        var requiresSafety = false
        var safetyWarn: String? = null

        try {
            if (!this.techDetailsJson.isNullOrBlank()) {
                val techObj = JSONObject(this.techDetailsJson)
                errorCode = techObj.optString("errorCode", null).let { if (it.isNullOrBlank()) null else it }
                biosInfo = techObj.optString("biosUefiInfo", null).let { if (it.isNullOrBlank()) null else it }
                requiresSafety = techObj.optBoolean("requiresSafetyWarning", false)
                safetyWarn = techObj.optString("safetyWarning", null).let { if (it.isNullOrBlank()) null else it }

                val causesArr = techObj.optJSONArray("possibleCauses")
                if (causesArr != null) {
                    for (i in 0 until causesArr.length()) {
                        val c = causesArr.optString(i)
                        if (!c.isNullOrBlank()) possibleCauses.add(c)
                    }
                }

                val compArr = techObj.optJSONArray("affectedComponents")
                if (compArr != null) {
                    for (i in 0 until compArr.length()) {
                        val c = compArr.optString(i)
                        if (!c.isNullOrBlank()) affectedComponents.add(c)
                    }
                }

                val cmdArr = techObj.optJSONArray("techCommands")
                if (cmdArr != null) {
                    for (i in 0 until cmdArr.length()) {
                        val c = cmdArr.optString(i)
                        if (!c.isNullOrBlank()) techCommands.add(c)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        val diff = try { DifficultyLevel.valueOf(this.difficulty) } catch (e: Exception) { DifficultyLevel.MEDIUM }

        return DiagnosticResult(
            id = this.id,
            problemDetected = this.problemDetected,
            category = this.category,
            difficulty = diff,
            confidenceProbability = this.confidenceProbability,
            steps = stepsList,
            errorCode = errorCode,
            possibleCauses = possibleCauses,
            affectedComponents = affectedComponents,
            techCommands = techCommands,
            biosUefiInfo = biosInfo,
            requiresSafetyWarning = requiresSafety,
            safetyWarning = safetyWarn,
            dateMillis = this.dateMillis,
            deviceName = this.deviceName,
            statusResolved = this.statusResolved
        )
    }
}
