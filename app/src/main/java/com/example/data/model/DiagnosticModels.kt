package com.example.data.model

import androidx.compose.runtime.Immutable

enum class AppLanguage(val code: String, val label: String, val flag: String) {
    FRENCH("fr", "Français", "🇫🇷"),
    ENGLISH("en", "English", "🇬🇧"),
    HEBREW("he", "עברית", "🇮🇱")
}

enum class DifficultyLevel(val labelFr: String, val labelEn: String, val labelHe: String) {
    EASY("Facile", "Easy", "קל"),
    MEDIUM("Moyen", "Medium", "בינוני"),
    HARD("Difficile", "Hard", "קשה")
}

enum class DiagnosticCategory(val id: String, val labelFr: String, val iconName: String) {
    COMPUTER("computer", "Ordinateur", "computer"),
    HARDWARE("hardware", "Matériel", "build"),
    NETWORK("network", "Réseau", "wifi"),
    WINDOWS("windows", "Windows", "window"),
    SMARTPHONE("smartphone", "Smartphones", "smartphone"),
    GAMING("gaming", "Gaming", "sports_esports")
}

@Immutable
data class DiagnosticStep(
    val stepNumber: Int,
    val title: String,
    val instructionBeginner: String,
    val instructionTech: String? = null,
    val requiresSafetyWarning: Boolean = false,
    val safetyWarningText: String? = null
)

@Immutable
data class DiagnosticResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val problemDetected: String,
    val category: String,
    val difficulty: DifficultyLevel,
    val confidenceProbability: Int, // 0-100%
    val steps: List<DiagnosticStep>,
    val errorCode: String? = null,
    val possibleCauses: List<String> = emptyList(),
    val affectedComponents: List<String> = emptyList(),
    val techCommands: List<String> = emptyList(),
    val biosUefiInfo: String? = null,
    val requiresSafetyWarning: Boolean = false,
    val safetyWarning: String? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val deviceName: String = "PC",
    val statusResolved: Boolean? = null // true = Résolu, false = Toujours en panne, null = Pending
)

@Immutable
data class InteractiveQuestionNode(
    val id: String,
    val questionFr: String,
    val questionEn: String,
    val questionHe: String,
    val optionYesNodeId: String?,
    val optionNoNodeId: String?,
    val finalDiagnostic: DiagnosticResult? = null
)

@Immutable
data class HardwareTestResult(
    val testName: String,
    val status: String, // "OK", "WARNING", "ERROR", "TESTING"
    val detailValue: String,
    val iconName: String
)
