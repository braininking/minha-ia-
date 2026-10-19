package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StepStatus
import com.example.model.TaskPlan
import com.example.model.TaskStatus
import com.example.model.TaskStep
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

@Composable
fun TaskPlanCard(
    plan: TaskPlan,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_plan_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(
            1.dp,
            when (plan.status) {
                TaskStatus.COMPLETED -> EmeraldGreen
                TaskStatus.FAILED -> ErrorRed
                TaskStatus.WAITING_CONFIRMATION -> WarningAmber
                else -> OutlineGreen
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Goal & Autonomy badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (plan.status) {
                                    TaskStatus.COMPLETED -> EmeraldGreen
                                    TaskStatus.FAILED -> ErrorRed
                                    TaskStatus.WAITING_CONFIRMATION -> WarningAmber
                                    TaskStatus.EXECUTING -> NeonGreen
                                    else -> WarningAmber
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = plan.status.labelPt.uppercase(),
                        color = when (plan.status) {
                            TaskStatus.COMPLETED -> EmeraldGreen
                            TaskStatus.FAILED -> ErrorRed
                            TaskStatus.WAITING_CONFIRMATION -> WarningAmber
                            TaskStatus.EXECUTING -> NeonGreen
                            else -> TextSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Autonomy badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Autonomia",
                        tint = NeonGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = plan.autonomyLevel.titlePt,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User Goal Text
            Text(
                text = plan.goal,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Steps Progress list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                plan.steps.forEachIndexed { idx, step ->
                    StepRow(step = step, isCurrent = (idx == plan.currentStepIndex && !plan.isFinished()))
                }
            }

            // Error message if any
            if (!plan.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Erro: ${plan.errorMessage}",
                    color = ErrorRed,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (plan.status == TaskStatus.WAITING_CONFIRMATION) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Autorização necessária para prosseguir com a etapa selecionada:",
                        color = WarningAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onApprove,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_approve_step"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Autorizar", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_reject_step"),
                            border = BorderStroke(1.dp, ErrorRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rejeitar")
                        }
                    }
                }
            }

            // Cancel / Interrupt button when active
            if (!plan.isFinished()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_cancel_task"),
                    border = BorderStroke(1.dp, OutlineGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interromper",
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Interromper Tarefa", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StepRow(step: TaskStep, isCurrent: Boolean) {
    val (icon, tint) = when (step.status) {
        StepStatus.COMPLETED -> Pair(Icons.Default.CheckCircle, EmeraldGreen)
        StepStatus.RUNNING -> Pair(Icons.Default.HourglassBottom, NeonGreen)
        StepStatus.WAITING_APPROVAL -> Pair(Icons.Default.Security, WarningAmber)
        StepStatus.FAILED -> Pair(Icons.Default.Error, ErrorRed)
        StepStatus.CANCELLED, StepStatus.SKIPPED -> Pair(Icons.Default.Cancel, TextMuted)
        StepStatus.PENDING -> Pair(Icons.Default.Schedule, TextMuted)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) SurfaceElevated else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = step.status.name,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${step.index + 1}. ${step.description}",
                color = if (isCurrent) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
            )
            if (!step.resultMessage.isNullOrBlank()) {
                Text(
                    text = step.resultMessage ?: "",
                    color = if (step.status == StepStatus.FAILED) ErrorRed else TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
