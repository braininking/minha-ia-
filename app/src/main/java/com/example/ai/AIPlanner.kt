package com.example.ai

import com.example.model.ActionType
import com.example.model.AutonomyLevel
import com.example.model.TaskPlan

class AIPlanner {

    companion object {
        val ALLOWED_ACTIONS = setOf(
            ActionType.LAUNCH_APP,
            ActionType.OPEN_URL,
            ActionType.ACCESSIBILITY_CLICK,
            ActionType.ACCESSIBILITY_INPUT,
            ActionType.ACCESSIBILITY_SCROLL,
            ActionType.SEND_WHATSAPP_MESSAGE,
            ActionType.SEARCH_WEB,
            ActionType.WAIT_DELAY,
            ActionType.VERIFY_RESULT
        )
    }

    fun validateAndApplyPolicy(plan: TaskPlan, autonomyLevel: AutonomyLevel): Result<TaskPlan> {
        if (plan.steps.isEmpty()) {
            return Result.failure(IllegalArgumentException("O plano gerado não contém etapas."))
        }

        if (plan.steps.size > 25) {
            return Result.failure(IllegalArgumentException("O plano excede o limite máximo seguro de 25 etapas."))
        }

        for (step in plan.steps) {
            if (!ALLOWED_ACTIONS.contains(step.actionType)) {
                return Result.failure(SecurityException("Ação não permitida detectada: ${step.actionType}"))
            }

            // Apply autonomy policy
            when (autonomyLevel) {
                AutonomyLevel.CONSERVADOR -> {
                    step.requiresConfirmation = (step.actionType != ActionType.WAIT_DELAY && step.actionType != ActionType.VERIFY_RESULT)
                }
                AutonomyLevel.EQUILIBRADO -> {
                    step.requiresConfirmation = when (step.actionType) {
                        ActionType.SEND_WHATSAPP_MESSAGE -> true
                        else -> false
                    }
                }
                AutonomyLevel.AVANCADO -> {
                    // In advanced mode, message preparation requires confirmation before actual send
                    step.requiresConfirmation = (step.actionType == ActionType.SEND_WHATSAPP_MESSAGE && step.targetText?.isBlank() == true)
                }
            }
        }

        plan.autonomyLevel = autonomyLevel
        return Result.success(plan)
    }
}
