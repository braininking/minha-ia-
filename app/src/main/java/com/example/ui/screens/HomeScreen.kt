package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.automation.TaskManager
import com.example.model.AssistantState
import com.example.model.InstalledAppInfo
import com.example.ui.components.HolographicOrb
import com.example.ui.components.TaskPlanCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.OutlineGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.voice.SpeechManager

@Composable
fun HomeScreen(
    taskManager: TaskManager,
    speechManager: SpeechManager,
    installedApps: List<InstalledAppInfo>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assistantState by taskManager.assistantState.collectAsState()
    val activePlan by taskManager.activePlan.collectAsState()
    val lastMessage by taskManager.lastMessage.collectAsState()

    val isListening by speechManager.isListening.collectAsState()
    val audioRms by speechManager.audioRms.collectAsState()

    var textInput by remember { mutableStateOf("") }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechManager.startListening(
                onResult = { text ->
                    textInput = text
                    taskManager.submitCommand(text, installedApps)
                },
                onError = { /* Logged in SpeechManager */ }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Holographic Orb Central Visualizer
            HolographicOrb(
                state = if (isListening) AssistantState.LISTENING else assistantState,
                audioLevel = audioRms,
                size = 230.dp,
                onClick = {
                    if (isListening) {
                        speechManager.stopListening()
                    } else if (assistantState == AssistantState.EXECUTING) {
                        taskManager.cancelActiveTask()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live State Label
            Text(
                text = if (isListening) "ESCUTANDO COMANDO..." else assistantState.labelPt.uppercase(),
                color = when {
                    isListening -> NeonGreen
                    assistantState == AssistantState.ERROR -> ErrorRed
                    assistantState == AssistantState.PAUSED -> WarningAmber
                    assistantState == AssistantState.COMPLETED -> EmeraldGreen
                    else -> NeonGreen
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            // Dynamic Subtitle / Last status message
            if (!lastMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lastMessage ?: "",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Active Task Card if executing, waiting or completed
            activePlan?.let { plan ->
                TaskPlanCard(
                    plan = plan,
                    onApprove = { taskManager.approveStep() },
                    onReject = { taskManager.rejectStep() },
                    onCancel = { taskManager.cancelActiveTask() }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Quick Preset Suggestions
            Text(
                text = "SUGESTÕES RÁPIDAS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            val suggestions = listOf(
                "Abra o Chrome e pesquise notícias de tecnologia",
                "Mande uma mensagem para Marcos dizendo que chego às 18h",
                "Abra a Calculadora",
                "Abra o YouTube",
                "Pesquise restaurantes japoneses próximos no Google",
                "Abra as Configurações do Android"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceDark)
                            .border(1.dp, OutlineGreen, RoundedCornerShape(20.dp))
                            .clickable {
                                textInput = suggestion
                                taskManager.submitCommand(suggestion, installedApps)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = suggestion,
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Command Input Section (Voice button + Text field)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(SurfaceDark)
                    .border(1.dp, if (isListening) NeonGreen else OutlineGreen, RoundedCornerShape(28.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Recognition Mic Button
                IconButton(
                    onClick = {
                        if (isListening) {
                            speechManager.stopListening()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                speechManager.startListening(
                                    onResult = { text ->
                                        textInput = text
                                        taskManager.submitCommand(text, installedApps)
                                    },
                                    onError = { /* fallback */ }
                                )
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isListening) DarkGreen else Color.Transparent)
                        .testTag("btn_voice_input")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Comando de voz",
                        tint = if (isListening) NeonGreen else EmeraldGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Text Input field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "Escutando..." else "Fale ou digite um comando...",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonGreen
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_text_input")
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val cmd = textInput
                            textInput = ""
                            taskManager.submitCommand(cmd, installedApps)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkGreen)
                        .testTag("btn_send_command")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar comando",
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
