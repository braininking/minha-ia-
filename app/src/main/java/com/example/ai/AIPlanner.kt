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

        private val ALWAYS_CONFIRM_ACTIONS = setOf(
            ActionType.SEND_WHATSAPP_MESSAGE,
            ActionType.ACCESSIBILITY_CLICK,
            ActionType.ACCESSIBILITY_INPUT
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
            if (step.actionType !in ALLOWED_ACTIONS) {
                return Result.failure(SecurityException("Ação não permitida detectada: ${step.actionType}"))
            }

            val missingRequiredTarget = when (step.actionType) {
                ActionType.LAUNCH_APP -> step.targetPackage.isNullOrBlank()
                ActionType.OPEN_URL, ActionType.SEARCH_WEB -> step.targetQuery.isNullOrBlank()
                ActionType.ACCESSIBILITY_CLICK -> (step.targetLabel ?: step.targetText).isNullOrBlank()
                ActionType.ACCESSIBILITY_INPUT -> step.targetText.isNullOrBlank()
                ActionType.SEND_WHATSAPP_MESSAGE -> step.targetText.isNullOrBlank()
                else -> false
            }
            if (missingRequiredTarget) {
                return Result.failure(
                    IllegalArgumentException("A etapa '${step.description}' não contém o destino ou conteúdo necessário.")
                )
            }

            // Policy is enforced locally; a model-generated plan cannot lower these safeguards.
            val explicitSendIntent = Regex("\\b(envie|envia|enviar|mande|manda|mandar|send)\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(plan.goal)
            val isWhatsAppFlow = explicitSendIntent &&
                plan.steps.any { it.actionType == ActionType.SEND_WHATSAPP_MESSAGE }
            step.requiresConfirmation = when (autonomyLevel) {
                AutonomyLevel.CONSERVADOR ->
                    step.actionType != ActionType.WAIT_DELAY &&
                        step.actionType != ActionType.VERIFY_RESULT
                AutonomyLevel.EQUILIBRADO, AutonomyLevel.AVANCADO -> {
                    // In a WhatsApp workflow, allow navigation and typing to proceed hands-free.
                    // The final send action runs only after runtime checks pass.
                    if (isWhatsAppFlow) {
                        // The spoken command explicitly requests sending; the executor only
                        // sends after verifying the exact conversation and the complete draft.
                        false
                    } else {
                        step.actionType in ALWAYS_CONFIRM_ACTIONS
                    }
                }
            }
        }

        plan.autonomyLevel = autonomyLevel
        return Result.success(plan)
    }
}
