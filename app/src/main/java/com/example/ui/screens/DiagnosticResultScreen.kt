package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiagnosticResult
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.SafetyWarningCard
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleElectric
import com.example.ui.theme.TechDanger
import com.example.ui.theme.TechGreen

@Composable
fun DiagnosticResultScreen(
    diagnostic: DiagnosticResult?,
    isTechnicianMode: Boolean,
    onToggleTechnicianMode: () -> Unit,
    isSpeaking: Boolean,
    onSpeakText: (String) -> Unit,
    onMarkStatus: (String, Boolean) -> Unit,
    onResumeDiagnostic: () -> Unit,
    onBack: () -> Unit
) {
    if (diagnostic == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucun résultat de diagnostic disponible.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Résultat FixAI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Speech Readout Button
            IconButton(
                onClick = {
                    val fullSpeech = "${diagnostic.problemDetected}. " +
                            diagnostic.steps.joinToString(". ") { "Étape ${it.stepNumber}: ${it.instructionBeginner}" }
                    onSpeakText(fullSpeech)
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSpeaking) TechGreen else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Read Loud",
                    tint = if (isSpeaking) Color.Black else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Detected Problem Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "🔴 PROBLÈME DÉTECTÉ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TechDanger,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = diagnostic.problemDetected,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!diagnostic.errorCode.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Code d'erreur: ${diagnostic.errorCode}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confidence & Difficulty Badges
                ConfidenceBadge(
                    difficulty = diagnostic.difficulty,
                    confidenceProbability = diagnostic.confidenceProbability
                )
            }
        }

        // Safety Warning Card if needed
        if (diagnostic.requiresSafetyWarning || diagnostic.safetyWarning != null) {
            Spacer(modifier = Modifier.height(14.dp))
            SafetyWarningCard(warningText = diagnostic.safetyWarning)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technician Mode Toggle Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onToggleTechnicianMode() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Tech Mode",
                    tint = CyanNeon
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isTechnicianMode) "MODE TECHNICIEN ACTIF" else "Afficher les détails techniques",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isTechnicianMode) CyanNeon else Color.Gray)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isTechnicianMode) "ON" else "OFF",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technician Detailed Specs Panel
        if (isTechnicianMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💻 RENSEIGNEMENTS TECHNIQUES & CLI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (diagnostic.possibleCauses.isNotEmpty()) {
                        Text(
                            text = "• Causes possibles : ${diagnostic.possibleCauses.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }

                    if (diagnostic.affectedComponents.isNotEmpty()) {
                        Text(
                            text = "• Composants : ${diagnostic.affectedComponents.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }

                    if (diagnostic.techCommands.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Commandes CLI conseillées :",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )
                        diagnostic.techCommands.forEach { cmd ->
                            Text(
                                text = "  > $cmd",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TechGreen
                            )
                        }
                    }

                    if (!diagnostic.biosUefiInfo.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• BIOS/UEFI : ${diagnostic.biosUefiInfo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFE0B2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Solution Steps
        Text(
            text = "🔧 SOLUTION ÉTAPE PAR ÉTAPE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        diagnostic.steps.forEach { step ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CyanNeon, PurpleElectric))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${step.stepNumber}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isTechnicianMode && !step.instructionTech.isNullOrBlank())
                                step.instructionTech
                            else
                                step.instructionBeginner,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Resolution Feedback Section
        Text(
            text = "RÉSULTAT DU DIAGNOSTIC :",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onMarkStatus(diagnostic.id, true) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (diagnostic.statusResolved == true) TechGreen else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Problème résolu ✅",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (diagnostic.statusResolved == true) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Button(
                onClick = {
                    onMarkStatus(diagnostic.id, false)
                    onResumeDiagnostic()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (diagnostic.statusResolved == false) TechDanger else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Toujours en panne ❌",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (diagnostic.statusResolved == false) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
