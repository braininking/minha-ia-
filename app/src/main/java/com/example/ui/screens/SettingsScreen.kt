package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.AppAccessibilityService
import com.example.ai.GeminiProvider
import com.example.data.SettingsRepository
import com.example.data.local.AppDatabase
import com.example.model.AutonomyLevel
import com.example.services.AssistantForegroundService
import com.example.services.FloatingBubbleService
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    database: AppDatabase,
    speechManager: SpeechManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var resumeVersion by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeVersion++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val aiProvider by settingsRepository.aiProviderFlow.collectAsState(initial = "LOCAL_INTELLIGENT")
    val autonomyLevel by settingsRepository.autonomyLevelFlow.collectAsState(initial = AutonomyLevel.EQUILIBRADO)
    val speechRate by settingsRepository.speechRateFlow.collectAsState(initial = 1.0f)
    val speechPitch by settingsRepository.speechPitchFlow.collectAsState(initial = 1.0f)
    val spokenResponses by settingsRepository.spokenResponsesFlow.collectAsState(initial = true)
    val foregroundServiceEnabled by settingsRepository.foregroundServiceFlow.collectAsState(initial = false)
    val floatingBubbleEnabled by settingsRepository.floatingBubbleFlow.collectAsState(initial = false)

    var connectionTestResult by remember { mutableStateOf<String?>(null) }
    val isAccessibilityActive = remember(resumeVersion) {
        AppAccessibilityService.isAccessibilityEnabled(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CONFIGURAÇÕES DO ASSISTENTE",
                color = NeonGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Section 1: Inteligência Artificial
        item {
            SettingsCard(title = "Inteligência Artificial", icon = Icons.Default.Memory) {
                Text(
                    text = "Provedor Ativo:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Option 1: Motor Local
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { settingsRepository.setAiProvider("LOCAL_INTELLIGENT") } },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = aiProvider == "LOCAL_INTELLIGENT",
                        onClick = { scope.launch { settingsRepository.setAiProvider("LOCAL_INTELLIGENT") } },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonGreen, unselectedColor = TextMuted)
                    )
                    Column {
                        Text("Motor Local Inteligente", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Execução nativa no aparelho, rápida, offline e sem custo", color = TextMuted, fontSize = 11.sp)
                    }
                }

                // Option 2: Gemini API
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { settingsRepository.setAiProvider("GEMINI_API") } },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = aiProvider == "GEMINI_API",
                        onClick = { scope.launch { settingsRepository.setAiProvider("GEMINI_API") } },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonGreen, unselectedColor = TextMuted)
                    )
                    Column {
                        Text("Google Gemini API (Cloud)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Modelo em nuvem para comandos complexos e raciocínio avançado", color = TextMuted, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (aiProvider == "GEMINI_API") {
                            connectionTestResult = "Testando conexão Gemini..."
                            scope.launch {
                                val result = GeminiProvider().testConnection()
                                connectionTestResult = result.fold(
                                    onSuccess = { it },
                                    onFailure = { "Falha no Gemini: ${it.message ?: "erro desconhecido"}" }
                                )
                            }
                        } else {
                            connectionTestResult = "Motor Local selecionado. Ele não depende de internet nem de chave de API."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = NeonGreen)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Testar Conexão do Provedor", fontSize = 12.sp)
                }

                if (!connectionTestResult.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = connectionTestResult ?: "",
                        color = if (connectionTestResult?.startsWith("Falha") == true) ErrorRed else EmeraldGreen,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Section 2: Segurança & Níveis de Autonomia
        item {
            SettingsCard(title = "Nível de Autonomia & Segurança", icon = Icons.Default.Security) {
                AutonomyLevel.values().forEach { level ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (autonomyLevel == level) SurfaceElevated else Color.Transparent)
                            .clickable { scope.launch { settingsRepository.setAutonomyLevel(level) } }
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = autonomyLevel == level,
                            onClick = { scope.launch { settingsRepository.setAutonomyLevel(level) } },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonGreen, unselectedColor = TextMuted)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = level.titlePt,
                                color = if (autonomyLevel == level) NeonGreen else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = level.descriptionPt,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Section 3: Voz & Respostas Faladas
        item {
            SettingsCard(title = "Voz & Respostas Faladas", icon = Icons.Default.GraphicEq) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Respostas por Voz", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Ouvir respostas do assistente em áudio", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = spokenResponses,
                        onCheckedChange = { scope.launch { settingsRepository.setSpokenResponses(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = DarkGreen)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Velocidade da Fala: ${"%.1f".format(speechRate)}x", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = speechRate,
                    onValueChange = { scope.launch { settingsRepository.setSpeechRate(it) } },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = EmeraldGreen)
                )

                Text("Tom da Voz: ${"%.1f".format(speechPitch)}x", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = speechPitch,
                    onValueChange = { scope.launch { settingsRepository.setSpeechPitch(it) } },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = EmeraldGreen)
                )

                Button(
                    onClick = {
                        speechManager.speak("Olá! Sou o Gemini Mobile AI, configurado com velocidade e tom ajustados.", speechRate, speechPitch)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = NeonGreen)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ouvir Teste de Voz", fontSize = 12.sp)
                }
            }
        }

        // Section 4: Acessibilidade & Controle
        item {
            SettingsCard(title = "Acessibilidade & Controle de Tela", icon = Icons.Default.AccessibilityNew) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Serviço de Acessibilidade", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isAccessibilityActive) "Ativo e pronto para ações na interface" else "Necessário ativar para clicar e digitar",
                            color = if (isAccessibilityActive) EmeraldGreen else WarningAmber,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAccessibilityActive) EmeraldGreen else WarningAmber)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = NeonGreen)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir Configurações de Acessibilidade", fontSize = 12.sp)
                }
            }
        }

        // Section 5: Segundo Plano & Botão Flutuante
        item {
            SettingsCard(title = "Segundo Plano & Atalhos", icon = Icons.Default.Layers) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Serviço em Segundo Plano", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Mantém o assistente ativo com notificação", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = foregroundServiceEnabled,
                        onCheckedChange = { enable ->
                            scope.launch {
                                settingsRepository.setForegroundService(enable)
                                if (enable) {
                                    AssistantForegroundService.startService(context)
                                } else {
                                    AssistantForegroundService.stopService(context)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = DarkGreen)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Botão Flutuante (Overlay)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Esfera verde holográfica sobre outros apps", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = floatingBubbleEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    context.startActivity(intent)
                                } else {
                                    scope.launch {
                                        settingsRepository.setFloatingBubble(true)
                                        FloatingBubbleService.startBubble(context)
                                    }
                                }
                            } else {
                                scope.launch {
                                    settingsRepository.setFloatingBubble(false)
                                    FloatingBubbleService.stopBubble(context)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = DarkGreen)
                    )
                }
            }
        }

        // Section 6: Privacidade & Dados
        item {
            SettingsCard(title = "Privacidade & Limpeza", icon = Icons.Default.DeleteForever) {
                Text(
                    text = "Todos os dados de conversas, automações e auditorias são armazenados estritamente no armazenamento local do aparelho.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            database.chatDao().clearAll()
                            database.taskDao().clearAll()
                            database.auditDao().clearAll()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Limpar Todos os Dados e Registros", fontSize = 12.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineGreen)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            content()
        }
    }
}
