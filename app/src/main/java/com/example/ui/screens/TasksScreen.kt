package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.AuditEntity
import com.example.data.local.TaskEntity
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
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen(
    database: AppDatabase,
    modifier: Modifier = Modifier
) {
    val tasks by database.taskDao().getAllTasks().collectAsState(initial = emptyList())
    val auditLogs by database.auditDao().getRecentLogs().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tarefas (${tasks.size})", "Auditoria de Ações (${auditLogs.size})")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BackgroundDark,
            contentColor = NeonGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonGreen
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) NeonGreen else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Tasks List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma tarefa automatizada registrada ainda.\nExecute comandos pela tela inicial.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(tasks, key = { it.id }) { task ->
                    TaskHistoryCard(task = task)
                }
            }
        } else {
            // Audit Logs List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (auditLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum registro de auditoria disponível.",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                items(auditLogs, key = { it.id }) { log ->
                    AuditLogRow(log = log)
                }
            }
        }
    }
}

@Composable
private fun TaskHistoryCard(task: TaskEntity) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(task.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineGreen)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (task.status) {
                        "COMPLETED" -> EmeraldGreen
                        "FAILED" -> ErrorRed
                        "CANCELLED" -> TextMuted
                        else -> WarningAmber
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formattedDate,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = task.goal,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modo: ${task.autonomyLevel}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expandir detalhes",
                    tint = NeonGreen
                )
            }

            val parsedSteps = remember(task.stepsJson) {
                val list = mutableListOf<Triple<String, String, String>>()
                try {
                    val array = JSONArray(task.stepsJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val desc = obj.optString("description")
                        val status = obj.optString("status")
                        val result = obj.optString("resultMessage")
                        list.add(Triple(desc, status, result))
                    }
                } catch (_: Exception) {}
                list
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Etapas Registradas:",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (parsedSteps.isEmpty()) {
                        Text(text = "Detalhes não disponíveis", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        parsedSteps.forEachIndexed { i, (desc, status, result) ->
                            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text(
                                    text = "${i + 1}. $desc [$status]",
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                                if (result.isNotBlank()) {
                                    Text(
                                        text = "   Resultado: $result",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogRow(log: AuditEntity) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(log.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, OutlineGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val levelColor = when (log.level) {
            "SUCCESS" -> EmeraldGreen
            "ERROR" -> ErrorRed
            "WARN" -> WarningAmber
            else -> NeonGreen
        }

        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(levelColor)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.action,
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedTime,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            if (log.target.isNotBlank()) {
                Text(
                    text = "Alvo: ${log.target}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Text(
                text = log.result,
                color = TextPrimary,
                fontSize = 12.sp
            )
        }
    }
}
