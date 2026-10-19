package com.example.automation

import android.content.Context
import android.util.Log
import com.example.accessibility.AppAccessibilityService
import com.example.ai.AIPlanner
import com.example.ai.AIProvider
import com.example.ai.GeminiProvider
import com.example.ai.LocalIntelligentProvider
import com.example.data.SettingsRepository
import com.example.data.local.AppDatabase
import com.example.data.local.AuditEntity
import com.example.data.local.ChatEntity
import com.example.data.local.TaskEntity
import com.example.model.AssistantState
import com.example.model.AutonomyLevel
import com.example.model.InstalledAppInfo
import com.example.model.MessageSender
import com.example.model.StepStatus
import com.example.model.TaskPlan
import com.example.model.TaskStatus
import com.example.model.TaskStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class TaskManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "TaskManager"
        @Volatile
        private var instance: TaskManager? = null

        fun getInstance(context: Context, scope: CoroutineScope): TaskManager {
            return instance ?: synchronized(this) {
                instance ?: TaskManager(context.applicationContext, scope).also { instance = it }
            }
        }
    }

    private val db = AppDatabase.getDatabase(context)
    private val settings = SettingsRepository(context)
    private val localProvider = LocalIntelligentProvider()
    private val geminiProvider = GeminiProvider(localProvider)
    private val planner = AIPlanner()
    private val actionExecutor = ActionExecutor(context)

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _activePlan = MutableStateFlow<TaskPlan?>(null)
    val activePlan: StateFlow<TaskPlan?> = _activePlan.asStateFlow()

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    private val isCancelled = AtomicBoolean(false)
    private var executionJob: Job? = null

    fun getActiveProvider(providerId: String): AIProvider {
        return if (providerId == "GEMINI_API") geminiProvider else localProvider
    }

    fun submitCommand(goal: String, installedApps: List<InstalledAppInfo>) {
        if (goal.isBlank()) return
        isCancelled.set(false)
        executionJob?.cancel()

        executionJob = scope.launch(Dispatchers.Main) {
            val autonomy = settings.autonomyLevelFlow.first()
            val providerType = settings.aiProviderFlow.first()
            val provider = getActiveProvider(providerType)

            // 1. Record user message in chat
            val userMsgId = UUID.randomUUID().toString()
            db.chatDao().insertMessage(
                ChatEntity(
                    id = userMsgId,
                    sender = MessageSender.USER.name,
                    text = goal,
                    timestamp = System.currentTimeMillis(),
                    associatedTaskId = null,
                    planSummary = null
                )
            )

            _assistantState.value = AssistantState.INTERPRETING
            _lastMessage.value = "Interpretando comando..."

            val screenContext = AppAccessibilityService.getInstance()?.getCurrentScreenSummary()

            _assistantState.value = AssistantState.PLANNING
            _lastMessage.value = "Criando plano de execução..."

            val planResult = provider.planTask(goal, installedApps, screenContext)

            if (planResult.isFailure) {
                _assistantState.value = AssistantState.ERROR
                val err = planResult.exceptionOrNull()?.message ?: "Erro ao planejar tarefa."
                _lastMessage.value = err
                recordAssistantChat("Falha ao criar plano: $err", null, null)
                return@launch
            }

            val rawPlan = planResult.getOrThrow()
            val validatedResult = planner.validateAndApplyPolicy(rawPlan, autonomy)

            if (validatedResult.isFailure) {
                _assistantState.value = AssistantState.ERROR
                val err = validatedResult.exceptionOrNull()?.message ?: "Plano bloqueado pela política de segurança."
                _lastMessage.value = err
                recordAssistantChat("Bloqueio de segurança: $err", null, null)
                return@launch
            }

            val validatedPlan = validatedResult.getOrThrow()
            _activePlan.value = validatedPlan

            persistTask(validatedPlan)

            // Start step-by-step execution
            executePlanSequence(validatedPlan)
        }
    }

    private suspend fun executePlanSequence(plan: TaskPlan) {
        plan.status = TaskStatus.EXECUTING
        _activePlan.value = plan
        persistTask(plan)

        while (plan.currentStepIndex < plan.steps.size) {
            if (isCancelled.get()) {
                plan.status = TaskStatus.CANCELLED
                plan.finishedAt = System.currentTimeMillis()
                _activePlan.value = plan
                _assistantState.value = AssistantState.IDLE
                _lastMessage.value = "Tarefa interrompida pelo usuário."
                persistTask(plan)
                recordAudit("CANCEL", plan.goal, "Cancelada pelo usuário na etapa ${plan.currentStepIndex + 1}", "WARN", plan.id)
                recordAssistantChat("Tarefa cancelada conforme solicitado. Progresso até a etapa ${plan.currentStepIndex} de ${plan.steps.size}.", plan.id, "Cancelada")
                return
            }

            val step = plan.steps[plan.currentStepIndex]

            // Check if step requires approval
            if (step.requiresConfirmation) {
                step.status = StepStatus.WAITING_APPROVAL
                plan.status = TaskStatus.WAITING_CONFIRMATION
                _activePlan.value = plan
                _assistantState.value = AssistantState.PAUSED
                _lastMessage.value = "Aguardando confirmação para: ${step.description}"
                persistTask(plan)
                return // Will resume when approveStep() is called
            }

            // Execute step
            _assistantState.value = AssistantState.EXECUTING
            _lastMessage.value = "Executando: ${step.description}"

            val result = actionExecutor.executeStep(step) { isCancelled.get() }

            when (result) {
                is StepExecutionResult.Success -> {
                    recordAudit(step.actionType.name, step.targetLabel ?: step.targetPackage ?: "", result.message, "INFO", plan.id)
                    plan.currentStepIndex++
                    _activePlan.value = plan
                    persistTask(plan)
                }
                is StepExecutionResult.Failure -> {
                    plan.status = TaskStatus.FAILED
                    plan.errorMessage = result.errorMessage
                    plan.finishedAt = System.currentTimeMillis()
                    _activePlan.value = plan
                    _assistantState.value = AssistantState.ERROR
                    _lastMessage.value = result.errorMessage
                    persistTask(plan)
                    recordAudit(step.actionType.name, step.targetLabel ?: "", result.errorMessage, "ERROR", plan.id)
                    recordAssistantChat("A tarefa falhou na etapa ${plan.currentStepIndex + 1}: ${result.errorMessage}", plan.id, "Falhou")
                    return
                }
                is StepExecutionResult.Cancelled -> {
                    plan.status = TaskStatus.CANCELLED
                    plan.finishedAt = System.currentTimeMillis()
                    _activePlan.value = plan
                    _assistantState.value = AssistantState.IDLE
                    _lastMessage.value = "Execução cancelada."
                    persistTask(plan)
                    return
                }
                is StepExecutionResult.NeedsConfirmation -> {
                    step.status = StepStatus.WAITING_APPROVAL
                    plan.status = TaskStatus.WAITING_CONFIRMATION
                    _activePlan.value = plan
                    _assistantState.value = AssistantState.PAUSED
                    return
                }
            }
        }

        // All steps completed!
        plan.status = TaskStatus.COMPLETED
        plan.finishedAt = System.currentTimeMillis()
        _activePlan.value = plan
        _assistantState.value = AssistantState.COMPLETED
        _lastMessage.value = "Tarefa concluída com sucesso!"
        persistTask(plan)
        recordAudit("COMPLETED", plan.goal, "Todas as ${plan.steps.size} etapas executadas com êxito", "SUCCESS", plan.id)
        recordAssistantChat("Tarefa concluída com sucesso! Todas as ${plan.steps.size} etapas foram executadas.", plan.id, "Concluída")
    }

    fun approveStep() {
        val plan = _activePlan.value ?: return
        if (plan.status != TaskStatus.WAITING_CONFIRMATION) return

        val step = plan.currentStep() ?: return
        step.requiresConfirmation = false // Approved!
        executionJob?.cancel()

        executionJob = scope.launch(Dispatchers.Main) {
            executePlanSequence(plan)
        }
    }

    fun rejectStep() {
        val plan = _activePlan.value ?: return
        val step = plan.currentStep()
        step?.status = StepStatus.SKIPPED
        step?.resultMessage = "Etapa rejeitada pelo usuário."

        scope.launch(Dispatchers.Main) {
            recordAudit(step?.actionType?.name ?: "REJECT", step?.description ?: "", "Rejeitado pelo usuário", "WARN", plan.id)
            cancelActiveTask()
        }
    }

    fun cancelActiveTask() {
        isCancelled.set(true)
        executionJob?.cancel()

        val plan = _activePlan.value
        if (plan != null && !plan.isFinished()) {
            plan.status = TaskStatus.CANCELLED
            plan.finishedAt = System.currentTimeMillis()
            _activePlan.value = plan
            scope.launch {
                persistTask(plan)
            }
        }

        _assistantState.value = AssistantState.IDLE
        _lastMessage.value = "Assistente disponível."
    }

    private suspend fun recordAssistantChat(text: String, taskId: String?, summary: String?) {
        db.chatDao().insertMessage(
            ChatEntity(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.ASSISTANT.name,
                text = text,
                timestamp = System.currentTimeMillis(),
                associatedTaskId = taskId,
                planSummary = summary
            )
        )
    }

    private suspend fun recordAudit(action: String, target: String, result: String, level: String, taskId: String?) {
        db.auditDao().insertLog(
            AuditEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                action = action,
                target = target,
                result = result,
                level = level,
                taskId = taskId
            )
        )
    }

    private suspend fun persistTask(plan: TaskPlan) {
        val stepsJsonArray = JSONArray()
        for (s in plan.steps) {
            val obj = JSONObject()
                .put("id", s.id)
                .put("index", s.index)
                .put("actionType", s.actionType.name)
                .put("description", s.description)
                .put("targetPackage", s.targetPackage ?: "")
                .put("targetLabel", s.targetLabel ?: "")
                .put("targetText", s.targetText ?: "")
                .put("targetQuery", s.targetQuery ?: "")
                .put("status", s.status.name)
                .put("resultMessage", s.resultMessage ?: "")
                .put("requiresConfirmation", s.requiresConfirmation)
            stepsJsonArray.put(obj)
        }

        db.taskDao().insertOrUpdateTask(
            TaskEntity(
                id = plan.id,
                goal = plan.goal,
                stepsJson = stepsJsonArray.toString(),
                currentStepIndex = plan.currentStepIndex,
                status = plan.status.name,
                createdAt = plan.createdAt,
                finishedAt = plan.finishedAt,
                autonomyLevel = plan.autonomyLevel.name,
                errorMessage = plan.errorMessage
            )
        )
    }
}
