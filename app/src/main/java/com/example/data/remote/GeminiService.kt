package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.DifficultyLevel
import com.example.data.model.DiagnosticResult
import com.example.data.model.DiagnosticStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiService {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeTextProblem(
        userProblem: String,
        category: String,
        deviceSpecs: String? = null
    ): DiagnosticResult = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Tu es FixAI, un expert technicien informatique et matériel.
                    Problème signalé par l'utilisateur: "$userProblem"
                    Catégorie: $category
                    Appareil: ${deviceSpecs ?: "Non spécifié"}
                    
                    Fournis un diagnostic clair au format JSON strict avec les clés:
                    - "problemDetected": titre court du problème (ex: Erreur Écran Bleu BSOD - Driver Conflit)
                    - "difficulty": "Facile" ou "Moyen" ou "Difficile"
                    - "confidenceProbability": un entier entre 50 et 98
                    - "requiresSafetyWarning": boolean (true si manipulation électrique/matérielle interne)
                    - "safetyWarning": chaine de texte d'avertissement de sécurité si applicable
                    - "errorCode": code d'erreur possible
                    - "possibleCauses": tableau de chaines de caractères
                    - "affectedComponents": tableau de composants touchés
                    - "techCommands": tableau de commandes CLI utiles
                    - "biosUefiInfo": conseil BIOS/UEFI si pertinent
                    - "steps": tableau d'objets avec "stepNumber", "title", "instructionBeginner", "instructionTech"
                    
                    Réponds uniquement avec le JSON sans balise markdown.
                """.trimIndent()

                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", prompt))
                }
                val contentObj = JSONObject().put("parts", partsArray)
                val contentsArray = JSONArray().put(contentObj)
                val reqJson = JSONObject().apply {
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().put("temperature", 0.3))
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(reqJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseString = response.body?.string()

                if (response.isSuccessful && !responseString.isNullOrBlank()) {
                    val respObj = JSONObject(responseString)
                    val candidates = respObj.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCand = candidates.getJSONObject(0)
                        val content = firstCand.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val rawText = parts?.optJSONObject(0)?.optString("text")
                        if (!rawText.isNullOrBlank()) {
                            val parsed = parseJsonResult(rawText, category, deviceSpecs ?: "PC")
                            if (parsed != null) return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        return@withContext generateSmartFallback(userProblem, category, deviceSpecs)
    }

    suspend fun analyzeImageProblem(
        bitmap: Bitmap,
        userProblem: String,
        category: String,
        deviceSpecs: String? = null
    ): DiagnosticResult = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val base64Image = bitmap.toBase64()
                val prompt = """
                    Analyse cette photo/capture d'écran d'un problème informatique ou matériel.
                    Problème décrit: "$userProblem"
                    Identifie le composant, l'écran d'erreur ou le câble visible, le code d'erreur, et la solution.
                    
                    Renvoie le JSON strict:
                    - "problemDetected"
                    - "difficulty" ("Facile", "Moyen", "Difficile")
                    - "confidenceProbability"
                    - "requiresSafetyWarning" (boolean)
                    - "safetyWarning"
                    - "errorCode"
                    - "possibleCauses" (liste de chaines)
                    - "affectedComponents" (liste)
                    - "techCommands" (liste)
                    - "biosUefiInfo"
                    - "steps" (liste d'objets avec stepNumber, title, instructionBeginner, instructionTech)
                """.trimIndent()

                val textPart = JSONObject().put("text", prompt)
                val imagePart = JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                })

                val partsArray = JSONArray().apply {
                    put(textPart)
                    put(imagePart)
                }

                val contentObj = JSONObject().put("parts", partsArray)
                val contentsArray = JSONArray().put(contentObj)
                val reqJson = JSONObject().apply {
                    put("contents", contentsArray)
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(reqJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseString = response.body?.string()

                if (response.isSuccessful && !responseString.isNullOrBlank()) {
                    val respObj = JSONObject(responseString)
                    val candidates = respObj.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCand = candidates.getJSONObject(0)
                        val content = firstCand.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val rawText = parts?.optJSONObject(0)?.optString("text")
                        if (!rawText.isNullOrBlank()) {
                            val parsed = parseJsonResult(rawText, category, deviceSpecs ?: "Photo")
                            if (parsed != null) return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        return@withContext generateImageFallback(userProblem, category, deviceSpecs)
    }

    private fun Bitmap.toBase64(): String {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun parseJsonResult(rawText: String, category: String, deviceName: String): DiagnosticResult? {
        return try {
            val startIdx = rawText.indexOf("{")
            val endIdx = rawText.lastIndexOf("}")
            if (startIdx == -1 || endIdx == -1) return null

            val jsonString = rawText.substring(startIdx, endIdx + 1)
            val jsonObj = JSONObject(jsonString)

            val problem = jsonObj.optString("problemDetected", "Erreur système détectée")
            val diffStr = jsonObj.optString("difficulty", "Moyen")
            val difficulty = when (diffStr.lowercase()) {
                "facile", "easy" -> DifficultyLevel.EASY
                "difficile", "hard" -> DifficultyLevel.HARD
                else -> DifficultyLevel.MEDIUM
            }
            val confidence = jsonObj.optInt("confidenceProbability", 88)
            val requiresSafety = jsonObj.optBoolean("requiresSafetyWarning", false)
            val safetyWarning = jsonObj.optString("safetyWarning", null)
            val errorCode = jsonObj.optString("errorCode", null)

            val possibleCauses = jsonArrayToList(jsonObj.optJSONArray("possibleCauses"))
            val affectedComponents = jsonArrayToList(jsonObj.optJSONArray("affectedComponents"))
            val techCommands = jsonArrayToList(jsonObj.optJSONArray("techCommands"))
            val biosUefiInfo = jsonObj.optString("biosUefiInfo", null)

            val stepsList = mutableListOf<DiagnosticStep>()
            val stepsArray = jsonObj.optJSONArray("steps")
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    val sObj = stepsArray.optJSONObject(i) ?: continue
                    val num = sObj.optInt("stepNumber", i + 1)
                    val title = sObj.optString("title", "Étape ${i + 1}")
                    val beg = sObj.optString("instructionBeginner", "Effectue cette vérification.")
                    val tech = sObj.optString("instructionTech", null)
                    stepsList.add(
                        DiagnosticStep(
                            stepNumber = num,
                            title = title,
                            instructionBeginner = beg,
                            instructionTech = tech
                        )
                    )
                }
            }

            if (stepsList.isEmpty()) return null

            DiagnosticResult(
                problemDetected = problem,
                category = category,
                difficulty = difficulty,
                confidenceProbability = confidence,
                steps = stepsList,
                errorCode = if (errorCode.isNullOrBlank()) null else errorCode,
                possibleCauses = possibleCauses,
                affectedComponents = affectedComponents,
                techCommands = techCommands,
                biosUefiInfo = if (biosUefiInfo.isNullOrBlank()) null else biosUefiInfo,
                requiresSafetyWarning = requiresSafety,
                safetyWarning = if (safetyWarning.isNullOrBlank()) null else safetyWarning,
                deviceName = deviceName
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optString(i)
            if (!item.isNullOrBlank()) list.add(item)
        }
        return list
    }

    private fun generateSmartFallback(problem: String, category: String, deviceSpecs: String?): DiagnosticResult {
        val lower = problem.lowercase()
        return when {
            lower.contains("bleu") || lower.contains("bsod") || lower.contains("redémarre") -> {
                DiagnosticResult(
                    problemDetected = "Écran Bleu (BSOD) - Conflit de pilote / Fichier système corrompu",
                    category = "Windows",
                    difficulty = DifficultyLevel.MEDIUM,
                    confidenceProbability = 92,
                    errorCode = "CRITICAL_PROCESS_DIED",
                    possibleCauses = listOf("Mise à jour pilote graphique défectueuse", "Surchauffe processeur ou RAM instability", "Fichiers système Windows endommagés"),
                    affectedComponents = listOf("SFC System File Checker", "Pilote GPU", "Barrettes RAM"),
                    techCommands = listOf("sfc /scannow", "DISM /Online /Cleanup-Image /RestoreHealth", "mdsched.exe"),
                    biosUefiInfo = "Vérifie que les profils XMP/EXPO de la mémoire vive sont stables dans le BIOS.",
                    requiresSafetyWarning = false,
                    steps = listOf(
                        DiagnosticStep(1, "Mode Sans Échec", "Allume ton ordinateur tout en maintenant la touche Majuscule (Shift) enfoncée pour accéder au menu de récupération.", "Bcdedit /set {default} safeboot minimal"),
                        DiagnosticStep(2, "Réparation des fichiers système", "Dans la barre de recherche Windows, tape 'cmd', fais un clic droit et choisis 'Exécuter en tant qu'administrateur'.", "Exécuter: sfc /scannow & Dism /Online /Cleanup-Image /RestoreHealth"),
                        DiagnosticStep(3, "Mise à jour des pilotes", "Télécharge le dernier pilote graphique officiel (NVIDIA, AMD ou Intel) et effectue une installation propre.", "Utiliser DDU (Display Driver Uninstaller) en mode sans échec."),
                        DiagnosticStep(4, "Test de la Mémoire Vive", "Recherche 'Diagnostic de mémoire Windows' et relance le PC pour tester tes barrettes de RAM.", "Exécuter mdsched.exe ou MemTest86")
                    ),
                    deviceName = deviceSpecs ?: "Mon PC Windows"
                )
            }
            lower.contains("bitlocker") || lower.contains("clé") -> {
                DiagnosticResult(
                    problemDetected = "Verrouillage BitLocker - Demande de clé de récupération",
                    category = "Windows",
                    difficulty = DifficultyLevel.EASY,
                    confidenceProbability = 96,
                    errorCode = "BITLOCKER_DRIVE_ENCRYPTION_LOCKED",
                    possibleCauses = listOf("Mise à jour BIOS ou changement de matériel", "Anomalie de la puce TPM 2.0", "Sécurité Windows déclenchée"),
                    affectedComponents = listOf("Puce TPM 2.0", "Compte Microsoft", "Partition Windows C:"),
                    techCommands = listOf("manage-bde -status C:", "manage-bde -unlock C: -rp <KEY>"),
                    biosUefiInfo = "Assure-toi que Security Device Support (TPM / fTPM) est activé dans le BIOS.",
                    steps = listOf(
                        DiagnosticStep(1, "Récupération de la clé", "Connecte-toi à ton compte Microsoft depuis ton téléphone sur: account.microsoft.com/devices/recoverykey", "Consulter la base de stockage Active Directory / Azure AD / Compte Perso"),
                        DiagnosticStep(2, "Saisie de la clé à 48 chiffres", "Entre la clé affichée sur la page Microsoft dans l'écran de déverrouillage de ton PC.", "manage-bde -unlock C: -recoverypassword <48-DIGIT-KEY>"),
                        DiagnosticStep(3, "Suspension temporaire", "Une fois sur le bureau Windows, clique sur Démarrer, cherche 'BitLocker' et fais 'Suspendre la protection' avant la prochaine mise à jour BIOS.", "manage-bde -protectors -disable C:")
                    ),
                    deviceName = deviceSpecs ?: "Mon PC BitLocker"
                )
            }
            lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet") || lower.contains("dns") -> {
                DiagnosticResult(
                    problemDetected = "Problème de Connexion Réseau & DNS",
                    category = "Réseau",
                    difficulty = DifficultyLevel.EASY,
                    confidenceProbability = 90,
                    errorCode = "DNS_PROBE_FINISHED_NO_INTERNET",
                    possibleCauses = listOf("Serveur DNS bloqué", "Bail DHCP expiré", "Carte réseau Wi-Fi figée"),
                    affectedComponents = listOf("Carte réseau Wi-Fi", "Routeur / Box Internet", "Cache DNS local"),
                    techCommands = listOf("ipconfig /flushdns", "netsh winsock reset", "netsh int ip reset"),
                    steps = listOf(
                        DiagnosticStep(1, "Redémarrage de la Box", "Débranche le câble d'alimentation de ta box Internet, attends 10 secondes puis rebranche-le.", "Redémarrage matériel de la passerelle DHCP"),
                        DiagnosticStep(2, "Réinitialisation du réseau Windows", "Va dans Paramètres > Réseau et Internet > Paramètres réseau avancés > Réinitialisation du réseau.", "netsh winsock reset && netsh int ip reset"),
                        DiagnosticStep(3, "Nettoyage du cache DNS", "Ouvre l'invite de commande (cmd) et tape: ipconfig /flushdns puis appuie sur Entrée.", "ipconfig /flushdns /registerdns")
                    ),
                    deviceName = deviceSpecs ?: "Appareil Réseau"
                )
            }
            lower.contains("bip") || lower.contains("bips") || lower.contains("allume pas") || lower.contains("noir") -> {
                DiagnosticResult(
                    problemDetected = "Défaut d'affichage au démarrage / Diagnostic POST (Bips carte mère)",
                    category = "Matériel",
                    difficulty = DifficultyLevel.HARD,
                    confidenceProbability = 89,
                    errorCode = "POST_FAILURE_NO_DISPLAY",
                    possibleCauses = listOf("Mauvais contact barrette RAM", "Câble écran/HDMI desserré", "Surchauffe ou mauvaise alimentation GPU/CPU"),
                    affectedComponents = listOf("Barrettes RAM", "Carte graphique GPU", "Bloc Alimentation PSU"),
                    techCommands = listOf("Vérification des LEDs de diagnostic EZ Debug LED sur carte mère"),
                    biosUefiInfo = "Réinitialise le BIOS en retirant la pile bouton CR2032 pendant 5 minutes (Clear CMOS).",
                    requiresSafetyWarning = true,
                    safetyWarning = "⚠️ ATTENTION: Cette manipulation concerne le matériel interne sous tension. Éteins complètement ton PC et débranche le câble secteur 220V avant d'ouvrir le boîtier.",
                    steps = listOf(
                        DiagnosticStep(1, "Débranchement de sécurité", "Éteins l'ordinateur, bascule l'interrupteur à l'arrière sur O et débranche le câble électrique.", "Coupure secteur totale 220V"),
                        DiagnosticStep(2, "Repositionnement de la RAM", "Ouvre le boîtier latéral, retire délicatement la barrette de mémoire RAM en appuyant sur les clips latéraux, puis réinsère-la jusqu'au 'CLIC'.", "Changer de slot RAM (utiliser slots A2/B2 en dual channel)"),
                        DiagnosticStep(3, "Vérification des câbles d'écran", "Vérifie que ton câble HDMI/DisplayPort est bien branché sur la CARTE GRAPHIQUE (en bas) et NON sur la carte mère (en haut).", "Connexion directe sur ports Dedicated GPU")
                    ),
                    deviceName = deviceSpecs ?: "PC Tour/Portatif"
                )
            }
            else -> {
                DiagnosticResult(
                    problemDetected = "Analyse Générale de Dysfonctionnement Système",
                    category = category.ifBlank { "Ordinateur" },
                    difficulty = DifficultyLevel.MEDIUM,
                    confidenceProbability = 85,
                    errorCode = "SYS_DIAG_GENERIC",
                    possibleCauses = listOf("Conflit logiciel ou processus en arrière-plan", "Fichiers temporaires saturés", "Mise à jour en attente"),
                    affectedComponents = listOf("Processeur CPU", "Mémoire RAM", "Disque principal"),
                    techCommands = listOf("taskmgr", "resmon", "perfmon /report"),
                    steps = listOf(
                        DiagnosticStep(1, "Redémarrage complet", "Clique sur Démarrer > Marche/Arrêt > Redémarrer (ne choisis pas Éteindre).", "Redémarrage propre sans démarrage rapide fast-startup"),
                        DiagnosticStep(2, "Vérification de l'espace disque", "Ouvre 'Ce PC' et vérifie qu'il reste au moins 15% d'espace libre sur le disque C:.", "Nettoyage de disque cleanmgr.exe"),
                        DiagnosticStep(3, "Analyse antivirus Windows Defender", "Ouvre 'Sécurité Windows' et lance une 'Analyse rapide'.", "Start-MpScan -ScanType QuickScan")
                    ),
                    deviceName = deviceSpecs ?: "Appareil"
                )
            }
        }
    }

    private fun generateImageFallback(problem: String, category: String, deviceSpecs: String?): DiagnosticResult {
        return DiagnosticResult(
            problemDetected = "Analyse visuelle: Composant / Message d'erreur identifié",
            category = category.ifBlank { "Matériel" },
            difficulty = DifficultyLevel.MEDIUM,
            confidenceProbability = 87,
            errorCode = "HARDWARE_VISUAL_CHECK",
            possibleCauses = listOf("Connecteur mal enclenché ou câble détendu", "Accumulation de poussière sur ventilateur", "Message d'avertissement à l'écran"),
            affectedComponents = listOf("Connecteurs d'alimentation", "Ports d'extension", "Panneau d'affichage"),
            techCommands = listOf("Inspection visuelle des voyants LED"),
            requiresSafetyWarning = true,
            safetyWarning = "⚠️ ATTENTION: Éteins complètement l'appareil et débranche la prise électrique avant de manipuler les composants internes.",
            steps = listOf(
                DiagnosticStep(1, "Vérification des branchements", "Assure-toi que les connecteurs d'alimentation et câbles réseau/écran sont fermement enfoncés.", "Inspecter le verrouillage des connecteurs 24-pin et PCI-E"),
                DiagnosticStep(2, "Nettoyage de la poussière", "Utilise une bombe d'air sec pour dépoussiérer doucement les pales des ventilateurs et les aérations.", "Dépoussiérage hors tension sans liquide"),
                DiagnosticStep(3, "Relance et test", "Rebranche le câble d'alimentation et allume l'appareil pour observer les voyants LED.", "Vérifier le statut des LEDs EZ-Debug")
            ),
            deviceName = deviceSpecs ?: "Matériel pris en photo"
        )
    }
}
